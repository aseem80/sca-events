package io.nordstrom.org.scaevents.producer;

import io.nordstrom.org.scaevents.dao.PayloadDao;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Created by bmwi on 4/11/18.
 */

@Component
public class KafkaTemplateWrapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaTemplateWrapper.class);


    private static final String TRACE_ID_HEADER = "TraceID";


    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private PayloadDao payloadDao;

    @Value("${scaevents.producer.max-retry-attempts:5}")
    private Integer maxRetry;

    @Value("${scaevents.producer.retry.delay.multiplier:2}")
    private Double multiplier;

    @Value("${scaevents.producer.retry.delay.ms:500}")
    private Integer retryDelayInMillis;



    //Use it in high performance and non-transactional scenario
    public void sendAsync(String topic, String uuid, String payload, String key, Map<String,Object> headers) {
        Message<String> message = buildMessage(topic, uuid, payload, key,headers);
        try {
            sendAsync(message);
        } catch(Throwable t) {
            LOGGER.error("Exception sending message to Kafka Broker. Retrying with BackOff. " + ExceptionUtils.getStackTrace(t));
        }
    }


    public SendResult<String, String> send(String topic, String uuid, String payload, String key, Map<String,Object> headers) throws Throwable{
        return send(buildMessage(topic, uuid, payload, key,headers));
    }


    private SendResult<String, String> send(final Message<String> message) throws Throwable {
        SendResult<String, String> result = null;
        try {
            result = kafkaTemplate.send(message).get();
            LOGGER.info("Successfully published payload ");
        }catch (ExecutionException ex) {
            //Retry can still be succesful. Throw Exception after final retry
            result = retryOnFailure(message, ex);
        }
        return result;
    }

    private Message<String> buildMessage(String topic, String uuid, String payload, String key, Map<String,Object> headers) {
        MessageBuilder messageBuilder= MessageBuilder.withPayload(payload).setHeader(KafkaHeaders.TOPIC, topic).setHeader(KafkaHeaders.MESSAGE_KEY, key)
                .setHeader(TRACE_ID_HEADER, uuid.toString());
        if(null!= headers) {
            for (String header : headers.keySet()) {
                Object headerValue = headers.get(header);
                if(null!=headerValue) {
                    messageBuilder = messageBuilder.setHeader(header, headerValue);
                }
            }
        }
        return messageBuilder.build();
    }



    private ListenableFuture<SendResult<String, String>> sendAsync(final Message<String> message) throws ExecutionException, InterruptedException {
        ListenableFuture<SendResult<String, String>> future = kafkaTemplate.send(message);
        future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {

            @Override
            public void onSuccess(SendResult<String, String> result) {
                LOGGER.info("Successfully published payload ");
            }

            @Override
            public void onFailure(Throwable ex) {
                try {
                    SendResult<String, String> result = retryOnFailure(message, ex);
                } catch (Throwable t) {
                    MessageHeaders headers = message.getHeaders();
                    String key = (String) headers.get(KafkaHeaders.MESSAGE_KEY);
                    String uuid = (String) headers.get(TRACE_ID_HEADER);
                    payloadDao.saveError(uuid, key, message.getPayload());
                }
            }

        });
        return future;
    }


    private SendResult<String, String> retryOnFailure(Message<String> message, Throwable ex) throws Throwable{
        SendResult<String, String> result = null;
        LOGGER.info("Retrying for : " + ex.getClass().getName());
        double sleepTimeInMilliSeconds = retryDelayInMillis;
        for (int i = 1; i <= maxRetry; i++) {
            String logString = new StringBuilder().append(ex.getClass().getName())
                    .append(" encountered. Retrying ").append(i).append(" times after ")
                    .append(sleepTimeInMilliSeconds).append(" milliseconds.").toString();
            LOGGER.info(logString);
            try {
                Thread.sleep((long) sleepTimeInMilliSeconds);
                try {
                    result=kafkaTemplate.send(message).get();
                    //Break on Successful retry
                    LOGGER.info("Got success after " + i + " retry attempt. Hence Exiting.");
                    return result;
                } catch (Throwable e) {
                    LOGGER.warn("Exception encountered on " + i + " retry attempt ");
                    if (i == maxRetry) {
                        LOGGER.warn("Final retry attempt reached and hence giving up.");
                        //Throw it only after final Attempt
                        throw e;
                    }
                }
                if (multiplier > 0) {
                    sleepTimeInMilliSeconds = multiplier * sleepTimeInMilliSeconds;
                }
            } catch (InterruptedException e) {
                LOGGER.warn("InterruptedException while retrying : " + ExceptionUtils.getStackTrace(e));
                throw e;
            }
        }
        return result;
    }




}

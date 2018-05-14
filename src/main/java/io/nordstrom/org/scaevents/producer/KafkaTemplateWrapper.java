package io.nordstrom.org.scaevents.producer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
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

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static io.nordstrom.org.scaevents.util.PropertiesUtil.DATADOG_METRICS_PREFIX;
import static io.nordstrom.org.scaevents.util.PropertiesUtil.DATADOG_METRICS_TAG_KEY;

/**
 * Created by bmwi on 4/11/18.
 */

@Component
public class KafkaTemplateWrapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaTemplateWrapper.class);


    private static final String TRACE_ID_HEADER = "TraceID";


    private final Counter totalMessagesCounter;
    private final Counter successMessagesCounter;
    private final Counter failedMessagesCounter;

    @Autowired
    private KafkaTemplate<String, byte[]> kafkaTemplate;

    @Autowired
    private PayloadDao payloadDao;

    @Value("${scaevents.producer.max-retry-attempts:5}")
    private Integer maxRetry;

    @Value("${scaevents.producer.retry.delay.multiplier:2}")
    private Double multiplier;

    @Value("${scaevents.producer.retry.delay.ms:500}")
    private Integer retryDelayInMillis;

    @Autowired
    public KafkaTemplateWrapper (MeterRegistry registry, @Value("${spring.profiles.active}") String metricsTag) {
        this.totalMessagesCounter = registry.counter(DATADOG_METRICS_PREFIX+"total.produced.messages", DATADOG_METRICS_TAG_KEY, metricsTag);
        this.successMessagesCounter = registry.counter(DATADOG_METRICS_PREFIX+"success.produced.messages", DATADOG_METRICS_TAG_KEY, metricsTag);
        this.failedMessagesCounter = registry.counter(DATADOG_METRICS_PREFIX+"failed.produced.messages", DATADOG_METRICS_TAG_KEY, metricsTag);
    }



    //Use it in high performance and non-transactional scenario
    public void sendAsync(String topic, String uuid, String payload, String key, Map<String,String> headers) {
        totalMessagesCounter.increment();
        Message<byte[]> message = buildMessage(topic, uuid, payload, key,headers);
        try {
            sendAsync(message);
        } catch(Throwable t) {
            LOGGER.error("Exception sending message to Kafka Broker. Retrying with BackOff. " + ExceptionUtils.getStackTrace(t));
        }
    }


    public SendResult<String, byte[]> send(String topic, String uuid, String payload, String key, Map<String,String> headers) {
        totalMessagesCounter.increment();
        SendResult<String, byte[]> result = null;
        try {
            result = send(buildMessage(topic, uuid, payload, key, headers));
            successMessagesCounter.increment();
            return result;
        } catch(Throwable t) {
            failedMessagesCounter.increment();
        }
        return result;
    }


    private SendResult<String, byte[]> send(final Message<byte[]> message) throws Throwable {
        SendResult<String, byte[]> result = null;
        try {
            MessageHeaders headers = message.getHeaders();
            String key = (String) headers.get(KafkaHeaders.MESSAGE_KEY);
            String uuid = (String) headers.get(TRACE_ID_HEADER);
            result = kafkaTemplate.send(message).get();
            LOGGER.info("Successfully published payload for key:{}, TraceId:{} ", key, uuid);
        }catch (Throwable ex) {
            //Retry can still be succesful. Throw Exception after final retry
            result = retryOnFailure(message, ex);
        }
        return result;
    }

    private Message<byte[]> buildMessage(String topic, String uuid, String payload, String key, Map<String,String> headers) {
        Instant instant = Instant.now();
        MessageBuilder messageBuilder= MessageBuilder.withPayload(payload.getBytes()).setHeader(KafkaHeaders.TOPIC, topic).setHeader(KafkaHeaders.MESSAGE_KEY, key)
                .setHeader(TRACE_ID_HEADER, uuid.getBytes()).setHeader(KafkaHeaders.TIMESTAMP, instant.toEpochMilli());
        if(null!= headers) {
            for (String header : headers.keySet()) {
                String headerValue = headers.get(header);
                if(null!=headerValue) {
                    messageBuilder = messageBuilder.setHeader(header, headerValue.getBytes());
                }
            }
        }
        return messageBuilder.build();
    }



    private ListenableFuture<SendResult<String, byte[]>> sendAsync(final Message<byte[]> message) throws ExecutionException, InterruptedException {
        ListenableFuture<SendResult<String, byte[]>> future = kafkaTemplate.send(message);
        future.addCallback(new ListenableFutureCallback<SendResult<String, byte[]>>() {
            @Override
            public void onSuccess(SendResult<String, byte[]> result) {
                MessageHeaders headers = message.getHeaders();
                String key = (String) headers.get(KafkaHeaders.MESSAGE_KEY);
                String uuid = (String) headers.get(TRACE_ID_HEADER);
                successMessagesCounter.increment();
                LOGGER.info("Successfully published payload for key:{}, TraceId:{} ", key, uuid);
            }

            @Override
            public void onFailure(Throwable ex) {
                try {
                    SendResult<String, byte[]> result = retryOnFailure(message, ex);
                } catch (Throwable t) {
                    MessageHeaders headers = message.getHeaders();
                    String key = (String) headers.get(KafkaHeaders.MESSAGE_KEY);
                    String uuid = (String) headers.get(TRACE_ID_HEADER);
                    payloadDao.saveError(uuid, key, message.getPayload());
                    failedMessagesCounter.increment();
                }
            }

        });
        return future;
    }


    private SendResult<String, byte[]> retryOnFailure(Message<byte[]> message, Throwable ex) throws Throwable{
        SendResult<String, byte[]> result = null;
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

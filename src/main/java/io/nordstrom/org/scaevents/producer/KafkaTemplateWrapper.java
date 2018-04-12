package io.nordstrom.org.scaevents.producer;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
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

    @Value("${scaevents.producer.max-retry-attempts:5}")
    private Integer maxRetry;

    @Value("${scaevents.producer.retry.delay.multiplier:2}")
    private Double multiplier;

    @Value("${scaevents.producer.retry.delay.ms:500}")
    private Integer retryDelayInMillis;



    public SendResult<String, String> send(String topic, String uuid, String payload, String key, Map<String,Object> headers) {
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
        Message<String> message = messageBuilder.build();
        try {
            SendResult<String, String> result = send(message);
            return result;
        } catch(Throwable t) {
            LOGGER.error("Exception sending message to Kafka Broker. Retrying with BackOff. " + ExceptionUtils.getStackTrace(t));
            return retryOnFailure(message, t);
        }
    }



    private SendResult<String, String> retryOnFailure(Message<String> message, Throwable ex) {
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
                    result=send(message);
                    //Break on Successful retry
                    LOGGER.info("Got success after " + i + " retry attempt. Hence Exiting.");
                    return result;
                } catch (Throwable e) {
                    LOGGER.warn("Exception encountered on " + i + " retry attempt ");
                    if (i == maxRetry) {
                        LOGGER.warn("Final retry attempt reached and hence giving up.");
                    }
                }
                if (multiplier > 0) {
                    sleepTimeInMilliSeconds = multiplier * sleepTimeInMilliSeconds;
                }
            } catch (InterruptedException e) {
                LOGGER.warn("InterruptedException while retrying : " + ExceptionUtils.getStackTrace(e));
            }
        }
        return result;
    }

    private SendResult<String, String> send(Message<String> message) throws ExecutionException, InterruptedException {
        return kafkaTemplate.send(message).get();
    }


}

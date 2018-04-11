package io.nordstrom.org.scaevents.producer;

import io.nordstrom.org.scaevents.annotation.AsyncRetryable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * Created by bmwi on 4/11/18.
 */

@Component
public class KafkaTemplateWrapper {

    private static final String TRACE_ID_HEADER = "TraceID";


    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;


    @AsyncRetryable(value = { IOException.class },
            maxAttempts = 5,
            backoff = @Backoff(delay = 1000, multiplier = 2))
    public ListenableFuture<SendResult<String, String>> send(String topic, String uuid, String payload, String key, Map<String,Object> headers) {

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
        return kafkaTemplate.send(message);
    }


}

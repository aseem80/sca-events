package io.nordstrom.org.scaevents.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by bmwi on 4/3/18.
 */
public class Sender {

    private static final Logger LOGGER = LoggerFactory.getLogger(Sender.class);
    private static final String SCHEMA_VERSION_HEADER = "SchemaVersion";
    private static final String TRACE_ID_HEADER = "TraceID";
    private static final String MESSAGE_MODE_HEADER = "MessageMode";
    private static final String MESSAGE_TYPE_HEADER = "MessageType";
    private static final String SCHEMA_VERSION_HEADER_VALUE = "1.0";
    private static final String MESSAGE_MODE_HEADER_VALUE = "event";
    private static final String MESSAGE_TYPE_HEADER_VALUE = "org";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${spring.kafka.producer.topic}")
    private String topic;

    @Retryable(value = { IOException.class },
            maxAttempts = 5,
            backoff = @Backoff(delay = 1000, multiplier = 2))
    public void send(String payload, String key) {
        UUID uuid = UUID.randomUUID();
        Message<String> message = MessageBuilder
                .withPayload(payload)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.MESSAGE_KEY, key)
                .setHeader(SCHEMA_VERSION_HEADER, SCHEMA_VERSION_HEADER_VALUE)
                .setHeader(TRACE_ID_HEADER, uuid.toString())
                .setHeader(MESSAGE_MODE_HEADER, MESSAGE_MODE_HEADER_VALUE)
                .setHeader(MESSAGE_TYPE_HEADER, MESSAGE_TYPE_HEADER_VALUE)
                .build();

        LOGGER.info("sending message='{} TraceID='{}' to topic='{}'", payload, uuid.toString(), topic);
        kafkaTemplate.send(message);
    }
}

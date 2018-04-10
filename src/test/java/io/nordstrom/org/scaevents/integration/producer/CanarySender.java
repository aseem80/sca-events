package io.nordstrom.org.scaevents.integration.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Created by bmwi on 4/4/18.
 */



public class CanarySender {

    private static final Logger LOGGER = LoggerFactory.getLogger(CanarySender.class);


    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;


    public void send(String payload, String topic) {
        LOGGER.info("Sending payload from canary producer : " + payload);
        kafkaTemplate.send(topic, payload);
    }
}

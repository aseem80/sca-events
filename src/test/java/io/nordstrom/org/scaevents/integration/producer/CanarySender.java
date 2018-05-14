package io.nordstrom.org.scaevents.integration.producer;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

/**
 * Created by bmwi on 4/4/18.
 */



public class CanarySender {

    private static final Logger LOGGER = LoggerFactory.getLogger(CanarySender.class);


    @Qualifier("canaryTemplate")
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;


    public void send(String payload, String topic) {
        LOGGER.info("Sending payload {} from canary producer  to topic {} " , payload,  topic );
        try {
            SendResult<String, String> result = kafkaTemplate.send(topic, payload).get();
            LOGGER.info("Producer offset {} to topic {} ", result.getRecordMetadata().offset(), result.getRecordMetadata().topic());
        } catch(Exception e) {
            LOGGER.error("Error sending payload from CanarySender " + ExceptionUtils.getStackTrace(e));
        }
    }
}

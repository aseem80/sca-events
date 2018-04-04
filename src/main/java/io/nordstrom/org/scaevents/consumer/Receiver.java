package io.nordstrom.org.scaevents.consumer;

import io.nordstrom.org.scaevents.producer.Sender;
import io.nordstrom.org.scaevents.util.SCAProcessor;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Created by bmwi on 4/3/18.
 */


public class Receiver {


    private static final Logger LOGGER = LoggerFactory.getLogger(Receiver.class);

    @Autowired
    private SCAProcessor scaProcessor;
    @Autowired
    private Sender sender;


    private CountDownLatch latch = new CountDownLatch(1);


    @KafkaListener(topics = "${spring.kafka.consumer.topic}")
    public void receive(String payload) {
        LOGGER.info("received payload='{}'");
        try {
            Map<String, Object> map = scaProcessor.fromCanonicalPayload(payload);
            Pair<String,Boolean> pair = scaProcessor.isSCANodeChanged(map);
            LOGGER.info("SCA changed for store {} : {}", pair.getLeft(), pair.getRight());
            if(pair.getRight()!=null && pair.getRight()) {
                String payloadToSend = scaProcessor.toSCAPayload(map);
                sender.send(payloadToSend, pair.getLeft());
            }
            LOGGER.info("map : " + map);

        } catch (IOException e) {
            LOGGER.error("Error Reading payload from Kafka. StackTrace " + ExceptionUtils.getStackTrace(e));
        }

        latch.countDown();
    }






}

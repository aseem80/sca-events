package io.nordstrom.org.scaevents.consumer;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nordstrom.org.scaevents.producer.Sender;
import io.nordstrom.org.scaevents.util.SCAProcessor;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.common.network.Send;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Created by bmwi on 4/3/18.
 */


public class Receiver {


    private static final Logger LOGGER = LoggerFactory.getLogger(Receiver.class);


    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private SCAProcessor scaProcessor;
    @Autowired
    private Sender sender;


    private CountDownLatch latch = new CountDownLatch(1);


    @KafkaListener(topics = "${spring.kafka.consumer.topic}")
    public void receive(String payload) {
        LOGGER.info("received payload='{}'");
        try {
            Map<String, Object> map = getMap(payload);
            Pair<String,Boolean> pair = scaProcessor.isSCANodeChanged(map);
            LOGGER.info("SCA changed for store {} : {}", pair.getLeft(), pair.getRight());
            if(pair.getRight()!=null && pair.getRight()) {
                Map<String, Object> scaMap = scaProcessor.getSCAPayload(map);
                String payloadToSend = mapper.writeValueAsString(scaMap);
                String key = (String)scaMap.get(SCAProcessor.STORE_NUMBER);
                sender.send(payloadToSend, key);
            }
            LOGGER.info("map : " + map);

        } catch (IOException e) {
            LOGGER.error("Error Reading payload from Kafka. StackTrace " + ExceptionUtils.getStackTrace(e));
        }

        latch.countDown();
    }



    @Retryable(value = { IOException.class },
            maxAttempts = 5,
            backoff = @Backoff(delay = 1000, multiplier = 2))
    public Map<String, Object> getMap(String payload) throws IOException{
        Map<String, Object> map = new HashMap<>();
        try {
            // convert JSON string to Map
            map = mapper.readValue(payload, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonParseException e) {
            LOGGER.error("Invalid Json Payload received. StackTrace " + ExceptionUtils.getStackTrace(e));
        } catch (JsonMappingException e) {
            LOGGER.error("Unexpected Format for Json. StackTrace" + ExceptionUtils.getStackTrace(e));
        }
        return map;
    }


}

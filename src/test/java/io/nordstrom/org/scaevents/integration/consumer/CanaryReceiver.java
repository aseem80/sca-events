package io.nordstrom.org.scaevents.integration.consumer;


import io.nordstrom.org.scaevents.util.OrgKafkaAppTestUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by bmwi on 4/4/18.
 */


public class CanaryReceiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(CanaryReceiver.class);
    private CountDownLatch latch = new CountDownLatch(1);


    @KafkaListener(topics = "${spring.kafka.producer.topic}", containerFactory="canaryKafkaListenerContainerFactory")
    public void receive(byte[] payload, @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key,
                        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                        @Header(KafkaHeaders.RECEIVED_TIMESTAMP) long ts,
                        @Header("TraceID") byte[] uuid,
                        @Header("SchemaVersion") byte[] schemaVersion,
                        @Header("MessageMode") byte[] messageMode,
                        @Header("MessageType") byte[] messageType,
                        @Header("ValueUpdatedTime") byte[] valueUpdatedTime) {
        LOGGER.info("received payload='{}'", payload);
        try {
            Path path  = OrgKafkaAppTestUtil.tempTestFilePath();
            assertEquals("1", key);
            assertTrue(!StringUtils.isBlank(new String(uuid)));
            assertEquals("1", new String(schemaVersion));
            assertEquals("event", new String(messageMode));
            assertEquals("org", new String(messageType));
            assertEquals("2018-05-03T17:05:31.363Z", new String(valueUpdatedTime));
            Files.write(path, payload,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING );
        } catch (IOException e) {
            e.printStackTrace();
        }
        latch.countDown();
    }

}

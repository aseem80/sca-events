package io.nordstrom.org.scaevents.consumer;


import io.nordstrom.org.scaevents.util.OrgKafkaAppTestUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by bmwi on 4/4/18.
 */
public class CanaryReceiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(CanaryReceiver.class);
    private CountDownLatch latch = new CountDownLatch(1);


    @KafkaListener(topics = "${spring.kafka.producer.topic}")
    public void receive(String payload, @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key,
                        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                        @Header(KafkaHeaders.RECEIVED_TIMESTAMP) long ts,
                        @Header("TraceID") String uuid,
                        @Header("SchemaVersion") String schemaVersion,
                        @Header("MessageMode") String messageMode,
                        @Header("MessageType") String messageType) {
        LOGGER.info("received payload='{}'", payload);
        try {
            Path path  = OrgKafkaAppTestUtil.tempTestFilePath();
            assertEquals("1", key);
            assertTrue(!StringUtils.isBlank(uuid));
            assertEquals("1.0", schemaVersion);
            assertEquals("event", messageMode);
            assertEquals("org", messageType);
            Files.write(path, payload.getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING );
        } catch (IOException e) {
            e.printStackTrace();
        }
        latch.countDown();
    }

}

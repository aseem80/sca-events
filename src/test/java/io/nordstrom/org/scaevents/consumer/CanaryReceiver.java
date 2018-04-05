package io.nordstrom.org.scaevents.consumer;


import io.nordstrom.org.scaevents.util.OrgKafkaAppTestUtil;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

/**
 * Created by bmwi on 4/4/18.
 */
public class CanaryReceiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(CanaryReceiver.class);
    private CountDownLatch latch = new CountDownLatch(1);


    @KafkaListener(topics = "${spring.kafka.producer.topic}")
    public void receive(String payload) {
        LOGGER.info("received payload='{}'", payload);
        try {
            Path path  = OrgKafkaAppTestUtil.tempTestFilePath();
            Files.write(path, payload.getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING );
        } catch (IOException e) {
            e.printStackTrace();
        }
        latch.countDown();
    }

}

package io.nordstrom.org.scaevents;

import io.nordstrom.org.scaevents.config.ReceiverConfig;
import io.nordstrom.org.scaevents.config.SenderConfig;
import io.nordstrom.org.scaevents.consumer.CanaryReceiver;
import io.nordstrom.org.scaevents.producer.CanarySender;
import io.nordstrom.org.scaevents.util.OrgKafkaAppTestUtil;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

/**
 * Created by bmwi on 4/4/18.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = {ReceiverConfig.class, SenderConfig.class})
@TestPropertySource("classpath:application-test.properties")
public class OrgKafkaAppIT {



    @Value("${spring.kafka.consumer.topic}")
    private String topic;


    @Autowired
    private CanarySender canarySender;

    @Autowired
    private CanaryReceiver canaryReceiver;

    @Test
    public void scaEvent() throws IOException, InterruptedException {
        String canonicalPayload = FileUtils.readFileToString(new ClassPathResource("payload_sca_change.json").getFile(), "UTF-8");
        canarySender.send(canonicalPayload, topic);
        Thread.sleep(5000);
        Path path  = OrgKafkaAppTestUtil.tempTestFilePath();
        String scaPayloadExpected = FileUtils.readFileToString(new ClassPathResource("sca_payload.json").getFile(), StandardCharsets.UTF_8);
        String scaPayloadActual = FileUtils.readFileToString(path.toFile(), StandardCharsets.UTF_8);
        assertEquals(scaPayloadExpected, scaPayloadActual);

    }

}

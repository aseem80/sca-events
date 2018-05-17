package io.nordstrom.org.scaevents.unit;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.nordstrom.org.scaevents.consumer.Receiver;
import io.nordstrom.org.scaevents.producer.KafkaTemplateWrapper;
import io.nordstrom.org.scaevents.producer.Sender;
import io.nordstrom.org.scaevents.util.SCAProcessor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Created by bmwi on 4/3/18.
 */

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class SCAProcessorTest {


    @Value("${spring.profiles.active}")
    private String metricsTag;

    @MockBean
    private MeterRegistry meterRegistry;
    @Autowired
    private SCAProcessor scaProcessor;
    @MockBean
    private Counter counter;
    @MockBean
    private Receiver receiver;
    @MockBean
    private Sender sender;
    @MockBean
    private KafkaTemplateWrapper wrapper;



    @Before
    public void setupMock() {
        MockitoAnnotations.initMocks(this);
        when(meterRegistry.counter( "total.processed.cannonical.messages"))
                .thenReturn(counter);
        when(meterRegistry.counter("success.converted.sca.messages"))
                .thenReturn(counter);
        when(meterRegistry.counter("failed.converted.sca.messages"))
                .thenReturn(counter);
        when(meterRegistry.counter("empty.sca.messages"))
                .thenReturn(counter);
    }





    @Test
    public void isSCANodeChangedWithChange() throws IOException{
        String canonicalPayload = FileUtils.readFileToString(new ClassPathResource("payload_sca_change.json").getFile(), StandardCharsets.UTF_8);
        Pair<String, Boolean> result = scaProcessor.isSCANodeChanged(scaProcessor.fromCanonicalPayload("1", canonicalPayload));
        assertTrue(result.getRight());
    }

    @Test
    public void isSCANodeChangedNoChange() throws IOException{
        String canonicalPayload = FileUtils.readFileToString(new ClassPathResource("payload_sca_no_change.json").getFile(), StandardCharsets.UTF_8);
        Pair<String, Boolean> result = scaProcessor.isSCANodeChanged(scaProcessor.fromCanonicalPayload("1", canonicalPayload));
        assertFalse(result.getRight());
    }

    @Test
    public void toSCAPayload() throws IOException{
        String canonicalPayload = FileUtils.readFileToString(new ClassPathResource("payload_sca_change.json").getFile(), StandardCharsets.UTF_8);
        Map<String,Object> canonicalMap = scaProcessor.fromCanonicalPayload("1", canonicalPayload);
        String scaPayload = scaProcessor.toSCAPayload(canonicalMap, new HashMap<>());
        String scaPayloadExpected = FileUtils.readFileToString(new ClassPathResource("sca_payload.json").getFile(), StandardCharsets.UTF_8);
        assertEquals(scaPayloadExpected, scaPayload);
    }

    @Test
    public void toSCAPayloadNullScaNode() throws IOException{
        String canonicalPayload = FileUtils.readFileToString(new ClassPathResource("payload_sca_change_null_sca.json").getFile(), StandardCharsets.UTF_8);
        Map<String,Object> canonicalMap = scaProcessor.fromCanonicalPayload("1", canonicalPayload);
        String scaPayload = scaProcessor.toSCAPayload(canonicalMap, new HashMap<>());
        String scaPayloadExpected = FileUtils.readFileToString(new ClassPathResource("sca_payload_sca_node_absent.json").getFile(), StandardCharsets.UTF_8);
        assertEquals(scaPayloadExpected, scaPayload);
    }



}

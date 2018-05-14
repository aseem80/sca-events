package io.nordstrom.org.scaevents.unit;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.nordstrom.org.scaevents.util.SCAProcessor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static io.nordstrom.org.scaevents.util.PropertiesUtil.DATADOG_METRICS_PREFIX;
import static io.nordstrom.org.scaevents.util.PropertiesUtil.DATADOG_METRICS_TAG_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Created by bmwi on 4/3/18.
 */

@RunWith(SpringRunner.class)
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



    @Before
    public void setupMock() {
        MockitoAnnotations.initMocks(this);
        when(meterRegistry.counter(DATADOG_METRICS_PREFIX + "total.processed.cannonical.messages", DATADOG_METRICS_TAG_KEY, metricsTag))
                .thenReturn(counter);
        when(meterRegistry.counter(DATADOG_METRICS_PREFIX + "success.converted.sca.messages", DATADOG_METRICS_TAG_KEY, metricsTag))
                .thenReturn(counter);
        when(meterRegistry.counter(DATADOG_METRICS_PREFIX + "failed.converted.sca.messages", DATADOG_METRICS_TAG_KEY, metricsTag))
                .thenReturn(counter);
        when(meterRegistry.counter(DATADOG_METRICS_PREFIX + "empty.sca.messages", DATADOG_METRICS_TAG_KEY, metricsTag))
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

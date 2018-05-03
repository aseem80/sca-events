package io.nordstrom.org.scaevents.unit;

import io.nordstrom.org.scaevents.util.SCAProcessor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by bmwi on 4/3/18.
 */

@RunWith(SpringRunner.class)
@SpringBootTest
public class SCAProcessorTest {

    @Autowired
    private SCAProcessor scaProcessor;


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



}

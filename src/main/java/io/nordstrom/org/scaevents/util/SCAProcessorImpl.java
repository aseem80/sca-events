package io.nordstrom.org.scaevents.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by bmwi on 4/3/18.
 */

@Service
public class SCAProcessorImpl implements SCAProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SCAProcessorImpl.class);



    @Autowired
    private ObjectMapper mapper;


    @Override
    @Retryable(value = { IOException.class },
            maxAttempts = 5,
            backoff = @Backoff(delay = 1000, multiplier = 2))
    public Map<String, Object> fromCanonicalPayload(String payload) throws IOException{
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


    @Override
    public Pair<String, Boolean> isSCANodeChanged(Map<String, Object> nodes) {
        String storeNumber = (String) nodes.get(STORE_NUMBER);

        if (!nodes.containsKey(ROOT_LEVEL_CHANGED_NODES)) {
            return Pair.of(storeNumber,false);
        }
        Object changedNodes = nodes.get(ROOT_LEVEL_CHANGED_NODES);
        if (changedNodes instanceof Collection) {
            Collection changedNodesCollection = (Collection) changedNodes;
            for (Object change : changedNodesCollection) {
                if (change instanceof Map) {
                    Map changeMap = (Map) change;
                    if (changeMap.containsValue(SCA)) {
                        LOGGER.info("sca node Changed for storeNumber." + storeNumber);
                        return Pair.of(storeNumber,true);
                    }
                }
            }

        }
        return Pair.of(storeNumber,false);

    }

    @Override
    @Retryable(value = { IOException.class },
            maxAttempts = 5,
            backoff = @Backoff(delay = 1000, multiplier = 2))
    public String toSCAPayload(Map<String, Object> nodes) throws IOException{
        Map<String, Object> payload = new LinkedHashMap<>();
        Object currentData = nodes.get(CURRENT_DATA);
        String storeNumber = (String) nodes.get(STORE_NUMBER);
        if (currentData instanceof Map) {
            Map currentDataMap = (Map) currentData;
            if (!currentDataMap.isEmpty()) {
                String storeNumberForCurrentData = (String)currentDataMap.get(STORE_NUMBER);
                if (!StringUtils.isBlank(storeNumberForCurrentData)) {
                    payload.put(STORE_NUMBER, storeNumberForCurrentData);
                } else {
                    payload.put(STORE_NUMBER, storeNumber);
                }
                String timeStamp = (String) nodes.get(TIMESTAMP);
                if (!StringUtils.isBlank(timeStamp)) {
                    payload.put(TIMESTAMP, timeStamp);
                } else {
                    LOGGER.warn("Empty {} node", TIMESTAMP);
                }
                Object scaCurrentData = currentDataMap.get(SCA);
                payload.put(SCA, scaCurrentData);
            } else {
                LOGGER.warn("Empty {} node", CURRENT_DATA);
            }


        }
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);

    }


}

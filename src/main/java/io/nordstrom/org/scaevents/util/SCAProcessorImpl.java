package io.nordstrom.org.scaevents.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nordstrom.org.scaevents.exception.SCAProcessorException;
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
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

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
    public Map<String, Object> fromCanonicalPayload(String key, String payload) {
        Map<String, Object> map = new HashMap<>();
        try {
            // convert JSON string to Map
            map = mapper.readValue(payload, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonParseException e) {
            //Log and Swallow as there is nothing that can be done on this message
            LOGGER.error("Invalid Json Payload received for key='{}'. StackTrace='{}' ", key , ExceptionUtils.getStackTrace(e));
        } catch (JsonMappingException e) {
            //Log and Swallow as there is nothing that can be done on this message
            LOGGER.error("Unexpected Format for Json for key='{}'. StackTrace='{}' ", key , ExceptionUtils.getStackTrace(e));
        } catch (IOException e) {
            LOGGER.error("IOException while parsing Json for key='{}'. StackTrace='{}' ", key , ExceptionUtils.getStackTrace(e));
            SCAProcessorException scaException = new SCAProcessorException("IOException");
            scaException.addSuppressed(e);
            throw scaException;
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
                        LOGGER.info("sca node Changed for storeNumber {}" , storeNumber);
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
    public String toSCAPayload(Map<String, Object> nodes, Map<String, String> headers) {
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
                String now =scaSimpleDateFormat.format( new Date());

                if (!StringUtils.isBlank(timeStamp)) {
                    try {
                        LocalDateTime time = LocalDateTime.parse(timeStamp, canonicalFormatter);

                        Date out = Date.from(time.atZone(ZoneId.systemDefault()).toInstant());

                        String scaDesiredFormatValue = scaSimpleDateFormat.format(out);
                        payload.put(TIMESTAMP, scaDesiredFormatValue);
                        headers.put(SCA_TIMESTAMP_KAFKA_HEADER, scaDesiredFormatValue);
                    } catch(Exception e) {
                        LOGGER.warn("Error in converting timestamp to desired format for store {}. Hence appending system timestamp {} node", storeNumber, timeStamp);
                        payload.put(TIMESTAMP, now);
                        headers.put(SCA_TIMESTAMP_KAFKA_HEADER, now);
                    }

                } else {
                    LOGGER.warn("Empty {} node", TIMESTAMP);
                    payload.put(TIMESTAMP, now);
                    headers.put(SCA_TIMESTAMP_KAFKA_HEADER, now);
                }
                Object scaCurrentData = currentDataMap.get(SCA);
                payload.put(SCA, scaCurrentData);
            } else {
                LOGGER.warn("Empty {} node", CURRENT_DATA);
            }


        }
        try {
            return mapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            SCAProcessorException scaException = new SCAProcessorException("JsonProcessingException");
            scaException.addSuppressed(e);
            throw scaException;
        }

    }


}

package io.nordstrom.org.scaevents.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by bmwi on 4/3/18.
 */

@Service
public class SCAProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SCAProcessor.class);
    private static final String ROOT_LEVEL_CHANGED_NODES = "rootLevelChanges";
    private static final String SCA = "sca";
    public static final String STORE_NUMBER = "storeNumber";
    private static final String TIMESTAMP = "timeStamp";
    private static final String CURRENT_DATA = "currentData";


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

    public Map<String, Object> getSCAPayload(Map<String, Object> nodes) {
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
        return payload;

    }


}

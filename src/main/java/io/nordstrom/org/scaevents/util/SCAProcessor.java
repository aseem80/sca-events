package io.nordstrom.org.scaevents.util;

import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.Map;

/**
 * Created by bmwi on 4/3/18.
 */
public interface SCAProcessor {


    static final String ROOT_LEVEL_CHANGED_NODES = "rootLevelChanges";
    static final String SCA = "sca";
    static final String STORE_NUMBER = "storeNumber";
    static final String TIMESTAMP = "currentDataUpdatedTimeStamp";
    static final String CURRENT_DATA = "currentData";

    Map<String, Object> fromCanonicalPayload(String key, String payload);

    Pair<String, Boolean> isSCANodeChanged(Map<String, Object> nodes);

    String toSCAPayload(Map<String, Object> nodes);


}

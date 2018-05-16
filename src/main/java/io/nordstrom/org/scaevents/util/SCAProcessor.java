package io.nordstrom.org.scaevents.util;

import org.apache.commons.lang3.tuple.Pair;

import java.text.SimpleDateFormat;
import java.util.Map;

/**
 * Created by bmwi on 4/3/18.
 */
public interface SCAProcessor {


    static final String ROOT_LEVEL_CHANGED_NODES = "rootLevelChanges";
    static final String SCA = "sca";
    static final String STORE_NUMBER = "storeNumber";
    static final String TIMESTAMP = "currentDataUpdatedTimeStampMS";
    static final String CURRENT_DATA = "currentData";
    static final SimpleDateFormat scaSimpleDateFormat = new SimpleDateFormat("YYYY-mm-dd'T'HH:mm:ss.SSS'Z'");

    static final String SCA_TIMESTAMP_KAFKA_HEADER = "ValueUpdatedTime";


    Map<String, Object> fromCanonicalPayload(String key, String payload);

    Pair<String, Boolean> isSCANodeChanged(Map<String, Object> nodes);

    String toSCAPayload(Map<String, Object> nodes, Map<String, String> headers);


}

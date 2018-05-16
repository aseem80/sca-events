package io.nordstrom.org.scaevents.dao;


import java.text.SimpleDateFormat;

/**
 * Created by bmwi on 4/6/18.
 */
public interface PayloadDao {


    static final SimpleDateFormat sdf = new SimpleDateFormat( "YYYY-MM-dd");
    static final String KEY_SEPARATOR = "_";
    static final String PATH_SEPARATOR = "/";


    void save(String uuid, String payloadKey, Object payload);

    void saveAsync(String uuid, String payloadKey, Object string);

    void saveError(String uuid, String payloadKey, Object payload);



}

package io.nordstrom.org.scaevents.dao;

import io.nordstrom.org.scaevents.annotation.AsyncRetryable;
import org.springframework.retry.annotation.Backoff;
import java.io.IOException;

/**
 * Created by bmwi on 4/6/18.
 */
public interface PayloadDao {

    void save(String uuid, Object payload);


    void saveAsync(String uuid, Object string);

}

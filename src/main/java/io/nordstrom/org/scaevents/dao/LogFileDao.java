package io.nordstrom.org.scaevents.dao;

import io.nordstrom.org.scaevents.annotation.AsyncRetryable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.annotation.Backoff;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by bmwi on 4/6/18.
 */
@Component
@Profile({"default","dev"})
public class LogFileDao implements PayloadDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogFileDao.class);

    @Override
    public void save(String uuid, Object payload) {
        LOGGER.info("UUID : {},  Payload : {}", uuid, payload.toString());
    }

    @Override
    @Async
    @AsyncRetryable(value = { IOException.class },
            maxAttempts = 5,
            backoff = @Backoff(delay = 1000, multiplier = 2))
    public void saveAsync(String uuid, Object payload) {
        LOGGER.info("UUID : {},  Payload : {}", uuid, payload.toString());
    }
}

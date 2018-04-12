package io.nordstrom.org.scaevents.dao;

import io.nordstrom.org.scaevents.annotation.AsyncRetryable;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.annotation.Backoff;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * Created by bmwi on 4/6/18.
 */
@Component
@Profile({"default","dev"})
public class LogFileDao implements PayloadDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogFileDao.class);

    private static final String ERROR_KEY_PREFIX = "data/sca-events/error-events/event_";
    private static final String ERROR_KEY_SUFFIX= ".json";

    @Override
    public void save(String uuid, String payloadKey, Object payload) {
        LOGGER.info("UUID : {},  PayloadKey: {},  Payload : {}", uuid, payloadKey, payload.toString());
    }

    @Override
    @Async
    @AsyncRetryable(value = { IOException.class },
            maxAttempts = 5,
            backoff = @Backoff(delay = 1000, multiplier = 2))
    public void saveAsync(String uuid, String payloadKey, Object payload) {
        LOGGER.info("UUID : {},  Payload : {}", uuid, payloadKey, payload.toString());
    }

    @Override
    public void saveError(String uuid, String payloadKey, Object payload) {
        String scaEventsHome = System.getenv("ORG_SCA_EVENTS_HOME");
        String tmpDirectory = System.getProperty("java.io.tmpdir");

        ZonedDateTime utc = ZonedDateTime.now(ZoneOffset.UTC);
        Date date = Date.from(utc.toInstant());
        String datePart = sdf.format(date);

        StringBuilder keyPrefix = new StringBuilder(scaEventsHome!=null ? scaEventsHome : tmpDirectory).append(ERROR_KEY_PREFIX).append(datePart);
        String key = keyPrefix.append(PATH_SEPARATOR).append(uuid).append(KEY_SEPARATOR).append(payloadKey).append(KEY_SEPARATOR).append(date.getTime()).append(ERROR_KEY_SUFFIX).toString();

        Path path = Paths.get(key);
        LOGGER.info("Saving payload to LocalFIleSystem with path : {} ", key);

        Path parentDir = path.getParent();


        try {
            if (!Files.exists(parentDir))
                Files.createDirectories(parentDir);
            Files.write(path, payload.toString().getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        }catch(IOException e) {
            LOGGER.error("Error writing event file : " + ExceptionUtils.getStackTrace(e));
        }
    }
}

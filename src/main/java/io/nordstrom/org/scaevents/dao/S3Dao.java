package io.nordstrom.org.scaevents.dao;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import io.nordstrom.org.scaevents.annotation.AsyncRetryable;
import io.nordstrom.org.scaevents.dao.PayloadDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.annotation.Backoff;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * Created by bmwi on 4/6/18.
 */

@Component
@Profile({"prod","non-prod"})
public class S3Dao implements PayloadDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3Dao.class);


    private static final String CONTENT_TYPE = "application/json";
    private static final String KEY_PREFIX = "sca-events/payload/event_";
    private static final SimpleDateFormat sdf = new SimpleDateFormat( "YYYY-MM-dd");
    private static final String KEY_SEPARATOR = "_";
    private static final String PATH_SEPARATOR = "/";





    @Autowired
    private AmazonS3 s3Client;

    @Value("${scaevents.aws.s3.bucket}")
    private String bucketName;


    @Override
    public void save(String uuid, Object payload) {
        saveWithMetaData(uuid, payload);
    }

    @Override
    @Async
    @AsyncRetryable(value = { AmazonClientException.class },
            maxAttempts = 5,
            backoff = @Backoff(delay = 1000, multiplier = 2))
    public void saveAsync(String uuid, Object payload) {
        saveWithMetaData(uuid, payload);
    }

    private PutObjectResult saveWithMetaData(String uuid, Object payload) {
        LOGGER.info("Logging payload to S3 for UUID : " + uuid);
        byte[] bytes = payload.toString().getBytes();
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(CONTENT_TYPE);
        metadata.setContentEncoding(StandardCharsets.UTF_8.name());
        metadata.setContentLength(bytes.length);
        ZonedDateTime utc = ZonedDateTime.now(ZoneOffset.UTC);
        Date date = Date.from(utc.toInstant());
        String datePart = sdf.format(date);
        StringBuilder keyPrefix = new StringBuilder(KEY_PREFIX).append(datePart);
        String key = keyPrefix.append(PATH_SEPARATOR).append(uuid).append(KEY_SEPARATOR).append(utc.getNano()).toString();
        return s3Client.putObject(bucketName, key, new ByteArrayInputStream(bytes), metadata);
    }
}

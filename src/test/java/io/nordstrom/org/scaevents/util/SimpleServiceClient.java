package io.nordstrom.org.scaevents.util;

import io.nordstrom.org.scaevents.annotation.AsyncRetryable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.SocketTimeoutException;

/**
 * Created by bmwi on 4/10/18.
 */
@AsyncRetryable(maxAttempts=4,value={IOException.class},backoff = @Backoff(delay = 1000, multiplier=2))
@Component
public class SimpleServiceClient {

    private Integer id = null;

    private int failureCount = 1;


    public synchronized Integer getId() {
        return id;
    }

    @Async
    public synchronized void setId() throws SocketTimeoutException {
        if (failureCount <= 3) {
            failureCount++;
            //This should throw NullPointerException
            throw new SocketTimeoutException("Retryable IOException");
        } else {
            id = 0;
        }

    }

    public int getFailureCount() {
        return failureCount;
    }
}

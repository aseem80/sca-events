package io.nordstrom.org.scaevents.unit;

import io.nordstrom.org.scaevents.util.SimpleServiceClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.SocketTimeoutException;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;

/**
 * Created by bmwi on 4/10/18.
 */

@RunWith(SpringRunner.class)
@SpringBootTest
public class SimpleAsyncExceptionHandlerTest {

    @Autowired
    private SimpleServiceClient simpleServiceClient;


    @Test
    public void handleUncaughtException() throws InterruptedException, SocketTimeoutException {
        simpleServiceClient.setId();
        Thread.sleep(1000);
        Integer id = simpleServiceClient.getId();
        assertNull(id);
        Thread.sleep(4000);
        id = simpleServiceClient.getId();
        assertNotNull(id);
        assertEquals(4, simpleServiceClient.getFailureCount());
    }

}

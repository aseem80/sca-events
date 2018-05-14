package io.nordstrom.org.scaevents.producer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.nordstrom.org.scaevents.dao.PayloadDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by bmwi on 4/3/18.
 */

@Component
public class Sender {

    private static final Logger LOGGER = LoggerFactory.getLogger(Sender.class);

    private static final Map<String,String> MESSAGE_HEADERS = new LinkedHashMap<>();
    static {
        MESSAGE_HEADERS.put("SchemaVersion","1");
        MESSAGE_HEADERS.put("MessageMode","event");
        MESSAGE_HEADERS.put("MessageType","org");
    }


    @Value("${spring.kafka.producer.topic}")
    private String topic;

    @Autowired
    private PayloadDao payloadDao;

    @Autowired
    private KafkaTemplateWrapper wrapper;



    public void sendAsync(String payload, String key, Map<String, String> headers) {
        String uuid = UUID.randomUUID().toString();
        if(null!=payload) {
            LOGGER.info("sending message with TraceID='{}' to topic='{}'", uuid, topic);
            payloadDao.saveAsync(uuid, key, payload);
            headers.putAll(MESSAGE_HEADERS);
            wrapper.sendAsync(topic, uuid, payload, key, headers);
        } else {
            LOGGER.warn(" Event with Null Payload ");
        }
    }

    public void send(String payload, String key, Map<String, String> headers) throws Throwable {
        String uuid = UUID.randomUUID().toString();
        if(null!=payload) {
            LOGGER.info("sending message with TraceID='{}' to topic='{}'", uuid, topic);
            payloadDao.saveAsync(uuid, key, payload);
            headers.putAll(MESSAGE_HEADERS);
            wrapper.send(topic, uuid, payload, key, headers);
        } else {
            LOGGER.warn(" Event with Null Payload ");
        }
    }

}

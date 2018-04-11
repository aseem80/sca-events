package io.nordstrom.org.scaevents.producer;

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

    private static final Map<String,Object> MESSAGE_HEADERS = new LinkedHashMap<>();
    static {
        MESSAGE_HEADERS.put("SchemaVersion","1.0");
        MESSAGE_HEADERS.put("MessageMode","event");
        MESSAGE_HEADERS.put("MessageType","org");
    }




    @Value("${spring.kafka.producer.topic}")
    private String topic;

    @Autowired
    private PayloadDao payloadDao;

    @Autowired
    private KafkaTemplateWrapper wrapper;

    public void send(String payload, String key) {
        String uuid = UUID.randomUUID().toString();
        if(null!=payload) {
            LOGGER.info("sending message='{} TraceID='{}' to topic='{}'", uuid, topic);
            payloadDao.saveAsync(uuid.toString(), payload);
            wrapper.send(topic, uuid, payload, key, MESSAGE_HEADERS);
        } else {
            LOGGER.warn(" Event with Null Payload ");
        }
    }
}

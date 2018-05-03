package io.nordstrom.org.scaevents.consumer;

import io.nordstrom.org.scaevents.producer.Sender;
import io.nordstrom.org.scaevents.util.SCAProcessor;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bmwi on 4/3/18.
 */

@Component
public class Receiver {


    private static final Logger LOGGER = LoggerFactory.getLogger(Receiver.class);

    @Autowired
    private SCAProcessor scaProcessor;
    @Autowired
    private Sender sender;


    @KafkaListener(id = "canonical-batch-listener", topics = "${spring.kafka.consumer.topic}")
    public void receive(final List<Message<String>> messages)  {
        LOGGER.info("Started processing batch of {} messages", messages.size());
        messages.forEach(message -> {
            MessageHeaders headers = message.getHeaders();
            String receivedMessageKey = "";
            Object receivedMessageKeyHeaderValue = headers.get(KafkaHeaders.MESSAGE_KEY);
            if (receivedMessageKeyHeaderValue != null) {
                receivedMessageKey = receivedMessageKeyHeaderValue.toString();
            }
            String receivedPartitionId = "";
            Object receivedParitionIdHeaderValue = headers.get(KafkaHeaders.PARTITION_ID);
            if (receivedParitionIdHeaderValue != null) {
                receivedPartitionId = receivedParitionIdHeaderValue.toString();
            }

            String receivedOffset = "";
            Object receivedOffsetHeaderValue = headers.get(KafkaHeaders.OFFSET);
            if (receivedOffsetHeaderValue != null) {
                receivedOffset = receivedOffsetHeaderValue.toString();
            }
            LOGGER.info("received message key : {}, partition : {}, offset : {}  ", receivedMessageKey, receivedPartitionId, receivedOffset);

            consume(receivedMessageKey, message.getPayload());});
    }

    private void consume(String key, String payload) {
        Map<String, Object> map = scaProcessor.fromCanonicalPayload(key, payload);
        Pair<String, Boolean> pair = scaProcessor.isSCANodeChanged(map);
        LOGGER.info("SCA changed for store {} : {}", pair.getLeft(), pair.getRight());
        if (pair.getRight() != null && pair.getRight()) {
            Map<String, Object> headersMap = new HashMap<>();
            String payloadToSend = scaProcessor.toSCAPayload(map, headersMap);
            sender.sendAsync(payloadToSend, pair.getLeft(), headersMap);
        }


    }


}

package io.nordstrom.org.scaevents.consumer;


import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by bmwi on 4/3/18.
 */

@Component
public class Receiver {


    private static final Logger LOGGER = LoggerFactory.getLogger(Receiver.class);
    private final Counter receivedMessagesCounter;
    private final Counter scaProducerEligibleMessages;
    private final Timer scaStreamProcessorTimer;


    @Autowired
    private SCAProcessor scaProcessor;
    @Autowired
    private Sender sender;


    public Receiver (MeterRegistry registry) {
        this.receivedMessagesCounter = registry.counter("total.received.cannonical.messages");
        this.scaProducerEligibleMessages = registry.counter("total.sca.producer.messages");
        this.scaStreamProcessorTimer = Timer.builder("time.taken.to.proceess.single.message")
                    .publishPercentileHistogram()
                    .publishPercentiles(0.5, 0.75, 0.95, 0.99)
                    .sla(Duration.ofSeconds(10))
                    .register(registry);
    }


    @KafkaListener(id = "canonical-batch-listener", topics = "${spring.kafka.consumer.topic}")
    public void receive(final List<Message<String>> messages)  {
        int size = messages.size();
        LOGGER.info("Started processing batch of {} messages", size);
        this.receivedMessagesCounter.increment(size);
        messages.forEach(message -> {
            MessageHeaders headers = message.getHeaders();
            String receivedMessageKey = "";
            Object receivedMessageKeyHeaderValue = headers.get(KafkaHeaders.RECEIVED_MESSAGE_KEY);
            if (receivedMessageKeyHeaderValue != null) {
                receivedMessageKey = receivedMessageKeyHeaderValue.toString();
            }
            String receivedPartitionId = "";
            Object receivedParitionIdHeaderValue = headers.get(KafkaHeaders.RECEIVED_PARTITION_ID);
            if (receivedParitionIdHeaderValue != null) {
                receivedPartitionId = receivedParitionIdHeaderValue.toString();
            }

            String receivedOffset = "";
            Object receivedOffsetHeaderValue = headers.get(KafkaHeaders.OFFSET);
            if (receivedOffsetHeaderValue != null) {
                receivedOffset = receivedOffsetHeaderValue.toString();
            }
            LOGGER.info("Received message key(store) : {}, partition : {}, offset : {}  ", receivedMessageKey, receivedPartitionId, receivedOffset);
            consume(receivedMessageKey, message.getPayload());
        });
    }

    private void consume(String key, String payload) {
        scaStreamProcessorTimer.record(() -> {
            Map<String, Object> map = scaProcessor.fromCanonicalPayload(key, payload);
            Pair<String, Boolean> pair = scaProcessor.isSCANodeChanged(map);
            LOGGER.info("SCA changed for store {} : {}", pair.getLeft(), pair.getRight());
            if (pair.getRight() != null && pair.getRight()) {
                scaProducerEligibleMessages.increment();
                Map<String, String> headersMap = new HashMap<>();
                String payloadToSend = scaProcessor.toSCAPayload(map, headersMap);
                sender.sendAsync(payloadToSend, pair.getLeft(), headersMap);
            }
        });


    }


}

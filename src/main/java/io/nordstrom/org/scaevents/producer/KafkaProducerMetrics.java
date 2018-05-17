package io.nordstrom.org.scaevents.producer;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.lang.NonNullApi;
import io.micrometer.core.lang.NonNullFields;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;

import static java.util.Collections.emptyList;

@NonNullApi
@NonNullFields
@Configuration
@ConditionalOnClass(org.apache.kafka.clients.producer.KafkaProducer.class)
public class KafkaProducerMetrics implements MeterBinder {

    private final MBeanServer mBeanServer;

    private final Iterable<Tag> tags;

    @Value("${spring.kafka.producer.topic}")
    private String topic;

    public KafkaProducerMetrics() {
        this(getMBeanServer(), emptyList());
    }

    public KafkaProducerMetrics(Iterable<Tag> tags) {
        this(getMBeanServer(), tags);
    }

    public KafkaProducerMetrics(MBeanServer mBeanServer, Iterable<Tag> tags) {
        this.tags = tags;
        this.mBeanServer = mBeanServer;
    }

    public static MBeanServer getMBeanServer() {
        List<MBeanServer> mBeanServers = MBeanServerFactory.findMBeanServer(null);
        if (!mBeanServers.isEmpty()) {
            return mBeanServers.get(0);
        }
        return ManagementFactory.getPlatformMBeanServer();
    }

    @Override
    public void bindTo(MeterRegistry reg) {
        registerProducerMetrics(reg);
    }

    private void registerProducerMetrics(MeterRegistry registry) {

        registerMetricsEventually("type", "producer-metrics", (name, allTags) -> {


                    Gauge.builder("sca.kafka.producer.request.latency.avg", mBeanServer, s -> safeDouble(() -> s.getAttribute(name, "request-latency-avg")))
                            .description("Kafka Producer Request Latency average")
                            .tags(allTags)
                            .register(registry);

                    Gauge.builder("sca.kafka.producer.request.latency.max", mBeanServer, s -> safeDouble(() -> s.getAttribute(name, "request-latency-max")))
                            .description("Kafka Producer Request Latency max")
                            .tags(allTags)
                            .register(registry);

                    Gauge.builder("sca.kafka.producer.outgoing.byte.rate", mBeanServer, s -> safeDouble(() -> s.getAttribute(name, "outgoing-byte-rate")))
                            .description("Kafka Producer OutgoingByteRate")
                            .tags(allTags)
                            .baseUnit("bytes")
                            .register(registry);


                }
        );
    }


    private void registerMetricsEventually(String key, String value, BiConsumer<ObjectName, Iterable<Tag>> perObject) {
        try {
            Set<ObjectName> objs = mBeanServer.queryNames(new ObjectName("kafka.producer:" + key + "=" + value + ",*"), null);
            if (!objs.isEmpty()) {
                objs.forEach(o -> perObject.accept(o, Tags.concat(tags, nameTag(o))));
                return;
            }
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException("Error registering Kafka JMX based metrics", e);
        }

        NotificationListener notificationListener = (notification, handback) -> {
            MBeanServerNotification mbs = (MBeanServerNotification) notification;
            ObjectName obj = mbs.getMBeanName();
            perObject.accept(obj, Tags.concat(tags, nameTag(obj)));
        };

        NotificationFilter filter = (NotificationFilter) notification -> {
            if (!MBeanServerNotification.REGISTRATION_NOTIFICATION.equals(notification.getType()))
                return false;
            ObjectName obj = ((MBeanServerNotification) notification).getMBeanName();
            return obj.getDomain().equals("kafka.producer") && obj.getKeyProperty(key).equals(value);
        };

        try {
            mBeanServer.addNotificationListener(MBeanServerDelegate.DELEGATE_NAME, notificationListener, filter, null);
        } catch (InstanceNotFoundException e) {
            throw new RuntimeException("Error registering Kafka MBean listener", e);
        }
    }

    private double safeDouble(Callable<Object> callable) {
        try {
            return Double.parseDouble(callable.call().toString());
        } catch (Exception e) {
            return 0.0;
        }
    }


    private Iterable<Tag> nameTag(ObjectName name) {
        if (name.getKeyProperty("client-id") != null) {
            return Tags.of("producer", name.getKeyProperty("client-id"),
                    "topic", (null != name.getKeyProperty("topic") ? name.getKeyProperty("topic") : topic));
        } else {
            return emptyList();
        }
    }
}

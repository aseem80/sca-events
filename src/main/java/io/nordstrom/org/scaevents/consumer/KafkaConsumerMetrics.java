package io.nordstrom.org.scaevents.consumer;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.lang.NonNullApi;
import io.micrometer.core.lang.NonNullFields;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerFactory;
import javax.management.MBeanServerNotification;
import javax.management.MalformedObjectNameException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;

import static java.util.Collections.emptyList;

@NonNullApi
@NonNullFields
@Configuration
@ConditionalOnClass(org.apache.kafka.clients.consumer.KafkaConsumer.class)
public class KafkaConsumerMetrics implements MeterBinder {

    private final MBeanServer mBeanServer;

    private final Iterable<Tag> tags;

    @Value("${spring.kafka.consumer.topic}")
    private String topic;

    public KafkaConsumerMetrics() {
        this(getMBeanServer(), emptyList());
    }

    public KafkaConsumerMetrics(Iterable<Tag> tags) {
        this(getMBeanServer(), tags);
    }

    public KafkaConsumerMetrics(MBeanServer mBeanServer, Iterable<Tag> tags) {
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
        registerConsumerFetchMetrics(reg);
        registerConsumerCoordinatorMetrics(reg);
    }

    private void registerConsumerFetchMetrics(MeterRegistry registry) {

        registerMetricsEventually("type", "consumer-fetch-manager-metrics", (name, allTags) -> {

                    FunctionCounter.builder("kafka.consumer.records.lag.max", mBeanServer,
                            s -> safeDouble(() -> s.getAttribute(name, "records-lag-max")))
                            .tags(allTags)
                            .register(registry);

                    FunctionCounter.builder("kafka.consumer.fetch.latency.avg", mBeanServer,
                            s -> safeDouble(() -> s.getAttribute(name, "fetch-latency-avg")))
                            .tags(allTags)
                            .register(registry);

                    FunctionCounter.builder("kafka.consumer.bytes.consumed.rate", mBeanServer,
                            s -> safeDouble(() -> s.getAttribute(name, "bytes-consumed-rate")))
                            .tags(allTags)
                            .baseUnit("bytes")
                            .register(registry);

                    FunctionCounter.builder("kafka.consumer.fetch.size.max", mBeanServer,
                            s -> safeDouble(() -> s.getAttribute(name, "fetch-size-max")))
                            .tags(allTags)
                            .baseUnit("bytes")
                            .register(registry);

                    FunctionCounter.builder("kafka.consumer.records.consumed.rate", mBeanServer,
                            s -> safeDouble(() -> s.getAttribute(name, "records-consumed-rate")))
                            .tags(allTags)
                            .register(registry);
                }
        );
    }

    private void registerConsumerCoordinatorMetrics(MeterRegistry registry) {

        registerMetricsEventually("type", "consumer-coordinator-metrics", (name, allTags) -> {

                    FunctionCounter.builder("kafka.consumer.assigned.partitions", mBeanServer,
                            s -> safeDouble(() -> s.getAttribute(name, "assigned-partitions")))
                            .tags(allTags)
                            .register(registry);

                    FunctionCounter.builder("kafka.consumer.commit.latency.avg", mBeanServer,
                            s -> safeDouble(() -> s.getAttribute(name, "commit-latency-avg")))
                            .tags(allTags)
                            .register(registry);

                    FunctionCounter.builder("kafka.consumer.commit.latency.max", mBeanServer,
                            s -> safeDouble(() -> s.getAttribute(name, "commit-latency-max")))
                            .tags(allTags)
                            .register(registry);

                    FunctionCounter.builder("kafka.consumer.commit.rate", mBeanServer,
                            s -> safeDouble(() -> s.getAttribute(name, "commit-rate")))
                            .tags(allTags)
                            .register(registry);
                }
        );
    }

    private void registerMetricsEventually(String key, String value, BiConsumer<ObjectName, Iterable<Tag>> perObject) {
        try {
            Set<ObjectName> objs = mBeanServer.queryNames(new ObjectName("kafka.consumer:" + key + "=" + value + ",*"), null);
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
            return obj.getDomain().equals("kafka.consumer") && obj.getKeyProperty(key).equals(value);
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
            return Tags.of("consumer", name.getKeyProperty("client-id"),
                    "topic", (null != name.getKeyProperty("topic") ? name.getKeyProperty("topic") : topic));
        } else {
            return emptyList();
        }
    }
}

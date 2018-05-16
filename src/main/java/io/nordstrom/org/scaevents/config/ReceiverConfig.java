package io.nordstrom.org.scaevents.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.nordstrom.org.scaevents.util.PropertiesUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.BatchLoggingErrorHandler;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

import javax.annotation.PostConstruct;
import java.util.*;


/**
 * Created by bmwi on 4/2/18.
 */

@Configuration
@EnableKafka
public class ReceiverConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReceiverConfig.class);

    @Autowired
    private MeterRegistry registry;

    @Value("${spring.kafka.consumer.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:proton-og-consumer}")
    private String consumerGroupId;

    @Value("${spring.kafka.consumer.ssl.enabled}")
    private boolean isSSLEnabled;

    @Value("${spring.kafka.consumer.security.protocol:SSL}")
    private String securityProtocol;
    @Value("${spring.kafka.consumer.ssl.truststore.location}")
    private String sslTruststoreLocation;
    @Value("${spring.kafka.consumer.ssl.truststore.password}")
    private String sslTruststorePassword;
    @Value("${spring.kafka.consumer.ssl.truststore.type:JKS}")
    private String sslTruststoreType;
    @Value("${spring.kafka.consumer.ssl.endpoint.identification.algorithm}")
    private String sslEndPointIdentificationAlgorithm;


    @Value("${spring.kafka.consumer.ssl.keystore.location}")
    private String sslKeyStoreLocation;
    @Value("${spring.kafka.consumer.ssl.keystore.password}")
    private String sslKeystorePassword;
    @Value("${spring.kafka.consumer.ssl.key.password}")
    private String sslKeyPassword;
    @Value("${spring.kafka.consumer.ssl.keystore.type:JKS}")
    private String sslKeystoreType;
    @Value("${scaevents.consumer.auto.offset.reset:latest}")
    private String autoReset;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaRegistry;


    @Bean
    public Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        // list of host:port pairs used for establishing the initial connections to the Kafka cluster
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // allows a pool of processes to divide the work of consuming and processing records
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoReset);
        if(isSSLEnabled || StringUtils.contains(bootstrapServers, PropertiesUtil.PROTON_URL_TEXT)) {
            setSSL(props);
        }

        return props;
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerConfigs());
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setBatchListener(true);
        factory.getContainerProperties().setBatchErrorHandler(new BatchLoggingErrorHandler());
        return factory;
    }


    @Bean
    public ObjectMapper mapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }


    private void setSSL(Map<String, Object> props) {
        props.put(PropertiesUtil.SECURITY_PROTOCOL, securityProtocol);
        props.put(PropertiesUtil.SSL_TRUSTSTORE_LOCATION, sslTruststoreLocation);
        props.put(PropertiesUtil.SSL_TRUSTSTORE_PASSWORD, sslTruststorePassword);
        props.put(PropertiesUtil.SSL_TRUSTSTORE_TYPE, sslTruststoreType);
        props.put(PropertiesUtil.SSL_END_POINT_IDENTIFICATION_ALGORITHM, sslEndPointIdentificationAlgorithm);

        props.put(PropertiesUtil.SSL_KEYSTORE_LOCATION, sslKeyStoreLocation);
        props.put(PropertiesUtil.SSL_KEYSTORE_PASSWORD, sslKeystorePassword);
        props.put(PropertiesUtil.SSL_KEYSTORE_TYPE, sslKeystoreType);
        props.put(PropertiesUtil.SSL_KEY_PASSWORD, sslKeyPassword);

    }



}

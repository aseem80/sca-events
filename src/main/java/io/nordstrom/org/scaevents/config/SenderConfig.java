package io.nordstrom.org.scaevents.config;

import io.nordstrom.org.scaevents.producer.Sender;
import io.nordstrom.org.scaevents.util.ProtonUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by bmwi on 4/3/18.
 */

@Configuration
public class SenderConfig {

    @Value("${spring.kafka.producer.bootstrap-servers}")
    private String bootstrapServers;


    @Value("${spring.kafka.producer.ssl.enabled}")
    private boolean isSSLEnabled;

    @Value("${spring.kafka.producer.security.protocol:SSL}")
    private String securityProtocol;
    @Value("${spring.kafka.producer.ssl.truststore.location}")
    private String sslTruststoreLocation;
    @Value("${spring.kafka.producer.ssl.truststore.password}")
    private String sslTruststorePassword;
    @Value("${spring.kafka.producer.ssl.truststore.type:JKS}")
    private String sslTruststoreType;
    @Value("${spring.kafka.producer.ssl.endpoint.identification.algorithm}")
    private String sslEndPointIdentificationAlgorithm;


    @Value("${spring.kafka.producer.ssl.keystore.location}")
    private String sslKeyStoreLocation;
    @Value("${spring.kafka.producer.ssl.keystore.password}")
    private String sslKeystorePassword;
    @Value("${spring.kafka.producer.ssl.key.password}")
    private String sslKeyPassword;
    @Value("${spring.kafka.producer.ssl.keystore.type:JKS}")
    private String sslKeystoreType;

    @Bean
    public Map<String, Object> producerConfigs() {
        Map<String, Object> props = new HashMap<>();
        // list of host:port pairs used for establishing the initial connections to the Kakfa cluster
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);

        if(isSSLEnabled || StringUtils.contains(bootstrapServers, ProtonUtil.PROTON_URL_TEXT)) {
            setSSL(props);
        }

        return props;
    }

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    private void setSSL(Map<String, Object> props) {
        props.put(ProtonUtil.SECURITY_PROTOCOL, securityProtocol);
        props.put(ProtonUtil.SSL_TRUSTSTORE_LOCATION, sslTruststoreLocation);
        props.put(ProtonUtil.SSL_TRUSTSTORE_PASSWORD, sslTruststorePassword);
        props.put(ProtonUtil.SSL_TRUSTSTORE_TYPE, sslTruststoreType);
        props.put(ProtonUtil.SSL_END_POINT_IDENTIFICATION_ALGORITHM, sslEndPointIdentificationAlgorithm);

        props.put(ProtonUtil.SSL_KEYSTORE_LOCATION, sslKeyStoreLocation);
        props.put(ProtonUtil.SSL_KEYSTORE_PASSWORD, sslKeystorePassword);
        props.put(ProtonUtil.SSL_KEYSTORE_TYPE, sslKeystoreType);
        props.put(ProtonUtil.SSL_KEY_PASSWORD, sslKeyPassword);

    }
}

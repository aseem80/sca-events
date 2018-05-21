package io.nordstrom.org.scaevents.config;

import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.datadog.DatadogNamingConvention;
import io.nordstrom.org.scaevents.exception.SimpleAsyncExceptionHandler;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import static io.nordstrom.org.scaevents.util.PropertiesUtil.*;

/**
 * Created by bmwi on 4/6/18.
 */

@Configuration
@EnableRetry
@EnableAsync
@ComponentScan("io.nordstrom.org.scaevents")
@EnableEncryptableProperties
public class OrgKafkaAppConfig implements AsyncConfigurer {

    @Autowired
    private ApplicationContext appContext;

    @Value("${sca.payload-log.threadPool.corepool:50}")
    private int corepool;
    @Value("${sca.payload-log.threadPool.maxpool:100}")
    private int maxpool;
    @Value("${sca.payload-log.threadPool.queuecapacity:2000}")
    private int queuecapacity;
    @Value("${scaevents.aws.infra.mode}")
    private boolean awsInfraMode;
    @Value("${scaevents.aws.s3.profile}")
    private String awsProfile;
    @Value("${spring.profiles.active}")
    private String environmentMetricsTag;


    @Bean
    public AWSCredentialsProviderChain awsCredentialsProviderChain() {
        if (awsInfraMode) {
            return DefaultAWSCredentialsProviderChain.getInstance();
        } else {
            return new AWSCredentialsProviderChain(new ProfileCredentialsProvider(awsProfile));
        }
    }

    @Bean
    public AmazonS3 s3Client() {
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withCredentials(awsCredentialsProviderChain()).withRegion(Regions.US_WEST_2).build();
        return s3Client;
    }

    @Bean(name = "s3-upload-executor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corepool);
        executor.setMaxPoolSize(maxpool);
        executor.setQueueCapacity(queuecapacity);
        executor.setThreadNamePrefix("PayloadLog-Executor");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncExceptionHandler(appContext);
    }



    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> {registry.config().commonTags(DATADOG_METRICS_TAG_KEY, environmentMetricsTag, DATADOG_METRICS_TEAM_TAG_KEY, DATADOG_METRICS_TEAM_NAME, DATADOG_METRICS_APPLICATION_NAME_TAG_KEY, DATADOG_METRICS_APPLICATION_NAME);
            registry.config().namingConvention(new DatadogNamingConvention());
        };
    }

    @Bean("jasyptStringEncryptor")
    public StringEncryptor stringEncryptor() {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword("orgencrytionpa$$word");
        config.setAlgorithm("PBEWithMD5AndDES");
        config.setKeyObtentionIterations("1000");
        config.setPoolSize("1");
        config.setProviderName("SunJCE");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        config.setStringOutputType("base64");
        encryptor.setConfig(config);
        return encryptor;
    }





}

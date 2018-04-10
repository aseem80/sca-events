package io.nordstrom.org.scaevents.config;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import io.nordstrom.org.scaevents.exception.SimpleAsyncExceptionHandler;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

/**
 * Created by bmwi on 4/6/18.
 */

@Configuration
@EnableRetry
@EnableAsync
@ComponentScan("io.nordstrom.org.scaevents")
public class OrgKafkaAppConfig implements AsyncConfigurer{

    @Autowired
    private ApplicationContext appContext;

    @Value("${sca.payload-log.threadPool.corepool:50}")
    private int corepool;
    @Value("${sca.payload-log.threadPool.maxpool:100}")
    private int maxpool;
    @Value("${sca.payload-log.threadPool.queuecapacity:2000}")
    private int queuecapacity;


    @Bean
    public AmazonS3 s3Client() {
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withCredentials(DefaultAWSCredentialsProviderChain.getInstance()).withRegion(Regions.US_WEST_2).build();
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
}

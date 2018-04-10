package io.nordstrom.org.scaevents;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Created by bmwi on 4/2/18.
 */

@SpringBootApplication
@EnableAsync
public class OrgKafkaApp {

    public static void main(String[] args) {
        SpringApplication.run(OrgKafkaApp.class, args);
    }
}

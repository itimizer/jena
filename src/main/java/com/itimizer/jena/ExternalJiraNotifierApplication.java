package com.itimizer.jena;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaAuditing
@EnableScheduling
@SpringBootApplication
public class ExternalJiraNotifierApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExternalJiraNotifierApplication.class, args);
    }
}

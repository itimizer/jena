package com.itimizer.jena;

import com.itimizer.jena.config.ContainersConfig;
import org.springframework.boot.SpringApplication;

class ExternalJiraNotifierApplicationTests {

    public static void main(String[] args) {
        SpringApplication
                .from(ExternalJiraNotifierApplication::main)
                .with(ContainersConfig.class)
                .run(args);
    }
}
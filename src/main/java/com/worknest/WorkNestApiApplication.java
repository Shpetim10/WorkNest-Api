package com.worknest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan(basePackages = "com.worknest")
public class WorkNestApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkNestApiApplication.class, args);
    }
}

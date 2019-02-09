package com.flower.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
//@EnableScheduling
public class SpringIntegrationApp {

    public static void main(String[] args) {
        SpringApplication.run(SpringIntegrationApp.class, args);
        System.out.println("Spring-Integration application start success.");
    }

}


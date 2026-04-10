package com.trianz.ltr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * LandTitleRegistryApplication - Spring Boot main application class.
 * 
 * Cloud-native improvements:
 * - Replaced EJB/WAR deployment with Spring Boot executable JAR
 * - Embedded Tomcat server (no external application server required)
 * - Compatible with AWS ECS, EKS, Fargate, and containerized deployments
 * - Supports Docker containerization
 * - Graceful shutdown for cloud orchestration
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.trianz.ltr")
public class LandTitleRegistryApplication {

    public static void main(String[] args) {
        SpringApplication.run(LandTitleRegistryApplication.class, args);
    }
}

package com.trianz.ltr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * LandTitleRegistryApplication - Spring Boot main application class.
 *
 * CLOUD-NATIVE DEPLOYMENT:
 *   - Executable JAR with embedded Tomcat (replaces WAR deployment)
 *   - Containerized deployment to AWS ECS, EKS, or Fargate
 *   - Stateless horizontal scaling with Redis session management
 *   - Health checks via Spring Actuator for AWS load balancers
 *
 * AWS INTEGRATION:
 *   - Database credentials from AWS Secrets Manager
 *   - Connection pooling via HikariCP to AWS RDS/Aurora
 *   - Session state in Amazon ElastiCache (Redis)
 *   - Logging to CloudWatch via structured JSON format
 *
 * MIGRATION FROM EJB:
 *   - Replaced EJB 2.x with Spring Boot microservices
 *   - Replaced WebSphere CMT with Spring @Transactional
 *   - Replaced JNDI lookups with Spring dependency injection
 *   - Replaced RMI-IIOP with REST APIs
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.trianz.ltr")
public class LandTitleRegistryApplication {

    public static void main(String[] args) {
        // Set UTC as default timezone for all date/time operations
        System.setProperty("user.timezone", "UTC");
        
        SpringApplication.run(LandTitleRegistryApplication.class, args);
    }
}

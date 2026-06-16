package com.trianz.ltr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * LandTitleRegistryApplication - Spring Boot main application class.
 * 
 * CLOUD-NATIVE MIGRATION:
 *   - Replaces IBM WebSphere EAR deployment with Spring Boot executable JAR
 *   - Embedded Tomcat server (no external application server required)
 *   - Auto-configuration for AWS cloud deployment
 *   - Supports containerization (Docker, ECS, EKS, Fargate)
 *   - Amazon ElastiCache (Redis) for distributed session management
 *   - Stateless architecture for horizontal scaling
 * 
 * DEPLOYMENT:
 *   Local: java -jar ltr-web-1.0.0.jar
 *   AWS ECS/Fargate: Deploy as container with environment variables
 *   AWS Elastic Beanstalk: Upload JAR directly
 * 
 * ENVIRONMENT VARIABLES:
 *   - DB_SECRET_NAME: AWS Secrets Manager secret name (default: ltr/db/credentials)
 *   - DB_JDBC_URL: Database JDBC URL (optional, can be in secret)
 *   - AWS_REGION: AWS region (default: us-east-1)
 *   - SERVER_PORT: HTTP port (default: 8080)
 *   - REDIS_HOST: Amazon ElastiCache Redis endpoint
 *   - REDIS_PORT: Redis port (default: 6379)
 *   - REDIS_PASSWORD: Redis authentication password (if enabled)
 */
@SpringBootApplication
@EnableTransactionManagement
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800)
@ServletComponentScan(basePackages = "com.trianz.ltr.servlet")
@ComponentScan(basePackages = {"com.trianz.ltr"})
public class LandTitleRegistryApplication {

    public static void main(String[] args) {
        SpringApplication.run(LandTitleRegistryApplication.class, args);
    }
}

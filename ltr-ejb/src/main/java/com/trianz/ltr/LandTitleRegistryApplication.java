package com.trianz.ltr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * LandTitleRegistryApplication - Spring Boot main application class.
 *
 * CLOUD-NATIVE FEATURES:
 *   - Spring Boot auto-configuration
 *   - Redis session management for distributed sessions (AWS ElastiCache)
 *   - Transaction management with HikariCP and AWS RDS
 *   - AWS Secrets Manager integration for credentials
 *   - Stateless design for horizontal scaling in EKS/ECS
 *
 * CONFIGURATION:
 *   Environment variables (set in ECS task definition or EKS deployment):
 *     - DB_HOST: RDS endpoint
 *     - DB_PORT: Database port (default: 5432)
 *     - DB_NAME: Database name
 *     - DB_SECRET_NAME: AWS Secrets Manager secret name
 *     - AWS_REGION: AWS region
 *     - REDIS_HOST: ElastiCache Redis endpoint
 *     - REDIS_PORT: Redis port (default: 6379)
 *     - SERVER_PORT: Application port (default: 8080)
 */
@SpringBootApplication
@EnableTransactionManagement
@EnableRedisHttpSession
public class LandTitleRegistryApplication {

    public static void main(String[] args) {
        SpringApplication.run(LandTitleRegistryApplication.class, args);
    }
}

package com.trianz.ltr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

/**
 * LandTitleWebApplication - Spring Boot application entry point for the
 * Land Title Registry web module.
 *
 * CLOUD-NATIVE FEATURES:
 *   - Executable JAR with embedded Tomcat (no external application server)
 *   - Distributed session management via Amazon ElastiCache (Redis)
 *   - Spring Boot Actuator for health checks and metrics
 *   - Deployable to AWS ECS, EKS, or Fargate
 *   - Horizontal scaling with stateless architecture
 *
 * CONFIGURATION:
 *   - Redis endpoint: ${REDIS_HOST:localhost}:${REDIS_PORT:6379}
 *   - Session timeout: ${SESSION_TIMEOUT:1800} seconds (30 minutes)
 *   - Server port: ${SERVER_PORT:8080}
 *
 * AWS DEPLOYMENT:
 *   - Set REDIS_HOST to ElastiCache cluster endpoint
 *   - Set REDIS_PORT to ElastiCache port (default 6379)
 *   - Configure security groups for Redis access
 *   - Use AWS Secrets Manager for Redis credentials if AUTH enabled
 *
 * MIGRATION FROM WEBSPHERE:
 *   - Replaces WAS session replication with Redis-backed sessions
 *   - Eliminates dependency on WAS clustering features
 *   - Enables cloud-native horizontal scaling
 */
@SpringBootApplication
@ServletComponentScan(basePackages = "com.trianz.ltr.servlet")
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800)
public class LandTitleWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(LandTitleWebApplication.class, args);
    }

    /**
     * Redis connection factory for Amazon ElastiCache.
     * Configured via environment variables for cloud deployment.
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        String redisHost = System.getenv().getOrDefault("REDIS_HOST", "localhost");
        int redisPort = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));
        
        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisHost, redisPort);
        
        // Optional: Configure Redis password from AWS Secrets Manager
        String redisPassword = System.getenv("REDIS_PASSWORD");
        if (redisPassword != null && !redisPassword.isEmpty()) {
            factory.setPassword(redisPassword);
        }
        
        return factory;
    }
}

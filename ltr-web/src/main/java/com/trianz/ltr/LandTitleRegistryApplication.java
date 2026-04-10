package com.trianz.ltr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Clock;

/**
 * Land Title Registry Application - Cloud-Native Spring Boot Application
 * 
 * Migrated from IBM WebSphere EJB 2.x to Spring Boot for AWS deployment.
 * 
 * Cloud-Ready Features:
 * - Executable JAR with embedded Tomcat (no external app server required)
 * - Externalized configuration via environment variables
 * - AWS Secrets Manager integration for credentials
 * - HikariCP connection pooling for AWS RDS
 * - Spring Boot Actuator for health checks and metrics
 * - Structured JSON logging for CloudWatch
 * - Stateless architecture for horizontal scaling
 * - UTC timezone standardization
 * 
 * AWS Deployment:
 * - Amazon ECS/EKS: Deploy as Docker container
 * - AWS Elastic Beanstalk: Deploy JAR directly
 * - AWS Lambda: Use with Spring Cloud Function adapter
 * 
 * Environment Variables Required:
 * - DB_HOST: RDS endpoint
 * - DB_PORT: RDS port (default: 5432)
 * - DB_NAME: Database name
 * - DB_SECRET_ARN: AWS Secrets Manager ARN for DB credentials
 * - AWS_REGION: AWS region for Secrets Manager
 * 
 * @author Cloud Migration Team
 * @version 2.0.0-cloud
 */
@SpringBootApplication
@EnableAsync
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class LandTitleRegistryApplication {

    public static void main(String[] args) {
        // Set UTC as default timezone for cloud consistency
        System.setProperty("user.timezone", "UTC");
        
        SpringApplication.run(LandTitleRegistryApplication.class, args);
    }

    /**
     * Provide Clock bean for testable time operations
     * Replaces java.util.Date with java.time API
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * Configure CORS for cloud-native API access
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("${cors.allowed-origins:*}")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(false)
                        .maxAge(3600);
            }
        };
    }

    /**
     * Security configuration for cloud deployment
     * Replaces WAS JAAS/LDAP with Spring Security
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable() // Disable for stateless REST API
            .authorizeRequests()
                .antMatchers("/actuator/health", "/actuator/info").permitAll()
                .antMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            .and()
            .httpBasic(); // Replace with JWT or OAuth2 for production
        
        return http.build();
    }
}

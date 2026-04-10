package com.trianz.ltr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.logging.Logger;

/**
 * CloudNativeApplication - Spring Boot main application class.
 *
 * CLOUD-READY FEATURES:
 *   - Spring Boot for embedded Tomcat (no external app server)
 *   - Auto-configuration for cloud deployment
 *   - Executable JAR packaging
 *   - Compatible with AWS ECS, EKS, Elastic Beanstalk
 *   - Environment variable configuration (12-factor app)
 *
 * DEPLOYMENT:
 *   - AWS ECS/Fargate: Docker container with this JAR
 *   - AWS EKS: Kubernetes pod with this JAR
 *   - AWS Elastic Beanstalk: Direct JAR deployment
 *   - AWS Lambda: With Spring Cloud Function adapter
 */
@SpringBootApplication
@ServletComponentScan
@EnableTransactionManagement
public class CloudNativeApplication {

    private static final Logger LOGGER = Logger.getLogger(CloudNativeApplication.class.getName());

    public static void main(String[] args) {
        LOGGER.info("Starting Land Title Registry - Cloud Native Application");
        LOGGER.info("Environment: " + getEnvironment());
        LOGGER.info("AWS Region: " + getAwsRegion());
        
        SpringApplication.run(CloudNativeApplication.class, args);
        
        LOGGER.info("Application started successfully");
    }

    /**
     * Get environment from environment variable.
     * Cloud-ready: AWS ECS/EKS task definition sets this.
     */
    private static String getEnvironment() {
        String env = System.getenv("ENVIRONMENT");
        return (env != null && !env.isEmpty()) ? env : "development";
    }

    /**
     * Get AWS region from environment variable.
     * Cloud-ready: AWS automatically sets AWS_REGION in ECS/Lambda.
     */
    private static String getAwsRegion() {
        String region = System.getenv("AWS_REGION");
        return (region != null && !region.isEmpty()) ? region : "us-east-1";
    }

    /**
     * Graceful shutdown hook for cloud environments.
     */
    @Bean
    public GracefulShutdownHook gracefulShutdownHook() {
        return new GracefulShutdownHook();
    }

    /**
     * Graceful shutdown hook to close resources properly.
     */
    static class GracefulShutdownHook {
        public GracefulShutdownHook() {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOGGER.info("Shutting down gracefully...");
                // Close connection pools, thread pools, etc.
                com.trianz.ltr.util.CloudDataSourceUtil.shutdown();
                com.trianz.ltr.dao.LandTitleDAO.shutdown();
                com.trianz.ltr.dao.TitleTransferDAO.shutdown();
                LOGGER.info("Shutdown complete");
            }));
        }
    }
}

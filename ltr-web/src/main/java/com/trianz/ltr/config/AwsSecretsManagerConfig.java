package com.trianz.ltr.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import javax.annotation.PostConstruct;

/**
 * AWS Secrets Manager Configuration
 *
 * Replaces hard-coded database credentials with AWS Secrets Manager.
 * Loads database credentials securely from AWS Secrets Manager at startup.
 *
 * Cloud-Ready Features:
 * - AWS Secrets Manager integration for credential management
 * - Automatic secret rotation support
 * - IAM role-based authentication (no access keys in code)
 * - Encrypted credential storage
 * - Audit logging via CloudTrail
 *
 * AWS Setup:
 * 1. Create secret in AWS Secrets Manager:
 *    aws secretsmanager create-secret \
 *      --name landtitle/db/credentials \
 *      --secret-string '{"username":"dbuser","password":"dbpass"}'
 *
 * 2. Grant IAM role permission:
 *    {
 *      "Effect": "Allow",
 *      "Action": "secretsmanager:GetSecretValue",
 *      "Resource": "arn:aws:secretsmanager:region:account:secret:landtitle/db/credentials"
 *    }
 *
 * 3. Set environment variable:
 *    DB_SECRET_ARN=arn:aws:secretsmanager:region:account:secret:landtitle/db/credentials
 *
 * @author Cloud Migration Team
 * @version 2.0.0-cloud
 */
@Configuration
@ConditionalOnProperty(name = "aws.secretsmanager.enabled", havingValue = "true")
public class AwsSecretsManagerConfig {

    private static final Logger logger = LoggerFactory.getLogger(AwsSecretsManagerConfig.class);

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Value("${aws.secretsmanager.secret-name}")
    private String secretName;

    private String dbUsername;
    private String dbPassword;

    /**
     * Initialize AWS Secrets Manager client
     */
    @Bean
    public SecretsManagerClient secretsManagerClient() {
        logger.info("Initializing AWS Secrets Manager client for region: {}", awsRegion);
        return SecretsManagerClient.builder()
                .region(Region.of(awsRegion))
                .build();
    }

    /**
     * Load database credentials from AWS Secrets Manager at startup
     * Replaces hard-coded credentials in WASTransactionUtil
     */
    @PostConstruct
    public void loadDatabaseCredentials() {
        if (secretName == null || secretName.isEmpty()) {
            logger.warn("AWS Secrets Manager secret name not configured, skipping credential loading");
            return;
        }

        try {
            logger.info("Loading database credentials from AWS Secrets Manager: {}", secretName);
            
            SecretsManagerClient client = secretsManagerClient();
            
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(secretName)
                    .build();
            
            GetSecretValueResponse response = client.getSecretValue(request);
            String secretString = response.secretString();
            
            // Parse JSON secret
            ObjectMapper mapper = new ObjectMapper();
            JsonNode secretJson = mapper.readTree(secretString);
            
            dbUsername = secretJson.get("username").asText();
            dbPassword = secretJson.get("password").asText();
            
            // Set system properties for Spring DataSource
            System.setProperty("DB_USERNAME", dbUsername);
            System.setProperty("DB_PASSWORD", dbPassword);
            
            logger.info("Successfully loaded database credentials from AWS Secrets Manager");
            
        } catch (Exception e) {
            logger.error("Failed to load database credentials from AWS Secrets Manager", e);
            throw new RuntimeException("Failed to load database credentials from AWS Secrets Manager", e);
        }
    }

    public String getDbUsername() {
        return dbUsername;
    }

    public String getDbPassword() {
        return dbPassword;
    }
}

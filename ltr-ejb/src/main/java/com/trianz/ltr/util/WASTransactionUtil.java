package com.trianz.ltr.util;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CloudTransactionUtil - Cloud-native transaction management helper.
 *
 * CLOUD-NATIVE REPLACEMENT:
 *   - Replaced IBM WebSphere UOWManager with Spring PlatformTransactionManager
 *   - Replaced WAS JNDI lookups with Spring dependency injection
 *   - Added AWS Secrets Manager integration for secure credential management
 *   - Removed WAS-specific APIs (com.ibm.websphere.uow, com.ibm.wsspi.uow)
 *
 * AWS INTEGRATION:
 *   - Database credentials retrieved from AWS Secrets Manager
 *   - Supports automatic credential rotation without redeployment
 *   - Credentials never stored in source code or configuration files
 *
 * USAGE:
 *   - Inject PlatformTransactionManager via Spring @Autowired
 *   - Use @Transactional annotation on service methods (preferred)
 *   - Use programmatic transactions only when dynamic control needed
 */
public class WASTransactionUtil {

    private static final Logger LOGGER = Logger.getLogger(WASTransactionUtil.class.getName());

    /** AWS region for Secrets Manager - configurable via environment variable */
    private static final String AWS_REGION = System.getenv().getOrDefault("AWS_REGION", "us-east-1");
    
    /** Secret name in AWS Secrets Manager containing database credentials */
    private static final String DB_SECRET_NAME = System.getenv().getOrDefault("DB_SECRET_NAME", "ltr/database/credentials");

    private WASTransactionUtil() { /* utility */ }

    /**
     * Execute a unit of work using Spring's transaction management.
     * Provides ACID transaction boundaries compatible with AWS RDS.
     *
     * @param transactionManager Spring transaction manager (injected)
     * @param action  the transactional work to perform
     * @param requiresNew  if true, always starts a new transaction (REQUIRES_NEW semantics)
     */
    public static void executeInTransaction(PlatformTransactionManager transactionManager,
                                           TransactionCallback action, 
                                           boolean requiresNew) throws Exception {

        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(requiresNew 
                ? TransactionDefinition.PROPAGATION_REQUIRES_NEW 
                : TransactionDefinition.PROPAGATION_REQUIRED);

        TransactionStatus status = transactionManager.getTransaction(def);

        try {
            LOGGER.fine("Executing transaction. RequiresNew=" + requiresNew);
            action.execute();
            transactionManager.commit(status);
            LOGGER.fine("Transaction committed successfully.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Transaction failed, rolling back", e);
            transactionManager.rollback(status);
            throw e;
        }
    }

    /**
     * Retrieve database credentials from AWS Secrets Manager.
     * Credentials are encrypted at rest and in transit.
     * Supports automatic rotation without application redeployment.
     *
     * @return DatabaseCredentials object containing connection details
     */
    public static DatabaseCredentials getDatabaseCredentials() {
        try (SecretsManagerClient client = SecretsManagerClient.builder()
                .region(Region.of(AWS_REGION))
                .build()) {

            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(DB_SECRET_NAME)
                    .build();

            GetSecretValueResponse response = client.getSecretValue(request);
            String secretString = response.secretString();

            // Parse JSON secret (format: {"username":"xxx","password":"yyy","host":"zzz","port":"5432","dbname":"ltr"})
            return DatabaseCredentials.fromJson(secretString);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve database credentials from AWS Secrets Manager", e);
            throw new RuntimeException("Database credential retrieval failed", e);
        }
    }

    /**
     * Functional interface for transaction callbacks.
     */
    @FunctionalInterface
    public interface TransactionCallback {
        void execute() throws Exception;
    }

    /**
     * Database credentials retrieved from AWS Secrets Manager.
     */
    public static class DatabaseCredentials {
        private String username;
        private String password;
        private String host;
        private int port;
        private String dbname;

        public static DatabaseCredentials fromJson(String json) {
            // Simple JSON parsing - in production use Jackson ObjectMapper
            DatabaseCredentials creds = new DatabaseCredentials();
            creds.username = extractJsonValue(json, "username");
            creds.password = extractJsonValue(json, "password");
            creds.host = extractJsonValue(json, "host");
            creds.port = Integer.parseInt(extractJsonValue(json, "port"));
            creds.dbname = extractJsonValue(json, "dbname");
            return creds;
        }

        private static String extractJsonValue(String json, String key) {
            String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            return m.find() ? m.group(1) : "";
        }

        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getHost() { return host; }
        public int getPort() { return port; }
        public String getDbname() { return dbname; }

        public String getJdbcUrl() {
            return String.format("jdbc:postgresql://%s:%d/%s", host, port, dbname);
        }
    }
}

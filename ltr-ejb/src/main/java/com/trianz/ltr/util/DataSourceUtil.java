package com.trianz.ltr.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DataSourceUtil - Cloud-native DataSource utility using HikariCP and AWS Secrets Manager.
 *
 * CLOUD-NATIVE FEATURES:
 *   - HikariCP connection pooling (optimized for AWS RDS)
 *   - AWS Secrets Manager integration for credential management
 *   - Environment variable configuration for cloud deployment
 *   - Automatic credential rotation support
 *
 * CONFIGURATION (via environment variables):
 *   - DB_SECRET_NAME: AWS Secrets Manager secret name (e.g., "ltr/db/credentials")
 *   - DB_HOST: Database host (e.g., RDS endpoint)
 *   - DB_PORT: Database port (default: 5432)
 *   - DB_NAME: Database name
 *   - AWS_REGION: AWS region for Secrets Manager (default: us-east-1)
 *
 * AWS Secrets Manager secret format (JSON):
 * {
 *   "username": "ltr_app_user",
 *   "password": "secure_password_from_secrets_manager"
 * }
 */
public class DataSourceUtil {

    private static final Logger LOGGER = Logger.getLogger(DataSourceUtil.class.getName());
    private static HikariDataSource dataSource;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private DataSourceUtil() { /* utility class */ }

    /**
     * Get or create the HikariCP DataSource with AWS Secrets Manager credentials.
     */
    public static synchronized DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = createDataSource();
        }
        return dataSource;
    }

    /**
     * Create HikariCP DataSource with credentials from AWS Secrets Manager.
     */
    private static HikariDataSource createDataSource() {
        try {
            // Retrieve database credentials from AWS Secrets Manager
            DatabaseCredentials credentials = getCredentialsFromSecretsManager();

            // Build JDBC URL from environment variables
            String dbHost = getEnvOrDefault("DB_HOST", "localhost");
            String dbPort = getEnvOrDefault("DB_PORT", "5432");
            String dbName = getEnvOrDefault("DB_NAME", "landtitle");
            String jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s", dbHost, dbPort, dbName);

            // Configure HikariCP for AWS RDS
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(credentials.getUsername());
            config.setPassword(credentials.getPassword());
            config.setDriverClassName("org.postgresql.Driver");

            // HikariCP optimizations for cloud/RDS
            config.setMaximumPoolSize(20);
            config.setMinimumIdle(5);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            config.setConnectionTestQuery("SELECT 1");
            config.setPoolName("LandTitleRegistryPool");

            // Additional RDS-specific settings
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            LOGGER.info("HikariCP DataSource initialized for: " + jdbcUrl);
            return new HikariDataSource(config);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create DataSource", e);
            throw new RuntimeException("DataSource initialization failed", e);
        }
    }

    /**
     * Retrieve database credentials from AWS Secrets Manager.
     */
    private static DatabaseCredentials getCredentialsFromSecretsManager() {
        String secretName = getEnvOrDefault("DB_SECRET_NAME", "ltr/db/credentials");
        String region = getEnvOrDefault("AWS_REGION", "us-east-1");

        try (SecretsManagerClient client = SecretsManagerClient.builder()
                .region(Region.of(region))
                .build()) {

            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(secretName)
                    .build();

            GetSecretValueResponse response = client.getSecretValue(request);
            String secretString = response.secretString();

            // Parse JSON secret
            JsonNode secretJson = objectMapper.readTree(secretString);
            String username = secretJson.get("username").asText();
            String password = secretJson.get("password").asText();

            LOGGER.info("Retrieved database credentials from AWS Secrets Manager: " + secretName);
            return new DatabaseCredentials(username, password);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve credentials from AWS Secrets Manager", e);
            // Fallback to environment variables for local development
            LOGGER.warning("Falling back to environment variables for database credentials");
            return new DatabaseCredentials(
                    getEnvOrDefault("DB_USERNAME", "postgres"),
                    getEnvOrDefault("DB_PASSWORD", "postgres")
            );
        }
    }

    /**
     * Get a connection from the HikariCP pool.
     */
    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    /**
     * Close connection (returns to pool).
     */
    public static void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to close connection", e);
            }
        }
    }

    /**
     * Shutdown the DataSource (for application shutdown).
     */
    public static synchronized void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            LOGGER.info("HikariCP DataSource closed");
        }
    }

    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    /**
     * Internal class to hold database credentials.
     */
    private static class DatabaseCredentials {
        private final String username;
        private final String password;

        public DatabaseCredentials(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }
    }
}

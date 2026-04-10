package com.trianz.ltr.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * CloudDataSourceUtil - Cloud-native DataSource utility using HikariCP and AWS Secrets Manager.
 * 
 * Replaces WAS-specific DataSource lookup with:
 * - HikariCP connection pooling for high performance
 * - AWS Secrets Manager for secure credential management
 * - Environment variable configuration for cloud deployment
 * - Connection timeout and retry logic for cloud resilience
 */
public class CloudDataSourceUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudDataSourceUtil.class);
    private static HikariDataSource dataSource;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Environment variables for configuration
    private static final String DB_SECRET_NAME = System.getenv().getOrDefault("DB_SECRET_NAME", "ltr/db/credentials");
    private static final String DB_HOST = System.getenv().getOrDefault("DB_HOST", "localhost");
    private static final String DB_PORT = System.getenv().getOrDefault("DB_PORT", "5432");
    private static final String DB_NAME = System.getenv().getOrDefault("DB_NAME", "landtitle");
    private static final String AWS_REGION = System.getenv().getOrDefault("AWS_REGION", "us-east-1");
    
    // Connection pool configuration
    private static final int MAX_POOL_SIZE = Integer.parseInt(System.getenv().getOrDefault("DB_POOL_MAX_SIZE", "20"));
    private static final int MIN_IDLE = Integer.parseInt(System.getenv().getOrDefault("DB_POOL_MIN_IDLE", "5"));
    private static final long CONNECTION_TIMEOUT = Long.parseLong(System.getenv().getOrDefault("DB_CONNECTION_TIMEOUT_MS", "30000"));
    private static final long IDLE_TIMEOUT = Long.parseLong(System.getenv().getOrDefault("DB_IDLE_TIMEOUT_MS", "600000"));
    private static final long MAX_LIFETIME = Long.parseLong(System.getenv().getOrDefault("DB_MAX_LIFETIME_MS", "1800000"));

    private CloudDataSourceUtil() { /* utility class */ }

    /**
     * Initialize and return the HikariCP DataSource with credentials from AWS Secrets Manager.
     * Thread-safe singleton initialization.
     */
    public static synchronized DataSource getDataSource() {
        if (dataSource == null) {
            try {
                LOGGER.info("Initializing cloud-native DataSource with HikariCP");
                
                // Retrieve database credentials from AWS Secrets Manager
                DatabaseCredentials credentials = getCredentialsFromSecretsManager();
                
                // Configure HikariCP
                HikariConfig config = new HikariConfig();
                
                // JDBC URL construction
                String jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s", 
                    DB_HOST, DB_PORT, DB_NAME);
                config.setJdbcUrl(jdbcUrl);
                config.setUsername(credentials.getUsername());
                config.setPassword(credentials.getPassword());
                config.setDriverClassName("org.postgresql.Driver");
                
                // Connection pool settings
                config.setMaximumPoolSize(MAX_POOL_SIZE);
                config.setMinimumIdle(MIN_IDLE);
                config.setConnectionTimeout(CONNECTION_TIMEOUT);
                config.setIdleTimeout(IDLE_TIMEOUT);
                config.setMaxLifetime(MAX_LIFETIME);
                
                // Connection validation
                config.setConnectionTestQuery("SELECT 1");
                config.setValidationTimeout(5000);
                
                // Pool name for monitoring
                config.setPoolName("LandTitleRegistryPool");
                
                // Leak detection (30 seconds)
                config.setLeakDetectionThreshold(30000);
                
                // Additional PostgreSQL optimizations
                config.addDataSourceProperty("cachePrepStmts", "true");
                config.addDataSourceProperty("prepStmtCacheSize", "250");
                config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                config.addDataSourceProperty("useServerPrepStmts", "true");
                config.addDataSourceProperty("reWriteBatchedInserts", "true");
                
                dataSource = new HikariDataSource(config);
                
                LOGGER.info("HikariCP DataSource initialized successfully. Pool: {}, Max: {}, Min: {}", 
                    config.getPoolName(), MAX_POOL_SIZE, MIN_IDLE);
                
            } catch (Exception e) {
                LOGGER.error("Failed to initialize DataSource", e);
                throw new RuntimeException("DataSource initialization failed", e);
            }
        }
        return dataSource;
    }

    /**
     * Get a connection from the HikariCP pool.
     * Caller is responsible for closing the connection.
     */
    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    /**
     * Retrieve database credentials from AWS Secrets Manager.
     * Secrets are cached by AWS SDK for performance.
     */
    private static DatabaseCredentials getCredentialsFromSecretsManager() {
        try {
            LOGGER.info("Retrieving database credentials from AWS Secrets Manager: {}", DB_SECRET_NAME);
            
            SecretsManagerClient client = SecretsManagerClient.builder()
                .region(Region.of(AWS_REGION))
                .build();
            
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                .secretId(DB_SECRET_NAME)
                .build();
            
            GetSecretValueResponse response = client.getSecretValue(request);
            String secretString = response.secretString();
            
            // Parse JSON secret
            JsonNode secretJson = objectMapper.readTree(secretString);
            
            String username = secretJson.get("username").asText();
            String password = secretJson.get("password").asText();
            
            LOGGER.info("Successfully retrieved database credentials for user: {}", username);
            
            return new DatabaseCredentials(username, password);
            
        } catch (Exception e) {
            LOGGER.error("Failed to retrieve credentials from AWS Secrets Manager", e);
            
            // Fallback to environment variables for local development
            LOGGER.warn("Falling back to environment variables for database credentials");
            String username = System.getenv().getOrDefault("DB_USERNAME", "postgres");
            String password = System.getenv().getOrDefault("DB_PASSWORD", "postgres");
            
            return new DatabaseCredentials(username, password);
        }
    }

    /**
     * Safely close a connection (returns it to the pool).
     */
    public static void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                LOGGER.warn("Failed to close connection", e);
            }
        }
    }

    /**
     * Shutdown the connection pool gracefully.
     * Should be called on application shutdown.
     */
    public static synchronized void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            LOGGER.info("Shutting down HikariCP DataSource");
            dataSource.close();
            dataSource = null;
        }
    }

    /**
     * Get pool statistics for monitoring.
     */
    public static String getPoolStats() {
        if (dataSource != null) {
            return String.format("Active: %d, Idle: %d, Total: %d, Waiting: %d",
                dataSource.getHikariPoolMXBean().getActiveConnections(),
                dataSource.getHikariPoolMXBean().getIdleConnections(),
                dataSource.getHikariPoolMXBean().getTotalConnections(),
                dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
        }
        return "DataSource not initialized";
    }

    /**
     * Inner class to hold database credentials.
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

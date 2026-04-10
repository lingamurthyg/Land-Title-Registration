package com.trianz.ltr.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CloudDataSourceUtil - Cloud-native DataSource utility using HikariCP connection pooling.
 * 
 * CLOUD-READY FEATURES:
 *   - Uses HikariCP for efficient connection pooling
 *   - Configured via environment variables (12-factor app)
 *   - Supports AWS RDS, Azure SQL, GCP Cloud SQL
 *   - No vendor-specific dependencies
 *   - Proper connection timeout and pool management
 *
 * ENVIRONMENT VARIABLES:
 *   - DB_URL: JDBC connection URL (e.g., jdbc:postgresql://rds-endpoint:5432/ltrdb)
 *   - DB_USERNAME: Database username (from AWS Secrets Manager)
 *   - DB_PASSWORD: Database password (from AWS Secrets Manager)
 *   - DB_POOL_SIZE: Maximum pool size (default: 10)
 *   - DB_CONNECTION_TIMEOUT: Connection timeout in ms (default: 30000)
 */
public class CloudDataSourceUtil {

    private static final Logger LOGGER = Logger.getLogger(CloudDataSourceUtil.class.getName());
    
    private static HikariDataSource dataSource;
    
    static {
        initializeDataSource();
    }

    private CloudDataSourceUtil() { /* utility class */ }

    /**
     * Initialize HikariCP DataSource from environment variables.
     * This is cloud-native and works with AWS Secrets Manager, Azure Key Vault, etc.
     */
    private static void initializeDataSource() {
        try {
            HikariConfig config = new HikariConfig();
            
            // Read from environment variables (12-factor app principle)
            String dbUrl = getEnvOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/ltrdb");
            String dbUsername = getEnvOrDefault("DB_USERNAME", "ltr_user");
            String dbPassword = getEnvOrDefault("DB_PASSWORD", "changeme");
            int poolSize = Integer.parseInt(getEnvOrDefault("DB_POOL_SIZE", "10"));
            int connectionTimeout = Integer.parseInt(getEnvOrDefault("DB_CONNECTION_TIMEOUT", "30000"));
            
            config.setJdbcUrl(dbUrl);
            config.setUsername(dbUsername);
            config.setPassword(dbPassword);
            config.setMaximumPoolSize(poolSize);
            config.setConnectionTimeout(connectionTimeout);
            config.setIdleTimeout(600000); // 10 minutes
            config.setMaxLifetime(1800000); // 30 minutes
            config.setConnectionTestQuery("SELECT 1");
            config.setPoolName("LTR-HikariCP-Pool");
            
            // Cloud-optimized settings
            config.setAutoCommit(false); // Explicit transaction control
            config.setLeakDetectionThreshold(60000); // 1 minute leak detection
            
            dataSource = new HikariDataSource(config);
            
            LOGGER.info("HikariCP DataSource initialized successfully for cloud deployment");
            LOGGER.info("Database URL: " + maskPassword(dbUrl));
            LOGGER.info("Pool size: " + poolSize);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize HikariCP DataSource", e);
            throw new RuntimeException("DataSource initialization failed", e);
        }
    }

    /**
     * Get a connection from the HikariCP pool.
     * Cloud-ready: uses connection pooling for efficient resource utilization.
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource not initialized");
        }
        return dataSource.getConnection();
    }

    /**
     * Get the DataSource instance.
     */
    public static DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Safely close a connection (returns it to the pool).
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
     * Shutdown the connection pool (for graceful application shutdown).
     */
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            LOGGER.info("HikariCP DataSource shut down successfully");
        }
    }

    /**
     * Get environment variable with default fallback.
     */
    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.trim().isEmpty()) ? value : defaultValue;
    }

    /**
     * Mask password in connection string for logging.
     */
    private static String maskPassword(String url) {
        if (url == null) return null;
        return url.replaceAll("password=[^&;]+", "password=***");
    }
}

package com.trianz.ltr.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CloudDataSourceUtil - Cloud-native DataSource management with HikariCP.
 *
 * CLOUD-NATIVE REPLACEMENT:
 *   - Replaced IBM WebSphere WSDataSource with HikariCP connection pool
 *   - Removed JNDI lookups (replaced with direct DataSource configuration)
 *   - Integrated with AWS Secrets Manager for credential management
 *   - Removed WAS-specific APIs (com.ibm.websphere.rsadapter.WSDataSource)
 *
 * HIKARICP BENEFITS:
 *   - Lightweight, high-performance JDBC connection pool
 *   - Optimized for cloud environments (AWS RDS, Aurora)
 *   - Automatic connection validation and leak detection
 *   - Minimal overhead compared to WAS connection pools
 *
 * AWS RDS INTEGRATION:
 *   - Credentials retrieved from AWS Secrets Manager
 *   - Connection pooling optimized for RDS/Aurora
 *   - Supports IAM database authentication (optional)
 */
public class WASDataSourceUtil {

    private static final Logger LOGGER = Logger.getLogger(WASDataSourceUtil.class.getName());

    /** Singleton HikariCP DataSource instance */
    private static volatile HikariDataSource dataSource;

    private WASDataSourceUtil() { /* utility class */ }

    /**
     * Obtains a HikariCP DataSource configured for AWS RDS.
     * Credentials are retrieved from AWS Secrets Manager.
     *
     * @return HikariCP DataSource
     */
    public static DataSource getDataSource() {
        if (dataSource == null) {
            synchronized (WASDataSourceUtil.class) {
                if (dataSource == null) {
                    dataSource = createDataSource();
                }
            }
        }
        return dataSource;
    }

    /**
     * Create and configure HikariCP DataSource with AWS Secrets Manager credentials.
     */
    private static HikariDataSource createDataSource() {
        try {
            // Retrieve credentials from AWS Secrets Manager
            WASTransactionUtil.DatabaseCredentials creds = WASTransactionUtil.getDatabaseCredentials();

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(creds.getJdbcUrl());
            config.setUsername(creds.getUsername());
            config.setPassword(creds.getPassword());

            // HikariCP optimizations for cloud environments
            config.setMaximumPoolSize(20);
            config.setMinimumIdle(5);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            config.setConnectionTestQuery("SELECT 1");
            config.setPoolName("LandTitleRegistryPool");

            // AWS RDS-specific optimizations
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");

            LOGGER.info("HikariCP DataSource initialized for AWS RDS: " + creds.getHost());
            return new HikariDataSource(config);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create HikariCP DataSource", e);
            throw new RuntimeException("DataSource initialization failed", e);
        }
    }

    /**
     * Convenience method: get a Connection from the HikariCP pool.
     * Caller is responsible for closing the Connection.
     *
     * @return JDBC Connection from HikariCP pool
     */
    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    /**
     * Silently close a JDBC Connection back to the HikariCP pool.
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
     * Shutdown the HikariCP DataSource (for application shutdown).
     */
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            LOGGER.info("HikariCP DataSource closed.");
        }
    }
}

package com.trianz.ltr.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * WASDataSourceUtil - Centralized DataSource utility for cloud-native applications.
 *
 * CLOUD-NATIVE MIGRATION:
 *   - Removed IBM WebSphere-specific imports (com.ibm.websphere.rsadapter.WSDataSource)
 *   - Replaced JNDI lookups with Spring dependency injection
 *   - Uses HikariCP connection pool (configured in application.properties)
 *   - Compatible with AWS RDS and cloud-native patterns
 *   - Stateless design for horizontal scaling in EKS/ECS
 *
 * MIGRATION FROM WAS:
 *   - WSDataSource → Standard javax.sql.DataSource
 *   - JNDI lookups → Spring @Autowired DataSource
 *   - WAS connection pool → HikariCP
 */
@Component
public class WASDataSourceUtil {

    private static final Logger LOGGER = Logger.getLogger(WASDataSourceUtil.class.getName());

    private static DataSource dataSource;

    @Autowired
    public void setDataSource(DataSource ds) {
        WASDataSourceUtil.dataSource = ds;
    }

    private WASDataSourceUtil() { /* utility class */ }

    /**
     * Obtains a DataSource from Spring context.
     * Uses HikariCP connection pool configured in application.properties.
     *
     * @return DataSource instance
     * @throws SQLException if DataSource is not available
     */
    public static DataSource getDataSource() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource not initialized. Ensure Spring context is loaded.");
        }
        return dataSource;
    }

    /**
     * Convenience method: get a Connection from the DataSource.
     * Caller is responsible for closing the Connection.
     *
     * @return JDBC Connection from HikariCP connection pool
     */
    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    /**
     * Silently close a JDBC Connection back to the connection pool.
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
}

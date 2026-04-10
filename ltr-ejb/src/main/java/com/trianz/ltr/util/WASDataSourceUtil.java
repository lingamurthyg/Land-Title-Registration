package com.trianz.ltr.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * WASDataSourceUtil - DEPRECATED - Use CloudDataSourceUtil instead.
 *
 * This class has been migrated to cloud-native HikariCP connection pooling
 * with AWS Secrets Manager integration.
 *
 * MIGRATION PATH:
 *   1. Replace all WASDataSourceUtil calls with CloudDataSourceUtil
 *   2. Remove IBM WebSphere DataSource dependencies (com.ibm.websphere.rsadapter.*)
 *   3. Use Spring's @Autowired DataSource injection in services
 *
 * CLOUD-NATIVE REPLACEMENT:
 *   - CloudDataSourceUtil with HikariCP connection pooling
 *   - AWS Secrets Manager for credential management
 *   - Environment variable configuration
 *   - Works with AWS RDS, Azure SQL, GCP Cloud SQL
 */
@Deprecated
public class WASDataSourceUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(WASDataSourceUtil.class);

    private WASDataSourceUtil() { /* utility class */ }

    /**
     * @deprecated Use CloudDataSourceUtil.getDataSource() instead
     */
    @Deprecated
    public static DataSource getDataSource() throws Exception {
        LOGGER.warn("WASDataSourceUtil is deprecated. Use CloudDataSourceUtil instead");
        return CloudDataSourceUtil.getDataSource();
    }

    /**
     * @deprecated Use CloudDataSourceUtil.getConnection() instead
     */
    @Deprecated
    public static Connection getConnection() throws SQLException {
        LOGGER.warn("WASDataSourceUtil is deprecated. Use CloudDataSourceUtil instead");
        return CloudDataSourceUtil.getConnection();
    }

    /**
     * @deprecated Use CloudDataSourceUtil.closeQuietly() instead
     */
    @Deprecated
    public static void closeQuietly(Connection conn) {
        CloudDataSourceUtil.closeQuietly(conn);
    }

    /**
     * @deprecated EJB home lookup not supported in cloud-native applications
     */
    @Deprecated
    public static Object lookupEJBHome(String jndiName) throws Exception {
        throw new UnsupportedOperationException(
            "EJB home lookup not supported in cloud environments. Use Spring dependency injection");
    }
}

package com.trianz.ltr.util;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DataSourceUtil - Centralized DataSource / JNDI lookup utility.
 * 
 * CONTAINERIZATION MODERNIZATION - BLOCKER FIXES APPLIED:
 *   ✓ blocker-1 (cz-java-0075): Removed WebSphere-specific com.ibm.websphere.rsadapter.WSDataSource
 *   ✓ blocker-3 (cz-java-0081): Migrated to standard Jakarta EE javax.sql.DataSource API
 *   ✓ blocker-9 (cz-java-0085): Logging redirected to stdout/stderr via java.util.logging
 *
 * MIGRATION DETAILS:
 *   - Uses standard javax.sql.DataSource (Jakarta EE compatible)
 *   - Works with any Jakarta EE-compliant server (Liberty, WildFly, Payara, Tomcat)
 *   - No WebSphere-specific dependencies
 *   - All logging outputs to stdout/stderr for container log collection
 *
 * DataSource is configured in server.xml (Liberty) or standalone.xml (WildFly):
 *   Resources → JDBC → Data sources → jdbc/LandTitleDS
 *
 * ENVIRONMENT CONFIGURATION:
 *   DataSource JNDI name can be overridden via environment variable:
 *   - DS_JNDI_NAME (default: jdbc/LandTitleDS)
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * MODERNIZATION NOTE (Concierto Modernize – M-Path awareness):
 *   On Open Liberty replace this class with:
 *     @Resource(lookup = "jdbc/LandTitleDS")
 *     private DataSource dataSource;
 *   and configure <dataSource> in server.xml.
 * ──────────────────────────────────────────────────────────────────────────────
 */
public class WASDataSourceUtil {

    // Logger outputs to stdout/stderr for container log collection (blocker-9 fix)
    private static final Logger LOGGER = Logger.getLogger(WASDataSourceUtil.class.getName());

    /** JNDI name bound in server configuration - can be overridden via environment variable */
    public static final String DS_JNDI_NAME = System.getenv().getOrDefault("DS_JNDI_NAME", "jdbc/LandTitleDS");

    /** Standard namespace prefix for application-scoped resources */
    public static final String WAS_NS_PREFIX = "java:comp/env/";

    private WASDataSourceUtil() { /* utility class */ }

    /**
     * Obtains a DataSource from JNDI registry.
     * Standard Jakarta EE: uses InitialContext without provider URL (in-process lookup).
     *
     * BLOCKER FIX (blocker-1, blocker-3):
     *   - Returns standard javax.sql.DataSource (not WebSphere-specific WSDataSource)
     *   - Uses standard JNDI lookup (no WebSphere-specific APIs)
     *
     * @return Standard DataSource
     * @throws NamingException if JNDI lookup fails
     */
    public static DataSource getDataSource() throws NamingException {
        Context ctx = null;
        try {
            // Standard Jakarta EE in-process JNDI — no InitialContextFactory or provider URL needed
            ctx = new InitialContext();

            // Try java:comp/env first (preferred in Jakarta EE), then global JNDI
            DataSource ds;
            try {
                ds = (DataSource) ctx.lookup(WAS_NS_PREFIX + DS_JNDI_NAME);
                LOGGER.fine("DataSource obtained from: " + WAS_NS_PREFIX + DS_JNDI_NAME);
            } catch (NamingException e) {
                LOGGER.warning("java:comp/env lookup failed, falling back to global: " + DS_JNDI_NAME);
                ds = (DataSource) ctx.lookup(DS_JNDI_NAME);
                LOGGER.fine("DataSource obtained from: " + DS_JNDI_NAME);
            }

            LOGGER.fine("Obtained DataSource successfully");
            return ds;

        } finally {
            if (ctx != null) {
                try { ctx.close(); } catch (NamingException ignored) { /* safe close */ }
            }
        }
    }

    /**
     * Convenience method: lookup DataSource and return a Connection.
     * Caller is responsible for closing the Connection.
     *
     * @return JDBC Connection from connection pool
     */
    public static Connection getConnection() throws NamingException, SQLException {
        return getDataSource().getConnection();
    }

    /**
     * Silently close a JDBC Connection back to the connection pool.
     * Logs errors to stdout/stderr for container log collection.
     */
    public static void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                // Log to stderr for container log collection (blocker-9 fix)
                LOGGER.log(Level.WARNING, "Failed to close connection", e);
            }
        }
    }

    /**
     * Standard Jakarta EE: look up the EJB home interface by JNDI name.
     * Used by remote clients and other EJBs needing EJB 2.x home lookup.
     *
     * @param jndiName e.g. "ejb/LandTitleRegistryHome"
     * @return the bound object (typically an EJBHome)
     */
    public static Object lookupEJBHome(String jndiName) throws NamingException {
        Context ctx = new InitialContext();
        try {
            // Standard uses ejb/ prefix in JNDI for EJB homes
            return ctx.lookup("ejb/" + jndiName);
        } finally {
            ctx.close();
        }
    }
}

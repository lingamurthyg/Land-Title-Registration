package com.trianz.ltr.util;

import com.ibm.websphere.rsadapter.WSDataSource;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * WASDataSourceUtil - Centralized WAS DataSource / JNDI lookup utility.
 *
 * WAS-SPECIFIC APIs USED:
 *   - com.ibm.websphere.rsadapter.WSDataSource  (WAS proprietary DataSource wrapper)
 *   - javax.naming.InitialContext with WAS JNDI namespace
 *
 * WAS DataSource is configured in the WAS admin console:
 *   Resources → JDBC → Data sources → ltr/jdbc/LandTitleDS
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

    private static final Logger LOGGER = Logger.getLogger(WASDataSourceUtil.class.getName());

    /** JNDI name bound in WAS admin console */
    public static final String DS_JNDI_NAME = "jdbc/LandTitleDS";

    /** WAS namespace prefix for application-scoped resources */
    public static final String WAS_NS_PREFIX = "java:comp/env/";

    private WASDataSourceUtil() { /* utility class */ }

    /**
     * Obtains a DataSource from WAS JNDI registry.
     * WAS-specific: uses InitialContext without provider URL (in-process lookup).
     *
     * @return WSDataSource cast to DataSource
     * @throws NamingException if JNDI lookup fails
     */
    public static DataSource getDataSource() throws NamingException {
        Context ctx = null;
        try {
            // WAS in-process JNDI — no InitialContextFactory or provider URL needed
            ctx = new InitialContext();

            // Try java:comp/env first (preferred in WAS EE7), then global JNDI
            DataSource ds;
            try {
                ds = (DataSource) ctx.lookup(WAS_NS_PREFIX + DS_JNDI_NAME);
            } catch (NamingException e) {
                LOGGER.warning("java:comp/env lookup failed, falling back to global: " + DS_JNDI_NAME);
                ds = (DataSource) ctx.lookup(DS_JNDI_NAME);
            }

            // WAS-specific: cast to WSDataSource to access IBM proprietary methods
            if (ds instanceof WSDataSource) {
                WSDataSource wsDs = (WSDataSource) ds;
                LOGGER.fine("Obtained WSDataSource. Max connections: " + wsDs.getMaxConnections());
            }

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
     * @return JDBC Connection from WAS connection pool
     */
    public static Connection getConnection() throws NamingException, SQLException {
        return getDataSource().getConnection();
    }

    /**
     * Silently close a JDBC Connection back to the WAS connection pool.
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
     * WAS-specific: look up the EJB home interface by JNDI name.
     * Used by remote clients and other EJBs needing EJB 2.x home lookup.
     *
     * @param jndiName e.g. "ejb/LandTitleRegistryHome"
     * @return the bound object (typically an EJBHome)
     */
    public static Object lookupEJBHome(String jndiName) throws NamingException {
        Context ctx = new InitialContext();
        try {
            // WAS uses ejb/ prefix in JNDI for EJB homes
            return ctx.lookup("ejb/" + jndiName);
        } finally {
            ctx.close();
        }
    }
}

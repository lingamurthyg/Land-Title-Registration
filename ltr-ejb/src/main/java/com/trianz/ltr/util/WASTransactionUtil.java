package com.trianz.ltr.util;

import com.ibm.websphere.uow.UOWSynchronizationRegistry;
import com.ibm.wsspi.uow.UOWAction;
import com.ibm.wsspi.uow.UOWManager;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.UserTransaction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * WASTransactionUtil - IBM WebSphere-specific transaction management helper.
 *
 * WAS-SPECIFIC APIS USED:
 *   - com.ibm.websphere.uow.UOWSynchronizationRegistry  (WAS proprietary UOW)
 *   - com.ibm.wsspi.uow.UOWManager                     (WAS UOW Manager SPI)
 *   - com.ibm.wsspi.uow.UOWAction                      (Lambda-style UOW work unit)
 *
 * These APIs allow precise transaction boundary control and UOW-scoped work
 * units that span multiple EJB calls — a WAS-proprietary pattern widely used
 * in government/enterprise registry applications.
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * MODERNIZATION NOTE (Concierto Modernize – M-Path awareness):
 *   Replace UOWManager with standard JTA:
 *     @Resource UserTransaction ut;
 *   or use CDI @Transactional on service methods in Open Liberty.
 * ──────────────────────────────────────────────────────────────────────────────
 */
public class WASTransactionUtil {

    private static final Logger LOGGER = Logger.getLogger(WASTransactionUtil.class.getName());

    /** WAS JNDI name for the UOW Manager */
    private static final String UOW_MANAGER_JNDI = "java:comp/websphere/UOWManager";

    /** WAS JNDI name for UserTransaction */
    private static final String USER_TX_JNDI = "java:comp/UserTransaction";

    private WASTransactionUtil() { /* utility */ }

    /**
     * Execute a unit of work using the WAS UOWManager.
     * Provides XA-capable, cluster-aware transaction boundaries.
     *
     * @param action  the transactional work to perform
     * @param requiresNew  if true, always starts a new transaction (REQUIRES_NEW semantics)
     */
    public static void executeInTransaction(UOWAction action, boolean requiresNew)
            throws Exception {

        UOWManager uowManager = lookupUOWManager();

        int uowType = requiresNew
                ? UOWManager.UOW_TYPE_GLOBAL_TRANSACTION
                : UOWManager.UOW_TYPE_GLOBAL_TRANSACTION;

        LOGGER.fine("Executing UOW action. RequiresNew=" + requiresNew);
        uowManager.runUnderUOW(uowType, requiresNew, action);
    }

    /**
     * Look up WAS UOWManager from JNDI.
     * This is a WAS-proprietary extension – not available in standard Java EE.
     */
    public static UOWManager lookupUOWManager() throws NamingException {
        InitialContext ctx = new InitialContext();
        try {
            return (UOWManager) ctx.lookup(UOW_MANAGER_JNDI);
        } finally {
            ctx.close();
        }
    }

    /**
     * Obtain a standard JTA UserTransaction from WAS JNDI.
     * Usable from servlets and non-EJB components.
     */
    public static UserTransaction getUserTransaction() throws NamingException {
        InitialContext ctx = new InitialContext();
        try {
            return (UserTransaction) ctx.lookup(USER_TX_JNDI);
        } finally {
            ctx.close();
        }
    }

    /**
     * Helper: begin a UserTransaction safely (for servlet/non-EJB use).
     */
    public static UserTransaction beginTransaction() {
        try {
            UserTransaction ut = getUserTransaction();
            ut.begin();
            return ut;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to begin UserTransaction", e);
            throw new RuntimeException("Transaction begin failed", e);
        }
    }

    /**
     * Commit or roll back based on success flag; always nulls the transaction.
     */
    public static void endTransaction(UserTransaction ut, boolean commit) {
        if (ut == null) return;
        try {
            if (commit) {
                ut.commit();
                LOGGER.fine("Transaction committed.");
            } else {
                ut.rollback();
                LOGGER.warning("Transaction rolled back.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to end transaction", e);
            try { ut.rollback(); } catch (Exception ignored) { /* best effort */ }
            throw new RuntimeException("Transaction end failed", e);
        }
    }
}

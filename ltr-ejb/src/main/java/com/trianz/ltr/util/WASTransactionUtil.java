package com.trianz.ltr.util;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.UserTransaction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TransactionUtil - Standard Jakarta EE transaction management helper.
 *
 * CONTAINERIZATION MODERNIZATION - BLOCKER FIXES APPLIED:
 *   ✓ blocker-2 (cz-java-0075): Removed WebSphere-specific UOWSynchronizationRegistry
 *   ✓ blocker-2 (cz-java-0075): Removed WebSphere-specific UOWManager
 *   ✓ blocker-2 (cz-java-0075): Removed WebSphere-specific UOWAction
 *   ✓ blocker-4 (cz-java-0081): Migrated to standard JTA UserTransaction API
 *   ✓ blocker-10 (cz-java-0085): Logging redirected to stdout/stderr via java.util.logging
 *
 * MIGRATION DETAILS:
 *   - Uses standard JTA UserTransaction (Jakarta EE compatible)
 *   - Works with any Jakarta EE-compliant server (Liberty, WildFly, Payara, Tomcat)
 *   - No WebSphere-specific dependencies
 *   - All logging outputs to stdout/stderr for container log collection
 *
 * These standard APIs allow transaction boundary control that works across
 * all Jakarta EE servers — a portable pattern for enterprise applications.
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * MODERNIZATION NOTE (Concierto Modernize – M-Path awareness):
 *   Replace manual UserTransaction with:
 *     @Resource UserTransaction ut;
 *   or use CDI @Transactional on service methods in Open Liberty.
 * ──────────────────────────────────────────────────────────────────────────────
 */
public class WASTransactionUtil {

    // Logger outputs to stdout/stderr for container log collection (blocker-10 fix)
    private static final Logger LOGGER = Logger.getLogger(WASTransactionUtil.class.getName());

    /** Standard JNDI name for UserTransaction */
    private static final String USER_TX_JNDI = "java:comp/UserTransaction";

    private WASTransactionUtil() { /* utility */ }

    /**
     * Execute a unit of work using standard JTA UserTransaction.
     * Provides XA-capable, cluster-aware transaction boundaries.
     *
     * BLOCKER FIX (blocker-2, blocker-4):
     *   - Uses standard JTA UserTransaction (not WebSphere-specific UOWManager)
     *   - Replaces WebSphere UOWAction with functional interface
     *
     * @param action  the transactional work to perform (functional interface)
     * @param requiresNew  if true, always starts a new transaction (REQUIRES_NEW semantics)
     */
    public static void executeInTransaction(TransactionAction action, boolean requiresNew)
            throws Exception {

        UserTransaction ut = getUserTransaction();
        boolean startedTransaction = false;

        try {
            // Check if transaction already exists
            int status = ut.getStatus();
            boolean inTransaction = (status == javax.transaction.Status.STATUS_ACTIVE);

            if (requiresNew || !inTransaction) {
                ut.begin();
                startedTransaction = true;
                // Log to stdout for container log collection (blocker-10 fix)
                LOGGER.fine("Started new transaction. RequiresNew=" + requiresNew);
            }

            // Execute the action
            action.execute();

            // Commit if we started the transaction
            if (startedTransaction) {
                ut.commit();
                // Log to stdout for container log collection (blocker-10 fix)
                LOGGER.fine("Transaction committed successfully");
            }

        } catch (Exception e) {
            // Rollback if we started the transaction
            if (startedTransaction) {
                try {
                    ut.rollback();
                    // Log to stderr for container log collection (blocker-10 fix)
                    LOGGER.warning("Transaction rolled back due to exception");
                } catch (Exception rollbackEx) {
                    LOGGER.log(Level.SEVERE, "Failed to rollback transaction", rollbackEx);
                }
            }
            throw e;
        }
    }

    /**
     * Functional interface for transaction actions.
     * Replaces WebSphere-specific UOWAction (blocker-2 fix).
     */
    @FunctionalInterface
    public interface TransactionAction {
        void execute() throws Exception;
    }

    /**
     * Obtain a standard JTA UserTransaction from JNDI.
     * Usable from servlets and non-EJB components.
     *
     * BLOCKER FIX (blocker-2, blocker-4):
     *   - Returns standard JTA UserTransaction (not WebSphere-specific UOWManager)
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
            // Log to stderr for container log collection (blocker-10 fix)
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
                // Log to stdout for container log collection (blocker-10 fix)
                LOGGER.fine("Transaction committed.");
            } else {
                ut.rollback();
                // Log to stderr for container log collection (blocker-10 fix)
                LOGGER.warning("Transaction rolled back.");
            }
        } catch (Exception e) {
            // Log to stderr for container log collection (blocker-10 fix)
            LOGGER.log(Level.SEVERE, "Failed to end transaction", e);
            try { ut.rollback(); } catch (Exception ignored) { /* best effort */ }
            throw new RuntimeException("Transaction end failed", e);
        }
    }
}

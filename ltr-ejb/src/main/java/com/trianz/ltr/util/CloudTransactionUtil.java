package com.trianz.ltr.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * CloudTransactionUtil - Cloud-native transaction management helper using Spring Framework.
 *
 * CLOUD-NATIVE APPROACH:
 *   - Uses Spring's PlatformTransactionManager (works with any cloud environment)
 *   - Supports programmatic transaction management when needed
 *   - Compatible with AWS RDS, Azure SQL, GCP Cloud SQL
 *   - No vendor lock-in (replaces IBM WebSphere UOWManager)
 *
 * RECOMMENDED USAGE:
 *   For most cases, use declarative @Transactional annotation on service methods.
 *   Use this utility only for complex programmatic transaction scenarios.
 *
 * MIGRATION FROM WAS:
 *   - Replaces com.ibm.websphere.uow.UOWManager
 *   - Replaces com.ibm.wsspi.uow.UOWAction
 *   - Standard JTA/Spring transactions work across all cloud platforms
 */
public class CloudTransactionUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudTransactionUtil.class);

    private CloudTransactionUtil() { /* utility class */ }

    /**
     * Execute work within a transaction using Spring's transaction management.
     * 
     * @param transactionManager Spring transaction manager (injected from context)
     * @param action The work to execute within transaction
     * @param requiresNew If true, always starts a new transaction (REQUIRES_NEW semantics)
     * @throws Exception if transaction fails
     */
    public static void executeInTransaction(
            PlatformTransactionManager transactionManager,
            TransactionalAction action,
            boolean requiresNew) throws Exception {

        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        
        if (requiresNew) {
            def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        } else {
            def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        }
        
        def.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        def.setTimeout(30); // 30 seconds timeout for cloud environments

        TransactionStatus status = transactionManager.getTransaction(def);

        try {
            LOGGER.debug("Executing transactional action. RequiresNew={}", requiresNew);
            action.execute();
            transactionManager.commit(status);
            LOGGER.debug("Transaction committed successfully");
        } catch (Exception e) {
            LOGGER.error("Transaction failed, rolling back", e);
            transactionManager.rollback(status);
            throw e;
        }
    }

    /**
     * Execute work within a transaction with default propagation (REQUIRED).
     */
    public static void executeInTransaction(
            PlatformTransactionManager transactionManager,
            TransactionalAction action) throws Exception {
        executeInTransaction(transactionManager, action, false);
    }

    /**
     * Functional interface for transactional work.
     * Replaces IBM's UOWAction.
     */
    @FunctionalInterface
    public interface TransactionalAction {
        void execute() throws Exception;
    }

    /**
     * Helper: Check if a transaction is currently active.
     * Useful for conditional transaction logic.
     */
    public static boolean isTransactionActive(PlatformTransactionManager transactionManager) {
        try {
            DefaultTransactionDefinition def = new DefaultTransactionDefinition();
            def.setPropagationBehavior(TransactionDefinition.PROPAGATION_MANDATORY);
            transactionManager.getTransaction(def);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

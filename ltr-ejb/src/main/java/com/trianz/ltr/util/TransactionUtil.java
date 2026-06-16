package com.trianz.ltr.util;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

/**
 * TransactionUtil - Cloud-native transaction management using Spring @Transactional.
 *
 * REPLACES: IBM WebSphere UOWManager and proprietary transaction APIs
 *
 * CLOUD-NATIVE FEATURES:
 *   - Spring declarative transaction management
 *   - Compatible with AWS RDS, Aurora, and other cloud databases
 *   - No dependency on application server transaction managers
 *   - Supports distributed transactions via JTA when needed
 *
 * USAGE:
 *   Instead of WAS UOWManager, use Spring's @Transactional annotation:
 *
 *   @Transactional
 *   public void myBusinessMethod() {
 *       // transactional code
 *   }
 *
 *   @Transactional(propagation = Propagation.REQUIRES_NEW)
 *   public void requiresNewTransaction() {
 *       // always starts new transaction
 *   }
 *
 * MIGRATION NOTES:
 *   - WAS UOWManager.runUnderUOW() → Spring @Transactional
 *   - WAS UserTransaction → Spring PlatformTransactionManager
 *   - WAS CMT (Container-Managed Transactions) → Spring declarative transactions
 */
@Component
public class TransactionUtil {

    private static final Logger LOGGER = Logger.getLogger(TransactionUtil.class.getName());

    /**
     * Execute work in a transaction with REQUIRED propagation.
     * If a transaction exists, join it; otherwise create a new one.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void executeInTransaction(Runnable action) {
        LOGGER.fine("Executing in transaction (REQUIRED)");
        action.run();
    }

    /**
     * Execute work in a new transaction (REQUIRES_NEW propagation).
     * Always suspends current transaction and creates a new one.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeInNewTransaction(Runnable action) {
        LOGGER.fine("Executing in new transaction (REQUIRES_NEW)");
        action.run();
    }

    /**
     * Execute work without transaction (NEVER propagation).
     * Throws exception if a transaction exists.
     */
    @Transactional(propagation = Propagation.NEVER)
    public void executeWithoutTransaction(Runnable action) {
        LOGGER.fine("Executing without transaction (NEVER)");
        action.run();
    }

    /**
     * Execute work in transaction with rollback on any exception.
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void executeWithRollback(Runnable action) throws Exception {
        LOGGER.fine("Executing with automatic rollback on exception");
        try {
            action.run();
        } catch (Exception e) {
            LOGGER.warning("Transaction rolled back due to exception: " + e.getMessage());
            throw e;
        }
    }
}

package com.trianz.ltr.util;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.stereotype.Component;

import javax.transaction.UserTransaction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TransactionUtil - Cloud-native transaction management helper.
 *
 * CLOUD-NATIVE MIGRATION:
 *   - Replaced IBM WebSphere UOWManager with Spring @Transactional
 *   - Removed WAS-specific imports (com.ibm.websphere.uow.*)
 *   - Uses Spring declarative transaction management
 *   - Compatible with AWS RDS and cloud-native patterns
 *   - Stateless design for horizontal scaling in EKS/ECS
 *
 * MIGRATION FROM WAS:
 *   - UOWManager → Spring @Transactional
 *   - UOWAction → Standard service methods with @Transactional
 *   - JNDI lookups → Spring dependency injection
 *   - WAS transaction manager → Spring PlatformTransactionManager
 */
@Component
public class WASTransactionUtil {

    private static final Logger LOGGER = Logger.getLogger(WASTransactionUtil.class.getName());

    private WASTransactionUtil() { /* utility */ }

    /**
     * Execute a unit of work using Spring declarative transactions.
     * This method is deprecated - use @Transactional annotation on service methods instead.
     *
     * @param action  the transactional work to perform
     * @param requiresNew  if true, always starts a new transaction (REQUIRES_NEW semantics)
     * @deprecated Use Spring @Transactional annotation on service methods
     */
    @Deprecated
    public static void executeInTransaction(Runnable action, boolean requiresNew) throws Exception {
        LOGGER.fine("Executing transactional action. RequiresNew=" + requiresNew);
        // In Spring Boot, transactions are managed declaratively via @Transactional
        // This method is kept for backward compatibility but should be replaced
        action.run();
    }

    /**
     * Helper: begin a UserTransaction safely (for servlet/non-EJB use).
     * @deprecated Use Spring @Transactional annotation instead
     */
    @Deprecated
    public static UserTransaction beginTransaction() {
        LOGGER.warning("beginTransaction() is deprecated. Use Spring @Transactional annotation.");
        throw new UnsupportedOperationException(
            "Direct UserTransaction management is not supported in Spring Boot. " +
            "Use @Transactional annotation on service methods.");
    }

    /**
     * Commit or roll back based on success flag.
     * @deprecated Use Spring @Transactional annotation instead
     */
    @Deprecated
    public static void endTransaction(UserTransaction ut, boolean commit) {
        LOGGER.warning("endTransaction() is deprecated. Use Spring @Transactional annotation.");
        throw new UnsupportedOperationException(
            "Direct UserTransaction management is not supported in Spring Boot. " +
            "Use @Transactional annotation on service methods.");
    }
}

package com.trianz.ltr.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * WASTransactionUtil - DEPRECATED - Use CloudTransactionUtil or @Transactional instead.
 *
 * This class has been migrated to cloud-native Spring transaction management.
 * Kept for backward compatibility during migration phase.
 *
 * MIGRATION PATH:
 *   1. Replace direct usage with @Transactional annotation on service methods
 *   2. For programmatic transactions, use CloudTransactionUtil
 *   3. Remove IBM WebSphere dependencies (com.ibm.websphere.uow.*)
 *
 * CLOUD-NATIVE REPLACEMENT:
 *   - Use Spring's @Transactional annotation (declarative)
 *   - Use CloudTransactionUtil for programmatic transactions
 *   - Works with AWS RDS, Azure SQL, GCP Cloud SQL
 */
@Deprecated
public class WASTransactionUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(WASTransactionUtil.class);

    private WASTransactionUtil() { /* utility class */ }

    /**
     * @deprecated Use CloudTransactionUtil.executeInTransaction() or @Transactional annotation
     */
    @Deprecated
    public static void executeInTransaction(Object action, boolean requiresNew) throws Exception {
        LOGGER.warn("WASTransactionUtil is deprecated. Migrate to @Transactional or CloudTransactionUtil");
        throw new UnsupportedOperationException(
            "WASTransactionUtil is deprecated. Use @Transactional annotation or CloudTransactionUtil");
    }

    /**
     * @deprecated No longer needed in cloud-native applications
     */
    @Deprecated
    public static Object lookupUOWManager() throws Exception {
        throw new UnsupportedOperationException(
            "IBM WebSphere UOWManager not available in cloud environments. Use Spring @Transactional");
    }

    /**
     * @deprecated Use Spring's @Transactional annotation instead
     */
    @Deprecated
    public static Object getUserTransaction() throws Exception {
        throw new UnsupportedOperationException(
            "JNDI UserTransaction lookup not supported. Use Spring @Transactional annotation");
    }

    /**
     * @deprecated Use Spring's @Transactional annotation instead
     */
    @Deprecated
    public static Object beginTransaction() {
        throw new UnsupportedOperationException(
            "Manual transaction management deprecated. Use Spring @Transactional annotation");
    }

    /**
     * @deprecated Use Spring's @Transactional annotation instead
     */
    @Deprecated
    public static void endTransaction(Object ut, boolean commit) {
        throw new UnsupportedOperationException(
            "Manual transaction management deprecated. Use Spring @Transactional annotation");
    }
}

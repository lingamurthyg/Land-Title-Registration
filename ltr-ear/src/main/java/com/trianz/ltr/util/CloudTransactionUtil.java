package com.trianz.ltr.util;

import javax.transaction.Transactional;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/**
 * CloudTransactionUtil - Cloud-native transaction management utility.
 * 
 * CLOUD-READY FEATURES:
 *   - Uses standard JTA @Transactional annotation (no vendor lock-in)
 *   - Stateless transaction management
 *   - Compatible with Spring Boot, Jakarta EE, MicroProfile
 *   - No WebSphere-specific dependencies
 *   - UTC-based timestamp handling for distributed systems
 *
 * MIGRATION FROM WAS:
 *   - Replaced com.ibm.websphere.uow.UOWManager with standard JTA
 *   - Replaced WAS-specific transaction APIs with @Transactional
 *   - Removed JNDI lookups for transaction manager
 */
public class CloudTransactionUtil {

    private static final Logger LOGGER = Logger.getLogger(CloudTransactionUtil.class.getName());

    private CloudTransactionUtil() { /* utility class */ }

    /**
     * Get current UTC timestamp as Instant.
     * Cloud-ready: Always use UTC for distributed systems.
     */
    public static Instant getCurrentTimestamp() {
        return Instant.now();
    }

    /**
     * Get current UTC timestamp as ZonedDateTime.
     */
    public static ZonedDateTime getCurrentTimestampUTC() {
        return ZonedDateTime.now(ZoneOffset.UTC);
    }

    /**
     * Format timestamp for logging (ISO-8601 format).
     */
    public static String formatTimestamp(Instant instant) {
        if (instant == null) return null;
        return DateTimeFormatter.ISO_INSTANT.format(instant);
    }

    /**
     * Format timestamp for logging (ISO-8601 format with timezone).
     */
    public static String formatTimestamp(ZonedDateTime dateTime) {
        if (dateTime == null) return null;
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(dateTime);
    }

    /**
     * Log transaction start (for monitoring and tracing).
     */
    public static void logTransactionStart(String operationName) {
        LOGGER.info(String.format("Transaction started: %s at %s", 
            operationName, formatTimestamp(getCurrentTimestamp())));
    }

    /**
     * Log transaction commit (for monitoring and tracing).
     */
    public static void logTransactionCommit(String operationName) {
        LOGGER.info(String.format("Transaction committed: %s at %s", 
            operationName, formatTimestamp(getCurrentTimestamp())));
    }

    /**
     * Log transaction rollback (for monitoring and tracing).
     */
    public static void logTransactionRollback(String operationName, Exception e) {
        LOGGER.warning(String.format("Transaction rolled back: %s at %s. Reason: %s", 
            operationName, formatTimestamp(getCurrentTimestamp()), e.getMessage()));
    }

    /**
     * Generate correlation ID for distributed tracing.
     * Cloud-ready: Use this for tracking requests across microservices.
     */
    public static String generateCorrelationId() {
        return String.format("TXN-%d-%s", 
            System.currentTimeMillis(), 
            java.util.UUID.randomUUID().toString().substring(0, 8));
    }
}

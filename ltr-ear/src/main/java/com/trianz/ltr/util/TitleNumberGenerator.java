package com.trianz.ltr.util;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TitleNumberGenerator - Generates unique land title registration numbers.
 * 
 * CLOUD-READY FEATURES:
 *   - Uses java.time.Instant instead of java.util.Date (timezone-safe)
 *   - UTC-based timestamp generation
 *   - Thread-safe atomic counter
 *   - Stateless design for horizontal scaling
 *
 * Format: LTR-{YYYY}-{REGION_CODE}-{SEQUENCE}
 * Example: LTR-2024-NRB-000042
 *
 * In production, the sequence should be persisted in the database or use
 * a distributed sequence generator (e.g., AWS RDS sequences, Redis counter).
 */
public class TitleNumberGenerator {

    private static final AtomicLong sequence = new AtomicLong(1000L);
    private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy");

    private TitleNumberGenerator() { /* utility */ }

    /**
     * Generate a title number using a provided sequence (DB-sourced in production).
     * Cloud-ready: Uses UTC time for consistency across regions.
     *
     * @param regionCode  2-5 char region/district code e.g. "NRB", "KSM", "MBS"
     * @param seqNumber   DB sequence value
     * @return formatted title number
     */
    public static String generate(String regionCode, long seqNumber) {
        // Use UTC time for consistency across cloud regions
        String year = YEAR_FORMATTER.format(Instant.now().atZone(ZoneOffset.UTC));
        return String.format("LTR-%s-%s-%06d", year, regionCode.toUpperCase(), seqNumber);
    }

    /**
     * Generate using the in-memory atomic counter (for testing/demo).
     * Cloud-ready: Thread-safe for concurrent requests.
     */
    public static String generateLocal(String regionCode) {
        return generate(regionCode, sequence.getAndIncrement());
    }

    /**
     * Validate title number format.
     */
    public static boolean isValid(String titleNumber) {
        return titleNumber != null && titleNumber.matches("LTR-\\d{4}-[A-Z]{2,5}-\\d{6}");
    }

    /**
     * Get current UTC timestamp as ISO-8601 string.
     * Cloud-ready: Always use UTC for distributed systems.
     */
    public static String getCurrentTimestampUTC() {
        return Instant.now().toString();
    }
}

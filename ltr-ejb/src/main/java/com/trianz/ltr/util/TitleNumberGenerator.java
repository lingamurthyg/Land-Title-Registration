package com.trianz.ltr.util;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TitleNumberGenerator - Generates unique land title registration numbers.
 * 
 * Cloud-native improvements:
 * - Uses java.time API (Instant) instead of java.util.Date for timezone safety
 * - All timestamps in UTC to avoid timezone issues in distributed cloud environments
 * - Thread-safe atomic counter for high-concurrency scenarios
 * 
 * Format: LTR-{YYYY}-{REGION_CODE}-{SEQUENCE}
 * Example: LTR-2024-NRB-000042
 * 
 * In production, the sequence should be persisted in the database or use a distributed
 * sequence generator (e.g., AWS DynamoDB atomic counters, Redis INCR).
 */
public class TitleNumberGenerator {

    private static final AtomicLong sequence = new AtomicLong(1000L);
    private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy")
        .withZone(ZoneOffset.UTC);

    private TitleNumberGenerator() { /* utility */ }

    /**
     * Generate a title number using a provided sequence (DB-sourced in production).
     * 
     * @param regionCode  2-5 char region/district code e.g. "NRB", "KSM", "MBS"
     * @param seqNumber   DB sequence value
     * @return formatted title number
     */
    public static String generate(String regionCode, long seqNumber) {
        // Use UTC time to avoid timezone issues in cloud deployments
        Instant now = Instant.now();
        String year = YEAR_FORMATTER.format(now);
        return String.format("LTR-%s-%s-%06d", year, regionCode.toUpperCase(), seqNumber);
    }

    /**
     * Generate using the in-memory atomic counter (for testing/demo).
     * In production, replace with database sequence or distributed counter.
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
     * Reset the local sequence counter (for testing only).
     */
    public static void resetSequence(long startValue) {
        sequence.set(startValue);
    }
}

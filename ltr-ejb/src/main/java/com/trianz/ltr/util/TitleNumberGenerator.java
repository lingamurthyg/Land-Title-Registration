package com.trianz.ltr.util;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TitleNumberGenerator - Generates unique land title registration numbers.
 *
 * Format: LTR-{YYYY}-{REGION_CODE}-{SEQUENCE}
 * Example: LTR-2024-NRB-000042
 *
 * CLOUD-NATIVE MIGRATION:
 *   - Replaced java.util.Date with java.time.Instant for UTC timestamps
 *   - Standardized on UTC timezone for cloud deployments
 *   - Thread-safe atomic counter for distributed environments
 *
 * In production the sequence is persisted in the DB (managed by the service layer).
 * This class provides a local atomic counter for single-node or testing scenarios.
 */
public class TitleNumberGenerator {

    private static final AtomicLong sequence = new AtomicLong(1000L);
    private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy").withZone(ZoneOffset.UTC);

    private TitleNumberGenerator() { /* utility */ }

    /**
     * Generate a title number using a provided sequence (DB-sourced in production).
     *
     * @param regionCode  2-5 char region/district code e.g. "NRB", "KSM", "MBS"
     * @param seqNumber   DB sequence value
     * @return formatted title number
     */
    public static String generate(String regionCode, long seqNumber) {
        String year = YEAR_FORMATTER.format(Instant.now());
        return String.format("LTR-%s-%s-%06d", year, regionCode.toUpperCase(), seqNumber);
    }

    /**
     * Generate using the in-memory atomic counter (for testing/demo).
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
}

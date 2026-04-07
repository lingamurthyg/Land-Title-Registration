package com.trianz.ltr;

import com.trianz.ltr.ejb.LandTitleException;
import com.trianz.ltr.model.LandTitle;
import com.trianz.ltr.model.LandTitle.TitleStatus;
import com.trianz.ltr.model.LandTitle.LandUseType;
import com.trianz.ltr.model.TitleTransfer;
import com.trianz.ltr.model.TitleTransfer.TransferType;
import com.trianz.ltr.util.TitleNumberGenerator;

import org.junit.Test;
import org.junit.Before;

import java.math.BigDecimal;

import static org.junit.Assert.*;

/**
 * Unit tests for LandTitle domain model, transfer model, and utilities.
 * NOTE: EJB container tests require a running WAS/OpenEJB environment.
 *       These tests cover model behaviour and utility classes only.
 */
public class LandTitleRegistryTest {

    private LandTitle sampleTitle;
    private TitleTransfer sampleTransfer;

    @Before
    public void setUp() {
        sampleTitle = new LandTitle();
        sampleTitle.setParcelId("PCL-TEST-0001-2024");
        sampleTitle.setOwnerNationalId("KE-NID-1990-112233");
        sampleTitle.setOwnerFullName("Alice Wanjiru Kamau");
        sampleTitle.setOwnerContactEmail("alice@example.co.ke");
        sampleTitle.setStreetAddress("25 Garden Estate Road");
        sampleTitle.setCity("Nairobi");
        sampleTitle.setCountry("Kenya");
        sampleTitle.setAreaSquareMetres(new BigDecimal("350.00"));
        sampleTitle.setLandUseType(LandUseType.RESIDENTIAL);
        sampleTitle.setAssessedValue(new BigDecimal("3500000.00"));
        sampleTitle.setCurrencyCode("KES");

        sampleTransfer = new TitleTransfer();
        sampleTransfer.setTitleNumber("LTR-2024-REG-001000");
        sampleTransfer.setNewOwnerNationalId("KE-NID-1985-556677");
        sampleTransfer.setNewOwnerName("Bob Otieno Odhiambo");
        sampleTransfer.setTransferType(TransferType.SALE);
        sampleTransfer.setTransferPrice(new BigDecimal("4800000.00"));
        sampleTransfer.setCurrencyCode("KES");
        sampleTransfer.setStampDutyPaid(new BigDecimal("192000.00")); // 4% stamp duty
    }

    // ── Model Tests ────────────────────────────────────────────────────────────

    @Test
    public void testNewTitleDefaultStatus() {
        LandTitle title = new LandTitle();
        assertEquals("New title should default to PENDING", TitleStatus.PENDING, title.getStatus());
    }

    @Test
    public void testNewTitleDefaultCurrency() {
        LandTitle title = new LandTitle();
        assertEquals("Default currency should be USD", "USD", title.getCurrencyCode());
    }

    @Test
    public void testNewTitleNoEncumbrance() {
        LandTitle title = new LandTitle();
        assertFalse("New title should have no lien", title.isHasLien());
        assertFalse("New title should have no mortgage", title.isHasMortgage());
    }

    @Test
    public void testTitleSettersAndGetters() {
        assertEquals("PCL-TEST-0001-2024", sampleTitle.getParcelId());
        assertEquals("KE-NID-1990-112233", sampleTitle.getOwnerNationalId());
        assertEquals("Alice Wanjiru Kamau", sampleTitle.getOwnerFullName());
        assertEquals(LandUseType.RESIDENTIAL, sampleTitle.getLandUseType());
        assertEquals(new BigDecimal("350.00"), sampleTitle.getAreaSquareMetres());
        assertEquals("KES", sampleTitle.getCurrencyCode());
    }

    @Test
    public void testTitleToString() {
        sampleTitle.setTitleNumber("LTR-2024-REG-001000");
        sampleTitle.setStatus(TitleStatus.ACTIVE);
        String str = sampleTitle.toString();
        assertTrue("toString should contain title number", str.contains("LTR-2024-REG-001000"));
        assertTrue("toString should contain owner", str.contains("Alice Wanjiru Kamau"));
        assertTrue("toString should contain status", str.contains("ACTIVE"));
    }

    @Test
    public void testEncumbranceFlags() {
        sampleTitle.setHasLien(true);
        sampleTitle.setHasMortgage(true);
        sampleTitle.setEncumbranceDetails("Mortgage to Kenya Commercial Bank; Lien by Nairobi County");
        assertTrue(sampleTitle.isHasLien());
        assertTrue(sampleTitle.isHasMortgage());
        assertNotNull(sampleTitle.getEncumbranceDetails());
    }

    // ── Transfer Model Tests ───────────────────────────────────────────────────

    @Test
    public void testTransferDefaults() {
        TitleTransfer tr = new TitleTransfer();
        assertEquals("New transfer defaults to INITIATED",
                TitleTransfer.TransferStatus.INITIATED, tr.getTransferStatus());
        assertEquals("USD", tr.getCurrencyCode());
        assertNotNull("Transfer date should be set", tr.getTransferDate());
    }

    @Test
    public void testTransferFields() {
        assertEquals("LTR-2024-REG-001000", sampleTransfer.getTitleNumber());
        assertEquals("Bob Otieno Odhiambo", sampleTransfer.getNewOwnerName());
        assertEquals(TransferType.SALE, sampleTransfer.getTransferType());
        assertEquals(new BigDecimal("4800000.00"), sampleTransfer.getTransferPrice());
        assertEquals(new BigDecimal("192000.00"), sampleTransfer.getStampDutyPaid());
    }

    // ── TitleNumberGenerator Tests ─────────────────────────────────────────────

    @Test
    public void testGenerateFormat() {
        String num = TitleNumberGenerator.generate("NRB", 42L);
        assertTrue("Should match pattern LTR-YYYY-NRB-000042",
                num.matches("LTR-\\d{4}-NRB-000042"));
    }

    @Test
    public void testGeneratePaddingZeros() {
        String num = TitleNumberGenerator.generate("KSM", 7L);
        assertTrue("Should be padded to 6 digits", num.endsWith("-000007"));
    }

    @Test
    public void testGenerateLocalIncrement() {
        String first  = TitleNumberGenerator.generateLocal("REG");
        String second = TitleNumberGenerator.generateLocal("REG");
        assertNotEquals("Sequential calls should produce different numbers", first, second);
    }

    @Test
    public void testValidTitleNumber() {
        assertTrue(TitleNumberGenerator.isValid("LTR-2024-NRB-000042"));
        assertTrue(TitleNumberGenerator.isValid("LTR-2025-REG-999999"));
    }

    @Test
    public void testInvalidTitleNumber() {
        assertFalse(TitleNumberGenerator.isValid(null));
        assertFalse(TitleNumberGenerator.isValid(""));
        assertFalse(TitleNumberGenerator.isValid("LTR-24-NRB-42"));
        assertFalse(TitleNumberGenerator.isValid("INVALID-FORMAT"));
    }

    // ── LandTitleException Tests ───────────────────────────────────────────────

    @Test
    public void testLandTitleExceptionCodes() {
        LandTitleException e = new LandTitleException(
                LandTitleException.ErrorCode.TITLE_NOT_FOUND,
                "Title not found: LTR-2024-REG-000001");
        assertEquals(LandTitleException.ErrorCode.TITLE_NOT_FOUND, e.getErrorCode());
        assertTrue(e.getMessage().contains("LTR-2024-REG-000001"));
        assertTrue(e.toString().contains("TITLE_NOT_FOUND"));
    }

    @Test
    public void testLandTitleExceptionWithCause() {
        RuntimeException cause = new RuntimeException("DB connection failed");
        LandTitleException e = new LandTitleException(
                LandTitleException.ErrorCode.DATA_ACCESS_ERROR,
                "Database error", cause);
        assertSame(cause, e.getCause());
    }

    // ── Business Logic Validation Tests ───────────────────────────────────────

    @Test
    public void testStampDutyCalculation() {
        // Stamp duty at 4% of transfer price
        BigDecimal price = sampleTransfer.getTransferPrice();
        BigDecimal expectedDuty = price.multiply(new BigDecimal("0.04"));
        assertEquals("Stamp duty should be 4% of transfer price",
                0, expectedDuty.compareTo(sampleTransfer.getStampDutyPaid()));
    }

    @Test
    public void testTitleStatusTransitions() {
        sampleTitle.setStatus(TitleStatus.ACTIVE);
        assertEquals(TitleStatus.ACTIVE, sampleTitle.getStatus());

        sampleTitle.setStatus(TitleStatus.PENDING);
        assertEquals(TitleStatus.PENDING, sampleTitle.getStatus());

        sampleTitle.setStatus(TitleStatus.TRANSFERRED);
        assertEquals(TitleStatus.TRANSFERRED, sampleTitle.getStatus());
    }
}

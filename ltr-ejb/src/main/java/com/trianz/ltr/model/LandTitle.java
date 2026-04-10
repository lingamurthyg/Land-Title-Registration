package com.trianz.ltr.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * LandTitle - Core domain entity representing a registered land parcel.
 * 
 * Cloud-native improvements:
 * - Uses java.time.Instant instead of java.util.Date for timezone safety
 * - All timestamps stored in UTC for distributed cloud environments
 * - Jackson annotations for proper JSON serialization
 * - Serializable for distributed caching (Redis, ElastiCache)
 */
public class LandTitle implements Serializable {

    private static final long serialVersionUID = 2L; // Incremented due to field type changes

    public enum TitleStatus {
        ACTIVE, PENDING, TRANSFERRED, ENCUMBERED, CANCELLED
    }

    public enum LandUseType {
        RESIDENTIAL, COMMERCIAL, AGRICULTURAL, INDUSTRIAL, MIXED_USE, GOVERNMENT
    }

    // ── Primary fields ─────────────────────────────────────────────────────────

    /** Unique title number assigned by the registry (e.g. LTR-2024-00123) */
    private String titleNumber;

    /** Survey/parcel identifier from the surveyor's records */
    private String parcelId;

    /** Full legal description of the land */
    private String legalDescription;

    /** Land area in square metres */
    private BigDecimal areaSquareMetres;

    /** Current registration status */
    private TitleStatus status;

    /** Intended land use classification */
    private LandUseType landUseType;

    // ── Location ───────────────────────────────────────────────────────────────

    private String streetAddress;
    private String suburb;
    private String city;
    private String stateProvince;
    private String country;
    private String postalCode;

    /** GPS coordinates – stored as decimal degrees */
    private BigDecimal latitude;
    private BigDecimal longitude;

    // ── Ownership ──────────────────────────────────────────────────────────────

    private String ownerNationalId;    // FK to Owner table
    private String ownerFullName;
    private String ownerContactEmail;
    private String ownerContactPhone;

    // ── Valuation ──────────────────────────────────────────────────────────────

    private BigDecimal assessedValue;
    private BigDecimal marketValue;
    private String currencyCode;       // ISO 4217 e.g. "USD", "KES", "ZAR"

    // ── Audit (Cloud-native: UTC timestamps) ───────────────────────────────────

    @JsonSerialize(using = InstantSerializer.class)
    @JsonDeserialize(using = InstantDeserializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant registrationDate;

    @JsonSerialize(using = InstantSerializer.class)
    @JsonDeserialize(using = InstantDeserializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant lastModifiedDate;

    private String registeredBy;       // User principal name
    private String lastModifiedBy;
    private String remarks;

    // ── Encumbrance ────────────────────────────────────────────────────────────

    private boolean hasLien;
    private boolean hasMortgage;
    private String encumbranceDetails;

    // ── Constructors ───────────────────────────────────────────────────────────

    public LandTitle() {
        this.status = TitleStatus.PENDING;
        this.registrationDate = Instant.now();
        this.currencyCode = "USD";
        this.hasLien = false;
        this.hasMortgage = false;
    }

    public LandTitle(String titleNumber, String parcelId, String ownerNationalId) {
        this();
        this.titleNumber = titleNumber;
        this.parcelId = parcelId;
        this.ownerNationalId = ownerNationalId;
    }

    // ── Getters / Setters ──────────────────────────────────────────────────────

    public String getTitleNumber()                        { return titleNumber; }
    public void   setTitleNumber(String v)                { this.titleNumber = v; }

    public String getParcelId()                           { return parcelId; }
    public void   setParcelId(String v)                   { this.parcelId = v; }

    public String getLegalDescription()                   { return legalDescription; }
    public void   setLegalDescription(String v)           { this.legalDescription = v; }

    public BigDecimal getAreaSquareMetres()               { return areaSquareMetres; }
    public void       setAreaSquareMetres(BigDecimal v)   { this.areaSquareMetres = v; }

    public TitleStatus getStatus()                        { return status; }
    public void        setStatus(TitleStatus v)           { this.status = v; }

    public LandUseType getLandUseType()                   { return landUseType; }
    public void        setLandUseType(LandUseType v)      { this.landUseType = v; }

    public String getStreetAddress()                      { return streetAddress; }
    public void   setStreetAddress(String v)              { this.streetAddress = v; }

    public String getSuburb()                             { return suburb; }
    public void   setSuburb(String v)                     { this.suburb = v; }

    public String getCity()                               { return city; }
    public void   setCity(String v)                       { this.city = v; }

    public String getStateProvince()                      { return stateProvince; }
    public void   setStateProvince(String v)              { this.stateProvince = v; }

    public String getCountry()                            { return country; }
    public void   setCountry(String v)                    { this.country = v; }

    public String getPostalCode()                         { return postalCode; }
    public void   setPostalCode(String v)                 { this.postalCode = v; }

    public BigDecimal getLatitude()                       { return latitude; }
    public void       setLatitude(BigDecimal v)           { this.latitude = v; }

    public BigDecimal getLongitude()                      { return longitude; }
    public void       setLongitude(BigDecimal v)          { this.longitude = v; }

    public String getOwnerNationalId()                    { return ownerNationalId; }
    public void   setOwnerNationalId(String v)            { this.ownerNationalId = v; }

    public String getOwnerFullName()                      { return ownerFullName; }
    public void   setOwnerFullName(String v)              { this.ownerFullName = v; }

    public String getOwnerContactEmail()                  { return ownerContactEmail; }
    public void   setOwnerContactEmail(String v)          { this.ownerContactEmail = v; }

    public String getOwnerContactPhone()                  { return ownerContactPhone; }
    public void   setOwnerContactPhone(String v)          { this.ownerContactPhone = v; }

    public BigDecimal getAssessedValue()                  { return assessedValue; }
    public void       setAssessedValue(BigDecimal v)      { this.assessedValue = v; }

    public BigDecimal getMarketValue()                    { return marketValue; }
    public void       setMarketValue(BigDecimal v)        { this.marketValue = v; }

    public String getCurrencyCode()                       { return currencyCode; }
    public void   setCurrencyCode(String v)               { this.currencyCode = v; }

    public Instant getRegistrationDate()                  { return registrationDate; }
    public void setRegistrationDate(Instant v)            { this.registrationDate = v; }

    public Instant getLastModifiedDate()                  { return lastModifiedDate; }
    public void setLastModifiedDate(Instant v)            { this.lastModifiedDate = v; }

    public String getRegisteredBy()                       { return registeredBy; }
    public void   setRegisteredBy(String v)               { this.registeredBy = v; }

    public String getLastModifiedBy()                     { return lastModifiedBy; }
    public void   setLastModifiedBy(String v)             { this.lastModifiedBy = v; }

    public String getRemarks()                            { return remarks; }
    public void   setRemarks(String v)                    { this.remarks = v; }

    public boolean isHasLien()                            { return hasLien; }
    public void    setHasLien(boolean v)                  { this.hasLien = v; }

    public boolean isHasMortgage()                        { return hasMortgage; }
    public void    setHasMortgage(boolean v)              { this.hasMortgage = v; }

    public String getEncumbranceDetails()                 { return encumbranceDetails; }
    public void   setEncumbranceDetails(String v)         { this.encumbranceDetails = v; }

    @Override
    public String toString() {
        return "LandTitle{titleNumber='" + titleNumber + "', parcel='" + parcelId
                + "', owner='" + ownerFullName + "', status=" + status + "}";
    }
}

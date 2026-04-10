package com.trianz.ltr.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * TitleTransfer - Records the chain of ownership for a land title.
 * Each row in TITLE_TRANSFER_HISTORY represents one transfer event.
 * 
 * CLOUD-READY FEATURES:
 *   - Uses java.time.Instant instead of java.util.Date (timezone-safe, UTC-based)
 *   - Serializable for distributed caching
 *   - No vendor-specific dependencies
 */
public class TitleTransfer implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum TransferType {
        SALE, INHERITANCE, DONATION, COURT_ORDER, GOVERNMENT_ACQUISITION, CORRECTION
    }

    public enum TransferStatus {
        INITIATED, DOCUMENTS_PENDING, UNDER_REVIEW, APPROVED, REJECTED, COMPLETED
    }

    private Long   transferId;
    private String titleNumber;

    // Previous owner
    private String previousOwnerNationalId;
    private String previousOwnerName;

    // New owner
    private String newOwnerNationalId;
    private String newOwnerName;
    private String newOwnerContactEmail;
    private String newOwnerContactPhone;

    private TransferType   transferType;
    private TransferStatus transferStatus;

    private BigDecimal transferPrice;
    private String     currencyCode;
    private BigDecimal stampDutyPaid;

    // Cloud-ready: using Instant for UTC timestamps
    private Instant transferDate;
    private Instant effectiveDate;
    private String  transferDeedNumber;
    private String  notaryNationalId;
    private String  notaryName;

    private String  initiatedBy;
    private String  approvedBy;
    private Instant approvedDate;
    private String  rejectionReason;
    private String  remarks;

    // ── Constructors ───────────────────────────────────────────────────────────

    public TitleTransfer() {
        this.transferDate  = Instant.now();
        this.transferStatus = TransferStatus.INITIATED;
        this.currencyCode  = "USD";
    }

    // ── Getters / Setters ──────────────────────────────────────────────────────

    public Long getTransferId()                           { return transferId; }
    public void setTransferId(Long v)                     { this.transferId = v; }

    public String getTitleNumber()                        { return titleNumber; }
    public void   setTitleNumber(String v)                { this.titleNumber = v; }

    public String getPreviousOwnerNationalId()            { return previousOwnerNationalId; }
    public void   setPreviousOwnerNationalId(String v)    { this.previousOwnerNationalId = v; }

    public String getPreviousOwnerName()                  { return previousOwnerName; }
    public void   setPreviousOwnerName(String v)          { this.previousOwnerName = v; }

    public String getNewOwnerNationalId()                 { return newOwnerNationalId; }
    public void   setNewOwnerNationalId(String v)         { this.newOwnerNationalId = v; }

    public String getNewOwnerName()                       { return newOwnerName; }
    public void   setNewOwnerName(String v)               { this.newOwnerName = v; }

    public String getNewOwnerContactEmail()               { return newOwnerContactEmail; }
    public void   setNewOwnerContactEmail(String v)       { this.newOwnerContactEmail = v; }

    public String getNewOwnerContactPhone()               { return newOwnerContactPhone; }
    public void   setNewOwnerContactPhone(String v)       { this.newOwnerContactPhone = v; }

    public TransferType getTransferType()                 { return transferType; }
    public void         setTransferType(TransferType v)   { this.transferType = v; }

    public TransferStatus getTransferStatus()             { return transferStatus; }
    public void           setTransferStatus(TransferStatus v) { this.transferStatus = v; }

    public BigDecimal getTransferPrice()                  { return transferPrice; }
    public void       setTransferPrice(BigDecimal v)      { this.transferPrice = v; }

    public String getCurrencyCode()                       { return currencyCode; }
    public void   setCurrencyCode(String v)               { this.currencyCode = v; }

    public BigDecimal getStampDutyPaid()                  { return stampDutyPaid; }
    public void       setStampDutyPaid(BigDecimal v)      { this.stampDutyPaid = v; }

    public Instant getTransferDate()                      { return transferDate; }
    public void    setTransferDate(Instant v)             { this.transferDate = v; }

    public Instant getEffectiveDate()                     { return effectiveDate; }
    public void    setEffectiveDate(Instant v)            { this.effectiveDate = v; }

    public String getTransferDeedNumber()                 { return transferDeedNumber; }
    public void   setTransferDeedNumber(String v)         { this.transferDeedNumber = v; }

    public String getNotaryNationalId()                   { return notaryNationalId; }
    public void   setNotaryNationalId(String v)           { this.notaryNationalId = v; }

    public String getNotaryName()                         { return notaryName; }
    public void   setNotaryName(String v)                 { this.notaryName = v; }

    public String getInitiatedBy()                        { return initiatedBy; }
    public void   setInitiatedBy(String v)                { this.initiatedBy = v; }

    public String getApprovedBy()                         { return approvedBy; }
    public void   setApprovedBy(String v)                 { this.approvedBy = v; }

    public Instant getApprovedDate()                      { return approvedDate; }
    public void    setApprovedDate(Instant v)             { this.approvedDate = v; }

    public String getRejectionReason()                    { return rejectionReason; }
    public void   setRejectionReason(String v)            { this.rejectionReason = v; }

    public String getRemarks()                            { return remarks; }
    public void   setRemarks(String v)                    { this.remarks = v; }
}

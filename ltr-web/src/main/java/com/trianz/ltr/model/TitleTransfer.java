package com.trianz.ltr.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * TitleTransfer - Entity representing a title transfer transaction
 *
 * Migrated from EJB entity to JPA entity with Spring Data.
 * Replaces java.util.Date with java.time.Instant for UTC consistency.
 *
 * Cloud-Ready Features:
 * - JPA entity for AWS RDS PostgreSQL
 * - UTC timestamp using java.time.Instant
 * - Jackson JSON serialization
 * - Bean validation annotations
 * - Lombok for reduced boilerplate
 *
 * @author Cloud Migration Team
 * @version 2.0.0-cloud
 */
@Entity
@Table(name = "title_transfers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TitleTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transfer_id")
    private Long transferId;

    @Column(name = "title_number", length = 50)
    @NotBlank(message = "Title number is required")
    private String titleNumber;

    @Column(name = "from_owner_id", length = 50)
    @NotBlank(message = "From owner ID is required")
    private String fromOwnerId;

    @Column(name = "from_owner_name", length = 200)
    private String fromOwnerName;

    @Column(name = "to_owner_id", length = 50)
    @NotBlank(message = "To owner ID is required")
    private String toOwnerId;

    @Column(name = "to_owner_name", length = 200)
    @NotBlank(message = "To owner name is required")
    private String toOwnerName;

    @Column(name = "transfer_amount", precision = 15, scale = 2)
    private BigDecimal transferAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @NotNull(message = "Status is required")
    private TransferStatus status;

    @Column(name = "initiated_date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant initiatedDate;

    @Column(name = "processed_date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant processedDate;

    @Column(name = "processed_by", length = 100)
    private String processedBy;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "notes", length = 1000)
    private String notes;

    /**
     * Transfer status enumeration
     */
    public enum TransferStatus {
        PENDING,
        APPROVED,
        REJECTED,
        COMPLETED,
        CANCELLED
    }

    /**
     * Pre-persist callback to set timestamps
     */
    @PrePersist
    protected void onCreate() {
        if (initiatedDate == null) {
            initiatedDate = Instant.now();
        }
        if (status == null) {
            status = TransferStatus.PENDING;
        }
    }

    /**
     * Pre-update callback
     */
    @PreUpdate
    protected void onUpdate() {
        if (status == TransferStatus.APPROVED || status == TransferStatus.REJECTED) {
            if (processedDate == null) {
                processedDate = Instant.now();
            }
        }
    }
}

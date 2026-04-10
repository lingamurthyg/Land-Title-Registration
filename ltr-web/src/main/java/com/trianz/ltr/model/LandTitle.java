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
 * LandTitle - Entity representing a land title record
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
@Table(name = "land_titles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LandTitle {

    @Id
    @Column(name = "title_number", length = 50)
    @NotBlank(message = "Title number is required")
    private String titleNumber;

    @Column(name = "parcel_id", length = 50, unique = true)
    @NotBlank(message = "Parcel ID is required")
    private String parcelId;

    @Column(name = "owner_id", length = 50)
    @NotBlank(message = "Owner ID is required")
    private String ownerId;

    @Column(name = "owner_name", length = 200)
    @NotBlank(message = "Owner name is required")
    private String ownerName;

    @Column(name = "property_address", length = 500)
    private String propertyAddress;

    @Column(name = "area_sqm", precision = 15, scale = 2)
    @NotNull(message = "Area is required")
    private BigDecimal areaSqm;

    @Column(name = "property_type", length = 50)
    private String propertyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @NotNull(message = "Status is required")
    private TitleStatus status;

    @Column(name = "registration_date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant registrationDate;

    @Column(name = "last_updated")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant lastUpdated;

    @Column(name = "notes", length = 1000)
    private String notes;

    /**
     * Title status enumeration
     */
    public enum TitleStatus {
        ACTIVE,
        PENDING_TRANSFER,
        TRANSFERRED,
        CANCELLED,
        SUSPENDED
    }

    /**
     * Pre-persist callback to set timestamps
     */
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (registrationDate == null) {
            registrationDate = now;
        }
        lastUpdated = now;
    }

    /**
     * Pre-update callback to update timestamp
     */
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = Instant.now();
    }
}

package com.worknest.domain.entities;

import com.worknest.domain.enums.*;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@Entity
@Table(
        name = "company_sites",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_company_sites_company_code", columnNames = {"company_id", "code"})
        },
        indexes = {
                @Index(name = "idx_company_sites_company_status", columnList = "company_id,status"),
                @Index(name = "idx_company_sites_company_type", columnList = "company_id,type")
        }
)
@EntityListeners(AuditingEntityListener.class)
public class CompanySite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "code", nullable = false, length = 50)
    private String code; // e.g. HQ, TIRANA-1, WAREHOUSE-2

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private SiteType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SiteStatus status = SiteStatus.DRAFT;

    /**
     * Optimistic locking token. Incremented by JPA on every update.
     * Callers that receive a 409 Conflict should surface:
     * "Updated in another tab — please refresh."
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    // Human-readable address
    @Column(name = "address_line_1", length = 255)
    private String addressLine1;

    @Column(name = "address_line_2", length = 255)
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state_region", length = 100)
    private String stateRegion;

    @Column(name = "postal_code", length = 30)
    private String postalCode;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "timezone", nullable = false, length = 100)
    private String timezone;

    // Attendance geolocation
    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "geofence_shape_type", length = 20)
    private GeofenceShapeType geofenceShapeType;

    @Column(name = "geofence_radius_meters")
    private Integer geofenceRadiusMeters; // for CIRCLE

    @Column(name = "geofence_polygon_geojson", columnDefinition = "TEXT")
    private String geofencePolygonGeoJson; // for POLYGON

    @Column(name = "entry_buffer_meters")
    private Integer entryBufferMeters; // helps reduce false negatives near edges

    @Column(name = "exit_buffer_meters")
    private Integer exitBufferMeters;

    @Column(name = "max_location_accuracy_meters")
    private Integer maxLocationAccuracyMeters; // reject very poor GPS

    @Column(name = "location_required", nullable = false)
    private Boolean locationRequired = true;

    @Column(name = "qr_enabled", nullable = false)
    private Boolean qrEnabled = true;

    @Column(name = "check_in_enabled", nullable = false)
    private Boolean checkInEnabled = true;

    @Column(name = "check_out_enabled", nullable = false)
    private Boolean checkOutEnabled = true;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Lifecycle callback that performs lightweight, non-blocking cleanup only.
     *
     * <p><strong>Strict geofence completeness validation has been intentionally removed
     * from this hook.</strong> Draft saves must never be blocked by incomplete location
     * data. Full validation (requiring a complete geofence when
     * {@code locationRequired == true} and {@code status == ACTIVE}) is performed
     * exclusively in the service-layer activation path.
     *
     * <p>The only work done here is clearing mutually-exclusive shape fields
     * when the site is already ACTIVE and a shape type is set, so the DB row
     * stays internally consistent after a confirmed location save.
     */
    @PrePersist
    @PreUpdate
    void normalizeGeofenceFields() {
        // Only clean up shape-specific fields when the site is ACTIVE and a shape
        // is explicitly chosen. For DRAFT sites, all fields are left as-is so
        // partial saves can be freely stored and later resumed.
        if (status != SiteStatus.ACTIVE || geofenceShapeType == null) {
            return;
        }
        if (geofenceShapeType == GeofenceShapeType.CIRCLE) {
            // A CIRCLE geofence must not carry polygon data.
            geofencePolygonGeoJson = null;
        } else if (geofenceShapeType == GeofenceShapeType.POLYGON) {
            // A POLYGON geofence must not carry point/radius data.
            geofenceRadiusMeters = null;
            latitude = null;
            longitude = null;
        }
    }
}

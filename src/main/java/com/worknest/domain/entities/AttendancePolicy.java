package com.worknest.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
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
        name = "attendance_policies",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_attendance_policies_company_site", columnNames = {"company_id", "site_id"})
        },
        indexes = {
                @Index(name = "idx_attendance_policies_company", columnList = "company_id")
        }
)
@EntityListeners(AuditingEntityListener.class)
public class AttendancePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private CompanySite site;

    @Column(name = "require_qr", nullable = false)
    private Boolean requireQr = true;

    @Column(name = "require_location", nullable = false)
    private Boolean requireLocation = true;

    @Column(name = "check_in_enabled", nullable = false)
    private Boolean checkInEnabled = true;

    @Column(name = "check_out_enabled", nullable = false)
    private Boolean checkOutEnabled = true;

    @Column(name = "use_network_as_warning", nullable = false)
    private Boolean useNetworkAsWarning = true;

    @Column(name = "reject_outside_geofence", nullable = false)
    private Boolean rejectOutsideGeofence = true;

    @Column(name = "reject_poor_accuracy", nullable = false)
    private Boolean rejectPoorAccuracy = false;

    @Column(name = "allow_manual_correction", nullable = false)
    private Boolean allowManualCorrection = true;

    @Column(name = "allow_manager_manual_entry", nullable = false)
    private Boolean allowManagerManualEntry = true;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

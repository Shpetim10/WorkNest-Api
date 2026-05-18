package com.worknest.domain.entities;

import com.worknest.common.security.encryption.EncryptedStringConverter;
import com.worknest.domain.enums.AttendanceCaptureMethod;
import com.worknest.domain.enums.AttendanceDecision;
import com.worknest.domain.enums.AttendanceEventStatus;
import com.worknest.domain.enums.AttendanceEventType;
import com.worknest.domain.enums.AttendanceQrValidationStatus;
import com.worknest.domain.enums.AttendanceReviewStatus;
import com.worknest.domain.enums.GeofenceDecision;
import com.worknest.domain.enums.NetworkDecision;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@Entity
@Table(
        name = "attendance_events",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_attendance_events_company_client_request_id", columnNames = {"company_id", "client_request_id"})
        },
        indexes = {
                @Index(name = "idx_attendance_events_company_employee_work_date", columnList = "company_id,employee_id,work_date,server_recorded_at"),
                @Index(name = "idx_attendance_events_site_recorded_at", columnList = "site_id,server_recorded_at")
        }
)
@EntityListeners(AuditingEntityListener.class)
public class AttendanceEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private CompanySite site;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private AttendanceEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_status", nullable = false, length = 40)
    private AttendanceEventStatus eventStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "capture_method", nullable = false, length = 30)
    private AttendanceCaptureMethod captureMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_decision", nullable = false, length = 30)
    private AttendanceDecision attendanceDecision;

    @Column(name = "server_recorded_at", nullable = false)
    private Instant serverRecordedAt;

    @Column(name = "client_captured_at")
    private Instant clientCapturedAt;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "timezone", nullable = false, length = 100)
    private String timezone;

    @Column(name = "client_request_id", length = 100)
    private String clientRequestId;

    @Column(name = "device_public_id", length = 120)
    private String devicePublicId;

    @Column(name = "platform", length = 30)
    private String platform;

    @Column(name = "app_version", length = 30)
    private String appVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qr_challenge_id")
    private AttendanceQrChallenge qrChallenge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qr_terminal_id")
    private AttendanceQrTerminal qrTerminal;

    @Enumerated(EnumType.STRING)
    @Column(name = "qr_validation_status", nullable = false, length = 40)
    private AttendanceQrValidationStatus qrValidationStatus = AttendanceQrValidationStatus.NOT_REQUIRED;

    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "accuracy_meters", precision = 10, scale = 2)
    private BigDecimal accuracyMeters;

    @Column(name = "distance_from_site_meters", precision = 10, scale = 2)
    private BigDecimal distanceFromSiteMeters;

    @Column(name = "inside_geofence")
    private Boolean insideGeofence;

    @Enumerated(EnumType.STRING)
    @Column(name = "geofence_decision", nullable = false, length = 40)
    private GeofenceDecision geofenceDecision;

    @Column(name = "request_ip_address", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String requestIpAddress;

    @Column(name = "forwarded_for_chain", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String forwardedForChain;

    @Column(name = "network_matched")
    private Boolean networkMatched;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_network_id")
    private SiteTrustedNetwork matchedNetwork;

    @Enumerated(EnumType.STRING)
    @Column(name = "network_decision", nullable = false, length = 30)
    private NetworkDecision networkDecision;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "warning_flags_json", columnDefinition = "jsonb")
    private String warningFlagsJson;

    @Column(name = "rejection_reason_code", length = 80)
    private String rejectionReasonCode;

    @Column(name = "rejection_reason_message", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String rejectionReasonMessage;

    @Column(name = "employee_note", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String employeeNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 30)
    private AttendanceReviewStatus reviewStatus = AttendanceReviewStatus.NONE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "review_note", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String reviewNote;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

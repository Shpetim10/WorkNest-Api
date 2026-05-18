package com.worknest.domain.entities;

import com.worknest.common.security.encryption.EncryptedStringConverter;
import com.worknest.domain.enums.LeaveStatus;
import com.worknest.domain.enums.LeaveType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
        name = "leave_requests",
        indexes = {
                @Index(name = "idx_leave_requests_company_status", columnList = "company_id,status"),
                @Index(name = "idx_leave_requests_company_employee", columnList = "company_id,employee_id")
        }
)
@EntityListeners(AuditingEntityListener.class)
public class LeaveRequest {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false, length = 30)
    private LeaveType leaveType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /** Supports half-days (e.g. 0.5). Computed from start/end dates at submission time. */
    @Column(name = "days_count", nullable = false, precision = 6, scale = 1)
    private BigDecimal daysCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private LeaveStatus status = LeaveStatus.PENDING;

    @Column(name = "note", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String note;

    @Column(name = "approval_note", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String approvalNote;

    @Column(name = "medical_report_document_id", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String medicalReportDocumentId;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private User reviewedByUser;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

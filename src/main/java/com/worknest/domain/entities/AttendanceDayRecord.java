package com.worknest.domain.entities;

import com.worknest.domain.enums.AttendanceDayStatus;
import com.worknest.domain.enums.AttendanceReviewStatus;
import jakarta.persistence.Column;
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
import jakarta.persistence.Version;
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
        name = "attendance_day_records",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_attendance_day_records_company_employee_work_date", columnNames = {"company_id", "employee_id", "work_date"})
        },
        indexes = {
                @Index(name = "idx_attendance_day_records_site_work_date", columnList = "site_id,work_date")
        }
)
@EntityListeners(AuditingEntityListener.class)
public class AttendanceDayRecord {

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

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "timezone", nullable = false, length = 100)
    private String timezone;

    @Column(name = "first_check_in_at")
    private Instant firstCheckInAt;

    @Column(name = "last_check_out_at")
    private Instant lastCheckOutAt;

    @Column(name = "worked_minutes", nullable = false)
    private Integer workedMinutes = 0;

    @Column(name = "break_minutes", nullable = false)
    private Integer breakMinutes = 0;

    @Column(name = "late_minutes", nullable = false)
    private Integer lateMinutes = 0;

    @Column(name = "early_leave_minutes", nullable = false)
    private Integer earlyLeaveMinutes = 0;

    @Column(name = "overtime_minutes", nullable = false)
    private Integer overtimeMinutes = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_status", nullable = false, length = 30)
    private AttendanceDayStatus dayStatus = AttendanceDayStatus.ABSENT;

    @Column(name = "has_warnings", nullable = false)
    private Boolean hasWarnings = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "warning_flags_json", columnDefinition = "jsonb")
    private String warningFlagsJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 30)
    private AttendanceReviewStatus reviewStatus = AttendanceReviewStatus.NONE;

    @Column(name = "payroll_locked", nullable = false)
    private Boolean payrollLocked = false;

    @Column(name = "source_event_count", nullable = false)
    private Integer sourceEventCount = 0;

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

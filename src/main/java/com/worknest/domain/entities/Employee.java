package com.worknest.domain.entities;

import com.worknest.domain.enums.EmploymentStatus;
import com.worknest.domain.enums.EmploymentType;
import com.worknest.domain.enums.PaymentMethod;
import com.worknest.domain.enums.PlatformRole;

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
        name = "employees",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_employees_user_company", columnNames = {"user_id", "company_id"})
        },
        indexes = {
                @Index(name = "idx_employees_company", columnList = "company_id"),
                @Index(name = "idx_employees_company_supervisor", columnList = "company_id,supervisor_role_assignment_id")
        }
)
@EntityListeners(AuditingEntityListener.class)
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_site_id")
    private CompanySite companySite;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type_role", nullable = false, length = 30)
    private PlatformRole employmentTypeRole;

    @Column(name = "start_date")
    private LocalDate startDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_role_assignment_id")
    private RoleAssignment supervisorRoleAssignment;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", nullable = false, length = 30)
    private EmploymentStatus employmentStatus = EmploymentStatus.PENDING;

    // --- Contract fields ---

    @Column(name = "contract_document_key", length = 500)
    private String contractDocumentKey;

    @Column(name = "contract_document_path", length = 1000)
    private String contractDocumentPath;

    @Column(name = "contract_expiry_date")
    private LocalDate contractExpiryDate;

    @Column(name = "leave_days_per_year")
    private Integer leaveDaysPerYear;

    /** Hours worked per day; used to derive daily leave pay for HOURLY employees: dailyLeavePay = hourlyRate × dailyWorkingHours. */
    @Column(name = "daily_working_hours", precision = 4, scale = 1)
    private BigDecimal dailyWorkingHours;

    // --- Payment fields ---

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "monthly_salary", precision = 12, scale = 2)
    private BigDecimal monthlySalary;

    @Column(name = "hourly_rate", precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", length = 30)
    private EmploymentType employmentType;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void validateBusinessRules() {
        if (supervisorRoleAssignment != null) {
            if (supervisorRoleAssignment.getRole() != PlatformRole.STAFF) {
                throw new IllegalStateException("Supervisor role assignment must be of STAFF platform role");
            }
            if (!supervisorRoleAssignment.getCompany().getId().equals(company.getId())) {
                throw new IllegalStateException("Supervisor must belong to the same company");
            }
        }
    }
}

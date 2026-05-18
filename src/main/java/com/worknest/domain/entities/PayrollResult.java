package com.worknest.domain.entities;

import com.worknest.common.security.encryption.EncryptedBigDecimalConverter;
import com.worknest.common.security.encryption.EncryptedStringConverter;
import com.worknest.domain.enums.PayrollStatus;
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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
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
        name = "payroll_results",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payroll_results_employee_period",
                        columnNames = {"company_id", "employee_id", "payroll_year", "payroll_month"})
        },
        indexes = {
                @Index(name = "idx_payroll_results_company_period", columnList = "company_id,payroll_year,payroll_month"),
                @Index(name = "idx_payroll_results_status", columnList = "status")
        }
)
@EntityListeners(AuditingEntityListener.class)
public class PayrollResult {

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

    @Column(name = "payroll_year", nullable = false)
    private int year;

    @Column(name = "payroll_month", nullable = false)
    private int month;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PayrollStatus status = PayrollStatus.DRAFT;

    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "base_pay", nullable = false, columnDefinition = "TEXT")
    private BigDecimal basePay = BigDecimal.ZERO;

    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "gross_earnings", nullable = false, columnDefinition = "TEXT")
    private BigDecimal grossEarnings = BigDecimal.ZERO;

    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "total_deductions", nullable = false, columnDefinition = "TEXT")
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "net_pay", nullable = false, columnDefinition = "TEXT")
    private BigDecimal netPay = BigDecimal.ZERO;

    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "income_tax", nullable = false, columnDefinition = "TEXT")
    private BigDecimal incomeTax = BigDecimal.ZERO;

    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "employee_social_security", nullable = false, columnDefinition = "TEXT")
    private BigDecimal employeeSocialSecurity = BigDecimal.ZERO;

    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "employee_pension", nullable = false, columnDefinition = "TEXT")
    private BigDecimal employeePension = BigDecimal.ZERO;

    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "employer_social_security", nullable = false, columnDefinition = "TEXT")
    private BigDecimal employerSocialSecurity = BigDecimal.ZERO;

    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "employer_pension", nullable = false, columnDefinition = "TEXT")
    private BigDecimal employerPension = BigDecimal.ZERO;

    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "taxable_income", nullable = false, columnDefinition = "TEXT")
    private BigDecimal taxableIncome = BigDecimal.ZERO;

    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "employer_cost_total", nullable = false, columnDefinition = "TEXT")
    private BigDecimal employerCostTotal = BigDecimal.ZERO;

    @Lob
    @Column(name = "calculation_snapshot_json", nullable = false, columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String calculationSnapshotJson;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calculated_by_user_id")
    private User calculatedByUser;

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

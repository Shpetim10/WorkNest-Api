package com.worknest.domain.entities;

import com.worknest.domain.enums.TaxBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@Entity
@Table(
        name = "company_payroll_settings",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payroll_settings_company", columnNames = {"company_id"})
        }
)
@EntityListeners(AuditingEntityListener.class)
public class CompanyPayrollSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "default_daily_working_hours", nullable = false, precision = 4, scale = 1)
    private BigDecimal defaultDailyWorkingHours = BigDecimal.valueOf(8);

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "weekend_days", nullable = false, columnDefinition = "jsonb")
    private String weekendDaysJson = "[\"SATURDAY\",\"SUNDAY\"]";

    @Column(name = "tax_enabled", nullable = false)
    private boolean taxEnabled = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_base", nullable = false, length = 30)
    private TaxBase taxBase = TaxBase.GROSS_MINUS_CONTRIBUTIONS;

    @Column(name = "social_security_employee_rate", nullable = false, precision = 6, scale = 3)
    private BigDecimal socialSecurityEmployeeRate = BigDecimal.ZERO;

    @Column(name = "social_security_employer_rate", nullable = false, precision = 6, scale = 3)
    private BigDecimal socialSecurityEmployerRate = BigDecimal.ZERO;

    @Column(name = "pension_employee_rate", nullable = false, precision = 6, scale = 3)
    private BigDecimal pensionEmployeeRate = BigDecimal.ZERO;

    @Column(name = "pension_employer_rate", nullable = false, precision = 6, scale = 3)
    private BigDecimal pensionEmployerRate = BigDecimal.ZERO;

    @Column(name = "contribution_min_base", precision = 14, scale = 2)
    private BigDecimal contributionMinBase;

    @Column(name = "contribution_max_base", precision = 14, scale = 2)
    private BigDecimal contributionMaxBase;

    @Column(name = "maternity_employer_topup_rate", nullable = false, precision = 6, scale = 3)
    private BigDecimal maternityEmployerTopupRate = BigDecimal.ZERO;

    @Column(name = "paternity_employer_topup_rate", nullable = false, precision = 6, scale = 3)
    private BigDecimal paternityEmployerTopupRate = BigDecimal.ZERO;

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

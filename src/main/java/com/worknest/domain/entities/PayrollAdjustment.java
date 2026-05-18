package com.worknest.domain.entities;

import com.worknest.common.security.encryption.EncryptedBigDecimalConverter;
import com.worknest.common.security.encryption.EncryptedStringConverter;
import com.worknest.domain.enums.PayrollAdjustmentType;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
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
        name = "payroll_adjustments",
        indexes = {
                @Index(name = "idx_payroll_adjustments_company_employee_period",
                        columnList = "company_id,employee_id,payroll_year,payroll_month"),
                @Index(name = "idx_payroll_adjustments_type", columnList = "adjustment_type")
        }
)
@EntityListeners(AuditingEntityListener.class)
public class PayrollAdjustment {

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
    @Column(name = "adjustment_type", nullable = false, length = 30)
    private PayrollAdjustmentType type;

    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "amount", nullable = false, columnDefinition = "TEXT")
    private BigDecimal amount;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "notes", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdByUser;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void validateAmount() {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalStateException("Payroll adjustment amount must be positive");
        }
    }
}

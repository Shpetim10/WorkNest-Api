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
        name = "company_tax_brackets",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tax_brackets_company_ordinal",
                        columnNames = {"company_id", "ordinal"})
        },
        indexes = {
                @Index(name = "idx_tax_brackets_company_ordinal", columnList = "company_id,ordinal")
        }
)
@EntityListeners(AuditingEntityListener.class)
public class CompanyTaxBracket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    /** 0-based ascending ordinal. */
    @Column(name = "ordinal", nullable = false)
    private int ordinal;

    /** Inclusive lower bound. */
    @Column(name = "lower_bound", nullable = false, precision = 14, scale = 2)
    private BigDecimal lowerBound;

    /** Exclusive upper bound; null means open-ended top bracket. */
    @Column(name = "upper_bound", precision = 14, scale = 2)
    private BigDecimal upperBound;

    /** Rate as percentage, e.g. 15.000 = 15%. */
    @Column(name = "rate", nullable = false, precision = 6, scale = 3)
    private BigDecimal rate;

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

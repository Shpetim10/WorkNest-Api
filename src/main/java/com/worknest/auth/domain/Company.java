package com.worknest.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
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
        name = "companies",
        indexes = {
                @Index(name = "idx_companies_slug", columnList = "slug"),
                @Index(name = "idx_companies_status", columnList = "status")
        }
)
@EntityListeners(AuditingEntityListener.class)
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "slug", nullable = false, length = 100)
    private String slug;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CompanyStatus status = CompanyStatus.ACTIVE;

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(name = "timezone", nullable = false, length = 100)
    private String timezone = "UTC";

    @Column(name = "locale", nullable = false, length = 10)
    private String locale = "en";

    @Column(name = "data_retention_days", nullable = false)
    private Integer dataRetentionDays = 90;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deletion_scheduled_at")
    private Instant deletionScheduledAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

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

    @Column(name = "slug", unique = true, nullable = false, length = 100)
    private String slug;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CompanyStatus status = CompanyStatus.ACTIVE;

    @Column(name = "nipt", unique = true, length = 30)
    private String nipt;


    @Column(name = "industry", length = 100)
    private String industry;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode = "AL";

    @Column(name = "logo_key", length = 500)
    private String logoKey;

    @Column(name = "logo_path", length = 1000)
    private String logoPath;

    @Column(name = "timezone", nullable = false, length = 100)
    private String timezone = "Europe/Tirane";

    @Column(name = "locale", nullable = false, length = 10)
    private String locale = "sq";

    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "ALL";

    @Column(name = "date_format", nullable = false, length = 20)
    private String dateFormat = "DD/MM/YYYY";

    @Column(name = "onboarding_completed_at")
    private Instant onboardingCompletedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_plan", length = 100)
    private SubscriptionPlan subscriptionPlan = SubscriptionPlan.BASIC;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_status", nullable = false, length = 50)
    private SubscriptionStatus subscriptionStatus = SubscriptionStatus.TRIAL;

    @Column(name = "trial_ends_at")
    private Instant trialEndsAt;

    @Column(name = "subscription_renewal_at")
    private Instant subscriptionRenewalAt;

    @Column(name = "data_retention_days", nullable = false)
    private Integer dataRetentionDays = 90;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deletion_scheduled_at")
    private Instant deletionScheduledAt;

    @Column(name = "suspended_at")
    private Instant suspendedAt;

    @Column(name = "suspended_reason", columnDefinition = "TEXT")
    private String suspendedReason;

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

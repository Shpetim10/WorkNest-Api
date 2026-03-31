package com.worknest.auth.domain;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_company_email", columnNames = {"company_id", "email"}),
                @UniqueConstraint(name = "uk_users_company_username", columnNames = {"company_id", "username"})
        },
        indexes = {
                @Index(name = "idx_users_company_email", columnList = "company_id,email"),
                @Index(name = "idx_users_company_username", columnList = "company_id,username"),
                @Index(name = "idx_users_company_status", columnList = "company_id,status"),
                @Index(name = "idx_users_status", columnList = "status")
        }
)
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status = UserStatus.PENDING;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName = "";

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName = "";

    @Column(name = "display_name", length = 150)
    private String displayName;

    @Column(name = "profile_image_s3_key", length = 500)
    private String profileImageS3Key;

    @Column(name = "phone_primary", length = 50)
    private String phonePrimary;

    @Column(name = "phone_secondary", length = 50)
    private String phoneSecondary;

    @Column(name = "personal_email", length = 255)
    private String personalEmail;

    @Column(name = "preferred_language", nullable = false, length = 10)
    private String preferredLanguage = "sq";

    @Column(name = "timezone_override", length = 100)
    private String timezoneOverride;

    @Column(name = "failed_login_count", nullable = false)
    private Short failedLoginCount = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    @Column(name = "mfa_enabled", nullable = false)
    private Boolean mfaEnabled = false;

    @Column(name = "mfa_secret_enc", length = 255)
    private String mfaSecretEnc;

    @Column(name = "gdpr_consent_at")
    private Instant gdprConsentAt;

    @Column(name = "gdpr_consent_ip", length = 45)
    private String gdprConsentIp;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

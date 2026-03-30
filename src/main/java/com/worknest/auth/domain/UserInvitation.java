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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@Entity
@Table(
        name = "user_invitations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_invitations_token_hash", columnNames = {"token_hash"})
        },
        indexes = {
                @Index(name = "idx_user_invitations_company_email", columnList = "company_id,email"),
                @Index(name = "idx_user_invitations_expires_at", columnList = "expires_at"),
                @Index(name = "idx_user_invitations_used_at", columnList = "used_at")
        }
)
@EntityListeners(AuditingEntityListener.class)
public class UserInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    // Store only token hash, never raw token.
    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by")
    private User invitedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform_role", nullable = false, length = 30)
    private PlatformRole platformRole = PlatformRole.EMPLOYEE;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (expiresAt == null) {
            expiresAt = Instant.now().plus(Duration.ofHours(24));
        }
    }

    ///  Helpers

    // Checks if the invitation has expired.
    public boolean isExpired(Instant now) {
        return expiresAt != null && expiresAt.isBefore(now);
    }

    // Checks if the invitation has already been used.
    public boolean isUsed() {
        return usedAt != null;
    }

    // Checks if the invitation is currently valid (not used and not expired).

    public boolean isValid(Instant now) {
        return !isUsed() && !isExpired(now);
    }
}

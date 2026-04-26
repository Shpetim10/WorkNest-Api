package com.worknest.domain.entities;

import com.worknest.domain.enums.AttendanceQrChallengeStatus;
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
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@Entity
@Table(
        name = "attendance_qr_challenges",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_attendance_qr_challenges_token_hash", columnNames = {"token_hash"}),
                @UniqueConstraint(name = "uk_attendance_qr_challenges_terminal_nonce", columnNames = {"company_id", "qr_terminal_id", "nonce"})
        },
        indexes = {
                @Index(name = "idx_attendance_qr_challenges_site_status_expiry", columnList = "site_id,status,expires_at")
        }
)
@EntityListeners(AuditingEntityListener.class)
public class AttendanceQrChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private CompanySite site;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "qr_terminal_id", nullable = false)
    private AttendanceQrTerminal qrTerminal;

    @Column(name = "nonce", nullable = false, length = 128)
    private String nonce;

    @Column(name = "token_hash", nullable = false, length = 128)
    private String tokenHash;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "used_by_user_id")
    private User usedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AttendanceQrChallengeStatus status = AttendanceQrChallengeStatus.ACTIVE;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

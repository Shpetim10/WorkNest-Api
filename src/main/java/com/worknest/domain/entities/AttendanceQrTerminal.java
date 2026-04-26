package com.worknest.domain.entities;

import com.worknest.domain.enums.AttendanceQrTerminalStatus;
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
        name = "attendance_qr_terminals",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_attendance_qr_terminals_company_site_name", columnNames = {"company_id", "site_id", "name"})
        },
        indexes = {
                @Index(name = "idx_attendance_qr_terminals_site_status", columnList = "site_id,status")
        }
)
@EntityListeners(AuditingEntityListener.class)
public class AttendanceQrTerminal {

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

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AttendanceQrTerminalStatus status = AttendanceQrTerminalStatus.ACTIVE;

    @Column(name = "rotation_seconds", nullable = false)
    private Integer rotationSeconds = 60;

    @Column(name = "secret_key_version", nullable = false, length = 50)
    private String secretKeyVersion = "v1";

    @Column(name = "auto_created", nullable = false)
    private Boolean autoCreated = false;

    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;

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

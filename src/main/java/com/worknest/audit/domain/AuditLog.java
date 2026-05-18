package com.worknest.audit.domain;

import com.worknest.common.security.encryption.EncryptedStringConverter;
import com.worknest.domain.enums.PlatformRole;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Immutable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_logs_company_action_created_at", columnList = "company_id,action,created_at"),
                @Index(name = "idx_audit_logs_entity_type_entity_id_created_at", columnList = "entity_type,entity_id,created_at"),
                @Index(name = "idx_audit_logs_created_at", columnList = "created_at")
        }
)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "actor_role_assignment_id")
    private UUID actorRoleAssignmentId;

    @Column(name = "actor_role", length = 30)
    @Enumerated(EnumType.STRING)
    private PlatformRole actorRole;

    @Column(name = "actor_job_title", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String actorJobTitle;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "diff", columnDefinition = "jsonb")
    private Map<String, Object> diff;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "ip_address", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AuditLog(
            UUID companyId,
            UUID actorUserId,
            UUID actorRoleAssignmentId,
            PlatformRole actorRole,
            String actorJobTitle,
            String action,
            String entityType,
            UUID entityId,
            Map<String, Object> diff,
            Map<String, Object> metadata,
            String ipAddress
    ) {
        this.companyId = companyId;
        this.actorUserId = actorUserId;
        this.actorRoleAssignmentId = actorRoleAssignmentId;
        this.actorRole = actorRole;
        this.actorJobTitle = actorJobTitle;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.diff = diff;
        this.metadata = metadata;
        this.ipAddress = ipAddress;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

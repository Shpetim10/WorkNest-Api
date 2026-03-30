package com.worknest.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
@Table(
        name = "platform_events",
        indexes = {
                @Index(name = "idx_platform_events_created_at", columnList = "created_at"),
                @Index(name = "idx_platform_events_event_type_created_at", columnList = "event_type,created_at"),
                @Index(name = "idx_platform_events_company_created_at", columnList = "company_id,created_at")
        }
)
public class PlatformEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "company_name", length = 255)
    private String companyName;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PlatformEvent(
            String eventType,
            UUID companyId,
            String companyName,
            UUID actorUserId,
            String description
    ) {
        this.eventType = eventType;
        this.companyId = companyId;
        this.companyName = companyName;
        this.actorUserId = actorUserId;
        this.description = description;
    }

    @PrePersist
    void setCreatedAtOnInsert() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

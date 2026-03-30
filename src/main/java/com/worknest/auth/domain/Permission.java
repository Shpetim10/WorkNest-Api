package com.worknest.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Immutable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "permissions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_permissions_resource_action", columnNames = {"resource", "action"}),
                @UniqueConstraint(name = "uk_permissions_code", columnNames = {"code"})
        },
        indexes = {
                @Index(name = "idx_permissions_code", columnList = "code"),
                @Index(name = "idx_permissions_resource_action", columnList = "resource,action")
        }
)
@EntityListeners(AuditingEntityListener.class)
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "resource", nullable = false, length = 100, updatable = false)
    private String resource;

    @Column(name = "action", nullable = false, length = 100, updatable = false)
    private String action;

    @Column(name = "code", nullable = false, length = 150, updatable = false)
    private String code;

    @Column(name = "description", updatable = false)
    private String description;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Permission(String resource, String action, String description) {
        this.resource = Objects.requireNonNull(resource, "resource must not be null");
        this.action = Objects.requireNonNull(action, "action must not be null");
        this.code = resource + "." + action;
        this.description = description;
    }
}

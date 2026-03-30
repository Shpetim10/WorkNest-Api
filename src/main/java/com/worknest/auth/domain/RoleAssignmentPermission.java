package com.worknest.auth.domain;

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
        name = "role_assignment_permissions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_role_assignment_permissions_assignment_permission",
                        columnNames = {"role_assignment_id", "permission_id"}
                )
        },
        indexes = {
                @Index(name = "idx_rap_assignment", columnList = "role_assignment_id"),
                @Index(name = "idx_rap_permission", columnList = "permission_id"),
                @Index(name = "idx_rap_assignment_granted", columnList = "role_assignment_id,is_granted")
        }
)
@EntityListeners(AuditingEntityListener.class)
public class RoleAssignmentPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_assignment_id", nullable = false)
    private RoleAssignment roleAssignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

    @Column(name = "is_granted", nullable = false)
    private Boolean isGranted = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by")
    private User grantedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

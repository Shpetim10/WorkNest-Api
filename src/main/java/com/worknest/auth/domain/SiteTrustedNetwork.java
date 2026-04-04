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
        name = "site_trusted_networks",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_site_trusted_networks_site_cidr_type",
                        columnNames = {"site_id", "cidr_block", "network_type"}
                )
        },
        indexes = {
                @Index(name = "idx_site_trusted_networks_site_active", columnList = "site_id,is_active"),
                @Index(name = "idx_site_trusted_networks_site_priority", columnList = "site_id,priority_order")
        }
)
@EntityListeners(AuditingEntityListener.class)
public class SiteTrustedNetwork {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private CompanySite site;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "network_type", nullable = false, length = 20)
    private NetworkType networkType;

    @Column(name = "cidr_block", nullable = false, length = 100)
    private String cidrBlock;

    @Enumerated(EnumType.STRING)
    @Column(name = "ip_version", nullable = false, length = 10)
    private NetworkIpVersion ipVersion;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "priority_order")
    private Integer priorityOrder;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

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

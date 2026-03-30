package com.worknest.audit.repository;

import com.worknest.audit.domain.AuditLog;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findAllByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            String entityType,
            UUID entityId,
            Pageable pageable
    );

    Page<AuditLog> findAllByCompanyIdAndActionAndCreatedAtBetweenOrderByCreatedAtDesc(
            UUID companyId,
            String action,
            Instant from,
            Instant to,
            Pageable pageable
    );

    Page<AuditLog> findAllByCompanyIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            UUID companyId,
            Instant from,
            Instant to,
            Pageable pageable
    );
}

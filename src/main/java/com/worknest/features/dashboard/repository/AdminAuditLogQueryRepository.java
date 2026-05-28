package com.worknest.features.dashboard.repository;

import com.worknest.audit.domain.AuditLog;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AdminAuditLogQueryRepository {

    private final EntityManager entityManager;

    public Page<AuditLog> findByCompanyId(UUID companyId, String action, Instant from, Instant to, Pageable pageable) {
        StringBuilder where = new StringBuilder("WHERE a.companyId = :companyId");
        if (action != null && !action.isBlank()) where.append(" AND UPPER(a.action) LIKE UPPER(CONCAT(:action, '%'))");
        if (from != null) where.append(" AND a.createdAt >= :from");
        if (to != null) where.append(" AND a.createdAt <= :to");

        String dataJpql = "SELECT a FROM AuditLog a " + where + " ORDER BY a.createdAt DESC";
        String countJpql = "SELECT COUNT(a) FROM AuditLog a " + where;

        TypedQuery<AuditLog> dataQuery = entityManager.createQuery(dataJpql, AuditLog.class);
        TypedQuery<Long> countQuery = entityManager.createQuery(countJpql, Long.class);

        dataQuery.setParameter("companyId", companyId);
        countQuery.setParameter("companyId", companyId);
        if (action != null && !action.isBlank()) {
            dataQuery.setParameter("action", action);
            countQuery.setParameter("action", action);
        }
        if (from != null) {
            dataQuery.setParameter("from", from);
            countQuery.setParameter("from", from);
        }
        if (to != null) {
            dataQuery.setParameter("to", to);
            countQuery.setParameter("to", to);
        }

        dataQuery.setFirstResult((int) pageable.getOffset());
        dataQuery.setMaxResults(pageable.getPageSize());

        List<AuditLog> results = dataQuery.getResultList();
        long total = countQuery.getSingleResult();

        return new PageImpl<>(results, pageable, total);
    }
}

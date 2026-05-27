package com.worknest.features.dashboard.repository;

import com.worknest.audit.domain.AuditLog;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
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

    public Page<AuditLog> findByCompanyId(UUID companyId, Pageable pageable) {
        String dataJpql = "SELECT a FROM AuditLog a WHERE a.companyId = :companyId ORDER BY a.createdAt DESC";
        String countJpql = "SELECT COUNT(a) FROM AuditLog a WHERE a.companyId = :companyId";

        TypedQuery<AuditLog> dataQuery = entityManager.createQuery(dataJpql, AuditLog.class);
        TypedQuery<Long> countQuery = entityManager.createQuery(countJpql, Long.class);

        dataQuery.setParameter("companyId", companyId);
        countQuery.setParameter("companyId", companyId);

        dataQuery.setFirstResult((int) pageable.getOffset());
        dataQuery.setMaxResults(pageable.getPageSize());

        List<AuditLog> results = dataQuery.getResultList();
        long total = countQuery.getSingleResult();

        return new PageImpl<>(results, pageable, total);
    }
}

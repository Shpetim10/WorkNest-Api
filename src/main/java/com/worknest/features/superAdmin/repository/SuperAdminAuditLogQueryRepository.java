package com.worknest.features.superAdmin.repository;

import com.worknest.audit.domain.PlatformEvent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SuperAdminAuditLogQueryRepository {

    private final EntityManager entityManager;

    public Page<PlatformEvent> findEvents(String search, Pageable pageable) {
        String condition = buildCondition(search);

        String dataJpql = "SELECT p FROM PlatformEvent p WHERE " + condition + " ORDER BY p.createdAt DESC";
        String countJpql = "SELECT COUNT(p) FROM PlatformEvent p WHERE " + condition;

        TypedQuery<PlatformEvent> dataQuery = entityManager.createQuery(dataJpql, PlatformEvent.class);
        TypedQuery<Long> countQuery = entityManager.createQuery(countJpql, Long.class);

        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.toLowerCase() + "%";
            dataQuery.setParameter("search", pattern);
            countQuery.setParameter("search", pattern);
        }

        dataQuery.setFirstResult((int) pageable.getOffset());
        dataQuery.setMaxResults(pageable.getPageSize());

        List<PlatformEvent> events = dataQuery.getResultList();
        long total = countQuery.getSingleResult();

        return new PageImpl<>(events, pageable, total);
    }

    public long countToday() {
        Instant startOfDay = Instant.now().truncatedTo(ChronoUnit.DAYS);
        return entityManager.createQuery(
                        "SELECT COUNT(p) FROM PlatformEvent p WHERE p.createdAt >= :startOfDay",
                        Long.class)
                .setParameter("startOfDay", startOfDay)
                .getSingleResult();
    }

    private String buildCondition(String search) {
        if (search == null || search.isBlank()) {
            return "1=1";
        }
        return "(LOWER(p.eventType) LIKE :search"
                + " OR LOWER(p.companyName) LIKE :search"
                + " OR LOWER(p.description) LIKE :search)";
    }
}
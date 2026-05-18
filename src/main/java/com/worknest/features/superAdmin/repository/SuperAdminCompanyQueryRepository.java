package com.worknest.features.superAdmin.repository;

import com.worknest.common.security.encryption.EncryptionService;
import com.worknest.domain.entities.Company;
import com.worknest.domain.enums.CompanyStatus;
import com.worknest.domain.enums.SubscriptionPlan;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SuperAdminCompanyQueryRepository {

    private final EntityManager entityManager;
    private final EncryptionService encryptionService;

    public Page<Company> findCompanies(String search, String status, String plan, Pageable pageable) {
        String normalizedSearch = search == null ? null : search.trim();
        String niptHash = encryptionService.hmacSha256Hex(encryptionService.normalizeNipt(normalizedSearch));
        String baseCondition = buildCondition(normalizedSearch, status, plan);

        String dataJpql = "SELECT c FROM Company c WHERE " + baseCondition + " ORDER BY c.createdAt DESC";
        String countJpql = "SELECT COUNT(c) FROM Company c WHERE " + baseCondition;

        TypedQuery<Company> dataQuery = entityManager.createQuery(dataJpql, Company.class);
        TypedQuery<Long> countQuery = entityManager.createQuery(countJpql, Long.class);

        bindParameters(dataQuery, normalizedSearch, niptHash, status, plan);
        bindParameters(countQuery, normalizedSearch, niptHash, status, plan);

        dataQuery.setFirstResult((int) pageable.getOffset());
        dataQuery.setMaxResults(pageable.getPageSize());

        List<Company> companies = dataQuery.getResultList();
        long total = countQuery.getSingleResult();

        return new PageImpl<>(companies, pageable, total);
    }

    public Map<UUID, Long> countEmployeesByCompanyIds(Collection<UUID> companyIds) {
        if (companyIds == null || companyIds.isEmpty()) {
            return Map.of();
        }

        List<Object[]> rows = entityManager.createQuery("""
                        SELECT ra.company.id, COUNT(DISTINCT ra.user.id)
                        FROM RoleAssignment ra
                        WHERE ra.company.id IN :companyIds
                        GROUP BY ra.company.id
                        """, Object[].class)
                .setParameter("companyIds", companyIds)
                .getResultList();

        return rows.stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Long) row[1]
                ));
    }

    private String buildCondition(String search, String status, String plan) {
        StringBuilder condition = new StringBuilder("c.deletedAt IS NULL AND c.slug != 'worknest-platform'");

        if (search != null && !search.isBlank()) {
            condition.append(" AND (LOWER(c.name) LIKE :search"
                    + " OR LOWER(c.email) LIKE :search"
                    + " OR LOWER(c.slug) LIKE :search"
                    + " OR (:niptHash IS NOT NULL AND c.niptHash = :niptHash))");
        }
        if (status != null && !status.isBlank()) {
            condition.append(" AND c.status = :status");
        }
        if (plan != null && !plan.isBlank()) {
            condition.append(" AND c.subscriptionPlan = :plan");
        }

        return condition.toString();
    }

    private <T> void bindParameters(TypedQuery<T> query, String search, String niptHash, String status, String plan) {
        if (search != null && !search.isBlank()) {
            query.setParameter("search", "%" + search.toLowerCase() + "%");
            query.setParameter("niptHash", niptHash);
        }
        if (status != null && !status.isBlank()) {
            query.setParameter("status", CompanyStatus.valueOf(status.toUpperCase()));
        }
        if (plan != null && !plan.isBlank()) {
            query.setParameter("plan", parsePlan(plan));
        }
    }

    private SubscriptionPlan parsePlan(String plan) {
        return switch (plan.trim().toLowerCase()) {
            case "starter", "basic" -> SubscriptionPlan.BASIC;
            case "professional", "premium" -> SubscriptionPlan.PREMIUM;
            default -> SubscriptionPlan.valueOf(plan.toUpperCase());
        };
    }
}

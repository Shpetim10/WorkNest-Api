package com.worknest.features.superAdmin.repository;

import com.worknest.domain.entities.Company;
import com.worknest.domain.enums.CompanyStatus;
import com.worknest.domain.enums.SubscriptionPlan;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SuperAdminCompanyQueryRepository {

    private final EntityManager entityManager;

    public Page<Company> findCompanies(String search, String status, String plan, Pageable pageable) {
        String baseCondition = buildCondition(search, status, plan);

        String dataJpql = "SELECT c FROM Company c WHERE " + baseCondition + " ORDER BY c.createdAt DESC";
        String countJpql = "SELECT COUNT(c) FROM Company c WHERE " + baseCondition;

        TypedQuery<Company> dataQuery = entityManager.createQuery(dataJpql, Company.class);
        TypedQuery<Long> countQuery = entityManager.createQuery(countJpql, Long.class);

        bindParameters(dataQuery, search, status, plan);
        bindParameters(countQuery, search, status, plan);

        dataQuery.setFirstResult((int) pageable.getOffset());
        dataQuery.setMaxResults(pageable.getPageSize());

        List<Company> companies = dataQuery.getResultList();
        long total = countQuery.getSingleResult();

        return new PageImpl<>(companies, pageable, total);
    }

    private String buildCondition(String search, String status, String plan) {
        StringBuilder condition = new StringBuilder("c.deletedAt IS NULL AND c.slug != 'worknest-platform'");

        if (search != null && !search.isBlank()) {
            condition.append(" AND (LOWER(c.name) LIKE :search"
                    + " OR LOWER(c.email) LIKE :search"
                    + " OR LOWER(c.nipt) LIKE :search)");
        }
        if (status != null && !status.isBlank()) {
            condition.append(" AND c.status = :status");
        }
        if (plan != null && !plan.isBlank()) {
            condition.append(" AND c.subscriptionPlan = :plan");
        }

        return condition.toString();
    }

    private <T> void bindParameters(TypedQuery<T> query, String search, String status, String plan) {
        if (search != null && !search.isBlank()) {
            query.setParameter("search", "%" + search.toLowerCase() + "%");
        }
        if (status != null && !status.isBlank()) {
            query.setParameter("status", CompanyStatus.valueOf(status.toUpperCase()));
        }
        if (plan != null && !plan.isBlank()) {
            query.setParameter("plan", SubscriptionPlan.valueOf(plan.toUpperCase()));
        }
    }
}
package com.worknest.features.companySite.repository;

import com.worknest.domain.entities.CompanySite;
import com.worknest.domain.enums.SiteStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA repository for {@link CompanySite}.
 *
 * <p>All finders are scoped to both {@code companyId} and the target record's
 * ID to prevent cross-tenant data leaks.
 */
@Repository
public interface CompanySiteRepository extends JpaRepository<CompanySite, UUID> {

    /**
     * Checks whether a site with the given {@code code} already exists for a company.
     * Used for uniqueness validation before persisting a new site.
     */
    boolean existsByCompanyIdAndCode(UUID companyId, String code);

    /**
     * Loads a site only if it belongs to the given company.
     * Used for all single-site retrieval to enforce tenant isolation.
     */
    Optional<CompanySite> findByIdAndCompanyId(UUID id, UUID companyId);

    /**
     * Returns all sites for a company, ordered by creation time (newest first).
     */
    List<CompanySite> findAllByCompanyIdOrderByCreatedAtDesc(UUID companyId);

    /**
     * Returns all sites for a company filtered by status.
     */
    List<CompanySite> findAllByCompanyIdAndStatus(UUID companyId, SiteStatus status);

    /**
     * Counts existing rules for priority-order suggestion in detect-network.
     */
    long countByCompanyId(UUID companyId);
}

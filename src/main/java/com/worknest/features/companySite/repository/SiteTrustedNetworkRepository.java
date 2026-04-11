package com.worknest.features.companySite.repository;

import com.worknest.domain.entities.SiteTrustedNetwork;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA repository for {@link SiteTrustedNetwork}.
 *
 * <p>All finders include {@code siteId} to maintain the ownership invariant
 * and prevent cross-site rule access.
 */
@Repository
public interface SiteTrustedNetworkRepository extends JpaRepository<SiteTrustedNetwork, UUID> {

    /**
     * Returns all rules for a given site ordered by priority (lowest number first).
     */
    List<SiteTrustedNetwork> findAllBySiteIdOrderByPriorityOrderAsc(UUID siteId);

    /**
     * Returns all active rules for a given site ordered by priority.
     * Used by the attendance engine at clock-in time.
     */
    List<SiteTrustedNetwork> findAllBySiteIdAndIsActiveTrueOrderByPriorityOrderAsc(UUID siteId);

    /**
     * Checks for an existing CIDR + network-type combination on the same site.
     * Used to detect duplicates within the inbound request list before persistence.
     */
    boolean existsBySiteIdAndCidrBlockAndNetworkType(
            UUID siteId,
            String cidrBlock,
            com.worknest.domain.enums.NetworkType networkType
    );

    /**
     * Counts existing rules for a site, used for priority-order suggestions.
     */
    long countBySiteId(UUID siteId);

    Optional<SiteTrustedNetwork> findByIdAndSiteId(UUID id, UUID siteId);

    boolean existsBySiteIdAndCidrBlockAndNetworkTypeAndIdNot(
            UUID siteId,
            String cidrBlock,
            com.worknest.domain.enums.NetworkType networkType,
            UUID id
    );
}

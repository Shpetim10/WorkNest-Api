package com.worknest.features.company.repository;

import com.worknest.domain.entities.SiteTrustedNetwork;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteTrustedNetworkRepository extends JpaRepository<SiteTrustedNetwork, UUID> {

    List<SiteTrustedNetwork> findAllBySiteIdOrderByPriorityOrderAscIdAsc(UUID siteId);

    Optional<SiteTrustedNetwork> findByIdAndSiteId(UUID id, UUID siteId);
}

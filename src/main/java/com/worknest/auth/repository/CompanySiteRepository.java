package com.worknest.auth.repository;

import com.worknest.auth.domain.CompanySite;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanySiteRepository extends JpaRepository<CompanySite, UUID> {

    List<CompanySite> findAllByCompanyIdOrderByNameAsc(UUID companyId);

    Optional<CompanySite> findByIdAndCompanyId(UUID id, UUID companyId);
}

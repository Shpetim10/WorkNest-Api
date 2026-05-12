package com.worknest.features.payroll.repository;

import com.worknest.domain.entities.CompanySickLeavePolicyConfig;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanySickLeavePolicyConfigRepository extends JpaRepository<CompanySickLeavePolicyConfig, UUID> {

    Optional<CompanySickLeavePolicyConfig> findByCompanyId(UUID companyId);
}

package com.worknest.features.payroll.repository;

import com.worknest.domain.entities.CompanyParentalLeavePolicyConfig;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyParentalLeavePolicyConfigRepository extends JpaRepository<CompanyParentalLeavePolicyConfig, UUID> {

    Optional<CompanyParentalLeavePolicyConfig> findByCompanyId(UUID companyId);
}

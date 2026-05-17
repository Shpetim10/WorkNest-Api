package com.worknest.features.payroll.repository;

import com.worknest.domain.entities.CompanyPayrollSettings;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyPayrollSettingsRepository extends JpaRepository<CompanyPayrollSettings, UUID> {

    Optional<CompanyPayrollSettings> findByCompanyId(UUID companyId);
}

package com.worknest.features.payroll.repository;

import com.worknest.domain.entities.CompanyTaxBracket;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyTaxBracketRepository extends JpaRepository<CompanyTaxBracket, UUID> {

    List<CompanyTaxBracket> findAllByCompanyIdOrderByOrdinalAsc(UUID companyId);

    void deleteAllByCompanyId(UUID companyId);
}

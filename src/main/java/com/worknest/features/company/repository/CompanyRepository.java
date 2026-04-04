package com.worknest.features.company.repository;

import com.worknest.domain.entities.Company;
import com.worknest.domain.enums.CompanyStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    Optional<Company> findBySlugIgnoreCase(String slug);

    Optional<Company> findBySlugIgnoreCaseAndStatus(String slug, CompanyStatus status);

    Optional<Company> findBySlugIgnoreCaseAndDeletedAtIsNull(String slug);

    Optional<Company> findBySlugIgnoreCaseAndStatusAndDeletedAtIsNull(String slug, CompanyStatus status);

    boolean existsBySlugIgnoreCaseAndDeletedAtIsNull(String slug);

    boolean existsBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCaseAndStatusAndDeletedAtIsNull(String slug, CompanyStatus status);

    List<Company> findAllByStatus(CompanyStatus status);

    List<Company> findAllByStatusAndDeletedAtIsNull(CompanyStatus status);
}

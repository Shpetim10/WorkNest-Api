package com.worknest.auth.repository;

import com.worknest.auth.domain.Company;
import com.worknest.auth.domain.CompanyStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    Optional<Company> findBySlugIgnoreCase(String slug);

    Optional<Company> findBySlugIgnoreCaseAndDeletedAtIsNull(String slug);

    boolean existsBySlugIgnoreCaseAndDeletedAtIsNull(String slug);

    List<Company> findAllByStatus(CompanyStatus status);
}

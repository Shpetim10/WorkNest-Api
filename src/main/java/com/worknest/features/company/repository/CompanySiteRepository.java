package com.worknest.features.company.repository;

import com.worknest.domain.entities.CompanySite;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanySiteRepository extends JpaRepository<CompanySite, UUID> {

    List<CompanySite> findAllByCompanyIdOrderByNameAsc(UUID companyId);

    Optional<CompanySite> findByIdAndCompanyId(UUID id, UUID companyId);

    boolean existsByCompanyIdAndCodeIgnoreCase(UUID companyId, String code);

    boolean existsByCompanyIdAndCodeIgnoreCaseAndIdNot(UUID companyId, String code, UUID id);

    @Modifying
    @Query("UPDATE CompanySite s SET s.version = :version WHERE s.id = :id")
    void updateVersionManually(@Param("id") UUID id, @Param("version") Long version);
}

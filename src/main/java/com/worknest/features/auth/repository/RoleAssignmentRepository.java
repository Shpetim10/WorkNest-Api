package com.worknest.features.auth.repository;

import com.worknest.domain.entities.RoleAssignment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoleAssignmentRepository extends JpaRepository<RoleAssignment, UUID> {

    List<RoleAssignment> findAllByUserIdAndIsActiveTrue(UUID userId);

    List<RoleAssignment> findAllByUserIdAndCompanyIdAndIsActiveTrue(UUID userId, UUID companyId);

    List<RoleAssignment> findAllByUserIdAndCompanyId(UUID userId, UUID companyId);

    long countByUserId(UUID userId);

    @Query("""
            select ra
            from RoleAssignment ra
            where ra.company.id = :companyId
              and ra.role = com.worknest.domain.enums.PlatformRole.ADMIN
              and ra.isActive = true
            """)
    Optional<RoleAssignment> findActiveAdminByCompanyId(@Param("companyId") UUID companyId);

    @Query("""
            select ra
            from RoleAssignment ra
            where ra.company.id = :companyId
              and ra.role = com.worknest.domain.enums.PlatformRole.STAFF
              and ra.isActive = true
            """)
    List<RoleAssignment> findAllActiveStaffByCompanyId(@Param("companyId") UUID companyId);

    List<RoleAssignment> findAllByUserIdAndIsActive(UUID userId, Boolean isActive);

    Optional<RoleAssignment> findFirstByUserIdAndCompanyIdAndIsActiveTrue(UUID userId, UUID companyId);

    Optional<RoleAssignment> findFirstByUserIdAndCompanyIdOrderByCreatedAtAsc(UUID userId, UUID companyId);
}

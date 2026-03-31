package com.worknest.auth.repository;

import com.worknest.auth.domain.RoleAssignment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoleAssignmentRepository extends JpaRepository<RoleAssignment, UUID> {

    List<RoleAssignment> findAllByUserIdAndIsActiveTrue(UUID userId);

    List<RoleAssignment> findAllByUserIdAndCompanyIdAndIsActiveTrue(UUID userId, UUID companyId);

    @Query("""
            select ra
            from RoleAssignment ra
            where ra.company.id = :companyId
              and ra.role = com.worknest.auth.domain.PlatformRole.ADMIN
              and ra.isActive = true
            """)
    Optional<RoleAssignment> findActiveAdminByCompanyId(@Param("companyId") UUID companyId);

    List<RoleAssignment> findAllByUserIdAndIsActive(UUID userId, Boolean isActive);
}

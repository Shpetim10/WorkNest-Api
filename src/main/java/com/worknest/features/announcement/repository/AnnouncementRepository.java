package com.worknest.features.announcement.repository;

import com.worknest.domain.entities.Announcement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {

    List<Announcement> findAllByCompanyIdOrderByCreatedAtDesc(UUID companyId);

    @Query("""
            SELECT DISTINCT a FROM Announcement a
            LEFT JOIN FETCH a.targetEmployees te
            LEFT JOIN FETCH te.user
            WHERE a.company.id = :companyId
            ORDER BY a.createdAt DESC
            """)
    List<Announcement> findAllForAdminByCompanyId(@Param("companyId") UUID companyId);

    Optional<Announcement> findByIdAndCompanyId(UUID id, UUID companyId);

    @Query("""
            SELECT DISTINCT a FROM Announcement a
            WHERE a.company.id = :companyId
            AND (
                a.targetAudience = com.worknest.domain.enums.AnnouncementAudience.ALL_EMPLOYEES
                OR (a.targetAudience = com.worknest.domain.enums.AnnouncementAudience.SPECIFIC_USERS
                    AND EXISTS (SELECT e FROM a.targetEmployees e WHERE e.id = :employeeId))
                OR (a.targetAudience = com.worknest.domain.enums.AnnouncementAudience.DEPARTMENT
                    AND :departmentId IS NOT NULL
                    AND EXISTS (SELECT d FROM a.targetDepartments d WHERE d.id = :departmentId))
            )
            ORDER BY a.createdAt DESC
            """)
    List<Announcement> findVisibleToEmployee(
            @Param("companyId") UUID companyId,
            @Param("employeeId") UUID employeeId,
            @Param("departmentId") UUID departmentId
    );
}
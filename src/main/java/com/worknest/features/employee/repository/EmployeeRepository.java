package com.worknest.features.employee.repository;

import com.worknest.domain.entities.Employee;
import com.worknest.domain.enums.EmploymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import com.worknest.domain.enums.PlatformRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.LocalDate;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    long countByCompanyIdAndEmploymentStatus(UUID companyId, EmploymentStatus employmentStatus);

    @Query("SELECT e FROM Employee e WHERE e.company.id = :companyId AND e.employmentTypeRole = :role AND e.supervisorRoleAssignment.id = :managerId")
    List<Employee> findAllAssignedToManager(@Param("companyId") UUID companyId, @Param("role") PlatformRole role, @Param("managerId") UUID managerId);

    @Query("SELECT e FROM Employee e WHERE e.company.id = :companyId AND e.employmentTypeRole = :role AND (e.supervisorRoleAssignment.id != :managerId OR e.supervisorRoleAssignment IS NULL)")
    List<Employee> findAllNotAssignedToManager(@Param("companyId") UUID companyId, @Param("role") PlatformRole role, @Param("managerId") UUID managerId);

    @Query("SELECT e FROM Employee e WHERE e.company.id = :companyId AND e.employmentTypeRole IN (:roles)")
    List<Employee> findAllByCompanyIdAndEmploymentTypeRoleIn(@Param("companyId") UUID companyId, @Param("roles") List<PlatformRole> roles);

    @Query("SELECT e FROM Employee e WHERE e.company.id = :companyId AND e.employmentTypeRole IN (:roles) AND (:deptId IS NULL OR e.department.id = :deptId)")
    List<Employee> findByCompanyAndRolesAndDepartment(
            @Param("companyId") UUID companyId, 
            @Param("roles") List<PlatformRole> roles, 
            @Param("deptId") UUID deptId);

    @Query("SELECT e FROM Employee e WHERE e.company.id = :companyId AND e.employmentTypeRole IN (:roles) AND (:deptId IS NULL OR e.department.id = :deptId) AND e.employmentStatus!=EmploymentStatus.PENDING AND e.startDate <= CURRENT_DATE AND e.contractExpiryDate>=CURRENT_DATE ")
    List<Employee> findByCompanyAndRolesAndDepartmentAndEmploymentStatusNotPendingAndTimeWithinContract(
            @Param("companyId") UUID companyId,
            @Param("roles") List<PlatformRole> roles,
            @Param("deptId") UUID deptId);

    @Query("SELECT e FROM Employee e WHERE e.company.id = :companyId AND e.employmentTypeRole = :role AND e.supervisorRoleAssignment IS NULL AND e.department.id = :deptId")
    List<Employee> findUnassignedEmployeesByDepartment(@Param("companyId") UUID companyId, @Param("role") PlatformRole role, @Param("deptId") UUID deptId);

    @Query("""
            SELECT e
            FROM Employee e
            WHERE e.company.id = :companyId
              AND e.employmentTypeRole = :role
              AND e.department.id = :departmentId
              AND e.supervisorRoleAssignment.id = :supervisorRoleAssignmentId
            """)
    List<Employee> findAssignedEmployeesByDepartmentAndSupervisor(
            @Param("companyId") UUID companyId,
            @Param("role") PlatformRole role,
            @Param("departmentId") UUID departmentId,
            @Param("supervisorRoleAssignmentId") UUID supervisorRoleAssignmentId);

    java.util.Optional<Employee> findByUserIdAndCompanyId(UUID userId, UUID companyId);

    java.util.Optional<Employee> findByIdAndCompanyId(UUID id, UUID companyId);

    List<Employee> findAllByCompanyId(UUID companyId);

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.supervisorRoleAssignment.id = :managerId")
    long countBySupervisorRoleAssignmentId(@Param("managerId") UUID managerId);

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.department.id=:departmentId")
    int countByDepartmentId(@Param("departmentId") UUID departmentId);

    @Query("""
            SELECT e FROM Employee e
            WHERE e.company.id = :companyId
              AND (:employeeIds IS NULL OR e.id IN :employeeIds)
              AND e.startDate <= :periodEnd
              AND (e.contractExpiryDate IS NULL OR e.contractExpiryDate >= :periodStart)
            ORDER BY e.createdAt ASC
            """)
    List<Employee> findPayrollCandidates(
            @Param("companyId") UUID companyId,
            @Param("employeeIds") List<UUID> employeeIds,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd);

    @Query(
            value = """
                    SELECT e
                    FROM Employee e
                    JOIN e.user u
                    WHERE e.company.id = :companyId
                      AND e.startDate <= :periodEnd
                      AND (e.contractExpiryDate IS NULL OR e.contractExpiryDate >= :periodStart)
                      AND (
                            :search IS NULL
                            OR :search = ''
                            OR LOWER(COALESCE(u.displayName, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                          )
                    ORDER BY LOWER(COALESCE(u.displayName, '')) ASC, e.createdAt ASC
                    """,
            countQuery = """
                    SELECT COUNT(e)
                    FROM Employee e
                    JOIN e.user u
                    WHERE e.company.id = :companyId
                      AND e.startDate <= :periodEnd
                      AND (e.contractExpiryDate IS NULL OR e.contractExpiryDate >= :periodStart)
                      AND (
                            :search IS NULL
                            OR :search = ''
                            OR LOWER(COALESCE(u.displayName, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                          )
                    """
    )
    Page<Employee> findPayrollCandidatesForAdmin(
            @Param("companyId") UUID companyId,
            @Param("search") String search,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT e
                    FROM Employee e
                    JOIN e.user u
                    WHERE e.company.id = :companyId
                      AND e.supervisorRoleAssignment.id = :supervisorRoleAssignmentId
                      AND e.employmentTypeRole = com.worknest.domain.enums.PlatformRole.EMPLOYEE
                      AND e.startDate <= :periodEnd
                      AND (e.contractExpiryDate IS NULL OR e.contractExpiryDate >= :periodStart)
                      AND (
                            :search IS NULL
                            OR :search = ''
                            OR LOWER(COALESCE(u.displayName, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                          )
                    ORDER BY LOWER(COALESCE(u.displayName, '')) ASC, e.createdAt ASC
                    """,
            countQuery = """
                    SELECT COUNT(e)
                    FROM Employee e
                    JOIN e.user u
                    WHERE e.company.id = :companyId
                      AND e.supervisorRoleAssignment.id = :supervisorRoleAssignmentId
                      AND e.employmentTypeRole = com.worknest.domain.enums.PlatformRole.EMPLOYEE
                      AND e.startDate <= :periodEnd
                      AND (e.contractExpiryDate IS NULL OR e.contractExpiryDate >= :periodStart)
                      AND (
                            :search IS NULL
                            OR :search = ''
                            OR LOWER(COALESCE(u.displayName, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                          )
                    """
    )
    Page<Employee> findPayrollCandidatesForStaff(
            @Param("companyId") UUID companyId,
            @Param("supervisorRoleAssignmentId") UUID supervisorRoleAssignmentId,
            @Param("search") String search,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(e) > 0
            FROM Employee e
            WHERE e.company.id = :companyId
              AND e.id = :employeeId
              AND e.supervisorRoleAssignment.id = :supervisorRoleAssignmentId
            """)
    boolean isEmployeeAssignedToSupervisor(
            @Param("companyId") UUID companyId,
            @Param("employeeId") UUID employeeId,
            @Param("supervisorRoleAssignmentId") UUID supervisorRoleAssignmentId
    );
}

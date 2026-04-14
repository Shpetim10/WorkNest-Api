package com.worknest.features.employee.repository;

import com.worknest.domain.entities.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import com.worknest.domain.enums.PlatformRole;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

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

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.supervisorRoleAssignment.id = :managerId")
    long countBySupervisorRoleAssignmentId(@Param("managerId") UUID managerId);
}

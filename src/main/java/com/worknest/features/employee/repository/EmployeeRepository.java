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
}

package com.worknest.features.department.repository;

import com.worknest.domain.entities.Department;
import com.worknest.domain.enums.DepartmentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    List<Department> findAllByCompanyId(UUID companyId);

    List<Department> findAllByCompanyIdAndStatus(UUID companyId, DepartmentStatus status);

    Optional<Department> findByIdAndCompanyId(UUID id, UUID companyId);

    boolean existsByCompanyIdAndName(UUID companyId, String name);

    boolean existsByCompanyIdAndNameAndIdNot(UUID companyId, String name, UUID id);
}

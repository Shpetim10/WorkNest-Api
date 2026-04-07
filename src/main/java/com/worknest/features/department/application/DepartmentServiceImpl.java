package com.worknest.features.department.application;

import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.Department;
import com.worknest.domain.enums.DepartmentStatus;
import com.worknest.features.department.dto.CreateDepartmentRequest;
import com.worknest.features.department.dto.DepartmentListResponse;
import com.worknest.features.department.dto.DepartmentLookup;
import com.worknest.features.department.dto.DepartmentResponse;
import com.worknest.features.department.dto.UpdateDepartmentRequest;
import com.worknest.features.department.exception.DepartmentNotFoundException;
import com.worknest.features.department.exception.DuplicateDepartmentNameException;
import com.worknest.features.department.repository.DepartmentRepository;
import com.worknest.tenant.TenantContextHolder;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EntityManager entityManager;

    private UUID getCurrentCompanyId() {
        return TenantContextHolder.get()
                .orElseThrow(() -> new IllegalStateException("No tenant context found"))
                .companyId();
    }

    private Company getCurrentCompanyReference() {
        return entityManager.getReference(Company.class, getCurrentCompanyId());
    }

    @Override
    @Transactional
    public DepartmentResponse createDepartment(CreateDepartmentRequest request) {
        UUID companyId = getCurrentCompanyId();
        
        if (departmentRepository.existsByCompanyIdAndName(companyId, request.name().trim())) {
            throw new DuplicateDepartmentNameException();
        }

        Department department = new Department();
        department.setCompany(getCurrentCompanyReference());
        department.setName(request.name().trim());
        department.setDescription(request.description());
        department.setStatus(request.statusOrDefault());

        department = departmentRepository.save(department);
        return DepartmentResponse.fromEntity(department);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getDepartment(UUID id) {
        Department department = departmentRepository.findByIdAndCompanyId(id, getCurrentCompanyId())
                .orElseThrow(DepartmentNotFoundException::new);
        return DepartmentResponse.fromEntity(department);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentListResponse> listDepartments() {
        return departmentRepository.findAllByCompanyId(getCurrentCompanyId())
                .stream()
                .map(DepartmentListResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public DepartmentResponse updateDepartment(UUID id, UpdateDepartmentRequest request) {
        UUID companyId = getCurrentCompanyId();
        
        Department department = departmentRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(DepartmentNotFoundException::new);

        if (departmentRepository.existsByCompanyIdAndNameAndIdNot(companyId, request.name().trim(), id)) {
            throw new DuplicateDepartmentNameException();
        }

        department.setName(request.name().trim());
        department.setDescription(request.description());
        department.setStatus(request.status());

        department = departmentRepository.save(department);
        return DepartmentResponse.fromEntity(department);
    }

    @Override
    @Transactional
    public void deleteDepartment(UUID id) {
        Department department = departmentRepository.findByIdAndCompanyId(id, getCurrentCompanyId())
                .orElseThrow(DepartmentNotFoundException::new);

        // TODO: Deletion must later be blocked if employees are assigned to the department.
        // Employee-assignment check is not implemented yet in this codebase.

        departmentRepository.delete(department);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentLookup> lookupDepartments() {
        return departmentRepository.findAllByCompanyIdAndStatus(getCurrentCompanyId(), DepartmentStatus.ACTIVE)
                .stream()
                .map(DepartmentLookup::fromEntity)
                .toList();
    }
}

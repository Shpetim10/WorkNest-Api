package com.worknest.features.department.application;

import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.Department;
import com.worknest.domain.enums.DepartmentStatus;
import com.worknest.features.department.dto.CreateDepartmentRequest;
import com.worknest.features.department.dto.DepartmentListResponse;
import com.worknest.features.department.dto.DepartmentLookup;
import com.worknest.features.department.dto.DepartmentResponse;
import com.worknest.features.department.dto.UpdateDepartmentRequest;
import com.worknest.features.department.exception.DepartmentCannotBeDeleted;
import com.worknest.features.department.exception.DepartmentNotFoundException;
import com.worknest.features.department.exception.DuplicateDepartmentNameException;
import com.worknest.features.department.repository.DepartmentRepository;
import com.worknest.features.employee.repository.EmployeeRepository;
import com.worknest.realtime.event.DepartmentCreatedDomainEvent;
import com.worknest.realtime.event.DepartmentDeletedDomainEvent;
import com.worknest.realtime.event.DepartmentUpdatedDomainEvent;
import com.worknest.security.AuthSessionPrincipal;
import com.worknest.tenant.TenantContextHolder;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EntityManager entityManager;
    private final EmployeeRepository employeeRepository;
    private final ApplicationEventPublisher eventPublisher;

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
        DepartmentResponse response = DepartmentResponse.fromEntity(department);
        eventPublisher.publishEvent(new DepartmentCreatedDomainEvent(companyId, department.getId(), resolveActorUserId(), response));
        return response;
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
    public List<DepartmentListResponse> listDepartments(UUID companyId) {
        return departmentRepository.findAllByCompanyId(companyId)
                .stream()
                .map(e->{
                    int employeeCount= employeeRepository.countByDepartmentId(e.getId());
                    return DepartmentListResponse.fromEntity(e,employeeCount);
                })
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
        DepartmentResponse response = DepartmentResponse.fromEntity(department);
        eventPublisher.publishEvent(new DepartmentUpdatedDomainEvent(companyId, department.getId(), resolveActorUserId(), response));
        return response;
    }

    @Override
    @Transactional
    public void deleteDepartment(UUID id) {
        Department department = departmentRepository.findByIdAndCompanyId(id, getCurrentCompanyId())
                .orElseThrow(DepartmentNotFoundException::new);

        if(employeeRepository.countByDepartmentId(department.getId()) > 0) {
            throw new DepartmentCannotBeDeleted("There are some employees in this department");
        }

        UUID companyId = department.getCompany().getId();
        String name = department.getName();
        departmentRepository.delete(department);
        eventPublisher.publishEvent(new DepartmentDeletedDomainEvent(companyId, id, resolveActorUserId(), name));
    }

    private UUID resolveActorUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthSessionPrincipal p) {
            return p.userId();
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentLookup> lookupDepartments(UUID companyId) {
        return departmentRepository.findAllByCompanyIdAndStatus(companyId, DepartmentStatus.ACTIVE)
                .stream()
                .map(DepartmentLookup::fromEntity)
                .toList();
    }
}

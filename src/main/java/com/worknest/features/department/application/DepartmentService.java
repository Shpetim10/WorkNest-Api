package com.worknest.features.department.application;

import com.worknest.features.department.dto.CreateDepartmentRequest;
import com.worknest.features.department.dto.DepartmentListResponse;
import com.worknest.features.department.dto.DepartmentLookup;
import com.worknest.features.department.dto.DepartmentResponse;
import com.worknest.features.department.dto.UpdateDepartmentRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DepartmentService {
    DepartmentResponse createDepartment(CreateDepartmentRequest request);
    DepartmentResponse getDepartment(UUID id);
    Page<DepartmentListResponse> listDepartments(UUID companyId, Pageable pageable);
    DepartmentResponse updateDepartment(UUID id, UpdateDepartmentRequest request);
    void deleteDepartment(UUID id);
    List<DepartmentLookup> lookupDepartments(UUID companyId);
}

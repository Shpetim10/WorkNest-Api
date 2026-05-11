package com.worknest.features.employee.application;

import com.worknest.features.employee.dto.EmployeeListResponse;
import com.worknest.features.employee.dto.StaffListResponse;
import com.worknest.features.employee.dto.EmployeeDetailsResponse;
import com.worknest.features.employee.dto.StaffDetailsResponse;
import com.worknest.features.employee.dto.StaffLookup;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EmployeeQueryService {
    Page<StaffListResponse> listStaff(UUID companyId, Pageable pageable);
    Page<EmployeeListResponse> listEmployees(UUID companyId, Pageable pageable);
    Page<EmployeeListResponse> listUnassignedEmployees(UUID companyId, UUID departmentId, Pageable pageable);
    Page<EmployeeListResponse> listAssignedEmployees(UUID companyId, UUID departmentId, UUID supervisorRoleAssignmentId, Pageable pageable);
    List<StaffLookup> lookupStaff(UUID companyId, UUID departmentId);
    
    EmployeeDetailsResponse getEmployee(UUID companyId, UUID employeeId);
    StaffDetailsResponse getStaff(UUID companyId, UUID staffId);
}

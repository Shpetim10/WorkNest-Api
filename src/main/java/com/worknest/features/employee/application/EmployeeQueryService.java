package com.worknest.features.employee.application;

import com.worknest.features.employee.dto.EmployeeListResponse;
import com.worknest.features.employee.dto.StaffListResponse;
import com.worknest.features.employee.dto.EmployeeDetailsResponse;
import com.worknest.features.employee.dto.StaffDetailsResponse;
import com.worknest.features.employee.dto.StaffLookup;
import java.util.List;
import java.util.UUID;

public interface EmployeeQueryService {
    List<StaffListResponse> listStaff(UUID companyId);
    List<EmployeeListResponse> listEmployees(UUID companyId);
    List<EmployeeListResponse> listUnassignedEmployees(UUID companyId, UUID departmentId);
    List<EmployeeListResponse> listAssignedEmployees(UUID companyId, UUID departmentId, UUID supervisorRoleAssignmentId);
    List<StaffLookup> lookupStaff(UUID companyId, UUID departmentId);
    
    EmployeeDetailsResponse getEmployee(UUID companyId, UUID employeeId);
    StaffDetailsResponse getStaff(UUID companyId, UUID staffId);
}

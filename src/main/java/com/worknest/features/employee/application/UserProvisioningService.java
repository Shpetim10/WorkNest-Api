package com.worknest.features.employee.application;

import com.worknest.features.employee.dto.CreateEmployeeRequest;
import com.worknest.features.employee.dto.CreateStaffRequest;
import com.worknest.features.employee.dto.ProvisioningResponse;
import com.worknest.features.employee.dto.UpdateEmployeeRequest;
import com.worknest.features.employee.dto.UpdateEmployeeResponse;
import com.worknest.features.employee.dto.UpdateStaffRequest;

public interface UserProvisioningService {

    ProvisioningResponse createEmployee(CreateEmployeeRequest request);

    ProvisioningResponse createStaff(CreateStaffRequest request);

    ProvisioningResponse resendInvitation(java.util.UUID companyId, java.util.UUID employeeId);

    UpdateEmployeeResponse updateEmployee(java.util.UUID companyId, java.util.UUID employeeId, UpdateEmployeeRequest request);

    UpdateEmployeeResponse updateStaff(java.util.UUID companyId, java.util.UUID employeeId, UpdateStaffRequest request);
}

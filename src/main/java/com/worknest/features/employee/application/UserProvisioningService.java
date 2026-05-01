package com.worknest.features.employee.application;

import com.worknest.features.employee.dto.CreateEmployeeRequest;
import com.worknest.features.employee.dto.CreateStaffRequest;
import com.worknest.features.employee.dto.ProvisioningResponse;
import com.worknest.features.employee.dto.UpdateEmployeeJobDetailsRequest;
import com.worknest.features.employee.dto.UpdateEmployeeRequest;
import com.worknest.features.employee.dto.UpdateEmployeeResponse;
import com.worknest.features.employee.dto.UpdateStaffJobDetailsRequest;
import com.worknest.features.employee.dto.UpdateStaffRequest;

import java.util.UUID;

public interface UserProvisioningService {

    ProvisioningResponse createEmployee(CreateEmployeeRequest request);

    ProvisioningResponse createStaff(CreateStaffRequest request);

    ProvisioningResponse resendInvitation(UUID companyId, UUID employeeId);

    UpdateEmployeeResponse updateEmployee(UUID companyId, UUID employeeId, UpdateEmployeeRequest request);

    UpdateEmployeeResponse updateStaff(UUID companyId, UUID employeeId, UpdateStaffRequest request);

    UpdateEmployeeResponse updateEmployeeJobDetails(UUID companyId, UUID employeeId, UpdateEmployeeJobDetailsRequest request);

    UpdateEmployeeResponse updateStaffJobDetails(UUID companyId, UUID employeeId, UpdateStaffJobDetailsRequest request);

    void deleteEmployee(UUID companyId, UUID employeeId);

    void deleteStaff(UUID companyId, UUID employeeId);

    void activateEmployee(UUID companyId, UUID employeeId);

    void terminateEmployee(UUID companyId, UUID employeeId);
}

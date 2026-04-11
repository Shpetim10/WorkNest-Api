package com.worknest.features.employee.application;

import com.worknest.features.employee.dto.CreateEmployeeRequest;
import com.worknest.features.employee.dto.CreateStaffRequest;
import com.worknest.features.employee.dto.ProvisioningResponse;

public interface UserProvisioningService {

    ProvisioningResponse createEmployee(CreateEmployeeRequest request);

    ProvisioningResponse createStaff(CreateStaffRequest request);

}

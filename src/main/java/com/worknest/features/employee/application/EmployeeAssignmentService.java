package com.worknest.features.employee.application;

import com.worknest.features.employee.dto.EmployeeAssignmentBoardResponse;
import com.worknest.features.employee.dto.ManagerSummaryDto;
import com.worknest.features.employee.dto.UpdateEmployeeAssignmentsRequest;

import java.util.List;
import java.util.UUID;

public interface EmployeeAssignmentService {

    List<ManagerSummaryDto> listAssignableManagers(UUID companyId);

    EmployeeAssignmentBoardResponse getManagerAssignmentBoard(UUID companyId, UUID managerRoleAssignmentId);

    void updateManagerAssignments(UUID companyId, UUID managerRoleAssignmentId, UpdateEmployeeAssignmentsRequest request);
}

package com.worknest.features.employee.dto;

import com.worknest.common.api.PaginatedResponse;

public record EmployeeAssignmentBoardResponse(
    ManagerSummaryDto manager,
    PaginatedResponse<EmployeeSummaryDto> assignedEmployees,
    PaginatedResponse<EmployeeSummaryDto> unassignedEmployees,
    int assignedCount,
    int unassignedCount
) {}

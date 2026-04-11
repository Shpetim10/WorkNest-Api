package com.worknest.features.employee.dto;

import java.util.List;

public record EmployeeAssignmentBoardResponse(
    ManagerSummaryDto manager,
    List<EmployeeSummaryDto> assignedEmployees,
    List<EmployeeSummaryDto> unassignedEmployees,
    int assignedCount,
    int unassignedCount
) {}

package com.worknest.features.employee.application;

import com.worknest.features.employee.dto.EmployeeAssignmentBoardResponse;
import com.worknest.features.employee.dto.ManagerSummaryDto;
import com.worknest.features.employee.dto.UpdateEmployeeAssignmentsRequest;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EmployeeAssignmentService {

    Page<ManagerSummaryDto> listAssignableManagers(UUID companyId, Pageable pageable);

    EmployeeAssignmentBoardResponse getManagerAssignmentBoard(
            UUID companyId,
            UUID managerRoleAssignmentId,
            Pageable assignedPageable,
            Pageable unassignedPageable
    );

    void updateManagerAssignments(UUID companyId, UUID managerRoleAssignmentId, UpdateEmployeeAssignmentsRequest request);
}

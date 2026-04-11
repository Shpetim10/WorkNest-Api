package com.worknest.features.employee.web;

import com.worknest.common.api.ApiResponse;
import com.worknest.features.employee.application.EmployeeAssignmentService;
import com.worknest.features.employee.dto.EmployeeAssignmentBoardResponse;
import com.worknest.features.employee.dto.ManagerSummaryDto;
import com.worknest.features.employee.dto.UpdateEmployeeAssignmentsRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/staff")
@RequiredArgsConstructor
@Tag(name = "Employee Assignments", description = "Endpoints for managing employee-to-manager supervisory relationships")
public class EmployeeAssignmentController {

    private final EmployeeAssignmentService employeeAssignmentService;

    @GetMapping("/managers")
    @Operation(summary = "List Assignable Managers", description = "Retrieves all STAFF members in the company capable of supervising employees.")
    @PreAuthorize("@teamSecurity.hasPermission(#companyId, 'MANAGE_ASSIGNMENTS')")
    public ResponseEntity<ApiResponse<List<ManagerSummaryDto>>> listManagers(@PathVariable UUID companyId) {
        List<ManagerSummaryDto> managers = employeeAssignmentService.listAssignableManagers(companyId);
        return ResponseEntity.ok(ApiResponse.success("Managers retrieved successfully", managers));
    }

    @GetMapping("/{roleAssignmentId}/employee-assignments")
    @Operation(summary = "Get Manager Assignment Board", description = "Retrieves both the assigned and unassigned employees for a specific manager.")
    @PreAuthorize("@teamSecurity.hasPermission(#companyId, 'MANAGE_ASSIGNMENTS')")
    public ResponseEntity<ApiResponse<EmployeeAssignmentBoardResponse>> getAssignmentBoard(
            @PathVariable UUID companyId,
            @PathVariable UUID roleAssignmentId) {
        EmployeeAssignmentBoardResponse board = employeeAssignmentService.getManagerAssignmentBoard(companyId, roleAssignmentId);
        return ResponseEntity.ok(ApiResponse.success("Assignment board retrieved successfully", board));
    }

    @PutMapping("/{roleAssignmentId}/employee-assignments")
    @Operation(summary = "Update Manager Assignments", description = "Shuttles employees between assigned and unassigned states for a manager. Automatically audits history.")
    @PreAuthorize("@teamSecurity.hasPermission(#companyId, 'MANAGE_ASSIGNMENTS')")
    public ResponseEntity<ApiResponse<Void>> updateAssignments(
            @PathVariable UUID companyId,
            @PathVariable UUID roleAssignmentId,
            @RequestBody @Valid UpdateEmployeeAssignmentsRequest request) {
        employeeAssignmentService.updateManagerAssignments(companyId, roleAssignmentId, request);
        return ResponseEntity.ok(ApiResponse.success("Assignments updated successfully", null));
    }
}

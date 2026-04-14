package com.worknest.features.employee.web;

import com.worknest.common.api.ApiResponse;
import com.worknest.features.employee.application.EmployeeQueryService;
import com.worknest.features.employee.dto.EmployeeDetailsResponse;
import com.worknest.features.employee.dto.EmployeeListResponse;
import com.worknest.features.employee.dto.StaffDetailsResponse;
import com.worknest.features.employee.dto.StaffListResponse;
import com.worknest.features.employee.dto.StaffLookup;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies/{companyId}")
@RequiredArgsConstructor
@Tag(name = "Employee Query", description = "Endpoints for retrieving staff and employee management lists")
public class EmployeeQueryController {

    private final EmployeeQueryService employeeQueryService;

    @GetMapping("/staff")
    @Operation(summary = "List Company Staff", description = "Retrieves a comprehensive list of all staff and admin members for the company table view.")
    //@PreAuthorize("@teamSecurity.hasPermission(#companyId, 'MANAGE_STAFF')")
    public ApiResponse<List<StaffListResponse>> listStaff(@PathVariable UUID companyId) {
        return ApiResponse.success("Staff members retrieved successfully", employeeQueryService.listStaff(companyId));
    }

    @GetMapping("/staff/{staffId}")
    @Operation(summary = "Get Staff Details", description = "Retrieves full details for a specific staff member.")
    //@PreAuthorize("@teamSecurity.hasPermission(#companyId, 'VIEW_STAFF')")
    public ApiResponse<StaffDetailsResponse> getStaff(
            @PathVariable UUID companyId,
            @PathVariable UUID staffId
    ) {
        return ApiResponse.success("Staff details retrieved successfully", employeeQueryService.getStaff(companyId, staffId));
    }

    @GetMapping("/employees")
    @Operation(summary = "List Company Employees", description = "Retrieves a comprehensive list of all mobile/field employees for the company table view.")
    //@PreAuthorize("@teamSecurity.hasPermission(#companyId, 'MANAGE_EMPLOYEES')")
    public ApiResponse<List<EmployeeListResponse>> listEmployees(@PathVariable UUID companyId) {
        return ApiResponse.success("Employees retrieved successfully", employeeQueryService.listEmployees(companyId));
    }

    @GetMapping("/employees/{employeeId}")
    @Operation(summary = "Get Employee Details", description = "Retrieves full details for a specific employee.")
    //@PreAuthorize("@teamSecurity.hasPermission(#companyId, 'VIEW_EMPLOYEES')")
    public ApiResponse<EmployeeDetailsResponse> getEmployee(
            @PathVariable UUID companyId,
            @PathVariable UUID employeeId
    ) {
        return ApiResponse.success("Employee details retrieved successfully", employeeQueryService.getEmployee(companyId, employeeId));
    }

    @GetMapping("/employees/unassigned")
    @Operation(summary = "List Unassigned Employees", description = "Retrieves a list of employees not assigned to any manager within a specific department.")
    //@PreAuthorize("@teamSecurity.hasPermission(#companyId, 'MANAGE_EMPLOYEES')")
    public ApiResponse<List<EmployeeListResponse>> listUnassignedEmployees(
            @PathVariable UUID companyId,
            @RequestParam UUID departmentId) {
        return ApiResponse.success("Unassigned employees retrieved successfully", employeeQueryService.listUnassignedEmployees(companyId, departmentId));
    }

    @GetMapping("/employees/assigned")
    @Operation(summary = "List Assigned Employees", description = "Retrieves employees assigned to a specific supervisor within a specific department.")
    //@PreAuthorize("@teamSecurity.hasPermission(#companyId, 'MANAGE_EMPLOYEES')")
    public ApiResponse<List<EmployeeListResponse>> listAssignedEmployees(
            @PathVariable UUID companyId,
            @RequestParam UUID departmentId,
            @RequestParam UUID supervisorRoleAssignmentId) {
        return ApiResponse.success(
                "Assigned employees retrieved successfully",
                employeeQueryService.listAssignedEmployees(companyId, departmentId, supervisorRoleAssignmentId)
        );
    }

    @GetMapping("/staff/lookup")
    @Operation(summary = "Lookup Staff", description = "Retrieves a lightweight list of staff (fullName/id) for dropdowns. Supports filtering by department.")
    @PreAuthorize("@teamSecurity.hasPermission(#companyId, 'VIEW_STAFF')")
    public ApiResponse<List<StaffLookup>> lookupStaff(
            @PathVariable UUID companyId,
            @RequestParam(required = false) UUID departmentId) {
        return ApiResponse.success("Staff lookup retrieved successfully", employeeQueryService.lookupStaff(companyId, departmentId));
    }
}

package com.worknest.features.employee.web;

import com.worknest.common.api.ApiResponse;
import com.worknest.features.employee.application.UserProvisioningService;
import com.worknest.features.employee.dto.CreateEmployeeRequest;
import com.worknest.features.employee.dto.CreateStaffRequest;
import com.worknest.features.employee.dto.ProvisioningResponse;
import com.worknest.features.employee.dto.UpdateEmployeeRequest;
import com.worknest.features.employee.dto.UpdateEmployeeResponse;
import com.worknest.features.employee.dto.UpdateStaffRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/provisioning")
@RequiredArgsConstructor
@Tag(name = "User Provisioning", description = "Endpoints for dispatching STAFF and EMPLOYEE profiles via explicit organizational onboarding")
public class UserProvisioningController {

    private final UserProvisioningService userProvisioningService;

    @PostMapping("/employee")
    @Operation(summary = "Provision Employee", description = "Creates a lightweight mobile-only user and attaches an Employee hierarchy to a predefined supervisor.")
    //@PreAuthorize("@teamSecurity.hasPermission(#companyId, 'PROVISION_USERS')")
    public ResponseEntity<ApiResponse<ProvisioningResponse>> provisionEmployee(
            @PathVariable UUID companyId,
            @RequestBody @Valid CreateEmployeeRequest request) {

        // Validating the internal structure since its part of the path
        if (!companyId.equals(request.companyId())) {
            throw new IllegalArgumentException("Company ID in path does not match request body");
        }

        ProvisioningResponse response = userProvisioningService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Employee provisioned successfully", response));
    }

    @PostMapping("/staff")
    @Operation(summary = "Provision Staff", description = "Creates a STAFF profile with specific permissions configurable upon creation.")
    //@PreAuthorize("@teamSecurity.hasPermission(#companyId, 'PROVISION_USERS')")
    public ResponseEntity<ApiResponse<ProvisioningResponse>> provisionStaff(
            @PathVariable UUID companyId,
            @RequestBody @Valid CreateStaffRequest request) {

        if (!companyId.equals(request.companyId())) {
            throw new IllegalArgumentException("Company ID in path does not match request body");
        }

        ProvisioningResponse response = userProvisioningService.createStaff(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Staff provisioned successfully", response));
    }

    @PostMapping("/{employeeId}/resend")
    @Operation(summary = "Resend Invitation", description = "Generates a new invitation token and resends the onboarding email for a pending employee.")
    //@PreAuthorize("@teamSecurity.hasPermission(#companyId, 'PROVISION_USERS')")
    public ResponseEntity<ApiResponse<ProvisioningResponse>> resendInvitation(
            @PathVariable UUID companyId,
            @PathVariable UUID employeeId) {
        ProvisioningResponse response = userProvisioningService.resendInvitation(companyId, employeeId);
        return ResponseEntity.ok(ApiResponse.success("Invitation resent successfully", response));
    }

    // -------------------------------------------------------------------------
    // Update endpoints
    // -------------------------------------------------------------------------

    @PutMapping("/employee/{employeeId}")
    @Operation(
            summary = "Update Employee",
            description = "Updates an existing EMPLOYEE's personal details (name, jobTitle), organisational placement "
                    + "(department, site, supervisor) and start date. All fields are required in the request body; "
                    + "optional fields (departmentId, companySiteId, supervisorRoleAssignmentId, startDate) may be null "
                    + "to clear or leave them unchanged."
    )
    //@PreAuthorize("@teamSecurity.hasPermission(#companyId, 'MANAGE_USERS')")
    public ResponseEntity<ApiResponse<UpdateEmployeeResponse>> updateEmployee(
            @PathVariable UUID companyId,
            @PathVariable UUID employeeId,
            @RequestBody @Valid UpdateEmployeeRequest request) {

        UpdateEmployeeResponse response = userProvisioningService.updateEmployee(companyId, employeeId, request);
        return ResponseEntity.ok(ApiResponse.success("Employee updated successfully", response));
    }

    @PutMapping("/staff/{employeeId}")
    @Operation(
            summary = "Update Staff",
            description = "Updates an existing STAFF member's personal details (name, jobTitle), organisational placement "
                    + "(department, site) and start date. Optionally replaces the full permission set and the list of "
                    + "supervised employees. Pass null for permissionCodes / assignedEmployeeIds to leave those unchanged; "
                    + "pass an empty list to clear them."
    )
    //@PreAuthorize("@teamSecurity.hasPermission(#companyId, 'MANAGE_USERS')")
    public ResponseEntity<ApiResponse<UpdateEmployeeResponse>> updateStaff(
            @PathVariable UUID companyId,
            @PathVariable UUID employeeId,
            @RequestBody @Valid UpdateStaffRequest request) {

        UpdateEmployeeResponse response = userProvisioningService.updateStaff(companyId, employeeId, request);
        return ResponseEntity.ok(ApiResponse.success("Staff updated successfully", response));
    }
}


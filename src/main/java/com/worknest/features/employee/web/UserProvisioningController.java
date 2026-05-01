package com.worknest.features.employee.web;

import com.worknest.common.api.ApiResponse;
import com.worknest.features.employee.application.UserProvisioningService;
import com.worknest.features.employee.dto.CreateEmployeeRequest;
import com.worknest.features.employee.dto.CreateStaffRequest;
import com.worknest.features.employee.dto.ProvisioningResponse;
import com.worknest.features.employee.dto.UpdateEmployeeJobDetailsRequest;
import com.worknest.features.employee.dto.UpdateEmployeeRequest;
import com.worknest.features.employee.dto.UpdateEmployeeResponse;
import com.worknest.features.employee.dto.UpdateStaffJobDetailsRequest;
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

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    @PostMapping("/employee")
    @Operation(summary = "Provision Employee", description = "Creates a mobile-only EMPLOYEE and attaches them to an optional supervisor.")
    public ResponseEntity<ApiResponse<ProvisioningResponse>> provisionEmployee(
            @PathVariable UUID companyId,
            @RequestBody @Valid CreateEmployeeRequest request) {

        if (!companyId.equals(request.companyId())) {
            throw new IllegalArgumentException("Company ID in path does not match request body");
        }
        ProvisioningResponse response = userProvisioningService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Employee provisioned successfully", response));
    }

    @PostMapping("/staff")
    @Operation(summary = "Provision Staff", description = "Creates a STAFF profile with configurable permissions.")
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
    @Operation(summary = "Resend Invitation", description = "Generates a new token and resends the onboarding email for a pending employee.")
    public ResponseEntity<ApiResponse<ProvisioningResponse>> resendInvitation(
            @PathVariable UUID companyId,
            @PathVariable UUID employeeId) {
        ProvisioningResponse response = userProvisioningService.resendInvitation(companyId, employeeId);
        return ResponseEntity.ok(ApiResponse.success("Invitation resent successfully", response));
    }

    // -------------------------------------------------------------------------
    // Update — main details
    // -------------------------------------------------------------------------

    @PutMapping("/employee/{employeeId}")
    @Operation(summary = "Update Employee Main Details",
            description = "Updates an EMPLOYEE's personal info (name, email, jobTitle), organisational placement, and start date.")
    public ResponseEntity<ApiResponse<UpdateEmployeeResponse>> updateEmployee(
            @PathVariable UUID companyId,
            @PathVariable UUID employeeId,
            @RequestBody @Valid UpdateEmployeeRequest request) {

        UpdateEmployeeResponse response = userProvisioningService.updateEmployee(companyId, employeeId, request);
        return ResponseEntity.ok(ApiResponse.success("Employee updated successfully", response));
    }

    @PutMapping("/staff/{employeeId}")
    @Operation(summary = "Update Staff Main Details",
            description = "Updates a STAFF member's personal info, organisational placement, permissions, and supervised employees.")
    public ResponseEntity<ApiResponse<UpdateEmployeeResponse>> updateStaff(
            @PathVariable UUID companyId,
            @PathVariable UUID employeeId,
            @RequestBody @Valid UpdateStaffRequest request) {

        UpdateEmployeeResponse response = userProvisioningService.updateStaff(companyId, employeeId, request);
        return ResponseEntity.ok(ApiResponse.success("Staff updated successfully", response));
    }

    // -------------------------------------------------------------------------
    // Update — job & contract details
    // -------------------------------------------------------------------------

    @PutMapping("/employee/{employeeId}/job-details")
    @Operation(summary = "Update Employee Job Details",
            description = "Updates an EMPLOYEE's employment type, contract document, contract expiry, leave days, and payment info.")
    public ResponseEntity<ApiResponse<UpdateEmployeeResponse>> updateEmployeeJobDetails(
            @PathVariable UUID companyId,
            @PathVariable UUID employeeId,
            @RequestBody @Valid UpdateEmployeeJobDetailsRequest request) {

        UpdateEmployeeResponse response = userProvisioningService.updateEmployeeJobDetails(companyId, employeeId, request);
        return ResponseEntity.ok(ApiResponse.success("Employee job details updated successfully", response));
    }

    @PutMapping("/staff/{employeeId}/job-details")
    @Operation(summary = "Update Staff Job Details",
            description = "Updates a STAFF member's employment type, contract document, contract expiry, leave days, and payment info.")
    public ResponseEntity<ApiResponse<UpdateEmployeeResponse>> updateStaffJobDetails(
            @PathVariable UUID companyId,
            @PathVariable UUID employeeId,
            @RequestBody @Valid UpdateStaffJobDetailsRequest request) {

        UpdateEmployeeResponse response = userProvisioningService.updateStaffJobDetails(companyId, employeeId, request);
        return ResponseEntity.ok(ApiResponse.success("Staff job details updated successfully", response));
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    @DeleteMapping("/employee/{employeeId}")
    @Operation(summary = "Delete Employee",
            description = "Permanently removes an EMPLOYEE and all their company-specific data. Deletes the user entity if they have no other company memberships.")
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(
            @PathVariable UUID companyId,
            @PathVariable UUID employeeId) {

        userProvisioningService.deleteEmployee(companyId, employeeId);
        return ResponseEntity.ok(ApiResponse.success("Employee deleted successfully", null));
    }

    @DeleteMapping("/staff/{employeeId}")
    @Operation(summary = "Delete Staff",
            description = "Permanently removes a STAFF member and all their company-specific data. Supervised employees become unassigned. Deletes the user entity if they have no other company memberships.")
    public ResponseEntity<ApiResponse<Void>> deleteStaff(
            @PathVariable UUID companyId,
            @PathVariable UUID employeeId) {

        userProvisioningService.deleteStaff(companyId, employeeId);
        return ResponseEntity.ok(ApiResponse.success("Staff deleted successfully", null));
    }

    // -------------------------------------------------------------------------
    // Lifecycle - Active/Inactive
    // -------------------------------------------------------------------------
    @PatchMapping("/employee/{employeeId}/activate")
    public ResponseEntity<ApiResponse<Void>> activateEmployee(
            @PathVariable UUID companyId,
            @PathVariable UUID employeeId
    ){
        userProvisioningService.activateEmployee(companyId, employeeId);
        return ResponseEntity.ok(ApiResponse.success("Employee activated successfully", null));
    }

    @PatchMapping("/employee/{employeeId}/terminate")
    public ResponseEntity<ApiResponse<Void>> deactivateEmployee(
            @PathVariable UUID companyId,
            @PathVariable UUID employeeId
    ){
        userProvisioningService.terminateEmployee(companyId, employeeId);
        return ResponseEntity.ok(ApiResponse.success("Employee deactivated successfully", null));
    }
}

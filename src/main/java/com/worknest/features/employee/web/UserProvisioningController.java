package com.worknest.features.employee.web;

import com.worknest.common.api.ApiResponse;
import com.worknest.features.employee.application.UserProvisioningService;
import com.worknest.features.employee.dto.CreateEmployeeRequest;
import com.worknest.features.employee.dto.CreateStaffRequest;
import com.worknest.features.employee.dto.ProvisioningResponse;
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
    @PreAuthorize("@teamSecurity.hasPermission(#companyId, 'PROVISION_USERS')")
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
    @PreAuthorize("@teamSecurity.hasPermission(#companyId, 'PROVISION_USERS')")
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
}

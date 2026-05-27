package com.worknest.features.superAdmin.web;

import com.worknest.common.api.ApiResponse;
import com.worknest.common.api.PaginatedResponse;
import com.worknest.common.api.PaginationSupport;
import com.worknest.features.superAdmin.application.SuperAdminCompaniesService;
import com.worknest.features.superAdmin.dto.CompanyRowDto;
import com.worknest.features.superAdmin.dto.ExtendTrialRequest;
import com.worknest.features.superAdmin.dto.ExtendTrialResponse;
import com.worknest.features.superAdmin.dto.PendingDeactivationDto;
import com.worknest.features.superAdmin.dto.SuspendCompanyRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/super-admin/companies")
@RequiredArgsConstructor
@Tag(name = "Super Admin", description = "Platform-level super admin API")
public class SuperAdminCompaniesController {

    private final SuperAdminCompaniesService companiesService;

    @GetMapping
    @PreAuthorize("@superAdminSecurity.isSuperAdmin()")
    @Operation(summary = "List companies", description = "Returns a paginated list of all companies with optional filters")
    public ApiResponse<PaginatedResponse<CompanyRowDto>> listCompanies(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String plan,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.success(
                "Companies retrieved successfully",
                companiesService.listCompanies(search, status, plan, PaginationSupport.pageable(page, size))
        );
    }

    @PostMapping("/{companyId}/suspend")
    @PreAuthorize("@superAdminSecurity.isSuperAdmin()")
    @Operation(summary = "Suspend or unsuspend a company", description = "Toggles company suspension status")
    public ApiResponse<CompanyRowDto> toggleSuspend(
            @PathVariable UUID companyId,
            @Valid @RequestBody SuspendCompanyRequest request
    ) {
        return ApiResponse.success("Company status updated successfully", companiesService.toggleSuspend(companyId, request));
    }

    @PostMapping("/{companyId}/extend-trial")
    @PreAuthorize("@superAdminSecurity.isSuperAdmin()")
    @Operation(summary = "Extend company trial", description = "Updates the trial end date for a company")
    public ApiResponse<ExtendTrialResponse> extendTrial(
            @PathVariable UUID companyId,
            @Valid @RequestBody ExtendTrialRequest request
    ) {
        return ApiResponse.success("Trial extended successfully", companiesService.extendTrial(companyId, request));
    }

    @GetMapping("/pending-deactivation")
    @PreAuthorize("@superAdminSecurity.isSuperAdmin()")
    @Operation(summary = "List companies pending deactivation", description = "Returns companies that have requested deactivation and are within the 30-day deletion window")
    public ApiResponse<PaginatedResponse<PendingDeactivationDto>> listPendingDeactivation(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.success(
                "Pending deactivations retrieved successfully",
                companiesService.listPendingDeactivation(PaginationSupport.pageable(page, size))
        );
    }

    @PostMapping("/{companyId}/reactivate")
    @PreAuthorize("@superAdminSecurity.isSuperAdmin()")
    @Operation(summary = "Reactivate a company", description = "Cancels a pending deactivation request, keeping the company active")
    public ApiResponse<CompanyRowDto> reactivateCompany(@PathVariable UUID companyId) {
        return ApiResponse.success("Company reactivated successfully", companiesService.reactivateCompany(companyId));
    }
}

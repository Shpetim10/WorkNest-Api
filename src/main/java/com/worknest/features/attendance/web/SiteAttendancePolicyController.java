package com.worknest.features.attendance.web;

import com.worknest.common.api.ApiResponse;
import com.worknest.features.attendance.application.SiteAttendancePolicyService;
import com.worknest.features.attendance.dto.SiteAttendancePolicyRequest;
import com.worknest.features.attendance.dto.SiteAttendancePolicyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/companies/{companyId}/sites/{siteId}/attendance-policy")
@Tag(name = "Site Attendance Policy", description = "Manage per-site attendance policy after site creation.")
public class SiteAttendancePolicyController {

    private final SiteAttendancePolicyService siteAttendancePolicyService;

    @GetMapping
    @PreAuthorize("@companySecurity.hasCompanyRole(#companyId, 'ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Get effective site attendance policy")
    public ApiResponse<SiteAttendancePolicyResponse> getPolicy(
            @PathVariable UUID companyId,
            @PathVariable UUID siteId
    ) {
        return ApiResponse.success(
                "Attendance policy retrieved successfully",
                siteAttendancePolicyService.getSitePolicy(companyId, siteId)
        );
    }

    @PutMapping
    @PreAuthorize("@companySecurity.hasCompanyRole(#companyId, 'ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Create or update site attendance policy override")
    public ApiResponse<SiteAttendancePolicyResponse> updatePolicy(
            @PathVariable UUID companyId,
            @PathVariable UUID siteId,
            @Valid @RequestBody SiteAttendancePolicyRequest request
    ) {
        return ApiResponse.success(
                "Attendance policy updated successfully",
                siteAttendancePolicyService.updateSitePolicy(companyId, siteId, request)
        );
    }
}

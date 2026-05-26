package com.worknest.features.attendance.web;

import com.worknest.common.api.ApiResponse;
import com.worknest.features.attendance.application.CompanyAttendancePolicyService;
import com.worknest.features.attendance.dto.CompanyAttendancePolicyResponse;
import com.worknest.features.attendance.dto.SiteAttendancePolicyRequest;
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
@RequestMapping("/api/v1/companies/{companyId}/attendance-policy")
@Tag(name = "Company Attendance Policy", description = "Manage company default attendance policy.")
public class CompanyAttendancePolicyController {

    private final CompanyAttendancePolicyService companyAttendancePolicyService;

    @GetMapping
    @PreAuthorize("@teamSecurity.hasPermission(#companyId, 'ATTENDANCE_POLICY_VIEW')")
    @Operation(summary = "Get company default attendance policy")
    public ApiResponse<CompanyAttendancePolicyResponse> getPolicy(@PathVariable UUID companyId) {
        return ApiResponse.success(
                "Company attendance policy retrieved successfully",
                companyAttendancePolicyService.getCompanyDefaultPolicy(companyId)
        );
    }

    @PutMapping
    @PreAuthorize("@teamSecurity.hasPermission(#companyId, 'ATTENDANCE_POLICY_UPDATE')")
    @Operation(summary = "Create or update company default attendance policy")
    public ApiResponse<CompanyAttendancePolicyResponse> updatePolicy(
            @PathVariable UUID companyId,
            @Valid @RequestBody SiteAttendancePolicyRequest request
    ) {
        return ApiResponse.success(
                "Company attendance policy updated successfully",
                companyAttendancePolicyService.upsertCompanyDefaultPolicy(companyId, request)
        );
    }
}

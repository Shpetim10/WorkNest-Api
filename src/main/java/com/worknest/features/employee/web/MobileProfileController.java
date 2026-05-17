package com.worknest.features.employee.web;

import com.worknest.common.api.ApiResponse;
import com.worknest.features.employee.application.MobileProfileService;
import com.worknest.features.employee.dto.MobileProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/mobile/profile")
@Tag(name = "Mobile Profile", description = "Self-service mobile profile APIs for employees")
public class MobileProfileController {

    private final MobileProfileService profileService;

    @GetMapping("/me")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('EMPLOYEE', 'STAFF', 'ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Get logged-in employee profile info", description = "Returns basic profile details (first name, last name, and profile image URL) of the currently authenticated employee")
    public ApiResponse<MobileProfileResponse> getMyProfile() {
        return ApiResponse.success("Profile loaded successfully", profileService.getMyProfile());
    }
}

package com.worknest.features.superAdmin.web;

import com.worknest.common.api.ApiResponse;
import com.worknest.common.exception.BusinessException;
import com.worknest.domain.entities.User;
import com.worknest.features.auth.repository.UserRepository;
import com.worknest.features.superAdmin.dto.SuperAdminProfileDto;
import com.worknest.features.superAdmin.dto.SuperAdminProfileRequest;
import com.worknest.security.AuthSessionPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/super-admin/profile")
@RequiredArgsConstructor
@Tag(name = "Super Admin", description = "Platform-level super admin API")
public class SuperAdminProfileController {

    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("@superAdminSecurity.isSuperAdmin()")
    @Operation(summary = "Get super admin profile")
    public ApiResponse<SuperAdminProfileDto> getProfile(@AuthenticationPrincipal AuthSessionPrincipal principal) {
        User user = resolveUser(principal);
        return ApiResponse.success("Profile retrieved successfully", toDto(user));
    }

    @PutMapping
    @PreAuthorize("@superAdminSecurity.isSuperAdmin()")
    @Operation(summary = "Update super admin profile")
    public ApiResponse<SuperAdminProfileDto> updateProfile(
            @AuthenticationPrincipal AuthSessionPrincipal principal,
            @Valid @RequestBody SuperAdminProfileRequest request
    ) {
        User user = resolveUser(principal);
        user.setDisplayName(request.displayName().trim());
        userRepository.save(user);
        return ApiResponse.success("Profile updated successfully", toDto(user));
    }

    private User resolveUser(AuthSessionPrincipal principal) {
        return userRepository.findById(principal.userId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
    }

    private SuperAdminProfileDto toDto(User user) {
        String displayName = user.getDisplayName() != null && !user.getDisplayName().isBlank()
                ? user.getDisplayName()
                : (user.getFirstName() + " " + user.getLastName()).trim();
        return new SuperAdminProfileDto(
                displayName,
                user.getEmail(),
                "Super Admin",
                user.getStatus().name().charAt(0) + user.getStatus().name().substring(1).toLowerCase()
        );
    }
}
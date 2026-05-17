package com.worknest.features.auth.web;

import com.worknest.common.api.ApiResponse;
import com.worknest.features.auth.application.PasswordResetRequestService;
import com.worknest.features.auth.dto.ForgotPasswordRequest;
import com.worknest.features.auth.dto.GenericMessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Super Admin Account Recovery", description = "Password recovery for platform-level super admin accounts.")
public class SuperAdminPasswordResetController {

    private final PasswordResetRequestService requestService;

    @PostMapping("/forgot-password/superadmin")
    @Operation(
            summary = "Request Super Admin Password Reset",
            description = "Initiates password recovery for platform-level accounts only. " +
                    "Regular company accounts are not accepted. Always returns 202 to prevent enumeration."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "Request accepted; check email for instructions.")
    public ResponseEntity<ApiResponse<GenericMessageResponse>> forgotPasswordSuperAdmin(
            @RequestBody @Valid ForgotPasswordRequest request,
            HttpServletRequest servletRequest
    ) {
        requestService.requestSuperAdminPasswordReset(request, servletRequest.getRemoteAddr());

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(
                        "Account recovery initiated",
                        new GenericMessageResponse("If the account exists, password reset instructions will be sent")
                ));
    }
}

package com.worknest.auth.controller;

import com.worknest.auth.dto.ForgotPasswordRequest;
import com.worknest.auth.dto.GenericMessageResponse;
import com.worknest.auth.dto.ResetPasswordRequest;
import com.worknest.auth.service.PasswordResetRequestService;
import com.worknest.auth.service.PasswordResetService;
import com.worknest.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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

/**
 * Controller for handling password reset processes.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Account Recovery", description = "Self-service endpoints for password recovery and account access restoration.")
public class PasswordResetController {

    private final PasswordResetRequestService requestService;
    private final PasswordResetService resetService;

    @PostMapping("/forgot-password")
    @Operation(
            summary = "Request Password Reset (Step 1)",
            description = "Initiates the account recovery process. If the email and company slug match an active account, " +
                    "a secure, short-lived reset token will be sent to the user's email. " +
                    "This endpoint always returns 202 Accepted to prevent user enumeration attacks."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "Request accepted; check email for instructions.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid request format (e.g., invalid email pattern)",
            content = @Content(schema = @Schema(implementation = com.worknest.common.api.ApiErrorResponse.class))
    )
    public ResponseEntity<ApiResponse<GenericMessageResponse>> forgotPassword(
            @RequestBody @Valid ForgotPasswordRequest request,
            HttpServletRequest servletRequest
    ) {
        String ipAddress = servletRequest.getRemoteAddr();
        requestService.requestPasswordReset(request, ipAddress);
        
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(
                        "Account recovery initiated",
                        new GenericMessageResponse("If the account exists, password reset instructions will be sent")
                ));
    }

    @PostMapping("/reset-password")
    @Operation(
            summary = "Execute Password Reset (Step 2)",
            description = "Completes the account recovery process. Requires a valid reset token from Step 1 and a new password. " +
                    "Once executed, the token is invalidated, and the new password is set immediately."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password reset successful")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Token is invalid, expired, or has already been used. Also returned if the new password does not meet security requirements.",
            content = @Content(schema = @Schema(implementation = com.worknest.common.api.ApiErrorResponse.class))
    )
    public ResponseEntity<ApiResponse<GenericMessageResponse>> resetPassword(
            @RequestBody @Valid ResetPasswordRequest request,
            HttpServletRequest servletRequest
    ) {
        String ipAddress = servletRequest.getRemoteAddr();
        GenericMessageResponse response = resetService.resetPassword(request, ipAddress);
        
        return ResponseEntity.ok(ApiResponse.success(response.message(), response));
    }
}

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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Password Reset", description = "Endpoints for requesting and completing password resets")
public class PasswordResetController {

    private final PasswordResetRequestService requestService;
    private final PasswordResetService resetService;

    @PostMapping("/forgot-password")
    @Operation(summary = "Request Password Reset", description = "Initiates the password reset process by sending an email with instructions to the user if the account exists.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "Request accepted")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid request",
            content = @Content(schema = @Schema(implementation = com.worknest.common.api.ApiErrorResponse.class))
    )
    public ResponseEntity<ApiResponse<GenericMessageResponse>> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        requestService.requestPasswordReset(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(
                        "Password reset request accepted",
                        new GenericMessageResponse("If the account exists, password reset instructions will be sent")
                ));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset Password", description = "Completes the password reset process using a valid token and a new password.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password reset successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid or expired token",
            content = @Content(schema = @Schema(implementation = com.worknest.common.api.ApiErrorResponse.class))
    )
    public ResponseEntity<ApiResponse<GenericMessageResponse>> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        GenericMessageResponse response = resetService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(response.message(), response));
    }
}

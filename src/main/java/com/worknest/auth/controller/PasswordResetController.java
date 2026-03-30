package com.worknest.auth.controller;

import com.worknest.auth.dto.ForgotPasswordRequest;
import com.worknest.auth.dto.GenericMessageResponse;
import com.worknest.auth.dto.ResetPasswordRequest;
import com.worknest.auth.service.PasswordResetRequestService;
import com.worknest.auth.service.PasswordResetService;
import com.worknest.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetRequestService requestService;
    private final PasswordResetService resetService;

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<GenericMessageResponse>> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        requestService.requestPasswordReset(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(
                        "Password reset request accepted",
                        new GenericMessageResponse("If the account exists, password reset instructions will be sent")
                ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<GenericMessageResponse>> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        GenericMessageResponse response = resetService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(response.message(), response));
    }
}

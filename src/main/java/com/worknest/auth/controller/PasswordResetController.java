package com.worknest.auth.controller;

import com.worknest.auth.dto.ForgotPasswordRequest;
import com.worknest.auth.dto.GenericMessageResponse;
import com.worknest.auth.dto.ResetPasswordRequest;
import com.worknest.auth.service.PasswordResetRequestService;
import com.worknest.auth.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetRequestService requestService;
    private final PasswordResetService resetService;

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        requestService.requestPasswordReset(request);
    }

    @PostMapping("/reset-password")
    public GenericMessageResponse resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        return resetService.resetPassword(request);
    }
}

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

    /**
     * Initiates a password reset request by sending a unique link to the user's email.
     *
     * @param request the email and company slug
     */
    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        requestService.requestPasswordReset(request);
    }

    /**
     * Completes a password reset operation using the token provided in the link.
     *
     * @param request the token and new password
     * @return a GenericMessageResponse confirming the new password has been set
     */
    @PostMapping("/reset-password")
    public GenericMessageResponse resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        return resetService.resetPassword(request);
    }
}

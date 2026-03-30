package com.worknest.auth.service;

import com.worknest.auth.dto.ForgotPasswordRequest;

public interface PasswordResetRequestService {

    /**
     * Initiates a password reset request by sending a reset link to the user's email.
     *
     * @param request the request containing the email and company slug
     */
    void requestPasswordReset(ForgotPasswordRequest request);
}

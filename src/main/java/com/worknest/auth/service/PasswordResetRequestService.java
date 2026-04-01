package com.worknest.auth.service;

import com.worknest.auth.dto.ForgotPasswordRequest;

/**
 * Service contract for initiating password reset requests.
 */
public interface PasswordResetRequestService {

    /**
     * Initiates a password reset process for a user.
     *
     * @param request   The forgot password request details.
     * @param ipAddress The IP address of the requester.
     */
    void requestPasswordReset(ForgotPasswordRequest request, String ipAddress);
}

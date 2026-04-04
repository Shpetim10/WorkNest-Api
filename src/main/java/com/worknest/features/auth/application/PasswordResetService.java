package com.worknest.features.auth.application;

import com.worknest.features.auth.dto.GenericMessageResponse;
import com.worknest.features.auth.dto.ResetPasswordRequest;

/**
 * Service contract for the actual password reset operation.
 */
public interface PasswordResetService {

    /**
     * Resets a user's password using a valid reset token.
     *
     * @param request   The reset password request containing token and new password.
     * @param ipAddress The IP address of the requester.
     * @return A generic success message on success.
     */
    GenericMessageResponse resetPassword(ResetPasswordRequest request, String ipAddress);
}

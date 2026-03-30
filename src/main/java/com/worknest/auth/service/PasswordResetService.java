package com.worknest.auth.service;

import com.worknest.auth.dto.GenericMessageResponse;
import com.worknest.auth.dto.ResetPasswordRequest;

public interface PasswordResetService {

    /**
     * Completes a password reset using a secure token.
     *
     * @param request the request containing the token and new password
     * @return a GenericMessageResponse confirming success
     */
    GenericMessageResponse resetPassword(ResetPasswordRequest request);
}

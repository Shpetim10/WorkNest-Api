package com.worknest.auth.service;

import com.worknest.auth.dto.ForgotPasswordRequest;

public interface PasswordResetRequestService {
    void requestPasswordReset(ForgotPasswordRequest request);
}

package com.worknest.auth.service;

import com.worknest.auth.dto.GenericMessageResponse;
import com.worknest.auth.dto.ResetPasswordRequest;

public interface PasswordResetService {
    GenericMessageResponse resetPassword(ResetPasswordRequest request);
}

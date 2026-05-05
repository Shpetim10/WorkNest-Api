package com.worknest.features.auth.application;

import com.worknest.features.auth.dto.ChangePasswordRequest;
import com.worknest.features.auth.dto.GenericMessageResponse;
import com.worknest.security.AuthSessionPrincipal;

public interface ChangePasswordService {
    GenericMessageResponse changePassword(ChangePasswordRequest request, AuthSessionPrincipal principal);
}
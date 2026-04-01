package com.worknest.auth.service;

import com.worknest.auth.dto.LoginRequest;
import com.worknest.auth.dto.LoginResponse;

public interface AuthLoginService {

    /**
     * Authenticates a user and prepares a platform-aware session.
     *
     * @param request   The login credentials and platform access type.
     * @param ipAddress The source IP address for session tracking.
     * @param userAgent The user agent string for session context.
     * @return A LoginResponse containing tokens and role context.
     */
    LoginResponse login(LoginRequest request, String ipAddress, String userAgent);
}

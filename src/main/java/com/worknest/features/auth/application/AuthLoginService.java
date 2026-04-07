package com.worknest.features.auth.application;

import com.worknest.features.auth.dto.LoginRequest;
import com.worknest.features.auth.dto.LoginResponse;

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

    /**
     * Ends a user session by revoking the provided refresh token.
     *
     * @param refreshToken The refresh token to revoke.
     */
    void logout(String refreshToken);
}

package com.worknest.auth.service;

import com.worknest.auth.dto.LoginRequest;
import com.worknest.auth.dto.LoginResponse;

public interface AuthLoginService {

    /**
     * Authenticates a user within a specific company context.
     *
     * @param request the login details (email, password, company slug)
     * @param ipAddress the IP address of the login request
     * @return the login response containing identity, tokens, and platform role
     */
    LoginResponse login(LoginRequest request, String ipAddress);

    /**
     * Terminates the current user session by revoking active tokens.
     *
     * @param userId the ID of the user logging out
     */
    void logout(String userId);
}

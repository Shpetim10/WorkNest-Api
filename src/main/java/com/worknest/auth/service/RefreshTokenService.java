package com.worknest.auth.service;

import com.worknest.auth.dto.RefreshTokenRequest;
import com.worknest.auth.dto.RefreshTokenResponse;
import java.util.UUID;

public interface RefreshTokenService {

    /**
     * Rotates a refresh token and generates a new access token.
     *
     * @param request the request containing the valid refresh token
     * @param ipAddress the IP address of the refresh request
     * @return the new token pair
     */
    RefreshTokenResponse refresh(RefreshTokenRequest request, String ipAddress);

    /**
     * Revokes a specific refresh token by its value.
     *
     * @param token the token string to revoke
     */
    void revokeToken(String token);

    /**
     * Revokes all active refresh tokens for a specific user ID.
     *
     * @param userId the ID of the user
     */
    void revokeAllUserTokens(UUID userId);
}

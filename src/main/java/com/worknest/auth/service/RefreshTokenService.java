package com.worknest.auth.service;

import com.worknest.auth.dto.RefreshTokenResponse;

/**
 * Service contract for refresh token operations.
 */
public interface RefreshTokenService {

    /**
     * Refreshes an access token using a valid, non-revoked, and non-expired refresh token.
     *
     * @param rawRefreshToken The raw refresh token string.
     * @param ipAddress       The IP address of the requester.
     * @param userAgent       The User-Agent of the requester.
     * @return Fresh access token and related session details.
     */
    RefreshTokenResponse refresh(String rawRefreshToken, String ipAddress, String userAgent);
}

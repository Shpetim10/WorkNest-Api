package com.worknest.auth.service;

import com.worknest.auth.dto.RefreshTokenRequest;
import com.worknest.auth.dto.RefreshTokenResponse;
import java.util.UUID;

public interface RefreshTokenService {

    RefreshTokenResponse refresh(RefreshTokenRequest request, String ipAddress);
    void revokeToken(String token);
    void revokeAllUserTokens(UUID userId);
}

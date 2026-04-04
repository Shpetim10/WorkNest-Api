package com.worknest.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for refreshing an access token.
 */
@Schema(description = "Request to rotate and renew access tokens")
public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token is required")
        @Schema(description = "The long-lived refresh token provided during login", example = "eyJhbG..")
        String refreshToken
) {
}

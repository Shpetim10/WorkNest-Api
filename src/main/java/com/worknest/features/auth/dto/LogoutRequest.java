package com.worknest.features.auth.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request DTO for user logout, containing the refresh token to be invalidated.
 */
public record LogoutRequest(
    @NotBlank(message = "Refresh token is required for logout")
    @Schema(description = "The persistent refresh token obtained during login.", example = "eyJhbGciOiJIUzI1NiJ9...")
    String refreshToken
) {
}

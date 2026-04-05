package com.worknest.features.auth.dto;

import com.worknest.domain.enums.PlatformAccess;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for login attempts.
 */
@Schema(description = "Credentials for primary authentication")
public record LoginRequest(
        @NotBlank
        @Email
        @Size(max = 255)
        @Schema(description = "User's primary e-mail address", example = "john.doe@example.com")
        String email,

        @NotBlank
        @Size(max = 255)
        @Schema(description = "User's account password", example = "********")
        String password,

        @NotNull
        @Schema(description = "The target platform access type", example = "WEB")
        PlatformAccess platformAccess
) {
}

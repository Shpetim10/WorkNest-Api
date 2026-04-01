package com.worknest.auth.dto;

import com.worknest.auth.domain.PlatformAccess;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for login attempts.
 */
public record LoginRequest(
        @NotBlank
        @Size(max = 100)
        String companySlug,

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(max = 255)
        String password,

        @NotNull
        PlatformAccess platformAccess
) {
}

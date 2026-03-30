package com.worknest.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequest(
        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = "^[a-z0-9-]+$", message = "companySlug must contain only lowercase letters, numbers, and hyphens")
        String companySlug,

        @NotBlank
        @Email
        @Size(max = 255)
        String email
) {
}

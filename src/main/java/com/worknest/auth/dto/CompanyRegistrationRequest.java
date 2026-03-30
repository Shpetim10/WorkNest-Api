package com.worknest.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CompanyRegistrationRequest(
        @NotBlank
        @Size(max = 255)
        String companyName,

        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = "^[a-z0-9-]+$", message = "companySlug must contain only lowercase letters, numbers, and hyphens")
        String companySlug,

        @Email
        @Size(max = 255)
        String contactEmail,

        @NotBlank
        @Email
        @Size(max = 255)
        String adminEmail,

        @NotBlank
        @Size(min = 8, max = 255)
        String adminPassword
) {
}

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
        @Pattern(regexp = "^[a-z0-9-]+$", message = "slug must contain only lowercase letters, numbers, and hyphens")
        String slug,

        @Email
        @Size(max = 255)
        String contactEmail,

        @Size(max = 100)
        String timezone,

        @Size(max = 10)
        String locale,

        @NotBlank
        @Email
        @Size(max = 255)
        String adminEmail,

        @Size(max = 10)
        String preferredLanguage
) {
}

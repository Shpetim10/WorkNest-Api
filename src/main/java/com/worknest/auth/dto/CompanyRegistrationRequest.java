package com.worknest.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CompanyRegistrationRequest(
        @NotBlank
        @Size(max = 255)
        String companyName,

        @Size(max = 255)
        String legalName,

        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = "^[a-z0-9-]+$", message = "slug must contain only lowercase letters, numbers, and hyphens")
        String slug,

        @Size(max = 30)
        String nipt,

        @Size(max = 100)
        String registrationNumber,

        @Size(max = 50)
        String vatNumber,

        @NotBlank
        @Email
        @Size(max = 255)
        String primaryEmail,

        @Size(max = 50)
        String primaryPhone,

        @Size(max = 500)
        String website,

        @Size(max = 2)
        String countryCode,

        @Size(max = 100)
        String timezone,

        @Size(max = 10)
        String locale,

        @Size(max = 10)
        String currency,

        @Size(max = 20)
        String dateFormat,

        @NotBlank
        @Email
        @Size(max = 255)
        String adminEmail,

        @NotBlank
        @Size(max = 100)
        String adminFirstName,

        @NotBlank
        @Size(max = 100)
        String adminLastName,

        @Size(max = 10)
        String preferredLanguage
) {
}

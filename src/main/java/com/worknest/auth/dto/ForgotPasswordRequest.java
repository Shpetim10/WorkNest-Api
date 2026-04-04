package com.worknest.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to initiate the forgot password flow")
public record ForgotPasswordRequest(
        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = "^[a-z0-9-]+$", message = "companySlug must contain only lowercase letters, numbers, and hyphens")
        @Schema(description = "The target company slug", example = "acme-corp")
        String companySlug,

        @NotBlank
        @Email
        @Size(max = 255)
        @Schema(description = "The user's registered e-mail address", example = "john.doe@example.com")
        String email
) {
}

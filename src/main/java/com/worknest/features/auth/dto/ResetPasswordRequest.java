package com.worknest.features.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to complete the password reset flow")
public record ResetPasswordRequest(
        @NotBlank
        @Schema(description = "The secret reset token received via e-mail", example = "rst789uvw012")
        String token,

        @NotBlank
        @Size(min = 8, max = 255)
        @Schema(description = "The new password to set for the account", example = "NewP@ssword123")
        String newPassword
) {
}

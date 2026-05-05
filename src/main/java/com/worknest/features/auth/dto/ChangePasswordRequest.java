package com.worknest.features.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to change the password of the currently authenticated user")
public record ChangePasswordRequest(

        @NotBlank
        @Schema(description = "The user's current password", example = "CurrentP@ss1")
        String currentPassword,

        @NotBlank
        @Size(min = 8, max = 255)
        @Schema(description = "The new password to set", example = "NewP@ssword1")
        String newPassword
) {}
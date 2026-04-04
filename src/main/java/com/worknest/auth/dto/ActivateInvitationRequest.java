package com.worknest.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for activating a pending invitation.
 */
@Schema(description = "Request object for activating an invitation and setting up the account (Step 2)")
public record ActivateInvitationRequest(

        /** Raw invitation token received via e-mail. */
        @NotBlank
        @Schema(description = "The secret activation token received via e-mail", example = "abc123xyz789")
        String token,

        /** Chosen password. Must be ≥ 8 characters, contain an uppercase letter and a digit. */
        @NotBlank
        @Size(min = 8, max = 255)
        @Schema(description = "The user's new account password", example = "P@ssword123")
        String password,

        /**
         * GDPR / Terms of Service consent flag.
         */
        @NotNull
        @Schema(description = "Explicit consent to Terms of Service and GDPR policies", example = "true")
        Boolean gdprConsent,

        @Size(max = 10)
        @Schema(description = "Preferred UI language code (BCP-47)", example = "sq", defaultValue = "sq")
        String preferredLanguage,

        @Size(max = 50)
        @Schema(description = "Contact phone number", example = "+355670000000")
        String phoneNumber,

        @Size(max = 500)
        @Schema(description = "Storage key for the uploaded profile image")
        String profileImageStorageKey,

        @Size(max = 1000)
        @Schema(description = "Full storage path/URL for the profile image")
        String profileImageStoragePath,

        @Size(max = 150)
        @Schema(description = "Name of an emergency contact person", example = "Jane Doe")
        String emergencyContactName,

        @Size(max = 50)
        @Schema(description = "Phone number of the emergency contact", example = "+355690000000")
        String emergencyContactPhone
) {
}

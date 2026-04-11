package com.worknest.features.invitation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request payload for validating an invitation token without activating it.")
public record ValidateInvitationRequest(
        @NotBlank
        @Schema(description = "The secret activation token received via e-mail", example = "abc123xyz789")
        String token
) {}

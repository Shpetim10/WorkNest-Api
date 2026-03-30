package com.worknest.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActivateInvitationRequest(
        @NotBlank
        String token,

        @NotBlank
        @Size(min = 8, max = 255)
        String password,

        @Size(max = 255)
        String jobTitle
) {
}

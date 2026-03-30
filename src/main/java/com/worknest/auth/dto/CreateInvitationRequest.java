package com.worknest.auth.dto;

import com.worknest.auth.domain.PlatformRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateInvitationRequest(
        @NotNull
        UUID companyId,

        @NotBlank
        @Email
        String email,

        @NotNull
        PlatformRole platformRole
) {
}

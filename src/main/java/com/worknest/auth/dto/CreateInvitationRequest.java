package com.worknest.auth.dto;

import com.worknest.auth.domain.PlatformAccess;
import com.worknest.auth.domain.PlatformRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateInvitationRequest(
        @NotNull
        UUID companyId,

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotNull
        PlatformRole platformRole,

        @Size(max = 255)
        String invitedJobTitle,

        @NotNull
        PlatformAccess platformAccess
) {
}

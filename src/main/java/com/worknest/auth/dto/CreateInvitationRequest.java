package com.worknest.auth.dto;

import com.worknest.auth.domain.PlatformRole;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.util.StringUtils;

public record CreateInvitationRequest(
        @NotNull
        UUID companyId,

        @Email
        @Size(max = 255)
        String email,

        @NotNull
        PlatformRole platformRole,

        @Size(max = 255)
        String jobTitle
) {

    @AssertTrue(message = "jobTitle is required when platformRole is STAFF")
    public boolean isJobTitleValid() {
        return platformRole != PlatformRole.STAFF || StringUtils.hasText(jobTitle);
    }
}

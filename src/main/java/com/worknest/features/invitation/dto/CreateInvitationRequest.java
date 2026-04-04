package com.worknest.features.invitation.dto;

import com.worknest.domain.enums.PlatformAccess;
import com.worknest.domain.enums.PlatformRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

@Schema(description = "Request to create a user invitation (Staff or Admin)")
public record CreateInvitationRequest(

        @NotNull
        @Schema(description = "The target company ID for the invitation")
        UUID companyId,

        @NotBlank
        @Size(max = 100)
        @Schema(description = "First name of the invitee", example = "Jane")
        String firstName,

        @NotBlank
        @Size(max = 100)
        @Schema(description = "Last name of the invitee", example = "Smith")
        String lastName,

        @NotBlank
        @Email
        @Size(max = 255)
        @Schema(description = "Primary e-mail to send the invitation to", example = "jane.smith@example.com")
        String email,

        @Size(max = 50)
        @Schema(description = "Contact phone number for the invitee", example = "+355670000000")
        String phoneNumber,

        @NotNull
        @Schema(description = "The platform-level role to assign", example = "STAFF")
        PlatformRole platformRole,

        /** Required for STAFF role. */
        @Size(max = 255)
        @Schema(description = "The job title to assign upon activation", example = "Marketing Manager")
        String invitedJobTitle,

        /** Permission codes granted to STAFF role assignments. */
        @Schema(description = "Specific permission strings (e.g. INVENTORY_READ, PAYROLL_WRITE) granted to the user")
        List<String> permissionCodes,

        @NotNull
        @Schema(description = "The primary platform this user is being invited to", example = "WEB")
        PlatformAccess platformAccess,

        @Size(max = 10)
        @Schema(description = "The preferred language for the invitation email", example = "sq", defaultValue = "sq")
        String preferredLanguage
) {
}

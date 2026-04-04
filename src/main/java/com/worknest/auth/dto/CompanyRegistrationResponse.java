package com.worknest.auth.dto;

import com.worknest.auth.domain.CompanyStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * Response returned after the company registration request is accepted.
 */
@Schema(description = "Response object for a successful company registration initiation")
public record CompanyRegistrationResponse(
        @Schema(description = "Unique identifier assigned to the new company")
        UUID companyId,

        @Schema(description = "Unique identifier assigned to the initial administrator")
        UUID adminUserId,

        @Schema(description = "The role assignment ID for the administrator within this company")
        UUID adminRoleAssignmentId,

        @Schema(description = "Unique identifier for the pending invitation")
        UUID adminInvitationId,

        /** Current lifecycle status of the company — {@code PENDING} until admin activates. */
        @Schema(description = "Current lifecycle status of the company")
        CompanyStatus companyStatus,

        /** {@code true} once the admin has completed the onboarding flow. */
        @Schema(description = "Flag indicating if the onboarding process is fully completed")
        Boolean onboardingCompleted,

        /** Always {@code true} at this stage — the invitation e-mail has been dispatched. */
        @Schema(description = "Flag confirming the activation e-mail was successfully queued/sent")
        Boolean activationEmailSent,

        @Schema(description = "Inforamative success message", example = "Company registration successful. Activation e-mail sent to admin@acme.com")
        String message
) {
}

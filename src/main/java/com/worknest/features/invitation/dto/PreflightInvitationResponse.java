package com.worknest.features.invitation.dto;

import com.worknest.domain.enums.PlatformRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response object containing masked invitation metadata if the token is valid.")
public record PreflightInvitationResponse(
        @Schema(description = "The masked email address associated with the invitation")
        String maskedEmail,
        
        @Schema(description = "The company name the invitation belongs to")
        String companyName,
        
        @Schema(description = "The assigned platform role")
        PlatformRole platformRole,
        
        @Schema(description = "The invited job title, if applicable")
        String invitedJobTitle
) {}

package com.worknest.features.auth.dto;

import com.worknest.domain.enums.PlatformAccess;
import com.worknest.domain.enums.PlatformRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * Data Transfer Object representing a selectable role context during login.
 */
@Schema(description = "A selectable organization/role context for a user")
public record AvailableLoginContextDto(
        @Schema(description = "Unique ID of the company/organization")
        UUID companyId,

        @Schema(description = "Display name of the company")
        String companyName,

        @Schema(description = "Tenant slug of the company")
        String companySlug,

        @Schema(description = "The specific role assignment ID to initialize the session with")
        UUID roleAssignmentId,

        @Schema(description = "The platform role associated with this context")
        PlatformRole role,

        @Schema(description = "The user's job title within this organization", example = "Senior Software Engineer")
        String jobTitle,

        @Schema(description = "The authorized platform access level for this context")
        PlatformAccess platformAccess
) {
}

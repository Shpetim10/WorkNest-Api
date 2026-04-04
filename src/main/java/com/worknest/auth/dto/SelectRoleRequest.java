package com.worknest.auth.dto;

import com.worknest.auth.domain.PlatformAccess;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "Request to select a specific organization/role context")
public record SelectRoleRequest(
        @NotNull
        @Schema(description = "The specific role assignment ID to activate", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID roleAssignmentId,

        @NotNull
        @Schema(description = "The platform access level requested for this session", example = "WEB_APP")
        PlatformAccess platformAccess
) {
}

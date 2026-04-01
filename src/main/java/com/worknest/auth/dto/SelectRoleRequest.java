package com.worknest.auth.dto;

import com.worknest.auth.domain.PlatformAccess;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SelectRoleRequest(
        @NotNull
        UUID roleAssignmentId,

        @NotNull
        PlatformAccess platformAccess
) {
}

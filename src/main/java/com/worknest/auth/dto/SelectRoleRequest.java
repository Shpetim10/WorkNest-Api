package com.worknest.auth.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SelectRoleRequest(
        @NotNull
        UUID roleAssignmentId
) {
}

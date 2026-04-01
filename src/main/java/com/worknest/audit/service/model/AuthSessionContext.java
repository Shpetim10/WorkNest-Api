package com.worknest.audit.service.model;

import com.worknest.auth.domain.PlatformAccess;
import com.worknest.auth.domain.PlatformRole;
import java.util.UUID;

public record AuthSessionContext(
        UUID userId,
        UUID roleAssignmentId,
        PlatformRole role,
        PlatformAccess platformAccess
) {
}

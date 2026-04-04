package com.worknest.audit.service.model;

import com.worknest.domain.enums.PlatformAccess;
import com.worknest.domain.enums.PlatformRole;
import java.util.UUID;

public record AuthSessionContext(
        UUID userId,
        UUID roleAssignmentId,
        PlatformRole role,
        PlatformAccess platformAccess
) {
}

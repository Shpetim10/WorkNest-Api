package com.worknest.audit.service.model;

import com.worknest.auth.domain.PlatformRole;
import java.util.UUID;

public record AuthAuditActorContext(
        UUID companyId,
        String companyName,
        UUID actorUserId,
        UUID actorRoleAssignmentId,
        PlatformRole actorRole,
        String actorJobTitle,
        String ipAddress
) {
}

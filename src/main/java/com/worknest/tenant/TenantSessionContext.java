package com.worknest.tenant;

import com.worknest.auth.domain.PlatformAccess;
import com.worknest.auth.domain.PlatformRole;
import java.util.UUID;

public record TenantSessionContext(
        UUID companyId,
        String companySlug,
        UUID roleAssignmentId,
        PlatformRole role,
        PlatformAccess platformAccess
) {
}

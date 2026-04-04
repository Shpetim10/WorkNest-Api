package com.worknest.tenant;

import com.worknest.domain.enums.PlatformAccess;
import com.worknest.domain.enums.PlatformRole;
import java.util.UUID;

public record TenantSessionContext(
        UUID companyId,
        String companySlug,
        UUID roleAssignmentId,
        PlatformRole role,
        PlatformAccess platformAccess
) {
}

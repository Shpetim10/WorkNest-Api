package com.worknest.security;

import com.worknest.auth.domain.PlatformAccess;
import com.worknest.auth.domain.PlatformRole;
import com.worknest.tenant.TenantSessionContext;
import java.security.Principal;
import java.util.UUID;

public record AuthSessionPrincipal(
        UUID userId,
        String username,
        UUID companyId,
        String companySlug,
        UUID roleAssignmentId,
        PlatformRole role,
        PlatformAccess platformAccess
) implements Principal {

    @Override
    public String getName() {
        return username;
    }

    public TenantSessionContext toTenantSessionContext() {
        return new TenantSessionContext(companyId, companySlug, roleAssignmentId, role, platformAccess);
    }
}

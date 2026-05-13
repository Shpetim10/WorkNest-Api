package com.worknest.security;

import com.worknest.domain.enums.PlatformRole;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("superAdminSecurity")
public class SuperAdminSecurity {

    public boolean isSuperAdmin() {
        return currentPrincipal()
                .map(p -> p.role() == PlatformRole.SUPERADMIN)
                .orElse(false);
    }

    public Optional<AuthSessionPrincipal> currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        if (!(auth.getPrincipal() instanceof AuthSessionPrincipal p)) {
            return Optional.empty();
        }
        return Optional.of(p);
    }
}

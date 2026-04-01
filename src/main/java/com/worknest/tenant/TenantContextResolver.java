package com.worknest.tenant;

import com.worknest.security.AuthSessionPrincipal;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class TenantContextResolver {

    public Optional<TenantSessionContext> resolveCurrentContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthSessionPrincipal authSessionPrincipal) {
            return Optional.of(authSessionPrincipal.toTenantSessionContext());
        }

        return Optional.empty();
    }
}

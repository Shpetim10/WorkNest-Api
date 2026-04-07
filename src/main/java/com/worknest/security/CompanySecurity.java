package com.worknest.security;

import com.worknest.domain.enums.PlatformRole;
import com.worknest.features.auth.repository.RoleAssignmentRepository;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("companySecurity")
@RequiredArgsConstructor
public class CompanySecurity {

    private final RoleAssignmentRepository roleAssignmentRepository;

    public boolean hasCurrentCompanyRole(String... roles) {
        return currentPrincipal()
                .map(principal -> hasCompanyRole(principal.companyId(), roles))
                .orElse(false);
    }

    public boolean hasCompanyRole(UUID companyId, String... roles) {
        if (companyId == null || roles == null || roles.length == 0) {
            return false;
        }

        boolean hasValidRoleParam = Arrays.stream(roles)
                .filter(Objects::nonNull)
                .anyMatch(r -> !r.trim().isEmpty());
                
        if (!hasValidRoleParam) {
            return false;
        }

        Optional<AuthSessionPrincipal> principalOptional = currentPrincipal();
        if (principalOptional.isEmpty()) {
            return false;
        }

        AuthSessionPrincipal principal = principalOptional.get();
        if (!companyId.equals(principal.companyId())) {
            return false;
        }

        return roleAssignmentRepository.findFirstByUserIdAndCompanyIdAndIsActiveTrue(principal.userId(), companyId)
                .map(assignment -> Arrays.stream(roles)
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(role -> !role.isEmpty())
                        .map(String::toUpperCase)
                        .anyMatch(role -> role.equals(assignment.getRole().name())))
                .orElse(false);
    }

    private Optional<AuthSessionPrincipal> currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        if (!(authentication.getPrincipal() instanceof AuthSessionPrincipal principal)) {
            return Optional.empty();
        }

        return Optional.of(principal);
    }
}

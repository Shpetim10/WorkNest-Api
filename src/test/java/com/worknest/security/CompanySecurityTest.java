package com.worknest.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.worknest.domain.entities.RoleAssignment;
import com.worknest.domain.enums.PlatformAccess;
import com.worknest.domain.enums.PlatformRole;
import com.worknest.features.auth.repository.RoleAssignmentRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class CompanySecurityTest {

    @Mock
    private RoleAssignmentRepository roleAssignmentRepository;

    @InjectMocks
    private CompanySecurity companySecurity;

    private final UUID companyId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setAuthentication(PlatformRole role) {
        AuthSessionPrincipal principal = new AuthSessionPrincipal(
                userId, "testuser", companyId, "test", UUID.randomUUID(), role, PlatformAccess.WEB
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void shouldReturnFalseWhenNoAuthentication() {
        assertFalse(companySecurity.hasCompanyRole(companyId, "ADMIN"));
        assertFalse(companySecurity.hasCurrentCompanyRole("ADMIN"));
    }

    @Test
    void shouldReturnFalseWhenPrincipalNotAuthSessionPrincipal() {
        Authentication auth = new UsernamePasswordAuthenticationToken("user", "pass", null);
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertFalse(companySecurity.hasCompanyRole(companyId, "ADMIN"));
        assertFalse(companySecurity.hasCurrentCompanyRole("ADMIN"));
    }

    @Test
    void shouldReturnFalseWhenRolesNullOrEmpty() {
        setAuthentication(PlatformRole.ADMIN);
        
        assertFalse(companySecurity.hasCompanyRole(companyId, (String[]) null));
        assertFalse(companySecurity.hasCompanyRole(companyId, new String[]{}));
        assertFalse(companySecurity.hasCompanyRole(companyId, "   ", ""));
    }

    @Test
    void shouldReturnFalseWhenUserNotPartOfTargetCompany() {
        setAuthentication(PlatformRole.ADMIN);
        UUID otherCompanyId = UUID.randomUUID();

        when(roleAssignmentRepository.findFirstByUserIdAndCompanyIdAndIsActiveTrue(userId, otherCompanyId))
                .thenReturn(Optional.empty());

        assertFalse(companySecurity.hasCompanyRole(otherCompanyId, "ADMIN"));
    }

    @Test
    void shouldReturnFalseWhenUserHasRoleButNotInRequestedRoles() {
        setAuthentication(PlatformRole.EMPLOYEE);

        RoleAssignment roleAssignment = new RoleAssignment();
        roleAssignment.setRole(PlatformRole.EMPLOYEE);
        
        when(roleAssignmentRepository.findFirstByUserIdAndCompanyIdAndIsActiveTrue(userId, companyId))
                .thenReturn(Optional.of(roleAssignment));

        assertFalse(companySecurity.hasCompanyRole(companyId, "ADMIN", "SUPERADMIN"));
    }

    @Test
    void shouldReturnTrueWhenUserBelongsToTargetCompanyAndHasRequestedRole() {
        setAuthentication(PlatformRole.ADMIN);

        RoleAssignment roleAssignment = new RoleAssignment();
        roleAssignment.setRole(PlatformRole.ADMIN);
        
        when(roleAssignmentRepository.findFirstByUserIdAndCompanyIdAndIsActiveTrue(userId, companyId))
                .thenReturn(Optional.of(roleAssignment));

        assertTrue(companySecurity.hasCompanyRole(companyId, "ADMIN", "SUPERADMIN"));
    }

    @Test
    void shouldHandleCaseInsensitiveRoles() {
        setAuthentication(PlatformRole.SUPERADMIN);

        RoleAssignment roleAssignment = new RoleAssignment();
        roleAssignment.setRole(PlatformRole.SUPERADMIN);
        
        when(roleAssignmentRepository.findFirstByUserIdAndCompanyIdAndIsActiveTrue(userId, companyId))
                .thenReturn(Optional.of(roleAssignment));

        assertTrue(companySecurity.hasCompanyRole(companyId, "  superadmin "));
    }

    @Test
    void shouldReturnFalseWhenRequestedCompanyDoesNotMatchAuthenticatedCompany() {
        setAuthentication(PlatformRole.ADMIN);
        UUID otherCompanyId = UUID.randomUUID();

        assertFalse(companySecurity.hasCompanyRole(otherCompanyId, "ADMIN"));
    }

    @Test
    void shouldReturnTrueForCurrentCompanyRoleCheck() {
        setAuthentication(PlatformRole.ADMIN);

        RoleAssignment roleAssignment = new RoleAssignment();
        roleAssignment.setRole(PlatformRole.ADMIN);

        when(roleAssignmentRepository.findFirstByUserIdAndCompanyIdAndIsActiveTrue(userId, companyId))
                .thenReturn(Optional.of(roleAssignment));

        assertTrue(companySecurity.hasCurrentCompanyRole("ADMIN", "SUPERADMIN"));
    }
}

package com.worknest.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.Employee;
import com.worknest.domain.entities.Permission;
import com.worknest.domain.entities.RoleAssignment;
import com.worknest.domain.entities.RoleAssignmentPermission;
import com.worknest.domain.entities.User;
import com.worknest.domain.enums.PlatformAccess;
import com.worknest.domain.enums.PlatformRole;
import com.worknest.features.auth.repository.RoleAssignmentPermissionRepository;
import com.worknest.features.auth.repository.RoleAssignmentRepository;
import com.worknest.features.employee.repository.EmployeeRepository;
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
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class TeamSecurityGuardTest {

    @Mock
    private RoleAssignmentRepository roleAssignmentRepository;

    @Mock
    private RoleAssignmentPermissionRepository roleAssignmentPermissionRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private TeamSecurityGuard teamSecurityGuard;

    private final UUID companyId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID roleAssignmentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void staffWithoutPermissionCannotAccessFeature() {
        authenticate(PlatformRole.STAFF);
        RoleAssignment assignment = assignment(PlatformRole.STAFF);
        when(roleAssignmentRepository.findFirstByUserIdAndCompanyIdAndIsActiveTrue(userId, companyId))
                .thenReturn(Optional.of(assignment));
        when(roleAssignmentPermissionRepository.findByRoleAssignmentIdAndPermissionCode(roleAssignmentId, "EMPLOYEE_VIEW"))
                .thenReturn(Optional.empty());

        assertFalse(teamSecurityGuard.hasPermission(companyId, "EMPLOYEE_VIEW"));
    }

    @Test
    void staffWithPermissionCanAccessFeature() {
        authenticate(PlatformRole.STAFF);
        RoleAssignment assignment = assignment(PlatformRole.STAFF);
        RoleAssignmentPermission grant = new RoleAssignmentPermission();
        grant.setPermission(new Permission("EMPLOYEE", "VIEW", "View employee records"));
        grant.setIsGranted(true);

        when(roleAssignmentRepository.findFirstByUserIdAndCompanyIdAndIsActiveTrue(userId, companyId))
                .thenReturn(Optional.of(assignment));
        when(roleAssignmentPermissionRepository.findByRoleAssignmentIdAndPermissionCode(roleAssignmentId, "EMPLOYEE_VIEW"))
                .thenReturn(Optional.of(grant));

        assertTrue(teamSecurityGuard.hasPermission(companyId, "EMPLOYEE_VIEW"));
    }

    @Test
    void employeeCanOnlyViewOwnEmployeeDetails() {
        authenticate(PlatformRole.EMPLOYEE);
        UUID employeeId = UUID.randomUUID();
        Company company = new Company();
        company.setId(companyId);
        User user = new User();
        user.setId(userId);
        Employee employee = new Employee();
        employee.setCompany(company);
        employee.setUser(user);

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        assertTrue(teamSecurityGuard.canViewEmployee(companyId, employeeId, "EMPLOYEE_VIEW"));
    }

    @Test
    void employeeCannotReceiveGenericFeaturePermission() {
        authenticate(PlatformRole.EMPLOYEE);
        RoleAssignment assignment = assignment(PlatformRole.EMPLOYEE);
        when(roleAssignmentRepository.findFirstByUserIdAndCompanyIdAndIsActiveTrue(userId, companyId))
                .thenReturn(Optional.of(assignment));

        assertFalse(teamSecurityGuard.hasPermission(companyId, "STAFF_VIEW"));
    }

    private void authenticate(PlatformRole role) {
        AuthSessionPrincipal principal = new AuthSessionPrincipal(
                userId,
                "user@example.com",
                companyId,
                "company",
                roleAssignmentId,
                role,
                PlatformAccess.WEB
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, null)
        );
    }

    private RoleAssignment assignment(PlatformRole role) {
        RoleAssignment assignment = new RoleAssignment();
        assignment.setId(roleAssignmentId);
        assignment.setRole(role);
        return assignment;
    }
}

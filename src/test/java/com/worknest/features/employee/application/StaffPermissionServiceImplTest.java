package com.worknest.features.employee.application;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.worknest.common.exception.BusinessException;
import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.Employee;
import com.worknest.domain.entities.User;
import com.worknest.domain.enums.PlatformAccess;
import com.worknest.domain.enums.PlatformRole;
import com.worknest.features.auth.repository.PermissionRepository;
import com.worknest.features.auth.repository.RoleAssignmentPermissionRepository;
import com.worknest.features.auth.repository.RoleAssignmentRepository;
import com.worknest.features.auth.repository.UserRepository;
import com.worknest.features.employee.repository.EmployeeRepository;
import com.worknest.security.AuthSessionPrincipal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class StaffPermissionServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private RoleAssignmentRepository roleAssignmentRepository;

    @Mock
    private RoleAssignmentPermissionRepository roleAssignmentPermissionRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private UserRepository userRepository;

    private StaffPermissionServiceImpl service;
    private UUID companyId;
    private UUID employeeId;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        service = new StaffPermissionServiceImpl(
                employeeRepository,
                roleAssignmentRepository,
                roleAssignmentPermissionRepository,
                permissionRepository,
                userRepository
        );
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthSessionPrincipal(actorId, "admin@example.com", companyId, "acme", UUID.randomUUID(),
                        PlatformRole.ADMIN, PlatformAccess.WEB),
                null,
                null
        ));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsAssigningPermissionsToEmployeeUsers() {
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee(PlatformRole.EMPLOYEE)));

        assertThrows(BusinessException.class,
                () -> service.replacePermissions(companyId, employeeId, List.of("EMPLOYEE_VIEW")));
    }

    @Test
    void rejectsUnknownPermissionCodes() {
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee(PlatformRole.STAFF)));

        assertThrows(BusinessException.class,
                () -> service.replacePermissions(companyId, employeeId, List.of("NOPE_VIEW")));
    }

    private Employee employee(PlatformRole role) {
        Company company = new Company();
        company.setId(companyId);
        User user = new User();
        user.setId(UUID.randomUUID());
        Employee employee = new Employee();
        employee.setId(employeeId);
        employee.setCompany(company);
        employee.setUser(user);
        employee.setEmploymentTypeRole(role);
        return employee;
    }
}

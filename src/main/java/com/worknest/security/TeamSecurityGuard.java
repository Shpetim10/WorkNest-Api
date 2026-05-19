package com.worknest.security;

import com.worknest.domain.entities.Employee;
import com.worknest.domain.enums.PlatformRole;
import com.worknest.features.auth.repository.RoleAssignmentPermissionRepository;
import com.worknest.features.auth.repository.RoleAssignmentRepository;
import com.worknest.features.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component("teamSecurity")
@RequiredArgsConstructor
public class TeamSecurityGuard {

    private final RoleAssignmentRepository roleAssignmentRepository;
    private final RoleAssignmentPermissionRepository roleAssignmentPermissionRepository;
    private final EmployeeRepository employeeRepository;

    private Optional<AuthSessionPrincipal> currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        if (!(auth.getPrincipal() instanceof AuthSessionPrincipal principal)) {
            return Optional.empty();
        }
        return Optional.of(principal);
    }

    /**
     * Verifies if the active user possesses a generic operational permission in their company.
     * Admins/SuperAdmins inherently bypass this check successfully. 
     * Employees inherently fail this.
     * Staff must be explicitly granted the matching permissionCode in `role_assignment_permissions`.
     */
    public boolean hasPermission(UUID companyId, String permissionCode) {
        if (companyId == null || permissionCode == null) return false;

        Optional<AuthSessionPrincipal> principalOpt = currentPrincipal();
        if (principalOpt.isEmpty() || !companyId.equals(principalOpt.get().companyId())) return false;

        AuthSessionPrincipal principal = principalOpt.get();

        return roleAssignmentRepository.findFirstByUserIdAndCompanyIdAndIsActiveTrue(principal.userId(), companyId)
                .map(ra -> {
                    if (ra.getRole() == PlatformRole.SUPERADMIN) return true;
                    if (ra.getRole() == PlatformRole.ADMIN) return true;
                    if (ra.getRole() == PlatformRole.EMPLOYEE) return false;

                    if (ra.getRole() == PlatformRole.STAFF) {
                        return roleAssignmentPermissionRepository.findByRoleAssignmentIdAndPermissionCode(ra.getId(), permissionCode)
                                .map(p -> Boolean.TRUE.equals(p.getIsGranted()))
                                .orElse(false);
                    }
                    return false;
                }).orElse(false);
    }

    public boolean hasCurrentCompanyPermission(String permissionCode) {
        return currentPrincipal()
                .map(principal -> hasPermission(principal.companyId(), permissionCode))
                .orElse(false);
    }

    public boolean hasCurrentCompanyRoleOrPermission(String permissionCode, String... roleNames) {
        Optional<AuthSessionPrincipal> principalOpt = currentPrincipal();
        if (principalOpt.isEmpty()) {
            return false;
        }
        AuthSessionPrincipal principal = principalOpt.get();
        if (matchesAnyRole(principal.role(), roleNames)) {
            return true;
        }
        return hasPermission(principal.companyId(), permissionCode);
    }

    public boolean hasCompanyRoleOrPermission(UUID companyId, String permissionCode, String... roleNames) {
        Optional<AuthSessionPrincipal> principalOpt = currentPrincipal();
        if (principalOpt.isEmpty() || companyId == null || !companyId.equals(principalOpt.get().companyId())) {
            return false;
        }
        AuthSessionPrincipal principal = principalOpt.get();
        if (matchesAnyRole(principal.role(), roleNames)) {
            return true;
        }
        return hasPermission(companyId, permissionCode);
    }

    public boolean canViewEmployee(UUID companyId, UUID employeeId, String permissionCode) {
        Optional<AuthSessionPrincipal> principalOpt = currentPrincipal();
        if (principalOpt.isEmpty() || companyId == null || employeeId == null || !companyId.equals(principalOpt.get().companyId())) {
            return false;
        }

        AuthSessionPrincipal principal = principalOpt.get();
        if (principal.role() == PlatformRole.SUPERADMIN || principal.role() == PlatformRole.ADMIN) {
            return true;
        }

        Optional<Employee> target = employeeRepository.findById(employeeId);
        if (target.isEmpty() || !target.get().getCompany().getId().equals(companyId)) {
            return false;
        }

        if (principal.role() == PlatformRole.EMPLOYEE) {
            return target.get().getUser().getId().equals(principal.userId());
        }

        return hasPermission(companyId, permissionCode);
    }

    /**
     * Targeted guard: Verifies if the active user both has the permission to perform an action AND 
     * mathematically scoping ownership over the target employee's team context.
     * Resolves false quickly if crossed tenant boundaries. 
     */
    public boolean canManageEmployee(UUID companyId, UUID employeeId, String permissionCode) {
        if (companyId == null || employeeId == null) return false;

        Optional<AuthSessionPrincipal> principalOpt = currentPrincipal();
        if (principalOpt.isEmpty() || !companyId.equals(principalOpt.get().companyId())) return false;

        AuthSessionPrincipal principal = principalOpt.get();
        return roleAssignmentRepository.findFirstByUserIdAndCompanyIdAndIsActiveTrue(principal.userId(), companyId)
                .map(ra -> {
                    if (ra.getRole() == PlatformRole.SUPERADMIN || ra.getRole() == PlatformRole.ADMIN) {
                        return true; // Admins bypass team-scope and permission scope explicitly
                    }
                    if (ra.getRole() == PlatformRole.EMPLOYEE) return false;

                    // Evaluate explicit permission check
                    boolean hasPerm = roleAssignmentPermissionRepository.findByRoleAssignmentIdAndPermissionCode(ra.getId(), permissionCode)
                            .map(p -> Boolean.TRUE.equals(p.getIsGranted()))
                            .orElse(false);

                    if (!hasPerm) return false;

                    // Guard team boundaries (Ensure targeted employee is natively inside this supervisor's subgraph)
                    Optional<Employee> targetEmployee = employeeRepository.findById(employeeId);
                    return targetEmployee.map(emp -> {
                        if (!emp.getCompany().getId().equals(companyId)) return false;
                        if (emp.getSupervisorRoleAssignment() == null) return false;
                        return emp.getSupervisorRoleAssignment().getId().equals(ra.getId());
                    }).orElse(false);

                }).orElse(false);
    }

    private boolean matchesAnyRole(PlatformRole role, String... roleNames) {
        if (role == null || roleNames == null) {
            return false;
        }
        for (String roleName : roleNames) {
            if (roleName != null && role.name().equalsIgnoreCase(roleName.trim())) {
                return true;
            }
        }
        return false;
    }
}

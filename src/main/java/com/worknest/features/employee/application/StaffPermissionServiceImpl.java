package com.worknest.features.employee.application;

import com.worknest.common.exception.BusinessException;
import com.worknest.common.exception.ResourceNotFoundException;
import com.worknest.domain.entities.Employee;
import com.worknest.domain.entities.Permission;
import com.worknest.domain.entities.RoleAssignment;
import com.worknest.domain.entities.RoleAssignmentPermission;
import com.worknest.domain.entities.User;
import com.worknest.domain.enums.PermissionCode;
import com.worknest.domain.enums.PlatformRole;
import com.worknest.features.auth.repository.PermissionRepository;
import com.worknest.features.auth.repository.RoleAssignmentPermissionRepository;
import com.worknest.features.auth.repository.RoleAssignmentRepository;
import com.worknest.features.auth.repository.UserRepository;
import com.worknest.features.employee.dto.StaffPermissionsResponse;
import com.worknest.features.employee.repository.EmployeeRepository;
import com.worknest.security.AuthSessionPrincipal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class StaffPermissionServiceImpl implements StaffPermissionService {

    private final EmployeeRepository employeeRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final RoleAssignmentPermissionRepository roleAssignmentPermissionRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public StaffPermissionsResponse getPermissions(UUID companyId, UUID staffId) {
        Employee staff = resolveStaff(companyId, staffId);
        RoleAssignment assignment = resolveRoleAssignment(companyId, staff);
        return response(staff, assignment);
    }

    @Override
    @Transactional
    public StaffPermissionsResponse replacePermissions(UUID companyId, UUID staffId, List<String> permissionCodes) {
        Employee staff = resolveStaff(companyId, staffId);
        RoleAssignment assignment = resolveRoleAssignment(companyId, staff);
        User actor = resolveActor();

        List<String> codes = normalizeCodes(permissionCodes);
        roleAssignmentPermissionRepository.deleteAllByRoleAssignmentId(assignment.getId());

        if (!codes.isEmpty()) {
            List<Permission> permissions = permissionRepository.findAllByCodeIn(codes);
            if (permissions.size() != codes.size()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_PERMISSION",
                        "One or more permission codes are invalid.");
            }

            for (Permission permission : permissions) {
                RoleAssignmentPermission grant = new RoleAssignmentPermission();
                grant.setRoleAssignment(assignment);
                grant.setPermission(permission);
                grant.setIsGranted(true);
                grant.setGrantedBy(actor);
                roleAssignmentPermissionRepository.save(grant);
            }
        }

        return response(staff, assignment);
    }

    private Employee resolveStaff(UUID companyId, UUID staffId) {
        Employee employee = employeeRepository.findById(staffId)
                .filter(e -> e.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found"));

        if (employee.getEmploymentTypeRole() != PlatformRole.STAFF) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_ACCOUNT_TYPE",
                    "Permissions can only be assigned to STAFF users.");
        }
        return employee;
    }

    private RoleAssignment resolveRoleAssignment(UUID companyId, Employee staff) {
        return roleAssignmentRepository
                .findFirstByUserIdAndCompanyIdOrderByCreatedAtAsc(staff.getUser().getId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Role assignment not found for staff member"));
    }

    private List<String> normalizeCodes(List<String> permissionCodes) {
        if (permissionCodes == null) {
            return List.of();
        }
        List<String> codes = permissionCodes.stream()
                .filter(StringUtils::hasText)
                .map(code -> code.trim().toUpperCase())
                .distinct()
                .toList();

        List<String> unknownCodes = codes.stream()
                .filter(code -> !PermissionCode.isKnown(code))
                .toList();
        if (!unknownCodes.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_PERMISSION",
                    "Unknown permission code: " + unknownCodes.getFirst());
        }
        return codes;
    }

    private StaffPermissionsResponse response(Employee staff, RoleAssignment assignment) {
        List<String> codes = roleAssignmentPermissionRepository
                .findAllByRoleAssignmentIdAndIsGranted(assignment.getId(), true)
                .stream()
                .map(grant -> grant.getPermission().getCode())
                .sorted()
                .toList();

        return new StaffPermissionsResponse(
                staff.getId(),
                staff.getUser().getId(),
                assignment.getId(),
                codes
        );
    }

    private User resolveActor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthSessionPrincipal principal)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "No authentication session found.");
        }
        return userRepository.findById(principal.userId())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED",
                        "Authenticated user not found."));
    }
}

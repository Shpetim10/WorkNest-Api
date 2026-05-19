package com.worknest.features.employee.application;

import com.worknest.common.exception.BusinessException;
import com.worknest.domain.entities.Employee;
import com.worknest.domain.entities.RoleAssignment;
import com.worknest.domain.entities.User;
import com.worknest.features.auth.repository.RoleAssignmentRepository;
import com.worknest.features.auth.repository.UserRepository;
import com.worknest.features.employee.repository.EmployeeRepository;
import com.worknest.features.employee.dto.MobileProfileResponse;
import com.worknest.security.AuthSessionPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MobileProfileServiceImpl implements MobileProfileService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;

    @Override
    public MobileProfileResponse getMyProfile() {
        AuthSessionPrincipal principal = principal();
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User was not found."));

        Employee employee = employeeRepository.findByUserIdAndCompanyId(principal.userId(), principal.companyId())
                .orElse(null);

        RoleAssignment roleAssignment = null;
        if (principal.roleAssignmentId() != null) {
            roleAssignment = roleAssignmentRepository.findById(principal.roleAssignmentId()).orElse(null);
        }

        String jobTitle = null;
        String department = null;
        String location = null;
        String role = null;

        if (roleAssignment != null) {
            jobTitle = roleAssignment.getJobTitle();
            if (roleAssignment.getRole() != null) {
                role = roleAssignment.getRole().name();
            }
        }

        if (employee != null) {
            if (employee.getDepartment() != null) {
                department = employee.getDepartment().getName();
            }
            if (employee.getCompanySite() != null) {
                String city = employee.getCompanySite().getCity();
                String country = employee.getCompanySite().getCountryCode();
                if (city != null && country != null) {
                    location = city + ", " + country;
                } else if (city != null) {
                    location = city;
                } else if (country != null) {
                    location = country;
                }
            }
            if (role == null && employee.getEmploymentTypeRole() != null) {
                role = employee.getEmploymentTypeRole().name();
            }
        }

        return new MobileProfileResponse(
                user.getFirstName(),
                user.getLastName(),
                user.getProfileImagePath(),
                jobTitle,
                department,
                location,
                role,
                user.getEmail()
        );
    }

    private AuthSessionPrincipal principal() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthSessionPrincipal p)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "No authentication session found.");
        }
        return p;
    }
}

package com.worknest.features.employee.application;

import com.worknest.common.api.PaginatedResponse;
import com.worknest.common.api.PaginationSupport;
import com.worknest.domain.entities.Employee;
import com.worknest.domain.entities.EmployeeSupervisorHistory;
import com.worknest.domain.entities.RoleAssignment;
import com.worknest.domain.entities.User;
import com.worknest.domain.enums.PlatformRole;
import org.springframework.security.access.AccessDeniedException;
import com.worknest.features.auth.repository.RoleAssignmentRepository;
import com.worknest.features.auth.repository.UserRepository;
import com.worknest.features.employee.dto.EmployeeAssignmentBoardResponse;
import com.worknest.features.employee.dto.EmployeeSummaryDto;
import com.worknest.features.employee.dto.ManagerSummaryDto;
import com.worknest.features.employee.dto.UpdateEmployeeAssignmentsRequest;
import com.worknest.features.employee.repository.EmployeeRepository;
import com.worknest.features.employee.repository.EmployeeSupervisorHistoryRepository;
import com.worknest.features.invitation.exception.InvalidInvitationRequestException;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployeeAssignmentServiceImpl implements EmployeeAssignmentService {

    private final EmployeeRepository employeeRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final EmployeeSupervisorHistoryRepository employeeSupervisorHistoryRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ManagerSummaryDto> listAssignableManagers(UUID companyId, Pageable pageable) {
        List<RoleAssignment> staffAssignments = roleAssignmentRepository.findAllActiveStaffByCompanyId(companyId);
        List<ManagerSummaryDto> managers = staffAssignments.stream()
                .map(ra -> new ManagerSummaryDto(
                        ra.getId(),
                        ra.getUser().getId(),
                        ra.getUser().getFirstName(),
                        ra.getUser().getLastName(),
                        ra.getUser().getEmail(),
                        ra.getJobTitle()
                ))
                .collect(Collectors.toList());
        return PaginationSupport.page(managers, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeAssignmentBoardResponse getManagerAssignmentBoard(
            UUID companyId,
            UUID managerRoleAssignmentId,
            Pageable assignedPageable,
            Pageable unassignedPageable
    ) {
        RoleAssignment manager = validateManager(companyId, managerRoleAssignmentId);

        List<Employee> assigned = employeeRepository.findAllAssignedToManager(companyId, PlatformRole.EMPLOYEE, manager.getId());
        List<Employee> unassigned = employeeRepository.findAllNotAssignedToManager(companyId, PlatformRole.EMPLOYEE, manager.getId());

        List<EmployeeSummaryDto> assignedDtos = assigned.stream().map(this::mapToSummary).collect(Collectors.toList());
        List<EmployeeSummaryDto> unassignedDtos = unassigned.stream().map(this::mapToSummary).collect(Collectors.toList());

        ManagerSummaryDto managerSummary = new ManagerSummaryDto(
                manager.getId(),
                manager.getUser().getId(),
                manager.getUser().getFirstName(),
                manager.getUser().getLastName(),
                manager.getUser().getEmail(),
                manager.getJobTitle()
        );

        return new EmployeeAssignmentBoardResponse(
                managerSummary,
                PaginatedResponse.from(PaginationSupport.page(assignedDtos, assignedPageable)),
                PaginatedResponse.from(PaginationSupport.page(unassignedDtos, unassignedPageable)),
                assignedDtos.size(),
                unassignedDtos.size()
        );
    }

    @Override
    @Transactional
    public void updateManagerAssignments(UUID companyId, UUID managerRoleAssignmentId, UpdateEmployeeAssignmentsRequest request) {
        RoleAssignment manager = validateManager(companyId, managerRoleAssignmentId);
        User currentUser = resolveCurrentAuthenticatedUser();

        List<Employee> currentlyAssigned = employeeRepository.findAllAssignedToManager(companyId, PlatformRole.EMPLOYEE, manager.getId());
        Set<UUID> requestedIds = request.assignedEmployeeIds() != null ? new HashSet<>(request.assignedEmployeeIds()) : new HashSet<>();
        Set<UUID> currentIds = currentlyAssigned.stream().map(Employee::getId).collect(Collectors.toSet());

        // Process Unassignments (Removals)
        List<Employee> toRemove = currentlyAssigned.stream()
                .filter(e -> !requestedIds.contains(e.getId()))
                .toList();

        for (Employee emp : toRemove) {
            RoleAssignment oldManager = emp.getSupervisorRoleAssignment();
            emp.setSupervisorRoleAssignment(null);
            employeeRepository.save(emp);
            appendHistory(emp, oldManager, null, currentUser);
        }

        // Process Assignments (Additions)
        Set<UUID> addedIds = requestedIds.stream()
                .filter(id -> !currentIds.contains(id))
                .collect(Collectors.toSet());
        
        if (!addedIds.isEmpty()) {
            List<Employee> toAdd = employeeRepository.findAllById(addedIds);
            for (Employee emp : toAdd) {
                if (!emp.getCompany().getId().equals(companyId)) {
                    throw new InvalidInvitationRequestException("Employee does not belong to this company");
                }
                if (emp.getEmploymentTypeRole() != PlatformRole.EMPLOYEE) {
                    throw new InvalidInvitationRequestException("Can only assign EMPLOYEE platform roles to a manager");
                }

                RoleAssignment oldManager = emp.getSupervisorRoleAssignment();
                emp.setSupervisorRoleAssignment(manager);
                employeeRepository.save(emp);
                appendHistory(emp, oldManager, manager, currentUser);
            }
        }
    }

    private RoleAssignment validateManager(UUID companyId, UUID managerRoleAssignmentId) {
        RoleAssignment manager = roleAssignmentRepository.findById(managerRoleAssignmentId)
                .orElseThrow(() -> new InvalidInvitationRequestException("Manager not found"));

        if (!manager.getCompany().getId().equals(companyId)) {
            throw new InvalidInvitationRequestException("Manager does not belong to the company");
        }
        if (manager.getRole() != PlatformRole.STAFF) {
            throw new InvalidInvitationRequestException("Only STAFF can act as managers");
        }
        return manager;
    }

    private void appendHistory(Employee employee, RoleAssignment fromManager, RoleAssignment toManager, User changedBy) {
        EmployeeSupervisorHistory history = new EmployeeSupervisorHistory();
        history.setCompany(employee.getCompany());
        history.setEmployee(employee);
        history.setFromRoleAssignment(fromManager);
        history.setToRoleAssignment(toManager);
        history.setChangedBy(changedBy);
        // Additional info about the transition action could be added here
        employeeSupervisorHistoryRepository.save(history);
    }

    private EmployeeSummaryDto mapToSummary(Employee e) {
        String jobTitle = (e.getUser() != null)
                ? roleAssignmentRepository.findFirstByUserIdAndCompanyIdOrderByCreatedAtAsc(e.getUser().getId(), e.getCompany().getId())
                    .map(com.worknest.domain.entities.RoleAssignment::getJobTitle)
                    .orElse(null)
                : null;

        return new EmployeeSummaryDto(
                e.getId(),
                e.getUser() != null ? e.getUser().getId() : null,
                e.getUser() != null ? e.getUser().getFirstName() : null,
                e.getUser() != null ? e.getUser().getLastName() : null,
                e.getUser() != null ? e.getUser().getEmail() : null,
                e.getDepartment() != null ? e.getDepartment().getName() : null,
                jobTitle
        );
    }

    private User resolveCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Unauthenticated");
        }
        if (!(auth.getPrincipal() instanceof com.worknest.security.AuthSessionPrincipal sessionPrincipal)) {
             throw new AccessDeniedException("Invalid authentication principal");
        }
        return userRepository.findById(sessionPrincipal.userId())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));
    }
}

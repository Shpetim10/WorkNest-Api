package com.worknest.features.employee.application;

import com.worknest.audit.service.AuthAuditService;
import com.worknest.audit.service.model.AuthAuditActorContext;
import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.CompanySite;
import com.worknest.domain.entities.Department;
import com.worknest.domain.entities.Employee;
import com.worknest.domain.entities.Permission;
import com.worknest.domain.entities.RoleAssignment;
import com.worknest.domain.entities.RoleAssignmentPermission;
import com.worknest.domain.entities.User;
import com.worknest.domain.entities.UserInvitation;
import com.worknest.domain.enums.CompanyStatus;
import com.worknest.domain.enums.EmploymentStatus;
import com.worknest.domain.enums.InvitationKind;
import com.worknest.domain.enums.PlatformAccess;
import com.worknest.domain.enums.PlatformRole;
import com.worknest.domain.enums.UserStatus;
import com.worknest.common.i18n.Language;
import com.worknest.features.auth.exception.UserAlreadyActiveException;
import com.worknest.features.auth.exception.UserEmailAlreadyExistsException;
import com.worknest.features.auth.repository.PermissionRepository;
import com.worknest.features.auth.repository.RoleAssignmentPermissionRepository;
import com.worknest.features.auth.repository.RoleAssignmentRepository;
import com.worknest.features.auth.repository.UserRepository;
import com.worknest.features.auth.utility.SecureTokenGenerator;
import com.worknest.features.auth.utility.Sha256TokenHashUtility;
import com.worknest.features.company.repository.CompanyRepository;
import com.worknest.features.companySite.repository.CompanySiteRepository;
import com.worknest.features.department.repository.DepartmentRepository;
import com.worknest.features.employee.dto.CreateEmployeeRequest;
import com.worknest.features.employee.dto.CreateStaffRequest;
import com.worknest.features.employee.dto.ProvisioningResponse;
import com.worknest.features.employee.dto.UpdateEmployeeJobDetailsRequest;
import com.worknest.features.employee.dto.UpdateEmployeeRequest;
import com.worknest.features.employee.dto.UpdateEmployeeResponse;
import com.worknest.features.employee.dto.UpdateStaffJobDetailsRequest;
import com.worknest.features.employee.dto.UpdateStaffRequest;
import com.worknest.features.employee.repository.EmployeeRepository;
import com.worknest.features.invitation.exception.InvalidInvitationRequestException;
import com.worknest.features.invitation.repository.UserInvitationRepository;
import com.worknest.features.notification.email.service.InvitationEmailService;
import com.worknest.realtime.event.EmployeeProvisionedDomainEvent;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserProvisioningServiceImpl implements UserProvisioningService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final CompanySiteRepository companySiteRepository;
    private final RoleAssignmentPermissionRepository roleAssignmentPermissionRepository;
    private final PermissionRepository permissionRepository;
    private final UserInvitationRepository userInvitationRepository;
    private final SecureTokenGenerator secureTokenGenerator;
    private final Sha256TokenHashUtility sha256TokenHashUtility;
    private final InvitationEmailService invitationEmailService;
    private final AuthAuditService authAuditService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.frontend.activation-link-base:https://app.worknest.local/activate-invitation}")
    private String activationLinkBase;

    @Override
    @Transactional
    public ProvisioningResponse createEmployee(CreateEmployeeRequest request) {
        Company company = validateAndGetCompany(request.companyId());
        User authenticatedUser = resolveCurrentAuthenticatedUser();
        RoleAssignment inviterRole = resolveActiveRoleAssignment(authenticatedUser.getId(), company.getId());
        validateInviterAuthorization(inviterRole);

        Department department = validateAndGetDepartment(request.departmentId(), company.getId());
        CompanySite site = validateAndGetSite(request.companySiteId(), company.getId());
        RoleAssignment supervisor = validateAndGetSupervisor(request.supervisorRoleAssignmentId(), company.getId());

        String normalizedEmail = request.email().trim().toLowerCase();
        User user = resolveOrCreateUser(company, normalizedEmail, request.firstName(), request.lastName(), null, null);
        if (hasActiveAssignmentInCompany(user, company)) {
            throw new UserAlreadyActiveException(normalizedEmail);
        }

        RoleAssignment roleAssignment = createInactiveRoleAssignment(user, company, PlatformRole.EMPLOYEE, PlatformAccess.MOBILE, authenticatedUser, request.jobTitle());

        Employee employee = new Employee();
        employee.setCompany(company);
        employee.setUser(user);
        employee.setDepartment(department);
        employee.setCompanySite(site);
        employee.setEmploymentTypeRole(PlatformRole.EMPLOYEE);
        employee.setEmploymentStatus(EmploymentStatus.PENDING);
        employee.setStartDate(request.startDate());
        employee.setSupervisorRoleAssignment(supervisor);
        employee.setEmploymentType(request.employmentType());
        employee.setContractDocumentKey(request.contractDocumentKey());
        employee.setContractDocumentPath(request.contractDocumentPath());
        employee.setContractExpiryDate(request.contractExpiryDate());
        employee.setLeaveDaysPerYear(request.leaveDaysPerYear());
        employee.setPaymentMethod(request.paymentMethod());
        employee.setMonthlySalary(request.monthlySalary());
        employee.setHourlyRate(request.hourlyRate());
        employee.setDailyWorkingHours(request.dailyWorkingHours());
        Employee savedEmployee = employeeRepository.save(employee);

        ProvisioningResponse response = processInvitation(normalizedEmail, user, company, roleAssignment, savedEmployee, authenticatedUser, inviterRole, PlatformRole.EMPLOYEE, request.jobTitle(), null);
        eventPublisher.publishEvent(new EmployeeProvisionedDomainEvent(company.getId(), savedEmployee.getId(), authenticatedUser.getId(), PlatformRole.EMPLOYEE, normalizedEmail));
        return response;
    }

    @Override
    @Transactional
    public ProvisioningResponse createStaff(CreateStaffRequest request) {
        Company company = validateAndGetCompany(request.companyId());
        User authenticatedUser = resolveCurrentAuthenticatedUser();
        RoleAssignment inviterRole = resolveActiveRoleAssignment(authenticatedUser.getId(), company.getId());
        validateInviterAuthorization(inviterRole);

        Department department = validateAndGetDepartment(request.departmentId(), company.getId());
        CompanySite site = validateAndGetSite(request.companySiteId(), company.getId());

        String normalizedEmail = request.email().trim().toLowerCase();
        User user = resolveOrCreateUser(company, normalizedEmail, request.firstName(), request.lastName(), null, request.preferredLanguage());
        if (hasActiveAssignmentInCompany(user, company)) {
            throw new UserAlreadyActiveException(normalizedEmail);
        }

        RoleAssignment roleAssignment = createInactiveRoleAssignment(user, company, PlatformRole.STAFF, PlatformAccess.BOTH, authenticatedUser, request.jobTitle());
        assignPermissions(roleAssignment, request.permissionCodes(), inviterRole.getUser());

        Employee employee = new Employee();
        employee.setCompany(company);
        employee.setUser(user);
        employee.setDepartment(department);
        employee.setCompanySite(site);
        employee.setEmploymentTypeRole(PlatformRole.STAFF);
        employee.setEmploymentStatus(EmploymentStatus.PENDING);
        employee.setStartDate(request.startDate());
        employee.setEmploymentType(request.employmentType());
        employee.setContractDocumentKey(request.contractDocumentKey());
        employee.setContractDocumentPath(request.contractDocumentPath());
        employee.setContractExpiryDate(request.contractExpiryDate());
        employee.setLeaveDaysPerYear(request.leaveDaysPerYear());
        employee.setPaymentMethod(request.paymentMethod());
        employee.setMonthlySalary(request.monthlySalary());
        employee.setHourlyRate(request.hourlyRate());
        employee.setDailyWorkingHours(request.dailyWorkingHours());
        Employee savedEmployee = employeeRepository.save(employee);

        assignEmployeesToStaff(request.assignedEmployeeIds(), roleAssignment, company);

        ProvisioningResponse response = processInvitation(normalizedEmail, user, company, roleAssignment, savedEmployee, authenticatedUser, inviterRole, PlatformRole.STAFF, request.jobTitle(), request.preferredLanguage());
        eventPublisher.publishEvent(new EmployeeProvisionedDomainEvent(company.getId(), savedEmployee.getId(), authenticatedUser.getId(), PlatformRole.STAFF, normalizedEmail));
        return response;
    }

    @Override
    @Transactional
    public ProvisioningResponse resendInvitation(UUID companyId, UUID employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new InvalidInvitationRequestException("Employee not found"));

        if (!employee.getCompany().getId().equals(companyId)) {
            throw new InvalidInvitationRequestException("Employee does not belong to this company");
        }

        if (employee.getUser().getStatus() == UserStatus.ACTIVE) {
            throw new InvalidInvitationRequestException("User is already active and does not require a new invitation");
        }

        if (employee.getEmploymentStatus() != EmploymentStatus.PENDING) {
            throw new InvalidInvitationRequestException("Invitation can only be resent for pending employees");
        }

        Company company = employee.getCompany();
        User user = employee.getUser();
        User authenticatedUser = resolveCurrentAuthenticatedUser();
        RoleAssignment inviterRole = resolveActiveRoleAssignment(authenticatedUser.getId(), company.getId());
        validateInviterAuthorization(inviterRole);

        RoleAssignment roleAssignment = roleAssignmentRepository.findFirstByUserIdAndCompanyIdOrderByCreatedAtAsc(user.getId(), company.getId())
                .orElseThrow(() -> new InvalidInvitationRequestException("Role assignment for employee not found"));

        String preferredLanguage = (user.getPreferredLanguage() != null) ? user.getPreferredLanguage().getCode() : null;

        return processInvitation(
                user.getEmail(),
                user,
                company,
                roleAssignment,
                employee,
                authenticatedUser,
                inviterRole,
                employee.getEmploymentTypeRole(),
                roleAssignment.getJobTitle(),
                preferredLanguage
        );
    }

    // -------------------------------------------------------------------------
    // Update — main details
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public UpdateEmployeeResponse updateEmployee(UUID companyId, UUID employeeId, UpdateEmployeeRequest request) {
        if (!companyId.equals(request.companyId())) {
            throw new InvalidInvitationRequestException("Company ID in path does not match request body");
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new InvalidInvitationRequestException("Employee not found"));

        if (!employee.getCompany().getId().equals(companyId)) {
            throw new InvalidInvitationRequestException("Employee does not belong to this company");
        }

        if (employee.getEmploymentTypeRole() != PlatformRole.EMPLOYEE) {
            throw new InvalidInvitationRequestException("Target record is not an EMPLOYEE — use the staff update endpoint");
        }

        User user = employee.getUser();
        user.setEmail(normalizeEmailForUpdate(request.email(), user.getId()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setDisplayName((user.getFirstName() + " " + user.getLastName()).trim());
        userRepository.save(user);

        RoleAssignment roleAssignment = roleAssignmentRepository
                .findFirstByUserIdAndCompanyIdOrderByCreatedAtAsc(user.getId(), companyId)
                .orElseThrow(() -> new InvalidInvitationRequestException("Role assignment not found for this employee"));
        roleAssignment.setJobTitle(request.jobTitle());
        roleAssignmentRepository.save(roleAssignment);

        Department department = validateAndGetDepartment(request.departmentId(), companyId);
        CompanySite site = validateAndGetSite(request.companySiteId(), companyId);
        RoleAssignment supervisor = validateAndGetSupervisor(request.supervisorRoleAssignmentId(), companyId);

        employee.setDepartment(department);
        employee.setCompanySite(site);
        employee.setSupervisorRoleAssignment(supervisor);
        if (request.startDate() != null) {
            employee.setStartDate(request.startDate());
        }
        employeeRepository.save(employee);

        return buildUpdateResponse(employee, user, roleAssignment, "Employee updated successfully");
    }

    @Override
    @Transactional
    public UpdateEmployeeResponse updateStaff(UUID companyId, UUID employeeId, UpdateStaffRequest request) {
        if (!companyId.equals(request.companyId())) {
            throw new InvalidInvitationRequestException("Company ID in path does not match request body");
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new InvalidInvitationRequestException("Staff member not found"));

        if (!employee.getCompany().getId().equals(companyId)) {
            throw new InvalidInvitationRequestException("Staff member does not belong to this company");
        }

        if (employee.getEmploymentTypeRole() != PlatformRole.STAFF) {
            throw new InvalidInvitationRequestException("Target record is not a STAFF member — use the employee update endpoint");
        }

        User user = employee.getUser();
        user.setEmail(normalizeEmailForUpdate(request.email(), user.getId()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setDisplayName((user.getFirstName() + " " + user.getLastName()).trim());
        userRepository.save(user);

        RoleAssignment roleAssignment = roleAssignmentRepository
                .findFirstByUserIdAndCompanyIdOrderByCreatedAtAsc(user.getId(), companyId)
                .orElseThrow(() -> new InvalidInvitationRequestException("Role assignment not found for this staff member"));
        roleAssignment.setJobTitle(request.jobTitle());
        roleAssignmentRepository.save(roleAssignment);

        Department department = validateAndGetDepartment(request.departmentId(), companyId);
        CompanySite site = validateAndGetSite(request.companySiteId(), companyId);

        employee.setDepartment(department);
        employee.setCompanySite(site);
        if (request.startDate() != null) {
            employee.setStartDate(request.startDate());
        }
        employeeRepository.save(employee);

        if (request.permissionCodes() != null) {
            User authenticatedUser = resolveCurrentAuthenticatedUser();
            roleAssignmentPermissionRepository.deleteAllByRoleAssignmentId(roleAssignment.getId());
            assignPermissions(roleAssignment, request.permissionCodes(), authenticatedUser);
        }

        if (request.assignedEmployeeIds() != null) {
            List<Employee> currentlyAssigned = employeeRepository
                    .findAllAssignedToManager(companyId, PlatformRole.EMPLOYEE, roleAssignment.getId());
            for (Employee emp : currentlyAssigned) {
                emp.setSupervisorRoleAssignment(null);
                employeeRepository.save(emp);
            }
            assignEmployeesToStaff(request.assignedEmployeeIds(), roleAssignment, employee.getCompany());
        }

        return buildUpdateResponse(employee, user, roleAssignment, "Staff updated successfully");
    }

    // -------------------------------------------------------------------------
    // Update — job & contract details
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public UpdateEmployeeResponse updateEmployeeJobDetails(UUID companyId, UUID employeeId, UpdateEmployeeJobDetailsRequest request) {
        if (!companyId.equals(request.companyId())) {
            throw new InvalidInvitationRequestException("Company ID in path does not match request body");
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new InvalidInvitationRequestException("Employee not found"));

        if (!employee.getCompany().getId().equals(companyId)) {
            throw new InvalidInvitationRequestException("Employee does not belong to this company");
        }

        if (employee.getEmploymentTypeRole() != PlatformRole.EMPLOYEE) {
            throw new InvalidInvitationRequestException("Target record is not an EMPLOYEE — use the staff job-details endpoint");
        }

        applyJobDetails(employee, request.employmentType(), request.contractDocumentKey(),
                request.contractDocumentPath(), request.contractExpiryDate(),
                request.leaveDaysPerYear(), request.paymentMethod(),
                request.monthlySalary(), request.hourlyRate(), request.dailyWorkingHours(),
                request.overtimeHourlyRate());
        employeeRepository.save(employee);

        User user = employee.getUser();
        RoleAssignment roleAssignment = roleAssignmentRepository
                .findFirstByUserIdAndCompanyIdOrderByCreatedAtAsc(user.getId(), companyId)
                .orElseThrow(() -> new InvalidInvitationRequestException("Role assignment not found"));

        return buildUpdateResponse(employee, user, roleAssignment, "Employee job details updated successfully");
    }

    @Override
    @Transactional
    public UpdateEmployeeResponse updateStaffJobDetails(UUID companyId, UUID employeeId, UpdateStaffJobDetailsRequest request) {
        if (!companyId.equals(request.companyId())) {
            throw new InvalidInvitationRequestException("Company ID in path does not match request body");
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new InvalidInvitationRequestException("Staff member not found"));

        if (!employee.getCompany().getId().equals(companyId)) {
            throw new InvalidInvitationRequestException("Staff member does not belong to this company");
        }

        if (employee.getEmploymentTypeRole() != PlatformRole.STAFF) {
            throw new InvalidInvitationRequestException("Target record is not a STAFF member — use the employee job-details endpoint");
        }

        applyJobDetails(employee, request.employmentType(), request.contractDocumentKey(),
                request.contractDocumentPath(), request.contractExpiryDate(),
                request.leaveDaysPerYear(), request.paymentMethod(),
                request.monthlySalary(), request.hourlyRate(), request.dailyWorkingHours(),
                request.overtimeHourlyRate());
        employeeRepository.save(employee);

        User user = employee.getUser();
        RoleAssignment roleAssignment = roleAssignmentRepository
                .findFirstByUserIdAndCompanyIdOrderByCreatedAtAsc(user.getId(), companyId)
                .orElseThrow(() -> new InvalidInvitationRequestException("Role assignment not found"));

        return buildUpdateResponse(employee, user, roleAssignment, "Staff job details updated successfully");
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void deleteEmployee(UUID companyId, UUID employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new InvalidInvitationRequestException("Employee not found"));

        if (!employee.getCompany().getId().equals(companyId)) {
            throw new InvalidInvitationRequestException("Employee does not belong to this company");
        }

        if (employee.getEmploymentTypeRole() != PlatformRole.EMPLOYEE) {
            throw new InvalidInvitationRequestException("Target record is not an EMPLOYEE — use the staff delete endpoint");
        }

        performDeletion(employee, companyId);
    }

    @Override
    @Transactional
    public void deleteStaff(UUID companyId, UUID employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new InvalidInvitationRequestException("Staff member not found"));

        if (!employee.getCompany().getId().equals(companyId)) {
            throw new InvalidInvitationRequestException("Staff member does not belong to this company");
        }

        if (employee.getEmploymentTypeRole() != PlatformRole.STAFF) {
            throw new InvalidInvitationRequestException("Target record is not a STAFF member — use the employee delete endpoint");
        }

        // Unassign all employees supervised by this staff member
        List<RoleAssignment> staffRoleAssignments = roleAssignmentRepository.findAllByUserIdAndCompanyId(
                employee.getUser().getId(), companyId);
        for (RoleAssignment ra : staffRoleAssignments) {
            List<Employee> supervised = employeeRepository.findAllAssignedToManager(companyId, PlatformRole.EMPLOYEE, ra.getId());
            for (Employee supervisedEmployee : supervised) {
                supervisedEmployee.setSupervisorRoleAssignment(null);
                employeeRepository.save(supervisedEmployee);
            }
        }

        performDeletion(employee, companyId);
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------
    @Override
    @Transactional
    public void activateEmployee(UUID companyId, UUID employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new InvalidInvitationRequestException("Staff member not found"));

        if (!employee.getCompany().getId().equals(companyId)) {
            throw new InvalidInvitationRequestException("Staff member does not belong to this company");
        }

        employee.setEmploymentStatus(EmploymentStatus.ACTIVE);
        employeeRepository.save(employee);
    }

    @Override
    @Transactional
    public void terminateEmployee(UUID companyId, UUID employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new InvalidInvitationRequestException("Staff member not found"));

        if (!employee.getCompany().getId().equals(companyId)) {
            throw new InvalidInvitationRequestException("Staff member does not belong to this company");
        }

        employee.setEmploymentStatus(EmploymentStatus.TERMINATED);
        employeeRepository.save(employee);

        // If terminated a staff member, also remove assigned employees
        if(employee.getEmploymentTypeRole() == PlatformRole.STAFF) {
            List<RoleAssignment> staffRoleAssignments = roleAssignmentRepository.findAllByUserIdAndCompanyId(
                    employee.getUser().getId(), companyId);
            for (RoleAssignment ra : staffRoleAssignments) {
                List<Employee> supervised = employeeRepository.findAllAssignedToManager(companyId, PlatformRole.EMPLOYEE, ra.getId());
                for (Employee supervisedEmployee : supervised) {
                    supervisedEmployee.setSupervisorRoleAssignment(null);
                    employeeRepository.save(supervisedEmployee);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Shared helpers
    // -------------------------------------------------------------------------

    private void performDeletion(Employee employee, UUID companyId) {
        User user = employee.getUser();
        UUID userId = user.getId();

        // Delete this user's pending invitations in this company
        userInvitationRepository.deleteAllByUserIdAndCompanyId(userId, companyId);

        // Delete permissions for all role assignments of this user in this company
        List<RoleAssignment> roleAssignments = roleAssignmentRepository.findAllByUserIdAndCompanyId(userId, companyId);
        for (RoleAssignment ra : roleAssignments) {
            roleAssignmentPermissionRepository.deleteAllByRoleAssignmentId(ra.getId());
        }

        // Delete employee record — DB CASCADE handles: attendance_day_records,
        // attendance_events, attendance_review_actions, employee_supervisor_history
        employeeRepository.delete(employee);

        // Delete this company's role assignments
        roleAssignmentRepository.deleteAll(roleAssignments);

        // Delete the user entity only when they have no remaining role assignments anywhere
        if (roleAssignmentRepository.countByUserId(userId) == 0) {
            userRepository.delete(user);
        }
    }

    private void applyJobDetails(Employee employee,
            com.worknest.domain.enums.EmploymentType employmentType,
            String contractDocumentKey, String contractDocumentPath,
            java.time.LocalDate contractExpiryDate, Integer leaveDaysPerYear,
            com.worknest.domain.enums.PaymentMethod paymentMethod,
            java.math.BigDecimal monthlySalary, java.math.BigDecimal hourlyRate,
            java.math.BigDecimal dailyWorkingHours, java.math.BigDecimal overtimeHourlyRate) {
        employee.setEmploymentType(employmentType);
        employee.setContractDocumentKey(contractDocumentKey);
        employee.setContractDocumentPath(contractDocumentPath);
        employee.setContractExpiryDate(contractExpiryDate);
        employee.setLeaveDaysPerYear(leaveDaysPerYear);
        employee.setPaymentMethod(paymentMethod);
        employee.setMonthlySalary(monthlySalary);
        employee.setHourlyRate(hourlyRate);
        employee.setDailyWorkingHours(dailyWorkingHours);
        employee.setOvertimeHourlyRate(overtimeHourlyRate);
    }

    private UpdateEmployeeResponse buildUpdateResponse(Employee employee, User user, RoleAssignment roleAssignment, String message) {
        return new UpdateEmployeeResponse(
                employee.getId(),
                user.getId(),
                roleAssignment.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                roleAssignment.getJobTitle(),
                employee.getEmploymentTypeRole(),
                employee.getEmploymentStatus(),
                employee.getDepartment() != null ? employee.getDepartment().getId() : null,
                employee.getDepartment() != null ? employee.getDepartment().getName() : null,
                employee.getCompanySite() != null ? employee.getCompanySite().getId() : null,
                employee.getCompanySite() != null ? employee.getCompanySite().getName() : null,
                employee.getSupervisorRoleAssignment() != null ? employee.getSupervisorRoleAssignment().getId() : null,
                employee.getStartDate(),
                employee.getEmploymentType(),
                employee.getContractDocumentKey(),
                employee.getContractExpiryDate(),
                employee.getLeaveDaysPerYear(),
                employee.getPaymentMethod(),
                employee.getMonthlySalary(),
                employee.getHourlyRate(),
                employee.getDailyWorkingHours(),
                message
        );
    }

    private ProvisioningResponse processInvitation(
            String email, User user, Company company, RoleAssignment roleAssignment, Employee employee,
            User invitedBy, RoleAssignment inviterRole, PlatformRole platformRole, String invitedJobTitle, String preferredLanguage) {

        String rawToken = secureTokenGenerator.generateToken();
        String tokenHash = sha256TokenHashUtility.hash(rawToken);
        Instant expiresAt = Instant.now().plusSeconds(24 * 60 * 60L);

        UserInvitation invitation = new UserInvitation();
        invitation.setCompany(company);
        invitation.setUser(user);
        invitation.setEmail(email);
        invitation.setTokenHash(tokenHash);
        invitation.setInvitedBy(invitedBy);
        invitation.setPlatformRole(platformRole);
        invitation.setPlatformAccess(roleAssignment.getPlatformAccess());
        invitation.setInvitationKind(platformRole == PlatformRole.STAFF ? InvitationKind.STAFF_INVITATION : InvitationKind.EMPLOYEE_INVITATION);
        invitation.setInvitedJobTitle(invitedJobTitle);
        invitation.setExpiresAt(expiresAt);

        UserInvitation savedInvitation = userInvitationRepository.save(invitation);

        if (user.getStatus() == UserStatus.ACTIVE) {
            invitationEmailService.sendNewPositionEmail(
                    company, email, user.getDisplayName(), platformRole,
                    preferredLanguage != null ? preferredLanguage : "en");
        } else {
            invitationEmailService.sendProvisioningEmail(
                    company, email, user.getDisplayName(), platformRole,
                    activationLinkBase + "?token=" + rawToken,
                    preferredLanguage != null ? preferredLanguage : "en");
        }

        AuthAuditActorContext actorContext = new AuthAuditActorContext(
                company.getId(), company.getName(),
                invitedBy.getId(), inviterRole.getId(),
                inviterRole.getRole(), inviterRole.getJobTitle(), null
        );

        authAuditService.appendInvitationCreated(
                actorContext, savedInvitation.getId(), email, platformRole,
                roleAssignment.getPlatformAccess(), null, expiresAt
        );

        return new ProvisioningResponse(
                employee.getId(), user.getId(), roleAssignment.getId(), savedInvitation.getId(),
                email, rawToken, expiresAt, "Provisioned successfully"
        );
    }

    private Company validateAndGetCompany(UUID companyId) {
        if (companyId == null) throw new InvalidInvitationRequestException("companyId is required");
        Company company = companyRepository.findById(companyId).orElseThrow(() -> new InvalidInvitationRequestException("Company not found"));
        if (company.getStatus() != CompanyStatus.ACTIVE) throw new InvalidInvitationRequestException("Company must be active");
        return company;
    }

    private Department validateAndGetDepartment(UUID departmentId, UUID companyId) {
        if (departmentId == null) return null;
        Department dept = departmentRepository.findById(departmentId).orElseThrow(() -> new InvalidInvitationRequestException("Department not found"));
        if (!dept.getCompany().getId().equals(companyId)) throw new InvalidInvitationRequestException("Department does not belong to the company");
        return dept;
    }

    private CompanySite validateAndGetSite(UUID siteId, UUID companyId) {
        if (siteId == null) return null;
        CompanySite site = companySiteRepository.findById(siteId).orElseThrow(() -> new InvalidInvitationRequestException("CompanySite not found"));
        if (!site.getCompany().getId().equals(companyId)) throw new InvalidInvitationRequestException("CompanySite does not belong to the company");
        return site;
    }

    private RoleAssignment validateAndGetSupervisor(UUID supervisorId, UUID companyId) {
        if (supervisorId == null) return null;
        RoleAssignment sup = roleAssignmentRepository.findById(supervisorId).orElseThrow(() -> new InvalidInvitationRequestException("Supervisor not found"));
        if (!sup.getCompany().getId().equals(companyId)) throw new InvalidInvitationRequestException("Supervisor does not belong to the company");
        if (sup.getRole() != PlatformRole.STAFF) throw new InvalidInvitationRequestException("Supervisor must be STAFF");
        return sup;
    }

    private void assignPermissions(RoleAssignment roleAssignment, List<String> permissionCodes, User grantedBy) {
        if (permissionCodes == null || permissionCodes.isEmpty()) return;
        List<String> codes = permissionCodes.stream().filter(StringUtils::hasText).map(String::trim).distinct().toList();
        if (codes.isEmpty()) return;

        List<Permission> permissions = permissionRepository.findAllByCodeIn(codes);
        if (permissions.size() != codes.size()) {
            throw new InvalidInvitationRequestException("One or more invalid permission codes");
        }

        for (Permission permission : permissions) {
            RoleAssignmentPermission rap = new RoleAssignmentPermission();
            rap.setRoleAssignment(roleAssignment);
            rap.setPermission(permission);
            rap.setIsGranted(true);
            rap.setGrantedBy(grantedBy);
            roleAssignmentPermissionRepository.save(rap);
        }
    }

    private RoleAssignment createInactiveRoleAssignment(User user, Company company, PlatformRole role, PlatformAccess access, User createdBy, String jobTitle) {
        RoleAssignment ra = roleAssignmentRepository.findFirstByUserIdAndCompanyIdAndIsActiveTrue(user.getId(), company.getId()).orElseGet(RoleAssignment::new);
        ra.setCompany(company);
        ra.setUser(user);
        ra.setRole(role);
        ra.setPlatformAccess(access);
        ra.setIsActive(false);
        ra.setCreatedBy(createdBy);
        ra.setJobTitle(jobTitle);
        return roleAssignmentRepository.save(ra);
    }

    private User resolveOrCreateUser(Company company, String email, String fn, String ln, String phone, String lang) {
        User user = userRepository.findAllByEmailIgnoreCase(email)
                .stream()
                .sorted(Comparator.comparing((User u) -> u.getStatus() == UserStatus.ACTIVE ? 0 : 1))
                .findFirst()
                .orElseGet(() -> {
                    User u = new User();
                    u.setEmail(email);
                    u.setStatus(UserStatus.PENDING);
                    u.setFailedLoginCount((short) 0);
                    return u;
                });

        if (user.getId() == null) {
            user.setFirstName(fn != null ? fn.trim() : "");
            user.setLastName(ln != null ? ln.trim() : "");
            user.setDisplayName((user.getFirstName() + " " + user.getLastName()).trim());
            user.setPhoneNumber(StringUtils.hasText(phone) ? phone.trim() : null);
            user.setPreferredLanguage(Language.fromCode(lang != null ? lang : "en"));
        }

        return userRepository.save(user);
    }

    private String normalizeEmailForUpdate(String email, UUID currentUserId) {
        String normalizedEmail = email.trim().toLowerCase();
        boolean usedByAnotherUser = userRepository.findAllByEmailIgnoreCase(normalizedEmail).stream()
                .anyMatch(existingUser -> !Objects.equals(existingUser.getId(), currentUserId));
        if (usedByAnotherUser) {
            throw new UserEmailAlreadyExistsException(normalizedEmail);
        }
        return normalizedEmail;
    }

    private boolean hasActiveAssignmentInCompany(User user, Company company) {
        if (user.getId() == null) return false;
        return roleAssignmentRepository.findFirstByUserIdAndCompanyIdAndIsActiveTrue(user.getId(), company.getId()).isPresent();
    }

    private User resolveCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new org.springframework.security.access.AccessDeniedException("Unauthenticated");
        }
        if (!(auth.getPrincipal() instanceof com.worknest.security.AuthSessionPrincipal sessionPrincipal)) {
            throw new org.springframework.security.access.AccessDeniedException("Invalid authentication principal");
        }
        return userRepository.findById(sessionPrincipal.userId()).orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Authenticated user not found"));
    }

    private RoleAssignment resolveActiveRoleAssignment(UUID userId, UUID companyId) {
        return roleAssignmentRepository.findFirstByUserIdAndCompanyIdAndIsActiveTrue(userId, companyId)
                .orElseThrow(() -> new InvalidInvitationRequestException("Inviter must have an active role assignment"));
    }

    private void validateInviterAuthorization(RoleAssignment inviterRole) {
        if (inviterRole.getRole() != PlatformRole.ADMIN && inviterRole.getRole() != PlatformRole.SUPERADMIN && inviterRole.getRole() != PlatformRole.STAFF) {
            throw new org.springframework.security.access.AccessDeniedException("Only admins or staff can provision users");
        }
    }

    private void assignEmployeesToStaff(List<UUID> assignedEmployeeIds, RoleAssignment staffRoleAssignment, Company company) {
        if (assignedEmployeeIds == null || assignedEmployeeIds.isEmpty()) return;
        for (UUID empId : assignedEmployeeIds) {
            employeeRepository.findById(empId).ifPresent(emp -> {
                if (emp.getCompany().getId().equals(company.getId()) &&
                    emp.getEmploymentTypeRole() == PlatformRole.EMPLOYEE) {
                    emp.setSupervisorRoleAssignment(staffRoleAssignment);
                    employeeRepository.save(emp);
                }
            });
        }
    }
}

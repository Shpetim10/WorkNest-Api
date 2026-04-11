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
import com.worknest.features.employee.repository.EmployeeRepository;
import com.worknest.features.invitation.exception.InvalidInvitationRequestException;
import com.worknest.features.invitation.repository.UserInvitationRepository;
import com.worknest.features.notification.email.service.InvitationEmailService;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
        User user = resolveOrCreateUser(company, normalizedEmail, request.firstName(), request.lastName(), request.phoneNumber(), request.preferredLanguage());
        if (hasActiveAssignmentInCompany(user, company)) {
            throw new UserAlreadyActiveException(normalizedEmail);
        }

        RoleAssignment roleAssignment = createInactiveRoleAssignment(user, company, PlatformRole.EMPLOYEE, PlatformAccess.MOBILE, authenticatedUser);
        
        Employee employee = new Employee();
        employee.setCompany(company);
        employee.setUser(user);
        employee.setDepartment(department);
        employee.setCompanySite(site);
        employee.setEmploymentTypeRole(PlatformRole.EMPLOYEE);
        employee.setEmploymentStatus(EmploymentStatus.ACTIVE);
        employee.setStartDate(request.startDate());
        employee.setSupervisorRoleAssignment(supervisor);
        Employee savedEmployee = employeeRepository.save(employee);

        return processInvitation(normalizedEmail, user, company, roleAssignment, savedEmployee, authenticatedUser, inviterRole, PlatformRole.EMPLOYEE, request.preferredLanguage());
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

        RoleAssignment roleAssignment = createInactiveRoleAssignment(user, company, PlatformRole.STAFF, PlatformAccess.BOTH, authenticatedUser);
        assignPermissions(roleAssignment, request.permissionCodes(), inviterRole.getUser());

        Employee employee = new Employee();
        employee.setCompany(company);
        employee.setUser(user);
        employee.setDepartment(department);
        employee.setCompanySite(site);
        employee.setEmploymentTypeRole(PlatformRole.STAFF);
        employee.setEmploymentStatus(EmploymentStatus.ACTIVE);
        employee.setStartDate(request.startDate());
        Employee savedEmployee = employeeRepository.save(employee);

        return processInvitation(normalizedEmail, user, company, roleAssignment, savedEmployee, authenticatedUser, inviterRole, PlatformRole.STAFF, request.preferredLanguage());
    }

    private ProvisioningResponse processInvitation(
            String email, User user, Company company, RoleAssignment roleAssignment, Employee employee, 
            User invitedBy, RoleAssignment inviterRole, PlatformRole platformRole, String preferredLanguage) {

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
        invitation.setExpiresAt(expiresAt);

        UserInvitation savedInvitation = userInvitationRepository.save(invitation);

        invitationEmailService.sendProvisioningEmail(
                company,
                email,
                user.getDisplayName(),
                platformRole,
                activationLinkBase + "?token=" + rawToken,
                preferredLanguage != null ? preferredLanguage : "en");

        AuthAuditActorContext actorContext = new AuthAuditActorContext(
                company.getId(),
                company.getName(),
                invitedBy.getId(),
                inviterRole.getId(),
                inviterRole.getRole(),
                inviterRole.getJobTitle(),
                null
        );

        authAuditService.appendInvitationCreated(
                actorContext,
                savedInvitation.getId(),
                email,
                platformRole,
                roleAssignment.getPlatformAccess(),
                null,
                expiresAt
        );

        return new ProvisioningResponse(
                employee.getId(),
                user.getId(),
                roleAssignment.getId(),
                savedInvitation.getId(),
                email,
                rawToken,
                expiresAt,
                "Provisioned successfully"
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
        if (supervisorId == null) throw new InvalidInvitationRequestException("Supervisor is required for Employee");
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

    private RoleAssignment createInactiveRoleAssignment(User user, Company company, PlatformRole role, PlatformAccess access, User createdBy) {
        RoleAssignment ra = roleAssignmentRepository.findFirstByUserIdAndCompanyIdAndIsActiveTrue(user.getId(), company.getId()).orElseGet(RoleAssignment::new);
        ra.setCompany(company);
        ra.setUser(user);
        ra.setRole(role);
        ra.setPlatformAccess(access);
        ra.setIsActive(false);
        ra.setCreatedBy(createdBy);
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
             user.setPreferredLanguage(Language.fromCode(lang != null ? lang : "en")); // Or 'en' depending on defaults
        }

        return userRepository.save(user);
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
}

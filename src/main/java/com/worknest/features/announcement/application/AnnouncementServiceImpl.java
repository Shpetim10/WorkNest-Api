package com.worknest.features.announcement.application;

import com.worknest.common.exception.BusinessException;
import com.worknest.domain.entities.Announcement;
import com.worknest.domain.entities.AnnouncementRead;
import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.Department;
import com.worknest.domain.entities.Employee;
import com.worknest.domain.entities.User;
import com.worknest.domain.enums.AnnouncementAudience;
import com.worknest.domain.enums.EmploymentStatus;
import com.worknest.features.announcement.dto.AnnouncementListResponse;
import com.worknest.features.announcement.dto.AnnouncementListResponse.TargetEmployeeSummary;
import com.worknest.features.announcement.dto.CreateAnnouncementRequest;
import com.worknest.features.announcement.dto.MobileAnnouncementDetail;
import com.worknest.features.announcement.dto.MobileAnnouncementListItem;
import com.worknest.features.announcement.dto.UnreadCountResponse;
import com.worknest.features.announcement.exception.AnnouncementNotFoundException;
import com.worknest.features.announcement.repository.AnnouncementReadRepository;
import com.worknest.features.announcement.repository.AnnouncementRepository;
import com.worknest.features.employee.repository.EmployeeRepository;
import com.worknest.realtime.event.AnnouncementCreatedDomainEvent;
import com.worknest.realtime.event.AnnouncementDeletedDomainEvent;
import com.worknest.security.AuthSessionPrincipal;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import com.worknest.features.notification.application.NotificationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementReadRepository announcementReadRepository;
    private final EmployeeRepository employeeRepository;
    private final EntityManager entityManager;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationService notificationService;

    @Override
    public AnnouncementListResponse create(UUID companyId, CreateAnnouncementRequest request) {
        validateAudienceTargets(request);

        AuthSessionPrincipal principal = principal();
        Announcement announcement = new Announcement();
        announcement.setCompany(entityManager.getReference(Company.class, companyId));
        announcement.setTitle(request.title().trim());
        announcement.setContent(request.content());
        announcement.setTargetAudience(request.targetAudience());
        announcement.setPriority(request.priorityOrDefault());
        announcement.setCreatedByUser(entityManager.getReference(User.class, principal.userId()));

        if (request.targetAudience() == AnnouncementAudience.DEPARTMENT
                && request.targetDepartmentIds() != null) {
            for (UUID deptId : request.targetDepartmentIds()) {
                announcement.getTargetDepartments().add(entityManager.getReference(Department.class, deptId));
            }
        }

        if (request.targetAudience() == AnnouncementAudience.SPECIFIC_USERS
                && request.targetEmployeeIds() != null) {
            for (UUID empId : request.targetEmployeeIds()) {
                announcement.getTargetEmployees().add(entityManager.getReference(Employee.class, empId));
            }
        }

        announcement = announcementRepository.save(announcement);
        AnnouncementListResponse response = toListResponse(announcement);
        eventPublisher.publishEvent(new AnnouncementCreatedDomainEvent(companyId, announcement.getId(), principal.userId(), response));

        // Create in-app notifications for targeted employees
        notificationService.createAnnouncementNotifications(announcement);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AnnouncementListResponse> listForAdmin(UUID companyId, Pageable pageable) {
        return announcementRepository.findAllByCompanyIdOrderByCreatedAtDesc(companyId, pageable)
                .map(this::toListResponse);
    }

    @Override
    public void delete(UUID companyId, UUID id) {
        AuthSessionPrincipal actor = principal();
        Announcement announcement = announcementRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(AnnouncementNotFoundException::new);
        announcementRepository.delete(announcement);
        eventPublisher.publishEvent(new AnnouncementDeletedDomainEvent(companyId, id, actor.userId()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MobileAnnouncementListItem> listForEmployee(Pageable pageable) {
        AuthSessionPrincipal principal = principal();
        Employee employee = resolveCurrentEmployee(principal);
        UUID departmentId = employee.getDepartment() != null ? employee.getDepartment().getId() : null;

        Page<Announcement> visible = announcementRepository.findVisibleToEmployee(
                principal.companyId(), employee.getId(), departmentId, pageable);

        if (visible.isEmpty()) {
            return Page.empty(pageable);
        }

        List<UUID> visibleIds = visible.getContent().stream().map(Announcement::getId).toList();
        Set<UUID> readIds = announcementReadRepository.findReadAnnouncementIds(employee.getId(), visibleIds);

        return visible.map(a -> toMobileListItem(a, readIds.contains(a.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public MobileAnnouncementDetail getDetail(UUID id) {
        AuthSessionPrincipal principal = principal();
        Employee employee = resolveCurrentEmployee(principal);

        Announcement announcement = announcementRepository.findByIdAndCompanyId(id, principal.companyId())
                .orElseThrow(AnnouncementNotFoundException::new);

        boolean read = announcementReadRepository
                .findByAnnouncementIdAndEmployeeId(id, employee.getId())
                .isPresent();

        return new MobileAnnouncementDetail(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getContent(),
                announcement.getPriority(),
                announcement.getCreatedAt(),
                read
        );
    }

    @Override
    public void markAsRead(UUID id) {
        AuthSessionPrincipal principal = principal();
        Employee employee = resolveCurrentEmployee(principal);

        Announcement announcement = announcementRepository.findByIdAndCompanyId(id, principal.companyId())
                .orElseThrow(AnnouncementNotFoundException::new);

        boolean alreadyRead = announcementReadRepository
                .findByAnnouncementIdAndEmployeeId(id, employee.getId())
                .isPresent();

        if (!alreadyRead) {
            AnnouncementRead read = new AnnouncementRead();
            read.setAnnouncement(announcement);
            read.setEmployee(employee);
            read.setReadAt(Instant.now());
            announcementReadRepository.save(read);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount() {
        AuthSessionPrincipal principal = principal();
        Employee employee = resolveCurrentEmployee(principal);
        UUID departmentId = employee.getDepartment() != null ? employee.getDepartment().getId() : null;

        List<Announcement> visible = announcementRepository.findVisibleToEmployee(
                        principal.companyId(),
                        employee.getId(),
                        departmentId,
                        Pageable.unpaged())
                .getContent();

        if (visible.isEmpty()) {
            return new UnreadCountResponse(0);
        }

        List<UUID> visibleIds = visible.stream().map(Announcement::getId).toList();
        Set<UUID> readIds = announcementReadRepository.findReadAnnouncementIds(employee.getId(), visibleIds);

        return new UnreadCountResponse(visible.size() - readIds.size());
    }

    private void validateAudienceTargets(CreateAnnouncementRequest request) {
        if (request.targetAudience() == AnnouncementAudience.DEPARTMENT
                && (request.targetDepartmentIds() == null || request.targetDepartmentIds().isEmpty())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "MISSING_TARGET_DEPARTMENTS",
                    "At least one department must be selected when targeting a department.");
        }
        if (request.targetAudience() == AnnouncementAudience.SPECIFIC_USERS
                && (request.targetEmployeeIds() == null || request.targetEmployeeIds().isEmpty())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "MISSING_TARGET_EMPLOYEES",
                    "At least one employee must be selected when targeting specific users.");
        }
    }

    private Employee resolveCurrentEmployee(AuthSessionPrincipal principal) {
        Employee employee = employeeRepository
                .findByUserIdAndCompanyId(principal.userId(), principal.companyId())
                .orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "EMPLOYEE_PROFILE_NOT_FOUND",
                        "Employee profile is not configured."));
        if (employee.getEmploymentStatus() != EmploymentStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "EMPLOYEE_INACTIVE", "Employee is not active.");
        }
        return employee;
    }

    private AnnouncementListResponse toListResponse(Announcement a) {
        User author = a.getCreatedByUser();
        String authorName = "Unknown";
        if (author != null) {
            authorName = (author.getDisplayName() != null && !author.getDisplayName().isBlank())
                    ? author.getDisplayName()
                    : author.getFirstName() + " " + author.getLastName();
        }
        List<TargetEmployeeSummary> targetEmployees = null;
        if (a.getTargetAudience() == AnnouncementAudience.SPECIFIC_USERS && !a.getTargetEmployees().isEmpty()) {
            targetEmployees = a.getTargetEmployees().stream()
                    .map(e -> new TargetEmployeeSummary(e.getId(), e.getUser().getFirstName(), e.getUser().getLastName()))
                    .toList();
        }
        return new AnnouncementListResponse(
                a.getId(),
                a.getTitle(),
                a.getContent(),
                a.getTargetAudience(),
                a.getPriority(),
                authorName,
                a.getCreatedAt(),
                targetEmployees
        );
    }

    private MobileAnnouncementListItem toMobileListItem(Announcement a, boolean read) {
        String preview = a.getContent().length() > 120
                ? a.getContent().substring(0, 120)
                : a.getContent();
        return new MobileAnnouncementListItem(
                a.getId(),
                a.getTitle(),
                preview,
                a.getPriority(),
                a.getCreatedAt(),
                read
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

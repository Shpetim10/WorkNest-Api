package com.worknest.features.notification.application;

import com.worknest.common.exception.BusinessException;
import com.worknest.domain.entities.Announcement;
import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.Employee;
import com.worknest.domain.entities.Notification;
import com.worknest.domain.entities.User;
import com.worknest.domain.enums.AnnouncementAudience;
import com.worknest.domain.enums.EmploymentStatus;
import com.worknest.domain.enums.NotificationTargetType;
import com.worknest.domain.enums.NotificationType;
import com.worknest.features.auth.repository.UserRepository;
import com.worknest.features.employee.repository.EmployeeRepository;
import com.worknest.features.notification.dto.NotificationDtos.NotificationResponse;
import com.worknest.features.notification.dto.NotificationDtos.UnreadCountResponse;
import com.worknest.features.notification.repository.NotificationRepository;
import com.worknest.security.AuthSessionPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications() {
        AuthSessionPrincipal p = principal();
        return notificationRepository.findAllByRecipientUserIdAndCompanyIdOrderByCreatedAtDesc(p.userId(), p.companyId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount() {
        AuthSessionPrincipal p = principal();
        long count = notificationRepository.countByRecipientUserIdAndCompanyIdAndReadFalse(p.userId(), p.companyId());
        return new UnreadCountResponse(count);
    }

    @Override
    public void markAsRead(UUID id) {
        AuthSessionPrincipal p = principal();
        Notification notification = notificationRepository.findByIdAndRecipientUserIdAndCompanyId(id, p.userId(), p.companyId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "Notification not found or access denied"));
        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
        }
    }

    @Override
    public void markAllAsRead() {
        AuthSessionPrincipal p = principal();
        List<Notification> unread = notificationRepository.findAllByRecipientUserIdAndCompanyIdAndReadFalse(p.userId(), p.companyId());
        Instant now = Instant.now();
        unread.forEach(n -> {
            n.setRead(true);
            n.setReadAt(now);
        });
        notificationRepository.saveAll(unread);
    }

    @Override
    public void createNotification(Company company, User recipientUser, NotificationType type, String title, String message, UUID targetId, NotificationTargetType targetType) {
        createNotificationInternal(company, recipientUser, type, title, message, targetId, targetType);
    }

    @Override
    public void createAnnouncementNotifications(Announcement announcement) {
        List<Employee> allEmployees = employeeRepository.findAllByCompanyId(announcement.getCompany().getId());
        for (Employee emp : allEmployees) {
            if (emp.getEmploymentStatus() != EmploymentStatus.ACTIVE) {
                continue;
            }
            boolean matches = false;
            if (announcement.getTargetAudience() == AnnouncementAudience.ALL_EMPLOYEES) {
                matches = true;
            } else if (announcement.getTargetAudience() == AnnouncementAudience.DEPARTMENT) {
                if (emp.getDepartment() != null && announcement.getTargetDepartments().stream()
                        .anyMatch(d -> d.getId().equals(emp.getDepartment().getId()))) {
                    matches = true;
                }
            } else if (announcement.getTargetAudience() == AnnouncementAudience.SPECIFIC_USERS) {
                if (announcement.getTargetEmployees().stream()
                        .anyMatch(e -> e.getId().equals(emp.getId()))) {
                    matches = true;
                }
            }

            if (matches) {
                createNotificationInternal(
                        announcement.getCompany(),
                        emp.getUser(),
                        NotificationType.ANNOUNCEMENT,
                        "New Announcement",
                        announcement.getTitle(),
                        announcement.getId(),
                        NotificationTargetType.ANNOUNCEMENT
                );
            }
        }
    }

    private void createNotificationInternal(Company company, User recipientUser, NotificationType type, String title, String message, UUID targetId, NotificationTargetType targetType) {
        Notification notification = new Notification();
        notification.setCompany(company);
        notification.setRecipientUser(recipientUser);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setTargetId(targetId);
        notification.setTargetType(targetType);
        notification.setRead(false);
        notificationRepository.save(notification);
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                n.getTargetId(),
                n.getTargetType(),
                n.isRead(),
                n.getCreatedAt()
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

package com.worknest.features.notification.application;

import com.worknest.domain.entities.Announcement;
import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.User;
import com.worknest.domain.enums.NotificationTargetType;
import com.worknest.domain.enums.NotificationType;
import com.worknest.features.notification.dto.NotificationDtos.NotificationResponse;
import com.worknest.features.notification.dto.NotificationDtos.UnreadCountResponse;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    List<NotificationResponse> getNotifications();

    UnreadCountResponse getUnreadCount();

    void markAsRead(UUID id);

    void markAllAsRead();

    void createNotification(Company company, User recipientUser, NotificationType type, String title, String message, UUID targetId, NotificationTargetType targetType);

    void createAnnouncementNotifications(Announcement announcement);
}

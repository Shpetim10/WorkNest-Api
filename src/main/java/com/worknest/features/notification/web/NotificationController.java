package com.worknest.features.notification.web;

import com.worknest.common.api.ApiResponse;
import com.worknest.features.notification.application.NotificationService;
import com.worknest.features.notification.dto.NotificationDtos.NotificationResponse;
import com.worknest.features.notification.dto.NotificationDtos.UnreadCountResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "In-app notifications for employees and administrators")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('EMPLOYEE', 'STAFF', 'ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Get all notifications for the authenticated user, ordered newest first")
    public ApiResponse<List<NotificationResponse>> getNotifications() {
        return ApiResponse.success("Notifications fetched successfully", notificationService.getNotifications());
    }

    @GetMapping("/unread-count")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('EMPLOYEE', 'STAFF', 'ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Get the count of unread notifications for the authenticated user")
    public ApiResponse<UnreadCountResponse> getUnreadCount() {
        return ApiResponse.success("Unread count fetched successfully", notificationService.getUnreadCount());
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('EMPLOYEE', 'STAFF', 'ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Mark a specific notification as read")
    public ApiResponse<Void> markAsRead(@PathVariable UUID id) {
        notificationService.markAsRead(id);
        return ApiResponse.success("Notification marked as read", null);
    }

    @PatchMapping("/read-all")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('EMPLOYEE', 'STAFF', 'ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Mark all notifications as read for the authenticated user")
    public ApiResponse<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ApiResponse.success("All notifications marked as read", null);
    }
}

package com.worknest.features.announcement.web;

import com.worknest.common.api.ApiResponse;
import com.worknest.features.announcement.application.AnnouncementService;
import com.worknest.features.announcement.dto.MobileAnnouncementDetail;
import com.worknest.features.announcement.dto.MobileAnnouncementListItem;
import com.worknest.features.announcement.dto.UnreadCountResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mobile/announcements")
@RequiredArgsConstructor
@Tag(name = "Mobile Announcements", description = "Announcement APIs for the employee mobile app")
public class MobileAnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('EMPLOYEE', 'STAFF', 'ADMIN', 'SUPERADMIN')")
    @Operation(summary = "List announcements", description = "Returns all announcements visible to the current employee")
    public ApiResponse<List<MobileAnnouncementListItem>> list() {
        return ApiResponse.success("Announcements loaded", announcementService.listForEmployee());
    }

    @GetMapping("/unread-count")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('EMPLOYEE', 'STAFF', 'ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Get unread count", description = "Returns the number of unread announcements for the current employee")
    public ApiResponse<UnreadCountResponse> unreadCount() {
        return ApiResponse.success("Unread count loaded", announcementService.getUnreadCount());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('EMPLOYEE', 'STAFF', 'ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Get announcement detail", description = "Returns the full content of an announcement")
    public ApiResponse<MobileAnnouncementDetail> detail(@PathVariable UUID id) {
        return ApiResponse.success("Announcement loaded", announcementService.getDetail(id));
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('EMPLOYEE', 'STAFF', 'ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Mark as read", description = "Marks an announcement as read for the current employee")
    public ApiResponse<Void> markAsRead(@PathVariable UUID id) {
        announcementService.markAsRead(id);
        return ApiResponse.success("Announcement marked as read", null);
    }
}
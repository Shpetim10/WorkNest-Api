package com.worknest.features.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

public record StaffDashboardResponse(
        @Schema(description = "Welcome header data")
        Header header,

        @Schema(description = "Today's attendance summary for the staff member")
        MyAttendance myAttendance,

        @Schema(description = "Leave summary for the staff member")
        MyLeave myLeave,

        @Schema(description = "Recent company announcements")
        List<AnnouncementItem> recentAnnouncements
) {

    public record Header(
            @Schema(description = "Staff member's display name for the welcome banner")
            String displayName,

            @Schema(description = "Server-formatted current time, e.g. '9:30 AM'")
            String currentTimeLabel,

            @Schema(description = "Server-formatted current date, e.g. 'Wed, May 28'")
            String currentDateLabel
    ) {}

    public record MyAttendance(
            @Schema(description = "Today's check-in time label, null if not clocked in")
            String checkInTime,

            @Schema(description = "Today's check-out time label, null if still checked in")
            String checkOutTime,

            @Schema(description = "Today's attendance status", allowableValues = {"CHECKED_IN", "CHECKED_OUT", "ABSENT", "ON_LEAVE"})
            String status,

            @Schema(description = "Hours worked so far today as a float, null if not clocked in")
            Double hoursWorkedToday,

            @Schema(description = "Total hours worked this week as a float")
            Double hoursWorkedThisWeek
    ) {}

    public record MyLeave(
            @Schema(description = "Number of own pending leave requests")
            Integer pendingRequests,

            @Schema(description = "Days of approved leave this month")
            Integer approvedThisMonth,

            @Schema(description = "Remaining vacation leave days for the year")
            Integer remainingDays
    ) {}

    public record AnnouncementItem(
            @Schema(description = "Announcement ID")
            UUID id,

            @Schema(description = "Announcement title")
            String title,

            @Schema(description = "Short preview of body text")
            String body,

            @Schema(description = "Human-readable time label, e.g. '2 hours ago'")
            String publishedAtLabel
    ) {}
}

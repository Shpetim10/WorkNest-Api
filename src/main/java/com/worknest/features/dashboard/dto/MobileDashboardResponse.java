package com.worknest.features.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

public record MobileDashboardResponse(
        @Schema(description = "Today's check-in time formatted as HH:mm or null if not checked in")
        String checkInTime,

        @Schema(description = "The 2 leave types with most recent activity")
        List<LeaveBalanceSummary> leaveBalances,

        @Schema(description = "Latest payroll month, e.g. 'March 2026' or null if none")
        String latestPayrollMonth,

        @Schema(description = "Latest payroll net pay amount or null if none")
        BigDecimal latestPayrollNetPay,

        @Schema(description = "Latest payroll ISO 4217 currency code or null if none")
        String latestPayrollCurrency,

        @Schema(description = "Number of unread announcements for the current employee")
        int announcementUnreadCount,

        @Schema(description = "Latest announcement title or null if none")
        String latestAnnouncementTitle
) {
    public record LeaveBalanceSummary(
            @Schema(description = "Leave type name")
            String leaveType,

            @Schema(description = "Remaining available days for this leave type")
            int remainingDays
    ) {}
}

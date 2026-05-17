package com.worknest.features.dashboard.dto;

import java.util.List;

public record AdminDashboardResponse(
        Header header,
        Kpis kpis,
        List<AttendanceTrendPoint> attendanceTrend,
        List<ActiveDayPoint> activeDays,
        List<ActivityItem> recentActivity,
        List<QuickStat> quickStats
) {

    public record Header(String displayName, String currentTimeLabel, String currentDateLabel) {}

    public record Kpis(
            long totalEmployees,
            long presentToday,
            long onLeaveToday,
            long pendingRequests
    ) {}

    public record AttendanceTrendPoint(String label, long count, double percentage) {}

    public record ActiveDayPoint(String label, long count, double percentage) {}

    public record ActivityItem(
            String id,
            String actorName,
            String description,
            String tag,
            String occurredAtLabel
    ) {}

    public record QuickStat(String id, String label, String valueLabel, double percentage) {}
}

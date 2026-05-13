package com.worknest.features.superAdmin.dto;

import java.util.List;

public record SuperAdminDashboardResponse(
        Header header,
        Kpis kpis,
        List<RegistrationPoint> companyRegistrations,
        List<SubscriptionPlanBreakdown> subscriptionPlans,
        List<ActivityItem> recentActivity,
        List<QuickStat> quickStats
) {

    public record Header(
            String displayName,
            String currentTimeLabel,
            String currentDateLabel
    ) {}

    public record Kpis(
            long totalCompanies,
            long activeCompanies,
            long suspendedCompanies,
            long expiringSoon
    ) {}

    public record RegistrationPoint(
            String label,
            long count,
            double percentage
    ) {}

    public record SubscriptionPlanBreakdown(
            String planId,
            String label,
            long companyCount,
            double percentage
    ) {}

    public record ActivityItem(
            String id,
            String actorName,
            String description,
            String occurredAtLabel
    ) {}

    public record QuickStat(
            String id,
            String label,
            String valueLabel,
            double percentage
    ) {}
}
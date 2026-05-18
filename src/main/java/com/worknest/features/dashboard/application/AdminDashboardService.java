package com.worknest.features.dashboard.application;

import com.worknest.features.dashboard.dto.AdminDashboardResponse;

public interface AdminDashboardService {

    default AdminDashboardResponse getDashboard(String period, String trendPeriod) {
        return getDashboard(period, trendPeriod, null, null);
    }

    AdminDashboardResponse getDashboard(String period, String trendPeriod, String startDate, String endDate);
}

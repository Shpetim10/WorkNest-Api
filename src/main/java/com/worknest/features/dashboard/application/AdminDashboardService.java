package com.worknest.features.dashboard.application;

import com.worknest.features.dashboard.dto.AdminDashboardResponse;

public interface AdminDashboardService {

    AdminDashboardResponse getDashboard(String period, String trendPeriod);
}

package com.worknest.features.superAdmin.application;

import com.worknest.features.superAdmin.dto.SuperAdminDashboardResponse;

public interface SuperAdminDashboardService {

    default SuperAdminDashboardResponse getDashboard(int year, String period) {
        return getDashboard(year, period, null, null, null);
    }

    SuperAdminDashboardResponse getDashboard(int year, String period, String startDate, String endDate, String section);
}

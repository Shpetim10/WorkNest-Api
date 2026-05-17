package com.worknest.features.superAdmin.application;

import com.worknest.features.superAdmin.dto.SuperAdminDashboardResponse;

public interface SuperAdminDashboardService {

    SuperAdminDashboardResponse getDashboard(int year, String period);
}
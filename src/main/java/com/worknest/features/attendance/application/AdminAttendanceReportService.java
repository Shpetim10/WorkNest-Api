package com.worknest.features.attendance.application;

import com.worknest.features.attendance.dto.AdminAttendanceMonthlyReportResponse;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface AdminAttendanceReportService {

    AdminAttendanceMonthlyReportResponse monthly(UUID companyId, int year, int month, UUID siteId, Pageable pageable);
}

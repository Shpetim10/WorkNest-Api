package com.worknest.features.attendance.dto;

import com.worknest.domain.enums.AttendanceDayStatus;
import com.worknest.domain.enums.AttendanceReviewStatus;
import com.worknest.domain.enums.AttendanceState;
import java.time.Instant;
import java.util.UUID;

public record AttendanceDashboardRowDto(
        UUID dayRecordId,
        UUID employeeId,
        UUID userId,
        String employeeName,
        UUID departmentId,
        String departmentName,
        UUID siteId,
        String siteName,
        AttendanceState attendanceState,
        AttendanceDayStatus dayStatus,
        Instant firstCheckInAt,
        Instant lastCheckOutAt,
        int workedMinutes,
        int lateMinutes,
        boolean hasWarnings,
        AttendanceReviewStatus reviewStatus,
        boolean payrollLocked
) {
}

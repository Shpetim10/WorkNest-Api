package com.worknest.features.attendance.dto;

import com.worknest.domain.enums.AttendanceDayStatus;
import com.worknest.domain.enums.AttendanceReviewStatus;
import com.worknest.domain.enums.AttendanceState;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record EmployeeAttendanceDayDetailDto(
        UUID dayRecordId,
        UUID employeeId,
        UUID userId,
        String employeeName,
        String departmentName,
        String siteName,
        LocalDate workDate,
        String timezone,
        AttendanceDayStatus dayStatus,
        AttendanceState attendanceState,
        Instant firstCheckInAt,
        Instant lastCheckOutAt,
        int workedMinutes,
        int lateMinutes,
        int earlyLeaveMinutes,
        int overtimeMinutes,
        int breakMinutes,
        boolean hasWarnings,
        List<String> warningFlags,
        AttendanceReviewStatus reviewStatus,
        boolean payrollLocked,
        List<AttendanceEventDetailDto> events
) {
}

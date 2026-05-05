package com.worknest.features.leave.dto;

import com.worknest.domain.enums.LeaveStatus;
import com.worknest.domain.enums.LeaveType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LeaveRequestDto(
        UUID id,
        UUID employeeId,
        String employeeName,
        String siteName,
        String departmentName,
        LeaveType leaveType,
        LocalDate startDate,
        LocalDate endDate,
        int totalDays,
        LeaveStatus status,
        String note,
        String rejectionReason,
        Instant reviewedAt,
        Instant createdAt
) {
}
package com.worknest.features.leave.dto;

import com.worknest.domain.enums.LeaveType;

public record LeaveBalanceDto(
        LeaveType leaveType,
        int totalDays,
        int usedDays,
        int availableDays
) {
}
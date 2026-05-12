package com.worknest.features.leave.dto;

import com.worknest.domain.enums.LeaveType;
import java.math.BigDecimal;

public record LeaveBalanceDto(
        LeaveType leaveType,
        BigDecimal totalDays,
        BigDecimal usedDays,
        BigDecimal availableDays
) {
}

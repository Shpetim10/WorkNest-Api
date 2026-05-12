package com.worknest.realtime.event;

import com.worknest.domain.enums.LeaveType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LeaveRequestSubmittedDomainEvent(
        UUID companyId,
        UUID leaveRequestId,
        UUID employeeId,
        UUID employeeUserId,
        LeaveType leaveType,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal daysCount
) {}

package com.worknest.realtime.event;

import com.worknest.domain.enums.LeaveType;

import java.util.UUID;

public record LeaveRequestApprovedDomainEvent(
        UUID companyId,
        UUID leaveRequestId,
        UUID employeeId,
        UUID employeeUserId,
        UUID actorUserId,
        LeaveType leaveType
) {}

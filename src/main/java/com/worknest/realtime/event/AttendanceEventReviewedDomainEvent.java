package com.worknest.realtime.event;

import java.util.UUID;

public record AttendanceEventReviewedDomainEvent(
        UUID companyId,
        UUID eventId,
        UUID employeeId,
        UUID employeeUserId,
        UUID actorUserId,
        String reviewStatus
) {}

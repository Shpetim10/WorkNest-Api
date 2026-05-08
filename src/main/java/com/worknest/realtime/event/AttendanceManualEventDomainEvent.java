package com.worknest.realtime.event;

import java.time.Instant;
import java.util.UUID;

public record AttendanceManualEventDomainEvent(
        UUID companyId,
        UUID employeeId,
        UUID actorUserId,
        String realtimeEventType,
        Instant occurredAt
) {}

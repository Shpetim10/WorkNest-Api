package com.worknest.realtime.event;

import java.time.LocalDate;
import java.util.UUID;

public record AttendanceDayAdjustedDomainEvent(
        UUID companyId,
        UUID recordId,
        UUID employeeId,
        UUID actorUserId,
        LocalDate workDate
) {}

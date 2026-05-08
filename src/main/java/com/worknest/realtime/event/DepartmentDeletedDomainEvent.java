package com.worknest.realtime.event;

import java.util.UUID;

public record DepartmentDeletedDomainEvent(
        UUID companyId,
        UUID departmentId,
        UUID actorUserId,
        String name
) {}

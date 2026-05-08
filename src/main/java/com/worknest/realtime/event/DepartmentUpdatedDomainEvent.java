package com.worknest.realtime.event;

import com.worknest.features.department.dto.DepartmentResponse;

import java.util.UUID;

public record DepartmentUpdatedDomainEvent(
        UUID companyId,
        UUID departmentId,
        UUID actorUserId,
        DepartmentResponse snapshot
) {}

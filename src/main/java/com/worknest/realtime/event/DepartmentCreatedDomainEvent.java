package com.worknest.realtime.event;

import com.worknest.features.department.dto.DepartmentResponse;

import java.util.UUID;

public record DepartmentCreatedDomainEvent(
        UUID companyId,
        UUID departmentId,
        UUID actorUserId,
        DepartmentResponse snapshot
) {}

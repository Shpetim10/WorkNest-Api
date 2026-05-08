package com.worknest.realtime.event;

import com.worknest.domain.enums.PlatformRole;

import java.util.UUID;

public record EmployeeProvisionedDomainEvent(
        UUID companyId,
        UUID employeeId,
        UUID actorUserId,
        PlatformRole role,
        String email
) {}

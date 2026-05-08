package com.worknest.realtime.event;

import com.worknest.features.company.dto.CompanySettingsResponse;

import java.util.UUID;

public record CompanySettingsUpdatedDomainEvent(
        UUID companyId,
        UUID actorUserId,
        CompanySettingsResponse snapshot
) {}

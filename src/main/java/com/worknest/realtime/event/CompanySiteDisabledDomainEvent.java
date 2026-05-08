package com.worknest.realtime.event;

import com.worknest.features.companySite.dto.CompanySiteResponse;

import java.util.UUID;

public record CompanySiteDisabledDomainEvent(
        UUID companyId,
        UUID siteId,
        UUID actorUserId,
        long version,
        CompanySiteResponse snapshot
) {}

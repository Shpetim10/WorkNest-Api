package com.worknest.realtime.event;

import java.util.UUID;

public record AnnouncementDeletedDomainEvent(
        UUID companyId,
        UUID announcementId,
        UUID actorUserId
) {}

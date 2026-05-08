package com.worknest.realtime.event;

import com.worknest.features.announcement.dto.AnnouncementListResponse;

import java.util.UUID;

public record AnnouncementCreatedDomainEvent(
        UUID companyId,
        UUID announcementId,
        UUID actorUserId,
        AnnouncementListResponse snapshot
) {}

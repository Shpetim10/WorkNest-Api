package com.worknest.features.notification.dto;

import com.worknest.domain.enums.NotificationTargetType;
import com.worknest.domain.enums.NotificationType;
import java.time.Instant;
import java.util.UUID;

public class NotificationDtos {

    public record NotificationResponse(
            UUID id,
            NotificationType type,
            String title,
            String message,
            UUID targetId,
            NotificationTargetType targetType,
            boolean read,
            Instant createdAt
    ) {}

    public record UnreadCountResponse(
            long count
    ) {}
}

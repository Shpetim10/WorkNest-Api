package com.worknest.features.announcement.dto;

import com.worknest.domain.enums.AnnouncementPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record MobileAnnouncementDetail(
        @Schema(description = "Announcement ID")
        UUID id,

        @Schema(description = "Announcement title")
        String title,

        @Schema(description = "Full announcement content")
        String content,

        @Schema(description = "Priority level")
        AnnouncementPriority priority,

        @Schema(description = "Creation timestamp")
        Instant createdAt,

        @Schema(description = "Whether the current employee has read this announcement")
        boolean read
) {
}
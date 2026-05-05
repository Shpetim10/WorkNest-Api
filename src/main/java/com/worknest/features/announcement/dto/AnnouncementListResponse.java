package com.worknest.features.announcement.dto;

import com.worknest.domain.enums.AnnouncementAudience;
import com.worknest.domain.enums.AnnouncementPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record AnnouncementListResponse(
        @Schema(description = "Announcement ID")
        UUID id,

        @Schema(description = "Announcement title")
        String title,

        @Schema(description = "Announcement content")
        String content,

        @Schema(description = "Target audience type")
        AnnouncementAudience targetAudience,

        @Schema(description = "Priority level")
        AnnouncementPriority priority,

        @Schema(description = "Display name of the author")
        String createdByName,

        @Schema(description = "Creation timestamp")
        Instant createdAt
) {
}
package com.worknest.features.announcement.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record UnreadCountResponse(
        @Schema(description = "Number of unread announcements for the current employee")
        long count
) {
}
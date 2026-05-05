package com.worknest.features.announcement.dto;

import com.worknest.domain.enums.AnnouncementAudience;
import com.worknest.domain.enums.AnnouncementPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateAnnouncementRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        @Schema(description = "Announcement title", example = "Office Closure - Public Holiday")
        String title,

        @NotBlank(message = "Content is required")
        @Schema(description = "Announcement content", example = "The office will be closed on April 10th.")
        String content,

        @NotNull(message = "Target audience is required")
        @Schema(description = "Who should receive this announcement")
        AnnouncementAudience targetAudience,

        @Schema(description = "Department IDs to target (required when targetAudience is DEPARTMENT)")
        List<UUID> targetDepartmentIds,

        @Schema(description = "Employee IDs to target (required when targetAudience is SPECIFIC_USERS)")
        List<UUID> targetEmployeeIds,

        @Schema(description = "Priority level, defaults to NORMAL")
        AnnouncementPriority priority
) {
    public AnnouncementPriority priorityOrDefault() {
        return priority != null ? priority : AnnouncementPriority.NORMAL;
    }
}
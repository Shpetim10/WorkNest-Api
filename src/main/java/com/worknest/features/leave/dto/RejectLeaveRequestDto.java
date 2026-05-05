package com.worknest.features.leave.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectLeaveRequestDto(
        @NotBlank(message = "Rejection reason is required")
        @Size(max = 500, message = "Reason cannot exceed 500 characters")
        String reason
) {
}
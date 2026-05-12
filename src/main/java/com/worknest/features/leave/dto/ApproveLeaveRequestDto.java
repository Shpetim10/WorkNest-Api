package com.worknest.features.leave.dto;

import jakarta.validation.constraints.Size;

public record ApproveLeaveRequestDto(
        @Size(max = 500, message = "Approval note cannot exceed 500 characters")
        String note
) {
}

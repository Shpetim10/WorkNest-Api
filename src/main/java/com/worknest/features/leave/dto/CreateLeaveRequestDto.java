package com.worknest.features.leave.dto;

import com.worknest.domain.enums.LeaveType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateLeaveRequestDto(
        @NotNull(message = "Leave type is required")
        LeaveType leaveType,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate,

        @Size(max = 500, message = "Note cannot exceed 500 characters")
        String note,

        @Size(max = 500, message = "Medical report document ID cannot exceed 500 characters")
        String medicalReportDocumentId
) {
}
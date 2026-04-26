package com.worknest.features.attendance.dto;

import com.worknest.domain.enums.AttendanceDayStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record AdjustAttendanceDayRecordRequest(
        Instant firstCheckInAt,
        Instant lastCheckOutAt,
        Integer workedMinutes,
        @NotNull AttendanceDayStatus dayStatus,
        @Size(max = 400) String reason
) {
}

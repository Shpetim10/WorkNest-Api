package com.worknest.features.attendance.application;

import com.worknest.domain.entities.AttendancePolicy;
import com.worknest.features.attendance.dto.EffectiveAttendancePolicyDto;

public record ResolvedAttendancePolicy(
        AttendancePolicy entity,
        EffectiveAttendancePolicyDto dto
) {
}

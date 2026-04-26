package com.worknest.features.attendance.dto;

import java.util.UUID;

public record CompanyAttendancePolicyResponse(
        UUID companyId,
        EffectiveAttendancePolicyDto policy
) {
}

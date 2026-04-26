package com.worknest.features.attendance.dto;

import java.util.UUID;

public record SiteAttendancePolicyResponse(
        UUID companyId,
        UUID siteId,
        EffectiveAttendancePolicyDto policy
) {
}

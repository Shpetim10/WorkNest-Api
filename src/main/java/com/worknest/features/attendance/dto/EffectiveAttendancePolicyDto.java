package com.worknest.features.attendance.dto;

import com.worknest.domain.enums.AttendancePolicySource;
import java.util.UUID;

public record EffectiveAttendancePolicyDto(
        UUID policyId,
        AttendancePolicySource policySource,
        boolean requireQr,
        boolean requireLocation,
        boolean checkInEnabled,
        boolean checkOutEnabled,
        boolean useNetworkAsWarning,
        boolean rejectOutsideGeofence,
        boolean rejectPoorAccuracy,
        boolean allowManualCorrection,
        boolean allowManagerManualEntry
) {
}

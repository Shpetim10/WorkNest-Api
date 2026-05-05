package com.worknest.features.attendance.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SiteAttendancePolicyRequest(
        @NotNull Boolean requireQr,
        @NotNull Boolean requireLocation,
        @NotNull Boolean checkInEnabled,
        @NotNull Boolean checkOutEnabled,
        @NotNull Boolean useNetworkAsWarning,
        @NotNull Boolean rejectOutsideGeofence,
        @NotNull Boolean rejectPoorAccuracy,
        @NotNull Boolean allowManualCorrection,
        @NotNull Boolean allowManagerManualEntry
) {
}

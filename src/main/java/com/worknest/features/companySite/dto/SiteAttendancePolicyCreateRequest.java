package com.worknest.features.companySite.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SiteAttendancePolicyCreateRequest(
        @NotNull Boolean requireQr,
        @NotNull Boolean requireLocation,
        @NotNull Boolean checkInEnabled,
        @NotNull Boolean checkOutEnabled,
        @NotNull Boolean useNetworkAsWarning,
        @NotNull Boolean rejectOutsideGeofence,
        @NotNull Boolean rejectPoorAccuracy,
        @NotNull Boolean allowManualCorrection,
        @NotNull Boolean allowManagerManualEntry,
        @NotNull Boolean missingCheckoutAutoCloseEnabled,
        @Min(1) @Max(1440) Integer autoCheckoutAfterMinutes,
        @NotNull @Min(0) @Max(300) Integer lateGraceMinutes,
        @NotNull @Min(0) @Max(300) Integer earlyClockInWindowMinutes
) {
}

package com.worknest.features.companySite.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SiteAttendancePolicyCreateRequest(
        @NotNull(message = "Choose whether QR is required for attendance.")
        Boolean requireQr,
        @NotNull(message = "Choose whether location is required for attendance.")
        Boolean requireLocation,
        @NotNull(message = "Choose whether check-in is enabled.")
        Boolean checkInEnabled,
        @NotNull(message = "Choose whether check-out is enabled.")
        Boolean checkOutEnabled,
        @NotNull(message = "Choose whether unmatched networks should be treated as a warning.")
        Boolean useNetworkAsWarning,
        @NotNull(message = "Choose whether attendance outside the geofence should be rejected.")
        Boolean rejectOutsideGeofence,
        @NotNull(message = "Choose whether poor GPS accuracy should be rejected.")
        Boolean rejectPoorAccuracy,
        @NotNull(message = "Choose whether employees can request manual corrections.")
        Boolean allowManualCorrection,
        @NotNull(message = "Choose whether managers can add manual entries.")
        Boolean allowManagerManualEntry,
        @NotNull(message = "Choose whether missing check-outs should be auto-closed.")
        Boolean missingCheckoutAutoCloseEnabled,
        @Min(value = 1, message = "Auto check-out minutes must be at least 1.")
        @Max(value = 1440, message = "Auto check-out minutes must be 1440 or less.")
        Integer autoCheckoutAfterMinutes,
        @NotNull(message = "Late grace minutes is required.")
        @Min(value = 0, message = "Late grace minutes cannot be negative.")
        @Max(value = 300, message = "Late grace minutes must be 300 or less.")
        Integer lateGraceMinutes,
        @NotNull(message = "Early clock-in window minutes is required.")
        @Min(value = 0, message = "Early clock-in window minutes cannot be negative.")
        @Max(value = 300, message = "Early clock-in window minutes must be 300 or less.")
        Integer earlyClockInWindowMinutes
) {
}

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
        Boolean allowManagerManualEntry
) {
}

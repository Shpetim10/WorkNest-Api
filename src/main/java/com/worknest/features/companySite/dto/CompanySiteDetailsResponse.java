package com.worknest.features.companySite.dto;

import java.util.List;

public record CompanySiteDetailsResponse(
        CompanySiteResponse site,
        String countryName,
        List<TrustedNetworkResponse> trustedNetworks,
        SiteAttendancePolicySummaryResponse attendancePolicy,
        List<LinkedQrTerminalResponse> linkedQrTerminals
) {
}

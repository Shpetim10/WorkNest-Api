package com.worknest.domain.enums;

public enum AttendanceWarningCode {
    LOW_LOCATION_ACCURACY("LOW"),
    LOCATION_NEAR_BOUNDARY("MEDIUM"),
    NETWORK_NOT_CONFIGURED("LOW"),
    NETWORK_NOT_MATCHED("MEDIUM"),
    OUTSIDE_OFFICE_NETWORK("HIGH"),
    CLIENT_TIME_SKEW("MEDIUM"),
    POSSIBLE_VPN_OR_PROXY("HIGH"),
    MANUAL_ENTRY("MEDIUM"),
    MULTI_SITE_AMBIGUITY("LOW");

    private final String severity;

    AttendanceWarningCode(String severity) {
        this.severity = severity;
    }

    public String getSeverity() {
        return severity;
    }
}

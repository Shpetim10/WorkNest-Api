package com.worknest.features.companySite.dto;

import com.worknest.domain.enums.AttendanceQrTerminalStatus;
import java.time.Instant;
import java.util.UUID;

public record LinkedQrTerminalResponse(
        UUID id,
        String name,
        AttendanceQrTerminalStatus status,
        int rotationSeconds,
        boolean autoCreated,
        Instant lastHeartbeatAt
) {
}

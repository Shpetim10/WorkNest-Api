package com.worknest.features.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record QrValidateRequest(
        @NotNull UUID siteId,
        @NotBlank String qrToken
) {
}

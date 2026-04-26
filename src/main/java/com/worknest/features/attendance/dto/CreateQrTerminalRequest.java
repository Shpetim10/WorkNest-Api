package com.worknest.features.attendance.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateQrTerminalRequest(
        @NotBlank @Size(max = 120) String name,
        @Min(30) @Max(300) Integer rotationSeconds
) {
}

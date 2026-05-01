package com.worknest.features.attendance.dto;

import jakarta.validation.constraints.Size;

public record DismissWarningsRequest(
        @Size(max = 400) String note
) {
}

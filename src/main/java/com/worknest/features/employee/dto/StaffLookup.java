package com.worknest.features.employee.dto;

import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lightweight staff projection for dropdowns")
public record StaffLookup(
        @Schema(description = "Employee ID")
        UUID id,

        @Schema(description = "Full name of the staff member")
        String fullName
) {}

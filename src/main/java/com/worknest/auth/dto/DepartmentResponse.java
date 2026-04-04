package com.worknest.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Response containing department details")
public record DepartmentResponse(
        @Schema(description = "Unique identifier of the department")
        UUID id,

        @Schema(description = "Internal unique code for the department", example = "ENG-AUTO")
        String code,

        @Schema(description = "Display name of the department", example = "Automotive Engineering")
        String name,

        @Schema(description = "Brief description of the department's purpose")
        String description,

        @Schema(description = "Whether the department is currently active")
        Boolean isActive
) {
}

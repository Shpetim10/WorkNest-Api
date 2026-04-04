package com.worknest.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to create or update a department")
public record DepartmentRequest(
        @NotBlank
        @Size(max = 50)
        @Schema(description = "Internal unique code for the department", example = "ENG-AUTO")
        String code,

        @NotBlank
        @Size(max = 150)
        @Schema(description = "Display name of the department", example = "Automotive Engineering")
        String name,

        @Size(max = 500)
        @Schema(description = "Brief description of the department's purpose")
        String description
) {
}

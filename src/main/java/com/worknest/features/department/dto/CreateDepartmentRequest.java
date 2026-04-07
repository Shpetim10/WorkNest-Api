package com.worknest.features.department.dto;

import com.worknest.domain.enums.DepartmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDepartmentRequest(
        @NotBlank(message = "Department name is required")
        @Size(max = 255, message = "Department name must not exceed 255 characters")
        @Schema(description = "Name of the department", example = "Engineering")
        String name,

        @Schema(description = "Description of the department", example = "Software engineering team")
        String description,

        @Schema(description = "Status of the department", example = "ACTIVE")
        DepartmentStatus status
) {
    public DepartmentStatus statusOrDefault() {
        return status != null ? status : DepartmentStatus.ACTIVE;
    }
}

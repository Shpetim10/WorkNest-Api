package com.worknest.features.department.dto;

import com.worknest.domain.entities.Department;
import com.worknest.domain.enums.DepartmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record DepartmentResponse(
        @Schema(description = "Department ID")
        UUID id,

        @Schema(description = "Company ID")
        UUID companyId,

        @Schema(description = "Department name")
        String name,

        @Schema(description = "Department description")
        String description,

        @Schema(description = "Department status")
        DepartmentStatus status,

        @Schema(description = "Creation timestamp")
        Instant createdAt,

        @Schema(description = "Last update timestamp")
        Instant updatedAt
) {
    public static DepartmentResponse fromEntity(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getCompany().getId(),
                department.getName(),
                department.getDescription(),
                department.getStatus(),
                department.getCreatedAt(),
                department.getUpdatedAt()
        );
    }
}

package com.worknest.features.department.dto;

import com.worknest.domain.entities.Department;
import com.worknest.domain.enums.DepartmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record DepartmentListResponse(
        @Schema(description = "Department ID")
        UUID id,

        @Schema(description = "Department name")
        String name,

        @Schema(description = "Department description")
        String description,

        @Schema(description = "Department status")
        DepartmentStatus status,

        @Schema(description = "Number of employees in the department")
        Integer employeeCount,
        
        @Schema(description = "Creation timestamp")
        Instant createdAt,

        @Schema(description = "Last update timestamp")
        Instant updatedAt
) {
    public static DepartmentListResponse fromEntity(Department department, int employeeCount) {
        return new DepartmentListResponse(
                department.getId(),
                department.getName(),
                department.getDescription(),
                department.getStatus(),
                employeeCount,
                department.getCreatedAt(),
                department.getUpdatedAt()
        );
    }
}

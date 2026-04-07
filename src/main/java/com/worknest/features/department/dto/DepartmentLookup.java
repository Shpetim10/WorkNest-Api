package com.worknest.features.department.dto;

import com.worknest.domain.entities.Department;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record DepartmentLookup(
        @Schema(description = "Department ID")
        UUID id,

        @Schema(description = "Department name")
        String name
) {
    public static DepartmentLookup fromEntity(Department department) {
        return new DepartmentLookup(
                department.getId(),
                department.getName()
        );
    }
}

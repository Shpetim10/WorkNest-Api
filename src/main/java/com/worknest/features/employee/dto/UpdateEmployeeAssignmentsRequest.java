package com.worknest.features.employee.dto;

import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateEmployeeAssignmentsRequest(
    @NotNull
    @Schema(description = "The updated complete list of employee IDs that should be assigned to this manager")
    List<UUID> assignedEmployeeIds
) {}

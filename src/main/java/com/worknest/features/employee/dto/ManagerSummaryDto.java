package com.worknest.features.employee.dto;

import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Summary of a STAFF user eligible to be a manager")
public record ManagerSummaryDto(
    UUID roleAssignmentId,
    UUID userId,
    String firstName,
    String lastName,
    String email,
    String jobTitle
) {}

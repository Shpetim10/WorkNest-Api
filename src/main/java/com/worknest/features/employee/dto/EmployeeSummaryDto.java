package com.worknest.features.employee.dto;

import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

public record EmployeeSummaryDto(
    UUID employeeId,
    UUID userId,
    String firstName,
    String lastName,
    String email,
    String departmentName
) {}

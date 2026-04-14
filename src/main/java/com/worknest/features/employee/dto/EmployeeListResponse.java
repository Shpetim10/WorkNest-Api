package com.worknest.features.employee.dto;

import java.util.UUID;
import java.time.LocalDate;
import com.worknest.domain.enums.EmploymentStatus;

public record EmployeeListResponse(
        UUID id,
        UUID userId,
        String firstName,
        String lastName,
        String name,
        String email,
        String departmentName,
        UUID departmentId,
        String jobTitle,
        String companySiteName,
        UUID companySiteId,
        LocalDate hireDate,
        EmploymentStatus status
) {}

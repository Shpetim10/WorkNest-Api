package com.worknest.features.employee.dto;

import com.worknest.domain.enums.EmploymentStatus;
import java.time.LocalDate;
import java.util.UUID;

public record EmployeeDetailsResponse(
        UUID id,
        UUID userId,
        String firstName,
        String lastName,
        String email,
        String departmentName,
        UUID departmentId,
        String jobTitle,
        String companySiteName,
        UUID companySiteId,
        LocalDate hireDate,
        EmploymentStatus status,
        UUID supervisorRoleAssignmentId,
        String supervisorName,
        String supervisorJobTitle
) {}

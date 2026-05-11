package com.worknest.features.employee.dto;

import java.util.UUID;
import java.time.LocalDate;
import com.worknest.domain.enums.EmploymentStatus;
import com.worknest.domain.enums.EmploymentType;
import com.worknest.domain.enums.PlatformRole;

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
        PlatformRole platformRole,
        EmploymentType employmentType,
        LocalDate hireDate,
        EmploymentStatus status
) {}

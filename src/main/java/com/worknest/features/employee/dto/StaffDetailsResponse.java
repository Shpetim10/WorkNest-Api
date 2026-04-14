package com.worknest.features.employee.dto;

import com.worknest.domain.enums.PlatformRole;
import com.worknest.domain.enums.EmploymentStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record StaffDetailsResponse(
        UUID id,
        UUID userId,
        String firstName,
        String lastName,
        String email,
        String jobTitle,
        String departmentName,
        UUID departmentId,
        String companySiteName,
        UUID companySiteId,
        PlatformRole role,
        LocalDate startDate,
        EmploymentStatus status,
        List<String> permissionCodes,
        long assignedEmployeesCount,
        List<EmployeeSummaryDto> assignedEmployees
) {}

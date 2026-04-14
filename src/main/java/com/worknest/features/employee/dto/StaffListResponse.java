package com.worknest.features.employee.dto;

import java.util.UUID;
import java.time.LocalDate;
import com.worknest.domain.enums.PlatformRole;
import com.worknest.domain.enums.EmploymentStatus;

public record StaffListResponse(
        UUID id,
        UUID roleAssignmentId,
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
        long assignedEmployeesCount,
        java.util.List<String> permissionCodes
) {}

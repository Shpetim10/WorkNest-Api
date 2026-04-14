package com.worknest.features.employee.dto;

import com.worknest.domain.enums.EmploymentStatus;
import com.worknest.domain.enums.PlatformRole;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Unified response returned after a successful employee or staff update.
 */
public record UpdateEmployeeResponse(

        UUID employeeId,
        UUID userId,
        UUID roleAssignmentId,

        String firstName,
        String lastName,
        String email,
        String jobTitle,

        PlatformRole role,
        EmploymentStatus status,

        UUID departmentId,
        String departmentName,

        UUID companySiteId,
        String companySiteName,

        /** Present only for EMPLOYEE records — the supervising staff role-assignment. */
        UUID supervisorRoleAssignmentId,

        LocalDate startDate,

        String message

) {}

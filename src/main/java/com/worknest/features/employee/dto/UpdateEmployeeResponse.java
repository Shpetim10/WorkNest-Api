package com.worknest.features.employee.dto;

import com.worknest.domain.enums.EmploymentStatus;
import com.worknest.domain.enums.EmploymentType;
import com.worknest.domain.enums.PaymentMethod;
import com.worknest.domain.enums.PlatformRole;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Unified response returned after any successful employee or staff update.
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

        /** Present only for EMPLOYEE records. */
        UUID supervisorRoleAssignmentId,

        LocalDate startDate,

        // Job & contract details
        EmploymentType employmentType,
        String contractDocumentKey,
        LocalDate contractExpiryDate,
        Integer leaveDaysPerYear,
        PaymentMethod paymentMethod,
        BigDecimal monthlySalary,
        BigDecimal hourlyRate,

        String message

) {}

package com.worknest.features.employee.dto;

import com.worknest.domain.enums.EmploymentStatus;
import com.worknest.domain.enums.EmploymentType;
import com.worknest.domain.enums.PaymentMethod;
import com.worknest.domain.enums.PlatformRole;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
        String supervisorJobTitle,
        PlatformRole role,
        List<String> permissionCodes,

        // Job & contract details
        EmploymentType employmentType,
        String contractDocumentKey,
        String contractDocumentPath,
        LocalDate contractExpiryDate,
        Integer leaveDaysPerYear,
        PaymentMethod paymentMethod,
        BigDecimal monthlySalary,
        BigDecimal hourlyRate,
        BigDecimal overtimeHourlyRate,
        BigDecimal dailyWorkingHours
) {}

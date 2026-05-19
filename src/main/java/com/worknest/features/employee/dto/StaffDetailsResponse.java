package com.worknest.features.employee.dto;

import com.worknest.domain.enums.EmploymentStatus;
import com.worknest.domain.enums.EmploymentType;
import com.worknest.domain.enums.PaymentMethod;
import com.worknest.domain.enums.PlatformRole;

import java.math.BigDecimal;
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
        List<EmployeeSummaryDto> assignedEmployees,

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

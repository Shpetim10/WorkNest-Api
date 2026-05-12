package com.worknest.features.employee.dto;

import com.worknest.domain.enums.EmploymentType;
import com.worknest.domain.enums.PaymentMethod;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateEmployeeRequest(

        @NotNull
        UUID companyId,

        @NotBlank
        String firstName,

        @NotBlank
        String lastName,

        @NotBlank
        @Email
        String email,

        @NotBlank
        String jobTitle,

        @NotNull
        UUID departmentId,

        @Nullable
        UUID companySiteId,

        @Nullable
        UUID supervisorRoleAssignmentId,

        @NotNull
        LocalDate startDate,

        @Nullable
        EmploymentType employmentType,

        @Nullable
        String contractDocumentKey,

        @Nullable
        String contractDocumentPath,

        @Nullable
        LocalDate contractExpiryDate,

        @Nullable
        Integer leaveDaysPerYear,

        @Nullable
        PaymentMethod paymentMethod,

        /** Required when paymentMethod is FIXED_MONTHLY. */
        @Nullable
        BigDecimal monthlySalary,

        /** Required when paymentMethod is HOURLY. */
        @Nullable
        BigDecimal hourlyRate,

        /** Hours worked per day; used to derive daily leave pay for HOURLY employees: hourlyRate × dailyWorkingHours. */
        @Nullable
        BigDecimal dailyWorkingHours

) {}

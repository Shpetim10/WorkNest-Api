package com.worknest.features.employee.dto;

import com.worknest.domain.enums.EmploymentType;
import com.worknest.domain.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request body for updating an EMPLOYEE's contract and payment details.
 * All fields are optional — pass null to clear a value.
 */
public record UpdateEmployeeJobDetailsRequest(

        @NotNull
        UUID companyId,

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

        @Nullable
        BigDecimal monthlySalary,

        @Nullable
        BigDecimal hourlyRate,

        @Nullable
        BigDecimal dailyWorkingHours

) {}

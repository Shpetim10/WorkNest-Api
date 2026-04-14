package com.worknest.features.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.lang.Nullable;

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
        LocalDate startDate

) {}

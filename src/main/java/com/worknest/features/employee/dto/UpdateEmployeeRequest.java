package com.worknest.features.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.lang.Nullable;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request body for updating an existing EMPLOYEE profile.
 *
 * <p>All mutable fields are accepted; omitting an optional field (null) will
 * leave the current value unchanged — callers only need to send what they want
 * to modify.</p>
 */
public record UpdateEmployeeRequest(

        /** The company this employee belongs to — must match the path variable. */
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

        /** Optional — update to a different department within the same company. */
        @Nullable
        UUID departmentId,

        /** Optional — update the physical site/location. */
        @Nullable
        UUID companySiteId,

        /**
         * Optional — UUID of the STAFF role-assignment acting as the new supervisor.
         * Pass null to unassign the current supervisor.
         */
        @Nullable
        UUID supervisorRoleAssignmentId,

        /** Optional — update the official start date. */
        @Nullable
        LocalDate startDate

) {}

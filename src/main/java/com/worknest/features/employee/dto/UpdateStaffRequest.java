package com.worknest.features.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.lang.Nullable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request body for updating an existing STAFF profile.
 *
 * <p>Supports updating personal details, organisational placement, the set of
 * supervised employees, and the permission codes attached to this staff
 * member's role-assignment.  Passing null for a list field will leave the
 * current state untouched; passing an empty list will clear it.</p>
 */
public record UpdateStaffRequest(

        /** The company this staff member belongs to — must match the path variable. */
        @NotNull
        UUID companyId,

        @NotBlank
        String firstName,

        @NotBlank
        String lastName,

        @NotBlank
        String jobTitle,

        /** Optional — move to a different department within the same company. */
        @Nullable
        UUID departmentId,

        /** Optional — move to a different company site. */
        @Nullable
        UUID companySiteId,

        /** Optional — update the official start date. */
        @Nullable
        LocalDate startDate,

        /**
         * Optional — full replacement list of employee IDs to supervise.
         * <ul>
         *   <li>null  → leave current assignments unchanged</li>
         *   <li>empty → remove all supervised employees</li>
         *   <li>list  → replace supervised employees with this set</li>
         * </ul>
         */
        @Nullable
        List<UUID> assignedEmployeeIds,

        /**
         * Optional — full replacement list of permission codes.
         * <ul>
         *   <li>null  → leave current permissions unchanged</li>
         *   <li>empty → revoke all permissions</li>
         *   <li>list  → replace permissions with this set</li>
         * </ul>
         */
        @Nullable
        List<String> permissionCodes

) {}

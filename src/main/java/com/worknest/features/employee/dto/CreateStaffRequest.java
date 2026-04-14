package com.worknest.features.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.lang.Nullable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateStaffRequest(

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

        @Nullable
        UUID departmentId,

        /** Optional — physical office/site location */
        @Nullable
        UUID companySiteId,

        /** Required — the date the staff member starts */
        @Nullable
        LocalDate startDate,

        /**
         * Optional — UUIDs of existing employees to be assigned
         * under this staff member's supervision.
         */
        @Nullable
        List<UUID> assignedEmployeeIds,

        /**
         * Optional — permission codes selected on the Permissions page.
         * Valid codes: users.invite, users.assign_job_title, users.deactivate,
         * attendance.mark, attendance.self_checkin, attendance.edit,
         * attendance.view, attendance.export,
         * employees.create_edit, employees.view_team, employees.view_all,
         * employees.upload_documents, employees.view_contracts,
         * announcements.create, announcements.view
         */
        @Nullable
        List<String> permissionCodes,

        @Nullable
        String preferredLanguage

) {}


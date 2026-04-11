package com.worknest.features.employee.dto;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.lang.Nullable;

public record CreateEmployeeRequest(
        UUID companyId,
        String firstName,
        String lastName,
        String email,
        @Nullable String phoneNumber,
        @Nullable UUID departmentId,
        @Nullable UUID companySiteId,
        @Nullable LocalDate startDate,
        UUID supervisorRoleAssignmentId,
        @Nullable String preferredLanguage
) {}

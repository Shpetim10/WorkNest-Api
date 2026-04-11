package com.worknest.features.employee.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.lang.Nullable;

public record CreateStaffRequest(
        UUID companyId,
        String firstName,
        String lastName,
        String email,
        @Nullable UUID departmentId,
        @Nullable UUID companySiteId,
        @Nullable LocalDate startDate,
        @Nullable List<String> permissionCodes,
        @Nullable String preferredLanguage
) {}

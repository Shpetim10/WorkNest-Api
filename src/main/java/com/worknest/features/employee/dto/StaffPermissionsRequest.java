package com.worknest.features.employee.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record StaffPermissionsRequest(
        @NotNull
        List<String> permissionCodes
) {}

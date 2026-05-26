package com.worknest.features.employee.dto;

import java.util.List;
import java.util.UUID;

public record StaffPermissionsResponse(
        UUID staffId,
        UUID userId,
        UUID roleAssignmentId,
        List<String> permissionCodes
) {}

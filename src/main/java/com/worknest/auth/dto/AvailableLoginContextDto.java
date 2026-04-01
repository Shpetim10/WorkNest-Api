package com.worknest.auth.dto;

import com.worknest.auth.domain.PlatformAccess;
import com.worknest.auth.domain.PlatformRole;
import java.util.UUID;

/**
 * Data Transfer Object representing a selectable role context during login.
 */
public record AvailableLoginContextDto(
        UUID companyId,
        String companyName,
        UUID roleAssignmentId,
        PlatformRole role,
        String jobTitle,
        PlatformAccess platformAccess
) {
}

package com.worknest.features.invitation.application;

import com.worknest.features.invitation.dto.SelectRoleRequest;
import com.worknest.features.invitation.dto.SelectRoleResponse;

public interface RoleSelectionService {
    SelectRoleResponse selectRole(SelectRoleRequest request, String authenticatedEmail, String ipAddress, String userAgent);
}

package com.worknest.auth.service;

import com.worknest.auth.dto.SelectRoleRequest;
import com.worknest.auth.dto.SelectRoleResponse;

public interface RoleSelectionService {

    /**
     * Allows a platform user to select/activate a specific business role (tenancy context).
     *
     * @param request the role selection details containing company and role IDs
     * @param userId the ID of the authenticated user
     * @return the role selection response with a context-specific access token
     */
    SelectRoleResponse selectRole(SelectRoleRequest request, String userId);
}

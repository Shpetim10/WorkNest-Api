package com.worknest.auth.service;

import com.worknest.auth.dto.SelectRoleRequest;
import com.worknest.auth.dto.SelectRoleResponse;

public interface RoleSelectionService {
    SelectRoleResponse selectRole(SelectRoleRequest request, String userId);
}

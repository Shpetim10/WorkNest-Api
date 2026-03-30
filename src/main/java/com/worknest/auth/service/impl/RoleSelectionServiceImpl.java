package com.worknest.auth.service.impl;

import com.worknest.auth.dto.SelectRoleRequest;
import com.worknest.auth.dto.SelectRoleResponse;
import com.worknest.auth.service.RoleSelectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleSelectionServiceImpl implements RoleSelectionService {

    @Override
    @Transactional
    public SelectRoleResponse selectRole(SelectRoleRequest request, String userId) {
        log.info("User {} selecting role assignment: {}", userId, request.roleAssignmentId());
        
        // TODO: Verify user is assigned to this company (User entry exists)
        // TODO: Verify user has requested permission/role assignment if provided
        // TODO: Generate new JWT including company ID and business roles in claims
        // TODO: Publish RoleSelectedEvent
        
        return new SelectRoleResponse(null, null, null, null, null, null);
    }
}

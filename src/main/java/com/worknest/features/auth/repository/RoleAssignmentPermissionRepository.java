package com.worknest.features.auth.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.worknest.domain.entities.RoleAssignmentPermission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleAssignmentPermissionRepository extends JpaRepository<RoleAssignmentPermission, UUID> {

    Optional<RoleAssignmentPermission> findByRoleAssignmentIdAndPermissionCode(UUID roleAssignmentId, String permissionCode);

    Optional<RoleAssignmentPermission> findByRoleAssignmentIdAndPermissionResourceAndPermissionAction(
            UUID roleAssignmentId,
            String resource,
            String action
    );

    List<RoleAssignmentPermission> findAllByRoleAssignmentId(UUID roleAssignmentId);

    List<RoleAssignmentPermission> findAllByRoleAssignmentIdAndIsGranted(UUID roleAssignmentId, Boolean isGranted);

    List<RoleAssignmentPermission> findAllByRoleAssignmentIdInAndPermissionCodeIn(
            Collection<UUID> roleAssignmentIds,
            Collection<String> permissionCodes
    );

    void deleteAllByRoleAssignmentId(UUID roleAssignmentId);
}

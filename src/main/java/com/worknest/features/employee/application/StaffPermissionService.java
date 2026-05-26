package com.worknest.features.employee.application;

import com.worknest.features.employee.dto.StaffPermissionsResponse;
import java.util.List;
import java.util.UUID;

public interface StaffPermissionService {

    StaffPermissionsResponse getPermissions(UUID companyId, UUID staffId);

    StaffPermissionsResponse replacePermissions(UUID companyId, UUID staffId, List<String> permissionCodes);
}

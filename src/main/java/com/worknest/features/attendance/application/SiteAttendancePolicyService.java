package com.worknest.features.attendance.application;

import com.worknest.features.attendance.dto.SiteAttendancePolicyRequest;
import com.worknest.features.attendance.dto.SiteAttendancePolicyResponse;
import java.util.UUID;

public interface SiteAttendancePolicyService {

    SiteAttendancePolicyResponse getSitePolicy(UUID companyId, UUID siteId);

    SiteAttendancePolicyResponse updateSitePolicy(UUID companyId, UUID siteId, SiteAttendancePolicyRequest request);
}

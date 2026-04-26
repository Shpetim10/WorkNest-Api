package com.worknest.features.attendance.application;

import com.worknest.features.attendance.dto.CompanyAttendancePolicyResponse;
import com.worknest.features.attendance.dto.SiteAttendancePolicyRequest;
import java.util.UUID;

public interface CompanyAttendancePolicyService {

    CompanyAttendancePolicyResponse getCompanyDefaultPolicy(UUID companyId);

    CompanyAttendancePolicyResponse upsertCompanyDefaultPolicy(UUID companyId, SiteAttendancePolicyRequest request);
}

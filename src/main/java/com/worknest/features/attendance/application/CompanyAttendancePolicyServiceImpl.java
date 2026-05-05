package com.worknest.features.attendance.application;

import com.worknest.common.exception.BusinessException;
import com.worknest.domain.entities.AttendancePolicy;
import com.worknest.domain.entities.Company;
import com.worknest.domain.enums.AttendancePolicySource;
import com.worknest.features.attendance.dto.CompanyAttendancePolicyResponse;
import com.worknest.features.attendance.dto.EffectiveAttendancePolicyDto;
import com.worknest.features.attendance.dto.SiteAttendancePolicyRequest;
import com.worknest.features.attendance.repository.AttendancePolicyRepository;
import com.worknest.features.company.repository.CompanyRepository;
import com.worknest.tenant.TenantContextHolder;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CompanyAttendancePolicyServiceImpl implements CompanyAttendancePolicyService {

    private final CompanyRepository companyRepository;
    private final AttendancePolicyRepository attendancePolicyRepository;
    private final AttendancePolicyResolver attendancePolicyResolver;

    @Override
    @Transactional(readOnly = true)
    public CompanyAttendancePolicyResponse getCompanyDefaultPolicy(UUID companyId) {
        validateTenant(companyId);
        AttendancePolicy policy = attendancePolicyRepository.findByCompanyIdAndSiteIsNull(companyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "COMPANY_POLICY_NOT_FOUND", "Company default attendance policy is not configured."));
        EffectiveAttendancePolicyDto dto = attendancePolicyResolver.map(policy, AttendancePolicySource.COMPANY_DEFAULT);
        return new CompanyAttendancePolicyResponse(companyId, dto);
    }

    @Override
    public CompanyAttendancePolicyResponse upsertCompanyDefaultPolicy(UUID companyId, SiteAttendancePolicyRequest request) {
        validateTenant(companyId);
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "COMPANY_NOT_FOUND", "Company does not exist."));

        AttendancePolicy policy = attendancePolicyRepository.findByCompanyIdAndSiteIsNull(companyId).orElseGet(() -> {
            AttendancePolicy created = new AttendancePolicy();
            created.setCompany(company);
            created.setSite(null);
            return created;
        });

        policy.setRequireQr(request.requireQr());
        policy.setRequireLocation(request.requireLocation());
        policy.setCheckInEnabled(request.checkInEnabled());
        policy.setCheckOutEnabled(request.checkOutEnabled());
        policy.setUseNetworkAsWarning(request.useNetworkAsWarning());
        policy.setRejectOutsideGeofence(request.rejectOutsideGeofence());
        policy.setRejectPoorAccuracy(request.rejectPoorAccuracy());
        policy.setAllowManualCorrection(request.allowManualCorrection());
        policy.setAllowManagerManualEntry(request.allowManagerManualEntry());

        AttendancePolicy saved = attendancePolicyRepository.save(policy);
        EffectiveAttendancePolicyDto dto = attendancePolicyResolver.map(saved, AttendancePolicySource.COMPANY_DEFAULT);
        return new CompanyAttendancePolicyResponse(companyId, dto);
    }

    private void validateTenant(UUID companyId) {
        UUID tenantCompanyId = TenantContextHolder.get()
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "TENANT_CONTEXT_MISSING", "No tenant context found."))
                .companyId();
        if (!tenantCompanyId.equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Cross-tenant access is not allowed.");
        }
    }
}

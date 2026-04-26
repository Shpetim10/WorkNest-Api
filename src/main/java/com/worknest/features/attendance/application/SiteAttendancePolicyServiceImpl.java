package com.worknest.features.attendance.application;

import com.worknest.common.exception.BusinessException;
import com.worknest.domain.entities.AttendancePolicy;
import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.CompanySite;
import com.worknest.domain.enums.AttendancePolicySource;
import com.worknest.features.attendance.dto.EffectiveAttendancePolicyDto;
import com.worknest.features.attendance.dto.SiteAttendancePolicyRequest;
import com.worknest.features.attendance.dto.SiteAttendancePolicyResponse;
import com.worknest.features.attendance.repository.AttendancePolicyRepository;
import com.worknest.features.company.repository.CompanyRepository;
import com.worknest.features.companySite.exception.SiteNotFoundException;
import com.worknest.features.companySite.repository.CompanySiteRepository;
import com.worknest.tenant.TenantContextHolder;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SiteAttendancePolicyServiceImpl implements SiteAttendancePolicyService {

    private final CompanyRepository companyRepository;
    private final CompanySiteRepository companySiteRepository;
    private final AttendancePolicyRepository attendancePolicyRepository;
    private final AttendancePolicyResolver attendancePolicyResolver;

    @Override
    @Transactional(readOnly = true)
    public SiteAttendancePolicyResponse getSitePolicy(UUID companyId, UUID siteId) {
        validateTenant(companyId);
        CompanySite site = companySiteRepository.findByIdAndCompanyId(siteId, companyId).orElseThrow(SiteNotFoundException::new);
        ResolvedAttendancePolicy resolved = attendancePolicyResolver.resolveForSite(site.getCompany(), site);
        return new SiteAttendancePolicyResponse(companyId, siteId, resolved.dto());
    }

    @Override
    public SiteAttendancePolicyResponse updateSitePolicy(UUID companyId, UUID siteId, SiteAttendancePolicyRequest request) {
        validateTenant(companyId);

        Company company = companyRepository.findById(companyId).orElseThrow(() -> new BusinessException(
                HttpStatus.NOT_FOUND,
                "COMPANY_NOT_FOUND",
                "Company does not exist."
        ));
        CompanySite site = companySiteRepository.findByIdAndCompanyId(siteId, companyId).orElseThrow(SiteNotFoundException::new);

        AttendancePolicy policy = attendancePolicyRepository.findByCompanyIdAndSiteId(companyId, siteId)
                .orElseGet(() -> {
                    AttendancePolicy created = new AttendancePolicy();
                    created.setCompany(company);
                    created.setSite(site);
                    return created;
                });

        applyRequest(policy, request);
        AttendancePolicy saved = attendancePolicyRepository.save(policy);

        // Keep legacy site flags synchronized for backward-compatible consumers.
        site.setLocationRequired(request.requireLocation());
        site.setQrEnabled(request.requireQr());
        site.setCheckInEnabled(request.checkInEnabled());
        site.setCheckOutEnabled(request.checkOutEnabled());
        companySiteRepository.save(site);

        EffectiveAttendancePolicyDto dto = attendancePolicyResolver.map(saved, AttendancePolicySource.SITE_OVERRIDE);
        return new SiteAttendancePolicyResponse(companyId, siteId, dto);
    }

    private void applyRequest(AttendancePolicy policy, SiteAttendancePolicyRequest request) {
        if (Boolean.TRUE.equals(request.missingCheckoutAutoCloseEnabled()) && request.autoCheckoutAfterMinutes() == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "AUTO_CHECKOUT_MINUTES_REQUIRED",
                    "autoCheckoutAfterMinutes is required when missing checkout auto-close is enabled."
            );
        }

        policy.setRequireQr(request.requireQr());
        policy.setRequireLocation(request.requireLocation());
        policy.setCheckInEnabled(request.checkInEnabled());
        policy.setCheckOutEnabled(request.checkOutEnabled());
        policy.setUseNetworkAsWarning(request.useNetworkAsWarning());
        policy.setRejectOutsideGeofence(request.rejectOutsideGeofence());
        policy.setRejectPoorAccuracy(request.rejectPoorAccuracy());
        policy.setAllowManualCorrection(request.allowManualCorrection());
        policy.setAllowManagerManualEntry(request.allowManagerManualEntry());
        policy.setMissingCheckoutAutoCloseEnabled(request.missingCheckoutAutoCloseEnabled());
        policy.setAutoCheckoutAfterMinutes(request.autoCheckoutAfterMinutes());
        policy.setLateGraceMinutes(request.lateGraceMinutes());
        policy.setEarlyClockInWindowMinutes(request.earlyClockInWindowMinutes());
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

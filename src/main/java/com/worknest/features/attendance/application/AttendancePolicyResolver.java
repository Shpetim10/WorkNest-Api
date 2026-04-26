package com.worknest.features.attendance.application;

import com.worknest.domain.entities.AttendancePolicy;
import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.CompanySite;
import com.worknest.domain.enums.AttendancePolicySource;
import com.worknest.features.attendance.dto.EffectiveAttendancePolicyDto;
import com.worknest.features.attendance.repository.AttendancePolicyRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AttendancePolicyResolver {

    private final AttendancePolicyRepository attendancePolicyRepository;

    public ResolvedAttendancePolicy resolveForSite(Company company, CompanySite site) {
        UUID companyId = company.getId();
        UUID siteId = site.getId();

        AttendancePolicy sitePolicy = attendancePolicyRepository.findByCompanyIdAndSiteId(companyId, siteId).orElse(null);
        if (sitePolicy != null) {
            return new ResolvedAttendancePolicy(sitePolicy, map(sitePolicy, AttendancePolicySource.SITE_OVERRIDE));
        }

        var companyPolicyOpt = attendancePolicyRepository.findByCompanyIdAndSiteIsNull(companyId);
        if (companyPolicyOpt.isPresent()) {
            AttendancePolicy companyPolicy = companyPolicyOpt.get();
            return new ResolvedAttendancePolicy(companyPolicy, map(companyPolicy, AttendancePolicySource.COMPANY_DEFAULT));
        }

        AttendancePolicy fallback = legacyFallbackPolicy(company, site);
        return new ResolvedAttendancePolicy(fallback, map(fallback, AttendancePolicySource.SITE_OVERRIDE));
    }

    public EffectiveAttendancePolicyDto map(AttendancePolicy policy, AttendancePolicySource source) {
        return new EffectiveAttendancePolicyDto(
                policy.getId(),
                source,
                Boolean.TRUE.equals(policy.getRequireQr()),
                Boolean.TRUE.equals(policy.getRequireLocation()),
                Boolean.TRUE.equals(policy.getCheckInEnabled()),
                Boolean.TRUE.equals(policy.getCheckOutEnabled()),
                Boolean.TRUE.equals(policy.getUseNetworkAsWarning()),
                Boolean.TRUE.equals(policy.getRejectOutsideGeofence()),
                Boolean.TRUE.equals(policy.getRejectPoorAccuracy()),
                Boolean.TRUE.equals(policy.getAllowManualCorrection()),
                Boolean.TRUE.equals(policy.getAllowManagerManualEntry()),
                Boolean.TRUE.equals(policy.getMissingCheckoutAutoCloseEnabled()),
                policy.getAutoCheckoutAfterMinutes(),
                policy.getLateGraceMinutes() != null ? policy.getLateGraceMinutes() : 0,
                policy.getEarlyClockInWindowMinutes() != null ? policy.getEarlyClockInWindowMinutes() : 0
        );
    }

    private AttendancePolicy legacyFallbackPolicy(Company company, CompanySite site) {
        AttendancePolicy fallback = new AttendancePolicy();
        fallback.setCompany(company);
        fallback.setSite(site);
        fallback.setRequireQr(Boolean.TRUE.equals(site.getQrEnabled()));
        fallback.setRequireLocation(Boolean.TRUE.equals(site.getLocationRequired()));
        fallback.setCheckInEnabled(Boolean.TRUE.equals(site.getCheckInEnabled()));
        fallback.setCheckOutEnabled(Boolean.TRUE.equals(site.getCheckOutEnabled()));
        fallback.setUseNetworkAsWarning(true);
        fallback.setRejectOutsideGeofence(true);
        fallback.setRejectPoorAccuracy(false);
        fallback.setAllowManualCorrection(true);
        fallback.setAllowManagerManualEntry(true);
        fallback.setMissingCheckoutAutoCloseEnabled(false);
        fallback.setLateGraceMinutes(0);
        fallback.setEarlyClockInWindowMinutes(0);
        return fallback;
    }
}

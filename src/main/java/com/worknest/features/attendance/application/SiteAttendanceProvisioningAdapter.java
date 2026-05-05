package com.worknest.features.attendance.application;

import com.worknest.domain.entities.AttendancePolicy;
import com.worknest.domain.entities.AttendanceQrTerminal;
import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.CompanySite;
import com.worknest.features.attendance.repository.AttendancePolicyRepository;
import com.worknest.features.companySite.application.SiteAttendanceProvisioningPort;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SiteAttendanceProvisioningAdapter implements SiteAttendanceProvisioningPort {

    private final AttendancePolicyRepository attendancePolicyRepository;
    private final AttendanceQrService attendanceQrService;

    @Override
    public AttendancePolicy savePolicy(AttendancePolicy policy) {
        return attendancePolicyRepository.save(policy);
    }

    @Override
    public AttendanceQrTerminal ensureDefaultTerminal(Company company, CompanySite site) {
        return attendanceQrService.ensureDefaultTerminal(company, site);
    }

    @Override
    public void syncRequireLocation(UUID companyId, UUID siteId, boolean requireLocation) {
        attendancePolicyRepository.findByCompanyIdAndSiteId(companyId, siteId).ifPresent(policy -> {
            policy.setRequireLocation(requireLocation);
            attendancePolicyRepository.save(policy);
        });
    }
}

package com.worknest.features.companySite.application;

import com.worknest.domain.entities.CompanySite;
import com.worknest.domain.enums.AttendancePolicySource;
import com.worknest.domain.enums.SiteStatus;
import com.worknest.features.attendance.application.AttendancePolicyResolver;
import com.worknest.features.attendance.repository.AttendanceQrTerminalRepository;
import com.worknest.features.company.repository.CompanyRepository;
import com.worknest.features.companySite.dto.CompanySiteDetailsResponse;
import com.worknest.features.companySite.dto.CompanySiteLookup;
import com.worknest.features.companySite.dto.CompanySiteResponse;
import com.worknest.features.companySite.dto.LinkedQrTerminalResponse;
import com.worknest.features.companySite.dto.SiteAttendancePolicySummaryResponse;
import com.worknest.features.companySite.dto.TrustedNetworkResponse;
import com.worknest.features.companySite.exception.CompanyNotFoundException;
import com.worknest.features.companySite.exception.SiteNotFoundException;
import com.worknest.features.companySite.repository.CompanySiteRepository;
import com.worknest.features.companySite.repository.SiteTrustedNetworkRepository;
import com.worknest.tenant.TenantContextHolder;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanySiteQueryServiceImpl implements CompanySiteQueryService {

    private final CompanySiteRepository         siteRepository;
    private final CompanyRepository             companyRepository;
    private final SiteTrustedNetworkRepository  networkRepository;
    private final AttendancePolicyResolver      attendancePolicyResolver;
    private final AttendanceQrTerminalRepository attendanceQrTerminalRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<CompanySiteResponse> listSites(UUID companyId, Pageable pageable) {
        UUID tenantCompanyId = TenantContextHolder.get()
                .orElseThrow(() -> new IllegalStateException("No tenant context found"))
                .companyId();

        if (!tenantCompanyId.equals(companyId) || !companyRepository.existsById(companyId)) {
            throw new CompanyNotFoundException();
        }

        return siteRepository.findAllByCompanyIdOrderByCreatedAtDesc(companyId, pageable)
                .map(CompanySiteResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanySiteDetailsResponse getSiteDetails(UUID companyId, UUID siteId) {
        UUID tenantCompanyId = TenantContextHolder.get()
                .orElseThrow(() -> new IllegalStateException("No tenant context found"))
                .companyId();

        if (!tenantCompanyId.equals(companyId) || !companyRepository.existsById(companyId)) {
            throw new CompanyNotFoundException();
        }

        CompanySite site = siteRepository.findByIdAndCompanyId(siteId, companyId)
                .orElseThrow(SiteNotFoundException::new);

        List<TrustedNetworkResponse> networks = networkRepository.findAllBySiteIdOrderByPriorityOrderAsc(
                        siteId,
                        Pageable.unpaged())
                .stream()
                .map(TrustedNetworkResponse::fromEntity)
                .toList();

        String countryName = site.getCountryCode() != null
                ? new Locale("", site.getCountryCode()).getDisplayCountry(Locale.ENGLISH)
                : null;

        return new CompanySiteDetailsResponse(
                CompanySiteResponse.fromEntity(site),
                countryName,
                networks,
                mapPolicy(site),
                attendanceQrTerminalRepository.findAllBySiteIdOrderByCreatedAtAsc(siteId).stream()
                        .map(terminal -> new LinkedQrTerminalResponse(
                                terminal.getId(),
                                terminal.getName(),
                                terminal.getStatus(),
                                terminal.getRotationSeconds(),
                                Boolean.TRUE.equals(terminal.getAutoCreated()),
                                terminal.getLastHeartbeatAt()
                        ))
                        .toList()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanySiteLookup> lookupSites(UUID companyId) {
        UUID tenantCompanyId = TenantContextHolder.get()
                .orElseThrow(() -> new IllegalStateException("No tenant context found"))
                .companyId();

        if (!tenantCompanyId.equals(companyId) || !companyRepository.existsById(companyId)) {
            throw new CompanyNotFoundException();
        }

        return siteRepository.findAllByCompanyIdAndStatus(companyId, SiteStatus.ACTIVE)
                .stream()
                .map(CompanySiteLookup::fromEntity)
                .toList();
    }

    private SiteAttendancePolicySummaryResponse mapPolicy(CompanySite site) {
        var resolved = attendancePolicyResolver.resolveForSite(site.getCompany(), site);
        var dto = resolved.dto();
        return new SiteAttendancePolicySummaryResponse(
                dto.policyId(),
                dto.policySource() != null ? dto.policySource() : AttendancePolicySource.COMPANY_DEFAULT,
                dto.requireQr(),
                dto.requireLocation(),
                dto.checkInEnabled(),
                dto.checkOutEnabled(),
                dto.useNetworkAsWarning(),
                dto.rejectOutsideGeofence(),
                dto.rejectPoorAccuracy(),
                dto.allowManualCorrection(),
                dto.allowManagerManualEntry()
        );
    }
}

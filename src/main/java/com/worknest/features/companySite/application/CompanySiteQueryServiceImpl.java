package com.worknest.features.companySite.application;

import com.worknest.domain.entities.CompanySite;
import com.worknest.domain.enums.SiteStatus;
import com.worknest.features.company.repository.CompanyRepository;
import com.worknest.features.companySite.dto.CompanySiteDetailsResponse;
import com.worknest.features.companySite.dto.CompanySiteLookup;
import com.worknest.features.companySite.dto.CompanySiteResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanySiteQueryServiceImpl implements CompanySiteQueryService {

    private final CompanySiteRepository         siteRepository;
    private final CompanyRepository             companyRepository;
    private final SiteTrustedNetworkRepository  networkRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CompanySiteResponse> listSites(UUID companyId) {
        UUID tenantCompanyId = TenantContextHolder.get()
                .orElseThrow(() -> new IllegalStateException("No tenant context found"))
                .companyId();

        if (!tenantCompanyId.equals(companyId) || !companyRepository.existsById(companyId)) {
            throw new CompanyNotFoundException();
        }

        return siteRepository.findAllByCompanyIdOrderByCreatedAtDesc(companyId)
                .stream()
                .map(CompanySiteResponse::fromEntity)
                .toList();
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

        List<TrustedNetworkResponse> networks = networkRepository.findAllBySiteIdOrderByPriorityOrderAsc(siteId)
                .stream()
                .map(TrustedNetworkResponse::fromEntity)
                .toList();

        String countryName = site.getCountryCode() != null
                ? new Locale("", site.getCountryCode()).getDisplayCountry(Locale.ENGLISH)
                : null;

        return new CompanySiteDetailsResponse(
                CompanySiteResponse.fromEntity(site),
                countryName,
                networks
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
}

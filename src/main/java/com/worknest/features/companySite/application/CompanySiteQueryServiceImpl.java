package com.worknest.features.companySite.application;

import com.worknest.features.company.repository.CompanyRepository;
import com.worknest.features.companySite.dto.CompanySiteResponse;
import com.worknest.features.companySite.exception.CompanyNotFoundException;
import com.worknest.features.companySite.repository.CompanySiteRepository;
import com.worknest.tenant.TenantContextHolder;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanySiteQueryServiceImpl implements CompanySiteQueryService {

    private final CompanySiteRepository siteRepository;
    private final CompanyRepository companyRepository;

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
}

package com.worknest.features.company.application;

import com.worknest.common.exception.BusinessException;
import com.worknest.domain.entities.Company;
import com.worknest.features.company.dto.CompanySettingsResponse;
import com.worknest.features.company.repository.CompanyRepository;
import com.worknest.tenant.TenantContextHolder;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanySettingsServiceImpl implements CompanySettingsService {

    private final CompanyRepository companyRepository;

    @Override
    @Transactional(readOnly = true)
    public CompanySettingsResponse getSettings(UUID companyId) {
        UUID tenantId = TenantContextHolder.get()
                .map(ctx -> ctx.companyId())
                .orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "TENANT_CONTEXT_MISSING", "No tenant context found."));

        if (!tenantId.equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "TENANT_MISMATCH", "Access denied to the requested company.");
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "COMPANY_NOT_FOUND", "Company does not exist."));

        return new CompanySettingsResponse(
                company.getId(),
                company.getName(),
                company.getTimezone(),
                company.getDateFormat(),
                company.getCurrency(),
                company.getLocale(),
                company.getCountryCode(),
                company.getLogoPath()
        );
    }
}

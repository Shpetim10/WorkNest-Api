package com.worknest.features.company.application;

import com.worknest.common.exception.BusinessException;
import com.worknest.domain.entities.Company;
import com.worknest.features.company.dto.CompanySettingsResponse;
import com.worknest.features.company.dto.UpdateCompanySettingsRequest;
import com.worknest.features.company.repository.CompanyRepository;
import com.worknest.tenant.TenantContextHolder;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CompanySettingsServiceImpl implements CompanySettingsService {

    private final CompanyRepository companyRepository;

    @Override
    @Transactional(readOnly = true)
    public CompanySettingsResponse getSettings(UUID companyId) {
        Company company = resolveCompany(companyId);
        return toResponse(company);
    }

    @Override
    @Transactional
    public CompanySettingsResponse updateSettings(UUID companyId, UpdateCompanySettingsRequest request) {
        Company company = resolveCompany(companyId);

        String newNipt = trimToNull(request.nipt());
        if (newNipt != null && !newNipt.equals(company.getNipt())
                && companyRepository.existsByNiptAndIdNot(newNipt, companyId)) {
            throw new BusinessException(HttpStatus.CONFLICT, "NIPT_ALREADY_IN_USE", "The provided NIPT is already registered to another company.");
        }

        company.setName(request.name().trim());
        company.setNipt(newNipt);
        company.setPhoneNumber(trimToNull(request.phoneNumber()));
        company.setIndustry(trimToNull(request.industry()));
        company.setCurrency(StringUtils.hasText(request.currency()) ? request.currency().trim() : company.getCurrency());
        company.setDateFormat(StringUtils.hasText(request.dateFormat()) ? request.dateFormat().trim() : company.getDateFormat());
        company.setTimezone(StringUtils.hasText(request.timezone()) ? request.timezone().trim() : company.getTimezone());
        company.setCountryCode(StringUtils.hasText(request.countryCode()) ? request.countryCode().trim().toUpperCase() : company.getCountryCode());
        if (Boolean.TRUE.equals(request.clearLogo())) {
            company.setLogoKey(null);
            company.setLogoPath(null);
        } else if (StringUtils.hasText(request.logoKey())) {
            company.setLogoKey(request.logoKey().trim());
            company.setLogoPath(request.logoPath());
        }

        return toResponse(companyRepository.save(company));
    }

    private Company resolveCompany(UUID companyId) {
        UUID tenantId = TenantContextHolder.get()
                .map(ctx -> ctx.companyId())
                .orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "TENANT_CONTEXT_MISSING", "No tenant context found."));

        if (!tenantId.equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "TENANT_MISMATCH", "Access denied to the requested company.");
        }

        return companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "COMPANY_NOT_FOUND", "Company does not exist."));
    }

    private CompanySettingsResponse toResponse(Company company) {
        return new CompanySettingsResponse(
                company.getId(),
                company.getName(),
                company.getEmail(),
                company.getNipt(),
                company.getPhoneNumber(),
                company.getIndustry(),
                company.getTimezone(),
                company.getDateFormat(),
                company.getCurrency(),
                company.getLocale(),
                company.getCountryCode(),
                company.getLogoKey(),
                company.getLogoPath()
        );
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
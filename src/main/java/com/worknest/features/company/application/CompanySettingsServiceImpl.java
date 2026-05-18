package com.worknest.features.company.application;

import com.worknest.common.exception.BusinessException;
import com.worknest.common.security.encryption.EncryptionService;
import com.worknest.domain.entities.Company;
import com.worknest.features.company.dto.CompanySettingsResponse;
import com.worknest.features.company.dto.CurrencyExchangeRequest;
import com.worknest.features.company.dto.UpdateCompanySettingsRequest;
import com.worknest.features.company.repository.CompanyRepository;
import com.worknest.features.employee.repository.EmployeeRepository;
import com.worknest.features.payroll.repository.PayrollAdjustmentRepository;
import com.worknest.features.payroll.repository.PayrollResultRepository;
import com.worknest.realtime.event.CompanySettingsUpdatedDomainEvent;
import com.worknest.security.AuthSessionPrincipal;
import com.worknest.tenant.TenantContextHolder;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CompanySettingsServiceImpl implements CompanySettingsService {

    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;
    private final PayrollResultRepository payrollResultRepository;
    private final PayrollAdjustmentRepository payrollAdjustmentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final EncryptionService encryptionService;

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

        String newNipt = encryptionService.normalizeNipt(request.nipt());
        String newNiptHash = encryptionService.hmacSha256Hex(newNipt);
        if (newNipt != null && !newNipt.equals(company.getNipt())
                && companyRepository.existsByNiptHashAndIdNot(newNiptHash, companyId)) {
            throw new BusinessException(HttpStatus.CONFLICT, "NIPT_ALREADY_IN_USE", "The provided NIPT is already registered to another company.");
        }

        company.setName(request.name().trim());
        company.setNipt(newNipt);
        company.setNiptHash(newNiptHash);
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

        CompanySettingsResponse response = toResponse(companyRepository.save(company));
        eventPublisher.publishEvent(new CompanySettingsUpdatedDomainEvent(companyId, resolveActorUserId(), response));
        return response;
    }

    @Override
    @Transactional
    public CompanySettingsResponse updateCurrency(UUID companyId, CurrencyExchangeRequest request) {
        Company company = resolveCompany(companyId);

        String newCurrency = request.newCurrency().trim().toUpperCase();
        if (newCurrency.equalsIgnoreCase(company.getCurrency())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "SAME_CURRENCY",
                    "The selected currency is already the company's active currency.");
        }

        BigDecimal rate = request.exchangeRate();
        employeeRepository.findAllByCompanyId(companyId).forEach(employee -> {
            if (employee.getMonthlySalary() != null) {
                employee.setMonthlySalary(employee.getMonthlySalary().multiply(rate));
            }
            if (employee.getHourlyRate() != null) {
                employee.setHourlyRate(employee.getHourlyRate().multiply(rate));
            }
        });

        payrollResultRepository.findAllByCompanyId(companyId).forEach(result -> {
            result.setBasePay(result.getBasePay().multiply(rate));
            result.setGrossEarnings(result.getGrossEarnings().multiply(rate));
            result.setTotalDeductions(result.getTotalDeductions().multiply(rate));
            result.setNetPay(result.getNetPay().multiply(rate));
            result.setIncomeTax(result.getIncomeTax().multiply(rate));
            result.setEmployeeSocialSecurity(result.getEmployeeSocialSecurity().multiply(rate));
            result.setEmployeePension(result.getEmployeePension().multiply(rate));
            result.setEmployerSocialSecurity(result.getEmployerSocialSecurity().multiply(rate));
            result.setEmployerPension(result.getEmployerPension().multiply(rate));
            result.setTaxableIncome(result.getTaxableIncome().multiply(rate));
            result.setEmployerCostTotal(result.getEmployerCostTotal().multiply(rate));
        });

        payrollAdjustmentRepository.findAllByCompanyId(companyId).forEach(adjustment ->
                adjustment.setAmount(adjustment.getAmount().multiply(rate)));

        company.setCurrency(newCurrency);
        CompanySettingsResponse response = toResponse(companyRepository.save(company));
        eventPublisher.publishEvent(new CompanySettingsUpdatedDomainEvent(companyId, resolveActorUserId(), response));
        return response;
    }

    private UUID resolveActorUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthSessionPrincipal p) {
            return p.userId();
        }
        return null;
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

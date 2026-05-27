package com.worknest.features.payroll.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.worknest.audit.domain.AuditLog;
import com.worknest.audit.service.AuditLogService;
import com.worknest.common.exception.BusinessException;
import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.CompanyPayrollSettings;
import com.worknest.domain.entities.CompanyTaxBracket;
import com.worknest.domain.entities.PublicHoliday;
import com.worknest.domain.enums.TaxBase;
import com.worknest.features.company.repository.CompanyRepository;
import com.worknest.features.payroll.dto.PayrollDtos.ParentalLeavePolicyResponse;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollSettingsResponse;
import com.worknest.features.payroll.dto.PayrollDtos.PublicHolidayRequest;
import com.worknest.features.payroll.dto.PayrollDtos.PublicHolidayResponse;
import com.worknest.features.payroll.dto.PayrollDtos.ReplaceTaxBracketsRequest;
import com.worknest.features.payroll.dto.PayrollDtos.SickLeavePolicyResponse;
import com.worknest.features.payroll.dto.PayrollDtos.TaxBracketRequest;
import com.worknest.features.payroll.dto.PayrollDtos.TaxBracketResponse;
import com.worknest.features.payroll.dto.PayrollDtos.UpsertPayrollSettingsRequest;
import com.worknest.features.payroll.repository.CompanyParentalLeavePolicyConfigRepository;
import com.worknest.features.payroll.repository.CompanyPayrollSettingsRepository;
import com.worknest.features.payroll.repository.CompanySickLeavePolicyConfigRepository;
import com.worknest.features.payroll.repository.CompanyTaxBracketRepository;
import com.worknest.features.payroll.repository.PublicHolidayRepository;
import com.worknest.security.AuthSessionPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PayrollSettingsServiceImpl implements PayrollSettingsService {

    private static final BigDecimal DEFAULT_SICK_PERCENTAGE = new BigDecimal("70.00");
    private static final int DEFAULT_SICK_MAX_DAYS = 14;
    private static final BigDecimal DEFAULT_PARENTAL_PERCENTAGE = new BigDecimal("80.00");
    private static final int DEFAULT_PARENTAL_MAX_DAYS = 90;

    private final CompanyPayrollSettingsRepository settingsRepository;
    private final CompanyTaxBracketRepository taxBracketRepository;
    private final PublicHolidayRepository holidayRepository;
    private final CompanySickLeavePolicyConfigRepository sickPolicyRepository;
    private final CompanyParentalLeavePolicyConfigRepository parentalPolicyRepository;
    private final CompanyRepository companyRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public PayrollSettingsResponse getSettings() {
        UUID companyId = principal().companyId();
        CompanyPayrollSettings settings = settingsRepository.findByCompanyId(companyId).orElse(null);
        return toSettingsResponse(companyId, settings);
    }

    @Override
    public PayrollSettingsResponse upsertSettings(UpsertPayrollSettingsRequest request) {
        UUID companyId = principal().companyId();
        validateWeekendDays(request.weekendDays());
        validateContributionBases(request.contributionMinBase(), request.contributionMaxBase());
        Company company = loadCompany(companyId);
        CompanyPayrollSettings settings = settingsRepository.findByCompanyId(companyId)
                .orElseGet(() -> {
                    CompanyPayrollSettings s = new CompanyPayrollSettings();
                    s.setCompany(company);
                    return s;
                });
        settings.setDefaultDailyWorkingHours(request.defaultDailyWorkingHours());
        settings.setWeekendDaysJson(toJson(request.weekendDays()));
        settings.setTaxEnabled(request.taxEnabled());
        settings.setTaxBase(request.taxBase());
        settings.setSocialSecurityEmployeeRate(request.socialSecurityEmployeeRate().setScale(3, java.math.RoundingMode.HALF_UP));
        settings.setSocialSecurityEmployerRate(request.socialSecurityEmployerRate().setScale(3, java.math.RoundingMode.HALF_UP));
        settings.setPensionEmployeeRate(request.pensionEmployeeRate().setScale(3, java.math.RoundingMode.HALF_UP));
        settings.setPensionEmployerRate(request.pensionEmployerRate().setScale(3, java.math.RoundingMode.HALF_UP));
        settings.setContributionMinBase(request.contributionMinBase());
        settings.setContributionMaxBase(request.contributionMaxBase());
        settingsRepository.save(settings);
        audit("PAYROLL_SETTINGS_UPSERTED", "company_payroll_settings", settings.getId(),
                Map.of("taxEnabled", settings.isTaxEnabled(), "taxBase", settings.getTaxBase()),
                Map.of("companyId", companyId));
        return toSettingsResponse(companyId, settings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaxBracketResponse> getTaxBrackets() {
        UUID companyId = principal().companyId();
        return taxBracketRepository.findAllByCompanyIdOrderByOrdinalAsc(companyId)
                .stream().map(this::toBracketResponse).toList();
    }

    @Override
    public List<TaxBracketResponse> replaceTaxBrackets(ReplaceTaxBracketsRequest request) {
        UUID companyId = principal().companyId();
        validateBrackets(request.brackets());
        Company company = loadCompany(companyId);
        taxBracketRepository.deleteAllByCompanyId(companyId);
        taxBracketRepository.flush();
        List<CompanyTaxBracket> saved = new ArrayList<>();
        for (int i = 0; i < request.brackets().size(); i++) {
            TaxBracketRequest br = request.brackets().get(i);
            CompanyTaxBracket bracket = new CompanyTaxBracket();
            bracket.setCompany(company);
            bracket.setOrdinal(i);
            bracket.setLowerBound(br.lowerBound());
            bracket.setUpperBound(br.upperBound());
            bracket.setRate(br.rate().setScale(3, java.math.RoundingMode.HALF_UP));
            saved.add(taxBracketRepository.save(bracket));
        }
        audit("TAX_BRACKETS_REPLACED", "company_tax_brackets", companyId,
                Map.of("count", saved.size()),
                Map.of("companyId", companyId));
        return saved.stream().map(this::toBracketResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicHolidayResponse> getHolidays(int year) {
        UUID companyId = principal().companyId();
        List<PublicHoliday> all = holidayRepository.findAllByCompanyId(companyId);
        return all.stream()
                .map(h -> resolveForYear(h, year))
                .filter(h -> h != null)
                .sorted(java.util.Comparator.comparing(PublicHolidayResponse::date))
                .toList();
    }

    @Override
    public PublicHolidayResponse createHoliday(PublicHolidayRequest request) {
        UUID companyId = principal().companyId();
        if (holidayRepository.existsByCompanyIdAndHolidayDate(companyId, request.date())) {
            throw new BusinessException(HttpStatus.CONFLICT, "HOLIDAY_DATE_CONFLICT",
                    "A holiday already exists for this date.");
        }
        Company company = loadCompany(companyId);
        PublicHoliday holiday = new PublicHoliday();
        holiday.setCompany(company);
        holiday.setHolidayDate(request.date());
        holiday.setName(request.name());
        holiday.setRecurring(request.recurring());
        holiday.setPaid(request.paid());
        holiday.setCreatedByUserId(principal().userId());
        holidayRepository.save(holiday);
        audit("PUBLIC_HOLIDAY_CREATED", "company_public_holidays", holiday.getId(),
                Map.of("date", holiday.getHolidayDate().toString(), "name", holiday.getName()),
                Map.of("companyId", companyId));
        return toHolidayResponse(holiday);
    }

    @Override
    public PublicHolidayResponse updateHoliday(UUID id, PublicHolidayRequest request) {
        UUID companyId = principal().companyId();
        PublicHoliday holiday = holidayRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "HOLIDAY_NOT_FOUND", "Holiday not found."));
        if (!holiday.getHolidayDate().equals(request.date())
                && holidayRepository.existsByCompanyIdAndHolidayDate(companyId, request.date())) {
            throw new BusinessException(HttpStatus.CONFLICT, "HOLIDAY_DATE_CONFLICT",
                    "A holiday already exists for this date.");
        }
        holiday.setHolidayDate(request.date());
        holiday.setName(request.name());
        holiday.setRecurring(request.recurring());
        holiday.setPaid(request.paid());
        holidayRepository.save(holiday);
        audit("PUBLIC_HOLIDAY_UPDATED", "company_public_holidays", holiday.getId(),
                Map.of("date", holiday.getHolidayDate().toString(), "name", holiday.getName()),
                Map.of("companyId", companyId));
        return toHolidayResponse(holiday);
    }

    @Override
    public void deleteHoliday(UUID id) {
        UUID companyId = principal().companyId();
        PublicHoliday holiday = holidayRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "HOLIDAY_NOT_FOUND", "Holiday not found."));
        holidayRepository.delete(holiday);
        audit("PUBLIC_HOLIDAY_DELETED", "company_public_holidays", id,
                Map.of("date", holiday.getHolidayDate().toString(), "name", holiday.getName()),
                Map.of("companyId", companyId));
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private void validateWeekendDays(List<String> weekendDays) {
        if (weekendDays == null || weekendDays.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_WORKWEEK_CONFIG",
                    "Weekend days list cannot be empty.");
        }
        if (weekendDays.size() >= 7) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_WORKWEEK_CONFIG",
                    "At least one working day must remain; all-weekend configuration is rejected.");
        }
        for (String day : weekendDays) {
            try {
                java.time.DayOfWeek.valueOf(day);
            } catch (IllegalArgumentException e) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_WORKWEEK_CONFIG",
                        "Invalid day of week: " + day);
            }
        }
    }

    private void validateContributionBases(BigDecimal min, BigDecimal max) {
        if (min != null && max != null && max.compareTo(min) < 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_CONTRIBUTION_BASES",
                    "contributionMaxBase must be >= contributionMinBase.");
        }
    }

    private void validateBrackets(List<TaxBracketRequest> brackets) {
        if (brackets == null || brackets.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_TAX_BRACKETS",
                    "At least one tax bracket is required.");
        }
        // Must start at 0
        if (brackets.get(0).lowerBound().compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_TAX_BRACKETS",
                    "First bracket must start at 0.");
        }
        // Contiguous, ordered, non-overlapping
        for (int i = 0; i < brackets.size() - 1; i++) {
            TaxBracketRequest curr = brackets.get(i);
            TaxBracketRequest next = brackets.get(i + 1);
            if (curr.upperBound() == null) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_TAX_BRACKETS",
                        "Only the last bracket may have a null upper bound.");
            }
            if (curr.upperBound().compareTo(next.lowerBound()) != 0) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_TAX_BRACKETS",
                        "Brackets must be contiguous: bracket " + i + " upper bound must equal bracket "
                                + (i + 1) + " lower bound.");
            }
        }
        // Last bracket must be open-ended
        if (brackets.get(brackets.size() - 1).upperBound() != null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_TAX_BRACKETS",
                    "Last tax bracket must be open-ended (null upperBound).");
        }
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private PayrollSettingsResponse toSettingsResponse(UUID companyId, CompanyPayrollSettings settings) {
        SickLeavePolicyResponse sickPolicy = sickPolicyRepository.findByCompanyId(companyId)
                .map(c -> new SickLeavePolicyResponse(c.getCompanyPaidPercentage(), c.getMaxCompanyPaidDays(), false))
                .orElse(new SickLeavePolicyResponse(DEFAULT_SICK_PERCENTAGE, DEFAULT_SICK_MAX_DAYS, true));
        ParentalLeavePolicyResponse parentalPolicy = parentalPolicyRepository.findByCompanyId(companyId)
                .map(c -> new ParentalLeavePolicyResponse(c.getCompanyPaidPercentage(), c.getMaxCompanyPaidDays(), false))
                .orElse(new ParentalLeavePolicyResponse(DEFAULT_PARENTAL_PERCENTAGE, DEFAULT_PARENTAL_MAX_DAYS, true));
        boolean isDefault = settings == null;
        if (isDefault) {
            return new PayrollSettingsResponse(
                    BigDecimal.valueOf(8), List.of("SATURDAY", "SUNDAY"), true,
                    TaxBase.GROSS_MINUS_CONTRIBUTIONS,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    null, null,
                    sickPolicy, parentalPolicy, true);
        }
        List<String> weekendDays = parseJson(settings.getWeekendDaysJson());
        return new PayrollSettingsResponse(
                settings.getDefaultDailyWorkingHours(), weekendDays,
                settings.isTaxEnabled(), settings.getTaxBase(),
                settings.getSocialSecurityEmployeeRate(), settings.getSocialSecurityEmployerRate(),
                settings.getPensionEmployeeRate(), settings.getPensionEmployerRate(),
                settings.getContributionMinBase(), settings.getContributionMaxBase(),
                sickPolicy, parentalPolicy, false);
    }

    private TaxBracketResponse toBracketResponse(CompanyTaxBracket b) {
        return new TaxBracketResponse(b.getId(), b.getOrdinal(), b.getLowerBound(), b.getUpperBound(), b.getRate());
    }

    private PublicHolidayResponse toHolidayResponse(PublicHoliday h) {
        return new PublicHolidayResponse(h.getId(), h.getHolidayDate(), h.getName(), h.isRecurring(), h.isPaid());
    }

    private PublicHolidayResponse resolveForYear(PublicHoliday h, int year) {
        LocalDate date;
        if (h.isRecurring()) {
            try {
                date = MonthDay.from(h.getHolidayDate()).atYear(year);
            } catch (Exception e) {
                return null;
            }
        } else {
            if (h.getHolidayDate().getYear() != year) return null;
            date = h.getHolidayDate();
        }
        return new PublicHolidayResponse(h.getId(), date, h.getName(), h.isRecurring(), h.isPaid());
    }

    private Company loadCompany(UUID companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "COMPANY_NOT_FOUND", "Company not found."));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> parseJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of("SATURDAY", "SUNDAY");
        }
    }

    private void audit(String action, String entityType, Object entityId,
                       Map<String, Object> diff, Map<String, Object> metadata) {
        try {
            AuthSessionPrincipal p = principal();
            UUID id = entityId instanceof UUID u ? u : null;
            auditLogService.logAction(new AuditLog(
                    p.companyId(), p.userId(), p.roleAssignmentId(), p.role(),
                    null, action, entityType, id, diff, metadata, null));
        } catch (Exception ignored) {
        }
    }

    private AuthSessionPrincipal principal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthSessionPrincipal p)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "No authentication session found.");
        }
        return p;
    }
}

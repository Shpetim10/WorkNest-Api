package com.worknest.features.payroll.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.CompanyPayrollSettings;
import com.worknest.domain.entities.PublicHoliday;
import com.worknest.features.payroll.repository.CompanyPayrollSettingsRepository;
import com.worknest.features.payroll.repository.PublicHolidayRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkingDayCalculatorTest {

    @Mock
    private CompanyPayrollSettingsRepository settingsRepository;

    @Mock
    private PublicHolidayRepository holidayRepository;

    private WorkingDayCalculator calculator;
    private UUID companyId;

    @BeforeEach
    void setUp() {
        calculator = new WorkingDayCalculator(settingsRepository, holidayRepository, new ObjectMapper());
        companyId = UUID.randomUUID();
        // Default: no settings → SAT+SUN weekend
        lenient().when(settingsRepository.findByCompanyId(any())).thenReturn(Optional.empty());
        // Default: no holidays
        lenient().when(holidayRepository.findAllByCompanyId(companyId)).thenReturn(List.of());
    }

    @Test
    void standardMonthMay2026Has21WorkingDays() {
        BigDecimal days = calculator.countWorkingDays(companyId, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));
        assertThat(days).isEqualByComparingTo("21");
    }

    @Test
    void unpaidNonRecurringHolidayReducesWorkingDays() {
        LocalDate holiday = LocalDate.of(2026, 5, 1); // Friday — a working day
        when(holidayRepository.findAllByCompanyId(companyId)).thenReturn(List.of(unpaidHoliday(holiday, false)));

        BigDecimal days = calculator.countWorkingDays(companyId, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));

        assertThat(days).isEqualByComparingTo("20");
    }

    @Test
    void paidHolidayDoesNotReduceWorkingDays() {
        LocalDate holiday = LocalDate.of(2026, 5, 1); // Friday
        when(holidayRepository.findAllByCompanyId(companyId)).thenReturn(List.of(paidHoliday(holiday, false)));

        BigDecimal days = calculator.countWorkingDays(companyId, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));

        assertThat(days).isEqualByComparingTo("21");
    }

    /**
     * Core regression: a recurring holiday stored with year 2024 must still be
     * excluded from working-day counts in 2026 and beyond.
     */
    @Test
    void recurringHolidayStoredInDifferentYearIsExcludedFromWorkingDays() {
        // Stored as 2024-05-01 but recurring=true → should fire on 2026-05-01 too
        LocalDate storedDate = LocalDate.of(2024, 5, 1);
        when(holidayRepository.findAllByCompanyId(companyId)).thenReturn(List.of(unpaidHoliday(storedDate, true)));

        BigDecimal days = calculator.countWorkingDays(companyId, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));

        assertThat(days).isEqualByComparingTo("20");
    }

    @Test
    void recurringHolidayIsExcludedAcrossYearBoundary() {
        // Dec 31 recurring — check it fires in both 2025 and 2026 when range spans two years
        LocalDate storedDate = LocalDate.of(2020, 12, 31);
        when(holidayRepository.findAllByCompanyId(companyId)).thenReturn(List.of(unpaidHoliday(storedDate, true)));

        // Dec 28 – Jan 2: Dec 31 (Wed) and Jan 1 (Thu) are in range; only Dec 31 is a holiday here
        BigDecimal days = calculator.countWorkingDays(companyId, LocalDate.of(2025, 12, 29), LocalDate.of(2026, 1, 2));

        // Mon Dec 29, Tue Dec 30, [Dec 31 excluded], Thu Jan 1 not a holiday, Fri Jan 2 = 4 working days
        assertThat(days).isEqualByComparingTo("4");
    }

    @Test
    void isPaidHolidayReturnsTrueForRecurringPaidHolidayInDifferentYear() {
        LocalDate storedDate = LocalDate.of(2024, 12, 25);
        when(holidayRepository.findAllByCompanyId(companyId)).thenReturn(List.of(paidHoliday(storedDate, true)));

        assertThat(calculator.isPaidHoliday(companyId, LocalDate.of(2026, 12, 25))).isTrue();
    }

    @Test
    void isPaidHolidayReturnsFalseForUnpaidHoliday() {
        LocalDate storedDate = LocalDate.of(2026, 5, 1);
        when(holidayRepository.findAllByCompanyId(companyId)).thenReturn(List.of(unpaidHoliday(storedDate, false)));

        assertThat(calculator.isPaidHoliday(companyId, LocalDate.of(2026, 5, 1))).isFalse();
    }

    @Test
    void customWeekendDaysAreRespected() {
        CompanyPayrollSettings settings = new CompanyPayrollSettings();
        settings.setWeekendDaysJson("[\"FRIDAY\",\"SATURDAY\"]");
        when(settingsRepository.findByCompanyId(companyId)).thenReturn(Optional.of(settings));

        // May 2026: FRI+SAT off instead of SAT+SUN → count manually
        BigDecimal days = calculator.countWorkingDays(companyId, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));

        // May 2026 has 4 Fridays (1,8,15,22,29=5) and 4 Saturdays(2,9,16,23,30=5): 31 - 10 = 21
        assertThat(days).isEqualByComparingTo("21");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private PublicHoliday unpaidHoliday(LocalDate date, boolean recurring) {
        return holiday(date, recurring, false);
    }

    private PublicHoliday paidHoliday(LocalDate date, boolean recurring) {
        return holiday(date, recurring, true);
    }

    private PublicHoliday holiday(LocalDate date, boolean recurring, boolean paid) {
        Company company = new Company();
        company.setId(companyId);
        PublicHoliday h = new PublicHoliday();
        h.setId(UUID.randomUUID());
        h.setCompany(company);
        h.setHolidayDate(date);
        h.setName("Test Holiday");
        h.setRecurring(recurring);
        h.setPaid(paid);
        return h;
    }
}

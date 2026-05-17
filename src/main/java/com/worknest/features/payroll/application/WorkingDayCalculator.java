package com.worknest.features.payroll.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.worknest.domain.entities.CompanyPayrollSettings;
import com.worknest.domain.entities.PublicHoliday;
import com.worknest.features.payroll.repository.CompanyPayrollSettingsRepository;
import com.worknest.features.payroll.repository.PublicHolidayRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Company-aware working-day calculator.
 * Excludes weekends (per company config, default SAT+SUN) and unpaid public holidays.
 * Paid public holidays count as paid non-working days — employee is paid but no leave is consumed.
 */
@Component
@RequiredArgsConstructor
public class WorkingDayCalculator {

    private static final Set<DayOfWeek> DEFAULT_WEEKEND = EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private final CompanyPayrollSettingsRepository settingsRepository;
    private final PublicHolidayRepository holidayRepository;
    private final ObjectMapper objectMapper;

    /**
     * Count working days in [start, end] for the company.
     * Working day = not a weekend, not an unpaid public holiday.
     */
    public BigDecimal countWorkingDays(UUID companyId, LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) {
            return BigDecimal.ZERO;
        }
        Set<DayOfWeek> weekendDays = resolveWeekendDays(companyId);
        Set<LocalDate> unpaidHolidays = resolveUnpaidHolidays(companyId, start, end);
        int count = 0;
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            if (!weekendDays.contains(cursor.getDayOfWeek()) && !unpaidHolidays.contains(cursor)) {
                count++;
            }
            cursor = cursor.plusDays(1);
        }
        return BigDecimal.valueOf(count);
    }

    /**
     * Returns true if the date is a working day for this company.
     */
    public boolean isWorkingDay(UUID companyId, LocalDate date) {
        Set<DayOfWeek> weekendDays = resolveWeekendDays(companyId);
        if (weekendDays.contains(date.getDayOfWeek())) {
            return false;
        }
        Set<LocalDate> unpaidHolidays = resolveUnpaidHolidays(companyId, date, date);
        return !unpaidHolidays.contains(date);
    }

    /**
     * Returns true if the date is a paid public holiday (paid non-working day).
     */
    public boolean isPaidHoliday(UUID companyId, LocalDate date) {
        Set<DayOfWeek> weekendDays = resolveWeekendDays(companyId);
        if (weekendDays.contains(date.getDayOfWeek())) {
            return false;
        }
        List<PublicHoliday> holidays = holidayRepository.findAllByCompanyIdAndHolidayDateBetween(companyId, date, date);
        for (PublicHoliday h : holidays) {
            if (matches(h, date) && h.isPaid()) {
                return true;
            }
        }
        return false;
    }

    public Set<DayOfWeek> resolveWeekendDays(UUID companyId) {
        return settingsRepository.findByCompanyId(companyId)
                .map(this::parseWeekendDays)
                .orElse(DEFAULT_WEEKEND);
    }

    private Set<DayOfWeek> parseWeekendDays(CompanyPayrollSettings settings) {
        try {
            List<String> names = objectMapper.readValue(settings.getWeekendDaysJson(), STRING_LIST);
            Set<DayOfWeek> result = EnumSet.noneOf(DayOfWeek.class);
            for (String name : names) {
                result.add(DayOfWeek.valueOf(name));
            }
            return result.isEmpty() ? DEFAULT_WEEKEND : result;
        } catch (Exception e) {
            return DEFAULT_WEEKEND;
        }
    }

    private Set<LocalDate> resolveUnpaidHolidays(UUID companyId, LocalDate from, LocalDate to) {
        List<PublicHoliday> holidays = holidayRepository.findAllByCompanyIdAndHolidayDateBetween(companyId, from, to);
        if (holidays.isEmpty()) {
            return Collections.emptySet();
        }
        Set<LocalDate> unpaid = new HashSet<>();
        for (PublicHoliday h : holidays) {
            if (!h.isPaid()) {
                LocalDate resolved = resolveDate(h, from.getYear());
                if (resolved != null && !resolved.isBefore(from) && !resolved.isAfter(to)) {
                    unpaid.add(resolved);
                }
            }
        }
        return unpaid;
    }

    private boolean matches(PublicHoliday h, LocalDate date) {
        if (!h.isRecurring()) {
            return h.getHolidayDate().equals(date);
        }
        return MonthDay.from(h.getHolidayDate()).equals(MonthDay.from(date));
    }

    private LocalDate resolveDate(PublicHoliday h, int year) {
        if (!h.isRecurring()) {
            return h.getHolidayDate();
        }
        try {
            return MonthDay.from(h.getHolidayDate()).atYear(year);
        } catch (Exception e) {
            return null;
        }
    }
}

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
import java.util.ArrayList;
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
        List<PublicHoliday> holidays = holidayRepository.findAllByCompanyId(companyId);
        for (PublicHoliday h : holidays) {
            if (h.isPaid() && matches(h, date)) {
                return true;
            }
        }
        return false;
    }

    public BigDecimal countPaidHolidays(UUID companyId, LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) {
            return BigDecimal.ZERO;
        }
        Set<DayOfWeek> weekendDays = resolveWeekendDays(companyId);
        Set<LocalDate> paidHolidays = resolvePaidHolidays(companyId, start, end);
        long count = paidHolidays.stream()
                .filter(date -> !weekendDays.contains(date.getDayOfWeek()))
                .count();
        return BigDecimal.valueOf(count);
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
        List<PublicHoliday> holidays = holidayRepository.findAllByCompanyId(companyId);
        if (holidays.isEmpty()) {
            return Collections.emptySet();
        }
        Set<LocalDate> unpaid = new HashSet<>();
        for (PublicHoliday h : holidays) {
            if (!h.isPaid()) {
                for (LocalDate resolved : resolveOccurrences(h, from, to)) {
                    unpaid.add(resolved);
                }
            }
        }
        return unpaid;
    }

    private Set<LocalDate> resolvePaidHolidays(UUID companyId, LocalDate from, LocalDate to) {
        List<PublicHoliday> holidays = holidayRepository.findAllByCompanyId(companyId);
        if (holidays.isEmpty()) {
            return Collections.emptySet();
        }
        Set<LocalDate> paid = new HashSet<>();
        for (PublicHoliday h : holidays) {
            if (h.isPaid()) {
                for (LocalDate resolved : resolveOccurrences(h, from, to)) {
                    paid.add(resolved);
                }
            }
        }
        return paid;
    }

    private boolean matches(PublicHoliday h, LocalDate date) {
        if (!h.isRecurring()) {
            return h.getHolidayDate().equals(date);
        }
        return MonthDay.from(h.getHolidayDate()).equals(MonthDay.from(date));
    }

    /**
     * Returns all concrete dates that this holiday falls on within [from, to].
     * For non-recurring holidays this is at most one date. For recurring holidays
     * every year in the range is checked so a range spanning year boundaries works correctly.
     */
    private List<LocalDate> resolveOccurrences(PublicHoliday h, LocalDate from, LocalDate to) {
        if (!h.isRecurring()) {
            LocalDate d = h.getHolidayDate();
            return (!d.isBefore(from) && !d.isAfter(to)) ? List.of(d) : List.of();
        }
        MonthDay md = MonthDay.from(h.getHolidayDate());
        List<LocalDate> result = new ArrayList<>();
        for (int year = from.getYear(); year <= to.getYear(); year++) {
            try {
                LocalDate candidate = md.atYear(year);
                if (!candidate.isBefore(from) && !candidate.isAfter(to)) {
                    result.add(candidate);
                }
            } catch (Exception ignored) {
                // Feb 29 in non-leap years — skip
            }
        }
        return result;
    }
}

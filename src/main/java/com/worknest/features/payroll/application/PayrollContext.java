package com.worknest.features.payroll.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

public record PayrollContext(
        YearMonth payrollMonth,
        LocalDate periodStart,
        LocalDate periodEnd,
        int calendarDaysInMonth,
        int workingDaysInMonth,
        BigDecimal defaultDailyWorkingHours,
        String currency
) {
}

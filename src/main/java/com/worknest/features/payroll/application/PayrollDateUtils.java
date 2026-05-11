package com.worknest.features.payroll.application;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

final class PayrollDateUtils {

    private PayrollDateUtils() {
    }

    static LocalDate max(LocalDate first, LocalDate second) {
        return first.isAfter(second) ? first : second;
    }

    static LocalDate min(LocalDate first, LocalDate second) {
        return first.isBefore(second) ? first : second;
    }

    static BigDecimal countWorkingDays(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) {
            return BigDecimal.ZERO;
        }
        int count = 0;
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            if (isWorkingDay(cursor)) {
                count++;
            }
            cursor = cursor.plusDays(1);
        }
        return BigDecimal.valueOf(count);
    }

    static boolean isWorkingDay(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }
}

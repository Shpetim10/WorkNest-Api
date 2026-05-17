package com.worknest.features.payroll.application;

import com.worknest.domain.entities.Employee;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface WorkHoursProvider {

    WorkHoursResult getWorkedHours(Employee employee, PayrollContext context,
                                   BigDecimal payableWorkingDays, LocalDate payableFrom, LocalDate payableTo);

    record WorkHoursResult(BigDecimal hours, String source) {
    }
}

package com.worknest.features.payroll.application;

import com.worknest.domain.entities.Employee;
import java.math.BigDecimal;

public interface WorkHoursProvider {

    WorkHoursResult getWorkedHours(Employee employee, PayrollContext context, BigDecimal payableWorkingDays);

    record WorkHoursResult(BigDecimal hours, String source) {
    }
}

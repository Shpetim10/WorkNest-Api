package com.worknest.features.payroll.application;

import com.worknest.domain.entities.Employee;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class DefaultWorkHoursProvider implements WorkHoursProvider {

    public static final String SOURCE = "DEFAULT_WORKING_DAYS_PLACEHOLDER";

    @Override
    public WorkHoursResult getWorkedHours(Employee employee, PayrollContext context, BigDecimal payableWorkingDays) {
        // TODO: Replace default working-hours calculation with actual timesheet/attendance data once available.
        return new WorkHoursResult(payableWorkingDays.multiply(context.defaultDailyWorkingHours()), SOURCE);
    }
}

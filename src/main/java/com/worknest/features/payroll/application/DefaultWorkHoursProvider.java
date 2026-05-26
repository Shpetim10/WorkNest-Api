package com.worknest.features.payroll.application;

import com.worknest.domain.entities.Employee;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class DefaultWorkHoursProvider implements WorkHoursProvider {

    public static final String SOURCE = "DEFAULT_WORKING_DAYS_PLACEHOLDER";

    @Override
    public WorkHoursResult getWorkedHours(Employee employee, PayrollContext context,
                                          BigDecimal payableWorkingDays, LocalDate payableFrom, LocalDate payableTo) {
        BigDecimal dailyHours = employee.getDailyWorkingHours() != null
                ? employee.getDailyWorkingHours()
                : context.defaultDailyWorkingHours();
        return new WorkHoursResult(payableWorkingDays.multiply(dailyHours), SOURCE);
    }
}

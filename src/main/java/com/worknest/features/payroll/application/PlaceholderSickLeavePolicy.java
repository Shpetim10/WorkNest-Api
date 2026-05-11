package com.worknest.features.payroll.application;

import com.worknest.domain.entities.Employee;
import com.worknest.domain.entities.LeaveRequest;
import com.worknest.features.payroll.dto.PayrollDtos.SickLeaveCalculationDetails;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PlaceholderSickLeavePolicy implements SickLeavePolicy {

    public static final String STATUS = "TODO_SICK_LEAVE_POLICY_NOT_CONFIGURED";

    @Override
    public SickLeaveCalculationDetails calculate(Employee employee, List<LeaveRequest> sickLeaves, PayrollContext context) {
        BigDecimal daysTaken = sickLeaves.stream()
                .map(leave -> PayrollDateUtils.countWorkingDays(
                        PayrollDateUtils.max(leave.getStartDate(), context.periodStart()),
                        PayrollDateUtils.min(leave.getEndDate(), context.periodEnd())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // TODO: Company-paid sick leave days and percentage must come from company payroll settings.
        // TODO: Insurance-covered sick leave calculation must be integrated later.
        return new SickLeaveCalculationDetails(daysTaken, null, null, null, null, null, STATUS);
    }
}

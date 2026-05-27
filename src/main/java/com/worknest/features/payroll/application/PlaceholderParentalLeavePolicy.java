package com.worknest.features.payroll.application;

import com.worknest.domain.entities.Employee;
import com.worknest.domain.entities.LeaveRequest;
import com.worknest.features.payroll.dto.PayrollDtos.ParentalLeaveCalculationDetails;
import java.math.BigDecimal;
import java.util.List;

public class PlaceholderParentalLeavePolicy implements ParentalLeavePolicy {

    public static final String STATUS = "TODO_PARENTAL_LEAVE_POLICY_NOT_CONFIGURED";

    @Override
    public ParentalLeaveCalculationDetails calculate(
            Employee employee,
            List<LeaveRequest> parentalLeavesInMonth,
            List<LeaveRequest> parentalLeavesInYear,
            PayrollContext context
    ) {
        BigDecimal daysTaken = parentalLeavesInMonth.stream()
                .map(leave -> PayrollDateUtils.countWorkingDays(
                        PayrollDateUtils.max(leave.getStartDate(), context.periodStart()),
                        PayrollDateUtils.min(leave.getEndDate(), context.periodEnd())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ParentalLeaveCalculationDetails(daysTaken, null, null, null, null, null, null, null, null, null, null, null, STATUS);
    }
}

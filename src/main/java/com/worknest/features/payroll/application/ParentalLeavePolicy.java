package com.worknest.features.payroll.application;

import com.worknest.domain.entities.Employee;
import com.worknest.domain.entities.LeaveRequest;
import com.worknest.features.payroll.dto.PayrollDtos.ParentalLeaveCalculationDetails;
import java.util.List;

public interface ParentalLeavePolicy {

    /**
     * @param parentalLeavesInMonth  approved parental leaves overlapping the payroll period
     * @param parentalLeavesInYear   all approved parental leaves in the calendar year (for YTD employer-liability tracking)
     */
    ParentalLeaveCalculationDetails calculate(
            Employee employee,
            List<LeaveRequest> parentalLeavesInMonth,
            List<LeaveRequest> parentalLeavesInYear,
            PayrollContext context
    );
}

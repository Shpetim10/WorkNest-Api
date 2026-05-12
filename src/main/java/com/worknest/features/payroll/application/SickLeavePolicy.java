package com.worknest.features.payroll.application;

import com.worknest.domain.entities.Employee;
import com.worknest.domain.entities.LeaveRequest;
import com.worknest.features.payroll.dto.PayrollDtos.SickLeaveCalculationDetails;
import java.util.List;

public interface SickLeavePolicy {

    /**
     * @param sickLeavesInMonth  approved sick leaves overlapping the payroll period
     * @param sickLeavesInYear   all approved sick leaves in the calendar year (used for YTD employer-liability tracking)
     */
    SickLeaveCalculationDetails calculate(
            Employee employee,
            List<LeaveRequest> sickLeavesInMonth,
            List<LeaveRequest> sickLeavesInYear,
            PayrollContext context
    );
}

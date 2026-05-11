package com.worknest.features.payroll.application;

import com.worknest.domain.entities.Employee;
import com.worknest.domain.entities.LeaveRequest;
import com.worknest.features.payroll.dto.PayrollDtos.SickLeaveCalculationDetails;
import java.util.List;

public interface SickLeavePolicy {

    SickLeaveCalculationDetails calculate(Employee employee, List<LeaveRequest> sickLeaves, PayrollContext context);
}

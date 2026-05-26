package com.worknest.features.payroll.application;

import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.Employee;
import com.worknest.domain.entities.LeaveRequest;
import com.worknest.domain.enums.LeaveType;
import com.worknest.domain.enums.PaymentMethod;
import com.worknest.features.payroll.repository.CompanySickLeavePolicyConfigRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanySickLeavePolicyTest {

    @Mock
    private CompanySickLeavePolicyConfigRepository policyConfigRepository;

    @Mock
    private WorkingDayCalculator workingDayCalculator;

    @Test
    void sickLeaveCountingUsesCompanyCalendarAndExcludesPaidPublicHolidays() {
        Company company = new Company();
        company.setId(UUID.randomUUID());
        Employee employee = new Employee();
        employee.setId(UUID.randomUUID());
        employee.setCompany(company);
        employee.setPaymentMethod(PaymentMethod.FIXED_MONTHLY);
        employee.setMonthlySalary(new BigDecimal("2200.00"));
        PayrollContext context = new PayrollContext(
                YearMonth.of(2026, 5),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                31,
                21,
                new BigDecimal("8.0"),
                "EUR");
        LeaveRequest sick = leave(employee, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 3));

        when(policyConfigRepository.findByCompanyId(company.getId())).thenReturn(Optional.empty());
        when(workingDayCalculator.isWorkingDay(company.getId(), LocalDate.of(2026, 5, 1))).thenReturn(false);
        when(workingDayCalculator.isWorkingDay(company.getId(), LocalDate.of(2026, 5, 2))).thenReturn(true);
        when(workingDayCalculator.isPaidHoliday(company.getId(), LocalDate.of(2026, 5, 2))).thenReturn(true);
        when(workingDayCalculator.isWorkingDay(company.getId(), LocalDate.of(2026, 5, 3))).thenReturn(true);
        when(workingDayCalculator.isPaidHoliday(company.getId(), LocalDate.of(2026, 5, 3))).thenReturn(false);

        var result = new CompanySickLeavePolicy(policyConfigRepository, workingDayCalculator)
                .calculate(employee, List.of(sick), List.of(sick), context);

        assertThat(result.daysTakenThisMonth()).isEqualByComparingTo("1");
        assertThat(result.companyPaidDays()).isEqualByComparingTo("1");
    }

    private LeaveRequest leave(Employee employee, LocalDate start, LocalDate end) {
        LeaveRequest leave = new LeaveRequest();
        leave.setId(UUID.randomUUID());
        leave.setCompany(employee.getCompany());
        leave.setEmployee(employee);
        leave.setLeaveType(LeaveType.SICK);
        leave.setStartDate(start);
        leave.setEndDate(end);
        return leave;
    }
}

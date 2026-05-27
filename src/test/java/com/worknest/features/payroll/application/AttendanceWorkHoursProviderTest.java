package com.worknest.features.payroll.application;

import com.worknest.domain.entities.AttendanceDayRecord;
import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.Employee;
import com.worknest.domain.enums.AttendanceDayStatus;
import com.worknest.domain.enums.PaymentMethod;
import com.worknest.features.attendance.repository.AttendanceDayRecordRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceWorkHoursProviderTest {

    @Mock
    private AttendanceDayRecordRepository attendanceRepository;

    @Mock
    private WorkingDayCalculator workingDayCalculator;

    @Test
    void returnsZeroPayWhenNoAttendanceRecordsExist() {
        Company company = company();
        Employee employee = employee(company);
        PayrollContext context = context();
        AttendanceWorkHoursProvider provider = new AttendanceWorkHoursProvider(attendanceRepository, workingDayCalculator);

        when(attendanceRepository.findAllByCompanyIdAndEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(
                company.getId(), employee.getId(), LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30)))
                .thenReturn(List.of());

        WorkHoursProvider.WorkHoursResult result = provider.getWorkedHours(
                employee, context, new BigDecimal("22"), LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30));

        assertThat(result.source()).isEqualTo(AttendanceWorkHoursProvider.SOURCE);
        assertThat(result.attendanceRecordsFound()).isFalse();
        assertThat(result.hours()).isEqualByComparingTo("0.00");
    }

    @Test
    void zeroMinuteAttendanceRecordsRemainZeroInsteadOfDefaultingToFullHours() {
        Company company = company();
        Employee employee = employee(company);
        PayrollContext context = context();
        AttendanceWorkHoursProvider provider = new AttendanceWorkHoursProvider(attendanceRepository, workingDayCalculator);
        AttendanceDayRecord absent = new AttendanceDayRecord();
        absent.setWorkDate(LocalDate.of(2026, 4, 1));
        absent.setDayStatus(AttendanceDayStatus.ABSENT);
        absent.setWorkedMinutes(0);

        when(attendanceRepository.findAllByCompanyIdAndEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(
                company.getId(), employee.getId(), LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30)))
                .thenReturn(List.of(absent));

        WorkHoursProvider.WorkHoursResult result = provider.getWorkedHours(
                employee, context, new BigDecimal("22"), LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30));

        assertThat(result.source()).isEqualTo(AttendanceWorkHoursProvider.SOURCE);
        assertThat(result.attendanceRecordsFound()).isTrue();
        assertThat(result.hours()).isEqualByComparingTo("0.00");
    }

    private Company company() {
        Company company = new Company();
        company.setId(UUID.randomUUID());
        return company;
    }

    private Employee employee(Company company) {
        Employee employee = new Employee();
        employee.setId(UUID.randomUUID());
        employee.setCompany(company);
        employee.setPaymentMethod(PaymentMethod.HOURLY);
        employee.setDailyWorkingHours(new BigDecimal("8.0"));
        return employee;
    }

    private PayrollContext context() {
        return new PayrollContext(
                YearMonth.of(2026, 4),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                30,
                22,
                new BigDecimal("8.0"),
                "EUR"
        );
    }
}

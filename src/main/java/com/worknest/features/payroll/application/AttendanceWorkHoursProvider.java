package com.worknest.features.payroll.application;

import com.worknest.domain.entities.AttendanceDayRecord;
import com.worknest.domain.entities.Employee;
import com.worknest.domain.enums.AttendanceDayStatus;
import com.worknest.domain.enums.PaymentMethod;
import com.worknest.features.attendance.repository.AttendanceDayRecordRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Production WorkHoursProvider that queries actual attendance records for hourly employees.
 * Fixed-monthly employees continue to use working-day proration (attendance irrelevant for base pay).
 */
@Primary
@Component
@RequiredArgsConstructor
public class AttendanceWorkHoursProvider implements WorkHoursProvider {

    public static final String SOURCE = "ATTENDANCE_RECORDS";
    public static final Set<AttendanceDayStatus> WORKED_STATUSES = Set.of(
            AttendanceDayStatus.PRESENT,
            AttendanceDayStatus.LATE,
            AttendanceDayStatus.HALF_DAY
    );

    private static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(60);

    private final AttendanceDayRecordRepository attendanceRepository;

    @Override
    public WorkHoursResult getWorkedHours(Employee employee, PayrollContext context,
                                          BigDecimal payableWorkingDays, LocalDate payableFrom, LocalDate payableTo) {
        if (employee.getPaymentMethod() != PaymentMethod.HOURLY) {
            return new WorkHoursResult(
                    payableWorkingDays.multiply(context.defaultDailyWorkingHours()),
                    DefaultWorkHoursProvider.SOURCE);
        }

        LocalDate effectiveTo = payableTo.isAfter(LocalDate.now()) ? LocalDate.now() : payableTo;
        BigDecimal hours = computeWorkedHours(
                employee.getCompany().getId(), employee.getId(), payableFrom, effectiveTo);

        if (hours.signum() == 0) {
            // No attendance records: fall back to default so the payroll does not produce zero silently.
            return new WorkHoursResult(
                    payableWorkingDays.multiply(context.defaultDailyWorkingHours()),
                    DefaultWorkHoursProvider.SOURCE);
        }
        return new WorkHoursResult(hours, SOURCE);
    }

    public BigDecimal computeWorkedHours(java.util.UUID companyId, java.util.UUID employeeId,
                                         LocalDate from, LocalDate to) {
        List<AttendanceDayRecord> records = attendanceRepository
                .findAllByCompanyIdAndEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(
                        companyId, employeeId, from, to);
        long totalMinutes = records.stream()
                .filter(r -> WORKED_STATUSES.contains(r.getDayStatus()))
                .mapToLong(r -> r.getWorkedMinutes() != null ? r.getWorkedMinutes() : 0L)
                .sum();
        return BigDecimal.valueOf(totalMinutes)
                .divide(MINUTES_PER_HOUR, 2, RoundingMode.HALF_UP);
    }
}

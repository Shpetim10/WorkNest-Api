package com.worknest.features.payroll.application;

import com.worknest.domain.entities.AttendanceDayRecord;
import com.worknest.domain.entities.Employee;
import com.worknest.domain.enums.AttendanceDayStatus;
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
            AttendanceDayStatus.HALF_DAY,
            AttendanceDayStatus.MISSING_CHECKOUT,
            AttendanceDayStatus.FLAGGED,
            AttendanceDayStatus.PENDING_REVIEW
    );

    private static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(60);

    private final AttendanceDayRecordRepository attendanceRepository;
    private final WorkingDayCalculator workingDayCalculator;

    @Override
    public WorkHoursResult getWorkedHours(Employee employee, PayrollContext context,
                                          BigDecimal payableWorkingDays, LocalDate payableFrom, LocalDate payableTo) {
        LocalDate effectiveTo = payableTo.isAfter(LocalDate.now()) ? LocalDate.now() : payableTo;
        List<AttendanceDayRecord> records = attendanceRepository
                .findAllByCompanyIdAndEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(
                        employee.getCompany().getId(), employee.getId(), payableFrom, effectiveTo);

        if (records.isEmpty()) {
            return new WorkHoursResult(BigDecimal.ZERO, SOURCE, false, BigDecimal.ZERO);
        }

        BigDecimal hours = sumWorkedHours(records);
        BigDecimal paidHolidayWorkedHours = sumPaidHolidayWorkedHours(employee, records);
        return new WorkHoursResult(hours, SOURCE, true, paidHolidayWorkedHours);
    }

    public BigDecimal computeWorkedHours(java.util.UUID companyId, java.util.UUID employeeId,
                                         LocalDate from, LocalDate to) {
        List<AttendanceDayRecord> records = attendanceRepository
                .findAllByCompanyIdAndEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(
                        companyId, employeeId, from, to);
        return sumWorkedHours(records);
    }

    private BigDecimal sumWorkedHours(List<AttendanceDayRecord> records) {
        long totalMinutes = records.stream()
                .filter(r -> WORKED_STATUSES.contains(r.getDayStatus()))
                .mapToLong(r -> r.getWorkedMinutes() != null ? r.getWorkedMinutes() : 0L)
                .sum();
        return BigDecimal.valueOf(totalMinutes)
                .divide(MINUTES_PER_HOUR, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumPaidHolidayWorkedHours(Employee employee, List<AttendanceDayRecord> records) {
        long totalMinutes = records.stream()
                .filter(r -> workingDayCalculator.isPaidHoliday(employee.getCompany().getId(), r.getWorkDate()))
                .filter(r -> WORKED_STATUSES.contains(r.getDayStatus()))
                .mapToLong(r -> r.getWorkedMinutes() != null ? r.getWorkedMinutes() : 0L)
                .sum();
        return BigDecimal.valueOf(totalMinutes)
                .divide(MINUTES_PER_HOUR, 2, RoundingMode.HALF_UP);
    }
}

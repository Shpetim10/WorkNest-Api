package com.worknest.features.payroll.application;

import com.worknest.domain.entities.CompanySickLeavePolicyConfig;
import com.worknest.domain.entities.Employee;
import com.worknest.domain.entities.LeaveRequest;
import com.worknest.domain.enums.PaymentMethod;
import com.worknest.features.payroll.dto.PayrollDtos.SickLeaveCalculationDetails;
import com.worknest.features.payroll.repository.CompanySickLeavePolicyConfigRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Applies the employer sick leave liability rule:
 * - Days 1–N (configurable, default 14) per calendar year: company pays at a configurable % of base daily wage.
 * - Days beyond that: state insurance liability (tracked, excluded from company payroll cost).
 * Falls back to {@link PlaceholderSickLeavePolicy} when no policy is configured for the company.
 */
@Primary
@Component
@RequiredArgsConstructor
public class CompanySickLeavePolicy implements SickLeavePolicy {

    private final CompanySickLeavePolicyConfigRepository policyConfigRepository;
    private final PlaceholderSickLeavePolicy placeholder = new PlaceholderSickLeavePolicy();

    @Override
    public SickLeaveCalculationDetails calculate(
            Employee employee,
            List<LeaveRequest> sickLeavesInMonth,
            List<LeaveRequest> sickLeavesInYear,
            PayrollContext context
    ) {
        Optional<CompanySickLeavePolicyConfig> configOpt =
                policyConfigRepository.findByCompanyId(employee.getCompany().getId());

        if (configOpt.isEmpty()) {
            return placeholder.calculate(employee, sickLeavesInMonth, sickLeavesInYear, context);
        }

        CompanySickLeavePolicyConfig config = configOpt.get();

        // Sick days taken so far this calendar year, BEFORE this payroll period
        LocalDate yearStart = context.periodStart().withDayOfYear(1);
        LocalDate dayBeforePeriod = context.periodStart().minusDays(1);
        BigDecimal ytdDaysBefore = countWorkingDays(sickLeavesInYear, yearStart, dayBeforePeriod);

        // Sick days taken within this payroll period
        BigDecimal daysThisMonth = countWorkingDays(sickLeavesInMonth, context.periodStart(), context.periodEnd());

        int maxCompanyDays = config.getMaxCompanyPaidDays();

        // How many of the employer-liability days remain after YTD usage
        BigDecimal companyAlreadyUsed = ytdDaysBefore.min(BigDecimal.valueOf(maxCompanyDays));
        BigDecimal companyCapacityRemaining = BigDecimal.valueOf(maxCompanyDays)
                .subtract(companyAlreadyUsed)
                .max(BigDecimal.ZERO);

        BigDecimal companyPaidDays = daysThisMonth.min(companyCapacityRemaining);
        BigDecimal statePaidDays = daysThisMonth.subtract(companyPaidDays).max(BigDecimal.ZERO);

        BigDecimal dailyPay = dailyPayValue(employee, context);
        BigDecimal percentage = config.getCompanyPaidPercentage();
        BigDecimal companyPaidAmount = dailyPay
                .multiply(companyPaidDays)
                .multiply(percentage.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP))
                .setScale(2, RoundingMode.HALF_UP);

        // State insurance amount is a TODO — integration with insurance provider is pending
        return new SickLeaveCalculationDetails(
                daysThisMonth,
                companyPaidDays,
                percentage,
                companyPaidAmount,
                statePaidDays,
                null,
                "COMPANY_POLICY_APPLIED"
        );
    }

    private BigDecimal countWorkingDays(List<LeaveRequest> leaves, LocalDate rangeStart, LocalDate rangeEnd) {
        if (rangeEnd.isBefore(rangeStart)) {
            return BigDecimal.ZERO;
        }
        Set<LocalDate> uniqueDays = new HashSet<>();
        for (LeaveRequest leave : leaves) {
            LocalDate from = PayrollDateUtils.max(leave.getStartDate(), rangeStart);
            LocalDate to = PayrollDateUtils.min(leave.getEndDate(), rangeEnd);
            LocalDate cursor = from;
            while (!cursor.isAfter(to)) {
                if (PayrollDateUtils.isWorkingDay(cursor)) {
                    uniqueDays.add(cursor);
                }
                cursor = cursor.plusDays(1);
            }
        }
        return BigDecimal.valueOf(uniqueDays.size());
    }

    private BigDecimal dailyPayValue(Employee employee, PayrollContext context) {
        BigDecimal dailyHours = employee.getDailyWorkingHours() != null
                ? employee.getDailyWorkingHours()
                : context.defaultDailyWorkingHours();
        BigDecimal hourlyRate;
        if (employee.getPaymentMethod() == PaymentMethod.FIXED_MONTHLY) {
            BigDecimal monthlyHours = BigDecimal.valueOf(context.workingDaysInMonth()).multiply(dailyHours);
            hourlyRate = employee.getMonthlySalary().divide(monthlyHours, 8, RoundingMode.HALF_UP);
        } else {
            hourlyRate = employee.getHourlyRate();
        }
        return hourlyRate.multiply(dailyHours).setScale(2, RoundingMode.HALF_UP);
    }
}

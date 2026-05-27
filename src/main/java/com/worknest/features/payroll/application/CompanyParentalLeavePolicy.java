package com.worknest.features.payroll.application;

import com.worknest.domain.entities.CompanyParentalLeavePolicyConfig;
import com.worknest.domain.entities.Employee;
import com.worknest.domain.entities.LeaveRequest;
import com.worknest.domain.enums.PaymentMethod;
import com.worknest.features.payroll.dto.PayrollDtos.ParentalLeaveCalculationDetails;
import com.worknest.features.payroll.repository.CompanyParentalLeavePolicyConfigRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Applies the employer parental leave liability rule:
 * - Days 1–N (configurable, default 90) per calendar year: company pays at a configurable % of base daily wage.
 * - Days beyond that: state insurance liability (tracked, excluded from company payroll cost).
 */
@Primary
@Component
@RequiredArgsConstructor
public class CompanyParentalLeavePolicy implements ParentalLeavePolicy {

    private static final BigDecimal DEFAULT_COMPANY_PAID_PERCENTAGE = new BigDecimal("80.00");
    private static final int DEFAULT_MAX_COMPANY_PAID_DAYS = 90;

    private final CompanyParentalLeavePolicyConfigRepository policyConfigRepository;
    private final WorkingDayCalculator workingDayCalculator;

    @Override
    public ParentalLeaveCalculationDetails calculate(
            Employee employee,
            List<LeaveRequest> parentalLeavesInMonth,
            List<LeaveRequest> parentalLeavesInYear,
            PayrollContext context
    ) {
        CompanyParentalLeavePolicyConfig config = policyConfigRepository
                .findByCompanyId(employee.getCompany().getId())
                .orElseGet(() -> defaultConfig(employee));

        LocalDate yearStart = context.periodStart().withDayOfYear(1);
        LocalDate dayBeforePeriod = context.periodStart().minusDays(1);
        BigDecimal ytdDaysBefore = countWorkingDays(
                employee.getCompany().getId(), parentalLeavesInYear, yearStart, dayBeforePeriod);

        BigDecimal daysThisMonth = countWorkingDays(
                employee.getCompany().getId(), parentalLeavesInMonth, context.periodStart(), context.periodEnd());

        int maxCompanyDays = config.getMaxCompanyPaidDays();
        BigDecimal companyAlreadyUsed = ytdDaysBefore.min(BigDecimal.valueOf(maxCompanyDays));
        BigDecimal companyCapacityRemaining = BigDecimal.valueOf(maxCompanyDays)
                .subtract(companyAlreadyUsed)
                .max(BigDecimal.ZERO);

        BigDecimal companyPaidDays = daysThisMonth.min(companyCapacityRemaining);
        BigDecimal statePaidDays = daysThisMonth.subtract(companyPaidDays).max(BigDecimal.ZERO);

        BigDecimal percentage = config.getCompanyPaidPercentage();
        BigDecimal rate = percentage.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
        BigDecimal unpaidRate = BigDecimal.ONE.subtract(rate);

        BigDecimal dailyPay = dailyPayValue(employee, context);
        BigDecimal dailyHours = context.defaultDailyWorkingHours();

        if (employee.getPaymentMethod() == PaymentMethod.FIXED_MONTHLY) {
            BigDecimal paidParentalDeductionEquivalent = dailyPay.multiply(companyPaidDays)
                    .multiply(unpaidRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal stateParentalDeduction = dailyPay.multiply(statePaidDays)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalParentalDeduction = paidParentalDeductionEquivalent.add(stateParentalDeduction)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal companyPaidAmount = dailyPay.multiply(companyPaidDays).multiply(rate)
                    .setScale(2, RoundingMode.HALF_UP);

            return new ParentalLeaveCalculationDetails(
                    daysThisMonth,
                    companyPaidDays,
                    statePaidDays,
                    percentage,
                    companyPaidAmount,
                    paidParentalDeductionEquivalent,
                    totalParentalDeduction,
                    null, null, null,
                    null, null,
                    "COMPANY_POLICY_APPLIED"
            );
        }

        // HOURLY
        BigDecimal paidParentalHours = companyPaidDays.multiply(dailyHours);
        BigDecimal stateParentalHours = statePaidDays.multiply(dailyHours);
        BigDecimal companyPaidAmount = paidParentalHours.multiply(employee.getHourlyRate())
                .multiply(rate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal paidParentalDeductionEquivalent = paidParentalHours.multiply(employee.getHourlyRate())
                .multiply(unpaidRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal stateParentalAmount = stateParentalHours.multiply(employee.getHourlyRate())
                .setScale(2, RoundingMode.HALF_UP);

        return new ParentalLeaveCalculationDetails(
                daysThisMonth,
                companyPaidDays,
                statePaidDays,
                percentage,
                companyPaidAmount,
                paidParentalDeductionEquivalent,
                null,
                paidParentalHours,
                stateParentalHours,
                stateParentalAmount,
                null, null,
                "COMPANY_POLICY_APPLIED"
        );
    }

    private BigDecimal countWorkingDays(
            java.util.UUID companyId,
            List<LeaveRequest> leaves,
            LocalDate rangeStart,
            LocalDate rangeEnd
    ) {
        if (rangeEnd.isBefore(rangeStart)) {
            return BigDecimal.ZERO;
        }
        Set<LocalDate> uniqueDays = new HashSet<>();
        for (LeaveRequest leave : leaves) {
            LocalDate from = PayrollDateUtils.max(leave.getStartDate(), rangeStart);
            LocalDate to = PayrollDateUtils.min(leave.getEndDate(), rangeEnd);
            LocalDate cursor = from;
            while (!cursor.isAfter(to)) {
                if (workingDayCalculator.isWorkingDay(companyId, cursor)
                        && !workingDayCalculator.isPaidHoliday(companyId, cursor)) {
                    uniqueDays.add(cursor);
                }
                cursor = cursor.plusDays(1);
            }
        }
        return BigDecimal.valueOf(uniqueDays.size());
    }

    private CompanyParentalLeavePolicyConfig defaultConfig(Employee employee) {
        CompanyParentalLeavePolicyConfig cfg = new CompanyParentalLeavePolicyConfig();
        cfg.setCompany(employee.getCompany());
        cfg.setCompanyPaidPercentage(DEFAULT_COMPANY_PAID_PERCENTAGE);
        cfg.setMaxCompanyPaidDays(DEFAULT_MAX_COMPANY_PAID_DAYS);
        return cfg;
    }

    private BigDecimal dailyPayValue(Employee employee, PayrollContext context) {
        if (employee.getPaymentMethod() == PaymentMethod.FIXED_MONTHLY) {
            return employee.getMonthlySalary()
                    .divide(BigDecimal.valueOf(context.workingDaysInMonth()), 8, RoundingMode.HALF_UP);
        }
        return employee.getHourlyRate().multiply(context.defaultDailyWorkingHours()).setScale(2, RoundingMode.HALF_UP);
    }
}

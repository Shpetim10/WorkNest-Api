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
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Applies the employer sick leave liability rule:
 * - Days 1–N (configurable, default 14) per calendar year: company pays at a configurable % of base daily wage.
 * - Days beyond that: state insurance liability (tracked, excluded from company payroll cost).
 * When no policy is configured for the company, the system default (70% / 14 days) is applied,
 * consistent with the default returned by getSickLeavePolicy().
 */
@Primary
@Component
@RequiredArgsConstructor
public class CompanySickLeavePolicy implements SickLeavePolicy {

    private static final BigDecimal DEFAULT_COMPANY_PAID_PERCENTAGE = new BigDecimal("70.00");
    private static final int DEFAULT_MAX_COMPANY_PAID_DAYS = 14;

    private final CompanySickLeavePolicyConfigRepository policyConfigRepository;

    @Override
    public SickLeaveCalculationDetails calculate(
            Employee employee,
            List<LeaveRequest> sickLeavesInMonth,
            List<LeaveRequest> sickLeavesInYear,
            PayrollContext context
    ) {
        CompanySickLeavePolicyConfig config = policyConfigRepository
                .findByCompanyId(employee.getCompany().getId())
                .orElseGet(() -> defaultConfig(employee));

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
        BigDecimal unpaidSickLeaveDays = daysThisMonth.subtract(companyPaidDays).max(BigDecimal.ZERO);
        BigDecimal statePaidDays = unpaidSickLeaveDays; // state covers days beyond employer cap

        BigDecimal percentage = config.getCompanyPaidPercentage();
        BigDecimal rate = percentage.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
        BigDecimal unpaidRate = BigDecimal.ONE.subtract(rate);

        BigDecimal dailyPay = dailyPayValue(employee, context);
        BigDecimal dailyHours = context.defaultDailyWorkingHours();

        if (employee.getPaymentMethod() == PaymentMethod.FIXED_MONTHLY) {
            // Option A: basePay already covers all days; deduct the unpaid sick portion.
            BigDecimal paidSickLeaveDeductionEquivalent = dailyPay.multiply(companyPaidDays)
                    .multiply(unpaidRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal unpaidSickLeaveDeduction = dailyPay.multiply(unpaidSickLeaveDays)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalSickLeaveDeduction = paidSickLeaveDeductionEquivalent.add(unpaidSickLeaveDeduction)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal companyPaidAmount = dailyPay.multiply(companyPaidDays).multiply(rate)
                    .setScale(2, RoundingMode.HALF_UP);

            return new SickLeaveCalculationDetails(
                    daysThisMonth,
                    companyPaidDays,
                    unpaidSickLeaveDays,
                    percentage,
                    companyPaidAmount,
                    paidSickLeaveDeductionEquivalent,
                    totalSickLeaveDeduction,
                    null, null, null, // hourly-specific fields
                    statePaidDays,
                    null,
                    "COMPANY_POLICY_APPLIED"
            );
        }

        // HOURLY: basePay = attendance hours × rate (sick hours absent from attendance).
        // Company pays for paid sick days at rate%; no separate deduction needed.
        BigDecimal paidSickLeaveHours = companyPaidDays.multiply(dailyHours);
        BigDecimal unpaidSickLeaveHours = unpaidSickLeaveDays.multiply(dailyHours);
        BigDecimal companyPaidAmount = paidSickLeaveHours.multiply(employee.getHourlyRate())
                .multiply(rate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal paidSickLeaveDeductionEquivalent = paidSickLeaveHours.multiply(employee.getHourlyRate())
                .multiply(unpaidRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal unpaidSickLeaveUnpaidAmount = unpaidSickLeaveHours.multiply(employee.getHourlyRate())
                .setScale(2, RoundingMode.HALF_UP);

        // State insurance amount is a TODO — integration with insurance provider is pending
        return new SickLeaveCalculationDetails(
                daysThisMonth,
                companyPaidDays,
                unpaidSickLeaveDays,
                percentage,
                companyPaidAmount,
                paidSickLeaveDeductionEquivalent,
                null, // totalSickLeaveDeduction not used for hourly (no deduction path)
                paidSickLeaveHours,
                unpaidSickLeaveHours,
                unpaidSickLeaveUnpaidAmount,
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

    private CompanySickLeavePolicyConfig defaultConfig(Employee employee) {
        CompanySickLeavePolicyConfig cfg = new CompanySickLeavePolicyConfig();
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

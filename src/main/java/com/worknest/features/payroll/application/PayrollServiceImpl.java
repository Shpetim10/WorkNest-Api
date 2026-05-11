package com.worknest.features.payroll.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.worknest.common.exception.BusinessException;
import com.worknest.domain.entities.Employee;
import com.worknest.domain.entities.LeaveRequest;
import com.worknest.domain.entities.PayrollAdjustment;
import com.worknest.domain.entities.PayrollResult;
import com.worknest.domain.entities.User;
import com.worknest.domain.enums.PayrollAdjustmentType;
import com.worknest.domain.enums.PayrollCalculationStatus;
import com.worknest.domain.enums.PayrollStatus;
import com.worknest.features.auth.repository.UserRepository;
import com.worknest.features.employee.repository.EmployeeRepository;
import com.worknest.features.leave.repository.LeaveRequestRepository;
import com.worknest.features.payroll.dto.PayrollDtos.BatchPayrollCalculationRequest;
import com.worknest.features.payroll.dto.PayrollDtos.BatchPayrollCalculationResponse;
import com.worknest.features.payroll.dto.PayrollDtos.BatchPayrollEmployeeResult;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollAdjustmentRequest;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollAdjustmentResponse;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollCalculationResponse;
import com.worknest.features.payroll.repository.PayrollAdjustmentRepository;
import com.worknest.features.payroll.repository.PayrollResultRepository;
import com.worknest.security.AuthSessionPrincipal;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PayrollServiceImpl implements PayrollService {

    private static final Set<PayrollStatus> LOCKED_STATUSES = Set.of(
            PayrollStatus.APPROVED,
            PayrollStatus.FINALIZED,
            PayrollStatus.PAID
    );

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final PayrollAdjustmentRepository adjustmentRepository;
    private final PayrollResultRepository resultRepository;
    private final PayrollCalculationEngine calculationEngine;
    private final ObjectMapper objectMapper;

    @Override
    public PayrollAdjustmentResponse addBonus(UUID employeeId, PayrollAdjustmentRequest request) {
        return addAdjustment(employeeId, request, PayrollAdjustmentType.BONUS);
    }

    @Override
    public PayrollAdjustmentResponse addDeduction(UUID employeeId, PayrollAdjustmentRequest request) {
        return addAdjustment(employeeId, request, PayrollAdjustmentType.DEDUCTION);
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollCalculationResponse previewEmployeePayroll(UUID employeeId, int year, int month) {
        AuthSessionPrincipal principal = principal();
        YearMonth payrollMonth = payrollMonth(year, month);
        Employee employee = loadEmployee(principal.companyId(), employeeId);
        PayrollStatus status = resultRepository.findByCompanyIdAndEmployeeIdAndYearAndMonth(
                        principal.companyId(), employeeId, year, month)
                .map(PayrollResult::getStatus)
                .orElse(PayrollStatus.DRAFT);
        return calculate(employee, payrollMonth, status, true);
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollCalculationResponse previewCurrentEmployeePayroll(int year, int month) {
        AuthSessionPrincipal principal = principal();
        YearMonth payrollMonth = payrollMonth(year, month);
        Employee employee = employeeRepository.findByUserIdAndCompanyId(principal.userId(), principal.companyId())
                .orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "EMPLOYEE_PROFILE_NOT_FOUND",
                        "Employee profile is not configured."));
        PayrollStatus status = resultRepository.findByCompanyIdAndEmployeeIdAndYearAndMonth(
                        principal.companyId(), employee.getId(), year, month)
                .map(PayrollResult::getStatus)
                .orElse(PayrollStatus.DRAFT);
        return calculate(employee, payrollMonth, status, true);
    }

    @Override
    public PayrollCalculationResponse calculateEmployeePayroll(UUID employeeId, int year, int month) {
        AuthSessionPrincipal principal = principal();
        User actor = loadUser(principal.userId());
        YearMonth payrollMonth = payrollMonth(year, month);
        Employee employee = loadEmployee(principal.companyId(), employeeId);
        ensurePayrollOpen(principal.companyId(), employeeId, year, month);
        PayrollCalculationResponse response = calculate(employee, payrollMonth, PayrollStatus.CALCULATED, false);
        persistResult(principal.companyId(), employee, actor, response);
        return response;
    }

    @Override
    public BatchPayrollCalculationResponse calculateBatch(BatchPayrollCalculationRequest request) {
        AuthSessionPrincipal principal = principal();
        YearMonth payrollMonth = payrollMonth(request.year(), request.month());
        List<Employee> employees = loadBatchEmployees(principal.companyId(), request.employeeIds());
        List<BatchPayrollEmployeeResult> results = new ArrayList<>();

        for (Employee employee : employees) {
            try {
                if (employee.getStartDate() != null && employee.getStartDate().isAfter(payrollMonth.atEndOfMonth())) {
                    results.add(new BatchPayrollEmployeeResult(employee.getId(), PayrollCalculationStatus.SKIPPED,
                            null, "NO_ACTIVE_CONTRACT_IN_PERIOD", "Employee starts after the payroll period."));
                    continue;
                }
                PayrollCalculationResponse calculated = calculateEmployeePayroll(
                        employee.getId(), request.year(), request.month());
                results.add(new BatchPayrollEmployeeResult(employee.getId(), PayrollCalculationStatus.SUCCESS,
                        calculated.totals().grossEarnings(), null, null));
            } catch (PayrollCalculationException exception) {
                results.add(new BatchPayrollEmployeeResult(employee.getId(), PayrollCalculationStatus.FAILED,
                        null, exception.getCode(), exception.getMessage()));
            } catch (BusinessException exception) {
                results.add(new BatchPayrollEmployeeResult(employee.getId(), PayrollCalculationStatus.FAILED,
                        null, exception.getCode(), exception.getMessage()));
            }
        }

        int success = (int) results.stream().filter(r -> r.status() == PayrollCalculationStatus.SUCCESS).count();
        int failed = (int) results.stream().filter(r -> r.status() == PayrollCalculationStatus.FAILED).count();
        int skipped = (int) results.stream().filter(r -> r.status() == PayrollCalculationStatus.SKIPPED).count();
        return new BatchPayrollCalculationResponse(
                request.year(), request.month(), employees.size(), success, failed, skipped, results);
    }

    private PayrollAdjustmentResponse addAdjustment(
            UUID employeeId,
            PayrollAdjustmentRequest request,
            PayrollAdjustmentType type
    ) {
        AuthSessionPrincipal principal = principal();
        YearMonth payrollMonth = payrollMonth(request.year(), request.month());
        Employee employee = loadEmployee(principal.companyId(), employeeId);
        ensurePayrollOpen(principal.companyId(), employeeId, payrollMonth.getYear(), payrollMonth.getMonthValue());
        User actor = loadUser(principal.userId());

        if (request.amount() == null || request.amount().signum() <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_PAYROLL_ADJUSTMENT_AMOUNT",
                    "Payroll adjustment amount must be positive.");
        }

        PayrollAdjustment adjustment = new PayrollAdjustment();
        adjustment.setCompany(employee.getCompany());
        adjustment.setEmployee(employee);
        adjustment.setYear(payrollMonth.getYear());
        adjustment.setMonth(payrollMonth.getMonthValue());
        adjustment.setType(type);
        adjustment.setAmount(request.amount().setScale(2, java.math.RoundingMode.HALF_UP));
        adjustment.setReason(request.reason());
        adjustment.setNotes(request.notes());
        adjustment.setCreatedByUser(actor);
        return toAdjustmentResponse(adjustmentRepository.save(adjustment));
    }

    private PayrollCalculationResponse calculate(
            Employee employee,
            YearMonth payrollMonth,
            PayrollStatus payrollStatus,
            boolean preview
    ) {
        List<LeaveRequest> approvedInMonth = leaveRequestRepository.findApprovedOverlappingPayrollPeriod(
                employee.getCompany().getId(), employee.getId(), payrollMonth.atDay(1), payrollMonth.atEndOfMonth());
        List<LeaveRequest> approvedInYear = leaveRequestRepository.findApprovedOverlappingRange(
                employee.getCompany().getId(), employee.getId(), payrollMonth.atDay(1).withDayOfYear(1), payrollMonth.atEndOfMonth());
        List<PayrollAdjustment> adjustments = adjustmentRepository
                .findAllByCompanyIdAndEmployeeIdAndYearAndMonthOrderByCreatedAtAsc(
                        employee.getCompany().getId(), employee.getId(), payrollMonth.getYear(), payrollMonth.getMonthValue());
        return calculationEngine.calculate(employee, payrollMonth, approvedInYear, approvedInMonth, adjustments, payrollStatus, preview);
    }

    private void persistResult(UUID companyId, Employee employee, User actor, PayrollCalculationResponse response) {
        PayrollResult result = resultRepository.findByCompanyIdAndEmployeeIdAndYearAndMonth(
                        companyId, employee.getId(), response.year(), response.month())
                .orElseGet(PayrollResult::new);
        result.setCompany(employee.getCompany());
        result.setEmployee(employee);
        result.setYear(response.year());
        result.setMonth(response.month());
        result.setStatus(PayrollStatus.CALCULATED);
        result.setBasePay(response.totals().basePay());
        result.setGrossEarnings(response.totals().grossEarnings());
        result.setTotalDeductions(response.totals().totalDeductions());
        result.setNetPay(response.totals().grossEarnings().subtract(response.totals().totalDeductions()));
        result.setCalculationSnapshotJson(toJson(response));
        result.setCalculatedAt(Instant.now());
        result.setCalculatedByUser(actor);
        resultRepository.save(result);
    }

    private String toJson(PayrollCalculationResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "PAYROLL_SNAPSHOT_SERIALIZATION_FAILED",
                    "Payroll calculation snapshot could not be stored.");
        }
    }

    private List<Employee> loadBatchEmployees(UUID companyId, List<UUID> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return employeeRepository.findAllByCompanyId(companyId);
        }
        return employeeIds.stream().map(id -> loadEmployee(companyId, id)).toList();
    }

    private Employee loadEmployee(UUID companyId, UUID employeeId) {
        return employeeRepository.findByIdAndCompanyId(employeeId, companyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "EMPLOYEE_NOT_FOUND",
                        "Employee was not found."));
    }

    private User loadUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User was not found."));
    }

    private void ensurePayrollOpen(UUID companyId, UUID employeeId, int year, int month) {
        if (resultRepository.existsByCompanyIdAndEmployeeIdAndYearAndMonthAndStatusIn(
                companyId, employeeId, year, month, LOCKED_STATUSES)) {
            throw new BusinessException(HttpStatus.CONFLICT, "PAYROLL_PERIOD_LOCKED",
                    "Payroll period is approved, finalized, or paid and cannot be modified.");
        }
    }

    private YearMonth payrollMonth(int year, int month) {
        try {
            return YearMonth.of(year, month);
        } catch (RuntimeException exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_PAYROLL_PERIOD",
                    "Payroll year and month are invalid.");
        }
    }

    private PayrollAdjustmentResponse toAdjustmentResponse(PayrollAdjustment adjustment) {
        return new PayrollAdjustmentResponse(
                adjustment.getId(),
                adjustment.getEmployee().getId(),
                adjustment.getYear(),
                adjustment.getMonth(),
                adjustment.getType(),
                adjustment.getAmount(),
                adjustment.getReason(),
                adjustment.getNotes(),
                adjustment.getCreatedByUser().getId(),
                adjustment.getCreatedAt()
        );
    }

    private AuthSessionPrincipal principal() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthSessionPrincipal p)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "No authentication session found.");
        }
        return p;
    }
}

package com.worknest.features.payroll.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.worknest.audit.domain.AuditLog;
import com.worknest.audit.service.AuditLogService;
import com.worknest.common.api.PaginatedResponse;
import com.worknest.common.api.PaginationSupport;
import com.worknest.common.exception.BusinessException;
import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.CompanySickLeavePolicyConfig;
import com.worknest.domain.entities.Employee;
import com.worknest.domain.entities.LeaveBalance;
import com.worknest.domain.entities.LeaveRequest;
import com.worknest.domain.entities.PayrollAdjustment;
import com.worknest.domain.entities.PayrollResult;
import com.worknest.domain.entities.User;
import com.worknest.domain.enums.PayrollAdjustmentType;
import com.worknest.domain.enums.PayrollCalculationStatus;
import com.worknest.domain.enums.PayrollStatus;
import com.worknest.domain.enums.PlatformRole;
import com.worknest.features.attendance.repository.AttendanceDayRecordRepository;
import com.worknest.features.auth.repository.UserRepository;
import com.worknest.features.company.repository.CompanyRepository;
import com.worknest.features.employee.repository.EmployeeRepository;
import com.worknest.features.leave.repository.LeaveBalanceRepository;
import com.worknest.features.leave.repository.LeaveRequestRepository;
import com.worknest.features.payroll.dto.PayrollDtos.BatchPayrollCalculationRequest;
import com.worknest.features.payroll.dto.PayrollDtos.BatchPayrollCalculationResponse;
import com.worknest.features.payroll.dto.PayrollDtos.BatchPayrollEmployeeResult;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollAdjustmentRequest;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollAdjustmentResponse;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollCalculationResponse;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollEmployeeSummaryResponse;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollMonthSummary;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollPeriodRequest;
import com.worknest.features.payroll.dto.PayrollDtos.SickLeavePolicyResponse;
import com.worknest.features.payroll.dto.PayrollDtos.UpsertSickLeavePolicyRequest;
import com.worknest.features.payroll.repository.CompanySickLeavePolicyConfigRepository;
import com.worknest.features.payroll.repository.PayrollAdjustmentRepository;
import com.worknest.features.payroll.repository.PayrollResultRepository;
import com.worknest.security.AuthSessionPrincipal;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final PayrollAdjustmentRepository adjustmentRepository;
    private final PayrollResultRepository resultRepository;
    private final PayrollCalculationEngine calculationEngine;
    private final ObjectMapper objectMapper;
    private final CompanySickLeavePolicyConfigRepository sickLeavePolicyRepository;
    private final CompanyRepository companyRepository;
    private final AttendanceDayRecordRepository attendanceDayRecordRepository;
    private final AuditLogService auditLogService;

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
        assertEmployeeAccess(principal, employeeId);
        YearMonth payrollMonth = payrollMonth(year, month);
        Employee employee = loadEmployee(principal.companyId(), employeeId);
        return resultRepository.findByCompanyIdAndEmployeeIdAndYearAndMonth(
                        principal.companyId(), employeeId, year, month)
                .map(result -> LOCKED_STATUSES.contains(result.getStatus())
                        ? responseFromSnapshot(result, true)
                        : calculate(employee, payrollMonth, result.getStatus(), true))
                .orElseGet(() -> calculate(employee, payrollMonth, PayrollStatus.DRAFT, true));
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollCalculationResponse previewCurrentEmployeePayroll(int year, int month) {
        AuthSessionPrincipal principal = principal();
        YearMonth payrollMonth = payrollMonth(year, month);
        Employee employee = employeeRepository.findByUserIdAndCompanyId(principal.userId(), principal.companyId())
                .orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "EMPLOYEE_PROFILE_NOT_FOUND",
                        "Employee profile is not configured."));
        return resultRepository.findByCompanyIdAndEmployeeIdAndYearAndMonth(
                        principal.companyId(), employee.getId(), year, month)
                .map(result -> LOCKED_STATUSES.contains(result.getStatus())
                        ? responseFromSnapshot(result, true)
                        : calculate(employee, payrollMonth, result.getStatus(), true))
                .orElseGet(() -> calculate(employee, payrollMonth, PayrollStatus.DRAFT, true));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<PayrollEmployeeSummaryResponse> listAdminPayrollEmployees(
            int year, int month, String search, Integer page, Integer size) {
        AuthSessionPrincipal principal = principal();
        YearMonth payrollMonth = payrollMonth(year, month);
        Pageable pageable = PaginationSupport.pageable(page, size);
        String normalizedSearch = search == null ? null : search.trim();
        LocalDate periodStart = payrollMonth.atDay(1);
        LocalDate periodEnd = payrollMonth.atEndOfMonth();

        Page<Employee> employees;
        if (principal.role() == PlatformRole.STAFF) {
            // STAFF may only see their assigned employees
            employees = employeeRepository.findPayrollCandidatesForStaff(
                    principal.companyId(), principal.roleAssignmentId(),
                    normalizedSearch, periodStart, periodEnd, pageable);
        } else {
            employees = employeeRepository.findPayrollCandidatesForAdmin(
                    principal.companyId(), normalizedSearch, periodStart, periodEnd, pageable);
        }

        Page<PayrollEmployeeSummaryResponse> summaries = employees.map(employee -> {
            Optional<PayrollResult> result = resultRepository.findByCompanyIdAndEmployeeIdAndYearAndMonth(
                    principal.companyId(), employee.getId(), year, month);
            PayrollCalculationResponse payroll = resolvePayrollForSummary(employee, payrollMonth, result);
            return toPayrollEmployeeSummary(employee, payroll);
        });

        return PaginatedResponse.from(summaries);
    }

    @Override
    public PayrollCalculationResponse calculateEmployeePayroll(UUID employeeId, int year, int month) {
        AuthSessionPrincipal principal = principal();
        assertEmployeeAccess(principal, employeeId);
        User actor = loadUser(principal.userId());
        YearMonth payrollMonth = payrollMonth(year, month);
        Employee employee = loadEmployee(principal.companyId(), employeeId);
        ensurePayrollOpen(principal.companyId(), employeeId, year, month);
        PayrollCalculationResponse response = calculate(employee, payrollMonth, PayrollStatus.CALCULATED, false);
        persistResult(principal.companyId(), employee, actor, response);
        audit("PAYROLL_CALCULATED", "payroll_results", employee.getId(),
                Map.of("year", year, "month", month, "netPay", response.totals().netPay()),
                Map.of("companyId", principal.companyId(), "employeeId", employeeId, "actorUserId", actor.getId()));
        return response;
    }

    @Override
    public BatchPayrollCalculationResponse calculateBatch(BatchPayrollCalculationRequest request) {
        AuthSessionPrincipal principal = principal();
        YearMonth payrollMonth = payrollMonth(request.year(), request.month());
        List<Employee> employees = loadBatchEmployees(principal.companyId(), request.employeeIds(),
                payrollMonth.atDay(1), payrollMonth.atEndOfMonth());
        List<BatchPayrollEmployeeResult> results = new ArrayList<>();

        for (Employee employee : employees) {
            results.add(calculateBatchEmployee(principal, employee, request.year(), request.month(), payrollMonth));
        }

        int success = (int) results.stream().filter(r -> r.status() == PayrollCalculationStatus.SUCCESS).count();
        int failed = (int) results.stream().filter(r -> r.status() == PayrollCalculationStatus.FAILED).count();
        int skipped = (int) results.stream().filter(r -> r.status() == PayrollCalculationStatus.SKIPPED).count();
        return new BatchPayrollCalculationResponse(
                request.year(), request.month(), employees.size(), success, failed, skipped, results);
    }

    // M7: each employee is isolated in REQUIRES_NEW so one failure doesn't poison the batch
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BatchPayrollEmployeeResult calculateBatchEmployee(
            AuthSessionPrincipal principal, Employee employee, int year, int month, YearMonth payrollMonth) {
        try {
            if (employee.getStartDate() != null && employee.getStartDate().isAfter(payrollMonth.atEndOfMonth())) {
                return new BatchPayrollEmployeeResult(employee.getId(), PayrollCalculationStatus.SKIPPED,
                        null, "NO_ACTIVE_CONTRACT_IN_PERIOD", "Employee starts after the payroll period.");
            }
            if (employee.getContractExpiryDate() != null
                    && employee.getContractExpiryDate().isBefore(payrollMonth.atDay(1))) {
                return new BatchPayrollEmployeeResult(employee.getId(), PayrollCalculationStatus.SKIPPED,
                        null, "CONTRACT_EXPIRED", "Employee contract expired before the payroll period.");
            }
            if (resultRepository.existsByCompanyIdAndEmployeeIdAndYearAndMonthAndStatusIn(
                    principal.companyId(), employee.getId(), year, month, LOCKED_STATUSES)) {
                return new BatchPayrollEmployeeResult(employee.getId(), PayrollCalculationStatus.SKIPPED,
                        null, "PAYROLL_PERIOD_LOCKED", "Payroll is already approved, finalized, or paid.");
            }
            User actor = loadUser(principal.userId());
            PayrollCalculationResponse calculated = calculate(employee, payrollMonth, PayrollStatus.CALCULATED, false);
            persistResult(principal.companyId(), employee, actor, calculated);
            return new BatchPayrollEmployeeResult(employee.getId(), PayrollCalculationStatus.SUCCESS,
                    calculated.totals().grossEarnings(), null, null);
        } catch (PayrollCalculationException exception) {
            return new BatchPayrollEmployeeResult(employee.getId(), PayrollCalculationStatus.FAILED,
                    null, exception.getCode(), exception.getMessage());
        } catch (BusinessException exception) {
            return new BatchPayrollEmployeeResult(employee.getId(), PayrollCalculationStatus.FAILED,
                    null, exception.getCode(), exception.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SickLeavePolicyResponse getSickLeavePolicy() {
        UUID companyId = principal().companyId();
        return sickLeavePolicyRepository.findByCompanyId(companyId)
                .map(cfg -> new SickLeavePolicyResponse(cfg.getCompanyPaidPercentage(), cfg.getMaxCompanyPaidDays(), false))
                .orElse(new SickLeavePolicyResponse(new BigDecimal("70.00"), 14, true));
    }

    @Override
    public SickLeavePolicyResponse upsertSickLeavePolicy(UpsertSickLeavePolicyRequest request) {
        UUID companyId = principal().companyId();
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "COMPANY_NOT_FOUND", "Company not found."));
        CompanySickLeavePolicyConfig cfg = sickLeavePolicyRepository.findByCompanyId(companyId)
                .orElseGet(() -> {
                    CompanySickLeavePolicyConfig c = new CompanySickLeavePolicyConfig();
                    c.setCompany(company);
                    return c;
                });
        cfg.setCompanyPaidPercentage(request.companyPaidPercentage().setScale(2, java.math.RoundingMode.HALF_UP));
        cfg.setMaxCompanyPaidDays(request.maxCompanyPaidDays());
        sickLeavePolicyRepository.save(cfg);
        audit("SICK_POLICY_UPSERTED", "sick_leave_policy", companyId,
                Map.of("percentage", cfg.getCompanyPaidPercentage(), "maxDays", cfg.getMaxCompanyPaidDays()),
                Map.of("companyId", companyId));
        return new SickLeavePolicyResponse(cfg.getCompanyPaidPercentage(), cfg.getMaxCompanyPaidDays(), false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollMonthSummary> listMyPayrollHistory() {
        AuthSessionPrincipal principal = principal();
        Employee employee = employeeRepository.findByUserIdAndCompanyId(principal.userId(), principal.companyId())
                .orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "EMPLOYEE_PROFILE_NOT_FOUND",
                        "Employee profile is not configured."));
        String currency = employee.getCompany().getCurrency();
        return resultRepository.findAllByCompanyIdAndEmployeeIdOrderByYearDescMonthDesc(
                        principal.companyId(), employee.getId())
                .stream()
                .map(r -> new PayrollMonthSummary(r.getYear(), r.getMonth(), r.getStatus(),
                        r.getGrossEarnings(), r.getNetPay(), currency))
                .toList();
    }

    @Override
    public PayrollCalculationResponse approvePayroll(UUID employeeId, PayrollPeriodRequest request) {
        AuthSessionPrincipal principal = principal();
        YearMonth payrollMonth = payrollMonth(request.year(), request.month());
        PayrollResult result = loadExistingResult(principal.companyId(), employeeId, request.year(), request.month());
        if (result.getStatus() != PayrollStatus.CALCULATED) {
            throw new BusinessException(HttpStatus.CONFLICT, "PAYROLL_NOT_CALCULATED",
                    "Payroll must be in CALCULATED status to approve.");
        }

        // M3: Block negative net pay unless explicitly overridden (no override param in current request — conservative)
        if (result.getNetPay().signum() < 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "PAYROLL_NEGATIVE_NET_PAY",
                    "Net pay is negative. Override is required to approve a negative-net payroll.");
        }

        // B4: Auto-refresh snapshot if live recompute differs from stored
        Employee employee = loadEmployee(principal.companyId(), employeeId);
        PayrollCalculationResponse live = calculate(employee, payrollMonth, PayrollStatus.CALCULATED, false);
        boolean snapshotStale = result.getNetPay().compareTo(live.totals().netPay()) != 0
                || result.getGrossEarnings().compareTo(live.totals().grossEarnings()) != 0;
        if (snapshotStale) {
            User actor = loadUser(principal.userId());
            persistResult(principal.companyId(), employee, actor, live);
            result = loadExistingResult(principal.companyId(), employeeId, request.year(), request.month());
            audit("PAYROLL_REFRESHED_ON_APPROVE", "payroll_results", result.getId(),
                    Map.of("year", request.year(), "month", request.month()),
                    Map.of("companyId", principal.companyId(), "employeeId", employeeId));
        }

        transitionPayrollStatus(result, PayrollStatus.CALCULATED, PayrollStatus.APPROVED,
                "PAYROLL_NOT_CALCULATED", "Payroll must be in CALCULATED status to approve.");
        attendanceDayRecordRepository.lockByCompanyIdAndEmployeeIdAndWorkDateBetween(
                principal.companyId(), employeeId,
                payrollMonth.atDay(1), payrollMonth.atEndOfMonth());
        audit("PAYROLL_APPROVED", "payroll_results", result.getId(),
                Map.of("year", request.year(), "month", request.month(), "netPay", result.getNetPay()),
                Map.of("companyId", principal.companyId(), "employeeId", employeeId,
                        "actorUserId", principal.userId(), "from", "CALCULATED", "to", "APPROVED"));
        return responseFromSnapshot(result, false);
    }

    @Override
    public PayrollCalculationResponse finalizePayroll(UUID employeeId, PayrollPeriodRequest request) {
        AuthSessionPrincipal principal = principal();
        PayrollResult result = loadExistingResult(principal.companyId(), employeeId, request.year(), request.month());
        transitionPayrollStatus(result, PayrollStatus.APPROVED, PayrollStatus.FINALIZED,
                "PAYROLL_NOT_APPROVED", "Payroll must be in APPROVED status to finalize.");
        audit("PAYROLL_FINALIZED", "payroll_results", result.getId(),
                Map.of("year", request.year(), "month", request.month()),
                Map.of("companyId", principal.companyId(), "employeeId", employeeId,
                        "actorUserId", principal.userId(), "from", "APPROVED", "to", "FINALIZED"));
        return responseFromSnapshot(result, false);
    }

    @Override
    public PayrollCalculationResponse completePayment(UUID employeeId, PayrollPeriodRequest request) {
        AuthSessionPrincipal principal = principal();
        PayrollResult result = loadExistingResult(principal.companyId(), employeeId, request.year(), request.month());
        transitionPayrollStatus(result, PayrollStatus.FINALIZED, PayrollStatus.PAID,
                "PAYROLL_NOT_FINALIZED", "Payroll must be in FINALIZED status to complete payment.");
        audit("PAYROLL_PAID", "payroll_results", result.getId(),
                Map.of("year", request.year(), "month", request.month()),
                Map.of("companyId", principal.companyId(), "employeeId", employeeId,
                        "actorUserId", principal.userId(), "from", "FINALIZED", "to", "PAID"));
        return responseFromSnapshot(result, false);
    }

    @Override
    public PayrollCalculationResponse revertApproval(UUID employeeId, PayrollPeriodRequest request) {
        AuthSessionPrincipal principal = principal();
        YearMonth payrollMonth = payrollMonth(request.year(), request.month());
        PayrollResult result = loadExistingResult(principal.companyId(), employeeId, request.year(), request.month());
        transitionPayrollStatus(result, PayrollStatus.APPROVED, PayrollStatus.CALCULATED,
                "PAYROLL_NOT_APPROVED", "Payroll must be in APPROVED status to revert approval.");
        attendanceDayRecordRepository.unlockByCompanyIdAndEmployeeIdAndWorkDateBetween(
                principal.companyId(), employeeId,
                payrollMonth.atDay(1), payrollMonth.atEndOfMonth());
        audit("PAYROLL_APPROVAL_REVERTED", "payroll_results", result.getId(),
                Map.of("year", request.year(), "month", request.month()),
                Map.of("companyId", principal.companyId(), "employeeId", employeeId,
                        "actorUserId", principal.userId(), "from", "APPROVED", "to", "CALCULATED"));
        return responseFromSnapshot(result, false);
    }

    @Override
    public PayrollCalculationResponse revertFinalization(UUID employeeId, PayrollPeriodRequest request) {
        AuthSessionPrincipal principal = principal();
        PayrollResult result = loadExistingResult(principal.companyId(), employeeId, request.year(), request.month());
        transitionPayrollStatus(result, PayrollStatus.FINALIZED, PayrollStatus.APPROVED,
                "PAYROLL_NOT_FINALIZED", "Payroll must be in FINALIZED status to revert finalization.");
        audit("PAYROLL_FINALIZATION_REVERTED", "payroll_results", result.getId(),
                Map.of("year", request.year(), "month", request.month()),
                Map.of("companyId", principal.companyId(), "employeeId", employeeId,
                        "actorUserId", principal.userId(), "from", "FINALIZED", "to", "APPROVED"));
        return responseFromSnapshot(result, false);
    }

    @Override
    public PayrollCalculationResponse revertPayment(UUID employeeId, PayrollPeriodRequest request) {
        AuthSessionPrincipal principal = principal();
        PayrollResult result = loadExistingResult(principal.companyId(), employeeId, request.year(), request.month());
        transitionPayrollStatus(result, PayrollStatus.PAID, PayrollStatus.FINALIZED,
                "PAYROLL_NOT_PAID", "Payroll must be in PAID status to revert payment.");
        audit("PAYROLL_PAYMENT_REVERTED", "payroll_results", result.getId(),
                Map.of("year", request.year(), "month", request.month()),
                Map.of("companyId", principal.companyId(), "employeeId", employeeId,
                        "actorUserId", principal.userId(), "from", "PAID", "to", "FINALIZED"));
        return responseFromSnapshot(result, false);
    }

    // ── Authorization helpers (B1) ────────────────────────────────────────────

    private void assertEmployeeAccess(AuthSessionPrincipal principal, UUID employeeId) {
        if (principal.role() == PlatformRole.STAFF) {
            boolean assigned = employeeRepository.isEmployeeAssignedToSupervisor(
                    principal.companyId(), employeeId, principal.roleAssignmentId());
            if (!assigned) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "PAYROLL_ACCESS_DENIED",
                        "Staff may only access payroll for their assigned employees.");
            }
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private PayrollResult loadExistingResult(UUID companyId, UUID employeeId, int year, int month) {
        return resultRepository.findByCompanyIdAndEmployeeIdAndYearAndMonth(companyId, employeeId, year, month)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PAYROLL_RESULT_NOT_FOUND",
                        "No calculated payroll found for this employee and period."));
    }

    private void transitionPayrollStatus(
            PayrollResult result,
            PayrollStatus requiredStatus,
            PayrollStatus nextStatus,
            String code,
            String message
    ) {
        if (result.getStatus() != requiredStatus) {
            throw new BusinessException(HttpStatus.CONFLICT, code, message);
        }
        result.setStatus(nextStatus);
        result.setCalculationSnapshotJson(snapshotJsonWithStatus(result, nextStatus));
        resultRepository.saveAndFlush(result);
    }

    private String snapshotJsonWithStatus(PayrollResult result, PayrollStatus status) {
        try {
            ObjectNode node = (ObjectNode) objectMapper.readTree(result.getCalculationSnapshotJson());
            node.put("payrollStatus", status.name());
            return objectMapper.writeValueAsString(node);
        } catch (Exception exception) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "PAYROLL_SNAPSHOT_SERIALIZATION_FAILED",
                    "Payroll calculation snapshot status could not be updated.");
        }
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
        adjustmentRepository.save(adjustment);

        // B4: Recalculate and re-persist if a CALCULATED payroll exists for this period
        recalculateIfCalculated(principal.companyId(), employee, actor, payrollMonth);

        audit(type == PayrollAdjustmentType.BONUS ? "PAYROLL_BONUS_ADDED" : "PAYROLL_DEDUCTION_ADDED",
                "payroll_adjustments", adjustment.getId(),
                Map.of("amount", adjustment.getAmount(), "reason", adjustment.getReason()),
                Map.of("companyId", principal.companyId(), "employeeId", employeeId,
                        "year", payrollMonth.getYear(), "month", payrollMonth.getMonthValue()));
        return toAdjustmentResponse(adjustment);
    }

    private void recalculateIfCalculated(UUID companyId, Employee employee, User actor, YearMonth payrollMonth) {
        resultRepository.findByCompanyIdAndEmployeeIdAndYearAndMonth(
                        companyId, employee.getId(), payrollMonth.getYear(), payrollMonth.getMonthValue())
                .filter(r -> r.getStatus() == PayrollStatus.CALCULATED || r.getStatus() == PayrollStatus.DRAFT)
                .ifPresent(existing -> {
                    PayrollCalculationResponse refreshed = calculate(employee, payrollMonth, existing.getStatus(), false);
                    persistResult(companyId, employee, actor, refreshed);
                    audit("PAYROLL_RECALCULATED", "payroll_results", existing.getId(),
                            Map.of("year", payrollMonth.getYear(), "month", payrollMonth.getMonthValue(),
                                    "netPay", refreshed.totals().netPay()),
                            Map.of("companyId", companyId, "employeeId", employee.getId(),
                                    "actorUserId", actor.getId()));
                });
    }

    private PayrollCalculationResponse calculate(
            Employee employee,
            YearMonth payrollMonth,
            PayrollStatus payrollStatus,
            boolean preview
    ) {
        UUID companyId = employee.getCompany().getId();
        List<LeaveRequest> approvedInMonth = leaveRequestRepository.findApprovedOverlappingPayrollPeriod(
                companyId, employee.getId(), payrollMonth.atDay(1), payrollMonth.atEndOfMonth());
        List<LeaveRequest> approvedInYear = leaveRequestRepository.findApprovedOverlappingRange(
                companyId, employee.getId(), payrollMonth.atDay(1).withDayOfYear(1), payrollMonth.atEndOfMonth());
        List<LeaveBalance> balancesForYear = leaveBalanceRepository.findAllByCompanyIdAndEmployeeIdAndYear(
                companyId, employee.getId(), payrollMonth.getYear());
        List<PayrollAdjustment> adjustments = adjustmentRepository
                .findAllByCompanyIdAndEmployeeIdAndYearAndMonthOrderByCreatedAtAsc(
                        companyId, employee.getId(), payrollMonth.getYear(), payrollMonth.getMonthValue());
        return calculationEngine.calculate(employee, payrollMonth, approvedInYear, approvedInMonth,
                balancesForYear, adjustments, payrollStatus, preview);
    }

    private PayrollCalculationResponse resolvePayrollForSummary(
            Employee employee,
            YearMonth payrollMonth,
            Optional<PayrollResult> result
    ) {
        if (result.isPresent()) {
            PayrollResult payrollResult = result.get();
            if (LOCKED_STATUSES.contains(payrollResult.getStatus())) {
                return responseFromSnapshot(payrollResult, true);
            }
            return calculate(employee, payrollMonth, payrollResult.getStatus(), true);
        }
        return calculate(employee, payrollMonth, PayrollStatus.DRAFT, true);
    }

    private PayrollEmployeeSummaryResponse toPayrollEmployeeSummary(
            Employee employee,
            PayrollCalculationResponse payroll
    ) {
        var statutory = payroll.statutoryDeductions();
        return new PayrollEmployeeSummaryResponse(
                employee.getId(),
                employeeName(employee),
                employee.getEmploymentTypeRole(),
                employee.getEmploymentType(),
                employee.getPaymentMethod(),
                employee.getMonthlySalary(),
                employee.getHourlyRate(),
                payroll.currency(),
                payroll.payrollStatus(),
                payroll.calculationStatus(),
                payroll.preview(),
                payroll.totals().basePay(),
                payroll.totals().overtimePay(),
                payroll.adjustments().totalBonus(),
                payroll.adjustments().totalManualDeduction(),
                payroll.hourlyAttendancePayment() != null ? payroll.hourlyAttendancePayment().fullPayment() : null,
                payroll.hourlyAttendancePayment() != null ? payroll.hourlyAttendancePayment().attendanceDeduction() : null,
                payroll.hourlyAttendancePayment() != null ? payroll.hourlyAttendancePayment().paymentReceived() : null,
                payroll.totals().grossEarnings(),
                payroll.totals().totalDeductions(),
                payroll.totals().netPay(),
                payroll.totals().netPayNegative(),
                statutory != null ? statutory.incomeTax() : BigDecimal.ZERO,
                statutory != null ? statutory.employeeSocialSecurity() : BigDecimal.ZERO,
                statutory != null ? statutory.employeePensionContribution() : BigDecimal.ZERO,
                statutory != null ? statutory.employerCostTotal() : BigDecimal.ZERO,
                payroll.warnings()
        );
    }

    private void persistResult(UUID companyId, Employee employee, User actor, PayrollCalculationResponse response) {
        PayrollResult result = resultRepository.findByCompanyIdAndEmployeeIdAndYearAndMonth(
                        companyId, employee.getId(), response.year(), response.month())
                .orElseGet(PayrollResult::new);
        result.setCompany(employee.getCompany());
        result.setEmployee(employee);
        result.setYear(response.year());
        result.setMonth(response.month());
        result.setStatus(response.payrollStatus());
        result.setBasePay(response.totals().basePay());
        result.setGrossEarnings(response.totals().grossEarnings());
        result.setTotalDeductions(response.totals().totalDeductions());
        result.setNetPay(response.totals().netPay());
        // I4: Persist all statutory columns atomically
        var statutory = response.statutoryDeductions();
        result.setIncomeTax(statutory != null ? statutory.incomeTax() : BigDecimal.ZERO);
        result.setEmployeeSocialSecurity(statutory != null ? statutory.employeeSocialSecurity() : BigDecimal.ZERO);
        result.setEmployeePension(statutory != null ? statutory.employeePensionContribution() : BigDecimal.ZERO);
        result.setEmployerSocialSecurity(statutory != null ? statutory.employerSocialSecurity() : BigDecimal.ZERO);
        result.setEmployerPension(statutory != null ? statutory.employerPensionContribution() : BigDecimal.ZERO);
        result.setTaxableIncome(statutory != null ? statutory.taxableIncome() : BigDecimal.ZERO);
        result.setEmployerCostTotal(statutory != null ? statutory.employerCostTotal() : BigDecimal.ZERO);
        result.setCalculationSnapshotJson(toJson(response));
        result.setCalculatedAt(Instant.now());
        result.setCalculatedByUser(actor);
        resultRepository.save(result);
    }

    PayrollCalculationResponse responseFromSnapshot(PayrollResult result, boolean preview) {
        try {
            ObjectNode node = (ObjectNode) objectMapper.readTree(result.getCalculationSnapshotJson());
            node.put("payrollStatus", result.getStatus().name());
            node.put("preview", preview);
            return objectMapper.treeToValue(node, PayrollCalculationResponse.class);
        } catch (Exception exception) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "PAYROLL_SNAPSHOT_READ_FAILED",
                    "Could not read persisted payroll snapshot.");
        }
    }

    private String toJson(PayrollCalculationResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "PAYROLL_SNAPSHOT_SERIALIZATION_FAILED",
                    "Payroll calculation snapshot could not be stored.");
        }
    }

    private List<Employee> loadBatchEmployees(UUID companyId, List<UUID> employeeIds,
                                               LocalDate periodStart, LocalDate periodEnd) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return employeeRepository.findPayrollCandidates(companyId, null, periodStart, periodEnd);
        }
        return employeeRepository.findPayrollCandidates(companyId, employeeIds, periodStart, periodEnd);
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

    private String employeeName(Employee employee) {
        if (employee.getUser().getDisplayName() != null && !employee.getUser().getDisplayName().isBlank()) {
            return employee.getUser().getDisplayName();
        }
        return (employee.getUser().getFirstName() + " " + employee.getUser().getLastName()).trim();
    }

    private void audit(String action, String entityType, Object entityId,
                       Map<String, Object> diff, Map<String, Object> metadata) {
        try {
            AuthSessionPrincipal p = principal();
            UUID id = entityId instanceof UUID u ? u : null;
            auditLogService.logAction(new AuditLog(
                    p.companyId(), p.userId(), p.roleAssignmentId(), p.role(),
                    null, action, entityType, id, diff, metadata, null));
        } catch (Exception ignored) {
            // Audit failure must never break the business flow
        }
    }

    private AuthSessionPrincipal principal() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthSessionPrincipal p)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "No authentication session found.");
        }
        return p;
    }
}

package com.worknest.features.payroll.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.worknest.common.exception.BusinessException;
import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.Employee;
import com.worknest.domain.entities.PayrollResult;
import com.worknest.domain.entities.User;
import com.worknest.domain.enums.EmploymentStatus;
import com.worknest.domain.enums.PaymentMethod;
import com.worknest.domain.enums.PayrollCalculationStatus;
import com.worknest.domain.enums.PayrollStatus;
import com.worknest.audit.service.AuditLogService;
import com.worknest.features.attendance.repository.AttendanceDayRecordRepository;
import com.worknest.features.auth.repository.UserRepository;
import com.worknest.features.company.repository.CompanyRepository;
import com.worknest.features.employee.repository.EmployeeRepository;
import com.worknest.features.leave.repository.LeaveBalanceRepository;
import com.worknest.features.leave.repository.LeaveRequestRepository;
import com.worknest.features.payroll.dto.PayrollDtos.AbsenceDetails;
import com.worknest.features.payroll.dto.PayrollDtos.AdjustmentDetails;
import com.worknest.features.payroll.dto.PayrollDtos.BasePayDetails;
import com.worknest.features.payroll.dto.PayrollDtos.EmploymentPeriodDetails;
import com.worknest.features.payroll.dto.PayrollDtos.LeaveCalculationDetails;
import com.worknest.features.payroll.dto.PayrollDtos.BatchPayrollCalculationRequest;
import com.worknest.features.payroll.dto.PayrollDtos.BatchPayrollCalculationResponse;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollCalculationResponse;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollPeriodRequest;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollTotals;
import com.worknest.features.payroll.dto.PayrollDtos.SickLeaveCalculationDetails;
import com.worknest.features.payroll.dto.PayrollDtos.StatutoryDeductionDetails;
import com.worknest.features.payroll.dto.PayrollDtos.WorkPeriodDetails;
import com.worknest.features.payroll.repository.CompanySickLeavePolicyConfigRepository;
import com.worknest.features.payroll.repository.PayrollAdjustmentRepository;
import com.worknest.features.payroll.repository.PayrollResultRepository;
import com.worknest.domain.enums.PlatformAccess;
import com.worknest.domain.enums.PlatformRole;
import com.worknest.security.AuthSessionPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollServiceImplTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private UserRepository userRepository;
    @Mock private LeaveRequestRepository leaveRequestRepository;
    @Mock private LeaveBalanceRepository leaveBalanceRepository;
    @Mock private PayrollAdjustmentRepository adjustmentRepository;
    @Mock private PayrollResultRepository resultRepository;
    @Mock private PayrollCalculationEngine calculationEngine;
    @Mock private CompanySickLeavePolicyConfigRepository sickLeavePolicyRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private AttendanceDayRecordRepository attendanceDayRecordRepository;
    @Mock private AuditLogService auditLogService;

    private PayrollServiceImpl service;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private final UUID companyId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new PayrollServiceImpl(
                employeeRepository, userRepository, leaveRequestRepository,
                leaveBalanceRepository, adjustmentRepository, resultRepository,
                calculationEngine, objectMapper, sickLeavePolicyRepository,
                companyRepository, attendanceDayRecordRepository, auditLogService);
        AuthSessionPrincipal principal = new AuthSessionPrincipal(
                userId, "testuser", companyId, "test-slug", UUID.randomUUID(),
                PlatformRole.ADMIN, PlatformAccess.WEB);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    // --- Snapshot consistency for locked payrolls ---

    @Test
    void previewReturnsSnapshotForApprovedPayroll() {
        Employee employee = employee();
        PayrollResult result = payrollResult(PayrollStatus.APPROVED, snapshotJson(PayrollStatus.APPROVED));
        when(employeeRepository.findByIdAndCompanyId(employeeId, companyId)).thenReturn(Optional.of(employee));
        when(resultRepository.findByCompanyIdAndEmployeeIdAndYearAndMonth(companyId, employeeId, 2026, 5))
                .thenReturn(Optional.of(result));

        PayrollCalculationResponse response = service.previewEmployeePayroll(employeeId, 2026, 5);

        assertThat(response.payrollStatus()).isEqualTo(PayrollStatus.APPROVED);
        assertThat(response.preview()).isTrue();
        verify(calculationEngine, never()).calculate(any(), any(), any(), any(), any(), any(), any(), any(Boolean.class));
    }

    @Test
    void previewReturnsSnapshotForPaidPayroll() {
        Employee employee = employee();
        PayrollResult result = payrollResult(PayrollStatus.PAID, snapshotJson(PayrollStatus.PAID));
        when(employeeRepository.findByIdAndCompanyId(employeeId, companyId)).thenReturn(Optional.of(employee));
        when(resultRepository.findByCompanyIdAndEmployeeIdAndYearAndMonth(companyId, employeeId, 2026, 5))
                .thenReturn(Optional.of(result));

        PayrollCalculationResponse response = service.previewEmployeePayroll(employeeId, 2026, 5);

        assertThat(response.payrollStatus()).isEqualTo(PayrollStatus.PAID);
        assertThat(response.preview()).isTrue();
        verify(calculationEngine, never()).calculate(any(), any(), any(), any(), any(), any(), any(), any(Boolean.class));
    }

    @Test
    void previewCalculatesLiveForCalculatedStatus() {
        Employee employee = employee();
        PayrollResult result = payrollResult(PayrollStatus.CALCULATED, snapshotJson(PayrollStatus.CALCULATED));
        PayrollCalculationResponse liveResponse = calculationResponse(PayrollStatus.CALCULATED, true);
        when(employeeRepository.findByIdAndCompanyId(employeeId, companyId)).thenReturn(Optional.of(employee));
        when(resultRepository.findByCompanyIdAndEmployeeIdAndYearAndMonth(companyId, employeeId, 2026, 5))
                .thenReturn(Optional.of(result));
        when(leaveRequestRepository.findApprovedOverlappingPayrollPeriod(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(leaveRequestRepository.findApprovedOverlappingRange(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(adjustmentRepository.findAllByCompanyIdAndEmployeeIdAndYearAndMonthOrderByCreatedAtAsc(any(), any(), eq(2026), eq(5)))
                .thenReturn(List.of());
        when(calculationEngine.calculate(any(), any(), any(), any(), any(), any(), eq(PayrollStatus.CALCULATED), eq(true)))
                .thenReturn(liveResponse);
        when(leaveBalanceRepository.findAllByCompanyIdAndEmployeeIdAndYear(any(), any(), eq(2026)))
                .thenReturn(List.of());

        PayrollCalculationResponse response = service.previewEmployeePayroll(employeeId, 2026, 5);

        assertThat(response.preview()).isTrue();
        verify(calculationEngine).calculate(any(), any(), any(), any(), any(), any(), eq(PayrollStatus.CALCULATED), eq(true));
    }

    // --- completePayment marks snapshot as PAID without recalculating ---

    @Test
    void completePaymentMarksFinalizedSnapshotAsPaidWithoutRecalculating() {
        PayrollResult result = payrollResult(PayrollStatus.FINALIZED, snapshotJson(PayrollStatus.FINALIZED));
        when(resultRepository.findByCompanyIdAndEmployeeIdAndYearAndMonth(companyId, employeeId, 2026, 5))
                .thenReturn(Optional.of(result));

        PayrollCalculationResponse response = service.completePayment(
                employeeId, new PayrollPeriodRequest(2026, 5));

        assertThat(response.payrollStatus()).isEqualTo(PayrollStatus.PAID);
        assertThat(snapshotStatus(result)).isEqualTo(PayrollStatus.PAID);
        assertThat(response.preview()).isFalse();
        verify(resultRepository).saveAndFlush(result);
        verify(calculationEngine, never()).calculate(any(), any(), any(), any(), any(), any(), any(), any(Boolean.class));
    }

    @Test
    void completePaymentRejectsNonFinalizedPayroll() {
        PayrollResult result = payrollResult(PayrollStatus.APPROVED, snapshotJson(PayrollStatus.APPROVED));
        when(resultRepository.findByCompanyIdAndEmployeeIdAndYearAndMonth(companyId, employeeId, 2026, 5))
                .thenReturn(Optional.of(result));

        assertThatThrownBy(() -> service.completePayment(employeeId, new PayrollPeriodRequest(2026, 5)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("FINALIZED");
    }

    // --- Attendance lock at APPROVED, not at CALCULATED ---

    @Test
    void calculatePayrollDoesNotLockAttendance() {
        Employee employee = employee();
        User actor = new User();
        actor.setId(userId);
        PayrollCalculationResponse calcResponse = calculationResponse(PayrollStatus.CALCULATED, false);
        when(employeeRepository.findByIdAndCompanyId(employeeId, companyId)).thenReturn(Optional.of(employee));
        when(userRepository.findById(userId)).thenReturn(Optional.of(actor));
        when(resultRepository.existsByCompanyIdAndEmployeeIdAndYearAndMonthAndStatusIn(any(), any(), eq(2026), eq(5), any()))
                .thenReturn(false);
        when(resultRepository.findByCompanyIdAndEmployeeIdAndYearAndMonth(any(), any(), eq(2026), eq(5)))
                .thenReturn(Optional.empty());
        when(leaveRequestRepository.findApprovedOverlappingPayrollPeriod(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(leaveRequestRepository.findApprovedOverlappingRange(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(adjustmentRepository.findAllByCompanyIdAndEmployeeIdAndYearAndMonthOrderByCreatedAtAsc(any(), any(), eq(2026), eq(5)))
                .thenReturn(List.of());
        when(leaveBalanceRepository.findAllByCompanyIdAndEmployeeIdAndYear(any(), any(), eq(2026)))
                .thenReturn(List.of());
        when(calculationEngine.calculate(any(), any(), any(), any(), any(), any(), eq(PayrollStatus.CALCULATED), eq(false)))
                .thenReturn(calcResponse);

        service.calculateEmployeePayroll(employeeId, 2026, 5);

        verify(attendanceDayRecordRepository, never()).lockByCompanyIdAndEmployeeIdAndWorkDateBetween(
                any(), any(), any(), any());
    }

    @Test
    void approvePayrollLocksAttendance() {
        Employee employee = employee();
        PayrollCalculationResponse liveResponse = calculationResponse(PayrollStatus.CALCULATED, false);
        PayrollResult result = payrollResult(PayrollStatus.CALCULATED, snapshotJson(PayrollStatus.CALCULATED));
        when(resultRepository.findByCompanyIdAndEmployeeIdAndYearAndMonth(companyId, employeeId, 2026, 5))
                .thenReturn(Optional.of(result));
        when(employeeRepository.findByIdAndCompanyId(employeeId, companyId)).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.findApprovedOverlappingPayrollPeriod(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(leaveRequestRepository.findApprovedOverlappingRange(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(adjustmentRepository.findAllByCompanyIdAndEmployeeIdAndYearAndMonthOrderByCreatedAtAsc(any(), any(), eq(2026), eq(5)))
                .thenReturn(List.of());
        when(leaveBalanceRepository.findAllByCompanyIdAndEmployeeIdAndYear(any(), any(), eq(2026)))
                .thenReturn(List.of());
        when(calculationEngine.calculate(any(), any(), any(), any(), any(), any(), eq(PayrollStatus.CALCULATED), eq(false)))
                .thenReturn(liveResponse);

        PayrollCalculationResponse response = service.approvePayroll(employeeId, new PayrollPeriodRequest(2026, 5));

        assertThat(response.payrollStatus()).isEqualTo(PayrollStatus.APPROVED);
        assertThat(result.getStatus()).isEqualTo(PayrollStatus.APPROVED);
        assertThat(snapshotStatus(result)).isEqualTo(PayrollStatus.APPROVED);
        verify(resultRepository).saveAndFlush(result);
        verify(attendanceDayRecordRepository).lockByCompanyIdAndEmployeeIdAndWorkDateBetween(
                eq(companyId), eq(employeeId),
                eq(LocalDate.of(2026, 5, 1)), eq(LocalDate.of(2026, 5, 31)));
    }

    @Test
    void finalizePayrollUpdatesResultAndSnapshotStatus() {
        PayrollResult result = payrollResult(PayrollStatus.APPROVED, snapshotJson(PayrollStatus.APPROVED));
        when(resultRepository.findByCompanyIdAndEmployeeIdAndYearAndMonth(companyId, employeeId, 2026, 5))
                .thenReturn(Optional.of(result));

        PayrollCalculationResponse response = service.finalizePayroll(employeeId, new PayrollPeriodRequest(2026, 5));

        assertThat(response.payrollStatus()).isEqualTo(PayrollStatus.FINALIZED);
        assertThat(result.getStatus()).isEqualTo(PayrollStatus.FINALIZED);
        assertThat(snapshotStatus(result)).isEqualTo(PayrollStatus.FINALIZED);
        verify(resultRepository).saveAndFlush(result);
    }

    // --- Revert transitions ---

    @Test
    void revertApprovalMovesApprovedToCalculatedAndUnlocksAttendance() {
        PayrollResult result = payrollResult(PayrollStatus.APPROVED, snapshotJson(PayrollStatus.APPROVED));
        when(resultRepository.findByCompanyIdAndEmployeeIdAndYearAndMonth(companyId, employeeId, 2026, 5))
                .thenReturn(Optional.of(result));

        PayrollCalculationResponse response = service.revertApproval(employeeId, new PayrollPeriodRequest(2026, 5));

        assertThat(response.payrollStatus()).isEqualTo(PayrollStatus.CALCULATED);
        assertThat(result.getStatus()).isEqualTo(PayrollStatus.CALCULATED);
        assertThat(snapshotStatus(result)).isEqualTo(PayrollStatus.CALCULATED);
        verify(resultRepository).saveAndFlush(result);
        verify(attendanceDayRecordRepository).unlockByCompanyIdAndEmployeeIdAndWorkDateBetween(
                eq(companyId), eq(employeeId),
                eq(LocalDate.of(2026, 5, 1)), eq(LocalDate.of(2026, 5, 31)));
    }

    @Test
    void revertApprovalRejectsNonApprovedPayroll() {
        PayrollResult result = payrollResult(PayrollStatus.FINALIZED, snapshotJson(PayrollStatus.FINALIZED));
        when(resultRepository.findByCompanyIdAndEmployeeIdAndYearAndMonth(companyId, employeeId, 2026, 5))
                .thenReturn(Optional.of(result));

        assertThatThrownBy(() -> service.revertApproval(employeeId, new PayrollPeriodRequest(2026, 5)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("APPROVED");
    }

    @Test
    void revertFinalizationMovesFinalizedToApproved() {
        PayrollResult result = payrollResult(PayrollStatus.FINALIZED, snapshotJson(PayrollStatus.FINALIZED));
        when(resultRepository.findByCompanyIdAndEmployeeIdAndYearAndMonth(companyId, employeeId, 2026, 5))
                .thenReturn(Optional.of(result));

        PayrollCalculationResponse response = service.revertFinalization(employeeId, new PayrollPeriodRequest(2026, 5));

        assertThat(response.payrollStatus()).isEqualTo(PayrollStatus.APPROVED);
        assertThat(result.getStatus()).isEqualTo(PayrollStatus.APPROVED);
        assertThat(snapshotStatus(result)).isEqualTo(PayrollStatus.APPROVED);
        verify(resultRepository).saveAndFlush(result);
        verify(attendanceDayRecordRepository, never()).unlockByCompanyIdAndEmployeeIdAndWorkDateBetween(
                any(), any(), any(), any());
    }

    @Test
    void revertFinalizationRejectsNonFinalizedPayroll() {
        PayrollResult result = payrollResult(PayrollStatus.PAID, snapshotJson(PayrollStatus.PAID));
        when(resultRepository.findByCompanyIdAndEmployeeIdAndYearAndMonth(companyId, employeeId, 2026, 5))
                .thenReturn(Optional.of(result));

        assertThatThrownBy(() -> service.revertFinalization(employeeId, new PayrollPeriodRequest(2026, 5)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("FINALIZED");
    }

    @Test
    void revertPaymentMovesPaidToFinalized() {
        PayrollResult result = payrollResult(PayrollStatus.PAID, snapshotJson(PayrollStatus.PAID));
        when(resultRepository.findByCompanyIdAndEmployeeIdAndYearAndMonth(companyId, employeeId, 2026, 5))
                .thenReturn(Optional.of(result));

        PayrollCalculationResponse response = service.revertPayment(employeeId, new PayrollPeriodRequest(2026, 5));

        assertThat(response.payrollStatus()).isEqualTo(PayrollStatus.FINALIZED);
        assertThat(result.getStatus()).isEqualTo(PayrollStatus.FINALIZED);
        assertThat(snapshotStatus(result)).isEqualTo(PayrollStatus.FINALIZED);
        verify(resultRepository).saveAndFlush(result);
    }

    @Test
    void revertPaymentRejectsNonPaidPayroll() {
        PayrollResult result = payrollResult(PayrollStatus.FINALIZED, snapshotJson(PayrollStatus.FINALIZED));
        when(resultRepository.findByCompanyIdAndEmployeeIdAndYearAndMonth(companyId, employeeId, 2026, 5))
                .thenReturn(Optional.of(result));

        assertThatThrownBy(() -> service.revertPayment(employeeId, new PayrollPeriodRequest(2026, 5)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PAID");
    }

    // --- Batch calculation skip logic ---

    @Test
    void calculateBatchSkipsEmployeeWithLockedPayroll() {
        Employee employee = employee();
        when(employeeRepository.findPayrollCandidates(eq(companyId), any(), any(), any()))
                .thenReturn(List.of(employee));
        when(resultRepository.existsByCompanyIdAndEmployeeIdAndYearAndMonthAndStatusIn(
                eq(companyId), eq(employeeId), eq(2026), eq(5), any())).thenReturn(true);

        BatchPayrollCalculationResponse response =
                service.calculateBatch(new BatchPayrollCalculationRequest(2026, 5, null));

        assertThat(response.totalEmployees()).isEqualTo(1);
        assertThat(response.successfulCalculations()).isEqualTo(0);
        assertThat(response.skippedCalculations()).isEqualTo(1);
        assertThat(response.failedCalculations()).isEqualTo(0);
        assertThat(response.results().get(0).errorCode()).isEqualTo("PAYROLL_PERIOD_LOCKED");
        verify(calculationEngine, never()).calculate(any(), any(), any(), any(), any(), any(), any(), any(Boolean.class));
    }

    @Test
    void calculateBatchSkipsEmployeeWithExpiredContract() {
        Employee employee = employee();
        employee.setContractExpiryDate(LocalDate.of(2026, 4, 30));
        when(employeeRepository.findPayrollCandidates(eq(companyId), any(), any(), any()))
                .thenReturn(List.of(employee));

        BatchPayrollCalculationResponse response =
                service.calculateBatch(new BatchPayrollCalculationRequest(2026, 5, null));

        assertThat(response.skippedCalculations()).isEqualTo(1);
        assertThat(response.failedCalculations()).isEqualTo(0);
        assertThat(response.results().get(0).errorCode()).isEqualTo("CONTRACT_EXPIRED");
    }

    @Test
    void calculateBatchSkipsEmployeeStartingAfterPeriod() {
        Employee employee = employee();
        employee.setStartDate(LocalDate.of(2026, 6, 1));
        when(employeeRepository.findPayrollCandidates(eq(companyId), any(), any(), any()))
                .thenReturn(List.of(employee));

        BatchPayrollCalculationResponse response =
                service.calculateBatch(new BatchPayrollCalculationRequest(2026, 5, null));

        assertThat(response.skippedCalculations()).isEqualTo(1);
        assertThat(response.failedCalculations()).isEqualTo(0);
        assertThat(response.results().get(0).errorCode()).isEqualTo("NO_ACTIVE_CONTRACT_IN_PERIOD");
    }

    // --- Helpers ---

    private Employee employee() {
        Company company = new Company();
        company.setId(companyId);
        User user = new User();
        user.setId(userId);
        user.setFirstName("Jane");
        user.setLastName("Doe");
        Employee emp = new Employee();
        emp.setId(employeeId);
        emp.setCompany(company);
        emp.setUser(user);
        emp.setEmploymentStatus(EmploymentStatus.ACTIVE);
        emp.setPaymentMethod(PaymentMethod.FIXED_MONTHLY);
        emp.setMonthlySalary(new BigDecimal("2000.00"));
        emp.setStartDate(LocalDate.of(2024, 1, 1));
        return emp;
    }

    private PayrollResult payrollResult(PayrollStatus status, String snapshotJson) {
        PayrollResult result = new PayrollResult();
        result.setId(UUID.randomUUID());
        result.setYear(2026);
        result.setMonth(5);
        result.setStatus(status);
        result.setBasePay(new BigDecimal("2000.00"));
        result.setGrossEarnings(new BigDecimal("2000.00"));
        result.setTotalDeductions(BigDecimal.ZERO);
        result.setNetPay(new BigDecimal("2000.00"));
        result.setCalculationSnapshotJson(snapshotJson);
        return result;
    }

    private String snapshotJson(PayrollStatus status) {
        PayrollCalculationResponse response = calculationResponse(status, false);
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private PayrollStatus snapshotStatus(PayrollResult result) {
        try {
            return PayrollStatus.valueOf(objectMapper.readTree(result.getCalculationSnapshotJson())
                    .get("payrollStatus")
                    .asText());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private PayrollCalculationResponse calculationResponse(PayrollStatus status, boolean preview) {
        return new PayrollCalculationResponse(
                employeeId, "Jane Doe", 2026, 5, "EUR",
                PaymentMethod.FIXED_MONTHLY, PayrollCalculationStatus.SUCCESS,
                status, preview,
                new EmploymentPeriodDetails(LocalDate.of(2024, 1, 1), null,
                        LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)),
                new WorkPeriodDetails(31, 21, new BigDecimal("21"), new BigDecimal("8"),
                        new BigDecimal("168"), "DEFAULT_WORKING_DAYS_PLACEHOLDER",
                        LocalDate.of(2026, 5, 31), new BigDecimal("21")),
                new BasePayDetails("monthlySalary * payableWorkingDays / workingDaysInMonth",
                        new BigDecimal("2000.00"), null, new BigDecimal("21"), 21,
                        null, new BigDecimal("2000.00"), "WORKING_DAYS"),
                null,
                new LeaveCalculationDetails(20, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of()),
                new SickLeaveCalculationDetails(BigDecimal.ZERO, null, null, null,
                        null, null, null, null, null, null, null, null, "TODO_SICK_LEAVE_POLICY_NOT_CONFIGURED"),
                new AdjustmentDetails(List.of(), List.of(), BigDecimal.ZERO, BigDecimal.ZERO),
                new StatutoryDeductionDetails(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of(), true),
                new AbsenceDetails(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false),
                null,
                new PayrollTotals(new BigDecimal("2000.00"), BigDecimal.ZERO, new BigDecimal("2000.00"),
                        BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("2000.00"), false, BigDecimal.ZERO),
                List.of());
    }
}

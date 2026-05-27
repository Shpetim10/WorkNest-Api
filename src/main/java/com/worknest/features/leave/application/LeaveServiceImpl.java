package com.worknest.features.leave.application;

import com.worknest.common.exception.BusinessException;
import com.worknest.domain.entities.Employee;
import com.worknest.domain.entities.LeaveBalance;
import com.worknest.domain.entities.LeaveRequest;
import com.worknest.domain.entities.User;
import com.worknest.domain.enums.EmploymentStatus;
import com.worknest.domain.enums.LeaveStatus;
import com.worknest.domain.enums.LeaveType;
import com.worknest.domain.enums.PayrollStatus;
import com.worknest.domain.enums.NotificationType;
import com.worknest.domain.enums.NotificationTargetType;
import com.worknest.features.notification.application.NotificationService;
import com.worknest.features.auth.repository.UserRepository;
import com.worknest.features.employee.repository.EmployeeRepository;
import com.worknest.features.leave.dto.ApproveLeaveRequestDto;
import com.worknest.features.leave.dto.CreateLeaveRequestDto;
import com.worknest.features.leave.dto.LeaveBalanceDto;
import com.worknest.features.leave.dto.LeaveRequestDto;
import com.worknest.features.leave.dto.RejectLeaveRequestDto;
import com.worknest.realtime.event.LeaveRequestApprovedDomainEvent;
import com.worknest.realtime.event.LeaveRequestRejectedDomainEvent;
import com.worknest.realtime.event.LeaveRequestSubmittedDomainEvent;
import com.worknest.features.leave.repository.LeaveBalanceRepository;
import com.worknest.features.leave.repository.LeaveRequestRepository;
import com.worknest.features.payroll.repository.PayrollResultRepository;
import com.worknest.security.AuthSessionPrincipal;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class LeaveServiceImpl implements LeaveService {

    private static final Set<PayrollStatus> LOCKED_PAYROLL_STATUSES = Set.of(
            PayrollStatus.APPROVED, PayrollStatus.FINALIZED, PayrollStatus.PAID);

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final PayrollResultRepository payrollResultRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationService notificationService;

    @Override
    public List<LeaveBalanceDto> getMyBalance() {
        AuthSessionPrincipal principal = principal();
        Employee employee = resolveCurrentEmployee(principal);
        int year = LocalDate.now().getYear();

        return Arrays.stream(LeaveType.values())
                .map(type -> {
                    LeaveBalance balance = findOrInitBalance(employee, type, year);
                    BigDecimal available = balance.getTotalDays().subtract(balance.getUsedDays()).max(BigDecimal.ZERO);
                    return new LeaveBalanceDto(type, balance.getTotalDays(), balance.getUsedDays(), available);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LeaveRequestDto> getMyRequests(Pageable pageable) {
        AuthSessionPrincipal principal = principal();
        Employee employee = resolveCurrentEmployee(principal);
        return leaveRequestRepository
                .findAllByCompanyIdAndEmployeeIdOrderByCreatedAtDesc(principal.companyId(), employee.getId(), pageable)
                .map(this::toDto);
    }

    @Override
    public void submitRequest(CreateLeaveRequestDto request) {
        AuthSessionPrincipal principal = principal();
        Employee employee = resolveCurrentEmployee(principal);

        if (request.endDate().isBefore(request.startDate())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE",
                    "End date must not be before start date.");
        }

        if (request.leaveType() == LeaveType.SICK &&
                (request.medicalReportDocumentId() == null || request.medicalReportDocumentId().isBlank())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "MISSING_MEDICAL_REPORT",
                    "A medical report document is required for sick leave requests.");
        }

        List<LeaveRequest> overlapping = leaveRequestRepository.findOverlapping(
                principal.companyId(), employee.getId(), request.startDate(), request.endDate(),
                List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED));
        if (!overlapping.isEmpty()) {
            throw new BusinessException(HttpStatus.CONFLICT, "LEAVE_OVERLAP",
                    "A pending or approved leave request already exists for the selected dates.");
        }

        long calendarDays = ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1;
        BigDecimal daysCount = BigDecimal.valueOf(calendarDays);

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setCompany(employee.getCompany());
        leaveRequest.setEmployee(employee);
        leaveRequest.setLeaveType(request.leaveType());
        leaveRequest.setStartDate(request.startDate());
        leaveRequest.setEndDate(request.endDate());
        leaveRequest.setDaysCount(daysCount);
        leaveRequest.setNote(request.note());
        leaveRequest.setMedicalReportDocumentId(request.medicalReportDocumentId());
        leaveRequest.setStatus(LeaveStatus.PENDING);
        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);

        eventPublisher.publishEvent(new LeaveRequestSubmittedDomainEvent(
                principal.companyId(), saved.getId(), employee.getId(), principal.userId(),
                saved.getLeaveType(), saved.getStartDate(), saved.getEndDate(), saved.getDaysCount()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LeaveRequestDto> adminListRequests(String search, LeaveStatus status, Pageable pageable) {
        AuthSessionPrincipal principal = principal();
        String trimmedSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        return leaveRequestRepository
                .findAllForAdmin(principal.companyId(), status, trimmedSearch, pageable)
                .map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public LeaveRequestDto adminGetRequest(UUID requestId) {
        AuthSessionPrincipal principal = principal();
        LeaveRequest request = loadRequest(requestId, principal.companyId());
        return toDto(request);
    }

    @Override
    public void approveRequest(UUID requestId, ApproveLeaveRequestDto dto) {
        AuthSessionPrincipal principal = principal();
        LeaveRequest request = loadRequest(requestId, principal.companyId());

        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new BusinessException(HttpStatus.CONFLICT, "LEAVE_NOT_PENDING",
                    "Only pending leave requests can be approved.");
        }

        LeaveBalance balance = findOrInitBalance(request.getEmployee(), request.getLeaveType(),
                request.getStartDate().getYear());

        User reviewer = loadReviewer(principal.userId());
        request.setStatus(LeaveStatus.APPROVED);
        request.setReviewedByUser(reviewer);
        request.setReviewedAt(Instant.now());
        if (dto != null && dto.note() != null && !dto.note().isBlank()) {
            request.setApprovalNote(dto.note());
        }

        balance.setUsedDays(balance.getUsedDays().add(request.getDaysCount()));
        leaveBalanceRepository.save(balance);
        leaveRequestRepository.save(request);

        UUID employeeUserId = request.getEmployee().getUser().getId();
        eventPublisher.publishEvent(new LeaveRequestApprovedDomainEvent(
                principal.companyId(), requestId, request.getEmployee().getId(),
                employeeUserId, principal.userId(), request.getLeaveType()
        ));

        // Create in-app notification
        String message = "Your " + request.getLeaveType().name().toLowerCase() + " leave (" + request.getStartDate() + " to " + request.getEndDate() + ") has been approved";
        if (request.getApprovalNote() != null && !request.getApprovalNote().isBlank()) {
            message += ": " + request.getApprovalNote();
        }
        notificationService.createNotification(
                request.getCompany(),
                request.getEmployee().getUser(),
                NotificationType.LEAVE_APPROVED,
                "Leave Request Approved",
                message,
                request.getId(),
                NotificationTargetType.LEAVE_REQUEST
        );
    }

    @Override
    public void rejectRequest(UUID requestId, RejectLeaveRequestDto dto) {
        AuthSessionPrincipal principal = principal();
        LeaveRequest request = loadRequest(requestId, principal.companyId());

        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new BusinessException(HttpStatus.CONFLICT, "LEAVE_NOT_PENDING",
                    "Only pending leave requests can be rejected.");
        }

        User reviewer = loadReviewer(principal.userId());
        request.setStatus(LeaveStatus.REJECTED);
        request.setRejectionReason(dto.reason());
        request.setReviewedByUser(reviewer);
        request.setReviewedAt(Instant.now());
        leaveRequestRepository.save(request);

        UUID employeeUserId = request.getEmployee().getUser().getId();
        eventPublisher.publishEvent(new LeaveRequestRejectedDomainEvent(
                principal.companyId(), requestId, request.getEmployee().getId(),
                employeeUserId, principal.userId(), request.getLeaveType(), dto.reason()
        ));

        // Create in-app notification
        String message = "Your " + request.getLeaveType().name().toLowerCase() + " leave (" + request.getStartDate() + " to " + request.getEndDate() + ") has been rejected";
        if (dto.reason() != null && !dto.reason().isBlank()) {
            message += ": " + dto.reason();
        }
        notificationService.createNotification(
                request.getCompany(),
                request.getEmployee().getUser(),
                NotificationType.LEAVE_REJECTED,
                "Leave Request Rejected",
                message,
                request.getId(),
                NotificationTargetType.LEAVE_REQUEST
        );
    }

    @Override
    public void cancelRequest(UUID requestId) {
        AuthSessionPrincipal principal = principal();
        Employee employee = resolveCurrentEmployee(principal);
        LeaveRequest request = loadRequest(requestId, principal.companyId());

        if (!request.getEmployee().getId().equals(employee.getId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                    "You can only cancel your own leave requests.");
        }

        if (request.getStatus() == LeaveStatus.CANCELLED) {
            throw new BusinessException(HttpStatus.CONFLICT, "LEAVE_ALREADY_CANCELLED",
                    "This leave request is already cancelled.");
        }

        if (request.getStatus() != LeaveStatus.PENDING && request.getStatus() != LeaveStatus.APPROVED) {
            throw new BusinessException(HttpStatus.CONFLICT, "LEAVE_CANNOT_BE_CANCELLED",
                    "Only pending or approved leave requests can be cancelled.");
        }

        if (request.getStatus() == LeaveStatus.APPROVED) {
            if (!LocalDate.now().isBefore(request.getStartDate())) {
                throw new BusinessException(HttpStatus.CONFLICT, "LEAVE_ALREADY_STARTED",
                        "Approved leave requests can only be cancelled before the leave start date.");
            }

            ensurePayrollNotLocked(employee, request);

            leaveBalanceRepository
                    .findByCompanyIdAndEmployeeIdAndYearAndLeaveType(
                            employee.getCompany().getId(), employee.getId(),
                            request.getStartDate().getYear(), request.getLeaveType())
                    .ifPresent(balance -> {
                        balance.setUsedDays(balance.getUsedDays().subtract(request.getDaysCount()).max(BigDecimal.ZERO));
                        leaveBalanceRepository.save(balance);
                    });
        }

        request.setStatus(LeaveStatus.CANCELLED);
        leaveRequestRepository.save(request);
    }

    /** Blocks cancellation of approved leave if payroll is locked for any month the leave spans. */
    private void ensurePayrollNotLocked(Employee employee, LeaveRequest request) {
        LocalDate cursor = request.getStartDate().withDayOfMonth(1);
        while (!cursor.isAfter(request.getEndDate())) {
            if (payrollResultRepository.existsByCompanyIdAndEmployeeIdAndYearAndMonthAndStatusIn(
                    employee.getCompany().getId(), employee.getId(),
                    cursor.getYear(), cursor.getMonthValue(), LOCKED_PAYROLL_STATUSES)) {
                throw new BusinessException(HttpStatus.CONFLICT, "PAYROLL_PERIOD_LOCKED",
                        "Cannot cancel approved leave — payroll for " + cursor.getYear() + "/" + cursor.getMonthValue()
                                + " is already approved, finalized, or paid.");
            }
            cursor = cursor.plusMonths(1);
        }
    }

    private LeaveRequest loadRequest(UUID requestId, UUID companyId) {
        return leaveRequestRepository.findByIdAndCompanyId(requestId, companyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "LEAVE_REQUEST_NOT_FOUND",
                        "Leave request was not found."));
    }

    private Employee resolveCurrentEmployee(AuthSessionPrincipal principal) {
        Employee employee = employeeRepository
                .findByUserIdAndCompanyId(principal.userId(), principal.companyId())
                .orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "EMPLOYEE_PROFILE_NOT_FOUND",
                        "Employee profile is not configured."));
        if (employee.getEmploymentStatus() != EmploymentStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "EMPLOYEE_INACTIVE", "Employee is not active.");
        }
        return employee;
    }

    private LeaveBalance findOrInitBalance(Employee employee, LeaveType type, int year) {
        return leaveBalanceRepository
                .findByCompanyIdAndEmployeeIdAndYearAndLeaveType(
                        employee.getCompany().getId(), employee.getId(), year, type)
                .orElseGet(() -> createDefaultBalance(employee, type, year));
    }

    private LeaveBalance createDefaultBalance(Employee employee, LeaveType type, int year) {
        LeaveBalance balance = new LeaveBalance();
        balance.setCompany(employee.getCompany());
        balance.setEmployee(employee);
        balance.setYear(year);
        balance.setLeaveType(type);
        balance.setTotalDays(BigDecimal.valueOf(defaultDays(employee, type)));
        balance.setUsedDays(BigDecimal.ZERO);
        return leaveBalanceRepository.save(balance);
    }

    private int defaultDays(Employee employee, LeaveType type) {
        return switch (type) {
            case VACATION, MATERNITY, PATERNITY, OTHER -> employee.getLeaveDaysPerYear() != null ? employee.getLeaveDaysPerYear() : 20;
            case SICK -> 10;
            case PERSONAL -> 5;
            case UNPAID -> 0;
        };
    }

    private User loadReviewer(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND",
                        "Reviewer user not found."));
    }

    private LeaveRequestDto toDto(LeaveRequest lr) {
        Employee employee = lr.getEmployee();
        User user = employee.getUser();
        String employeeName = (user.getDisplayName() != null && !user.getDisplayName().isBlank())
                ? user.getDisplayName()
                : user.getFirstName() + " " + user.getLastName();
        String siteName = employee.getCompanySite() != null ? employee.getCompanySite().getName() : null;
        String departmentName = employee.getDepartment() != null ? employee.getDepartment().getName() : null;

        return new LeaveRequestDto(
                lr.getId(),
                employee.getId(),
                employeeName,
                siteName,
                departmentName,
                lr.getLeaveType(),
                lr.getStartDate(),
                lr.getEndDate(),
                lr.getDaysCount(),
                lr.getStatus(),
                lr.getNote(),
                lr.getApprovalNote(),
                lr.getMedicalReportDocumentId(),
                lr.getRejectionReason(),
                lr.getReviewedAt(),
                lr.getCreatedAt()
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

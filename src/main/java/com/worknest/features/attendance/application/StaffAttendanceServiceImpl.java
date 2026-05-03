package com.worknest.features.attendance.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.worknest.common.exception.BusinessException;
import com.worknest.domain.entities.AttendanceDayRecord;
import com.worknest.domain.entities.AttendanceEvent;
import com.worknest.domain.entities.AttendanceReviewAction;
import com.worknest.domain.entities.CompanySite;
import com.worknest.domain.entities.Employee;
import com.worknest.domain.entities.User;
import com.worknest.domain.enums.AttendanceCaptureMethod;
import com.worknest.domain.enums.AttendanceDayStatus;
import com.worknest.domain.enums.AttendanceDecision;
import com.worknest.domain.enums.AttendanceEventStatus;
import com.worknest.domain.enums.AttendanceEventType;
import com.worknest.domain.enums.AttendanceQrValidationStatus;
import com.worknest.domain.enums.AttendanceReviewActionType;
import com.worknest.domain.enums.AttendanceReviewStatus;
import com.worknest.domain.enums.AttendanceState;
import com.worknest.domain.enums.GeofenceDecision;
import com.worknest.domain.enums.NetworkDecision;
import com.worknest.domain.enums.PlatformRole;
import com.worknest.features.attendance.dto.AdjustAttendanceDayRecordRequest;
import com.worknest.features.attendance.dto.AttendanceDashboardResponse;
import com.worknest.features.attendance.dto.AttendanceDashboardRowDto;
import com.worknest.features.attendance.dto.AttendanceEventDetailDto;
import com.worknest.features.attendance.dto.AttendanceSummaryDto;
import com.worknest.features.attendance.dto.DismissWarningsRequest;
import com.worknest.features.attendance.dto.EmployeeAttendanceDayDetailDto;
import com.worknest.features.attendance.dto.ManualAttendanceRequest;
import com.worknest.features.attendance.dto.ManualCheckInRequest;
import com.worknest.features.attendance.dto.ManualCheckOutRequest;
import com.worknest.features.attendance.dto.ReviewAttendanceEventRequest;
import com.worknest.features.attendance.repository.AttendanceDayRecordRepository;
import com.worknest.features.attendance.repository.AttendanceEventRepository;
import com.worknest.features.attendance.repository.AttendanceReviewActionRepository;
import com.worknest.features.auth.repository.UserRepository;
import com.worknest.features.company.repository.CompanyRepository;
import com.worknest.features.companySite.exception.SiteNotFoundException;
import com.worknest.features.companySite.repository.CompanySiteRepository;
import com.worknest.features.employee.repository.EmployeeRepository;
import com.worknest.security.AuthSessionPrincipal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class StaffAttendanceServiceImpl implements StaffAttendanceService {

    private final EmployeeRepository employeeRepository;
    private final CompanySiteRepository companySiteRepository;
    private final AttendanceDayRecordRepository attendanceDayRecordRepository;
    private final AttendanceEventRepository attendanceEventRepository;
    private final AttendanceReviewActionRepository attendanceReviewActionRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public AttendanceDashboardResponse dashboard(LocalDate date, UUID departmentId, UUID siteId) {
        AuthSessionPrincipal principal = principal();
        UUID companyId = principal.companyId();

        com.worknest.domain.entities.Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "COMPANY_NOT_FOUND", "Company not found."));
        ZoneId companyZone = ZoneId.of(company.getTimezone());

        List<Employee> employees;
        if (isAdmin(principal.role())) {
            employees = employeeRepository.findByCompanyAndRolesAndDepartmentAndEmploymentStatusNotPendingAndTimeWithinContract(
                    companyId, List.of(PlatformRole.EMPLOYEE, PlatformRole.STAFF), departmentId
            );
            if (siteId != null) {
                employees = employees.stream()
                        .filter(e -> e.getCompanySite() != null && siteId.equals(e.getCompanySite().getId()))
                        .toList();
            }
        } else {
            employees = employeeRepository.findAllAssignedToManager(
                    companyId, PlatformRole.EMPLOYEE, principal.roleAssignmentId()
            );
        }

        LocalDate resolvedDate = date != null ? date : LocalDate.now(companyZone);

        employees = employees.stream()
                .filter(emp -> emp.getStartDate() == null || !emp.getStartDate().isAfter(resolvedDate))
                .toList();

        List<UUID> employeeIds = employees.stream().map(Employee::getId).toList();
        Map<UUID, AttendanceDayRecord> recordByEmployeeId = employeeIds.isEmpty()
                ? Collections.emptyMap()
                : attendanceDayRecordRepository
                        .findAllByCompanyIdAndEmployeeIdsAndWorkDate(companyId, employeeIds, resolvedDate)
                        .stream()
                        .collect(Collectors.toMap(r -> r.getEmployee().getId(), Function.identity()));

        List<AttendanceDashboardRowDto> rows = employees.stream()
                .map(emp -> buildDashboardRow(emp, recordByEmployeeId.get(emp.getId()), resolvedDate))
                .toList();

        AttendanceSummaryDto summary = computeSummary(rows);
        return new AttendanceDashboardResponse(resolvedDate, company.getTimezone(), summary, rows);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeAttendanceDayDetailDto getEmployeeDetail(UUID employeeId, LocalDate date) {
        AuthSessionPrincipal principal = principal();
        Employee employee = loadAndAssertScope(employeeId, principal);

        CompanySite site = employee.getCompanySite();
        String timezone = site != null ? site.getTimezone() : "UTC";
        LocalDate workDate = date != null ? date : LocalDate.now(ZoneId.of(timezone));

        AttendanceDayRecord record = attendanceDayRecordRepository
                .findByCompanyIdAndEmployeeIdAndWorkDate(principal.companyId(), employeeId, workDate)
                .orElse(null);

        List<AttendanceEvent> events = attendanceEventRepository
                .findAllByCompanyIdAndEmployeeIdAndWorkDateOrderByServerRecordedAtAsc(
                        principal.companyId(), employeeId, workDate
                );

        AttendanceState state = deriveState(record);

        List<AttendanceEventDetailDto> eventDtos = events.stream()
                .map(this::toEventDetailDto)
                .toList();

        return new EmployeeAttendanceDayDetailDto(
                record != null ? record.getId() : null,
                employee.getId(),
                employee.getUser().getId(),
                displayName(employee.getUser()),
                employee.getDepartment() != null ? employee.getDepartment().getName() : null,
                site != null ? site.getName() : null,
                workDate,
                timezone,
                record != null ? record.getDayStatus() : AttendanceDayStatus.ABSENT,
                state,
                record != null ? record.getFirstCheckInAt() : null,
                record != null ? record.getLastCheckOutAt() : null,
                record != null && record.getWorkedMinutes() != null ? record.getWorkedMinutes() : 0,
                record != null && record.getLateMinutes() != null ? record.getLateMinutes() : 0,
                record != null && record.getEarlyLeaveMinutes() != null ? record.getEarlyLeaveMinutes() : 0,
                record != null && record.getOvertimeMinutes() != null ? record.getOvertimeMinutes() : 0,
                record != null && record.getBreakMinutes() != null ? record.getBreakMinutes() : 0,
                record != null && Boolean.TRUE.equals(record.getHasWarnings()),
                parseWarningFlags(record != null ? record.getWarningFlagsJson() : null),
                record != null ? record.getReviewStatus() : AttendanceReviewStatus.NONE,
                record != null && Boolean.TRUE.equals(record.getPayrollLocked()),
                eventDtos
        );
    }

    @Override
    public void manualCheckIn(UUID employeeId, ManualCheckInRequest request) {
        AuthSessionPrincipal principal = principal();
        Employee employee = loadAndAssertScope(employeeId, principal);
        CompanySite site = resolveSiteForEmployee(employee, principal.companyId());
        createManualEventInternal(principal, employee, site, AttendanceEventType.MANUAL_CHECK_IN,
                request.eventAt(), request.reason());
    }

    @Override
    public void manualCheckOut(UUID employeeId, ManualCheckOutRequest request) {
        AuthSessionPrincipal principal = principal();
        Employee employee = loadAndAssertScope(employeeId, principal);
        CompanySite site = resolveSiteForEmployee(employee, principal.companyId());
        createManualEventInternal(principal, employee, site, AttendanceEventType.MANUAL_CHECK_OUT,
                request.eventAt(), request.reason());
    }

    @Override
    public void dismissWarnings(UUID recordId, DismissWarningsRequest request) {
        AuthSessionPrincipal principal = principal();
        AttendanceDayRecord record = attendanceDayRecordRepository
                .findByIdAndCompanyId(recordId, principal.companyId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "DAY_RECORD_NOT_FOUND", "Attendance day record was not found."));

        if (Boolean.TRUE.equals(record.getPayrollLocked())) {
            throw new BusinessException(HttpStatus.CONFLICT, "PAYROLL_LOCKED", "Payroll-locked records cannot be modified.");
        }

        if (principal.role() == PlatformRole.STAFF) {
            assertManagesEmployee(record.getEmployee().getId(), principal);
        }

        ZoneId recordZone = ZoneId.of(record.getTimezone());
        assertTodayOrAdmin(record.getWorkDate(), recordZone, principal.role());

        record.setHasWarnings(false);
        record.setReviewStatus(AttendanceReviewStatus.APPROVED);
        attendanceDayRecordRepository.save(record);

        User actor = userRepository.findById(principal.userId())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "Current user not found."));

        AttendanceReviewAction action = new AttendanceReviewAction();
        action.setCompany(record.getCompany());
        action.setAttendanceDayRecord(record);
        action.setActionType(AttendanceReviewActionType.WARNING_DISMISSED);
        action.setReason(request.note());
        action.setActedBy(actor);
        action.setActedAt(Instant.now());
        attendanceReviewActionRepository.save(action);
    }

    @Override
    public void createManualEvent(ManualAttendanceRequest request) {
        AuthSessionPrincipal principal = principal();
        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "EMPLOYEE_NOT_FOUND", "Employee was not found."));
        if (!employee.getCompany().getId().equals(principal.companyId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Cross-tenant access is not allowed.");
        }
        if (principal.role() == PlatformRole.STAFF) {
            assertManagesEmployee(request.employeeId(), principal);
        }
        CompanySite site = companySiteRepository.findByIdAndCompanyId(request.siteId(), principal.companyId())
                .orElseThrow(SiteNotFoundException::new);

        createManualEventInternal(principal, employee, site, request.eventType(), request.eventAt(), request.reason());
    }

    @Override
    public void reviewEvent(UUID eventId, ReviewAttendanceEventRequest request) {
        AuthSessionPrincipal principal = principal();
        AttendanceEvent event = attendanceEventRepository.findByIdAndCompanyId(eventId, principal.companyId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND", "Attendance event was not found."));

        ZoneId eventZone = ZoneId.of(event.getTimezone() != null ? event.getTimezone() : "UTC");
        assertTodayOrAdmin(event.getWorkDate(), eventZone, principal.role());

        User reviewer = userRepository.findById(principal.userId())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "REVIEWER_NOT_FOUND", "Current user not found."));
        event.setReviewStatus(request.reviewStatus());
        event.setReviewedAt(Instant.now());
        event.setReviewedBy(reviewer);
        event.setReviewNote(request.note());
        attendanceEventRepository.save(event);
    }

    @Override
    public void adjustDayRecord(UUID recordId, AdjustAttendanceDayRecordRequest request) {
        AuthSessionPrincipal principal = principal();
        AttendanceDayRecord record = attendanceDayRecordRepository.findByIdAndCompanyId(recordId, principal.companyId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "DAY_RECORD_NOT_FOUND", "Attendance day record was not found."));
        if (Boolean.TRUE.equals(record.getPayrollLocked())) {
            throw new BusinessException(HttpStatus.CONFLICT, "PAYROLL_LOCKED", "Payroll-locked records cannot be adjusted.");
        }

        ZoneId recordZone = ZoneId.of(record.getTimezone());
        assertTodayOrAdmin(record.getWorkDate(), recordZone, principal.role());

        if (request.firstCheckInAt() != null && request.lastCheckOutAt() != null
                && request.lastCheckOutAt().isBefore(request.firstCheckInAt())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_TIME_RANGE",
                    "Check-out time must not be before check-in time.");
        }
        if (request.workedMinutes() != null && request.workedMinutes() < 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_WORKED_MINUTES",
                    "Worked minutes must not be negative.");
        }

        record.setFirstCheckInAt(request.firstCheckInAt());
        record.setLastCheckOutAt(request.lastCheckOutAt());
        if (request.workedMinutes() != null) {
            record.setWorkedMinutes(request.workedMinutes());
        } else if (request.firstCheckInAt() != null && request.lastCheckOutAt() != null) {
            record.setWorkedMinutes((int) Math.max(0L, java.time.Duration.between(request.firstCheckInAt(), request.lastCheckOutAt()).toMinutes()));
        }
        record.setDayStatus(request.dayStatus());
        record.setReviewStatus(AttendanceReviewStatus.PENDING_REVIEW);
        attendanceDayRecordRepository.save(record);
    }

    // --- helpers ---

    private void createManualEventInternal(
            AuthSessionPrincipal principal,
            Employee employee,
            CompanySite site,
            AttendanceEventType eventType,
            Instant eventAt,
            String reason
    ) {
        Instant resolvedAt = eventAt != null ? eventAt : Instant.now();
        ZoneId siteZone = ZoneId.of(site.getTimezone());
        LocalDate workDate = resolvedAt.atZone(siteZone).toLocalDate();

        assertTodayOrAdmin(workDate, siteZone, principal.role());

        AttendanceEvent event = new AttendanceEvent();
        event.setCompany(employee.getCompany());
        event.setEmployee(employee);
        event.setUser(employee.getUser());
        event.setSite(site);
        event.setEventType(eventType);
        event.setEventStatus(AttendanceEventStatus.ACCEPTED_WITH_WARNINGS);
        event.setCaptureMethod(AttendanceCaptureMethod.MANUAL);
        event.setAttendanceDecision(AttendanceDecision.ACCEPTED_WITH_WARNINGS);
        event.setServerRecordedAt(resolvedAt);
        event.setWorkDate(workDate);
        event.setTimezone(site.getTimezone());
        event.setQrValidationStatus(AttendanceQrValidationStatus.NOT_REQUIRED);
        event.setGeofenceDecision(GeofenceDecision.NOT_REQUIRED);
        event.setNetworkDecision(NetworkDecision.NOT_CONFIGURED);
        event.setEmployeeNote(reason);
        event.setReviewStatus(AttendanceReviewStatus.NONE);
        event.setWarningFlagsJson("[\"MANUAL_ENTRY\"]");
        attendanceEventRepository.save(event);

        AttendanceDayRecord dayRecord = attendanceDayRecordRepository
                .findByCompanyIdAndEmployeeIdAndWorkDate(principal.companyId(), employee.getId(), workDate)
                .orElseGet(() -> {
                    AttendanceDayRecord created = new AttendanceDayRecord();
                    created.setCompany(employee.getCompany());
                    created.setEmployee(employee);
                    created.setUser(employee.getUser());
                    created.setSite(site);
                    created.setWorkDate(workDate);
                    created.setTimezone(site.getTimezone());
                    return created;
                });

        if (eventType == AttendanceEventType.MANUAL_CHECK_IN) {
            dayRecord.setFirstCheckInAt(resolvedAt);
        } else if (eventType == AttendanceEventType.MANUAL_CHECK_OUT) {
            dayRecord.setLastCheckOutAt(resolvedAt);
        }

        if (dayRecord.getFirstCheckInAt() != null) {
            dayRecord.setDayStatus(AttendanceDayStatus.PRESENT);
            if (dayRecord.getLastCheckOutAt() != null) {
                int worked = (int) Math.max(
                        0L,
                        java.time.Duration.between(dayRecord.getFirstCheckInAt(), dayRecord.getLastCheckOutAt()).toMinutes()
                );
                dayRecord.setWorkedMinutes(worked);
            }
        }

        dayRecord.setHasWarnings(true);
        dayRecord.setWarningFlagsJson("[\"MANUAL_ENTRY\"]");
        dayRecord.setReviewStatus(AttendanceReviewStatus.PENDING_REVIEW);
        attendanceDayRecordRepository.save(dayRecord);
    }


    private AttendanceDashboardRowDto buildDashboardRow(Employee emp, AttendanceDayRecord record, LocalDate workDate) {
        AttendanceState state = deriveState(record);
        return new AttendanceDashboardRowDto(
                record != null ? record.getId() : null,
                emp.getId(),
                emp.getUser().getId(),
                displayName(emp.getUser()),
                emp.getDepartment() != null ? emp.getDepartment().getId() : null,
                emp.getDepartment() != null ? emp.getDepartment().getName() : null,
                emp.getCompanySite() != null ? emp.getCompanySite().getId() : null,
                emp.getCompanySite() != null ? emp.getCompanySite().getName() : null,
                state,
                record != null ? record.getDayStatus() : AttendanceDayStatus.ABSENT,
                record != null ? record.getFirstCheckInAt() : null,
                record != null ? record.getLastCheckOutAt() : null,
                record != null && record.getWorkedMinutes() != null ? record.getWorkedMinutes() : 0,
                record != null && record.getLateMinutes() != null ? record.getLateMinutes() : 0,
                record != null && Boolean.TRUE.equals(record.getHasWarnings()),
                record != null ? record.getReviewStatus() : AttendanceReviewStatus.NONE,
                record != null && Boolean.TRUE.equals(record.getPayrollLocked())
        );
    }

    private AttendanceSummaryDto computeSummary(List<AttendanceDashboardRowDto> rows) {
        int total = rows.size();
        int present = 0, absent = 0, late = 0, onLeave = 0, withWarnings = 0;
        for (AttendanceDashboardRowDto row : rows) {
            if (row.dayStatus() == AttendanceDayStatus.ON_LEAVE) {
                onLeave++;
            } else if (row.attendanceState() == AttendanceState.CHECKED_IN
                    || row.attendanceState() == AttendanceState.CHECKED_OUT) {
                present++;
            } else {
                absent++;
            }
            if (row.lateMinutes() > 0) {
                late++;
            }
            if (row.hasWarnings()) {
                withWarnings++;
            }
        }
        return new AttendanceSummaryDto(total, present, absent, late, onLeave, withWarnings);
    }

    private AttendanceState deriveState(AttendanceDayRecord record) {
        if (record == null || record.getFirstCheckInAt() == null) {
            return AttendanceState.NOT_CHECKED_IN;
        }
        if (record.getLastCheckOutAt() == null) {
            return AttendanceState.CHECKED_IN;
        }
        return AttendanceState.CHECKED_OUT;
    }

    private AttendanceEventDetailDto toEventDetailDto(AttendanceEvent event) {
        String reviewedByName = event.getReviewedBy() != null ? displayName(event.getReviewedBy()) : null;
        return new AttendanceEventDetailDto(
                event.getId(),
                event.getEventType(),
                event.getCaptureMethod(),
                event.getServerRecordedAt(),
                event.getAttendanceDecision(),
                parseWarningFlags(event.getWarningFlagsJson()),
                event.getGeofenceDecision(),
                event.getNetworkDecision(),
                event.getReviewStatus(),
                event.getEmployeeNote(),
                event.getReviewNote(),
                reviewedByName,
                event.getReviewedAt()
        );
    }

    private List<String> parseWarningFlags(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private Employee loadAndAssertScope(UUID employeeId, AuthSessionPrincipal principal) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "EMPLOYEE_NOT_FOUND", "Employee was not found."));
        if (!employee.getCompany().getId().equals(principal.companyId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Cross-tenant access is not allowed.");
        }
        if (principal.role() == PlatformRole.STAFF) {
            assertManagesEmployee(employeeId, principal);
        }
        return employee;
    }

    private void assertManagesEmployee(UUID employeeId, AuthSessionPrincipal principal) {
        boolean manages = employeeRepository
                .findAllAssignedToManager(principal.companyId(), PlatformRole.EMPLOYEE, principal.roleAssignmentId())
                .stream()
                .anyMatch(e -> e.getId().equals(employeeId));
        if (!manages) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "You do not manage this employee.");
        }
    }

    private CompanySite resolveSiteForEmployee(Employee employee, UUID companyId) {
        if (employee.getCompanySite() != null) {
            return employee.getCompanySite();
        }
        return companySiteRepository.findAllByCompanyIdOrderByCreatedAtDesc(companyId).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "NO_SITE_FOUND", "No site configured for this company."));
    }

    private String displayName(User user) {
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
            return user.getDisplayName();
        }
        return user.getFirstName() + " " + user.getLastName();
    }

    private static boolean isAdmin(PlatformRole role) {
        return role == PlatformRole.ADMIN || role == PlatformRole.SUPERADMIN;
    }

    private static void assertTodayOrAdmin(LocalDate workDate, ZoneId siteZone, PlatformRole role) {
        if (!isAdmin(role) && workDate.isBefore(LocalDate.now(siteZone))) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "PREVIOUS_DAY_NOT_ALLOWED",
                    "Only admins can modify attendance records for previous dates.");
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

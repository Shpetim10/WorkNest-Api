package com.worknest.features.attendance.application;

import com.worknest.common.exception.BusinessException;
import com.worknest.domain.entities.AttendanceDayRecord;
import com.worknest.domain.entities.AttendanceEvent;
import com.worknest.domain.entities.CompanySite;
import com.worknest.domain.entities.Employee;
import com.worknest.domain.enums.AttendanceCaptureMethod;
import com.worknest.domain.enums.AttendanceDayStatus;
import com.worknest.domain.enums.AttendanceDecision;
import com.worknest.domain.enums.AttendanceEventStatus;
import com.worknest.domain.enums.AttendanceEventType;
import com.worknest.domain.enums.AttendanceQrValidationStatus;
import com.worknest.domain.enums.AttendanceReviewStatus;
import com.worknest.domain.enums.AttendanceState;
import com.worknest.domain.enums.GeofenceDecision;
import com.worknest.domain.enums.NetworkDecision;
import com.worknest.domain.enums.PlatformRole;
import com.worknest.features.attendance.dto.AdjustAttendanceDayRecordRequest;
import com.worknest.features.attendance.dto.ManualAttendanceRequest;
import com.worknest.features.attendance.dto.ReviewAttendanceEventRequest;
import com.worknest.features.attendance.dto.StaffTodayAttendanceItemDto;
import com.worknest.features.attendance.dto.StaffTodayAttendanceResponse;
import com.worknest.features.attendance.repository.AttendanceDayRecordRepository;
import com.worknest.features.attendance.repository.AttendanceEventRepository;
import com.worknest.features.companySite.exception.SiteNotFoundException;
import com.worknest.features.companySite.repository.CompanySiteRepository;
import com.worknest.features.employee.repository.EmployeeRepository;
import com.worknest.security.AuthSessionPrincipal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
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

    @Override
    @Transactional(readOnly = true)
    public StaffTodayAttendanceResponse today(java.util.UUID siteId, java.util.UUID departmentId) {
        AuthSessionPrincipal principal = principal();
        List<Employee> employees = employeeRepository.findByCompanyAndRolesAndDepartment(
                principal.companyId(),
                List.of(PlatformRole.EMPLOYEE, PlatformRole.STAFF),
                departmentId
        );

        LocalDate workDate = LocalDate.now();
        List<StaffTodayAttendanceItemDto> items = employees.stream()
                .filter(emp -> siteId == null || (emp.getCompanySite() != null && siteId.equals(emp.getCompanySite().getId())))
                .map(emp -> {
                    AttendanceDayRecord record = attendanceDayRecordRepository
                            .findByCompanyIdAndEmployeeIdAndWorkDate(principal.companyId(), emp.getId(), workDate)
                            .orElse(null);
                    AttendanceState state;
                    if (record == null || record.getFirstCheckInAt() == null) {
                        state = AttendanceState.NOT_CHECKED_IN;
                    } else if (record.getLastCheckOutAt() == null) {
                        state = AttendanceState.CHECKED_IN;
                    } else {
                        state = AttendanceState.CHECKED_OUT;
                    }
                    return new StaffTodayAttendanceItemDto(
                            emp.getId(),
                            emp.getUser().getId(),
                            emp.getUser().getDisplayName() != null ? emp.getUser().getDisplayName() : (emp.getUser().getFirstName() + " " + emp.getUser().getLastName()),
                            emp.getCompanySite() != null ? emp.getCompanySite().getId() : null,
                            state,
                            record != null ? record.getFirstCheckInAt() : null,
                            record != null ? record.getLastCheckOutAt() : null,
                            record != null && record.getWorkedMinutes() != null ? record.getWorkedMinutes() : 0
                    );
                })
                .toList();

        return new StaffTodayAttendanceResponse(workDate, "UTC", items);
    }

    @Override
    public void createManualEvent(ManualAttendanceRequest request) {
        AuthSessionPrincipal principal = principal();
        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "EMPLOYEE_NOT_FOUND", "Employee was not found."));
        if (!employee.getCompany().getId().equals(principal.companyId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Cross-tenant access is not allowed.");
        }
        CompanySite site = companySiteRepository.findByIdAndCompanyId(request.siteId(), principal.companyId())
                .orElseThrow(SiteNotFoundException::new);

        Instant eventAt = request.eventAt() != null ? request.eventAt() : Instant.now();
        LocalDate workDate = LocalDate.now(ZoneId.of(site.getTimezone()));

        AttendanceEvent event = new AttendanceEvent();
        event.setCompany(employee.getCompany());
        event.setEmployee(employee);
        event.setUser(employee.getUser());
        event.setSite(site);
        event.setEventType(request.eventType());
        event.setEventStatus(AttendanceEventStatus.ACCEPTED_WITH_WARNINGS);
        event.setCaptureMethod(AttendanceCaptureMethod.MANUAL);
        event.setAttendanceDecision(AttendanceDecision.ACCEPTED_WITH_WARNINGS);
        event.setServerRecordedAt(eventAt);
        event.setWorkDate(workDate);
        event.setTimezone(site.getTimezone());
        event.setQrValidationStatus(AttendanceQrValidationStatus.NOT_REQUIRED);
        event.setGeofenceDecision(GeofenceDecision.NOT_REQUIRED);
        event.setNetworkDecision(NetworkDecision.NOT_CONFIGURED);
        event.setEmployeeNote(request.note());
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
                    created.setDayStatus(AttendanceDayStatus.PENDING_REVIEW);
                    return created;
                });

        if (request.eventType() == AttendanceEventType.MANUAL_CHECK_IN) {
            dayRecord.setFirstCheckInAt(eventAt);
        } else if (request.eventType() == AttendanceEventType.MANUAL_CHECK_OUT) {
            dayRecord.setLastCheckOutAt(eventAt);
        }
        if (dayRecord.getFirstCheckInAt() != null && dayRecord.getLastCheckOutAt() != null) {
            int worked = (int) Math.max(0L, java.time.Duration.between(dayRecord.getFirstCheckInAt(), dayRecord.getLastCheckOutAt()).toMinutes());
            dayRecord.setWorkedMinutes(worked);
            dayRecord.setDayStatus(AttendanceDayStatus.PRESENT);
        }
        dayRecord.setHasWarnings(true);
        dayRecord.setWarningFlagsJson("[\"MANUAL_ENTRY\"]");
        attendanceDayRecordRepository.save(dayRecord);
    }

    @Override
    public void reviewEvent(java.util.UUID eventId, ReviewAttendanceEventRequest request) {
        AuthSessionPrincipal principal = principal();
        AttendanceEvent event = attendanceEventRepository.findByIdAndCompanyId(eventId, principal.companyId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND", "Attendance event was not found."));
        event.setReviewStatus(request.reviewStatus());
        event.setReviewedAt(Instant.now());
        event.setReviewNote(request.note());
        attendanceEventRepository.save(event);
    }

    @Override
    public void adjustDayRecord(java.util.UUID recordId, AdjustAttendanceDayRecordRequest request) {
        AuthSessionPrincipal principal = principal();
        AttendanceDayRecord record = attendanceDayRecordRepository.findByIdAndCompanyId(recordId, principal.companyId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "DAY_RECORD_NOT_FOUND", "Attendance day record was not found."));
        if (Boolean.TRUE.equals(record.getPayrollLocked())) {
            throw new BusinessException(HttpStatus.CONFLICT, "PAYROLL_LOCKED", "Payroll-locked records cannot be adjusted.");
        }

        record.setFirstCheckInAt(request.firstCheckInAt());
        record.setLastCheckOutAt(request.lastCheckOutAt());
        if (request.workedMinutes() != null) {
            record.setWorkedMinutes(request.workedMinutes());
        } else if (request.firstCheckInAt() != null && request.lastCheckOutAt() != null) {
            record.setWorkedMinutes((int) Math.max(0L, java.time.Duration.between(request.firstCheckInAt(), request.lastCheckOutAt()).toMinutes()));
        }
        record.setDayStatus(request.dayStatus());
        attendanceDayRecordRepository.save(record);
    }

    private AuthSessionPrincipal principal() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthSessionPrincipal principal)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "No authentication session found.");
        }
        return principal;
    }
}

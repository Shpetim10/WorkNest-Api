package com.worknest.features.attendance.application;

import com.worknest.common.exception.BusinessException;
import com.worknest.domain.entities.AttendanceDayRecord;
import com.worknest.features.attendance.dto.AdminAttendanceMonthlyReportResponse;
import com.worknest.features.attendance.dto.AdminAttendanceReportRowDto;
import com.worknest.features.attendance.repository.AttendanceDayRecordRepository;
import com.worknest.tenant.TenantContextHolder;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAttendanceReportServiceImpl implements AdminAttendanceReportService {

    private final AttendanceDayRecordRepository attendanceDayRecordRepository;

    @Override
    public AdminAttendanceMonthlyReportResponse monthly(UUID companyId, int year, int month, UUID siteId) {
        UUID tenantCompanyId = TenantContextHolder.get()
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "TENANT_CONTEXT_MISSING", "No tenant context found."))
                .companyId();
        if (!tenantCompanyId.equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Cross-tenant access is not allowed.");
        }

        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        List<AttendanceDayRecord> records = attendanceDayRecordRepository
                .findAllByCompanyIdAndWorkDateBetweenOrderByWorkDateAsc(companyId, from, to);

        List<AdminAttendanceReportRowDto> rows = records.stream()
                .filter(r -> siteId == null || r.getSite().getId().equals(siteId))
                .map(r -> new AdminAttendanceReportRowDto(
                        r.getEmployee().getId(),
                        r.getUser().getId(),
                        r.getUser().getDisplayName() != null ? r.getUser().getDisplayName() : (r.getUser().getFirstName() + " " + r.getUser().getLastName()),
                        r.getSite().getId(),
                        r.getWorkDate(),
                        r.getDayStatus(),
                        r.getFirstCheckInAt(),
                        r.getLastCheckOutAt(),
                        r.getWorkedMinutes() != null ? r.getWorkedMinutes() : 0
                ))
                .toList();

        return new AdminAttendanceMonthlyReportResponse(year, month, rows);
    }
}

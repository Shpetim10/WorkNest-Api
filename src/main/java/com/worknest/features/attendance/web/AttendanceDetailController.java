package com.worknest.features.attendance.web;

import com.worknest.common.api.ApiResponse;
import com.worknest.features.attendance.application.StaffAttendanceService;
import com.worknest.features.attendance.dto.EmployeeAttendanceDayDetailDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/attendance")
@Tag(name = "Attendance Detail", description = "Full day detail view for a single employee — accessible by managers (own team) and admins (any employee).")
public class AttendanceDetailController {

    private final StaffAttendanceService staffAttendanceService;

    @GetMapping("/employees/{employeeId}/detail")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('STAFF', 'ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Get full attendance day detail for an employee")
    public ApiResponse<EmployeeAttendanceDayDetailDto> detail(
            @PathVariable UUID employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.success("Employee attendance detail loaded",
                staffAttendanceService.getEmployeeDetail(employeeId, date));
    }
}

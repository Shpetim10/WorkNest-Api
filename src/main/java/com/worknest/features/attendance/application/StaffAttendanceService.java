package com.worknest.features.attendance.application;

import com.worknest.features.attendance.dto.AdjustAttendanceDayRecordRequest;
import com.worknest.features.attendance.dto.AttendanceDashboardResponse;
import com.worknest.features.attendance.dto.DismissWarningsRequest;
import com.worknest.features.attendance.dto.EmployeeAttendanceDayDetailDto;
import com.worknest.features.attendance.dto.ManualAttendanceRequest;
import com.worknest.features.attendance.dto.ManualCheckInRequest;
import com.worknest.features.attendance.dto.ManualCheckOutRequest;
import com.worknest.features.attendance.dto.ReviewAttendanceEventRequest;
import java.time.LocalDate;
import java.util.UUID;

public interface StaffAttendanceService {

    AttendanceDashboardResponse dashboard(LocalDate date, UUID departmentId, UUID siteId);

    EmployeeAttendanceDayDetailDto getEmployeeDetail(UUID employeeId, LocalDate date);

    void manualCheckIn(UUID employeeId, ManualCheckInRequest request);

    void manualCheckOut(UUID employeeId, ManualCheckOutRequest request);

    void dismissWarnings(UUID recordId, DismissWarningsRequest request);

    void createManualEvent(ManualAttendanceRequest request);

    void reviewEvent(UUID eventId, ReviewAttendanceEventRequest request);

    void adjustDayRecord(UUID recordId, AdjustAttendanceDayRecordRequest request);
}

package com.worknest.features.attendance.application;

import com.worknest.features.attendance.dto.AdjustAttendanceDayRecordRequest;
import com.worknest.features.attendance.dto.ManualAttendanceRequest;
import com.worknest.features.attendance.dto.ReviewAttendanceEventRequest;
import com.worknest.features.attendance.dto.StaffTodayAttendanceResponse;
import java.util.UUID;

public interface StaffAttendanceService {

    StaffTodayAttendanceResponse today(UUID siteId, UUID departmentId);

    void createManualEvent(ManualAttendanceRequest request);

    void reviewEvent(UUID eventId, ReviewAttendanceEventRequest request);

    void adjustDayRecord(UUID recordId, AdjustAttendanceDayRecordRequest request);
}

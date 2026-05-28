package com.worknest.features.dashboard.application;

import com.worknest.domain.enums.AttendanceDayStatus;
import com.worknest.domain.enums.AttendanceState;
import com.worknest.domain.enums.LeaveStatus;
import com.worknest.domain.enums.LeaveType;
import com.worknest.features.announcement.application.AnnouncementService;
import com.worknest.features.announcement.dto.MobileAnnouncementListItem;
import com.worknest.features.attendance.application.MobileAttendanceService;
import com.worknest.features.attendance.dto.MonthlyAttendanceDayDto;
import com.worknest.features.attendance.dto.TodayAttendanceResponse;
import com.worknest.features.dashboard.dto.StaffDashboardResponse;
import com.worknest.features.employee.application.MobileProfileService;
import com.worknest.features.employee.dto.MobileProfileResponse;
import com.worknest.features.leave.application.LeaveService;
import com.worknest.features.leave.dto.LeaveBalanceDto;
import com.worknest.features.leave.dto.LeaveRequestDto;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffDashboardServiceImpl implements StaffDashboardService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH);

    private final MobileProfileService profileService;
    private final MobileAttendanceService mobileAttendanceService;
    private final LeaveService leaveService;
    private final AnnouncementService announcementService;

    @Override
    public StaffDashboardResponse getDashboard() {
        StaffDashboardResponse.Header header = buildHeader();
        StaffDashboardResponse.MyAttendance myAttendance = buildMyAttendance();
        StaffDashboardResponse.MyLeave myLeave = buildMyLeave();
        List<StaffDashboardResponse.AnnouncementItem> announcements = buildAnnouncements();
        return new StaffDashboardResponse(header, myAttendance, myLeave, announcements);
    }

    private StaffDashboardResponse.Header buildHeader() {
        String displayName = null;
        String currentTimeLabel = null;
        String currentDateLabel = null;
        try {
            MobileProfileResponse profile = profileService.getMyProfile();
            if (profile != null) {
                String first = profile.firstName();
                String last = profile.lastName();
                if (first != null && last != null) {
                    displayName = first + " " + last;
                } else if (first != null) {
                    displayName = first;
                } else if (last != null) {
                    displayName = last;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch profile for staff dashboard header", e);
        }
        try {
            ZoneId zoneId = resolveTimezone();
            LocalDateTime now = LocalDateTime.now(zoneId);
            currentTimeLabel = now.format(TIME_FMT);
            currentDateLabel = now.format(DATE_FMT);
        } catch (Exception e) {
            log.warn("Failed to build time/date labels for staff dashboard header", e);
        }
        return new StaffDashboardResponse.Header(displayName, currentTimeLabel, currentDateLabel);
    }

    private StaffDashboardResponse.MyAttendance buildMyAttendance() {
        String checkInTime = null;
        String checkOutTime = null;
        String status = "ABSENT";
        Double hoursWorkedToday = null;
        Double hoursWorkedThisWeek = null;

        try {
            TodayAttendanceResponse today = mobileAttendanceService.getToday();
            if (today == null) {
                return new StaffDashboardResponse.MyAttendance(null, null, "ABSENT", null, null);
            }

            ZoneId zoneId = today.timezone() != null ? ZoneId.of(today.timezone()) : ZoneOffset.UTC;
            LocalDate workDate = today.workDate() != null ? today.workDate() : LocalDate.now(zoneId);

            if (today.clockIn() != null) {
                checkInTime = today.clockIn().format(TIME_FMT);
            }
            if (today.clockOut() != null) {
                checkOutTime = today.clockOut().format(TIME_FMT);
            }

            // Attendance status
            if (today.todayRecord() != null && today.todayRecord().dayStatus() == AttendanceDayStatus.ON_LEAVE) {
                status = "ON_LEAVE";
            } else if (today.state() == AttendanceState.CHECKED_IN) {
                status = "CHECKED_IN";
            } else if (today.state() == AttendanceState.CHECKED_OUT) {
                status = "CHECKED_OUT";
            } else {
                status = "ABSENT";
            }

            // Hours worked today
            if (today.clockIn() != null) {
                if (today.clockOut() != null) {
                    long mins = Duration.between(today.clockIn(), today.clockOut()).toMinutes();
                    hoursWorkedToday = Math.max(0, mins) / 60.0;
                } else if (today.state() == AttendanceState.CHECKED_IN) {
                    long mins = Duration.between(today.clockIn(), LocalDateTime.now(zoneId)).toMinutes();
                    hoursWorkedToday = Math.max(0, mins) / 60.0;
                }
            }

            // Hours worked this week
            hoursWorkedThisWeek = computeWeeklyHours(today, workDate, zoneId);

        } catch (Exception e) {
            log.warn("Failed to fetch attendance for staff dashboard", e);
        }

        return new StaffDashboardResponse.MyAttendance(checkInTime, checkOutTime, status, hoursWorkedToday, hoursWorkedThisWeek);
    }

    private double computeWeeklyHours(TodayAttendanceResponse today, LocalDate workDate, ZoneId zoneId) {
        LocalDate weekStart = workDate.with(DayOfWeek.MONDAY);
        long totalMinutes = 0;

        try {
            if (weekStart.getMonth() == workDate.getMonth()) {
                totalMinutes = sumMinutesFromMonth(
                        mobileAttendanceService.month(workDate.getYear(), workDate.getMonthValue()).days(),
                        weekStart, workDate, today, zoneId
                );
            } else {
                // Week spans two months (start of month edge case)
                totalMinutes += sumMinutesFromMonth(
                        mobileAttendanceService.month(weekStart.getYear(), weekStart.getMonthValue()).days(),
                        weekStart, weekStart.withDayOfMonth(weekStart.lengthOfMonth()), today, zoneId
                );
                totalMinutes += sumMinutesFromMonth(
                        mobileAttendanceService.month(workDate.getYear(), workDate.getMonthValue()).days(),
                        workDate.withDayOfMonth(1), workDate, today, zoneId
                );
            }
        } catch (Exception e) {
            log.warn("Failed to compute weekly hours for staff dashboard", e);
        }

        return totalMinutes / 60.0;
    }

    private long sumMinutesFromMonth(
            List<MonthlyAttendanceDayDto> days,
            LocalDate from, LocalDate to,
            TodayAttendanceResponse today,
            ZoneId zoneId
    ) {
        long total = 0;
        for (MonthlyAttendanceDayDto day : days) {
            if (day.date().isBefore(from) || day.date().isAfter(to)) {
                continue;
            }
            if (today.workDate() != null && day.date().equals(today.workDate())
                    && today.state() == AttendanceState.CHECKED_IN
                    && today.clockIn() != null) {
                // Use live time for today if currently checked in
                total += Math.max(0, Duration.between(today.clockIn(), LocalDateTime.now(zoneId)).toMinutes());
            } else {
                total += day.workedMinutes();
            }
        }
        return total;
    }

    private StaffDashboardResponse.MyLeave buildMyLeave() {
        Integer pendingRequests = null;
        Integer approvedThisMonth = null;
        Integer remainingDays = null;

        try {
            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            LocalDate monthStart = today.withDayOfMonth(1);
            LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

            Page<LeaveRequestDto> requests = leaveService.getMyRequests(PageRequest.of(0, 200));
            if (requests != null) {
                int pending = 0;
                int approvedDays = 0;
                for (LeaveRequestDto req : requests.getContent()) {
                    if (req.status() == LeaveStatus.PENDING) {
                        pending++;
                    }
                    if (req.status() == LeaveStatus.APPROVED
                            && req.startDate() != null && req.endDate() != null
                            && !req.endDate().isBefore(monthStart)
                            && !req.startDate().isAfter(monthEnd)) {
                        approvedDays += req.daysCount() != null ? req.daysCount().intValue() : 0;
                    }
                }
                pendingRequests = pending;
                approvedThisMonth = approvedDays;
            }
        } catch (Exception e) {
            log.warn("Failed to fetch leave requests for staff dashboard", e);
        }

        try {
            List<LeaveBalanceDto> balances = leaveService.getMyBalance();
            if (balances != null) {
                for (LeaveBalanceDto b : balances) {
                    if (b.leaveType() == LeaveType.VACATION && b.availableDays() != null) {
                        remainingDays = b.availableDays().intValue();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch leave balance for staff dashboard", e);
        }

        return new StaffDashboardResponse.MyLeave(pendingRequests, approvedThisMonth, remainingDays);
    }

    private List<StaffDashboardResponse.AnnouncementItem> buildAnnouncements() {
        List<StaffDashboardResponse.AnnouncementItem> items = new ArrayList<>();
        try {
            Page<MobileAnnouncementListItem> page = announcementService.listForEmployee(PageRequest.of(0, 5));
            if (page != null) {
                for (MobileAnnouncementListItem a : page.getContent()) {
                    items.add(new StaffDashboardResponse.AnnouncementItem(
                            a.id(),
                            a.title(),
                            a.contentPreview(),
                            formatRelativeTime(a.createdAt())
                    ));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch announcements for staff dashboard", e);
        }
        return items;
    }

    private String formatRelativeTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        long seconds = Duration.between(instant, Instant.now()).getSeconds();
        if (seconds < 60) {
            return "just now";
        }
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + " minute" + (minutes == 1 ? "" : "s") + " ago";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + " hour" + (hours == 1 ? "" : "s") + " ago";
        }
        long days = hours / 24;
        return days + " day" + (days == 1 ? "" : "s") + " ago";
    }

    private ZoneId resolveTimezone() {
        try {
            TodayAttendanceResponse today = mobileAttendanceService.getToday();
            if (today != null && today.timezone() != null) {
                return ZoneId.of(today.timezone());
            }
        } catch (Exception e) {
            log.debug("Could not resolve timezone from attendance; falling back to UTC", e);
        }
        return ZoneOffset.UTC;
    }
}

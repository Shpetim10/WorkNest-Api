package com.worknest.features.dashboard.application;

import com.worknest.features.announcement.application.AnnouncementService;
import com.worknest.features.attendance.application.MobileAttendanceService;
import com.worknest.features.attendance.dto.TodayAttendanceResponse;
import com.worknest.features.dashboard.dto.MobileDashboardResponse;
import com.worknest.features.leave.application.LeaveService;
import com.worknest.features.leave.dto.LeaveBalanceDto;
import com.worknest.features.leave.dto.LeaveRequestDto;
import com.worknest.features.payroll.application.PayrollService;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollCalculationResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
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
public class MobileDashboardServiceImpl implements MobileDashboardService {

    private final MobileAttendanceService mobileAttendanceService;
    private final LeaveService leaveService;
    private final PayrollService payrollService;
    private final AnnouncementService announcementService;

    @Override
    public MobileDashboardResponse getDashboard() {
        // 1. Attendance Check-In Time
        String checkInTime = null;
        try {
            TodayAttendanceResponse attendanceToday = mobileAttendanceService.getToday();
            if (attendanceToday != null && attendanceToday.clockIn() != null) {
                checkInTime = attendanceToday.clockIn().format(DateTimeFormatter.ofPattern("HH:mm"));
            }
        } catch (Exception e) {
            log.warn("Failed to fetch today's attendance for mobile dashboard", e);
        }

        // 2. Leave Balance Summary (2 leave types with most recent activity)
        List<MobileDashboardResponse.LeaveBalanceSummary> leaveBalances = new ArrayList<>();
        try {
            List<LeaveBalanceDto> balances = leaveService.getMyBalance();
            Page<LeaveRequestDto> requestsPage = leaveService.getMyRequests(PageRequest.of(0, 50));

            List<String> activeTypes = new ArrayList<>();
            if (requestsPage != null && requestsPage.getContent() != null) {
                for (LeaveRequestDto req : requestsPage.getContent()) {
                    String typeName = req.leaveType().name();
                    if (!activeTypes.contains(typeName)) {
                        activeTypes.add(typeName);
                        if (activeTypes.size() == 2) {
                            break;
                        }
                    }
                }
            }

            // Fallback to other types from balances if < 2 active types
            if (activeTypes.size() < 2 && balances != null) {
                for (LeaveBalanceDto b : balances) {
                    String typeName = b.leaveType().name();
                    if (!activeTypes.contains(typeName)) {
                        activeTypes.add(typeName);
                        if (activeTypes.size() == 2) {
                            break;
                        }
                    }
                }
            }

            // Map to response records
            for (String typeName : activeTypes) {
                int remainingDays = 0;
                if (balances != null) {
                    for (LeaveBalanceDto b : balances) {
                        if (b.leaveType().name().equals(typeName)) {
                            remainingDays = b.availableDays() != null ? b.availableDays().intValue() : 0;
                            break;
                        }
                    }
                }
                leaveBalances.add(new MobileDashboardResponse.LeaveBalanceSummary(typeName, remainingDays));
            }
        } catch (Exception e) {
            log.warn("Failed to fetch leave balances/activity for mobile dashboard", e);
        }

        // 3. Payroll (try current month first, fallback up to 6 months back)
        String latestPayrollMonth = null;
        BigDecimal latestPayrollNetPay = null;
        String latestPayrollCurrency = null;
        try {
            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            for (int i = 0; i <= 6; i++) {
                LocalDate targetDate = today.minusMonths(i);
                try {
                    PayrollCalculationResponse payroll = payrollService.previewCurrentEmployeePayroll(
                            targetDate.getYear(), targetDate.getMonthValue());
                    if (payroll != null && payroll.totals() != null && payroll.totals().netPay() != null) {
                        latestPayrollNetPay = payroll.totals().netPay();
                        latestPayrollCurrency = payroll.currency();
                        latestPayrollMonth = targetDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH));
                        break;
                    }
                } catch (Exception e) {
                    // Try next month back
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch payroll preview for mobile dashboard", e);
        }

        // 4. Announcements
        int announcementUnreadCount = 0;
        try {
            var unreadResponse = announcementService.getUnreadCount();
            if (unreadResponse != null) {
                announcementUnreadCount = (int) unreadResponse.count();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch unread announcements count for mobile dashboard", e);
        }

        String latestAnnouncementTitle = null;
        try {
            var announcements = announcementService.listForEmployee(PageRequest.of(0, 1));
            if (announcements != null && !announcements.getContent().isEmpty()) {
                latestAnnouncementTitle = announcements.getContent().get(0).title();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch latest announcement title for mobile dashboard", e);
        }

        return new MobileDashboardResponse(
                checkInTime,
                leaveBalances,
                latestPayrollMonth,
                latestPayrollNetPay,
                latestPayrollCurrency,
                announcementUnreadCount,
                latestAnnouncementTitle
        );
    }
}

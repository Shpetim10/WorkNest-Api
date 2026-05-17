package com.worknest.features.dashboard.application;

import com.worknest.common.exception.BusinessException;
import com.worknest.domain.entities.AttendanceDayRecord;
import com.worknest.domain.entities.LeaveRequest;
import com.worknest.domain.entities.User;
import com.worknest.domain.enums.EmploymentStatus;
import com.worknest.domain.enums.LeaveStatus;
import com.worknest.features.auth.repository.UserRepository;
import com.worknest.features.dashboard.dto.AdminDashboardResponse;
import com.worknest.features.employee.repository.EmployeeRepository;
import com.worknest.security.AuthSessionPrincipal;
import jakarta.persistence.EntityManager;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private static final DateTimeFormatter ACTIVITY_FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm").withZone(ZoneOffset.UTC);

    private static final String[] DAY_LABELS = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
    private static final String[] MONTH_LABELS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    private final EntityManager entityManager;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    // ─── public entry point ─────────────────────────────────────────────

    @Override
    public AdminDashboardResponse getDashboard(String period, String trendPeriod) {
        AuthSessionPrincipal principal = principal();
        UUID companyId = principal.companyId();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        // Header
        AdminDashboardResponse.Header header = buildHeader(principal);

        // KPIs
        long totalEmployees = employeeRepository.countByCompanyIdAndEmploymentStatus(companyId, EmploymentStatus.ACTIVE);
        long presentToday = countPresentToday(companyId, today);
        long onLeaveToday = countOnLeaveToday(companyId, today);
        long pendingRequests = countPendingRequests(companyId);
        AdminDashboardResponse.Kpis kpis = new AdminDashboardResponse.Kpis(totalEmployees, presentToday, onLeaveToday, pendingRequests);

        // Attendance trend
        List<AdminDashboardResponse.AttendanceTrendPoint> attendanceTrend = buildAttendanceTrend(companyId, trendPeriod, today);

        // Active days
        List<AdminDashboardResponse.ActiveDayPoint> activeDays = buildActiveDays(companyId, today);

        // Recent activity
        List<AdminDashboardResponse.ActivityItem> recentActivity = buildRecentActivity(companyId);

        // Quick stats
        List<AdminDashboardResponse.QuickStat> quickStats = buildQuickStats(companyId, period, today, totalEmployees);

        return new AdminDashboardResponse(header, kpis, attendanceTrend, activeDays, recentActivity, quickStats);
    }

    // ─── header ─────────────────────────────────────────────────────────

    private AdminDashboardResponse.Header buildHeader(AuthSessionPrincipal principal) {
        String displayName = userRepository.findById(principal.userId())
                .map(this::resolveDisplayName)
                .orElse("Admin");
        return new AdminDashboardResponse.Header(displayName, null, null);
    }

    // ─── KPI helpers ────────────────────────────────────────────────────

    private long countPresentToday(UUID companyId, LocalDate today) {
        return entityManager.createQuery(
                        "SELECT COUNT(a) FROM AttendanceDayRecord a " +
                                "WHERE a.company.id = :companyId AND a.workDate = :today AND a.firstCheckInAt IS NOT NULL",
                        Long.class)
                .setParameter("companyId", companyId)
                .setParameter("today", today)
                .getSingleResult();
    }

    private long countOnLeaveToday(UUID companyId, LocalDate today) {
        return entityManager.createQuery(
                        "SELECT COUNT(lr) FROM LeaveRequest lr " +
                                "WHERE lr.company.id = :companyId " +
                                "AND lr.status = :status " +
                                "AND lr.startDate <= :today AND lr.endDate >= :today",
                        Long.class)
                .setParameter("companyId", companyId)
                .setParameter("status", LeaveStatus.APPROVED)
                .setParameter("today", today)
                .getSingleResult();
    }

    private long countPendingRequests(UUID companyId) {
        return entityManager.createQuery(
                        "SELECT COUNT(lr) FROM LeaveRequest lr " +
                                "WHERE lr.company.id = :companyId AND lr.status = :status",
                        Long.class)
                .setParameter("companyId", companyId)
                .setParameter("status", LeaveStatus.PENDING)
                .getSingleResult();
    }

    // ─── attendance trend ───────────────────────────────────────────────

    private List<AdminDashboardResponse.AttendanceTrendPoint> buildAttendanceTrend(
            UUID companyId, String trendPeriod, LocalDate today) {

        String resolvedPeriod = (trendPeriod == null || trendPeriod.isBlank()) ? "this-week" : trendPeriod;

        return switch (resolvedPeriod) {
            case "this-week" -> buildWeekTrend(companyId, today);
            case "this-month" -> buildWeeklyTrendForMonth(companyId, today.withDayOfMonth(1));
            case "last-month" -> buildWeeklyTrendForMonth(companyId, today.minusMonths(1).withDayOfMonth(1));
            case "last-3-months" -> buildMonthTrend(companyId, today.minusMonths(2).withDayOfMonth(1), today);
            case "last-6-months" -> buildMonthTrend(companyId, today.minusMonths(5).withDayOfMonth(1), today);
            case "this-year" -> buildMonthTrend(companyId, today.withDayOfYear(1), today.withDayOfYear(1).plusMonths(11));
            default -> buildWeekTrend(companyId, today);
        };
    }

    private List<AdminDashboardResponse.AttendanceTrendPoint> buildWeekTrend(UUID companyId, LocalDate today) {
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);

        List<AttendanceDayRecord> records = entityManager.createQuery(
                        "SELECT a FROM AttendanceDayRecord a " +
                                "WHERE a.company.id = :companyId AND a.workDate BETWEEN :from AND :to AND a.firstCheckInAt IS NOT NULL",
                        AttendanceDayRecord.class)
                .setParameter("companyId", companyId)
                .setParameter("from", monday)
                .setParameter("to", sunday)
                .getResultList();

        Map<DayOfWeek, Long> byDay = records.stream()
                .collect(Collectors.groupingBy(r -> r.getWorkDate().getDayOfWeek(), Collectors.counting()));

        long max = byDay.values().stream().mapToLong(Long::longValue).max().orElse(1L);

        String[] labels = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        DayOfWeek[] days = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY};

        List<AdminDashboardResponse.AttendanceTrendPoint> result = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            long count = byDay.getOrDefault(days[i], 0L);
            double pct = max > 0 ? clampPercentage(count * 100.0 / max) : 0.0;
            result.add(new AdminDashboardResponse.AttendanceTrendPoint(labels[i], count, pct));
        }
        return result;
    }

    private List<AdminDashboardResponse.AttendanceTrendPoint> buildWeeklyTrendForMonth(UUID companyId, LocalDate monthStart) {
        LocalDate monthEnd = monthStart.with(TemporalAdjusters.lastDayOfMonth());

        List<AttendanceDayRecord> records = entityManager.createQuery(
                        "SELECT a FROM AttendanceDayRecord a " +
                                "WHERE a.company.id = :companyId AND a.workDate BETWEEN :from AND :to AND a.firstCheckInAt IS NOT NULL",
                        AttendanceDayRecord.class)
                .setParameter("companyId", companyId)
                .setParameter("from", monthStart)
                .setParameter("to", monthEnd)
                .getResultList();

        long[] weekCounts = new long[4];
        for (AttendanceDayRecord r : records) {
            int day = r.getWorkDate().getDayOfMonth();
            if (day <= 7) weekCounts[0]++;
            else if (day <= 14) weekCounts[1]++;
            else if (day <= 21) weekCounts[2]++;
            else weekCounts[3]++;
        }

        long max = 0;
        for (long count : weekCounts) if (count > max) max = count;

        List<AdminDashboardResponse.AttendanceTrendPoint> result = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            long count = weekCounts[i];
            double pct = max > 0 ? clampPercentage(count * 100.0 / max) : 0.0;
            result.add(new AdminDashboardResponse.AttendanceTrendPoint("Week " + (i + 1), count, pct));
        }
        return result;
    }

    private List<AdminDashboardResponse.AttendanceTrendPoint> buildMonthTrend(
            UUID companyId, LocalDate from, LocalDate to) {

        List<AttendanceDayRecord> records = entityManager.createQuery(
                        "SELECT a FROM AttendanceDayRecord a " +
                                "WHERE a.company.id = :companyId AND a.workDate BETWEEN :from AND :to AND a.firstCheckInAt IS NOT NULL",
                        AttendanceDayRecord.class)
                .setParameter("companyId", companyId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();

        Map<Integer, Long> byMonth = records.stream()
                .collect(Collectors.groupingBy(r -> r.getWorkDate().getMonthValue(), Collectors.counting()));

        long max = byMonth.values().stream().mapToLong(Long::longValue).max().orElse(0L);
        List<AdminDashboardResponse.AttendanceTrendPoint> result = new ArrayList<>();

        YearMonth cursor = YearMonth.from(from);
        YearMonth end = YearMonth.from(to);
        while (!cursor.isAfter(end)) {
            int monthVal = cursor.getMonthValue();
            long count = byMonth.getOrDefault(monthVal, 0L);
            double pct = max > 0 ? clampPercentage(count * 100.0 / max) : 0.0;
            result.add(new AdminDashboardResponse.AttendanceTrendPoint(MONTH_LABELS[monthVal - 1], count, pct));
            cursor = cursor.plusMonths(1);
        }
        return result;
    }

    // ─── active days ────────────────────────────────────────────────────

    private List<AdminDashboardResponse.ActiveDayPoint> buildActiveDays(UUID companyId, LocalDate today) {
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.with(TemporalAdjusters.lastDayOfMonth());

        List<AttendanceDayRecord> records = entityManager.createQuery(
                        "SELECT a FROM AttendanceDayRecord a " +
                                "WHERE a.company.id = :companyId AND a.workDate BETWEEN :from AND :to AND a.firstCheckInAt IS NOT NULL",
                        AttendanceDayRecord.class)
                .setParameter("companyId", companyId)
                .setParameter("from", monthStart)
                .setParameter("to", monthEnd)
                .getResultList();

        // Map Java DayOfWeek (MONDAY=1..SUNDAY=7) to our Sun–Sat order
        Map<Integer, Long> bySunSatIndex = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) bySunSatIndex.put(i, 0L);

        records.forEach(r -> {
            int idx = dayOfWeekToSunSatIndex(r.getWorkDate().getDayOfWeek());
            bySunSatIndex.merge(idx, 1L, Long::sum);
        });

        long max = bySunSatIndex.values().stream().mapToLong(Long::longValue).max().orElse(1L);
        List<AdminDashboardResponse.ActiveDayPoint> result = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            long count = bySunSatIndex.get(i);
            double pct = max > 0 ? clampPercentage(count * 100.0 / max) : 0.0;
            result.add(new AdminDashboardResponse.ActiveDayPoint(DAY_LABELS[i], count, pct));
        }
        return result;
    }

    /** Maps Java's DayOfWeek (MONDAY=1..SUNDAY=7) to our Sun=0..Sat=6 index. */
    private int dayOfWeekToSunSatIndex(DayOfWeek dow) {
        return switch (dow) {
            case SUNDAY -> 0;
            case MONDAY -> 1;
            case TUESDAY -> 2;
            case WEDNESDAY -> 3;
            case THURSDAY -> 4;
            case FRIDAY -> 5;
            case SATURDAY -> 6;
        };
    }

    // ─── recent activity ────────────────────────────────────────────────

    private List<AdminDashboardResponse.ActivityItem> buildRecentActivity(UUID companyId) {
        List<LeaveRequest> requests = entityManager.createQuery(
                        "SELECT lr FROM LeaveRequest lr " +
                                "JOIN FETCH lr.employee e " +
                                "JOIN FETCH e.user u " +
                                "WHERE lr.company.id = :companyId " +
                                "ORDER BY lr.createdAt DESC",
                        LeaveRequest.class)
                .setParameter("companyId", companyId)
                .setMaxResults(10)
                .getResultList();

        return requests.stream()
                .map(lr -> {
                    User user = lr.getEmployee().getUser();
                    String actorName = resolveDisplayName(user);
                    String description = switch (lr.getStatus()) {
                        case PENDING -> "submitted a leave request";
                        case APPROVED -> "leave request approved";
                        case REJECTED -> "leave request rejected";
                        default -> "leave request updated";
                    };
                    String tag = switch (lr.getStatus()) {
                        case APPROVED -> "Leave";
                        default -> "Request";
                    };
                    return new AdminDashboardResponse.ActivityItem(
                            lr.getId().toString(),
                            actorName,
                            description,
                            tag,
                            ACTIVITY_FMT.format(lr.getCreatedAt())
                    );
                })
                .toList();
    }

    // ─── quick stats ────────────────────────────────────────────────────

    private List<AdminDashboardResponse.QuickStat> buildQuickStats(
            UUID companyId, String period, LocalDate today, long totalEmployees) {

        LocalDate[] range = filterByPeriod(period, today);
        LocalDate from = range[0];
        LocalDate to = range[1];

        // attendance-rate: employees who checked in at least once in the period / total active
        long employeesWhoCheckedIn = entityManager.createQuery(
                        "SELECT COUNT(DISTINCT a.employee.id) FROM AttendanceDayRecord a " +
                                "WHERE a.company.id = :companyId AND a.workDate BETWEEN :from AND :to AND a.firstCheckInAt IS NOT NULL",
                        Long.class)
                .setParameter("companyId", companyId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();

        double attendanceRate = totalEmployees > 0 ? clampPercentage(employeesWhoCheckedIn * 100.0 / totalEmployees) : 0.0;

        // TODO: Implement on-time check-ins when shift/schedule data is available
        double onTimeRate = 0.0;

        // leave-utilization: employees with at least one approved leave in period / total active
        long employeesWithLeave = entityManager.createQuery(
                        "SELECT COUNT(DISTINCT lr.employee.id) FROM LeaveRequest lr " +
                                "WHERE lr.company.id = :companyId " +
                                "AND lr.status = :status " +
                                "AND lr.startDate <= :to AND lr.endDate >= :from",
                        Long.class)
                .setParameter("companyId", companyId)
                .setParameter("status", LeaveStatus.APPROVED)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();

        double leaveUtilization = totalEmployees > 0 ? clampPercentage(employeesWithLeave * 100.0 / totalEmployees) : 0.0;

        return List.of(
                new AdminDashboardResponse.QuickStat("attendance-rate", "Attendance Rate", fmt(attendanceRate), attendanceRate),
                new AdminDashboardResponse.QuickStat("on-time-check-ins", "On-Time Check-Ins", "0%", onTimeRate),
                new AdminDashboardResponse.QuickStat("leave-utilization", "Leave Utilization", fmt(leaveUtilization), leaveUtilization)
        );
    }

    // ─── utility methods ────────────────────────────────────────────────

    private LocalDate[] filterByPeriod(String period, LocalDate today) {
        if (period == null || period.isBlank()) {
            return new LocalDate[]{today.withDayOfMonth(1), today};
        }
        return switch (period) {
            case "this-month" -> new LocalDate[]{today.withDayOfMonth(1), today};
            case "last-month" -> {
                YearMonth lm = YearMonth.from(today).minusMonths(1);
                yield new LocalDate[]{lm.atDay(1), lm.atEndOfMonth()};
            }
            case "last-3-months" -> new LocalDate[]{today.minusMonths(3).withDayOfMonth(1), today};
            case "last-6-months" -> new LocalDate[]{today.minusMonths(6).withDayOfMonth(1), today};
            case "this-year" -> new LocalDate[]{today.withDayOfYear(1), today};
            default -> new LocalDate[]{today.withDayOfMonth(1), today};
        };
    }

    private String resolveDisplayName(User user) {
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
            return user.getDisplayName();
        }
        return (user.getFirstName() + " " + user.getLastName()).trim();
    }

    private double clampPercentage(double pct) {
        return Math.min(100.0, Math.max(0.0, pct));
    }

    private String fmt(double pct) {
        return Math.round(pct) + "%";
    }

    private AuthSessionPrincipal principal() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthSessionPrincipal p)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "No authentication session found.");
        }
        return p;
    }
}

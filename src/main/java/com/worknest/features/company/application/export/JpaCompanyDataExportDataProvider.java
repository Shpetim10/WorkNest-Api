package com.worknest.features.company.application.export;

import com.worknest.audit.domain.AuditLog;
import com.worknest.domain.entities.Announcement;
import com.worknest.domain.entities.AttendanceDayRecord;
import com.worknest.domain.entities.CompanySite;
import com.worknest.domain.entities.Department;
import com.worknest.domain.entities.Employee;
import com.worknest.domain.entities.LeaveRequest;
import com.worknest.domain.entities.PayrollResult;
import com.worknest.domain.entities.RoleAssignment;
import com.worknest.domain.entities.User;
import com.worknest.domain.enums.AttendanceDayStatus;
import com.worknest.domain.enums.AttendanceReviewStatus;
import com.worknest.domain.enums.AttendanceState;
import com.worknest.domain.enums.PayrollAdjustmentType;
import com.worknest.domain.enums.PlatformRole;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class JpaCompanyDataExportDataProvider implements CompanyDataExportDataProvider {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);

    private final EntityManager entityManager;
    private final ExportLocalizationService localization;

    @Override
    public List<ExportWorkbookData> loadCompanyData(UUID companyId, String locale) {
        List<Employee> employees = employees(companyId, PlatformRole.EMPLOYEE);
        List<Employee> staff = employees(companyId, PlatformRole.STAFF);
        Map<UUID, RoleAssignment> employeeAssignments = assignmentsByUser(companyId, PlatformRole.EMPLOYEE);
        Map<UUID, RoleAssignment> staffAssignments = assignmentsByUser(companyId, PlatformRole.STAFF);
        Map<UUID, Long> assignedCounts = assignedEmployeeCounts(companyId);

        return List.of(
                employeeList(locale, employees, employeeAssignments),
                staffList(locale, staff, staffAssignments, assignedCounts),
                assignEmployees(locale, staff, staffAssignments, assignedCounts),
                attendance(locale, companyId),
                leaveRequests(locale, companyId),
                payroll(locale, companyId),
                locations(locale, companyId),
                departments(locale, companyId),
                announcements(locale, companyId),
                auditLog(locale, companyId)
        );
    }

    private ExportWorkbookData employeeList(String locale, List<Employee> employees, Map<UUID, RoleAssignment> assignments) {
        List<List<Object>> rows = employees.stream()
                .map(employee -> {
                    RoleAssignment assignment = assignments.get(employee.getUser().getId());
                    return row(
                            fullName(employee.getUser()),
                            localization.roleLabel(locale, employee.getEmploymentTypeRole()),
                            localization.employmentTypeLabel(locale, employee.getEmploymentType()),
                            employee.getUser().getEmail(),
                            name(employee.getDepartment()),
                            name(employee.getCompanySite()),
                            assignment == null ? "" : assignment.getJobTitle(),
                            localization.statusLabel(locale, employee.getEmploymentStatus())
                    );
                })
                .toList();

        return workbook(
                "employees/employee-list.xlsx",
                "employeeList",
                locale,
                List.of("name", "role", "employmentType", "email", "department", "location", "jobTitle", "status"),
                rows
        );
    }

    private ExportWorkbookData staffList(
            String locale,
            List<Employee> staff,
            Map<UUID, RoleAssignment> assignments,
            Map<UUID, Long> assignedCounts
    ) {
        List<List<Object>> rows = staff.stream()
                .map(employee -> {
                    RoleAssignment assignment = assignments.get(employee.getUser().getId());
                    return row(
                            fullName(employee.getUser()),
                            employee.getUser().getEmail(),
                            name(employee.getDepartment()),
                            name(employee.getCompanySite()),
                            assignment == null ? "" : assignment.getJobTitle(),
                            assignment == null ? 0L : assignedCounts.getOrDefault(assignment.getId(), 0L),
                            localization.statusLabel(locale, employee.getEmploymentStatus())
                    );
                })
                .toList();

        return workbook(
                "employees/staff-list.xlsx",
                "staffList",
                locale,
                List.of("name", "email", "department", "location", "jobTitle", "employees", "status"),
                rows
        );
    }

    private ExportWorkbookData assignEmployees(
            String locale,
            List<Employee> staff,
            Map<UUID, RoleAssignment> assignments,
            Map<UUID, Long> assignedCounts
    ) {
        List<List<Object>> rows = staff.stream()
                .map(employee -> {
                    RoleAssignment assignment = assignments.get(employee.getUser().getId());
                    return row(
                            fullName(employee.getUser()),
                            assignment == null ? "" : assignment.getJobTitle(),
                            name(employee.getDepartment()),
                            assignment == null ? 0L : assignedCounts.getOrDefault(assignment.getId(), 0L)
                    );
                })
                .toList();

        return workbook(
                "employees/assign-employees.xlsx",
                "assignEmployees",
                locale,
                List.of("name", "jobTitle", "department", "assignedEmployees"),
                rows
        );
    }

    private ExportWorkbookData attendance(String locale, UUID companyId) {
        List<AttendanceDayRecord> records = entityManager.createQuery("""
                        select distinct r
                        from AttendanceDayRecord r
                        join fetch r.employee e
                        join fetch e.user
                        left join fetch e.department
                        join fetch r.site
                        where r.company.id = :companyId
                        order by r.workDate desc, r.createdAt desc
                        """, AttendanceDayRecord.class)
                .setParameter("companyId", companyId)
                .getResultList();

        List<List<Object>> rows = records.stream()
                .map(record -> row(
                        fullName(record.getUser()),
                        name(record.getSite()),
                        name(record.getEmployee().getDepartment()),
                        localization.attendanceStateLabel(locale, attendanceState(record)),
                        timestamp(record.getFirstCheckInAt()),
                        timestamp(record.getLastCheckOutAt()),
                        localization.statusLabel(locale, record.getDayStatus()),
                        worked(record.getWorkedMinutes()),
                        Boolean.TRUE.equals(record.getHasWarnings())
                                ? localization.statusLabel(locale, AttendanceDayStatus.FLAGGED)
                                : ""
                ))
                .toList();

        return workbook(
                "attendance/attendance.xlsx",
                "attendance",
                locale,
                List.of("name", "site", "department", "status", "checkIn", "checkOut", "dayStatus", "worked", "warnings"),
                rows
        );
    }

    private ExportWorkbookData leaveRequests(String locale, UUID companyId) {
        List<LeaveRequest> requests = entityManager.createQuery("""
                        select distinct lr
                        from LeaveRequest lr
                        join fetch lr.employee e
                        join fetch e.user
                        left join fetch e.department
                        left join fetch e.companySite
                        where lr.company.id = :companyId
                        order by lr.createdAt desc
                        """, LeaveRequest.class)
                .setParameter("companyId", companyId)
                .getResultList();

        List<List<Object>> rows = requests.stream()
                .map(request -> row(
                        fullName(request.getEmployee().getUser()),
                        name(request.getEmployee().getCompanySite()),
                        name(request.getEmployee().getDepartment()),
                        localization.leaveTypeLabel(locale, request.getLeaveType()),
                        dateRange(request.getStartDate(), request.getEndDate()),
                        request.getDaysCount(),
                        localization.statusLabel(locale, request.getStatus())
                ))
                .toList();

        return workbook(
                "leave/leave-requests.xlsx",
                "leaveRequests",
                locale,
                List.of("name", "site", "department", "type", "dateRange", "days", "status"),
                rows
        );
    }

    private ExportWorkbookData payroll(String locale, UUID companyId) {
        List<PayrollResult> results = entityManager.createQuery("""
                        select distinct pr
                        from PayrollResult pr
                        join fetch pr.employee e
                        join fetch e.user
                        where pr.company.id = :companyId
                        order by pr.year desc, pr.month desc, pr.createdAt desc
                        """, PayrollResult.class)
                .setParameter("companyId", companyId)
                .getResultList();
        Map<PayrollAdjustmentKey, BigDecimal> adjustmentTotals = payrollAdjustmentTotals(companyId);

        List<List<Object>> rows = results.stream()
                .map(result -> {
                    Employee employee = result.getEmployee();
                    BigDecimal bonuses = adjustmentTotals.getOrDefault(
                            new PayrollAdjustmentKey(employee.getId(), result.getYear(), result.getMonth(), PayrollAdjustmentType.BONUS),
                            BigDecimal.ZERO
                    );
                    String employeeType = String.join(" / ", nonBlank(List.of(
                            localization.roleLabel(locale, employee.getEmploymentTypeRole()),
                            localization.employmentTypeLabel(locale, employee.getEmploymentType())
                    )));

                    return row(
                            fullName(employee.getUser()),
                            employeeType,
                            localization.paymentMethodLabel(locale, employee.getPaymentMethod()),
                            result.getBasePay(),
                            bonuses,
                            result.getTotalDeductions(),
                            result.getGrossEarnings(),
                            localization.payrollStatusLabel(locale, result.getStatus())
                    );
                })
                .toList();

        return workbook(
                "payroll/payroll.xlsx",
                "payroll",
                locale,
                List.of("employeeName", "typeOfEmployee", "payment", "basePay", "bonuses", "deductions", "grossEarnings", "status"),
                rows
        );
    }

    private ExportWorkbookData locations(String locale, UUID companyId) {
        List<CompanySite> sites = entityManager.createQuery("""
                        select s
                        from CompanySite s
                        where s.company.id = :companyId
                        order by s.createdAt desc
                        """, CompanySite.class)
                .setParameter("companyId", companyId)
                .getResultList();

        List<List<Object>> rows = sites.stream()
                .map(site -> row(
                        site.getName(),
                        site.getCode(),
                        localization.siteTypeLabel(locale, site.getType()),
                        countryName(site.getCountryCode(), locale),
                        localization.statusLabel(locale, site.getStatus()),
                        date(site.getCreatedAt())
                ))
                .toList();

        return workbook(
                "locations/locations.xlsx",
                "locations",
                locale,
                List.of("siteName", "siteCode", "siteType", "country", "status", "createdAt"),
                rows
        );
    }

    private ExportWorkbookData departments(String locale, UUID companyId) {
        List<Department> departments = entityManager.createQuery("""
                        select d
                        from Department d
                        where d.company.id = :companyId
                        order by d.createdAt desc
                        """, Department.class)
                .setParameter("companyId", companyId)
                .getResultList();
        Map<UUID, Long> employeeCounts = departmentEmployeeCounts(companyId);

        List<List<Object>> rows = departments.stream()
                .map(department -> row(
                        department.getName(),
                        localization.statusLabel(locale, department.getStatus()),
                        department.getDescription(),
                        employeeCounts.getOrDefault(department.getId(), 0L),
                        date(department.getCreatedAt())
                ))
                .toList();

        return workbook(
                "departments/departments.xlsx",
                "departments",
                locale,
                List.of("name", "status", "description", "employees", "createdAt"),
                rows
        );
    }

    private ExportWorkbookData announcements(String locale, UUID companyId) {
        List<Announcement> announcements = entityManager.createQuery("""
                        select distinct a
                        from Announcement a
                        left join fetch a.createdByUser
                        where a.company.id = :companyId
                        order by a.createdAt desc
                        """, Announcement.class)
                .setParameter("companyId", companyId)
                .getResultList();

        List<List<Object>> rows = announcements.stream()
                .map(announcement -> row(
                        announcement.getTitle(),
                        localization.audienceLabel(locale, announcement.getTargetAudience()),
                        localization.priorityLabel(locale, announcement.getPriority()),
                        fullName(announcement.getCreatedByUser()),
                        date(announcement.getCreatedAt()),
                        announcement.getContent()
                ))
                .toList();

        return workbook(
                "announcements/announcements.xlsx",
                "announcements",
                locale,
                List.of("title", "targetAudience", "priority", "createdBy", "createdAt", "content"),
                rows
        );
    }

    private ExportWorkbookData auditLog(String locale, UUID companyId) {
        List<AuditLog> logs = entityManager.createQuery("""
                        select a
                        from AuditLog a
                        where a.companyId = :companyId
                        order by a.createdAt desc
                        """, AuditLog.class)
                .setParameter("companyId", companyId)
                .getResultList();
        Map<UUID, User> users = usersById(logs.stream()
                .map(AuditLog::getActorUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        List<List<Object>> rows = logs.stream()
                .map(log -> {
                    User user = users.get(log.getActorUserId());
                    return row(
                            user == null ? string(log.getActorUserId()) : fullName(user),
                            localization.roleLabel(locale, log.getActorRole()),
                            log.getAction(),
                            auditDetails(log),
                            timestamp(log.getCreatedAt())
                    );
                })
                .toList();

        return workbook(
                "audit-log/audit-log.xlsx",
                "auditLog",
                locale,
                List.of("user", "role", "action", "details", "timestamp"),
                rows
        );
    }

    private List<Employee> employees(UUID companyId, PlatformRole role) {
        return entityManager.createQuery("""
                        select distinct e
                        from Employee e
                        join fetch e.user
                        left join fetch e.department
                        left join fetch e.companySite
                        left join fetch e.supervisorRoleAssignment
                        where e.company.id = :companyId
                          and e.employmentTypeRole = :role
                        order by e.createdAt asc
                        """, Employee.class)
                .setParameter("companyId", companyId)
                .setParameter("role", role)
                .getResultList();
    }

    private Map<UUID, RoleAssignment> assignmentsByUser(UUID companyId, PlatformRole role) {
        List<RoleAssignment> assignments = entityManager.createQuery("""
                        select distinct ra
                        from RoleAssignment ra
                        join fetch ra.user
                        where ra.company.id = :companyId
                          and ra.role = :role
                          and ra.isActive = true
                        order by ra.createdAt asc
                        """, RoleAssignment.class)
                .setParameter("companyId", companyId)
                .setParameter("role", role)
                .getResultList();

        Map<UUID, RoleAssignment> byUserId = new LinkedHashMap<>();
        for (RoleAssignment assignment : assignments) {
            byUserId.putIfAbsent(assignment.getUser().getId(), assignment);
        }
        return byUserId;
    }

    private Map<UUID, Long> assignedEmployeeCounts(UUID companyId) {
        List<Object[]> rows = entityManager.createQuery("""
                        select e.supervisorRoleAssignment.id, count(e)
                        from Employee e
                        where e.company.id = :companyId
                          and e.supervisorRoleAssignment is not null
                        group by e.supervisorRoleAssignment.id
                        """, Object[].class)
                .setParameter("companyId", companyId)
                .getResultList();

        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : rows) {
            counts.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    private Map<UUID, Long> departmentEmployeeCounts(UUID companyId) {
        List<Object[]> rows = entityManager.createQuery("""
                        select e.department.id, count(e)
                        from Employee e
                        where e.company.id = :companyId
                          and e.department is not null
                        group by e.department.id
                        """, Object[].class)
                .setParameter("companyId", companyId)
                .getResultList();

        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : rows) {
            counts.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    private Map<PayrollAdjustmentKey, BigDecimal> payrollAdjustmentTotals(UUID companyId) {
        List<Object[]> rows = entityManager.createQuery("""
                        select a.employee.id, a.year, a.month, a.type, sum(a.amount)
                        from PayrollAdjustment a
                        where a.company.id = :companyId
                        group by a.employee.id, a.year, a.month, a.type
                        """, Object[].class)
                .setParameter("companyId", companyId)
                .getResultList();

        Map<PayrollAdjustmentKey, BigDecimal> totals = new HashMap<>();
        for (Object[] row : rows) {
            PayrollAdjustmentKey key = new PayrollAdjustmentKey(
                    (UUID) row[0],
                    ((Number) row[1]).intValue(),
                    ((Number) row[2]).intValue(),
                    (PayrollAdjustmentType) row[3]
            );
            totals.put(key, (BigDecimal) row[4]);
        }
        return totals;
    }

    private Map<UUID, User> usersById(Set<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }

        return entityManager.createQuery("""
                        select u
                        from User u
                        where u.id in :ids
                        """, User.class)
                .setParameter("ids", ids)
                .getResultStream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private ExportWorkbookData workbook(
            String path,
            String sheetKey,
            String locale,
            List<String> headerKeys,
            List<List<Object>> rows
    ) {
        List<String> headers = headerKeys.stream()
                .map(key -> localization.header(locale, key))
                .toList();
        return new ExportWorkbookData(path, localization.sheet(locale, sheetKey), headers, rows);
    }

    private List<Object> row(Object... values) {
        return new ArrayList<>(Arrays.asList(values));
    }

    private String fullName(User user) {
        if (user == null) {
            return "";
        }
        if (StringUtils.hasText(user.getDisplayName())) {
            return user.getDisplayName().trim();
        }

        String fullName = String.join(" ", nonBlank(Arrays.asList(user.getFirstName(), user.getLastName()))).trim();
        return StringUtils.hasText(fullName) ? fullName : string(user.getEmail());
    }

    private String name(Department department) {
        return department == null ? "" : string(department.getName());
    }

    private String name(CompanySite site) {
        return site == null ? "" : string(site.getName());
    }

    private String string(Object value) {
        return value == null ? "" : value.toString();
    }

    private List<String> nonBlank(Collection<String> values) {
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
    }

    private String date(Instant value) {
        return value == null ? "" : DATE_FORMATTER.format(value);
    }

    private String timestamp(Instant value) {
        return value == null ? "" : TIMESTAMP_FORMATTER.format(value);
    }

    private String dateRange(LocalDate start, LocalDate end) {
        if (start == null && end == null) {
            return "";
        }
        if (Objects.equals(start, end)) {
            return string(start);
        }
        return string(start) + " - " + string(end);
    }

    private String worked(Integer minutes) {
        if (minutes == null || minutes <= 0) {
            return "";
        }
        int hours = minutes / 60;
        int remainder = minutes % 60;
        if (hours <= 0) {
            return remainder + "m";
        }
        return "%dh %02dm".formatted(hours, remainder);
    }

    private String countryName(String countryCode, String locale) {
        if (!StringUtils.hasText(countryCode)) {
            return "";
        }

        Locale countryLocale = new Locale("", countryCode.trim().toUpperCase(Locale.ROOT));
        String countryName = countryLocale.getDisplayCountry(localization.javaLocale(locale));
        return StringUtils.hasText(countryName) ? countryName : countryCode;
    }

    private AttendanceState attendanceState(AttendanceDayRecord record) {
        if (record.getReviewStatus() == AttendanceReviewStatus.PENDING_REVIEW) {
            return AttendanceState.PENDING_REVIEW;
        }
        if (record.getFirstCheckInAt() == null) {
            return AttendanceState.NOT_CHECKED_IN;
        }
        if (record.getLastCheckOutAt() == null) {
            return record.getDayStatus() == AttendanceDayStatus.MISSING_CHECKOUT
                    ? AttendanceState.MISSING_CHECKOUT
                    : AttendanceState.CHECKED_IN;
        }
        return AttendanceState.CHECKED_OUT;
    }

    private String auditDetails(AuditLog log) {
        if (log.getMetadata() != null && !log.getMetadata().isEmpty()) {
            return log.getMetadata().toString();
        }
        if (log.getDiff() != null && !log.getDiff().isEmpty()) {
            return log.getDiff().toString();
        }
        return "";
    }

    private record PayrollAdjustmentKey(
            UUID employeeId,
            int year,
            int month,
            PayrollAdjustmentType type
    ) {
    }
}

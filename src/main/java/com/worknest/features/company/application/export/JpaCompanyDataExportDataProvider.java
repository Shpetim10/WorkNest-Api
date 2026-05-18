package com.worknest.features.company.application.export;

import com.worknest.domain.enums.AttendanceDayStatus;
import com.worknest.domain.enums.AttendanceReviewStatus;
import com.worknest.domain.enums.AttendanceState;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
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
        List<ExportWorkbookData> workbooks = new ArrayList<>();
        workbooks.add(employeeList(locale, companyId));
        workbooks.add(staffList(locale, companyId));
        workbooks.add(assignEmployees(locale, companyId));
        workbooks.add(attendance(locale, companyId));
        workbooks.add(leaveRequests(locale, companyId));
        workbooks.add(payroll(locale, companyId));
        workbooks.add(locations(locale, companyId));
        workbooks.add(departments(locale, companyId));
        workbooks.add(announcements(locale, companyId));
        workbooks.add(auditLog(locale, companyId));
        return List.copyOf(workbooks);
    }

    private ExportWorkbookData employeeList(String locale, UUID companyId) {
        List<Object[]> employees = nativeRows("""
                        /* export:employee-list */
                        select
                            coalesce(nullif(u.display_name, ''), nullif(trim(concat(coalesce(u.first_name, ''), ' ', coalesce(u.last_name, ''))), ''), u.email) as employee_name,
                            e.employment_type_role,
                            e.employment_type,
                            u.email,
                            d.name as department_name,
                            s.name as site_name,
                            ra.job_title,
                            e.employment_status
                        from employees e
                        left join users u on u.id = e.user_id
                        left join departments d on d.id = e.department_id
                        left join company_sites s on s.id = e.company_site_id
                        left join role_assignments ra on ra.id = (
                            select ra2.id
                            from role_assignments ra2
                            where ra2.company_id = e.company_id
                              and ra2.user_id = e.user_id
                            order by case when ra2.is_active then 0 else 1 end, ra2.created_at asc
                            limit 1
                        )
                        where e.company_id = :companyId
                          and e.employment_type_role in ('EMPLOYEE', 'STAFF')
                        order by e.created_at asc
                        """, companyId);

        List<List<Object>> rows = employees.stream()
                .map(employee -> row(
                        string(employee[0]),
                        localization.roleLabel(locale, employee[1]),
                        localization.employmentTypeLabel(locale, employee[2]),
                        string(employee[3]),
                        string(employee[4]),
                        string(employee[5]),
                        string(employee[6]),
                        localization.statusLabel(locale, employee[7])
                ))
                .toList();

        return workbook(
                "employees/employee-list.xlsx",
                "employeeList",
                locale,
                List.of("name", "role", "employmentType", "email", "department", "location", "jobTitle", "status"),
                rows
        );
    }

    private ExportWorkbookData staffList(String locale, UUID companyId) {
        List<Object[]> staff = nativeRows("""
                        /* export:staff-list */
                        select
                            coalesce(nullif(u.display_name, ''), nullif(trim(concat(coalesce(u.first_name, ''), ' ', coalesce(u.last_name, ''))), ''), u.email) as staff_name,
                            u.email,
                            d.name as department_name,
                            s.name as site_name,
                            ra.job_title,
                            coalesce(assigned.assigned_count, 0) as assigned_count,
                            e.employment_status
                        from employees e
                        left join users u on u.id = e.user_id
                        left join departments d on d.id = e.department_id
                        left join company_sites s on s.id = e.company_site_id
                        left join role_assignments ra on ra.id = (
                            select ra2.id
                            from role_assignments ra2
                            where ra2.company_id = e.company_id
                              and ra2.user_id = e.user_id
                            order by case when ra2.is_active then 0 else 1 end, ra2.created_at asc
                            limit 1
                        )
                        left join (
                            select supervisor_role_assignment_id, count(*) as assigned_count
                            from employees
                            where company_id = :companyId
                              and supervisor_role_assignment_id is not null
                            group by supervisor_role_assignment_id
                        ) assigned on assigned.supervisor_role_assignment_id = ra.id
                        where e.company_id = :companyId
                          and e.employment_type_role in ('STAFF', 'ADMIN', 'SUPERADMIN')
                        order by e.created_at asc
                        """, companyId);

        List<List<Object>> rows = staff.stream()
                .map(employee -> row(
                        string(employee[0]),
                        string(employee[1]),
                        string(employee[2]),
                        string(employee[3]),
                        string(employee[4]),
                        employee[5],
                        localization.statusLabel(locale, employee[6])
                ))
                .toList();

        return workbook(
                "employees/staff-list.xlsx",
                "staffList",
                locale,
                List.of("name", "email", "department", "location", "jobTitle", "employees", "status"),
                rows
        );
    }

    private ExportWorkbookData assignEmployees(String locale, UUID companyId) {
        List<Object[]> managers = nativeRows("""
                        /* export:assign-employees */
                        select
                            coalesce(nullif(u.display_name, ''), nullif(trim(concat(coalesce(u.first_name, ''), ' ', coalesce(u.last_name, ''))), ''), u.email) as staff_name,
                            ra.job_title,
                            d.name as department_name,
                            coalesce(assigned.assigned_count, 0) as assigned_count
                        from employees e
                        left join users u on u.id = e.user_id
                        left join departments d on d.id = e.department_id
                        left join role_assignments ra on ra.id = (
                            select ra2.id
                            from role_assignments ra2
                            where ra2.company_id = e.company_id
                              and ra2.user_id = e.user_id
                            order by case when ra2.is_active then 0 else 1 end, ra2.created_at asc
                            limit 1
                        )
                        left join (
                            select supervisor_role_assignment_id, count(*) as assigned_count
                            from employees
                            where company_id = :companyId
                              and supervisor_role_assignment_id is not null
                            group by supervisor_role_assignment_id
                        ) assigned on assigned.supervisor_role_assignment_id = ra.id
                        where e.company_id = :companyId
                          and e.employment_type_role in ('STAFF', 'ADMIN', 'SUPERADMIN')
                        order by e.created_at asc
                        """, companyId);

        List<List<Object>> rows = managers.stream()
                .map(manager -> row(
                        string(manager[0]),
                        string(manager[1]),
                        string(manager[2]),
                        manager[3]
                ))
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
        List<Object[]> records = nativeRows("""
                        /* export:attendance */
                        select
                            coalesce(nullif(u.display_name, ''), nullif(trim(concat(coalesce(u.first_name, ''), ' ', coalesce(u.last_name, ''))), ''), u.email) as employee_name,
                            coalesce(record_site.name, employee_site.name) as site_name,
                            d.name as department_name,
                            coalesce(r.review_status, 'NONE') as review_status,
                            r.first_check_in_at,
                            r.last_check_out_at,
                            coalesce(r.day_status, 'ABSENT') as day_status,
                            coalesce(r.worked_minutes, 0) as worked_minutes,
                            coalesce(r.has_warnings, false) as has_warnings
                        from employees e
                        left join users u on u.id = e.user_id
                        left join departments d on d.id = e.department_id
                        left join company_sites employee_site on employee_site.id = e.company_site_id
                        left join lateral (
                            select r.*
                            from attendance_day_records r
                            where r.company_id = e.company_id
                              and r.employee_id = e.id
                            order by r.work_date desc, r.created_at desc
                            limit 1
                        ) r on true
                        left join company_sites record_site on record_site.id = r.site_id
                        where e.company_id = :companyId
                          and e.employment_type_role in ('EMPLOYEE', 'STAFF')
                          and e.employment_status <> 'PENDING'
                        order by coalesce(r.work_date, current_date) desc, e.created_at asc
                        """, companyId);

        List<List<Object>> rows = records.stream()
                .map(record -> row(
                        string(record[0]),
                        string(record[1]),
                        string(record[2]),
                        attendanceStateLabel(locale, record[3], record[4], record[5], record[6]),
                        timestamp(record[4]),
                        timestamp(record[5]),
                        localization.statusLabel(locale, record[6]),
                        worked(asInteger(record[7])),
                        Boolean.TRUE.equals(asBoolean(record[8]))
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
        List<Object[]> requests = nativeRows("""
                        /* export:leave */
                        select
                            coalesce(nullif(u.display_name, ''), nullif(trim(concat(coalesce(u.first_name, ''), ' ', coalesce(u.last_name, ''))), ''), u.email) as employee_name,
                            s.name as site_name,
                            d.name as department_name,
                            lr.leave_type,
                            lr.start_date,
                            lr.end_date,
                            lr.days_count,
                            lr.status
                        from leave_requests lr
                        left join employees e on e.id = lr.employee_id
                        left join users u on u.id = e.user_id
                        left join departments d on d.id = e.department_id
                        left join company_sites s on s.id = e.company_site_id
                        where lr.company_id = :companyId
                        order by lr.created_at desc
                        """, companyId);

        List<List<Object>> rows = requests.stream()
                .map(request -> row(
                        string(request[0]),
                        string(request[1]),
                        string(request[2]),
                        localization.leaveTypeLabel(locale, request[3]),
                        dateRange(request[4], request[5]),
                        request[6],
                        localization.statusLabel(locale, request[7])
                ))
                .toList();

        return workbook(
                "leave/leave.xlsx",
                "leaveRequests",
                locale,
                List.of("name", "site", "department", "type", "dateRange", "days", "status"),
                rows
        );
    }

    private ExportWorkbookData payroll(String locale, UUID companyId) {
        List<Object[]> results = nativeRows("""
                        /* export:payroll */
                        select
                            coalesce(nullif(u.display_name, ''), nullif(trim(concat(coalesce(u.first_name, ''), ' ', coalesce(u.last_name, ''))), ''), u.email) as employee_name,
                            e.employment_type_role,
                            e.employment_type,
                            e.payment_method,
                            pr.base_pay,
                            coalesce(b.bonuses, 0) as bonuses,
                            pr.total_deductions,
                            pr.gross_earnings,
                            pr.status
                        from payroll_results pr
                        left join employees e on e.id = pr.employee_id
                        left join users u on u.id = e.user_id
                        left join (
                            select employee_id, payroll_year, payroll_month, sum(amount) as bonuses
                            from payroll_adjustments
                            where company_id = :companyId
                              and adjustment_type = 'BONUS'
                            group by employee_id, payroll_year, payroll_month
                        ) b on b.employee_id = pr.employee_id
                            and b.payroll_year = pr.payroll_year
                            and b.payroll_month = pr.payroll_month
                        where pr.company_id = :companyId
                        order by pr.payroll_year desc, pr.payroll_month desc, pr.created_at desc
                        """, companyId);

        List<List<Object>> rows = results.stream()
                .map(result -> row(
                        string(result[0]),
                        String.join(" / ", nonBlank(List.of(
                                localization.roleLabel(locale, result[1]),
                                localization.employmentTypeLabel(locale, result[2])
                        ))),
                        localization.paymentMethodLabel(locale, result[3]),
                        result[4],
                        result[5],
                        result[6],
                        result[7],
                        localization.payrollStatusLabel(locale, result[8])
                ))
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
        List<Object[]> sites = nativeRows("""
                        /* export:locations */
                        select name, code, type, country_code, status, created_at
                        from company_sites
                        where company_id = :companyId
                        order by created_at desc
                        """, companyId);

        List<List<Object>> rows = sites.stream()
                .map(site -> row(
                        string(site[0]),
                        string(site[1]),
                        localization.siteTypeLabel(locale, site[2]),
                        countryName(string(site[3]), locale),
                        localization.statusLabel(locale, site[4]),
                        date(site[5])
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
        List<Object[]> departments = nativeRows("""
                        /* export:departments */
                        select d.name, d.status, d.description, count(e.id) as employee_count, d.created_at
                        from departments d
                        left join employees e on e.department_id = d.id and e.company_id = d.company_id
                        where d.company_id = :companyId
                        group by d.id, d.name, d.status, d.description, d.created_at
                        order by d.created_at desc
                        """, companyId);

        List<List<Object>> rows = departments.stream()
                .map(department -> row(
                        string(department[0]),
                        localization.statusLabel(locale, department[1]),
                        string(department[2]),
                        department[3],
                        date(department[4])
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
        List<Object[]> announcements = nativeRows("""
                        /* export:announcements */
                        select
                            a.title,
                            a.target_audience,
                            a.priority,
                            coalesce(nullif(u.display_name, ''), nullif(trim(concat(coalesce(u.first_name, ''), ' ', coalesce(u.last_name, ''))), ''), u.email) as created_by_name,
                            a.created_at,
                            a.content
                        from announcements a
                        left join users u on u.id = a.created_by_user_id
                        where a.company_id = :companyId
                        order by a.created_at desc
                        """, companyId);

        List<List<Object>> rows = announcements.stream()
                .map(announcement -> row(
                        string(announcement[0]),
                        localization.audienceLabel(locale, announcement[1]),
                        localization.priorityLabel(locale, announcement[2]),
                        string(announcement[3]),
                        date(announcement[4]),
                        string(announcement[5])
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
        List<Object[]> logs = nativeRows("""
                        /* export:audit-log */
                        select
                            coalesce(nullif(u.display_name, ''), nullif(trim(concat(coalesce(u.first_name, ''), ' ', coalesce(u.last_name, ''))), ''), u.email, cast(a.actor_user_id as text)) as actor_name,
                            a.actor_role,
                            a.action,
                            coalesce(nullif(cast(a.metadata as text), '{}'), nullif(cast(a.diff as text), '{}'), '') as details,
                            a.created_at
                        from audit_logs a
                        left join users u on u.id = a.actor_user_id
                        where a.company_id = :companyId
                        order by a.created_at desc
                        """, companyId);

        List<List<Object>> rows = logs.stream()
                .map(log -> row(
                        string(log[0]),
                        localization.roleLabel(locale, log[1]),
                        string(log[2]),
                        string(log[3]),
                        timestamp(log[4])
                ))
                .toList();

        return workbook(
                "audit-log/audit-log.xlsx",
                "auditLog",
                locale,
                List.of("user", "role", "action", "details", "timestamp"),
                rows
        );
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

    private String string(Object value) {
        return value == null ? "" : value.toString();
    }

    private List<String> nonBlank(Collection<String> values) {
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> nativeRows(String sql, UUID companyId) {
        return entityManager.createNativeQuery(sql)
                .setParameter("companyId", companyId)
                .getResultList();
    }

    private String date(Object value) {
        if (value == null) {
            return "";
        }

        LocalDate localDate = toLocalDate(value);
        if (localDate != null) {
            return localDate.toString();
        }

        Instant instant = toInstant(value);
        return instant == null ? string(value) : DATE_FORMATTER.format(instant);
    }

    private String timestamp(Object value) {
        if (value == null) {
            return "";
        }

        Instant instant = toInstant(value);
        return instant == null ? string(value) : TIMESTAMP_FORMATTER.format(instant);
    }

    private String dateRange(Object startValue, Object endValue) {
        LocalDate start = toLocalDate(startValue);
        LocalDate end = toLocalDate(endValue);
        if (start == null && end == null) {
            return "";
        }
        if (Objects.equals(start, end)) {
            return string(start);
        }
        return string(start) + " - " + string(end);
    }

    private LocalDate toLocalDate(Object value) {
        return switch (value) {
            case null -> null;
            case LocalDate localDate -> localDate;
            case java.sql.Date sqlDate -> sqlDate.toLocalDate();
            case Instant instant -> instant.atZone(ZoneOffset.UTC).toLocalDate();
            case Timestamp timestamp -> timestamp.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
            case LocalDateTime localDateTime -> localDateTime.toLocalDate();
            case OffsetDateTime offsetDateTime -> offsetDateTime.toLocalDate();
            case Date date -> date.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
            default -> {
                try {
                    yield LocalDate.parse(value.toString());
                } catch (RuntimeException ignored) {
                    yield null;
                }
            }
        };
    }

    private Instant toInstant(Object value) {
        return switch (value) {
            case null -> null;
            case Instant instant -> instant;
            case Timestamp timestamp -> timestamp.toInstant();
            case LocalDateTime localDateTime -> localDateTime.toInstant(ZoneOffset.UTC);
            case OffsetDateTime offsetDateTime -> offsetDateTime.toInstant();
            case Date date -> date.toInstant();
            default -> null;
        };
    }

    private Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value == null) {
            return false;
        }
        return Boolean.parseBoolean(value.toString());
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

    private String attendanceStateLabel(
            String locale,
            Object reviewStatus,
            Object firstCheckInAt,
            Object lastCheckOutAt,
            Object dayStatus
    ) {
        if (Objects.equals(string(reviewStatus), AttendanceReviewStatus.PENDING_REVIEW.name())) {
            return localization.attendanceStateLabel(locale, AttendanceState.PENDING_REVIEW);
        }
        if (firstCheckInAt == null) {
            return localization.attendanceStateLabel(locale, AttendanceState.NOT_CHECKED_IN);
        }
        if (lastCheckOutAt == null) {
            AttendanceState state = Objects.equals(string(dayStatus), AttendanceDayStatus.MISSING_CHECKOUT.name())
                    ? AttendanceState.MISSING_CHECKOUT
                    : AttendanceState.CHECKED_IN;
            return localization.attendanceStateLabel(locale, state);
        }
        return localization.attendanceStateLabel(locale, AttendanceState.CHECKED_OUT);
    }
}

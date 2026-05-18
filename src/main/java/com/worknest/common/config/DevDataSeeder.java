package com.worknest.common.config;

import com.worknest.common.i18n.Language;
import com.worknest.domain.entities.CompanySite;
import com.worknest.domain.entities.Department;
import com.worknest.domain.entities.Employee;
import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.RoleAssignment;
import com.worknest.domain.entities.User;
import com.worknest.domain.enums.EmploymentStatus;
import com.worknest.domain.enums.EmploymentType;
import com.worknest.domain.enums.PaymentMethod;
import com.worknest.domain.enums.PlatformAccess;
import com.worknest.domain.enums.PlatformRole;
import com.worknest.domain.enums.UserStatus;
import com.worknest.features.auth.repository.RoleAssignmentRepository;
import com.worknest.features.auth.repository.UserRepository;
import com.worknest.features.company.repository.CompanyRepository;
import com.worknest.features.companySite.repository.CompanySiteRepository;
import com.worknest.features.department.repository.DepartmentRepository;
import com.worknest.features.employee.repository.EmployeeRepository;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class DevDataSeeder implements ApplicationRunner {

    private static final String SEED_SCRIPT = "db/seed/seed-dev.sql";
    private static final String GUARD_SLUG = "worknest-tech";

    private final DevDataSeedProperties properties;
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final CompanyRepository companyRepository;
    private final DepartmentRepository departmentRepository;
    private final CompanySiteRepository companySiteRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }

        if (properties.isEraseAllBeforeSeed()) {
            eraseAllBusinessTables();
        }

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM companies WHERE slug = ?",
                Integer.class, GUARD_SLUG);

        boolean baseDataMissing = count == null || count == 0;
        if (baseDataMissing && properties.isRunBaseScript()) {
            runBaseScript();
            count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM companies WHERE slug = ?",
                    Integer.class, GUARD_SLUG);
            baseDataMissing = count == null || count == 0;
        }

        if (baseDataMissing) {
            log.warn("Dev seed skipped: base company '{}' not found and base script disabled.", GUARD_SLUG);
            return;
        }

        if (!properties.isForceReseed() && !properties.isEraseAllBeforeSeed() && hasBulkSeedMarker()) {
            log.info("Dev bulk seed skipped: marker detected. Enable force-reseed to run again.");
            return;
        }

        if (properties.isGenerateBulkData()) {
            seedBulkData();
        }
    }

    private void runBaseScript() {
        log.info("Running dev base seed from '{}'...", SEED_SCRIPT);
        try {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource(SEED_SCRIPT));
            populator.setSeparator(";");
            populator.setIgnoreFailedDrops(false);
            populator.execute(dataSource);
            log.info("Dev base seed completed successfully.");
        } catch (Exception ex) {
            log.error("Dev base seed FAILED. Cause: {}", ex.getMessage(), ex);
        }
    }

    private void eraseAllBusinessTables() {
        log.warn("Erasing all business tables before dev seed...");
        jdbcTemplate.execute("""
                DO $$
                DECLARE
                  r RECORD;
                BEGIN
                  FOR r IN (
                    SELECT tablename
                    FROM pg_tables
                    WHERE schemaname = 'public'
                      AND tablename NOT IN ('databasechangelog', 'databasechangeloglock')
                  ) LOOP
                    EXECUTE 'TRUNCATE TABLE public.' || quote_ident(r.tablename) || ' RESTART IDENTITY CASCADE';
                  END LOOP;
                END $$;
                """);
        log.warn("Business tables erased.");
    }

    private boolean hasBulkSeedMarker() {
        Integer marker = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM users
                WHERE email = 'seed.bulk.marker@worknest-tech.al'
                """, Integer.class);
        return marker != null && marker > 0;
    }

    private void seedBulkData() {
        UUID companyId = uuidOf("SELECT id FROM companies WHERE slug = ? LIMIT 1", GUARD_SLUG);
        UUID managerUserId = uuidOf("""
                SELECT ra.user_id
                FROM role_assignments ra
                WHERE ra.company_id = ? AND ra.role = 'ADMIN' AND ra.is_active = true
                ORDER BY ra.created_at ASC
                LIMIT 1
                """, companyId);
        UUID managerRoleAssignmentId = uuidOf("""
                SELECT ra.id
                FROM role_assignments ra
                WHERE ra.company_id = ? AND ra.role = 'ADMIN' AND ra.is_active = true
                ORDER BY ra.created_at ASC
                LIMIT 1
                """, companyId);
        UUID siteId = uuidOf("SELECT id FROM company_sites WHERE company_id = ? ORDER BY created_at ASC LIMIT 1", companyId);
        UUID departmentId = uuidOf("SELECT id FROM departments WHERE company_id = ? ORDER BY created_at ASC LIMIT 1", companyId);

        if (managerUserId == null || managerRoleAssignmentId == null || siteId == null || departmentId == null) {
            log.warn("Dev bulk seed skipped: missing manager/site/department prerequisites.");
            return;
        }

        List<EmployeeSeedRow> employees = createBulkEmployees(companyId, managerRoleAssignmentId, departmentId, siteId);
        seedAttendance(companyId, siteId, employees);
        seedLeaves(companyId, managerUserId, employees);
        seedPayroll(companyId, managerUserId, employees);
        createMarkerUser();

        log.info("Dev bulk seed completed. employees={}, attendanceDays={}, leaveRequestsPerEmployee={}, payrollMonths={}",
                employees.size(), properties.getAttendanceDays(), properties.getLeaveRequestsPerEmployee(), properties.getPayrollMonths());
    }

    private List<EmployeeSeedRow> createBulkEmployees(UUID companyId, UUID managerRoleAssignmentId, UUID departmentId, UUID siteId) {
        List<EmployeeSeedRow> rows = new ArrayList<>();
        Instant now = Instant.now();
        int target = Math.max(properties.getBulkEmployees(), 0);
        Company company = companyRepository.findById(companyId).orElse(null);
        if (company == null) {
            log.warn("Dev bulk seed skipped: company not found for id={}", companyId);
            return rows;
        }

        Department department = departmentRepository.findById(departmentId).orElse(null);
        CompanySite site = companySiteRepository.findById(siteId).orElse(null);
        if (department == null || site == null) {
            log.warn("Dev bulk seed skipped: missing department/site entities for company={}", companyId);
            return rows;
        }

        for (int i = 1; i <= target; i++) {
            String email = "bulk.employee." + i + "@worknest-tech.al";
            if (userRepository.existsByEmailIgnoreCase(email)) {
                continue;
            }

            String firstName = "Bulk" + i;
            String lastName = "Employee";
            String displayName = firstName + " " + lastName;
            LocalDate startDate = LocalDate.now().minusDays((i % 400) + 30L);
            User user = new User();
            user.setEmail(email);
            user.setPasswordHash("$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi");
            user.setStatus(UserStatus.ACTIVE);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setDisplayName(displayName);
            user.setPreferredLanguage(Language.EN);
            user.setFailedLoginCount((short) 0);
            user.setMfaEnabled(false);
            User savedUser = userRepository.save(user);
            UUID userId = savedUser.getId();

            RoleAssignment roleAssignment = new RoleAssignment();
            roleAssignment.setCompany(company);
            roleAssignment.setUser(savedUser);
            roleAssignment.setRole(PlatformRole.STAFF);
            roleAssignment.setJobTitle("Staff " + i);
            roleAssignment.setIsActive(true);
            roleAssignment.setPlatformAccess(PlatformAccess.MOBILE);
            roleAssignment.setActivatedAt(now);
            roleAssignmentRepository.save(roleAssignment);

            boolean hourly = i % 3 == 0;
            String paymentMethod = hourly ? "HOURLY" : "FIXED_MONTHLY";
            BigDecimal monthlySalary = hourly ? null : BigDecimal.valueOf(70000 + ((i % 10) * 6500L)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal hourlyRate = hourly ? BigDecimal.valueOf(500 + ((i % 9) * 55L)).setScale(2, RoundingMode.HALF_UP) : null;
            BigDecimal dailyHours = hourly ? BigDecimal.valueOf(6.0) : BigDecimal.valueOf(8.0);
            String employmentType = hourly ? "PART_TIME" : (i % 11 == 0 ? "CONTRACT" : "FULL_TIME");
            LocalDate contractExpiry = "CONTRACT".equals(employmentType) ? LocalDate.now().plusMonths(8 + (i % 6)) : null;

            Employee employee = new Employee();
            employee.setCompany(company);
            employee.setUser(savedUser);
            employee.setDepartment(department);
            employee.setCompanySite(site);
            employee.setEmploymentTypeRole(PlatformRole.STAFF);
            employee.setStartDate(startDate);
            employee.setSupervisorRoleAssignment(null);
            employee.setEmploymentStatus(EmploymentStatus.ACTIVE);
            employee.setContractExpiryDate(contractExpiry);
            employee.setLeaveDaysPerYear(hourly ? 10 : 20);
            employee.setDailyWorkingHours(dailyHours);
            employee.setPaymentMethod(PaymentMethod.valueOf(paymentMethod));
            employee.setMonthlySalary(monthlySalary);
            employee.setHourlyRate(hourlyRate);
            employee.setEmploymentType(EmploymentType.valueOf(employmentType));
            Employee savedEmployee = employeeRepository.save(employee);

            rows.add(new EmployeeSeedRow(savedEmployee.getId(), savedUser.getId(), paymentMethod, monthlySalary, hourlyRate, dailyHours));
        }

        List<EmployeeSeedRow> existingCore = jdbcTemplate.query("""
                SELECT e.id, e.user_id, e.payment_method, e.monthly_salary, e.hourly_rate, e.daily_working_hours
                FROM employees e
                JOIN users u ON u.id = e.user_id
                WHERE e.company_id = ?
                  AND u.email IN (
                    'blerina.hoxha@worknest-tech.al',
                    'agron.musa@worknest-tech.al',
                    'vjollca.rama@worknest-tech.al',
                    'driton.berisha@worknest-tech.al'
                  )
                """, (rs, rowNum) -> new EmployeeSeedRow(
                rs.getObject("id", UUID.class),
                rs.getObject("user_id", UUID.class),
                rs.getString("payment_method"),
                rs.getBigDecimal("monthly_salary"),
                rs.getBigDecimal("hourly_rate"),
                rs.getBigDecimal("daily_working_hours")
        ), companyId);
        rows.addAll(existingCore);
        return rows;
    }

    private void seedAttendance(UUID companyId, UUID siteId, List<EmployeeSeedRow> employees) {
        int days = Math.max(properties.getAttendanceDays(), 0);
        LocalDate today = LocalDate.now();
        ZoneId zone = ZoneId.of("Europe/Tirane");

        String daySql = """
                INSERT INTO attendance_day_records (
                    id, company_id, employee_id, user_id, site_id, work_date, timezone,
                    first_check_in_at, last_check_out_at, worked_minutes, break_minutes,
                    late_minutes, early_leave_minutes, overtime_minutes,
                    day_status, has_warnings, warning_flags_json, review_status,
                    payroll_locked, source_event_count, version, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, 'Europe/Tirane', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, 0, ?, ?)
                ON CONFLICT (company_id, employee_id, work_date) DO NOTHING
                """;

        String eventSql = """
                INSERT INTO attendance_events (
                    id, company_id, employee_id, user_id, site_id,
                    event_type, event_status, capture_method, attendance_decision,
                    server_recorded_at, client_captured_at, work_date, timezone,
                    client_request_id, device_public_id, platform, app_version,
                    qr_validation_status, latitude, longitude, accuracy_meters,
                    distance_from_site_meters, inside_geofence, geofence_decision,
                    request_ip_address, forwarded_for_chain, network_matched, network_decision,
                    risk_score, warning_flags_json, rejection_reason_code, rejection_reason_message,
                    employee_note, review_status, reviewed_by_user_id, reviewed_at, review_note, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Europe/Tirane', ?, ?, 'ANDROID', '1.8.0', ?,
                          ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        for (EmployeeSeedRow e : employees) {
            for (int i = 1; i <= days; i++) {
                LocalDate d = today.minusDays(i);
                if (d.getDayOfWeek() == DayOfWeek.SUNDAY) {
                    continue;
                }

                int pattern = Math.floorMod(i + Math.abs(e.employeeId().hashCode()), 10);
                String dayStatus = switch (pattern) {
                    case 1 -> "LATE";
                    case 2 -> "MISSING_CHECKOUT";
                    case 3 -> "HALF_DAY";
                    case 4 -> "ON_LEAVE";
                    case 5 -> "ABSENT";
                    case 6 -> "FLAGGED";
                    case 7 -> "PENDING_REVIEW";
                    case 8 -> "HOLIDAY";
                    default -> "PRESENT";
                };

                boolean acceptedDay = "PRESENT".equals(dayStatus) || "LATE".equals(dayStatus) || "HALF_DAY".equals(dayStatus) || "FLAGGED".equals(dayStatus);
                LocalDateTime inDt = d.atTime("LATE".equals(dayStatus) ? 9 : 8, (pattern % 3) * 10);
                LocalDateTime outDt = inDt.plusHours("HALF_DAY".equals(dayStatus) ? 4 : 8).plusMinutes(pattern % 4 == 0 ? 45 : 15);
                Instant inAt = inDt.atZone(zone).toInstant();
                Instant outAt = outDt.atZone(zone).toInstant();

                int worked = switch (dayStatus) {
                    case "HALF_DAY" -> 240;
                    case "MISSING_CHECKOUT" -> 290;
                    case "ABSENT", "ON_LEAVE", "HOLIDAY" -> 0;
                    default -> 480 + (pattern == 0 ? 60 : 0);
                };

                int overtime = Math.max(worked - 480, 0);
                int lateMin = "LATE".equals(dayStatus) ? 35 : 0;
                boolean warnings = "FLAGGED".equals(dayStatus) || pattern == 7;
                String reviewStatus = "PENDING_REVIEW".equals(dayStatus) ? "PENDING_REVIEW" : (warnings ? "CORRECTED" : "NONE");

                jdbcTemplate.update(daySql,
                        UUID.randomUUID(), companyId, e.employeeId(), e.userId(), siteId, Date.valueOf(d),
                        acceptedDay ? Timestamp.from(inAt) : null,
                        ("MISSING_CHECKOUT".equals(dayStatus) || !acceptedDay) ? null : Timestamp.from(outAt),
                        worked, 45, lateMin, "HALF_DAY".equals(dayStatus) ? 50 : 0, overtime,
                        dayStatus, warnings,
                        warnings ? "[\"GEOFENCE_WARNING\",\"NETWORK_WARNING\"]" : "[]",
                        reviewStatus,
                        i < 45,
                        "MISSING_CHECKOUT".equals(dayStatus) ? 1 : (acceptedDay ? 2 : 0),
                        Timestamp.from(inAt),
                        Timestamp.from(acceptedDay ? outAt : inAt.plusSeconds(900))
                );

                if (!acceptedDay && !"MISSING_CHECKOUT".equals(dayStatus)) {
                    continue;
                }

                String statusIn = warnings ? "ACCEPTED_WITH_WARNINGS" : "ACCEPTED";
                String decisionIn = warnings ? "ACCEPTED_WITH_WARNINGS" : "ACCEPTED";
                String geofenceIn = warnings ? "WARNING_BOUNDARY_ZONE" : "PASSED";
                String networkIn = warnings ? "WARNING" : "PASSED";

                jdbcTemplate.update(eventSql,
                        UUID.randomUUID(), companyId, e.employeeId(), e.userId(), siteId,
                        "CHECK_IN", statusIn, "QR_GEOFENCE", decisionIn,
                        Timestamp.from(inAt), Timestamp.from(inAt.minusSeconds(20)), Date.valueOf(d),
                        "seed-ci-" + e.employeeId() + "-" + d,
                        "dev-device-" + Math.floorMod(e.employeeId().hashCode(), 1000),
                        "VALID", BigDecimal.valueOf(41.327953), BigDecimal.valueOf(19.819025), BigDecimal.valueOf(warnings ? 38.5 : 9.5),
                        BigDecimal.valueOf(warnings ? 95 : 12), true, geofenceIn,
                        "192.168.10.55", "10.0.0.15, 192.168.10.55", true, networkIn,
                        warnings ? 70 : 8,
                        warnings ? "[\"GEOFENCE_WARNING\",\"NETWORK_WARNING\"]" : "[]",
                        null, null,
                        warnings ? "Boundary area entry" : null,
                        warnings ? "PENDING_REVIEW" : "NONE",
                        null, null, null,
                        Timestamp.from(inAt)
                );

                if ("MISSING_CHECKOUT".equals(dayStatus)) {
                    jdbcTemplate.update(eventSql,
                            UUID.randomUUID(), companyId, e.employeeId(), e.userId(), siteId,
                            "AUTO_CHECK_OUT", "PENDING_REVIEW", "SYSTEM", "PENDING_REVIEW",
                            Timestamp.from(inAt.plusSeconds(9 * 3600L)), Timestamp.from(inAt.plusSeconds(9 * 3600L)), Date.valueOf(d),
                            "seed-aco-" + e.employeeId() + "-" + d,
                            "system-device",
                            "NOT_REQUIRED", null, null, null,
                            null, null, "NOT_REQUIRED",
                            null, null, null, "NOT_CONFIGURED",
                            20,
                            "[]",
                            null, null,
                            "Auto-checkout due to missing checkout",
                            "PENDING_REVIEW",
                            null, null, null,
                            Timestamp.from(inAt.plusSeconds(9 * 3600L))
                    );
                } else {
                    jdbcTemplate.update(eventSql,
                            UUID.randomUUID(), companyId, e.employeeId(), e.userId(), siteId,
                            "CHECK_OUT", statusIn, "QR_GEOFENCE", decisionIn,
                            Timestamp.from(outAt), Timestamp.from(outAt.minusSeconds(15)), Date.valueOf(d),
                            "seed-co-" + e.employeeId() + "-" + d,
                            "dev-device-" + Math.floorMod(e.employeeId().hashCode(), 1000),
                            "VALID", BigDecimal.valueOf(41.327955), BigDecimal.valueOf(19.819021), BigDecimal.valueOf(warnings ? 35.5 : 8.2),
                            BigDecimal.valueOf(warnings ? 87 : 10), true, geofenceIn,
                            "192.168.10.55", "10.0.0.15, 192.168.10.55", true, networkIn,
                            warnings ? 68 : 7,
                            warnings ? "[\"GEOFENCE_WARNING\"]" : "[]",
                            null, null,
                            null,
                            warnings ? "PENDING_REVIEW" : "NONE",
                            null, null, null,
                            Timestamp.from(outAt)
                    );
                }
            }
        }
    }

    private void seedLeaves(UUID companyId, UUID reviewerUserId, List<EmployeeSeedRow> employees) {
        int perEmployee = Math.max(properties.getLeaveRequestsPerEmployee(), 0);
        int year = LocalDate.now().getYear();
        Instant now = Instant.now();
        Timestamp nowTs = Timestamp.from(now);

        String balanceSql = """
                INSERT INTO leave_balances (id, company_id, employee_id, year, leave_type, total_days, used_days, version, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?, ?)
                ON CONFLICT (company_id, employee_id, year, leave_type) DO NOTHING
                """;

        String leaveSql = """
                INSERT INTO leave_requests (
                    id, company_id, employee_id, leave_type, start_date, end_date, days_count, status,
                    note, approval_note, medical_report_document_id, rejection_reason,
                    reviewed_by_user_id, reviewed_at, version, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?)
                """;

        for (EmployeeSeedRow e : employees) {
            jdbcTemplate.update(balanceSql, UUID.randomUUID(), companyId, e.employeeId(), year, "VACATION", BigDecimal.valueOf(20.0), BigDecimal.valueOf(6.5), nowTs, nowTs);
            jdbcTemplate.update(balanceSql, UUID.randomUUID(), companyId, e.employeeId(), year, "SICK", BigDecimal.valueOf(12.0), BigDecimal.valueOf(2.0), nowTs, nowTs);
            jdbcTemplate.update(balanceSql, UUID.randomUUID(), companyId, e.employeeId(), year, "PERSONAL", BigDecimal.valueOf(5.0), BigDecimal.valueOf(1.0), nowTs, nowTs);

            for (int i = 0; i < perEmployee; i++) {
                LocalDate start = LocalDate.now().minusDays((long) (i + 1) * 9 + Math.floorMod(e.employeeId().hashCode(), 5));
                int span = (i % 4 == 0) ? 1 : (i % 5 == 0 ? 0 : 2);
                LocalDate end = span == 0 ? start : start.plusDays(span);
                String type = switch (i % 6) {
                    case 0 -> "VACATION";
                    case 1 -> "SICK";
                    case 2 -> "PERSONAL";
                    case 3 -> "UNPAID";
                    case 4 -> "MATERNITY";
                    default -> "OTHER";
                };
                String status = switch (i % 4) {
                    case 0 -> "APPROVED";
                    case 1 -> "PENDING";
                    case 2 -> "REJECTED";
                    default -> "CANCELLED";
                };
                BigDecimal daysCount = span == 0 ? BigDecimal.valueOf(0.5) : BigDecimal.valueOf(span + 1.0);

                jdbcTemplate.update(leaveSql,
                        UUID.randomUUID(), companyId, e.employeeId(), type, Date.valueOf(start), Date.valueOf(end), daysCount, status,
                        "Seeded leave request for edge-case coverage",
                        "APPROVED".equals(status) ? "Approved by manager" : null,
                        "SICK".equals(type) ? "medical-doc/seed/" + UUID.randomUUID() : null,
                        "REJECTED".equals(status) ? "Insufficient team coverage" : null,
                        ("PENDING".equals(status) ? null : reviewerUserId),
                        ("PENDING".equals(status) ? null : Timestamp.from(now.minusSeconds(i * 800L))),
                        nowTs, nowTs
                );
            }
        }
    }

    private void seedPayroll(UUID companyId, UUID calculatedBy, List<EmployeeSeedRow> employees) {
        int months = Math.max(properties.getPayrollMonths(), 0);
        LocalDate monthCursor = LocalDate.now().withDayOfMonth(1).minusMonths(1);
        Instant now = Instant.now();
        Timestamp nowTs = Timestamp.from(now);

        String payrollSql = """
                INSERT INTO payroll_results (
                    id, company_id, employee_id, payroll_year, payroll_month, status,
                    base_pay, gross_earnings, total_deductions, net_pay,
                    income_tax, employee_social_security, employee_pension,
                    employer_social_security, employer_pension,
                    taxable_income, employer_cost_total,
                    calculation_snapshot_json, calculated_at, calculated_by_user_id,
                    version, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::text, ?, ?, 0, ?, ?)
                ON CONFLICT (company_id, employee_id, payroll_year, payroll_month) DO NOTHING
                """;

        String adjustmentSql = """
                INSERT INTO payroll_adjustments (
                    id, company_id, employee_id, payroll_year, payroll_month, adjustment_type,
                    amount, reason, notes, created_by_user_id, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        for (EmployeeSeedRow e : employees) {
            for (int i = 0; i < months; i++) {
                LocalDate month = monthCursor.minusMonths(i);
                int year = month.getYear();
                int m = month.getMonthValue();

                BigDecimal base = "HOURLY".equals(e.paymentMethod())
                        ? e.hourlyRate().multiply(BigDecimal.valueOf(140 + (i % 12))).setScale(2, RoundingMode.HALF_UP)
                        : e.monthlySalary().setScale(2, RoundingMode.HALF_UP);
                BigDecimal gross = base.add(BigDecimal.valueOf((i % 3 == 0) ? 5000 : 1200));
                BigDecimal deductions = gross.multiply(BigDecimal.valueOf(0.18)).setScale(2, RoundingMode.HALF_UP);
                BigDecimal tax = gross.multiply(BigDecimal.valueOf(0.12)).setScale(2, RoundingMode.HALF_UP);
                BigDecimal net = gross.subtract(deductions).subtract(tax).setScale(2, RoundingMode.HALF_UP);
                BigDecimal empSocial = gross.multiply(BigDecimal.valueOf(0.095)).setScale(2, RoundingMode.HALF_UP);
                BigDecimal empPension = gross.multiply(BigDecimal.valueOf(0.015)).setScale(2, RoundingMode.HALF_UP);
                BigDecimal employerSocial = gross.multiply(BigDecimal.valueOf(0.112)).setScale(2, RoundingMode.HALF_UP);
                BigDecimal employerPension = gross.multiply(BigDecimal.valueOf(0.017)).setScale(2, RoundingMode.HALF_UP);
                BigDecimal taxable = gross.subtract(empSocial).subtract(empPension).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
                BigDecimal employerCost = gross.add(employerSocial).add(employerPension).setScale(2, RoundingMode.HALF_UP);

                String status = switch (i % 6) {
                    case 0 -> "PAID";
                    case 1 -> "FINALIZED";
                    case 2 -> "APPROVED";
                    case 3 -> "CALCULATED";
                    case 4 -> "DRAFT";
                    default -> "CANCELLED";
                };

                String snapshot = "{\"seed\":true,\"status\":\"" + status + "\",\"edgeCase\":\"bulk\"}";
                Timestamp calculatedAt = Timestamp.from(now.minusSeconds((long) i * 86400L));

                jdbcTemplate.update(payrollSql,
                        UUID.randomUUID(), companyId, e.employeeId(), year, m, status,
                        base, gross, deductions, net,
                        tax, empSocial, empPension,
                        employerSocial, employerPension,
                        taxable, employerCost,
                        snapshot, calculatedAt, calculatedBy,
                        nowTs, nowTs
                );

                jdbcTemplate.update(adjustmentSql,
                        UUID.randomUUID(), companyId, e.employeeId(), year, m,
                        (i % 2 == 0) ? "BONUS" : "DEDUCTION",
                        BigDecimal.valueOf((i % 2 == 0) ? 1800 : 700).setScale(2, RoundingMode.HALF_UP),
                        (i % 2 == 0) ? "Performance bonus" : "Payroll correction",
                        "Generated by bulk dev seed",
                        calculatedBy, nowTs, nowTs
                );
            }
        }
    }

    private void createMarkerUser() {
        if (userRepository.existsByEmailIgnoreCase("seed.bulk.marker@worknest-tech.al")) {
            return;
        }
        User marker = new User();
        marker.setEmail("seed.bulk.marker@worknest-tech.al");
        marker.setPasswordHash("$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi");
        marker.setStatus(UserStatus.ACTIVE);
        marker.setFirstName("Seed");
        marker.setLastName("Marker");
        marker.setDisplayName("Seed Marker");
        marker.setPreferredLanguage(Language.EN);
        marker.setFailedLoginCount((short) 0);
        marker.setMfaEnabled(false);
        userRepository.save(marker);
    }

    private UUID uuidOf(String sql, Object... args) {
        List<UUID> result = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getObject(1, UUID.class), args);
        return result.isEmpty() ? null : result.get(0);
    }

    private record EmployeeSeedRow(UUID employeeId,
                                   UUID userId,
                                   String paymentMethod,
                                   BigDecimal monthlySalary,
                                   BigDecimal hourlyRate,
                                   BigDecimal dailyHours) {
    }
}

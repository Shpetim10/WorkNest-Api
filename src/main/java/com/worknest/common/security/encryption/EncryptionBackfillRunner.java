package com.worknest.common.security.encryption;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.encryption.backfill", name = "enabled", havingValue = "true")
public class EncryptionBackfillRunner implements ApplicationRunner {

    private static final String PREFIX = "ENCv1:";

    private final JdbcTemplate jdbcTemplate;
    private final EncryptionService encryptionService;

    @Override
    public void run(ApplicationArguments args) {
        log.warn("Starting one-time encryption backfill run. This should be enabled only for controlled migrations.");
        backfillUsers();
        backfillEmployees();
        backfillCompanies();
        backfillPayrollResults();
        backfillPayrollAdjustments();
        backfillLeaveRequests();
        backfillAttendanceEvents();
        backfillAttendanceReviewActions();
        backfillRefreshTokens();
        backfillCompanySites();
        backfillSiteTrustedNetworks();
        backfillEmployeeSupervisorHistory();
        backfillRoleAssignments();
        backfillUserInvitations();
        backfillAuditLogs();
        log.warn("Encryption backfill run completed.");
    }

    private void backfillUsers() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, first_name, last_name, phone_number, last_login_ip, mfa_secret_enc, gdpr_consent_ip
                FROM users
                WHERE (first_name IS NOT NULL AND first_name NOT LIKE ?)
                   OR (last_name IS NOT NULL AND last_name NOT LIKE ?)
                   OR (phone_number IS NOT NULL AND phone_number NOT LIKE ?)
                   OR (last_login_ip IS NOT NULL AND last_login_ip NOT LIKE ?)
                   OR (mfa_secret_enc IS NOT NULL AND mfa_secret_enc NOT LIKE ?)
                   OR (gdpr_consent_ip IS NOT NULL AND gdpr_consent_ip NOT LIKE ?)
                """, likePrefix(), likePrefix(), likePrefix(), likePrefix(), likePrefix(), likePrefix());

        int updated = 0;
        for (Map<String, Object> row : rows) {
            jdbcTemplate.update("""
                    UPDATE users
                    SET first_name = ?, last_name = ?, phone_number = ?, last_login_ip = ?, mfa_secret_enc = ?, gdpr_consent_ip = ?
                    WHERE id = ?
                    """,
                    encryptValue(row.get("first_name")),
                    encryptValue(row.get("last_name")),
                    encryptValue(row.get("phone_number")),
                    encryptValue(row.get("last_login_ip")),
                    encryptValue(row.get("mfa_secret_enc")),
                    encryptValue(row.get("gdpr_consent_ip")),
                    uuid(row.get("id")));
            updated++;
        }
        log.info("Encryption backfill: users updated={}", updated);
    }

    private void backfillEmployees() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, monthly_salary, hourly_rate
                FROM employees
                WHERE (monthly_salary IS NOT NULL AND monthly_salary NOT LIKE ?)
                   OR (hourly_rate IS NOT NULL AND hourly_rate NOT LIKE ?)
                """, likePrefix(), likePrefix());

        int updated = 0;
        for (Map<String, Object> row : rows) {
            jdbcTemplate.update("UPDATE employees SET monthly_salary = ?, hourly_rate = ? WHERE id = ?",
                    encryptValue(row.get("monthly_salary")),
                    encryptValue(row.get("hourly_rate")),
                    uuid(row.get("id")));
            updated++;
        }
        log.info("Encryption backfill: employees updated={}", updated);
    }

    private void backfillCompanies() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, nipt, nipt_hash, phone_number, suspended_reason
                FROM companies
                WHERE (nipt IS NOT NULL AND nipt NOT LIKE ?)
                   OR (nipt IS NOT NULL AND nipt_hash IS NULL)
                   OR (phone_number IS NOT NULL AND phone_number NOT LIKE ?)
                   OR (suspended_reason IS NOT NULL AND suspended_reason NOT LIKE ?)
                """, likePrefix(), likePrefix(), likePrefix());

        int updated = 0;
        for (Map<String, Object> row : rows) {
            String nipt = stringValue(row.get("nipt"));
            String normalizedNipt = encryptionService.normalizeNipt(encryptionService.decrypt(nipt));
            jdbcTemplate.update("""
                    UPDATE companies
                    SET nipt = ?, nipt_hash = ?, phone_number = ?, suspended_reason = ?
                    WHERE id = ?
                    """,
                    encryptValue(normalizedNipt),
                    encryptionService.hmacSha256Hex(normalizedNipt),
                    encryptValue(row.get("phone_number")),
                    encryptValue(row.get("suspended_reason")),
                    uuid(row.get("id")));
            updated++;
        }
        log.info("Encryption backfill: companies updated={}", updated);
    }

    private void backfillPayrollResults() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, base_pay, gross_earnings, total_deductions, net_pay, income_tax,
                       employee_social_security, employee_pension, employer_social_security,
                       employer_pension, taxable_income, employer_cost_total, calculation_snapshot_json
                FROM payroll_results
                WHERE (base_pay IS NOT NULL AND base_pay NOT LIKE ?)
                   OR (gross_earnings IS NOT NULL AND gross_earnings NOT LIKE ?)
                   OR (total_deductions IS NOT NULL AND total_deductions NOT LIKE ?)
                   OR (net_pay IS NOT NULL AND net_pay NOT LIKE ?)
                   OR (income_tax IS NOT NULL AND income_tax NOT LIKE ?)
                   OR (employee_social_security IS NOT NULL AND employee_social_security NOT LIKE ?)
                   OR (employee_pension IS NOT NULL AND employee_pension NOT LIKE ?)
                   OR (employer_social_security IS NOT NULL AND employer_social_security NOT LIKE ?)
                   OR (employer_pension IS NOT NULL AND employer_pension NOT LIKE ?)
                   OR (taxable_income IS NOT NULL AND taxable_income NOT LIKE ?)
                   OR (employer_cost_total IS NOT NULL AND employer_cost_total NOT LIKE ?)
                   OR (calculation_snapshot_json IS NOT NULL AND calculation_snapshot_json NOT LIKE ?)
                """,
                likePrefix(), likePrefix(), likePrefix(), likePrefix(), likePrefix(), likePrefix(),
                likePrefix(), likePrefix(), likePrefix(), likePrefix(), likePrefix(), likePrefix());

        int updated = 0;
        for (Map<String, Object> row : rows) {
            jdbcTemplate.update("""
                    UPDATE payroll_results
                    SET base_pay = ?, gross_earnings = ?, total_deductions = ?, net_pay = ?, income_tax = ?,
                        employee_social_security = ?, employee_pension = ?, employer_social_security = ?,
                        employer_pension = ?, taxable_income = ?, employer_cost_total = ?, calculation_snapshot_json = ?
                    WHERE id = ?
                    """,
                    encryptValue(row.get("base_pay")),
                    encryptValue(row.get("gross_earnings")),
                    encryptValue(row.get("total_deductions")),
                    encryptValue(row.get("net_pay")),
                    encryptValue(row.get("income_tax")),
                    encryptValue(row.get("employee_social_security")),
                    encryptValue(row.get("employee_pension")),
                    encryptValue(row.get("employer_social_security")),
                    encryptValue(row.get("employer_pension")),
                    encryptValue(row.get("taxable_income")),
                    encryptValue(row.get("employer_cost_total")),
                    encryptValue(row.get("calculation_snapshot_json")),
                    uuid(row.get("id")));
            updated++;
        }
        log.info("Encryption backfill: payroll_results updated={}", updated);
    }

    private void backfillPayrollAdjustments() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, amount, reason, notes
                FROM payroll_adjustments
                WHERE (amount IS NOT NULL AND amount NOT LIKE ?)
                   OR (reason IS NOT NULL AND reason NOT LIKE ?)
                   OR (notes IS NOT NULL AND notes NOT LIKE ?)
                """, likePrefix(), likePrefix(), likePrefix());

        int updated = 0;
        for (Map<String, Object> row : rows) {
            jdbcTemplate.update("UPDATE payroll_adjustments SET amount = ?, reason = ?, notes = ? WHERE id = ?",
                    encryptValue(row.get("amount")),
                    encryptValue(row.get("reason")),
                    encryptValue(row.get("notes")),
                    uuid(row.get("id")));
            updated++;
        }
        log.info("Encryption backfill: payroll_adjustments updated={}", updated);
    }

    private void backfillLeaveRequests() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, note, approval_note, medical_report_document_id, rejection_reason
                FROM leave_requests
                WHERE (note IS NOT NULL AND note NOT LIKE ?)
                   OR (approval_note IS NOT NULL AND approval_note NOT LIKE ?)
                   OR (medical_report_document_id IS NOT NULL AND medical_report_document_id NOT LIKE ?)
                   OR (rejection_reason IS NOT NULL AND rejection_reason NOT LIKE ?)
                """, likePrefix(), likePrefix(), likePrefix(), likePrefix());

        int updated = 0;
        for (Map<String, Object> row : rows) {
            jdbcTemplate.update("""
                    UPDATE leave_requests
                    SET note = ?, approval_note = ?, medical_report_document_id = ?, rejection_reason = ?
                    WHERE id = ?
                    """,
                    encryptValue(row.get("note")),
                    encryptValue(row.get("approval_note")),
                    encryptValue(row.get("medical_report_document_id")),
                    encryptValue(row.get("rejection_reason")),
                    uuid(row.get("id")));
            updated++;
        }
        log.info("Encryption backfill: leave_requests updated={}", updated);
    }

    private void backfillAttendanceEvents() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, request_ip_address, forwarded_for_chain, rejection_reason_message, employee_note, review_note
                FROM attendance_events
                WHERE (request_ip_address IS NOT NULL AND request_ip_address NOT LIKE ?)
                   OR (forwarded_for_chain IS NOT NULL AND forwarded_for_chain NOT LIKE ?)
                   OR (rejection_reason_message IS NOT NULL AND rejection_reason_message NOT LIKE ?)
                   OR (employee_note IS NOT NULL AND employee_note NOT LIKE ?)
                   OR (review_note IS NOT NULL AND review_note NOT LIKE ?)
                """, likePrefix(), likePrefix(), likePrefix(), likePrefix(), likePrefix());

        int updated = 0;
        for (Map<String, Object> row : rows) {
            jdbcTemplate.update("""
                    UPDATE attendance_events
                    SET request_ip_address = ?, forwarded_for_chain = ?, rejection_reason_message = ?, employee_note = ?, review_note = ?
                    WHERE id = ?
                    """,
                    encryptValue(row.get("request_ip_address")),
                    encryptValue(row.get("forwarded_for_chain")),
                    encryptValue(row.get("rejection_reason_message")),
                    encryptValue(row.get("employee_note")),
                    encryptValue(row.get("review_note")),
                    uuid(row.get("id")));
            updated++;
        }
        log.info("Encryption backfill: attendance_events updated={}", updated);
    }

    private void backfillAttendanceReviewActions() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, before_snapshot_json, after_snapshot_json, reason
                FROM attendance_review_actions
                WHERE (before_snapshot_json IS NOT NULL AND before_snapshot_json NOT LIKE ?)
                   OR (after_snapshot_json IS NOT NULL AND after_snapshot_json NOT LIKE ?)
                   OR (reason IS NOT NULL AND reason NOT LIKE ?)
                """, likePrefix(), likePrefix(), likePrefix());

        int updated = 0;
        for (Map<String, Object> row : rows) {
            jdbcTemplate.update("""
                    UPDATE attendance_review_actions
                    SET before_snapshot_json = ?, after_snapshot_json = ?, reason = ?
                    WHERE id = ?
                    """,
                    encryptValue(row.get("before_snapshot_json")),
                    encryptValue(row.get("after_snapshot_json")),
                    encryptValue(row.get("reason")),
                    uuid(row.get("id")));
            updated++;
        }
        log.info("Encryption backfill: attendance_review_actions updated={}", updated);
    }

    private void backfillRefreshTokens() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, revoked_reason, ip_address, user_agent
                FROM refresh_tokens
                WHERE (revoked_reason IS NOT NULL AND revoked_reason NOT LIKE ?)
                   OR (ip_address IS NOT NULL AND ip_address NOT LIKE ?)
                   OR (user_agent IS NOT NULL AND user_agent NOT LIKE ?)
                """, likePrefix(), likePrefix(), likePrefix());

        int updated = 0;
        for (Map<String, Object> row : rows) {
            jdbcTemplate.update("UPDATE refresh_tokens SET revoked_reason = ?, ip_address = ?, user_agent = ? WHERE id = ?",
                    encryptValue(row.get("revoked_reason")),
                    encryptValue(row.get("ip_address")),
                    encryptValue(row.get("user_agent")),
                    uuid(row.get("id")));
            updated++;
        }
        log.info("Encryption backfill: refresh_tokens updated={}", updated);
    }

    private void backfillCompanySites() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, address_line_1, address_line_2, city, state_region, postal_code, geofence_polygon_geojson, notes
                FROM company_sites
                WHERE (address_line_1 IS NOT NULL AND address_line_1 NOT LIKE ?)
                   OR (address_line_2 IS NOT NULL AND address_line_2 NOT LIKE ?)
                   OR (city IS NOT NULL AND city NOT LIKE ?)
                   OR (state_region IS NOT NULL AND state_region NOT LIKE ?)
                   OR (postal_code IS NOT NULL AND postal_code NOT LIKE ?)
                   OR (geofence_polygon_geojson IS NOT NULL AND geofence_polygon_geojson NOT LIKE ?)
                   OR (notes IS NOT NULL AND notes NOT LIKE ?)
                """, likePrefix(), likePrefix(), likePrefix(), likePrefix(), likePrefix(), likePrefix(), likePrefix());

        int updated = 0;
        for (Map<String, Object> row : rows) {
            jdbcTemplate.update("""
                    UPDATE company_sites
                    SET address_line_1 = ?, address_line_2 = ?, city = ?, state_region = ?, postal_code = ?,
                        geofence_polygon_geojson = ?, notes = ?
                    WHERE id = ?
                    """,
                    encryptValue(row.get("address_line_1")),
                    encryptValue(row.get("address_line_2")),
                    encryptValue(row.get("city")),
                    encryptValue(row.get("state_region")),
                    encryptValue(row.get("postal_code")),
                    encryptValue(row.get("geofence_polygon_geojson")),
                    encryptValue(row.get("notes")),
                    uuid(row.get("id")));
            updated++;
        }
        log.info("Encryption backfill: company_sites updated={}", updated);
    }

    private void backfillSiteTrustedNetworks() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, notes
                FROM site_trusted_networks
                WHERE notes IS NOT NULL AND notes NOT LIKE ?
                """, likePrefix());

        int updated = 0;
        for (Map<String, Object> row : rows) {
            jdbcTemplate.update("UPDATE site_trusted_networks SET notes = ? WHERE id = ?",
                    encryptValue(row.get("notes")),
                    uuid(row.get("id")));
            updated++;
        }
        log.info("Encryption backfill: site_trusted_networks updated={}", updated);
    }

    private void backfillEmployeeSupervisorHistory() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, reason
                FROM employee_supervisor_history
                WHERE reason IS NOT NULL AND reason NOT LIKE ?
                """, likePrefix());

        int updated = 0;
        for (Map<String, Object> row : rows) {
            jdbcTemplate.update("UPDATE employee_supervisor_history SET reason = ? WHERE id = ?",
                    encryptValue(row.get("reason")),
                    uuid(row.get("id")));
            updated++;
        }
        log.info("Encryption backfill: employee_supervisor_history updated={}", updated);
    }

    private void backfillRoleAssignments() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, job_title
                FROM role_assignments
                WHERE job_title IS NOT NULL AND job_title NOT LIKE ?
                """, likePrefix());

        int updated = 0;
        for (Map<String, Object> row : rows) {
            jdbcTemplate.update("UPDATE role_assignments SET job_title = ? WHERE id = ?",
                    encryptValue(row.get("job_title")),
                    uuid(row.get("id")));
            updated++;
        }
        log.info("Encryption backfill: role_assignments updated={}", updated);
    }

    private void backfillUserInvitations() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, invited_job_title
                FROM user_invitations
                WHERE invited_job_title IS NOT NULL AND invited_job_title NOT LIKE ?
                """, likePrefix());

        int updated = 0;
        for (Map<String, Object> row : rows) {
            jdbcTemplate.update("UPDATE user_invitations SET invited_job_title = ? WHERE id = ?",
                    encryptValue(row.get("invited_job_title")),
                    uuid(row.get("id")));
            updated++;
        }
        log.info("Encryption backfill: user_invitations updated={}", updated);
    }

    private void backfillAuditLogs() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, actor_job_title, ip_address
                FROM audit_logs
                WHERE (actor_job_title IS NOT NULL AND actor_job_title NOT LIKE ?)
                   OR (ip_address IS NOT NULL AND ip_address NOT LIKE ?)
                """, likePrefix(), likePrefix());

        int updated = 0;
        for (Map<String, Object> row : rows) {
            jdbcTemplate.update("UPDATE audit_logs SET actor_job_title = ?, ip_address = ? WHERE id = ?",
                    encryptValue(row.get("actor_job_title")),
                    encryptValue(row.get("ip_address")),
                    row.get("id"));
            updated++;
        }
        log.info("Encryption backfill: audit_logs updated={}", updated);
    }

    private String encryptValue(Object rawValue) {
        String value = stringValue(rawValue);
        if (value == null) {
            return value;
        }
        return value.startsWith(PREFIX) ? value : encryptionService.encrypt(value);
    }

    private String stringValue(Object rawValue) {
        return rawValue == null ? null : String.valueOf(rawValue);
    }

    private UUID uuid(Object rawValue) {
        if (rawValue instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(String.valueOf(rawValue));
    }

    private String likePrefix() {
        return PREFIX + "%";
    }
}

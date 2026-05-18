-- =============================================================================
-- WorkNest API – Comprehensive Development Seed
-- Run manually against a local/dev PostgreSQL instance:
--   psql -U root -d worknest_db -f src/main/resources/db/seed/seed-dev.sql
--
-- Password for all seeded users: "password"  (BCrypt hash below)
-- =============================================================================
-- Edge cases covered:
--   Attendance : PRESENT, LATE, MISSING_CHECKOUT, ON_LEAVE, HOLIDAY, ABSENT,
--                overtime, geofence warning, network warning, rejected event,
--                manager manual entry, payroll_locked, review flow
--   Payroll    : PAID, FINALIZED, APPROVED, CALCULATED, DRAFT states;
--                FIXED_MONTHLY + HOURLY; bonus + deduction; tax brackets;
--                progressive tax on GROSS_MINUS_CONTRIBUTIONS
--   Leave      : APPROVED vacation, PENDING sick, REJECTED, CANCELLED,
--                half-day, MATERNITY; balance tracking; sick-leave policy
--   Tenancy    : two independent companies; no cross-company FK leakage
-- =============================================================================

BEGIN;

-- ============================================================
-- 1. COMPANIES
-- ============================================================
INSERT INTO companies (
    id, name, slug, status, nipt, industry, email, phone_number,
    country_code, timezone, locale, currency, date_format,
    subscription_plan, subscription_status, data_retention_days,
    version, created_at, updated_at
) VALUES
(
    'c1000000-0000-0000-0000-000000000001',
    'WorkNest Tech SH.P.K.', 'worknest-tech', 'ACTIVE',
    'L91234567A', 'Technology', 'admin@worknest-tech.al', '+355691234567',
    'AL', 'Europe/Tirane', 'SQ', 'ALL', 'DD/MM/YYYY',
    'PREMIUM', 'ACTIVE', 365,
    0, NOW(), NOW()
),
(
    'c2000000-0000-0000-0000-000000000002',
    'Acme Corp SH.P.K.', 'acme-corp', 'ACTIVE',
    'M99876543B', 'Retail', 'admin@acmecorp.al', '+355692345678',
    'AL', 'Europe/Tirane', 'EN', 'ALL', 'MM/DD/YYYY',
    'BASIC', 'ACTIVE', 90,
    0, NOW(), NOW()
);

-- ============================================================
-- 2. USERS  (BCrypt of "password")
-- ============================================================
INSERT INTO users (
    id, email, password_hash, status,
    first_name, last_name, display_name,
    preferred_language, failed_login_count, mfa_enabled,
    created_at, updated_at
) VALUES
-- Company 1 – Owner
('a1100001-0000-0000-0000-000000000001', 'arta.kelmendi@worknest-tech.al',
 '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
 'ACTIVE', 'Arta', 'Kelmendi', 'Arta Kelmendi', 'SQ', 0, false, NOW(), NOW()),
-- Company 1 – Manager
('a1100002-0000-0000-0000-000000000001', 'erjon.duka@worknest-tech.al',
 '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
 'ACTIVE', 'Erjon', 'Duka', 'Erjon Duka', 'SQ', 0, false, NOW(), NOW()),
-- Company 1 – Staff: senior engineer (high salary)
('a1100003-0000-0000-0000-000000000001', 'blerina.hoxha@worknest-tech.al',
 '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
 'ACTIVE', 'Blerina', 'Hoxha', 'Blerina Hoxha', 'SQ', 0, false, NOW(), NOW()),
-- Company 1 – Staff: HR specialist (mid salary)
('a1100004-0000-0000-0000-000000000001', 'agron.musa@worknest-tech.al',
 '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
 'ACTIVE', 'Agron', 'Musa', 'Agron Musa', 'SQ', 0, false, NOW(), NOW()),
-- Company 1 – Staff: sales rep (part-time hourly)
('a1100005-0000-0000-0000-000000000001', 'vjollca.rama@worknest-tech.al',
 '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
 'ACTIVE', 'Vjollca', 'Rama', 'Vjollca Rama', 'SQ', 0, false, NOW(), NOW()),
-- Company 1 – Staff: junior dev (contract, failed login attempt, locked briefly)
('a1100006-0000-0000-0000-000000000001', 'driton.berisha@worknest-tech.al',
 '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
 'ACTIVE', 'Driton', 'Berisha', 'Driton Berisha', 'SQ', 2, false, NOW(), NOW()),
-- Company 2 – Owner (isolation test)
('a2200001-0000-0000-0000-000000000002', 'admin@acmecorp.al',
 '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
 'ACTIVE', 'Luan', 'Gashi', 'Luan Gashi', 'EN', 0, false, NOW(), NOW()),
-- Company 2 – Staff (isolation test: must not appear in c1 queries)
('a2200002-0000-0000-0000-000000000002', 'staff@acmecorp.al',
 '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
 'ACTIVE', 'Fatos', 'Cela', 'Fatos Cela', 'EN', 0, false, NOW(), NOW());

-- ============================================================
-- 3. ROLE ASSIGNMENTS
-- ============================================================
INSERT INTO role_assignments (
    id, company_id, user_id, role, job_title,
    is_active, platform_access,
    activated_at, created_at, updated_at
) VALUES
-- Company 1
('ea110001-0000-0000-0000-000000000001', 'c1000000-0000-0000-0000-000000000001',
 'a1100001-0000-0000-0000-000000000001', 'ADMIN', 'CEO',
 true, 'BOTH', NOW(), NOW(), NOW()),
('ea110002-0000-0000-0000-000000000001', 'c1000000-0000-0000-0000-000000000001',
 'a1100002-0000-0000-0000-000000000001', 'ADMIN', 'Engineering Manager',
 true, 'BOTH', NOW(), NOW(), NOW()),
('ea110003-0000-0000-0000-000000000001', 'c1000000-0000-0000-0000-000000000001',
 'a1100003-0000-0000-0000-000000000001', 'STAFF', 'Senior Software Engineer',
 true, 'MOBILE', NOW(), NOW(), NOW()),
('ea110004-0000-0000-0000-000000000001', 'c1000000-0000-0000-0000-000000000001',
 'a1100004-0000-0000-0000-000000000001', 'STAFF', 'HR Specialist',
 true, 'MOBILE', NOW(), NOW(), NOW()),
('ea110005-0000-0000-0000-000000000001', 'c1000000-0000-0000-0000-000000000001',
 'a1100005-0000-0000-0000-000000000001', 'STAFF', 'Sales Representative',
 true, 'MOBILE', NOW(), NOW(), NOW()),
('ea110006-0000-0000-0000-000000000001', 'c1000000-0000-0000-0000-000000000001',
 'a1100006-0000-0000-0000-000000000001', 'STAFF', 'Junior Developer',
 true, 'MOBILE', NOW(), NOW(), NOW()),
-- Company 2
('ea220001-0000-0000-0000-000000000002', 'c2000000-0000-0000-0000-000000000002',
 'a2200001-0000-0000-0000-000000000002', 'ADMIN', 'CEO',
 true, 'BOTH', NOW(), NOW(), NOW()),
('ea220002-0000-0000-0000-000000000002', 'c2000000-0000-0000-0000-000000000002',
 'a2200002-0000-0000-0000-000000000002', 'STAFF', 'Sales Clerk',
 true, 'MOBILE', NOW(), NOW(), NOW());

-- ============================================================
-- 4. DEPARTMENTS (no manager_id / parent_id in schema)
-- ============================================================
INSERT INTO departments (id, company_id, name, description, status, created_at, updated_at) VALUES
('d1100001-0000-0000-0000-000000000001', 'c1000000-0000-0000-0000-000000000001',
 'Engineering', 'Product and platform engineering', 'ACTIVE', NOW(), NOW()),
('d1100002-0000-0000-0000-000000000001', 'c1000000-0000-0000-0000-000000000001',
 'Human Resources', 'People operations', 'ACTIVE', NOW(), NOW()),
('d1100003-0000-0000-0000-000000000001', 'c1000000-0000-0000-0000-000000000001',
 'Sales', 'Revenue and business development', 'ACTIVE', NOW(), NOW()),
-- Company 2
('d2200001-0000-0000-0000-000000000002', 'c2000000-0000-0000-0000-000000000002',
 'Operations', 'Daily ops', 'ACTIVE', NOW(), NOW());

-- ============================================================
-- 5. COMPANY SITES
-- ============================================================
INSERT INTO company_sites (
    id, company_id, code, name, type, status,
    address_line_1, city, country_code, timezone,
    latitude, longitude,
    geofence_shape_type, geofence_radius_meters,
    entry_buffer_meters, exit_buffer_meters, max_location_accuracy_meters,
    location_required, qr_enabled, check_in_enabled, check_out_enabled,
    created_at, updated_at
) VALUES
-- HQ with geofence
(
    '51100001-0000-0000-0000-000000000001',
    'c1000000-0000-0000-0000-000000000001',
    'HQ-TIR', 'Tirana HQ', 'HQ', 'ACTIVE',
    'Rruga e Kavajës, Blloku', 'Tirana', 'AL', 'Europe/Tirane',
    41.327953, 19.819025, 'CIRCLE', 100, 20, 20, 30,
    true, true, true, true, NOW(), NOW()
),
-- Remote site (no geofence)
(
    '51100002-0000-0000-0000-000000000001',
    'c1000000-0000-0000-0000-000000000001',
    'REMOTE', 'Remote / Work From Home', 'FIELD_ZONE', 'ACTIVE',
    NULL, NULL, 'AL', 'Europe/Tirane',
    NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    false, false, true, true, NOW(), NOW()
),
-- Company 2
(
    '52200001-0000-0000-0000-000000000002',
    'c2000000-0000-0000-0000-000000000002',
    'SHOP', 'Acme Main Shop', 'STORE', 'ACTIVE',
    'Rruga Myslym Shyri', 'Tirana', 'AL', 'Europe/Tirane',
    41.332000, 19.825000, 'CIRCLE', 50, 10, 10, 25,
    true, true, true, true, NOW(), NOW()
);

-- ============================================================
-- 6. SITE TRUSTED NETWORKS (HQ only)
-- ============================================================
INSERT INTO site_trusted_networks (
    id, site_id, name, network_type, cidr_block, ip_version,
    is_active, priority_order, notes, version, created_at, updated_at
) VALUES
('f1100001-0000-0000-0000-000000000001',
 '51100001-0000-0000-0000-000000000001',
 'Office WiFi', 'OFFICE_NETWORK', '192.168.10.0/24', 'IPV4',
 true, 1, 'Primary office WiFi network', 0, NOW(), NOW()),
('f1100002-0000-0000-0000-000000000001',
 '51100001-0000-0000-0000-000000000001',
 'Office LAN', 'OFFICE_NETWORK', '10.0.0.0/16', 'IPV4',
 true, 2, 'Wired office LAN', 0, NOW(), NOW());

-- ============================================================
-- 7. EMPLOYEES
--   emp1 (Blerina) – FULL_TIME, FIXED_MONTHLY 150 000 ALL → payroll PAID Apr
--   emp2 (Agron)   – FULL_TIME, FIXED_MONTHLY  90 000 ALL → payroll FINALIZED Apr
--   emp3 (Vjollca) – PART_TIME, HOURLY 600 ALL/h 6h/day   → payroll CALCULATED Apr
--   emp4 (Driton)  – CONTRACT,  FIXED_MONTHLY  80 000 ALL → payroll DRAFT Apr
--   (Erjon – ADMIN, no employee record – reviews others)
-- ============================================================
INSERT INTO employees (
    id, company_id, user_id, department_id, company_site_id,
    employment_type_role, start_date, supervisor_role_assignment_id,
    employment_status, contract_expiry_date, leave_days_per_year,
    daily_working_hours, payment_method, monthly_salary, hourly_rate,
    employment_type, created_at, updated_at
) VALUES
(
    'e1100001-0000-0000-0000-000000000001',
    'c1000000-0000-0000-0000-000000000001', 'a1100003-0000-0000-0000-000000000001',
    'd1100001-0000-0000-0000-000000000001', '51100001-0000-0000-0000-000000000001',
    'STAFF', '2023-01-15', 'ea110002-0000-0000-0000-000000000001',
    'ACTIVE', '2027-01-14', 20, 8.0,
    'FIXED_MONTHLY', 150000.00, NULL, 'FULL_TIME', NOW(), NOW()
),
(
    'e1100002-0000-0000-0000-000000000001',
    'c1000000-0000-0000-0000-000000000001', 'a1100004-0000-0000-0000-000000000001',
    'd1100002-0000-0000-0000-000000000001', '51100001-0000-0000-0000-000000000001',
    'STAFF', '2023-06-01', 'ea110002-0000-0000-0000-000000000001',
    'ACTIVE', '2027-05-31', 20, 8.0,
    'FIXED_MONTHLY', 90000.00, NULL, 'FULL_TIME', NOW(), NOW()
),
(
    'e1100003-0000-0000-0000-000000000001',
    'c1000000-0000-0000-0000-000000000001', 'a1100005-0000-0000-0000-000000000001',
    'd1100003-0000-0000-0000-000000000001', '51100001-0000-0000-0000-000000000001',
    'STAFF', '2024-03-01', 'ea110002-0000-0000-0000-000000000001',
    'ACTIVE', NULL, 10, 6.0,
    'HOURLY', NULL, 600.00, 'PART_TIME', NOW(), NOW()
),
(
    'e1100004-0000-0000-0000-000000000001',
    'c1000000-0000-0000-0000-000000000001', 'a1100006-0000-0000-0000-000000000001',
    'd1100001-0000-0000-0000-000000000001', '51100001-0000-0000-0000-000000000001',
    'STAFF', '2025-09-01', 'ea110002-0000-0000-0000-000000000001',
    'ACTIVE', '2026-05-31', 15, 8.0,
    'FIXED_MONTHLY', 80000.00, NULL, 'CONTRACT', NOW(), NOW()
),
-- Company 2 employee (isolation)
(
    'e2200001-0000-0000-0000-000000000002',
    'c2000000-0000-0000-0000-000000000002', 'a2200002-0000-0000-0000-000000000002',
    'd2200001-0000-0000-0000-000000000002', '52200001-0000-0000-0000-000000000002',
    'STAFF', '2025-01-10', 'ea220001-0000-0000-0000-000000000002',
    'ACTIVE', NULL, 20, 8.0,
    'FIXED_MONTHLY', 70000.00, NULL, 'FULL_TIME', NOW(), NOW()
);

-- Supervisor history: emp4 was initially unsupervised, then assigned to Erjon
INSERT INTO employee_supervisor_history (
    id, company_id, employee_id, from_role_assignment_id, to_role_assignment_id,
    changed_by_user_id, changed_at, reason
) VALUES
(
    'eb110001-0000-0000-0000-000000000001',
    'c1000000-0000-0000-0000-000000000001',
    'e1100004-0000-0000-0000-000000000001',
    NULL, 'ea110002-0000-0000-0000-000000000001',
    'a1100001-0000-0000-0000-000000000001',
    '2025-09-01 09:00:00+00',
    'Initial supervisor assignment at onboarding'
);

-- ============================================================
-- 8. ATTENDANCE POLICIES
--   company-level (site_id NULL) + HQ site override
-- ============================================================
INSERT INTO attendance_policies (
    id, company_id, site_id,
    require_qr, require_location, check_in_enabled, check_out_enabled,
    use_network_as_warning, reject_outside_geofence, reject_poor_accuracy,
    allow_manual_correction, allow_manager_manual_entry,
    version, created_at, updated_at
) VALUES
(
    'ab100000-0000-0000-0000-000000000001',
    'c1000000-0000-0000-0000-000000000001', NULL,
    true, true, true, true,
    true, true, false,
    true, true,
    0, NOW(), NOW()
),
(
    'ab100001-0000-0000-0000-000000000001',
    'c1000000-0000-0000-0000-000000000001', '51100001-0000-0000-0000-000000000001',
    true, true, true, true,
    true, true, true,
    true, true,
    0, NOW(), NOW()
);

-- ============================================================
-- 9. QR TERMINALS
-- ============================================================
INSERT INTO attendance_qr_terminals (
    id, company_id, site_id, name, status, rotation_seconds,
    secret_key_version, auto_created, last_heartbeat_at,
    version, created_at, updated_at
) VALUES
('b4110001-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001', '51100001-0000-0000-0000-000000000001',
 'Reception Terminal', 'ACTIVE', 30, 'v1', false,
 NOW() - INTERVAL '5 minutes', 0, NOW(), NOW()),
('b4110002-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001', '51100001-0000-0000-0000-000000000001',
 'Floor 2 Terminal', 'ACTIVE', 30, 'v1', true,
 NOW() - INTERVAL '90 minutes', 0, NOW(), NOW()),
-- Disabled terminal – tests terminal lifecycle
('b4110003-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001', '51100001-0000-0000-0000-000000000001',
 'Old Lobby Terminal', 'DISABLED', 30, 'v1', false,
 NOW() - INTERVAL '10 days', 0, NOW(), NOW());

-- ============================================================
-- 10. QR CHALLENGES (ACTIVE / USED / EXPIRED / REVOKED)
-- ============================================================
INSERT INTO attendance_qr_challenges (
    id, company_id, site_id, qr_terminal_id, nonce, token_hash,
    issued_at, expires_at, used_at, used_by_user_id, status, created_at
) VALUES
-- Currently active challenge
('bc110001-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001', '51100001-0000-0000-0000-000000000001',
 'b4110001-0000-0000-0000-000000000001',
 'nonce-active-001a', 'hash-active-001a-c9d3f4e5b6a78901',
 NOW() - INTERVAL '10 seconds', NOW() + INTERVAL '20 seconds',
 NULL, NULL, 'ACTIVE', NOW() - INTERVAL '10 seconds'),
-- Used by emp1 today at check-in
('bc110002-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001', '51100001-0000-0000-0000-000000000001',
 'b4110001-0000-0000-0000-000000000001',
 'nonce-used-002b', 'hash-used-002b-d4e5f6a7b8c90123',
 NOW() - INTERVAL '8 hours' - INTERVAL '5 seconds',
 NOW() - INTERVAL '8 hours' + INTERVAL '25 seconds',
 NOW() - INTERVAL '8 hours',
 'a1100003-0000-0000-0000-000000000001', 'USED',
 NOW() - INTERVAL '8 hours' - INTERVAL '5 seconds'),
-- Expired (nobody used it before TTL)
('bc110003-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001', '51100001-0000-0000-0000-000000000001',
 'b4110001-0000-0000-0000-000000000001',
 'nonce-expired-003c', 'hash-expired-003c-e5f6a7b8c9d01234',
 NOW() - INTERVAL '90 minutes',
 NOW() - INTERVAL '60 minutes',
 NULL, NULL, 'EXPIRED', NOW() - INTERVAL '90 minutes'),
-- Revoked (admin revoked mid-session)
('bc110004-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001', '51100001-0000-0000-0000-000000000001',
 'b4110002-0000-0000-0000-000000000001',
 'nonce-revoked-004d', 'hash-revoked-004d-f6a7b8c9d0e12345',
 NOW() - INTERVAL '2 hours',
 NOW() - INTERVAL '1 hour 59 minutes',
 NULL, NULL, 'REVOKED', NOW() - INTERVAL '2 hours');

-- ============================================================
-- 11. ATTENDANCE DAY RECORDS – April 2026 (payroll_locked = TRUE)
-- ============================================================
-- Working days seeded for April: 1,2,3,7,8,14,15,28
-- April 1 = Wednesday; weekends excluded.

-- ---- emp1 (Blerina) Apr ----
INSERT INTO attendance_day_records (
    id, company_id, employee_id, user_id, site_id,
    work_date, timezone,
    first_check_in_at, last_check_out_at,
    worked_minutes, break_minutes, late_minutes, early_leave_minutes, overtime_minutes,
    day_status, has_warnings, warning_flags_json, review_status, payroll_locked,
    source_event_count, version, created_at, updated_at
) VALUES
-- Apr 01: PRESENT, perfect day
('add11001-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100001-0000-0000-0000-000000000001', 'a1100003-0000-0000-0000-000000000001',
 '51100001-0000-0000-0000-000000000001',
 '2026-04-01', 'Europe/Tirane',
 '2026-04-01 07:00:00+00', '2026-04-01 15:00:00+00',
 480, 0, 0, 0, 0,
 'PRESENT', false, NULL, 'NONE', true, 2, 0, NOW(), NOW()),
-- Apr 02: LATE (checked in 25 min late; within attendance tolerance after grace)
('add11002-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100001-0000-0000-0000-000000000001', 'a1100003-0000-0000-0000-000000000001',
 '51100001-0000-0000-0000-000000000001',
 '2026-04-02', 'Europe/Tirane',
 '2026-04-02 07:25:00+00', '2026-04-02 15:25:00+00',
 480, 0, 15, 0, 0,
 'LATE', false, NULL, 'NONE', true, 2, 0, NOW(), NOW()),
-- Apr 07: PRESENT + overtime (stayed 60 min extra)
('add11007-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100001-0000-0000-0000-000000000001', 'a1100003-0000-0000-0000-000000000001',
 '51100001-0000-0000-0000-000000000001',
 '2026-04-07', 'Europe/Tirane',
 '2026-04-07 07:00:00+00', '2026-04-07 16:00:00+00',
 540, 0, 0, 0, 60,
 'PRESENT', false, NULL, 'NONE', true, 2, 0, NOW(), NOW()),
-- Apr 08: ON_LEAVE (approved vacation)
('add11008-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100001-0000-0000-0000-000000000001', 'a1100003-0000-0000-0000-000000000001',
 '51100001-0000-0000-0000-000000000001',
 '2026-04-08', 'Europe/Tirane',
 NULL, NULL,
 0, 0, 0, 0, 0,
 'ON_LEAVE', false, NULL, 'NONE', true, 0, 0, NOW(), NOW()),
-- Apr 14: ABSENT (no events, no leave approved)
('add11014-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100001-0000-0000-0000-000000000001', 'a1100003-0000-0000-0000-000000000001',
 '51100001-0000-0000-0000-000000000001',
 '2026-04-14', 'Europe/Tirane',
 NULL, NULL,
 0, 0, 0, 0, 0,
 'ABSENT', false, NULL, 'APPROVED', true, 0, 0, NOW(), NOW()),
-- Apr 28: PRESENT (normal last week)
('add11028-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100001-0000-0000-0000-000000000001', 'a1100003-0000-0000-0000-000000000001',
 '51100001-0000-0000-0000-000000000001',
 '2026-04-28', 'Europe/Tirane',
 '2026-04-28 07:00:00+00', '2026-04-28 15:00:00+00',
 480, 0, 0, 0, 0,
 'PRESENT', false, NULL, 'NONE', true, 2, 0, NOW(), NOW()),

-- ---- emp2 (Agron) Apr ----
-- Apr 01: PRESENT
('add12001-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100002-0000-0000-0000-000000000001', 'a1100004-0000-0000-0000-000000000001',
 '51100001-0000-0000-0000-000000000001',
 '2026-04-01', 'Europe/Tirane',
 '2026-04-01 07:05:00+00', '2026-04-01 15:00:00+00',
 475, 0, 0, 0, 0,
 'PRESENT', false, NULL, 'NONE', true, 2, 0, NOW(), NOW()),
-- Apr 03: LATE (30 min) with geofence warning (marginal accuracy)
('add12003-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100002-0000-0000-0000-000000000001', 'a1100004-0000-0000-0000-000000000001',
 '51100001-0000-0000-0000-000000000001',
 '2026-04-03', 'Europe/Tirane',
 '2026-04-03 07:30:00+00', '2026-04-03 15:30:00+00',
 480, 0, 20, 0, 0,
 'LATE', true,
 '["GPS_ACCURACY_LOW"]',
 'NONE', true, 2, 0, NOW(), NOW()),
-- Apr 04: MISSING_CHECKOUT (forgot to clock out; auto-close didn't fire)
('add12004-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100002-0000-0000-0000-000000000001', 'a1100004-0000-0000-0000-000000000001',
 '51100001-0000-0000-0000-000000000001',
 '2026-04-04', 'Europe/Tirane',
 '2026-04-04 07:00:00+00', NULL,
 0, 0, 0, 0, 0,
 'MISSING_CHECKOUT', true,
 '["MISSING_CHECKOUT"]',
 'CORRECTED', true, 1, 0, NOW(), NOW()),
-- Apr 15: FLAGGED – outside geofence (used ACCEPTED_WITH_WARNINGS)
('add12015-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100002-0000-0000-0000-000000000001', 'a1100004-0000-0000-0000-000000000001',
 '51100001-0000-0000-0000-000000000001',
 '2026-04-15', 'Europe/Tirane',
 '2026-04-15 07:00:00+00', '2026-04-15 15:00:00+00',
 480, 0, 0, 0, 0,
 'FLAGGED', true,
 '["OUTSIDE_GEOFENCE","NETWORK_MISMATCH"]',
 'PENDING_REVIEW', true, 2, 0, NOW(), NOW()),

-- ---- emp3 (Vjollca - part-time 6h) Apr ----
-- Apr 01: PRESENT (6h = 360 min)
('add13001-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100003-0000-0000-0000-000000000001', 'a1100005-0000-0000-0000-000000000001',
 '51100001-0000-0000-0000-000000000001',
 '2026-04-01', 'Europe/Tirane',
 '2026-04-01 07:00:00+00', '2026-04-01 13:00:00+00',
 360, 0, 0, 0, 0,
 'PRESENT', false, NULL, 'NONE', true, 2, 0, NOW(), NOW()),
-- Apr 03: HALF_DAY (left 3h early)
('add13003-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100003-0000-0000-0000-000000000001', 'a1100005-0000-0000-0000-000000000001',
 '51100001-0000-0000-0000-000000000001',
 '2026-04-03', 'Europe/Tirane',
 '2026-04-03 07:00:00+00', '2026-04-03 10:00:00+00',
 180, 0, 0, 180, 0,
 'HALF_DAY', false, NULL, 'NONE', true, 2, 0, NOW(), NOW()),

-- ---- emp4 (Driton) Apr ----
-- Apr 01: PRESENT
('add14001-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100004-0000-0000-0000-000000000001', 'a1100006-0000-0000-0000-000000000001',
 '51100001-0000-0000-0000-000000000001',
 '2026-04-01', 'Europe/Tirane',
 '2026-04-01 07:00:00+00', '2026-04-01 15:00:00+00',
 480, 0, 0, 0, 0,
 'PRESENT', false, NULL, 'NONE', false, 2, 0, NOW(), NOW());

-- ============================================================
-- 12. ATTENDANCE DAY RECORDS – May 2026 (payroll_locked = FALSE)
-- ============================================================
INSERT INTO attendance_day_records (
    id, company_id, employee_id, user_id, site_id,
    work_date, timezone,
    first_check_in_at, last_check_out_at,
    worked_minutes, break_minutes, late_minutes, early_leave_minutes, overtime_minutes,
    day_status, has_warnings, warning_flags_json, review_status, payroll_locked,
    source_event_count, version, created_at, updated_at
) VALUES
-- ---- emp1 May ----
-- May 04: PRESENT
('add21004-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100001-0000-0000-0000-000000000001', 'a1100003-0000-0000-0000-000000000001',
 '51100001-0000-0000-0000-000000000001',
 '2026-05-04', 'Europe/Tirane',
 '2026-05-04 07:00:00+00', '2026-05-04 15:00:00+00',
 480, 0, 0, 0, 0,
 'PRESENT', false, NULL, 'NONE', false, 2, 0, NOW(), NOW()),
-- May 05: LATE (15 min, within policy grace of 10 min → still marked late)
('add21005-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100001-0000-0000-0000-000000000001', 'a1100003-0000-0000-0000-000000000001',
 '51100001-0000-0000-0000-000000000001',
 '2026-05-05', 'Europe/Tirane',
 '2026-05-05 07:15:00+00', '2026-05-05 15:15:00+00',
 480, 0, 5, 0, 0,
 'LATE', false, NULL, 'NONE', false, 2, 0, NOW(), NOW()),
-- May 12: MISSING_CHECKOUT (open record, manager must correct)
('add21012-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100001-0000-0000-0000-000000000001', 'a1100003-0000-0000-0000-000000000001',
 '51100001-0000-0000-0000-000000000001',
 '2026-05-12', 'Europe/Tirane',
 '2026-05-12 07:00:00+00', NULL,
 0, 0, 0, 0, 0,
 'MISSING_CHECKOUT', true,
 '["MISSING_CHECKOUT"]',
 'PENDING_REVIEW', false, 1, 0, NOW(), NOW()),
-- May 18: PRESENT (today)
('add21018-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100001-0000-0000-0000-000000000001', 'a1100003-0000-0000-0000-000000000001',
 '51100001-0000-0000-0000-000000000001',
 '2026-05-18', 'Europe/Tirane',
 '2026-05-18 07:00:00+00', NULL,
 0, 0, 0, 0, 0,
 'PRESENT', false, NULL, 'NONE', false, 1, 0, NOW(), NOW()),

-- ---- emp3 May (on sick leave May 12-16) ----
-- May 04: PRESENT (6h)
('add23004-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100003-0000-0000-0000-000000000001', 'a1100005-0000-0000-0000-000000000001',
 '51100001-0000-0000-0000-000000000001',
 '2026-05-04', 'Europe/Tirane',
 '2026-05-04 07:00:00+00', '2026-05-04 13:00:00+00',
 360, 0, 0, 0, 0,
 'PRESENT', false, NULL, 'NONE', false, 2, 0, NOW(), NOW()),
-- May 12: ON_LEAVE (sick)
('add23012-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100003-0000-0000-0000-000000000001', 'a1100005-0000-0000-0000-000000000001',
 '51100001-0000-0000-0000-000000000001',
 '2026-05-12', 'Europe/Tirane',
 NULL, NULL, 0, 0, 0, 0, 0,
 'ON_LEAVE', false, NULL, 'NONE', false, 0, 0, NOW(), NOW()),
-- May 13: ON_LEAVE (sick)
('add23013-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100003-0000-0000-0000-000000000001', 'a1100005-0000-0000-0000-000000000001',
 '51100001-0000-0000-0000-000000000001',
 '2026-05-13', 'Europe/Tirane',
 NULL, NULL, 0, 0, 0, 0, 0,
 'ON_LEAVE', false, NULL, 'NONE', false, 0, 0, NOW(), NOW()),

-- ---- emp4 May (contract expiry edge case) ----
-- May 04: PRESENT
('add24004-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100004-0000-0000-0000-000000000001', 'a1100006-0000-0000-0000-000000000001',
 '51100001-0000-0000-0000-000000000001',
 '2026-05-04', 'Europe/Tirane',
 '2026-05-04 07:00:00+00', '2026-05-04 15:00:00+00',
 480, 0, 0, 0, 0,
 'PRESENT', false, NULL, 'NONE', false, 2, 0, NOW(), NOW()),
-- May 07: FLAGGED (clocked in from outside geofence – network warning + geofence warning)
('add24007-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100004-0000-0000-0000-000000000001', 'a1100006-0000-0000-0000-000000000001',
 '51100001-0000-0000-0000-000000000001',
 '2026-05-07', 'Europe/Tirane',
 '2026-05-07 07:00:00+00', '2026-05-07 15:00:00+00',
 480, 0, 0, 0, 0,
 'FLAGGED', true,
 '["OUTSIDE_GEOFENCE","NETWORK_MISMATCH"]',
 'PENDING_REVIEW', false, 2, 0, NOW(), NOW()),
-- May 13: PRESENT with PENDING_REVIEW (manager requested review for no apparent reason)
('add24013-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100004-0000-0000-0000-000000000001', 'a1100006-0000-0000-0000-000000000001',
 '51100001-0000-0000-0000-000000000001',
 '2026-05-13', 'Europe/Tirane',
 '2026-05-13 07:00:00+00', '2026-05-13 15:00:00+00',
 480, 0, 0, 0, 0,
 'PRESENT', false, NULL, 'PENDING_REVIEW', false, 2, 0, NOW(), NOW());

-- ============================================================
-- 13. ATTENDANCE EVENTS (representative subset)
-- ============================================================
INSERT INTO attendance_events (
    id, company_id, employee_id, user_id, site_id,
    event_type, event_status, capture_method, attendance_decision,
    server_recorded_at, client_captured_at, work_date, timezone,
    client_request_id, device_public_id, platform, app_version,
    qr_challenge_id, qr_terminal_id, qr_validation_status,
    latitude, longitude, accuracy_meters, distance_from_site_meters,
    inside_geofence, geofence_decision,
    request_ip_address, network_matched, matched_network_id, network_decision,
    risk_score, review_status,
    created_at
) VALUES
-- emp1 Apr 01 check-in (perfect: QR + inside geofence + office network)
('ae110001-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100001-0000-0000-0000-000000000001', 'a1100003-0000-0000-0000-000000000001',
 '51100001-0000-0000-0000-000000000001',
 'CHECK_IN', 'ACCEPTED', 'QR_GEOFENCE', 'ACCEPTED',
 '2026-04-01 07:00:00+00', '2026-04-01 06:59:55+00', '2026-04-01', 'Europe/Tirane',
 'req-e1-2026-04-01-in', 'device-blerina-01', 'ANDROID', '2.1.0',
 'bc110002-0000-0000-0000-000000000001', 'b4110001-0000-0000-0000-000000000001', 'VALID',
 41.327953, 19.819025, 5.0, 3.2,
 true, 'PASSED',
 '192.168.10.45', true, 'f1100001-0000-0000-0000-000000000001', 'PASSED',
 0, 'NONE', '2026-04-01 07:00:00+00'),
-- emp1 Apr 01 check-out (perfect)
('ae110002-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100001-0000-0000-0000-000000000001', 'a1100003-0000-0000-0000-000000000001',
 '51100001-0000-0000-0000-000000000001',
 'CHECK_OUT', 'ACCEPTED', 'QR_GEOFENCE', 'ACCEPTED',
 '2026-04-01 15:00:00+00', '2026-04-01 14:59:50+00', '2026-04-01', 'Europe/Tirane',
 'req-e1-2026-04-01-out', 'device-blerina-01', 'ANDROID', '2.1.0',
 NULL, 'b4110001-0000-0000-0000-000000000001', 'NOT_REQUIRED',
 41.327953, 19.819025, 5.0, 3.2,
 true, 'PASSED',
 '192.168.10.45', true, 'f1100001-0000-0000-0000-000000000001', 'PASSED',
 0, 'NONE', '2026-04-01 15:00:00+00'),
-- emp2 Apr 04 check-in (missing checkout – only one event)
('ae120004-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100002-0000-0000-0000-000000000001', 'a1100004-0000-0000-0000-000000000001',
 '51100001-0000-0000-0000-000000000001',
 'CHECK_IN', 'ACCEPTED', 'QR_GEOFENCE', 'ACCEPTED',
 '2026-04-04 07:00:00+00', '2026-04-04 06:59:50+00', '2026-04-04', 'Europe/Tirane',
 'req-e2-2026-04-04-in', 'device-agron-01', 'IOS', '2.1.0',
 NULL, 'b4110001-0000-0000-0000-000000000001', 'NOT_REQUIRED',
 41.327953, 19.819025, 8.0, 6.5,
 true, 'PASSED',
 '192.168.10.62', true, 'f1100001-0000-0000-0000-000000000001', 'PASSED',
 0, 'NONE', '2026-04-04 07:00:00+00'),
-- emp2 Apr 15 check-in OUTSIDE geofence (warning accepted)
('ae12015a-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100002-0000-0000-0000-000000000001', 'a1100004-0000-0000-0000-000000000001',
 '51100001-0000-0000-0000-000000000001',
 'CHECK_IN', 'ACCEPTED_WITH_WARNINGS', 'QR_GEOFENCE', 'ACCEPTED_WITH_WARNINGS',
 '2026-04-15 07:00:00+00', '2026-04-15 06:59:50+00', '2026-04-15', 'Europe/Tirane',
 'req-e2-2026-04-15-in', 'device-agron-01', 'IOS', '2.1.0',
 NULL, 'b4110001-0000-0000-0000-000000000001', 'NOT_REQUIRED',
 41.333000, 19.830000, 12.0, 650.0,
 false, 'FAILED_OUTSIDE',
 '10.1.2.100', false, NULL, 'WARNING',
 45, 'PENDING_REVIEW',
 '2026-04-15 07:00:00+00'),
-- emp2 Apr 15 check-out (outside geofence, warning)
('ae12015b-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100002-0000-0000-0000-000000000001', 'a1100004-0000-0000-0000-000000000001',
 '51100001-0000-0000-0000-000000000001',
 'CHECK_OUT', 'ACCEPTED_WITH_WARNINGS', 'QR_GEOFENCE', 'ACCEPTED_WITH_WARNINGS',
 '2026-04-15 15:00:00+00', '2026-04-15 14:59:50+00', '2026-04-15', 'Europe/Tirane',
 'req-e2-2026-04-15-out', 'device-agron-01', 'IOS', '2.1.0',
 NULL, 'b4110001-0000-0000-0000-000000000001', 'NOT_REQUIRED',
 41.333000, 19.830000, 12.0, 650.0,
 false, 'FAILED_OUTSIDE',
 '10.1.2.100', false, NULL, 'WARNING',
 45, 'PENDING_REVIEW',
 '2026-04-15 15:00:00+00'),
-- emp1 May 18 check-in TODAY (still open)
('ae211018-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100001-0000-0000-0000-000000000001', 'a1100003-0000-0000-0000-000000000001',
 '51100001-0000-0000-0000-000000000001',
 'CHECK_IN', 'ACCEPTED', 'QR_GEOFENCE', 'ACCEPTED',
 '2026-05-18 07:00:00+00', '2026-05-18 06:59:58+00', '2026-05-18', 'Europe/Tirane',
 'req-e1-2026-05-18-in', 'device-blerina-01', 'ANDROID', '2.2.0',
 'bc110002-0000-0000-0000-000000000001', 'b4110001-0000-0000-0000-000000000001', 'VALID',
 41.327953, 19.819025, 4.5, 2.8,
 true, 'PASSED',
 '192.168.10.45', true, 'f1100001-0000-0000-0000-000000000001', 'PASSED',
 0, 'NONE', '2026-05-18 07:00:00+00'),
-- emp4 May 07 check-in REJECTED (policy: reject_outside_geofence=true but geofence decision WARNING)
-- Actually we set it as ACCEPTED_WITH_WARNINGS based on the policy
('ae24007a-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100004-0000-0000-0000-000000000001', 'a1100006-0000-0000-0000-000000000001',
 '51100001-0000-0000-0000-000000000001',
 'CHECK_IN', 'ACCEPTED_WITH_WARNINGS', 'QR_GEOFENCE', 'ACCEPTED_WITH_WARNINGS',
 '2026-05-07 07:00:00+00', '2026-05-07 06:59:55+00', '2026-05-07', 'Europe/Tirane',
 'req-e4-2026-05-07-in', 'device-driton-01', 'ANDROID', '2.2.0',
 NULL, 'b4110001-0000-0000-0000-000000000001', 'NOT_REQUIRED',
 41.340000, 19.840000, 20.0, 1450.0,
 false, 'FAILED_OUTSIDE',
 '172.16.5.88', false, NULL, 'WARNING',
 60, 'PENDING_REVIEW',
 '2026-05-07 07:00:00+00'),
-- Manual correction event by manager (for emp2 Apr 04 missing checkout)
('ae12004f-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100002-0000-0000-0000-000000000001', 'a1100004-0000-0000-0000-000000000001',
 '51100001-0000-0000-0000-000000000001',
 'MANUAL_CHECK_OUT', 'ACCEPTED', 'MANUAL', 'ACCEPTED',
 '2026-04-05 08:00:00+00', NULL, '2026-04-04', 'Europe/Tirane',
 'req-mgr-corr-20260404', 'manager-device', 'WEB', NULL,
 NULL, NULL, 'NOT_REQUIRED',
 NULL, NULL, NULL, NULL,
 NULL, 'NOT_CONFIGURED',
 NULL, NULL, NULL, 'NOT_CONFIGURED',
 0, 'APPROVED',
 '2026-04-05 08:00:00+00');

-- ============================================================
-- 14. ATTENDANCE REVIEW ACTIONS
-- ============================================================
INSERT INTO attendance_review_actions (
    id, company_id, attendance_event_id, attendance_day_record_id,
    action_type, before_snapshot_json, after_snapshot_json,
    reason, acted_by_user_id, acted_at
) VALUES
-- Manager corrected missing checkout for emp2 Apr 04
(
    'ada12004-0000-0000-0000-000000000001',
    'c1000000-0000-0000-0000-000000000001',
    'ae12004f-0000-0000-0000-000000000001',
    'add12004-0000-0000-0000-000000000001',
    'MANUAL_CHECKOUT_ADDED',
    '{"dayStatus":"MISSING_CHECKOUT","lastCheckOutAt":null,"workedMinutes":0}',
    '{"dayStatus":"PRESENT","lastCheckOutAt":"2026-04-04T15:00:00Z","workedMinutes":480}',
    'Employee forgot to clock out; manager added manual checkout at standard end time',
    'a1100002-0000-0000-0000-000000000001',
    '2026-04-05 08:05:00+00'
),
-- Manager approved emp1 Apr 14 absence
(
    'ada11014-0000-0000-0000-000000000001',
    'c1000000-0000-0000-0000-000000000001',
    NULL,
    'add11014-0000-0000-0000-000000000001',
    'ABSENCE_APPROVED',
    '{"reviewStatus":"NONE","dayStatus":"ABSENT"}',
    '{"reviewStatus":"APPROVED","dayStatus":"ABSENT","note":"Medical appointment confirmed"}',
    'Medical appointment confirmed via HR note',
    'a1100002-0000-0000-0000-000000000001',
    '2026-04-15 09:00:00+00'
);

-- ============================================================
-- 15. COMPANY PAYROLL SETTINGS
-- Albanian statutory rates (2025):
--   SS employee 9.5%, SS employer 15.0%
--   Pension employee 5.0%, Pension employer 7.5%
--   Tax base: GROSS_MINUS_CONTRIBUTIONS
--   Daily working hours default: 8h
-- ============================================================
INSERT INTO company_payroll_settings (
    id, company_id, default_daily_working_hours, weekend_days,
    tax_enabled, tax_base,
    social_security_employee_rate, social_security_employer_rate,
    pension_employee_rate, pension_employer_rate,
    contribution_min_base, contribution_max_base,
    maternity_employer_topup_rate, paternity_employer_topup_rate,
    version, created_at, updated_at
) VALUES
(
    'b5110000-0000-0000-0000-000000000001',
    'c1000000-0000-0000-0000-000000000001',
    8.0,
    '["SATURDAY","SUNDAY"]',
    true, 'GROSS_MINUS_CONTRIBUTIONS',
    9.5, 15.0,
    5.0, 7.5,
    30000.00, 1254857.00,
    50.0, 50.0,
    0, NOW(), NOW()
),
-- Company 2 minimal settings (tax disabled)
(
    'b5220000-0000-0000-0000-000000000002',
    'c2000000-0000-0000-0000-000000000002',
    8.0,
    '["SATURDAY","SUNDAY"]',
    false, 'GROSS',
    0.0, 0.0, 0.0, 0.0,
    NULL, NULL,
    0.0, 0.0,
    0, NOW(), NOW()
);

-- ============================================================
-- 16. TAX BRACKETS (Albanian progressive income tax)
-- ============================================================
INSERT INTO company_tax_brackets (
    id, company_id, ordinal, lower_bound, upper_bound, rate,
    version, created_at, updated_at
) VALUES
-- 0 – 30,000 ALL: 0%
('4b110001-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001', 0, 0.00, 30000.00, 0.000,
 0, NOW(), NOW()),
-- 30,001 – 150,000 ALL: 13%
('4b110002-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001', 1, 30000.00, 150000.00, 13.000,
 0, NOW(), NOW()),
-- 150,001 – 200,000 ALL: 23%
('4b110003-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001', 2, 150000.00, 200000.00, 23.000,
 0, NOW(), NOW()),
-- 200,001+ ALL: 23% (top bracket, open-ended)
('4b110004-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001', 3, 200000.00, NULL, 23.000,
 0, NOW(), NOW());

-- ============================================================
-- 17. SICK LEAVE POLICY
-- ============================================================
INSERT INTO company_sick_leave_policies (
    id, company_id, company_paid_percentage, max_company_paid_days,
    created_at, updated_at
) VALUES
(
    '5ab11000-0000-0000-0000-000000000001',
    'c1000000-0000-0000-0000-000000000001',
    50.00, 14,
    NOW(), NOW()
),
(
    '5ab22000-0000-0000-0000-000000000002',
    'c2000000-0000-0000-0000-000000000002',
    70.00, 7,
    NOW(), NOW()
);

-- ============================================================
-- 18. PUBLIC HOLIDAYS (company 1 – Albania 2026)
-- ============================================================
INSERT INTO company_public_holidays (
    id, company_id, holiday_date, name, recurring, paid,
    created_by_user_id, version, created_at, updated_at
) VALUES
-- National holidays (recurring)
('fe110001-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 '2026-01-01', 'New Year''s Day', true, true,
 'a1100001-0000-0000-0000-000000000001', 0, NOW(), NOW()),
('fe110002-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 '2026-03-22', 'Nevruz', true, true,
 'a1100001-0000-0000-0000-000000000001', 0, NOW(), NOW()),
('fe110003-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 '2026-05-01', 'Labour Day', true, true,
 'a1100001-0000-0000-0000-000000000001', 0, NOW(), NOW()),
-- One-time company holiday (non-recurring)
('fe110004-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 '2026-04-10', 'Company Founding Day', false, true,
 'a1100001-0000-0000-0000-000000000001', 0, NOW(), NOW()),
-- Unpaid holiday (edge case: paid = false)
('fe110005-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 '2026-11-28', 'Independence Day', true, false,
 'a1100001-0000-0000-0000-000000000001', 0, NOW(), NOW());

-- ============================================================
-- 19. PAYROLL ADJUSTMENTS
-- ============================================================
INSERT INTO payroll_adjustments (
    id, company_id, employee_id, payroll_year, payroll_month,
    adjustment_type, amount, reason, notes,
    created_by_user_id, created_at, updated_at
) VALUES
-- emp1: performance bonus April (PAID payroll)
('fab11001-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100001-0000-0000-0000-000000000001',
 2026, 4, 'BONUS', 20000.00,
 'Q1 performance bonus',
 'Exceeds KPIs for Q1 2026',
 'a1100001-0000-0000-0000-000000000001',
 '2026-04-25 10:00:00+00', '2026-04-25 10:00:00+00'),
-- emp2: deduction April (salary advance repayment)
('fab12001-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100002-0000-0000-0000-000000000001',
 2026, 4, 'DEDUCTION', 5000.00,
 'Salary advance repayment March 2026',
 NULL,
 'a1100001-0000-0000-0000-000000000001',
 '2026-04-25 10:00:00+00', '2026-04-25 10:00:00+00'),
-- emp1: bonus May (DRAFT – not yet in payroll)
('fab11002-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100001-0000-0000-0000-000000000001',
 2026, 5, 'BONUS', 10000.00,
 'Project delivery bonus',
 'Delivered platform migration ahead of schedule',
 'a1100001-0000-0000-0000-000000000001',
 NOW(), NOW());

-- ============================================================
-- 20. PAYROLL RESULTS
--
-- Calculation reference (Albanian rates, April 2026):
-- emp1 (150 000 + 20 000 bonus = 170 000 gross):
--   SS employee  = 150 000 * 9.5% = 14 250
--   Pension empl = 150 000 * 5.0% =  7 500  → total employee contribs = 21 750
--   Tax base (GROSS_MINUS_CONTRIBUTIONS): 170 000 – 21 750 = 148 250
--   Tax: bracket0(0) + bracket1(30 000-148 250 → 118 250 * 13%) = 15 372.50
--   Net = 170 000 – 14 250 – 7 500 – 15 372.50 = 132 877.50
--   SS employer  = 150 000 * 15.0% = 22 500
--   Pension empl-er = 150 000 * 7.5% = 11 250
--   Employer cost = 170 000 + 22 500 + 11 250 = 203 750
--
-- emp2 (90 000 – 5 000 deduction = 85 000 net-adjusted gross):
--   SS employee  = 90 000 * 9.5% = 8 550
--   Pension empl = 90 000 * 5.0% = 4 500 → total = 13 050
--   Tax base: 85 000 – 13 050 = 71 950
--   Tax: 0 + (71 950 – 30 000) * 13% = 41 950 * 13% = 5 453.50
--   Net = 85 000 – 8 550 – 4 500 – 5 453.50 = 66 496.50
--
-- emp3 (hourly 600 ALL/h × 6h/day × 22 working days = 79 200):
--   SS employee  = 79 200 * 9.5% = 7 524
--   Pension empl = 79 200 * 5.0% = 3 960 → total = 11 484
--   Tax base: 79 200 – 11 484 = 67 716
--   Tax: 0 + (67 716 – 30 000) * 13% = 37 716 * 13% = 4 903.08
--   Net = 79 200 – 7 524 – 3 960 – 4 903.08 = 62 812.92
-- ============================================================
INSERT INTO payroll_results (
    id, company_id, employee_id, payroll_year, payroll_month,
    status, base_pay, gross_earnings, total_deductions, net_pay,
    income_tax, employee_social_security, employee_pension,
    employer_social_security, employer_pension, taxable_income, employer_cost_total,
    calculation_snapshot_json, calculated_at, calculated_by_user_id,
    version, created_at, updated_at
) VALUES
-- emp1 April: PAID (full cycle complete)
(
    'fda11001-0000-0000-0000-000000000001',
    'c1000000-0000-0000-0000-000000000001',
    'e1100001-0000-0000-0000-000000000001',
    2026, 4,
    'PAID',
    150000.00, 170000.00, 37122.50, 132877.50,
    15372.50, 14250.00, 7500.00,
    22500.00, 11250.00, 148250.00, 203750.00,
    '{"snapshot":"v1","period":"2026-04","employee":"Blerina Hoxha","basePay":150000,"bonus":20000,"grossEarnings":170000,"ssEmployee":14250,"pensionEmployee":7500,"totalContributions":21750,"taxBase":148250,"incomeTax":15372.50,"netPay":132877.50,"ssEmployer":22500,"pensionEmployer":11250,"employerCostTotal":203750,"calculatedAt":"2026-05-01T09:00:00Z","paymentMethod":"FIXED_MONTHLY"}',
    '2026-05-01 09:00:00+00', 'a1100001-0000-0000-0000-000000000001',
    3, NOW(), NOW()
),
-- emp2 April: FINALIZED (awaiting payment)
(
    'fda12001-0000-0000-0000-000000000001',
    'c1000000-0000-0000-0000-000000000001',
    'e1100002-0000-0000-0000-000000000001',
    2026, 4,
    'FINALIZED',
    90000.00, 85000.00, 18503.50, 66496.50,
    5453.50, 8550.00, 4500.00,
    13500.00, 6750.00, 71950.00, 105250.00,
    '{"snapshot":"v1","period":"2026-04","employee":"Agron Musa","basePay":90000,"deduction":5000,"grossEarnings":85000,"ssEmployee":8550,"pensionEmployee":4500,"totalContributions":13050,"taxBase":71950,"incomeTax":5453.50,"netPay":66496.50,"ssEmployer":13500,"pensionEmployer":6750,"employerCostTotal":105250,"calculatedAt":"2026-05-01T09:05:00Z","paymentMethod":"FIXED_MONTHLY"}',
    '2026-05-01 09:05:00+00', 'a1100001-0000-0000-0000-000000000001',
    2, NOW(), NOW()
),
-- emp3 April: CALCULATED (approved but not finalized)
(
    'fda13001-0000-0000-0000-000000000001',
    'c1000000-0000-0000-0000-000000000001',
    'e1100003-0000-0000-0000-000000000001',
    2026, 4,
    'CALCULATED',
    79200.00, 79200.00, 16387.08, 62812.92,
    4903.08, 7524.00, 3960.00,
    11880.00, 5940.00, 67716.00, 97020.00,
    '{"snapshot":"v1","period":"2026-04","employee":"Vjollca Rama","basePay":79200,"grossEarnings":79200,"hourlyRate":600,"dailyHours":6,"workingDays":22,"ssEmployee":7524,"pensionEmployee":3960,"totalContributions":11484,"taxBase":67716,"incomeTax":4903.08,"netPay":62812.92,"ssEmployer":11880,"pensionEmployer":5940,"employerCostTotal":97020,"calculatedAt":"2026-05-01T09:10:00Z","paymentMethod":"HOURLY"}',
    '2026-05-01 09:10:00+00', 'a1100001-0000-0000-0000-000000000001',
    1, NOW(), NOW()
),
-- emp4 April: DRAFT (manager hasn't triggered calculation yet)
(
    'fda14001-0000-0000-0000-000000000001',
    'c1000000-0000-0000-0000-000000000001',
    'e1100004-0000-0000-0000-000000000001',
    2026, 4,
    'DRAFT',
    80000.00, 80000.00, 0.00, 80000.00,
    0.00, 0.00, 0.00,
    0.00, 0.00, 0.00, 0.00,
    '{"snapshot":"draft","period":"2026-04","employee":"Driton Berisha","note":"Draft only – calculation pending"}',
    '2026-04-30 17:00:00+00', NULL,
    0, NOW(), NOW()
),
-- emp1 May: DRAFT (current month, just created)
(
    'fdb11001-0000-0000-0000-000000000001',
    'c1000000-0000-0000-0000-000000000001',
    'e1100001-0000-0000-0000-000000000001',
    2026, 5,
    'DRAFT',
    150000.00, 150000.00, 0.00, 150000.00,
    0.00, 0.00, 0.00,
    0.00, 0.00, 0.00, 0.00,
    '{"snapshot":"draft","period":"2026-05","employee":"Blerina Hoxha","note":"Draft only – month in progress"}',
    NOW(), NULL,
    0, NOW(), NOW()
);

-- ============================================================
-- 21. LEAVE REQUESTS
-- ============================================================
INSERT INTO leave_requests (
    id, company_id, employee_id, leave_type,
    start_date, end_date, days_count, status,
    note, approval_note, rejection_reason,
    reviewed_by_user_id, reviewed_at,
    version, created_at, updated_at
) VALUES
-- emp1: APPROVED vacation (covers Apr 08 – used in attendance above)
('ca110001-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100001-0000-0000-0000-000000000001',
 'VACATION', '2026-04-08', '2026-04-08', 1.0, 'APPROVED',
 'Personal appointment', 'Approved – planned ahead', NULL,
 'a1100002-0000-0000-0000-000000000001', '2026-04-05 10:00:00+00',
 1, '2026-04-03 12:00:00+00', '2026-04-05 10:00:00+00'),
-- emp3: APPROVED sick leave (covers May 12-13 attendance above)
('ca130001-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100003-0000-0000-0000-000000000001',
 'SICK', '2026-05-12', '2026-05-14', 3.0, 'APPROVED',
 'Flu symptoms', 'Approved with medical certificate', NULL,
 'a1100002-0000-0000-0000-000000000001', '2026-05-12 09:00:00+00',
 1, '2026-05-12 08:00:00+00', '2026-05-12 09:00:00+00'),
-- emp3: half-day leave (edge case: days_count = 0.5)
('ca130002-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100003-0000-0000-0000-000000000001',
 'PERSONAL', '2026-04-03', '2026-04-03', 0.5, 'APPROVED',
 'Personal errand – leaving at noon', 'Approved', NULL,
 'a1100002-0000-0000-0000-000000000001', '2026-04-02 14:00:00+00',
 1, '2026-04-02 10:00:00+00', '2026-04-02 14:00:00+00'),
-- emp4: PENDING leave request (not yet reviewed)
('ca140001-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100004-0000-0000-0000-000000000001',
 'VACATION', '2026-06-01', '2026-06-05', 5.0, 'PENDING',
 'Summer vacation', NULL, NULL,
 NULL, NULL,
 0, '2026-05-15 09:00:00+00', '2026-05-15 09:00:00+00'),
-- emp2: REJECTED leave (not enough balance)
('ca120001-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100002-0000-0000-0000-000000000001',
 'VACATION', '2026-04-20', '2026-04-25', 5.0, 'REJECTED',
 'Holiday trip', NULL, 'Insufficient leave balance for requested period',
 'a1100002-0000-0000-0000-000000000001', '2026-04-17 11:00:00+00',
 1, '2026-04-15 14:00:00+00', '2026-04-17 11:00:00+00'),
-- emp1: CANCELLED leave (employee withdrew it)
('ca110002-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100001-0000-0000-0000-000000000001',
 'VACATION', '2026-05-25', '2026-05-29', 5.0, 'CANCELLED',
 'Weekend getaway', NULL, NULL,
 NULL, NULL,
 1, '2026-05-10 08:00:00+00', '2026-05-14 09:00:00+00'),
-- emp2: MATERNITY leave – longer duration (edge case for payroll/balance)
('ca120002-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100002-0000-0000-0000-000000000001',
 'MATERNITY', '2026-07-01', '2026-10-31', 90.0, 'APPROVED',
 'Maternity leave',
 'Approved per labour law – 365 days maximum entitlement',
 NULL,
 'a1100001-0000-0000-0000-000000000001', '2026-06-20 10:00:00+00',
 1, '2026-06-15 09:00:00+00', '2026-06-20 10:00:00+00');

-- ============================================================
-- 22. LEAVE BALANCES (2026)
-- ============================================================
INSERT INTO leave_balances (
    id, company_id, employee_id, year, leave_type,
    total_days, used_days,
    version, created_at, updated_at
) VALUES
-- emp1 balances (20 vacation days/year; 1 used)
('cb110001-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100001-0000-0000-0000-000000000001',
 2026, 'VACATION', 20.0, 1.0, 0, NOW(), NOW()),
('cb110002-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100001-0000-0000-0000-000000000001',
 2026, 'SICK', 14.0, 0.0, 0, NOW(), NOW()),
-- emp2 balances (vacation exhausted → rejection scenario above)
('cb120001-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100002-0000-0000-0000-000000000001',
 2026, 'VACATION', 20.0, 18.0, 0, NOW(), NOW()),
('cb120002-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100002-0000-0000-0000-000000000001',
 2026, 'SICK', 14.0, 0.0, 0, NOW(), NOW()),
-- emp3 balances (10 days/year; 3 sick used + 0.5 personal half-day)
('cb130001-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100003-0000-0000-0000-000000000001',
 2026, 'VACATION', 10.0, 0.0, 0, NOW(), NOW()),
('cb130002-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100003-0000-0000-0000-000000000001',
 2026, 'SICK', 14.0, 3.0, 0, NOW(), NOW()),
('cb130003-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100003-0000-0000-0000-000000000001',
 2026, 'PERSONAL', 5.0, 0.5, 0, NOW(), NOW()),
-- emp4 balances (15 days/year; nothing used yet)
('cb140001-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100004-0000-0000-0000-000000000001',
 2026, 'VACATION', 15.0, 0.0, 0, NOW(), NOW()),
('cb140002-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'e1100004-0000-0000-0000-000000000001',
 2026, 'SICK', 14.0, 0.0, 0, NOW(), NOW());

-- ============================================================
-- 23. ANNOUNCEMENTS
-- ============================================================
INSERT INTO announcements (
    id, company_id, title, content, target_audience, priority,
    created_by_user_id, created_at, updated_at
) VALUES
-- Company-wide high-priority
('a0110001-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'Q2 All-Hands Meeting – May 22',
 'Our Q2 all-hands is scheduled for Friday May 22 at 10:00 in the main conference room. Attendance is mandatory for all staff.',
 'ALL', 'HIGH',
 'a1100001-0000-0000-0000-000000000001',
 '2026-05-15 08:00:00+00', '2026-05-15 08:00:00+00'),
-- Department-targeted (Engineering)
('a0110002-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'New Code Review Guidelines',
 'Effective June 1, all PRs require a minimum of two reviewer approvals. See confluence for updated process.',
 'DEPARTMENT', 'NORMAL',
 'a1100002-0000-0000-0000-000000000001',
 '2026-05-10 09:00:00+00', '2026-05-10 09:00:00+00'),
-- Normal announcement (all staff)
('a0110003-0000-0000-0000-000000000001',
 'c1000000-0000-0000-0000-000000000001',
 'Office AC Maintenance – May 19',
 'The office AC system will be serviced on Monday May 19 from 08:00–10:00. Please bring a light jacket.',
 'ALL', 'NORMAL',
 'a1100001-0000-0000-0000-000000000001',
 '2026-05-16 14:00:00+00', '2026-05-16 14:00:00+00');

-- Department targeting for Engineering announcement
INSERT INTO announcement_departments (announcement_id, department_id) VALUES
('a0110002-0000-0000-0000-000000000001', 'd1100001-0000-0000-0000-000000000001');

-- Announcement reads (emp1 read the all-hands; emp3 hasn't read anything)
INSERT INTO announcement_reads (id, announcement_id, employee_id, read_at) VALUES
('ad110001-0000-0000-0000-000000000001',
 'a0110001-0000-0000-0000-000000000001',
 'e1100001-0000-0000-0000-000000000001',
 '2026-05-15 09:30:00+00'),
('ad110002-0000-0000-0000-000000000001',
 'a0110002-0000-0000-0000-000000000001',
 'e1100001-0000-0000-0000-000000000001',
 '2026-05-10 10:15:00+00');

-- ============================================================
-- 24. AUDIT LOG (representative entries)
-- ============================================================
INSERT INTO audit_logs (
    company_id, actor_user_id, actor_role_assignment_id, actor_role, actor_job_title,
    action, entity_type, entity_id,
    diff, metadata, ip_address, created_at
) VALUES
-- Payroll approved (emp1 Apr)
('c1000000-0000-0000-0000-000000000001',
 'a1100001-0000-0000-0000-000000000001',
 'ea110001-0000-0000-0000-000000000001', 'ADMIN', 'CEO',
 'PAYROLL_APPROVED', 'PayrollResult',
 'fda11001-0000-0000-0000-000000000001',
 '{"before":"CALCULATED","after":"APPROVED"}', NULL,
 '192.168.1.10', '2026-05-03 10:00:00+00'),
-- Payroll finalized (emp1 Apr)
('c1000000-0000-0000-0000-000000000001',
 'a1100001-0000-0000-0000-000000000001',
 'ea110001-0000-0000-0000-000000000001', 'ADMIN', 'CEO',
 'PAYROLL_FINALIZED', 'PayrollResult',
 'fda11001-0000-0000-0000-000000000001',
 '{"before":"APPROVED","after":"FINALIZED"}', NULL,
 '192.168.1.10', '2026-05-05 10:00:00+00'),
-- Leave approved (emp1 vacation)
('c1000000-0000-0000-0000-000000000001',
 'a1100002-0000-0000-0000-000000000001',
 'ea110002-0000-0000-0000-000000000001', 'ADMIN', 'Engineering Manager',
 'LEAVE_APPROVED', 'LeaveRequest',
 'ca110001-0000-0000-0000-000000000001',
 '{"before":"PENDING","after":"APPROVED"}', NULL,
 '192.168.1.20', '2026-04-05 10:00:00+00'),
-- Attendance warning dismissed
('c1000000-0000-0000-0000-000000000001',
 'a1100002-0000-0000-0000-000000000001',
 'ea110002-0000-0000-0000-000000000001', 'ADMIN', 'Engineering Manager',
 'ATTENDANCE_WARNINGS_DISMISSED', 'AttendanceDayRecord',
 'add12003-0000-0000-0000-000000000001',
 '{"dismissed":["GPS_ACCURACY_LOW"],"reason":"Employee confirmed location verbally"}', NULL,
 '192.168.1.20', '2026-04-04 09:00:00+00');

COMMIT;

# Backend Agent Task — Make the Payroll Feature Production-Ready

You are a **senior backend engineer** on the WorkNest API (Java 21, Spring Boot 3.4.1,
PostgreSQL, Liquibase with `ddl-auto=validate`, Spring Security + JWT multi-tenant,
JUnit 5 / Mockito / Testcontainers / ArchUnit). Read `CLAUDE.md` first and obey it.

This is a **production financial feature. Mistakes are not tolerated.** Money math
must be exact, deterministic, and reproducible from a stored snapshot. Every state
change must be auditable. Tenant and role isolation must be airtight. Do **not**
leave TODOs, placeholders, hallucinated Swagger text, or unreferenced "future"
fields. If a requirement is ambiguous, choose the conservative, legally-safe
interpretation, document it in code comments only where the WHY is non-obvious, and
record the decision in `PAYROLL_REVIEW.md`'s "Decisions" section (append; do not
rewrite existing content).

Companion documents in repo root (read both before coding):
- `PAYROLL_REVIEW.md` — the defect inventory. Every B/I/M item below maps to it.
- `PAYROLL.md` — the current (partly inaccurate) API contract. You will rewrite it.

---

## 0. Hard rules (violating any of these fails the task)

1. **Liquibase only** for schema. New changelog files go in
   `src/main/resources/db/changelog/changes/2026/05/` with the next free numeric
   prefix (current max is `015`; you start at `016-...xml`, `017-...xml`, …).
   `db.changelog-master.yml` uses `includeAll` — do **not** edit it. Use
   `objectQuotingStrategy="QUOTE_ONLY_RESERVED_WORDS"`, `author="codex"`, stable
   `changeSet id`s, `CHECK` constraints for enums/ranges (see `012-payroll.xml`
   for the house style). **Never edit an already-applied changelog.** After
   schema work, JPA entities must match the DB exactly (`ddl-auto=validate` will
   fail the boot otherwise) — verify with `./mvnw -Pliquibase-diff liquibase:diff`
   if available, else by booting the app in a Testcontainer test.
2. **No cross-feature internal imports.** Payroll may depend on `leave`,
   `attendance`, `employee` *repositories/entities* it already uses, but must not
   reach into another feature's `application`/`web` internals. Shared config
   entities live in `domain.entities`.
3. **Tenant scoping:** every query is filtered by `companyId` taken from
   `AuthSessionPrincipal` (`SecurityContextHolder` → `AuthSessionPrincipal`:
   `userId, companyId, roleAssignmentId, role`). Never trust an ID from the
   request body/path for tenant resolution.
4. **API envelope is fixed:** controllers return `ApiResponse.success(message, data)`
   (`com.worknest.common.api.ApiResponse`). Paginated payloads use
   `PaginatedResponse.from(page)` (fields: `items, currentPage, pageSize,
   totalItems, totalPages, hasNext, hasPrevious`). Pagination via
   `PaginationSupport.pageable(page, size)`. Errors via
   `new BusinessException(HttpStatus, "CODE", "message")`.
5. **Security annotation style:** `@PreAuthorize("@companySecurity.hasCurrentCompanyRole('ADMIN','SUPERADMIN')")`.
   Role names: `EMPLOYEE`, `STAFF`, `ADMIN`, `SUPERADMIN`.
6. **Money:** `BigDecimal` everywhere, scale 2 for currency amounts, scale 8 for
   intermediate ratios, `RoundingMode.HALF_UP`, single rounding step at each
   published boundary. No `double`/`float` anywhere in payroll.
7. **Determinism:** a calculated payroll is persisted as an immutable JSON
   snapshot. Locked statuses (`APPROVED`, `FINALIZED`, `PAID`) are **always**
   served from the snapshot, never recomputed. Any input change after
   `CALCULATED` must follow the recalculation rule in B4 below.
8. **Tests are mandatory and must pass:** unit tests for the calculation engine
   (table-driven, covering every branch), integration tests (Testcontainers) for
   lifecycle + authorization + tenant isolation, and ArchUnit must stay green.
   `./mvnw test` must pass. A change without tests is incomplete.
9. **Do not weaken existing passing tests** to make new code pass. If an existing
   test encodes wrong behavior described in `PAYROLL_REVIEW.md`, update it
   deliberately and note why.

---

## 1. Blocker fixes (do these first, in this order)

### B1 — Staff may only see *their assigned* employees; admins see all
- Add a staff-scoped path. The established pattern is
  `EmployeeRepository.findAllAssignedToManager(companyId, PlatformRole.EMPLOYEE,
  principal.roleAssignmentId())` and
  `findAssignedEmployeesByDepartmentAndSupervisor(...)`. Mirror
  `EmployeeQueryServiceImpl` (it already does role-based narrowing using
  `principal.roleAssignmentId()` and rejects access when
  `supervisorRoleAssignment.id != principal.roleAssignmentId()`).
- In `PayrollServiceImpl.listAdminPayrollEmployees`: if caller role is `STAFF`,
  restrict candidates to employees whose `supervisorRoleAssignment.id ==
  principal.roleAssignmentId()`. Add a dedicated paginated repository query
  (do **not** post-filter an unbounded list). `ADMIN`/`SUPERADMIN` keep the
  full company scope.
- In `previewEmployeePayroll` and every `/employees/{employeeId}/...` admin
  endpoint: when caller is `STAFF`, verify the target employee is assigned to
  that staff member; otherwise throw
  `BusinessException(HttpStatus.FORBIDDEN, "PAYROLL_ACCESS_DENIED", ...)`.
- Add an explicit forbidden-path integration test: STAFF A cannot read STAFF B's
  reportee payroll; STAFF cannot list company-wide payroll.
- Keep `MobilePayrollController /details` as self-only (resolve employee by
  `principal.userId()` + `companyId`). Add an ownership guarantee test.

### B2 — Real net salary: income tax + social security + pension
Introduce configurable statutory deductions (see §3 for the settings model).
The calculation engine must compute and return, as **discrete components**:

```
grossEarnings              = basePay + paidLeaveAmount + companyPaidSick + totalBonus
employeeSocialSecurity     = socialSecurityBase  × employeeSocialSecurityRate
employeePensionContribution= pensionBase         × employeePensionRate
taxableIncome              = grossEarnings − employeeSocialSecurity − employeePensionContribution   (configurable: see §3.3 `taxBase`)
incomeTax                  = progressiveTax(taxableIncome, brackets)         // see §3.2
statutoryDeductions        = employeeSocialSecurity + employeePensionContribution + incomeTax
totalDeductions            = statutoryDeductions + manualDeductions + unpaidLeaveDeduction + unpaidSickDeduction
netPay                     = grossEarnings − totalDeductions
employerSocialSecurity     = socialSecurityBase × employerSocialSecurityRate  // employer cost, NOT deducted from net
employerPensionContribution= pensionBase        × employerPensionRate         // employer cost, NOT deducted from net
employerCostTotal          = grossEarnings + employerSocialSecurity + employerPensionContribution
```

- Bases (`socialSecurityBase`, `pensionBase`, `taxBase`) are configurable
  (§3.3). Defaults: social-security & pension base = `grossEarnings`; clamp to
  `[minContributionBase, maxContributionBase]` when configured.
- Progressive tax is bracket-based, **marginal** (tax each slice at its rate),
  ordered, last bracket may have null `upperBound` (open-ended). Deterministic
  rounding (scale 2, HALF_UP) only on the final `incomeTax`.
- If statutory config is missing for a company, **fail loudly is wrong here** —
  instead apply documented system defaults (see §3 defaults) and add a
  `warnings` entry `"Statutory deductions used system defaults; configure
  company payroll settings."` Never silently produce a tax-free payroll without
  a warning.
- Add a new response sub-record `StatutoryDeductionDetails` to
  `PayrollDtos.PayrollCalculationResponse` with every component above plus the
  bracket breakdown (per-bracket: bound range, rate, taxed amount). Add the
  scalar mirrors to `PayrollEmployeeSummaryResponse` (`incomeTax`,
  `employeeSocialSecurity`, `employeePension`, `employerCostTotal`).
- Persist `PayrollResult` columns for the new authoritative totals (migration):
  `income_tax`, `employee_social_security`, `employee_pension`,
  `employer_social_security`, `employer_pension`, `taxable_income`,
  `employer_cost_total` (all `numeric(14,2)`, default 0, not null). Keep them in
  sync with the snapshot (see I4).

### B3 — Currency from company, not hardcoded
- Remove `PayrollCalculationEngine.CURRENCY = "EUR"`. Source currency from
  `employee.getCompany().getCurrency()` (e.g. `"ALL"`). Thread it through
  `PayrollContext` (add `String currency`) and stamp it into every response and
  snapshot.
- Audit `PayrollResultRepository.convertAmountsByCompanyId` /
  `EmployeeRepository.convertSalariesByCompanyId`: after currency conversion the
  stored snapshots still carry the old currency label and old amounts. Add a
  migration-safe path: on currency conversion, **either** re-stamp non-locked
  snapshots' currency + scale amounts, **or** explicitly document that locked
  historical payrolls retain their original currency (recommended: keep
  historical snapshots immutable, only convert non-locked). Implement the
  recommended behavior and unit-test it.
- All money in a single response shares one currency. Add an assertion/test.

### B4 — No silent loss of post-CALCULATE changes
Adopt this rule and implement it exactly:
- While status is `CALCULATED` (or `DRAFT`), the persisted `PayrollResult`
  snapshot + total columns must always reflect the latest inputs.
- Therefore: `addBonus` / `addDeduction` (and any leave approval/cancellation
  that overlaps a `CALCULATED` period) must **trigger a recalculation** of that
  employee/period and re-persist the snapshot + totals atomically in the same
  transaction. Implement a single private
  `recalculateAndPersist(companyId, employee, actor, year, month)` and call it
  from `addAdjustment` after the row is saved, and from the leave
  approve/cancel paths (see I-leave below) when the affected month is in a
  non-locked payroll state.
- `approvePayroll` must **reject** if the live recomputation differs from the
  stored snapshot (guard against approving stale numbers): recompute, compare
  component-by-component; if different, refresh snapshot and return
  `409 PAYROLL_RECALCULATION_REQUIRED` with a message telling the admin to
  re-review, OR auto-refresh then proceed — choose auto-refresh-then-proceed and
  record an audit entry `PAYROLL_REFRESHED_ON_APPROVE`. Locked→snapshot rule
  still applies after APPROVED.
- Integration test: calculate → add bonus → approve → assert approved snapshot
  includes the bonus; calculate → approve leave overlapping the month → assert
  recompute happened while CALCULATED and was blocked once APPROVED.

### B5 — One authoritative leave-allowance model
The leave feature (`LeaveBalance`, per `LeaveType`, calendar days, decremented
on cancel) and the payroll engine (single pooled `leaveDaysPerYear`, working
days, lumps VACATION+PERSONAL+MATERNITY+PATERNITY+OTHER) disagree. Fix:
- **Payroll consumes `LeaveBalance` as the source of truth** for paid allowance.
  For each approved leave overlapping the period, classify by `LeaveType`:
  - `VACATION` (and `PERSONAL` if product treats it as paid time off — confirm
    via the Decisions section; default: PERSONAL paid from its own balance):
    paid up to the **remaining balance for that type/year** from
    `LeaveBalance`; the **excess** becomes an unpaid salary deduction.
  - `UNPAID`: always an unpaid deduction.
  - `SICK`: handled by `CompanySickLeavePolicy` only — never charged to the
    vacation pool.
  - `MATERNITY` / `PATERNITY`: **statutory leave** — must **not** consume the
    vacation pool and must **not** become an unpaid salary deduction. Treat as a
    separate tracked category: by default state/insurance-covered (paid amount =
    documented policy, employer cost = 0 unless configured). Add a config flag
    in §3 for employer top-up percentage (default 0).
  - `OTHER`: configurable; default treat as paid from its own `LeaveBalance`
    pool, excess unpaid. Document the choice.
- Day counting must use **one** unit consistently. Decision: count **working
  days** (per the holiday/weekend rules in §2), and make `LeaveBalance`
  accrual/decrement use the **same** working-day basis so the employee-facing
  balance and payroll reconcile exactly. If changing leave accrual is
  out-of-scope, instead expose a reconciliation note in the response and
  document the residual divergence — but the **preferred** solution is to unify
  on working days. Pick one, implement fully, test the reconciliation.
- Honor `LeaveRequest.daysCount` for fractional (half) days if and only if
  half-day input is implemented end-to-end (see I8). Until then, document that
  partial days are rounded to full working days and make engine + leave
  submission agree.
- Remove the now-dead `isAllowanceLeave` lumping logic and the
  `leaveTreatment`/`SICK_LEAVE_PLACEHOLDER_POLICY` strings; replace with an
  explicit per-type classification enum surfaced in
  `PayrollLeaveRecordDetails.payrollTreatment`
  (`PAID_FROM_BALANCE`, `UNPAID_EXCESS`, `UNPAID_EXPLICIT`,
  `SICK_COMPANY_POLICY`, `STATUTORY_MATERNITY`, `STATUTORY_PATERNITY`).

### B6 — Truthful API contract
- Fix the `complete-payment` `@Operation` summary in `AdminPayrollController`:
  it currently claims "Adds missing-hours compensation … and clears applied
  adjustments" which the code does not do. State exactly what it does:
  "Transition FINALIZED → PAID and freeze the payslip snapshot."
- Audit every `@Operation` summary in both payroll controllers against actual
  behavior. No aspirational text.
- Rewrite `PAYROLL.md` from scratch to match the final implementation (every
  endpoint, request/response schema, enum, error code, lifecycle, formula). It
  is the single source of truth the frontend agent will consume.

---

## 2. New feature — Holidays & Work-Week settings (company-level)

### 2.1 Public holiday calendar
- New entity `PublicHoliday` + table `company_public_holidays`
  (migration `016-...`):
  `id uuid pk`, `company_id uuid fk companies(id) not null`,
  `holiday_date date not null`, `name varchar(150) not null`,
  `recurring boolean not null default false` (true ⇒ same month/day every year),
  `paid boolean not null default true` (paid holiday ⇒ counts as a paid
  non-working day; unpaid ⇒ excluded working day but not paid),
  audit columns (`created_at`, `updated_at`, `created_by_user_id` nullable),
  `version bigint not null`.
  Unique constraint `(company_id, holiday_date)`; index `(company_id,
  holiday_date)`.
- Repository: `PublicHolidayRepository` with
  `findAllByCompanyIdAndHolidayDateBetween(...)` and a recurring-aware resolver
  (expand recurring entries for the requested year).
- Endpoints under `AdminPayrollSettingsController`
  (`/api/v1/admin/payroll/settings/holidays`), `ADMIN,SUPERADMIN` only:
  - `GET ?year=` — list resolved holidays for the year (recurring expanded).
  - `POST` — create `{date, name, recurring, paid}`.
  - `PUT /{id}` — update.
  - `DELETE /{id}` — delete (block if any **locked** payroll period already
    consumed it; otherwise allow and document that non-locked periods will
    recompute on next calculate).
- Integrate into `PayrollDateUtils`: replace the static
  `isWorkingDay`/`countWorkingDays` with a company-aware
  `WorkingDayCalculator(companyId, year)` that excludes weekends (per 2.2) and
  unpaid holidays from working days, and treats paid holidays as paid
  non-working days (employee is paid, no leave consumed, no attendance expected).
  Every place that currently calls `PayrollDateUtils.countWorkingDays` /
  `isWorkingDay` (engine base-pay proration, leave overlap counting, sick
  policy, hourly full-payable-hours, absence reporting) must use the calculator.

### 2.2 Work-week / weekend configuration
- Add to the company payroll settings entity (§3.1) a `weekend_days` column
  storing a set of `java.time.DayOfWeek` names (JSONB or a small join table;
  prefer JSONB `jsonb` like `AttendanceDayRecord.warningFlagsJson` pattern,
  default `["SATURDAY","SUNDAY"]`). The `WorkingDayCalculator` derives
  non-working weekdays from this set. Validate: at least one working day must
  remain; reject an all-weekend configuration with
  `400 INVALID_WORKWEEK_CONFIG`.

---

## 3. New feature — Statutory settings (taxes, social security, pension, sick)

All settings are **per company**, edited by `ADMIN,SUPERADMIN`, consumed by the
engine, and **immutable in already-locked payroll snapshots** (locked payrolls
keep the rates that were in effect at calculation time — snapshots already
capture the resolved numbers, so just never recompute locked).

### 3.1 `CompanyPayrollSettings` (one row per company)
Entity + table `company_payroll_settings` (migration `017-...`):
- `id`, `company_id` unique fk, `version`, audit cols.
- `default_daily_working_hours numeric(4,1) not null default 8.0` — replaces the
  hardcoded `DEFAULT_DAILY_HOURS=8` in the engine **as a fallback only** when
  `Employee.dailyWorkingHours` is null. The engine must use **one** daily-hours
  source consistently everywhere (fixes I2): `employee.dailyWorkingHours`
  if set, else this company default. Remove the divergent
  `context.defaultDailyWorkingHours()`-vs-`employee.dailyWorkingHours` split in
  `AttendanceWorkHoursProvider` fallback, `calculateHourlyAttendancePayment`,
  `dailyPayValue`, and `CompanySickLeavePolicy`.
- `weekend_days jsonb not null default '["SATURDAY","SUNDAY"]'`.
- `tax_enabled boolean not null default true`.
- `tax_base varchar(20) not null default 'GROSS_MINUS_CONTRIBUTIONS'`
  (enum `TaxBase`: `GROSS`, `GROSS_MINUS_CONTRIBUTIONS`).
- `social_security_employee_rate numeric(6,3) not null default 0`
  (percentage, e.g. `11.200` = 11.2%).
- `social_security_employer_rate numeric(6,3) not null default 0`.
- `pension_employee_rate numeric(6,3) not null default 0`.
- `pension_employer_rate numeric(6,3) not null default 0`.
- `contribution_min_base numeric(14,2)` nullable.
- `contribution_max_base numeric(14,2)` nullable.
- `maternity_employer_topup_rate numeric(6,3) not null default 0`.
- `paternity_employer_topup_rate numeric(6,3) not null default 0`.
- CHECK constraints: all rates in `[0, 100]`; `contribution_max_base >=
  contribution_min_base` when both set.

### 3.2 `CompanyTaxBracket` (progressive, marginal)
Entity + table `company_tax_brackets` (same migration):
- `id`, `company_id` fk, `ordinal int not null` (0-based, ascending),
  `lower_bound numeric(14,2) not null` (inclusive),
  `upper_bound numeric(14,2)` nullable (exclusive; null ⇒ open-ended top
  bracket), `rate numeric(6,3) not null` (percentage), `version`, audit.
- Unique `(company_id, ordinal)`. CHECK `rate between 0 and 100`,
  `lower_bound >= 0`, `upper_bound is null or upper_bound > lower_bound`.
- Service must validate on upsert that brackets are **contiguous, ordered, and
  non-overlapping**, start at `0`, and exactly one open-ended top bracket.
  Reject otherwise with `400 INVALID_TAX_BRACKETS` and a precise message.
- `progressiveTax(taxable, brackets)`: sum over brackets of
  `max(0, min(taxable, upper) − lower) × rate/100`; final scale-2 HALF_UP.
- System default when none configured: a single bracket `[0, null) @ 0%` plus a
  warning (never tax silently at a guessed rate).

### 3.3 Settings API
`AdminPayrollSettingsController` (`/api/v1/admin/payroll/settings`),
`ADMIN,SUPERADMIN`:
- `GET /` → full `PayrollSettingsResponse` (work-week, daily hours, tax
  enabled/base, contribution rates/bounds, maternity/paternity top-ups,
  sick-leave policy summary, plus `isDefault` flags where defaults are applied).
- `PUT /` → upsert all scalar settings (validated).
- `GET /tax-brackets` / `PUT /tax-brackets` (replace-all, validated contiguous).
- Holidays endpoints from §2.1.
- Keep the existing `/api/v1/admin/payroll/sick-leave-policy` endpoints working;
  optionally fold them into the settings response but **do not break** the
  existing path/contract without updating `PAYROLL.md` and frontend prompt.

Mirror the established settings pattern: company-scoped config entity →
`findByCompanyId` repository → upsert service that creates-or-updates →
`@Transactional` service, `@Transactional(readOnly=true)` getters → thin
controller returning `ApiResponse`. (Reference: `CompanyAttendancePolicyServiceImpl`,
`PayrollServiceImpl.upsertSickLeavePolicy`.)

---

## 4. Important correctness fixes

- **I1 — Monthly absence reporting (rule #3).** For `FIXED_MONTHLY`, do **not**
  change pay, but read attendance and report an `AbsenceDetails` block:
  expected working minutes (working days × resolved daily hours),
  attended minutes, absent minutes, and the **monetary equivalent** of the
  absence at the daily rate (informational only, `applied=false`). Add to
  `PayrollCalculationResponse` and to the summary row. Unit-test that monthly
  net pay is unchanged while the block is populated.
- **I2 — Single daily-hours source** (covered in §3.1). Add a test proving the
  hourly no-attendance fallback and `fullPayableHours` use the same daily-hours
  basis (no more overpay/clamp-to-zero artifact).
- **I3 — Components must reconcile to `netPay`.** Every figure in the response
  carries an explicit `applied` boolean (or is split into
  `appliedComponents` vs `informationalComponents`). The sum of applied
  components must equal `netPay` exactly. Add an engine self-check that throws
  `PayrollCalculationException("PAYROLL_RECONCILIATION_FAILED", ...)` if
  `Σ applied != netPay` (defensive; should never fire — proves the math).
  Add a test asserting reconciliation for monthly, hourly, with leave, sick,
  bonus, deduction, tax, negative-net.
- **I4 — `PayrollResult` columns never stale.** Persist all authoritative
  totals (existing + new from B2) atomically with the snapshot in
  `persistResult`/`recalculateAndPersist`. Lifecycle transitions that only
  patch JSON status must also keep columns consistent (they don't change
  amounts, but assert equality in a test).
- **I5 — Holidays/weekends** (covered in §2).
- **I6 — Batch path.** Replace `loadBatchEmployees`'
  `findAllByCompanyId` with the period/contract-filtered query. Fix
  `EmployeeRepository.findPayrollCandidates` null-list handling: split into two
  methods (`findPayrollCandidates(companyId, periodStart, periodEnd)` and
  `findPayrollCandidatesByIds(companyId, ids, periodStart, periodEnd)`) instead
  of the fragile `(:employeeIds IS NULL OR ...)`. Exclude employees whose
  `employmentStatus` is `PENDING` (never started) from batch and the admin
  table unless they have a payable contract overlap **and** a non-PENDING
  status — confirm intended population in Decisions. Delete genuinely dead
  query methods.
- **I7 — Audit logging.** Use the existing audit mechanism (there is an
  `AuditService` used by auth/superadmin — locate and reuse the company-level
  one; do not invent a parallel system). Emit an audit event for: calculate,
  recalculate, addBonus, addDeduction, approve, finalize, complete-payment,
  every revert, sick-policy upsert, settings upsert, tax-bracket replace,
  holiday create/update/delete. Include actor `userId`, `companyId`,
  `employeeId` (where applicable), period, from→to status, and a compact
  amount summary. If no reusable company audit service exists, add a minimal
  `payroll_audit_log` table + writer following the SuperAdmin audit pattern, and
  say so in Decisions.
- **I8 — Half-day leave.** Either implement half-day end-to-end (request DTO
  accepts a half-day flag/duration, `daysCount` reflects 0.5, engine honors
  `daysCount` instead of counting whole `LocalDate`s) **or** remove the
  half-day claim from `LeaveRequest.daysCount`'s comment and make everything
  whole-day-consistent. Pick one, implement fully, test.
- **I9 — Payslip PDF.** Add `GET /api/v1/mobile/payroll/payslip?year=&month=`
  (self, `EMPLOYEE,STAFF,ADMIN,SUPERADMIN`) and
  `GET /api/v1/admin/payroll/employees/{employeeId}/payslip?year=&month=`
  (`STAFF` scoped per B1, `ADMIN,SUPERADMIN`) returning
  `application/pdf` (`Content-Disposition: attachment; filename=...`).
  - Render from an HTML template (formal, branded) → PDF. Add the dependency
    (recommend **openhtmltopdf**; pin a stable version in `pom.xml`, justify in
    Decisions). No new dependency without justification.
  - The payslip must include: company block (name, **`Company.nipt`** as
    Tax/VAT ID, address/contact available on `Company`, logo if present),
    employee block, period, payment method, the full component breakdown
    (gross, each statutory deduction with bracket detail, leave/sick treatment,
    bonuses/deductions, net), currency from company, generated-at timestamp,
    and the payroll status. Only allow PDF for `CALCULATED`+ (not `DRAFT`
    preview) — return `409 PAYROLL_NOT_CALCULATED` otherwise.
  - Generate from the **snapshot** for locked statuses; from the live
    recomputation for `CALCULATED`.

---

## 5. Minor fixes (do not skip — they are part of "production ready")

- **M1/M2 — Lifecycle hygiene.** `DRAFT` is never persisted (only a preview
  marker) and `PayrollStatus` includes an unused `CANCELLED`. Decide and
  implement: keep `DRAFT` as preview-only (document it), and either give
  `CANCELLED` real semantics (e.g. void a calculated payroll, reversible only
  by recalculate) **or** remove it from the enum + DB CHECK. Give `FINALIZED`
  distinct meaning (it currently has zero side effects): make `FINALIZE` the
  step that freezes/locks the payslip artifact and forbids any further
  adjustment/recalculation (APPROVED may still be reverted+recalculated;
  FINALIZED may not, only revert-finalization). Document the final state
  machine precisely in `PAYROLL.md`.
- **M3 — Negative net.** Block `approve` when `netPay < 0` unless the request
  carries an explicit `allowNegativeNet=true` (admin override), audited.
- **M4 — Validate `dailyWorkingHours`.** Enforce non-null positive
  `dailyWorkingHours` for `HOURLY` employees at employee create/update (in the
  employee feature's existing validation path — coordinate, do not duplicate).
  Engine still has the company-default fallback but must warn if it had to use
  it for an hourly employee.
- **M5 — Centralize sick-policy defaults.** One constant/config source for
  `70% / 14 days`; remove the triple duplication.
- **M6 — Remove dead `PlaceholderSickLeavePolicy`** (unreachable under
  `@Primary CompanySickLeavePolicy`) and its dead warning branch, or wire a
  real "not configured" signal. No dead code.
- **M7 — Batch transaction intent.** Make the per-employee calculation a
  separately-proxied `@Transactional(propagation = REQUIRES_NEW)` unit so one
  employee's failure cannot poison the batch — make the isolation explicit, not
  incidental via self-invocation. Test: one bad employee, others still persist.
- **M8 — Reconcile leave "used before" with `LeaveBalance`** (folds into B5).
- **M9 — Self-service performance.** For locked statuses serve the snapshot
  (already required); for `CALCULATED` it is acceptable to recompute, but cache
  within the request — do not issue duplicate leave/adjustment queries.

---

## 6. Deliverables & acceptance criteria

1. All Liquibase changelogs added (`016+`), entities aligned, app boots with
   `ddl-auto=validate` (prove via a Testcontainer boot test).
2. Engine produces reconciling, deterministic output; snapshot round-trips
   (serialize → deserialize → equal) including all new fields and null-safe for
   old snapshots.
3. Authorization matrix enforced and tested: EMPLOYEE(self) / STAFF(assigned
   only) / ADMIN(all) / SUPERADMIN(all); cross-tenant denied.
4. New settings + holidays + tax-brackets CRUD working, validated, audited.
5. Net salary = gross − (tax + employee SSC + pension + manual + unpaid),
   employer cost reported separately; bracket math unit-tested with at least:
   below-first-bracket, mid-bracket, top open-ended, zero-tax-config,
   min/max-base clamping.
6. Payslip PDF endpoints return a correct, branded document with company NIPT
   and full breakdown; locked from snapshot, calculated live.
7. `PAYROLL.md` fully rewritten and accurate; `@Operation` summaries truthful.
8. `./mvnw test` green (unit + Testcontainers integration + ArchUnit). No
   skipped/ignored tests. No `TODO`/`FIXME`/placeholder strings left in payroll.
9. Append a dated "Decisions" section to `PAYROLL_REVIEW.md` recording every
   ambiguity you resolved (PERSONAL/OTHER leave treatment, currency-conversion
   of snapshots, CANCELLED semantics, audit mechanism chosen, PDF library).

## 7. Suggested implementation order

1. Settings + holidays schema/entities/repos/CRUD (foundation for the engine).
2. `WorkingDayCalculator` (weekends + holidays) and wire everywhere (I5/I2).
3. Leave model unification (B5/M8/I8).
4. Statutory deductions in engine + new totals + reconciliation self-check
   (B2/I3).
5. Currency from company (B3).
6. Recalculation rule + lifecycle hardening (B4/M1/M2/M3/M7).
7. Authorization (B1) + audit (I7).
8. Payslip PDF (I9).
9. Truthful contract + `PAYROLL.md` rewrite (B6) + Decisions.
10. Full test pass, `liquibase:diff`, ArchUnit, cleanup (M4/M5/M6/I6).

Work in small, compiling, tested increments. Do not open a PR or create a branch
unless explicitly instructed (`CLAUDE.md` rule). Ask before any `git commit`.

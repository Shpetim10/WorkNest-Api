# Payroll Feature — Architecture & Business-Logic Review

Reviewer: senior backend engineer pass over `features/payroll` and its coupling to
`leave`, `attendance`, `employee`. Scope: correctness, cross-feature coherence,
tenant/role isolation, persistence, and the business rules supplied by the product owner.

Verdict: the lifecycle/state plumbing is solid, tenant (company) isolation is sound,
but the **calculation domain model is incoherent across features**, several documented
behaviors are hallucinated, and three explicit business rules (staff scoping, payslip
PDF, true net salary) are simply not implemented.

---

## 1. Blockers

### B1. Staff role sees every employee's salary (rules #5/#6 violated, authorization hole)
`AdminPayrollController.listEmployees`, `/employees/{id}/details`, and
`GET /employees/{id}/calculate` are all `@PreAuthorize(... 'STAFF', 'ADMIN', 'SUPERADMIN')`.
`PayrollServiceImpl.listAdminPayrollEmployees` → `findPayrollCandidatesForAdmin(companyId, …)`
has **no supervisor/assignment filter**. Any STAFF user can read the full payroll —
monthly salary, hourly rate, net pay — of every employee in the company, and can pull
any individual's payroll detail by ID.

Required: rule #5 — staff sees payroll of *only their assigned employees*; rule #6 —
only admin sees all. `EmployeeRepository.findAllAssignedToManager(...)` already exists
and is the scoping primitive. Needs:
- a staff-scoped candidate query (filter by `supervisorRoleAssignment` of the caller),
- an ownership check in `previewEmployeePayroll`/detail endpoints when caller is STAFF,
- separation of the `STAFF` vs `ADMIN` controller surface.

### B2. "Net pay" is not net — no tax / social security / pension
`PayrollCalculationEngine`: `netPay = grossEarnings − totalDeductions`, where
`totalDeductions` is only manual deductions (+ unpaid-leave/sick for monthly).
There is **no income tax, no employee social-security, no pension** withholding.
The field is labelled `netPay` and persisted/served as such — it is actually
gross-minus-unpaid-leave. This directly contradicts the stated requirement to
integrate taxes to compute net salary, and is a financial-correctness blocker:
downstream consumers (payslip, reports, currency conversion) treat it as net.

`SickLeaveCalculationDetails.insuranceCoveredAmount` is always `null`,
`insuranceCoveredDays` is set but never valued — social-security/insurance is a
stubbed shape with no logic.

### B3. Hardcoded `CURRENCY = "EUR"` while companies run other currencies
`Company.currency` defaults to `"ALL"` (country `AL`, NIPT tax id, locale `SQ`).
The engine hardcodes `private static final String CURRENCY = "EUR"` and stamps it
into every response and snapshot. There is an admin currency-conversion feature
(`EmployeeRepository.convertSalariesByCompanyId`,
`PayrollResultRepository.convertAmountsByCompanyId`) that rescales amounts but the
payroll snapshot still says EUR. Amounts and currency label are inconsistent →
wrong payslips and wrong reporting. Currency must come from `Company.currency`.

### B4. Adjustments/leave added after CALCULATE are silently lost on approval
Flow: `calculateEmployeePayroll` persists a snapshot. `addBonus`/`addDeduction`
only inserts a `PayrollAdjustment` row — it does **not** recompute or refresh
`PayrollResult` totals/snapshot (`ensurePayrollOpen` permits it in CALCULATED).
`approvePayroll` does **not** recalculate; it freezes the *existing* (now stale)
snapshot and patches only `payrollStatus`. Net effect: a bonus/deduction (or a
newly-approved leave, same mechanism) entered after the last calculate but before
approve is **dropped from the approved/paid payroll** with no warning. Either
adjustments must mutate the snapshot, or approve must force a fresh recalculation,
or post-calculate mutation must be blocked. This is a money-correctness defect.

### B5. Dual, divergent leave-allowance models (core cross-feature incoherence)
Two independent accounts of "paid days off" that do not agree:

- **Leave feature** (`LeaveServiceImpl` + `LeaveBalance`): per-`LeaveType` yearly
  pool. `defaultDays`: VACATION/MATERNITY/PATERNITY/OTHER = `leaveDaysPerYear`
  (fallback 20), SICK=10, PERSONAL=5, UNPAID=0. `usedDays` accrues **calendar
  days** (`ChronoUnit.DAYS.between+1`), decremented on cancel. This is what the
  employee sees as their balance.
- **Payroll engine** (`calculateLeave`): ignores `LeaveBalance` entirely,
  re-derives a *single* annual pool = `employee.leaveDaysPerYear`, and lumps
  **VACATION + PERSONAL + MATERNITY + PATERNITY + OTHER** into it
  (`isAllowanceLeave` = "not SICK and not UNPAID"), counted in **working days**.

Consequences:
- PERSONAL has its own 5-day balance in the leave feature but is charged against
  the annual vacation pool in payroll → an employee within their PERSONAL balance
  is still docked as unpaid by payroll once the combined total passes
  `leaveDaysPerYear`.
- MATERNITY/PATERNITY consume the annual paid-vacation pool and then become
  *unpaid salary deductions* once exhausted — statutory leave treated as unpaid
  absence. Almost certainly wrong and a legal exposure.
- Calendar-day vs working-day bases differ, so the balance the employee sees and
  the days payroll charges will not reconcile.

Rule #1 ("paid days off, distinct from sick leave; deduct only the excess") needs
**one** authoritative model. Recommendation: payroll consumes `LeaveBalance`
(remaining paid allowance) as the source of truth and only the *excess over
remaining balance* + explicit `UNPAID` becomes a deduction; classify
MATERNITY/PATERNITY/SICK on their own statutory tracks, not the vacation pool.

### B6. Hallucinated API contract — `complete-payment` Swagger
`@Operation` on `complete-payment` states: *"Adds missing-hours compensation for
hourly employees and clears applied adjustments."* The implementation
(`completePayment`) only does `FINALIZED → PAID` + a JSON status patch. No
missing-hours compensation, no adjustment clearing. The persisted memory even
records "missing-hours removed". This is a false contract shipped to API
consumers and must be corrected (doc fix, or implement the promised behavior —
clarify which is intended).

---

## 2. Important suggestions

### I1. Monthly employees: absence in hours/money is never reported (rule #3 unmet)
Rule #3: monthly pay is unaffected by attendance, but the detail must still state
hours/minutes of absence and the money equivalent. Today, for `FIXED_MONTHLY`,
`AttendanceWorkHoursProvider` short-circuits to default hours and
`hourlyAttendancePayment` is `null`. Attendance is never read for monthly staff,
so the response carries **zero** absence reporting for them. Need an
informational absence block (expected vs attended minutes, monetary equivalent at
daily rate) for monthly employees without altering their pay.

### I2. Inconsistent "daily hours" basis produces wrong hourly deductions
`AttendanceWorkHoursProvider` fallback uses `context.defaultDailyWorkingHours()`
(hardcoded 8), but `calculateHourlyAttendancePayment`, `dailyPayValue`, and
`CompanySickLeavePolicy` use `employee.getDailyWorkingHours()`. For an hourly
employee with `dailyWorkingHours = 6` and no attendance records: fallback hours =
days×8 (→ `paymentReceived` = rate×days×8) while `fullPayableHours` = days×6 (→
`fullPayment` = rate×days×6). `attendanceDeduction = max(full − received, 0)` then
clamps a genuinely inconsistent state to 0 and the employee is paid for more than
"full". `defaultDailyWorkingHours` on `WorkPeriodDetails` is likewise hardcoded 8
and ignores the employee's configured value. Pick one daily-hours source
(employee value with documented fallback) and use it everywhere.

### I3. Reported figures don't tie out to `netPay` (rule #4 — "show how it's calculated")
For HOURLY, the engine intentionally does **not** apply
`leaveCalculation.unpaidLeaveDeduction`, `sickLeave.paidSickLeaveDeductionEquivalent`,
`sickLeave.unpaidSickLeaveUnpaidAmount` (attendance already excludes those hours),
yet all are populated in the response as non-zero deductions. A "comprehensive"
detail page that sums the displayed components will not equal `netPay`. Either
zero/flag the not-applied components for hourly, or add an explicit
"applied vs informational" marker per line so the frontend can render a reconciling
breakdown.

### I4. `PayrollResult` total columns go stale
`addAdjustment` never refreshes `PayrollResult.grossEarnings/netPay/...`. The
admin list recomputes live for non-locked statuses, but the persisted columns
(consumed by `convertAmountsByCompanyId` and any direct reporting/export) drift
from reality until a re-calculate. Tie the columns to the snapshot, or treat the
columns as authoritative and keep them in sync on every mutation.

### I5. Public holidays not modeled (affects pay, leave, absence)
`PayrollDateUtils.isWorkingDay` = Mon–Fri only. No company holiday calendar; the
weekend is hardcoded Sat/Sun (not configurable per company/country, despite
multi-country `countryCode`). This skews base-pay proration, leave-day counting,
and absence-hour math. A `CompanyHolidayCalendar` (date set per company/year)
should feed `countWorkingDays`/`isWorkingDay`. Currently only surfaced as a
warning string.

### I6. Batch path ignores the purpose-built query; dead/fragile code
`loadBatchEmployees` uses `findAllByCompanyId` (no period/contract/status filter)
and relies on pre-loop guards. The tailored `findPayrollCandidates`
(period+contract filtered, `employeeIds` aware) is **never called** — dead code,
and its `(:employeeIds IS NULL OR e.id IN :employeeIds)` is the known
JPA-null-list pitfall (will fail/behave oddly with a null list param). Either
adopt `findPayrollCandidates` with the param-handling fixed, or delete it.
Separately, `findPayrollCandidatesForAdmin` has no `employmentStatus` filter, so
PENDING/TERMINATED employees appear in the payroll table and get calculated
(engine only warns). Decide intended population and filter accordingly.

### I7. No audit trail on financial state changes (CLAUDE.md guardrail)
An audit infrastructure exists (`AuditService` used by auth, SuperAdmin audit log).
Payroll records `calculatedByUser`, but **no actor/timestamp/audit entry** is
written for approve/finalize/complete-payment/revert-* or for bonus/deduction or
sick-policy upserts. These are exactly the "state changes [that] must be captured
via audit logging" called out in project guardrails. Add audit events with actor,
from→to status, period, and amounts.

### I8. Half-day leave is claimed but not real
`LeaveRequest.daysCount` is `precision=6, scale=1` and commented "Supports
half-days (e.g. 0.5)", but `submitRequest` always computes whole calendar days and
payroll counts whole `LocalDate`s via a `Set` (ignoring `daysCount` entirely). A
0.5-day leave is charged as a full day in payroll. Either implement half-day input
end-to-end and have payroll honor `daysCount`, or drop the half-day claim.

### I9. Payslip PDF not implemented (rule #7)
No PDF/HTML rendering, no payslip endpoint, no template, and no company
identity (name, **NIPT/VAT** — `Company.nipt` exists and is unused — address,
logo) in any payroll response. Rule #7 (downloadable, formal, company-branded
payslip for employee/staff) is entirely absent. Needs: an HTML→PDF renderer
(e.g. openhtmltopdf/flying-saucer — no such dependency present yet), a
company-header block in the response/template, and a secured
`GET …/payslip` endpoint with the same role/ownership scoping as B1.

---

## 3. Minor suggestions

- **M1. `DRAFT` is a phantom state.** `PayrollResult` defaults to DRAFT but
  `persistResult` always writes CALCULATED; DRAFT is only an in-memory preview
  marker. Per rule #8 ("no unnecessary states"), drop DRAFT from the persisted
  enum or document it as preview-only.
- **M2. `FINALIZED` has no distinct behavior.** APPROVED locks attendance; PAID is
  terminal; FINALIZED carries zero side effects (no payslip emission, no extra
  lock). Either give it meaning (e.g. generate/lock the payslip, freeze
  adjustments definitively) or collapse APPROVED↔FINALIZED (rule #8).
- **M3. Negative net pay only warns.** Lifecycle proceeds to PAID with a negative
  `netPay`. Consider blocking approve/finalize on negative net unless explicitly
  overridden.
- **M4. `dailyWorkingHours` not enforced for HOURLY.** Null → silent fallback to 8,
  distorting full-payable-hours and sick valuation. Validate at employee
  create/update for HOURLY (and ideally for monthly when used in sick math).
- **M5. Sick-policy default duplicated.** `70.00 / 14` lives in
  `PayrollServiceImpl.getSickLeavePolicy`, `CompanySickLeavePolicy`, and the
  `PlaceholderSickLeavePolicy` warning path. Centralize one constant/config.
- **M6. `PlaceholderSickLeavePolicy` is unreachable.** `CompanySickLeavePolicy` is
  `@Primary` and always returns a value (default config when none stored), so the
  placeholder + its warning branch in the engine (`PlaceholderSickLeavePolicy.STATUS`)
  is dead. Remove, or wire a real "policy not configured" path.
- **M7. Self-invocation transaction.** `calculateBatch` calls
  `calculateEmployeePayroll` on `this`, so the inner `@Transactional` is bypassed
  and per-employee failures don't roll back the batch — correct outcome, but by
  accident. Make the per-employee unit an explicitly separate transactional bean
  / `REQUIRES_NEW` so the intent is enforced, not incidental.
- **M8. `usedBefore` vs `LeaveBalance.usedDays` mismatch.** Payroll's YTD
  pre-period usage is recomputed from approved leave rows in working days; the
  leave feature tracks calendar days on the balance. Cross-month-spanning leave
  splits will not reconcile between the two screens. Folds into B5's single-model
  fix.
- **M9. `previewCurrentEmployeePayroll` always recomputes for non-locked.** Cheap
  now, but for self-service at scale consider serving the CALCULATED snapshot
  rather than recomputing leave/adjustment queries on every mobile hit.

---

## 4. Cross-feature coherence summary

| Trigger in another feature | Should affect payroll | Current behavior |
|---|---|---|
| Approve paid leave within allowance | No deduction | OK if before calculate; **lost if after calculate** (B4) |
| Approve leave exceeding allowance | Deduct excess | Partially — wrong allowance model (B5), wrong types pooled |
| Approve SICK leave | Company% paid, rest state/unpaid | Implemented for monthly; hourly numbers reported but not applied (I3); insurance amount stubbed (B2) |
| Cancel approved leave | Recompute payroll | Blocked when payroll locked (good); silent staleness when only CALCULATED (B4) |
| Hourly employee attendance | Pay = attended hours | OK, but daily-hours basis inconsistent (I2); no-records fallback can overpay |
| Monthly employee absence | Report hours/money, no pay change | **Not reported at all** (I1) |
| Public holiday in period | Excluded from working days | **Not modeled** (I5) |
| Company currency configured | Payroll uses it | **Hardcoded EUR** (B3) |
| Employee/staff requests payslip | Scoped, downloadable PDF | **Not implemented** (B1 scoping, I9 PDF) |

---

## 5. Recommended remediation order

1. B1 (staff scoping) and B3 (currency) — security + correctness, low blast radius.
2. B5 (single leave-allowance model) — unblocks I3/M8 and rule #1 correctness.
3. B4 (snapshot/adjustment consistency) — money correctness.
4. B2 (tax/SSC/net) + I9 (payslip with company NIPT) — required features.
5. I1/I2/I5 (absence reporting, daily-hours, holidays) — calculation fidelity.
6. I7 (audit), B6/M-series (contract truthfulness, state cleanup) — hardening.

No code changes were made; this is review output only.

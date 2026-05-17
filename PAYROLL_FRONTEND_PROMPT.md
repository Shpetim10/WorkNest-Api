# Frontend Agent Task — Integrate the Payroll Feature End-to-End

You are a **senior frontend engineer** integrating the WorkNest payroll feature.
This is a **production financial UI. Mistakes are not tolerated.** Money must
never be miscomputed client-side, never displayed without its currency, never
shown to a user who is not authorized to see it. The backend is authoritative for
**all** money math — the frontend **never** computes pay, tax, or totals; it only
renders what the API returns.

## 0. Source of truth & ground rules

1. The backend contract is being finalized in parallel. The authoritative,
   regenerated contract will live in repo-root **`PAYROLL.md`**. **Always read
   the latest `PAYROLL.md` before implementing.** This document describes the
   intended final shape; if `PAYROLL.md` and this document disagree, **`PAYROLL.md`
   wins** — flag the discrepancy, do not guess.
2. Every response is wrapped:
   ```ts
   interface ApiResponse<T> { success: boolean; message: string; data: T; timestamp: string }
   ```
   Paginated payloads:
   ```ts
   interface PaginatedResponse<T> {
     items: T[]; currentPage: number; pageSize: number;
     totalItems: number; totalPages: number; hasNext: boolean; hasPrevious: boolean;
   }
   ```
   Always read `data`. On `success:false` or HTTP ≥ 400, surface
   `message` and handle the `code` (see §7 error codes). Never render `data`
   when `success` is false.
3. **Authorization is enforced server-side; the UI must mirror it, never
   substitute for it.** Roles: `EMPLOYEE`, `STAFF`, `ADMIN`, `SUPERADMIN`.
   - `EMPLOYEE`: only their own payslip (mobile self endpoint).
   - `STAFF`: only employees assigned to them (server filters; UI must not offer
     navigation to non-assigned employees and must handle
     `PAYROLL_ACCESS_DENIED` gracefully).
   - `ADMIN`/`SUPERADMIN`: all employees + all settings.
   Hide actions the role cannot perform, but still handle a 403 defensively.
4. **Money rendering:** every amount is `BigDecimal`-serialized (string or
   number — treat as decimal, do not do float math). Always render with the
   `currency` field from the same response object. Never assume EUR. Use a
   locale-aware currency formatter seeded by the company currency/locale.
   Negative net pay must be visually distinct and explained.
5. **Determinism:** for locked statuses (`APPROVED`, `FINALIZED`, `PAID`) the
   API returns a frozen snapshot — render exactly what is returned, do not
   recompute or "fix" anything client-side.
6. Idempotent, double-submit-safe mutations (disable buttons in-flight, confirm
   destructive/irreversible transitions). All lifecycle actions require an
   explicit confirm modal stating the consequence.
7. TypeScript types must be generated from / matched to `PAYROLL.md` exactly. No
   `any` on payroll payloads. Add runtime validation (zod or equivalent) at the
   API boundary; a contract mismatch must fail loudly in dev, degrade safely in
   prod.

---

## 1. Enums (mirror backend; confirm against `PAYROLL.md`)

```ts
type PayrollStatus = 'DRAFT' | 'CALCULATED' | 'APPROVED' | 'FINALIZED' | 'PAID' /* | 'CANCELLED' if kept */;
type PayrollCalculationStatus = 'SUCCESS' | 'FAILED' | 'SKIPPED';
type PaymentMethod = 'FIXED_MONTHLY' | 'HOURLY';
type LeaveType = 'VACATION' | 'SICK' | 'PERSONAL' | 'UNPAID' | 'MATERNITY' | 'PATERNITY' | 'OTHER';
type TaxBase = 'GROSS' | 'GROSS_MINUS_CONTRIBUTIONS';
type PayrollTreatment =
  | 'PAID_FROM_BALANCE' | 'UNPAID_EXCESS' | 'UNPAID_EXPLICIT'
  | 'SICK_COMPANY_POLICY' | 'STATUTORY_MATERNITY' | 'STATUTORY_PATERNITY';
```

`DRAFT` = unsaved live preview (never persisted). Treat `CANCELLED` as void if
present. The exact final set is in `PAYROLL.md`.

---

## 2. Endpoint inventory (integrate all)

Base: `/api/v1`. All require auth (JWT, tenant-scoped). `{employeeId}` is a UUID.

### Admin payroll — `/admin/payroll`
| Method | Path | Role | Purpose |
|---|---|---|---|
| GET | `/employees?year=&month=&search=&page=&size=` | STAFF*(assigned), ADMIN, SUPERADMIN | Paginated payroll table for a period |
| GET | `/employees/{employeeId}/details?year=&month=` | STAFF*(assigned), ADMIN, SUPERADMIN | Full payroll breakdown (preview if not yet calculated) |
| GET | `/employees/{employeeId}/calculate?year=&month=` | STAFF*(assigned), ADMIN, SUPERADMIN | Preview calculation (no persist) |
| POST | `/employees/{employeeId}/calculate` body `{year,month}` | STAFF*(assigned), ADMIN, SUPERADMIN | Calculate **and persist** |
| POST | `/calculate` body `{year,month,employeeIds?}` | STAFF*(assigned), ADMIN, SUPERADMIN | Batch calculate |
| POST | `/employees/{employeeId}/adjustments/bonus` body `PayrollAdjustmentRequest` | ADMIN, SUPERADMIN | Add bonus (triggers recalculation) |
| POST | `/employees/{employeeId}/adjustments/deduction` body `PayrollAdjustmentRequest` | ADMIN, SUPERADMIN | Add deduction (triggers recalculation) |
| POST | `/employees/{employeeId}/approve` body `{year,month}` (`allowNegativeNet?`) | ADMIN, SUPERADMIN | CALCULATED → APPROVED (locks attendance) |
| POST | `/employees/{employeeId}/finalize` body `{year,month}` | ADMIN, SUPERADMIN | APPROVED → FINALIZED (freezes payslip) |
| POST | `/employees/{employeeId}/complete-payment` body `{year,month}` | ADMIN, SUPERADMIN | FINALIZED → PAID |
| POST | `/employees/{employeeId}/revert-approval` body `{year,month}` | ADMIN, SUPERADMIN | APPROVED → CALCULATED (unlocks attendance) |
| POST | `/employees/{employeeId}/revert-finalization` body `{year,month}` | ADMIN, SUPERADMIN | FINALIZED → APPROVED |
| POST | `/employees/{employeeId}/revert-payment` body `{year,month}` | ADMIN, SUPERADMIN | PAID → FINALIZED |
| GET | `/employees/{employeeId}/payslip?year=&month=` | STAFF*(assigned), ADMIN, SUPERADMIN | Download PDF payslip (`application/pdf`) |

### Mobile / self-service — `/mobile/payroll`
| Method | Path | Role | Purpose |
|---|---|---|---|
| GET | `/details?year=&month=` | EMPLOYEE, STAFF, ADMIN, SUPERADMIN | Caller's own payroll breakdown |
| GET | `/payslip?year=&month=` | EMPLOYEE, STAFF, ADMIN, SUPERADMIN | Caller's own PDF payslip |

### Payroll settings — `/admin/payroll/settings` (ADMIN, SUPERADMIN)
| Method | Path | Purpose |
|---|---|---|
| GET | `/` | Full payroll settings (work-week, daily hours, tax config, contributions, top-ups, sick policy, isDefault flags) |
| PUT | `/` | Upsert scalar settings |
| GET | `/tax-brackets` | Ordered progressive tax brackets |
| PUT | `/tax-brackets` | Replace-all brackets (validated contiguous) |
| GET | `/holidays?year=` | Resolved public holidays for the year |
| POST | `/holidays` | Create holiday |
| PUT | `/holidays/{id}` | Update holiday |
| DELETE | `/holidays/{id}` | Delete holiday |
| GET | `/sick-leave-policy` | Sick leave policy (may be folded into `/` — check `PAYROLL.md`) |
| PUT | `/sick-leave-policy` | Upsert sick leave policy |

\* STAFF is server-scoped to assigned employees and may be split onto a separate
controller surface — confirm exact paths/role gating in `PAYROLL.md`.

---

## 3. Request payloads

```ts
interface PayrollPeriodRequest { year: number; month: number; /* allowNegativeNet?: boolean (approve only) */ }
interface BatchPayrollCalculationRequest { year: number; month: number; employeeIds?: string[] }
interface PayrollAdjustmentRequest {
  year: number; month: number;
  amount: number;   // > 0, 2-dp
  reason: string;   // required, ≤ 300
  notes?: string;   // ≤ 2000
}
interface UpsertSickLeavePolicyRequest { companyPaidPercentage: number; maxCompanyPaidDays: number } // 0<pct≤100, 1≤days≤365
interface PayrollSettingsRequest {
  defaultDailyWorkingHours: number;            // > 0
  weekendDays: DayOfWeek[];                     // ≥1 working day must remain
  taxEnabled: boolean;
  taxBase: TaxBase;
  socialSecurityEmployeeRate: number;          // % 0..100, 3-dp
  socialSecurityEmployerRate: number;
  pensionEmployeeRate: number;
  pensionEmployerRate: number;
  contributionMinBase?: number | null;
  contributionMaxBase?: number | null;         // ≥ min when both set
  maternityEmployerTopupRate: number;          // % 0..100
  paternityEmployerTopupRate: number;
}
type DayOfWeek = 'MONDAY'|'TUESDAY'|'WEDNESDAY'|'THURSDAY'|'FRIDAY'|'SATURDAY'|'SUNDAY';
interface TaxBracket { ordinal: number; lowerBound: number; upperBound: number | null; rate: number } // contiguous, start 0, one open top
interface TaxBracketsReplaceRequest { brackets: TaxBracket[] }
interface PublicHolidayRequest { date: string /*YYYY-MM-DD*/; name: string; recurring: boolean; paid: boolean }
```

Validate client-side to match server constraints (mirror them; server still
authoritative). Block submit on violation with inline messages.

---

## 4. Response shapes (the comprehensive detail object — render ALL of it)

`PayrollCalculationResponse` (exact field names per `PAYROLL.md`; expect at
least):

```ts
interface PayrollCalculationResponse {
  employeeId: string; employeeName: string;
  year: number; month: number;
  currency: string;                 // ALWAYS render amounts with this
  paymentMethod: PaymentMethod;
  calculationStatus: PayrollCalculationStatus;
  payrollStatus: PayrollStatus;
  preview: boolean;                 // true ⇒ not persisted / live
  employmentPeriod: { employmentStartDate; employmentEndDate; payableFrom; payableTo };
  workPeriod: {
    calendarDaysInMonth; workingDaysInMonth; payableWorkingDays;
    resolvedDailyWorkingHours; payableHours; workHoursSource;   // ATTENDANCE_RECORDS | default fallback
    weekendDays: DayOfWeek[]; holidaysApplied: { date; name; paid }[];
  };
  basePayCalculation: { formula; monthlySalary?; hourlyRate?; payableWorkingDays; workingDaysInMonth; payableHours?; basePay; prorationMethod? };
  hourlyAttendancePayment?: { fullPayableHours; attendedHours; fullPayment; attendanceDeduction; paymentReceived; workHoursSource } | null;
  absenceDetails?: {                // present for FIXED_MONTHLY (informational, applied:false)
    expectedMinutes; attendedMinutes; absentMinutes; monetaryEquivalent; applied: false;
  } | null;
  leaveCalculation: {
    annualPaidLeaveAllowanceDays; usedPaidLeaveBeforeThisMonth;
    leaveTakenThisMonth; paidLeaveDaysThisMonth; paidLeaveAmount;
    unpaidLeaveDaysThisMonth; unpaidLeaveDeduction;
    leaveRecordsIncluded: { id; leaveType; startDate; endDate; daysCountedInPayroll; payrollTreatment: PayrollTreatment; applied: boolean }[];
  };
  sickLeaveCalculation: {
    daysTakenThisMonth; companyPaidDays; unpaidSickLeaveDays;
    companyPaidPercentage; companyPaidAmount;
    /* hourly/monthly specific fields, statePaidDays, etc. */ status;
  };
  statutoryDeductions: {            // from B2 — render the full breakdown
    taxableIncome; incomeTax; employeeSocialSecurity; employeePensionContribution;
    employerSocialSecurity; employerPensionContribution; employerCostTotal;
    taxBase: TaxBase;
    bracketBreakdown: { ordinal; lowerBound; upperBound: number|null; rate; taxedAmount }[];
  };
  adjustments: { bonuses: Line[]; deductions: Line[]; totalBonus; totalManualDeduction };
  totals: { basePay; grossEarnings; totalDeductions; netPay; netPayNegative };
  warnings: string[];               // ALWAYS display prominently if non-empty
}
interface Line { id: string; amount: number; reason: string; notes?: string }
```

`PayrollEmployeeSummaryResponse` (table row) includes scalar mirrors:
`employeeId, employeeName, employeeRole, employmentType, paymentMethod,
monthlySalary?, hourlyRate?, currency, payrollStatus, calculationStatus,
preview, basePay, totalBonus, totalManualDeduction, hourlyFullPayment?,
attendanceDeduction?, attendancePaymentReceived?, incomeTax,
employeeSocialSecurity, employeePension, grossEarnings, totalDeductions, netPay,
employerCostTotal, netPayNegative, warnings[]`. Confirm final fields in
`PAYROLL.md` and null-guard every optional.

---

## 5. Screens to build

### 5.1 Admin payroll table (`/admin/payroll`)
- Month/year picker (default current month), search, server pagination using
  `PaginatedResponse`.
- Columns: employee, role, employment/payment type, base pay, bonuses,
  deductions, income tax, employee SSC, gross, **net**, employer cost, status
  chip, calc status. All money with currency. Negative net flagged.
- Row → detail drawer/page. Bulk "Calculate" (batch) with a result summary
  (success/failed/skipped counts + per-employee reasons from
  `BatchPayrollCalculationResponse`).
- STAFF: table is pre-scoped server-side; do not show a company-wide toggle.

### 5.2 Payroll detail page (comprehensive — rule #4)
Render **every** section from §4 as labeled, expandable cards:
1. Header: employee, period, status chip, currency, `preview` badge.
2. Warnings banner (if any) — must be visually prominent.
3. Employment period & work period (calendar/working days, resolved daily
   hours, weekend days, holidays applied with names, work-hours source).
4. Base pay card: show the `formula` string and the substituted numbers.
5. Hourly attendance card (hourly only): full vs attended hours, deduction,
   payment received.
6. Absence card (monthly only): expected/attended/absent minutes + monetary
   equivalent, clearly labeled **informational, not deducted**.
7. Leave card: allowance, used-before, taken, paid vs unpaid, per-record table
   with `payrollTreatment` and an `applied` indicator.
8. Sick leave card: company-paid %/days/amount, unpaid, state-covered.
9. **Statutory deductions card:** taxable income, tax base, per-bracket table
   (range, rate, taxed amount) summing to income tax; employee SSC; pension;
   then employer-side contributions clearly separated as **employer cost, not
   deducted from the employee**.
10. Adjustments: bonuses & deductions lists with reason/notes.
11. Totals: base → gross → deductions → **net** (big, prominent). The displayed
    applied components must visibly sum to net (render a reconciliation line).
12. Actions (role/status-gated): Calculate/Recalculate, Add bonus, Add
    deduction, lifecycle transitions, Download payslip.

### 5.3 Lifecycle controls
- State machine UI: `DRAFT(preview) → CALCULATED → APPROVED → FINALIZED → PAID`
  with reverse transitions. Show only valid next actions for the current
  status. Each action: confirm modal stating the consequence (e.g. "Approving
  locks attendance for this month"; "Finalizing freezes the payslip and blocks
  further adjustments").
- Approve with negative net: surface a blocking dialog; only proceed if admin
  ticks an explicit override → send `allowNegativeNet:true`.
- After any mutation, refetch the detail + table row (do not optimistically
  trust local state for money).

### 5.4 Self-service (employee/staff "My Payroll")
- Month picker → own `PayrollCalculationResponse` (same comprehensive detail,
  read-only, no lifecycle/adjustment actions).
- "Download payslip" button → `GET /mobile/payroll/payslip`. Disable when
  status is `DRAFT`/not calculated; show the API message
  (`PAYROLL_NOT_CALCULATED`) if returned.

### 5.5 Payslip download (both surfaces)
- Request with `Accept: application/pdf`; expect a binary `application/pdf`
  body and `Content-Disposition` filename. Trigger a browser download; do not
  render PDF bytes as JSON. Handle non-200 (parse `ApiResponse` error JSON if
  the server returns JSON on error).

### 5.6 Payroll Settings screens (`/admin/payroll/settings`, ADMIN/SUPERADMIN)
Tabbed settings page; each tab is GET-prefill + PUT-save with validation,
optimistic-off (save → refetch), and audit-friendly confirm on save:
1. **Work week & hours:** weekend day multi-select (≥1 working day enforced),
   default daily working hours.
2. **Holidays:** calendar/list per year; add/edit/delete with
   `recurring` and `paid` toggles. Warn when deleting a holiday that an existing
   non-locked period will recompute; block (server 409) if a locked period used
   it — show the server message.
3. **Income tax:** `taxEnabled`, `taxBase`, and a bracket editor — ordered
   rows, lower/upper/rate, enforce client-side that brackets start at 0, are
   contiguous & non-overlapping, exactly one open-ended top (upper = null).
   Replace-all on save. Show a worked example (enter a gross, preview tax) using
   a read-only call to the detail/preview endpoint — never compute tax in JS.
4. **Social security & pension:** employee/employer rates, min/max contribution
   base (max ≥ min), maternity/paternity employer top-up rates.
5. **Sick leave policy:** company-paid % and max company-paid days.
Every tab: show `isDefault` indicator when the company is running system
defaults; reflect server validation errors inline (codes in §7).

---

## 6. Cross-feature UX coherence (must reflect backend behavior)

- Approving/cancelling a **leave** in the leave UI can change payroll. After
  leave actions that affect a non-locked month, payroll figures will recompute
  server-side; ensure payroll screens refetch and do not cache stale numbers.
- Leave cancellation is **blocked** by the server when payroll for an affected
  month is locked (`PAYROLL_PERIOD_LOCKED`). In the leave UI, surface this
  message clearly and do not retry blindly.
- Attendance for the month is **locked** on payroll APPROVE and unlocked on
  revert-approval. If you have attendance editing UI, reflect the
  `payrollLocked` state (read-only when locked) and explain why.
- Hourly vs monthly: detail layout differs (hourly shows attendance payment
  card; monthly shows informational absence card). Drive layout off
  `paymentMethod`, never hardcode.

---

## 7. Error codes to handle explicitly (show `message`, take the right action)

`INVALID_PAYROLL_PERIOD`, `EMPLOYEE_NOT_FOUND`, `EMPLOYEE_PROFILE_NOT_FOUND`,
`PAYROLL_RESULT_NOT_FOUND`, `PAYROLL_NOT_CALCULATED`, `PAYROLL_NOT_APPROVED`,
`PAYROLL_NOT_FINALIZED`, `PAYROLL_NOT_PAID`, `PAYROLL_PERIOD_LOCKED`,
`PAYROLL_ACCESS_DENIED`, `PAYROLL_RECALCULATION_REQUIRED`,
`INVALID_PAYROLL_ADJUSTMENT_AMOUNT`, `INVALID_TAX_BRACKETS`,
`INVALID_WORKWEEK_CONFIG`, `INVALID_PAGE`, `INVALID_PAGE_SIZE`,
`UNAUTHENTICATED`, `ACCESS_DENIED`. Treat unknown codes generically (toast
`message`, no crash). Confirm the final list against `PAYROLL.md`.

- 403 / `PAYROLL_ACCESS_DENIED` / `ACCESS_DENIED` → show "not authorized",
  navigate away from the resource; never expose the data.
- 409 lifecycle/lock codes → non-destructive toast, refetch current state,
  re-render available actions.
- 409 `PAYROLL_RECALCULATION_REQUIRED` → prompt user to re-review recalculated
  figures, then re-fetch detail before allowing the transition again.

---

## 8. Deliverables & acceptance criteria

1. Typed API client for **every** endpoint in §2, with runtime schema
   validation at the boundary and no `any`.
2. Admin table, comprehensive detail page, lifecycle controls, self-service
   "My Payroll", payslip download, and the full settings suite (work-week,
   holidays, tax brackets, social security/pension, sick policy) — all wired,
   validated, and role-gated to mirror the server matrix.
3. All money rendered with the response `currency`; negative net flagged;
   applied components visibly reconcile to net.
4. Every state-changing action is confirm-gated, double-submit-safe, and
   refetches authoritative state afterward.
5. Robust error handling for all §7 codes; no uncaught contract mismatch.
6. Loading/empty/error states for every screen; STAFF never sees non-assigned
   employees; EMPLOYEE only sees self.
7. E2E/integration tests for: role visibility matrix, lifecycle happy path +
   each reverse, batch result rendering, settings validation (bad tax brackets,
   all-weekend rejection, max<min base), payslip download, locked-period
   behaviors. Component/unit tests for currency + reconciliation rendering.
8. Verify the actual built UI in a browser against a running backend (golden
   path + edge cases: hourly vs monthly, negative net, missing config defaults,
   STAFF scope, locked period). If you cannot run the backend, state so
   explicitly and what remains unverified — do not claim success blindly.

## 9. Senior-engineer guidance / pitfalls

- Never do arithmetic on payroll amounts in JS beyond display formatting.
  Totals, tax, reconciliation come from the API. The only client "math" allowed
  is formatting and the tax-bracket *editor's structural validation*
  (contiguity), not tax computation.
- Decimal strings: parse with a decimal-safe library if amounts arrive as
  strings; do not `parseFloat` into binary floats for anything shown as money.
- Treat `preview:true` distinctly from a persisted result (badge it; preview
  numbers can change on save).
- Locked statuses are immutable snapshots — disable all mutation affordances and
  the "recalculate" action; only payslip download + view remain.
- Don't assume EUR or any locale; derive from the company/`currency` field.
- Build screens defensively against optional/null fields (old snapshots may
  lack newer fields per `PAYROLL.md` notes).
- Keep the settings forms as the single place statutory config is edited; the
  payroll detail is read-only for config.
- When backend and this doc disagree, **`PAYROLL.md` is law** — raise the
  discrepancy rather than implementing a guess.

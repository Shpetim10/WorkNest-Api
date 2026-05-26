# Frontend Payroll Changes — Backend Update Notice

This document describes backend payroll API changes that require frontend updates.

---

## 1. 🔴 Breaking — Hourly overtime now calculated correctly (gross earnings will change)

**What changed:** For HOURLY employees who have an `overtimeHourlyRate` set, the backend was previously double-paying overtime hours (once in `basePay`, once in `overtimePay`). This is now fixed.

**Numerical impact:**
- `totals.basePay` — now reflects **regular hours only** (hours worked up to the expected threshold), not all worked hours.
- `totals.overtimePay` — unchanged computation (overtimeHours × overtimeRate), but previously the gross total was wrong.
- `totals.grossEarnings` — will be **lower** than before for hourly employees who were working overtime with an OT rate set. This is the correct value.

**No change for:** Fixed-monthly employees (their gross calculation was already correct). Hourly employees with no `overtimeHourlyRate` configured (no overtime pay applied, all hours paid at regular rate as before).

**What to update in the UI:**
- Any "base pay" display for hourly employees should now read as "Regular pay (hourlyRate × regularHours)". OT is a separate line.
- If you were deriving expected total from `basePay + overtimePay`, this now works correctly — no change needed to that formula.
- Re-test any payslip rendering that shows basePay for hourly employees with an OT rate.

---

## 2. 🟠 New field — `overtimePay` in the employee list summary response

**Endpoint:** `GET /api/v1/admin/payroll/employees`

**What changed:** `PayrollEmployeeSummaryResponse` now includes:

```json
{
  "basePay": "1760.00",
  "overtimePay": "150.00",   // ← NEW field (was missing from list view)
  "totalBonus": "...",
  ...
}
```

`overtimePay` is `null` if no overtime was calculated for the employee.

**What to update:**
- Add an "Overtime" column (or sub-line) to the payroll list table. Show it when `overtimePay` is non-null and non-zero.
- Guard against `null`: `(overtimePay ?? 0)` before rendering or summing.

---

## 3. 🟠 Bug fix — `totalDeductions` in the summary was wrong before

**Endpoint:** `GET /api/v1/admin/payroll/employees`

**What changed:** `totalDeductions` in `PayrollEmployeeSummaryResponse` was previously returning the value of `totalManualDeduction` (a bug). It now correctly returns the **full deductions total** (statutory + manual deductions + unpaid leave/sick deductions), matching `totals.totalDeductions` from the detail response.

There are two deduction fields in the summary response with different semantics:
| Field | Meaning |
|---|---|
| `totalManualDeduction` / `deductions` | Manual deductions only (bonus/deduction adjustments) |
| `totalDeductions` | All deductions combined (statutory tax, SS, pension + manual + leave/sick) |

**What to update:**
- If you were using `totalDeductions` to show "manual deductions only", switch to `totalManualDeduction`.
- If you were using `totalDeductions` to show the full picture (net pay derivation), it now works correctly: `grossEarnings - totalDeductions = netPay`.

---

## 4. 🟡 Clarification — `hourlyAttendancePayment.fullPayment` is elapsed-period, not full-month

No code change — informational only.

`hourlyAttendancePayment.fullPayment` shows the expected pay **up to `effectiveAttendanceTo`** (today for the current month, or end-of-month for past months). It is NOT the full-month expected salary.

To show "what the employee would earn for the full month with perfect attendance":
```
fullMonthExpectedPay = workPeriod.payableWorkingDays × workPeriod.defaultDailyWorkingHours × employee.hourlyRate
```

`workPeriod.payableWorkingDays` is already available in the detail response. The `fullPayment` field in `hourlyAttendancePayment` is better read as "expected earned so far" and `paymentReceived` as "actually earned so far" — the deduction between them shows the mid-period attendance shortfall.

---

## 5. 🟡 Clarification — `hourlyAttendancePayment.paymentReceived` excludes overtime

After the fix in point 1, `paymentReceived` = regular base pay (worked hours capped at expected threshold × hourlyRate). It does **not** include overtime pay. The employee's full current earning is:

```
totalEarningsSoFar = paymentReceived + (overtimeDetails?.overtimePay ?? 0) + sickLeave + paidLeave + bonuses
```

This is already what `totals.grossEarnings` shows. You don't need to recompute it — just be aware that `paymentReceived` alone is not the full picture when overtime is present.

---

## 6. ℹ️ No-change summary — what stayed the same

- All lifecycle endpoints (calculate, approve, finalize, pay, reverts) — unchanged.
- `PayrollCalculationResponse` structure — unchanged (all existing fields preserved, no removals).
- Fixed-monthly employees — no changes to their calculation.
- Sick leave, leave, statutory deduction, adjustment endpoints — unchanged.
- Mobile endpoints (`/mobile/payroll/details`, `/mobile/payroll/history`) — unchanged.
- Old snapshots (already APPROVED/FINALIZED/PAID) — served from stored snapshot, not recalculated. Their values will not retroactively change.

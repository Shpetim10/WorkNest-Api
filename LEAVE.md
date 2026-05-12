did # Leave Management — Integration Guide

This document is the source of truth for implementing the leave management feature in **WorkNest-Web** and **WorkNest-Mobile**. It covers every API endpoint, all type definitions, business rules, and error codes.

---

## Table of Contents

1. [Enums](#enums)
2. [TypeScript Types](#typescript-types)
3. [API Overview](#api-overview)
4. [Mobile Endpoints (self-service)](#mobile-endpoints-self-service)
   - [GET /balance](#get-apiv1mobileleavebalance)
   - [GET /requests](#get-apiv1mobileleaverequests)
   - [POST /requests](#post-apiv1mobileleaverequests)
   - [POST /requests/:id/cancel](#post-apiv1mobileleaverequestsidcancel)
5. [Admin Endpoints](#admin-endpoints)
   - [GET /requests (list)](#get-apiv1adminleaverequests)
   - [GET /requests/:id](#get-apiv1adminleaverequestsid)
   - [POST /requests/:id/approve](#post-apiv1adminleaverequestsidapprove)
   - [POST /requests/:id/reject](#post-apiv1adminleaverequestsidreject)
6. [Business Rules](#business-rules)
7. [Error Codes](#error-codes)
8. [Response Envelope](#response-envelope)

---

## Enums

### LeaveType

```ts
type LeaveType = 'VACATION' | 'SICK' | 'PERSONAL' | 'UNPAID' | 'MATERNITY' | 'PATERNITY' | 'OTHER';
```

### LeaveStatus

```ts
type LeaveStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';
```

Status transitions:
- `PENDING → APPROVED` (admin/manager)
- `PENDING → REJECTED` (admin/manager)
- `PENDING → CANCELLED` (employee self-service)
- `APPROVED → CANCELLED` (employee self-service, before payroll is finalised)

---

## TypeScript Types

Copy these into your `features/leave/types/` directory (or equivalent) in both Web and Mobile.

```ts
// ─── Enums ───────────────────────────────────────────────────────────────────

export type LeaveType = 'VACATION' | 'SICK' | 'PERSONAL' | 'UNPAID' | 'MATERNITY' | 'PATERNITY' | 'OTHER';
export type LeaveStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

// ─── Request bodies ───────────────────────────────────────────────────────────

/** POST /api/v1/mobile/leave/requests */
export interface CreateLeaveRequestBody {
  leaveType: LeaveType;
  /** ISO 8601 date string: "YYYY-MM-DD" */
  startDate: string;
  /** ISO 8601 date string: "YYYY-MM-DD" */
  endDate: string;
  /** Optional employee note, max 500 characters */
  note?: string | null;
  /**
   * Required when leaveType === 'SICK'. Must be a document reference ID
   * from the media/document service. Omitting this for SICK leave returns
   * 400 MISSING_MEDICAL_REPORT.
   */
  medicalReportDocumentId?: string | null;
}

/** POST /api/v1/admin/leave/requests/:id/reject */
export interface RejectLeaveRequestBody {
  /** Required, max 500 characters */
  reason: string;
}

/** POST /api/v1/admin/leave/requests/:id/approve — body is optional */
export interface ApproveLeaveRequestBody {
  /** Optional reviewer note, max 500 characters */
  note?: string | null;
}

// ─── Response shapes ─────────────────────────────────────────────────────────

/** One entry per leave type */
export interface LeaveBalanceDto {
  leaveType: LeaveType;
  totalDays: number;
  usedDays: number;
  /** Pre-calculated: Math.max(0, totalDays - usedDays) */
  availableDays: number;
}

export interface LeaveRequestDto {
  id: string;                       // UUID
  employeeId: string;               // UUID
  employeeName: string;             // displayName if set, else "firstName lastName"
  siteName: string | null;
  departmentName: string | null;
  leaveType: LeaveType;
  /** "YYYY-MM-DD" */
  startDate: string;
  /** "YYYY-MM-DD" */
  endDate: string;
  /** Supports half-days (e.g. 0.5). Currently always a whole number at submission. */
  daysCount: number;
  status: LeaveStatus;
  note: string | null;
  /** Set when an admin/manager approves with a note */
  approvalNote: string | null;
  /** Only present for SICK leave; the external document reference ID */
  medicalReportDocumentId: string | null;
  rejectionReason: string | null;   // only set when status === 'REJECTED'
  reviewedAt: string | null;        // ISO 8601 timestamp, null when PENDING or CANCELLED
  createdAt: string;                // ISO 8601 timestamp
}

// ─── Pagination (admin list endpoint) ────────────────────────────────────────

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;   // current page index (0-based)
  first: boolean;
  last: boolean;
}
```

---

## API Overview

| Method | Path | Who can call | Purpose |
|--------|------|--------------|---------|
| `GET` | `/api/v1/mobile/leave/balance` | EMPLOYEE, STAFF, ADMIN, SUPERADMIN | My leave balance for this year |
| `GET` | `/api/v1/mobile/leave/requests` | EMPLOYEE, STAFF, ADMIN, SUPERADMIN | My leave request history |
| `POST` | `/api/v1/mobile/leave/requests` | EMPLOYEE, STAFF, ADMIN, SUPERADMIN | Submit a leave request |
| `POST` | `/api/v1/mobile/leave/requests/:id/cancel` | EMPLOYEE, STAFF, ADMIN, SUPERADMIN | Cancel my own leave request |
| `GET` | `/api/v1/admin/leave/requests` | STAFF, ADMIN, SUPERADMIN | List all company requests (paginated) |
| `GET` | `/api/v1/admin/leave/requests/:id` | STAFF, ADMIN, SUPERADMIN | Get one request in full detail |
| `POST` | `/api/v1/admin/leave/requests/:id/approve` | STAFF, ADMIN, SUPERADMIN | Approve a pending request |
| `POST` | `/api/v1/admin/leave/requests/:id/reject` | STAFF, ADMIN, SUPERADMIN | Reject a pending request |

All endpoints require a valid JWT in the `Authorization: Bearer <token>` header. The token encodes the user's company context — there is no company ID parameter to pass separately.

---

## Mobile Endpoints (self-service)

These are designed for the **employee mobile app** but any authenticated role can call them. They always operate on the currently authenticated user's employee profile.

---

### GET /api/v1/mobile/leave/balance

Returns the authenticated employee's leave balance for the **current calendar year**. All leave types are always returned, even if a balance record has not been explicitly created yet — the API auto-initialises defaults on first read.

**Default allocations (if no balance record exists yet):**

| Type | Default days |
|------|-------------|
| `VACATION` | Employee's `leaveDaysPerYear` value, or `20` if not set |
| `SICK` | `10` |
| `PERSONAL` | `5` |
| `UNPAID` | `0` |
| `MATERNITY` | Employee's `leaveDaysPerYear` value, or `20` if not set |
| `PATERNITY` | Employee's `leaveDaysPerYear` value, or `20` if not set |
| `OTHER` | Employee's `leaveDaysPerYear` value, or `20` if not set |

**Request**

```
GET /api/v1/mobile/leave/balance
Authorization: Bearer <token>
```

**Response `200`**

```json
{
  "status": "success",
  "message": "Leave balance loaded",
  "data": [
    {
      "leaveType": "VACATION",
      "totalDays": 20,
      "usedDays": 5,
      "availableDays": 15
    },
    {
      "leaveType": "SICK",
      "totalDays": 10,
      "usedDays": 0,
      "availableDays": 10
    },
    {
      "leaveType": "PERSONAL",
      "daysCount": 5,
      "usedDays": 2,
      "availableDays": 3
    }
  ]
}
```

**UI guidance:** Display as cards — one per leave type. Show a progress bar using `usedDays / totalDays`. Display `availableDays` prominently as the primary figure an employee cares about.

---

### GET /api/v1/mobile/leave/requests

Returns the authenticated employee's full leave request history, sorted newest first. Includes requests in all statuses: PENDING, APPROVED, REJECTED, CANCELLED.

**Request**

```
GET /api/v1/mobile/leave/requests
Authorization: Bearer <token>
```

**Response `200`**

```json
{
  "status": "success",
  "message": "Leave requests loaded",
  "data": [
    {
      "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "employeeId": "...",
      "employeeName": "Jane Doe",
      "siteName": "London HQ",
      "departmentName": "Engineering",
      "leaveType": "VACATION",
      "startDate": "2026-06-01",
      "endDate": "2026-06-05",
      "daysCount": 5,
      "status": "APPROVED",
      "note": "Summer holiday",
      "medicalReportDocumentId": null,
      "rejectionReason": null,
      "reviewedAt": "2026-05-10T09:00:00Z",
      "createdAt": "2026-05-04T14:30:00Z"
    }
  ]
}
```

**UI guidance:** Render as a list with status badges (colour-coded: yellow = PENDING, green = APPROVED, red = REJECTED, grey = CANCELLED). Show `rejectionReason` inline when status is `REJECTED`. Show a "Cancel" button on PENDING and APPROVED requests.

---

### POST /api/v1/mobile/leave/requests

Submits a new leave request on behalf of the authenticated employee. The request lands in `PENDING` status and awaits admin review.

**Request**

```
POST /api/v1/mobile/leave/requests
Authorization: Bearer <token>
Content-Type: application/json

{
  "leaveType": "VACATION",
  "startDate": "2026-07-14",
  "endDate": "2026-07-18",
  "note": "Summer trip"
}
```

For SICK leave:

```json
{
  "leaveType": "SICK",
  "startDate": "2026-05-05",
  "endDate": "2026-05-07",
  "note": "Flu",
  "medicalReportDocumentId": "doc-uuid-abc123"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `leaveType` | `LeaveType` | Yes | One of the supported leave types |
| `startDate` | `string` | Yes | ISO date `YYYY-MM-DD` |
| `endDate` | `string` | Yes | ISO date `YYYY-MM-DD`, must not be before `startDate` |
| `note` | `string` | No | Max 500 characters |
| `medicalReportDocumentId` | `string` | **Required for `SICK`** | Document reference ID from the media service; omitting it returns `400 MISSING_MEDICAL_REPORT` |

`totalDays` is **calculated by the server**: `endDate - startDate + 1` (inclusive, calendar days including weekends).

**Response `200`**

```json
{
  "status": "success",
  "message": "Leave request submitted successfully",
  "data": null
}
```

**Important:** The server does **not** validate against leave balance at submission time. Balance is advisory only — it is used for reporting and the balance screen, but it does not gate approval. Show a local warning in the UI if the requested days exceed `availableDays`, but do not block submission.

---

### POST /api/v1/mobile/leave/requests/:id/cancel

Cancels the authenticated employee's own `PENDING` or `APPROVED` leave request.

- A `CANCELLED` request is excluded from all payroll calculations.
- When an `APPROVED` request is cancelled, the used days are reversed in the employee's balance automatically.
- Only the employee who owns the request can cancel it.

**Request**

```
POST /api/v1/mobile/leave/requests/3fa85f64-5717-4562-b3fc-2c963f66afa6/cancel
Authorization: Bearer <token>
```

No request body.

**Response `200`**

```json
{
  "status": "success",
  "message": "Leave request cancelled",
  "data": null
}
```

**Error cases:**
- `404 LEAVE_REQUEST_NOT_FOUND` — request does not exist or belongs to another company
- `403 FORBIDDEN` — caller is not the owner of the leave request
- `409 LEAVE_ALREADY_CANCELLED` — request is already cancelled
- `409 LEAVE_CANNOT_BE_CANCELLED` — request is in a final state (REJECTED) that cannot be cancelled

---

## Admin Endpoints

These are designed for the **web dashboard** (managers / HR admins). They operate across all employees of the authenticated user's company.

---

### GET /api/v1/admin/leave/requests

Returns a paginated list of leave requests for the whole company. Supports filtering by status and searching by employee name.

**Request**

```
GET /api/v1/admin/leave/requests?search=jane&status=PENDING&page=0&size=20
Authorization: Bearer <token>
```

| Query param | Type | Required | Default | Description |
|-------------|------|----------|---------|-------------|
| `search` | `string` | No | — | Case-insensitive partial match on employee first + last name |
| `status` | `LeaveStatus` | No | — | Filter by status. Omit for all statuses (including CANCELLED) |
| `page` | `number` | No | `0` | Zero-based page index |
| `size` | `number` | No | `20` | Page size |

**Response `200`**

```json
{
  "status": "success",
  "message": "Leave requests loaded",
  "data": {
    "content": [ /* LeaveRequestDto[] */ ],
    "totalElements": 47,
    "totalPages": 3,
    "size": 20,
    "number": 0,
    "first": true,
    "last": false
  }
}
```

**UI guidance:** Default view should filter to `status=PENDING` so admins see the action queue first. Provide tabs or a dropdown to switch between PENDING / APPROVED / REJECTED / CANCELLED / All. The `search` field should debounce (300 ms) before firing.

---

### GET /api/v1/admin/leave/requests/:id

Returns the full detail of a single leave request.

**Request**

```
GET /api/v1/admin/leave/requests/3fa85f64-5717-4562-b3fc-2c963f66afa6
Authorization: Bearer <token>
```

**Response `200`**

```json
{
  "status": "success",
  "message": "Leave request loaded",
  "data": {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "employeeId": "...",
    "employeeName": "Jane Doe",
    "siteName": "London HQ",
    "departmentName": "Engineering",
    "leaveType": "SICK",
    "startDate": "2026-06-01",
    "endDate": "2026-06-05",
    "totalDays": 5,
    "status": "PENDING",
    "note": "Flu",
    "medicalReportDocumentId": "doc-uuid-abc123",
    "rejectionReason": null,
    "reviewedAt": null,
    "createdAt": "2026-05-04T14:30:00Z"
  }
}
```

**UI guidance:** For SICK leave requests, display the `medicalReportDocumentId` as a link or reference so the reviewer can inspect the medical document before approving.

---

### POST /api/v1/admin/leave/requests/:id/approve

Approves a `PENDING` leave request. On success, `usedDays` is incremented in `leave_balances` regardless of whether the remaining balance is sufficient — balance is informational and does not gate approval.

**Request**

```
POST /api/v1/admin/leave/requests/3fa85f64-5717-4562-b3fc-2c963f66afa6/approve
Authorization: Bearer <token>
Content-Type: application/json

{
  "note": "Approved — medical report reviewed"
}
```

The request body is **optional**. Send it only if you want to attach a reviewer note (e.g. "Approved after medical review"). Omit the body entirely for a plain approval.

**Response `200`**

```json
{
  "status": "success",
  "message": "Leave request approved",
  "data": null
}
```

**Error cases:**
- `409 LEAVE_NOT_PENDING` — request is not in PENDING state

---

### POST /api/v1/admin/leave/requests/:id/reject

Rejects a `PENDING` leave request. A rejection reason is required. The employee's balance is **not** affected.

**Request**

```
POST /api/v1/admin/leave/requests/3fa85f64-5717-4562-b3fc-2c963f66afa6/reject
Authorization: Bearer <token>
Content-Type: application/json

{
  "reason": "Insufficient team coverage during this period."
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `reason` | `string` | Yes | Non-blank, max 500 characters |

**Response `200`**

```json
{
  "status": "success",
  "message": "Leave request rejected",
  "data": null
}
```

**Error cases:**
- `409 LEAVE_NOT_PENDING` — request is not in PENDING state

---

## Business Rules

1. **Date range:** `endDate` must not be before `startDate`. Single-day requests are valid (`startDate === endDate`).
2. **Total days:** calculated as `endDate - startDate + 1` (inclusive, all calendar days — weekends count).
3. **Overlap prevention:** A new request is rejected at submission if any existing `PENDING` or `APPROVED` request for the same employee overlaps the requested date range.
4. **Medical report for SICK leave:** Submitting a `SICK` leave request without `medicalReportDocumentId` returns `400 MISSING_MEDICAL_REPORT`. The document reference is immutable once set.
5. **Balance is advisory:** Balance is not checked at submission or at approval time. Approving a leave request always succeeds (subject to status check). The balance counters are updated on approval and reversed on cancellation for reporting purposes only.
6. **Balance auto-init:** If no balance record exists for an employee/year/type combination, the API creates one with defaults transparently on first GET or on approval.
7. **State machine:** See the status transitions in [Enums](#enums). REJECTED is a terminal state — it cannot be cancelled.
8. **Cancel reverses balance:** When an APPROVED request is cancelled, `usedDays` is decremented automatically.
9. **Reviewer recorded:** Both `approve` and `reject` record the calling admin's user ID and a `reviewedAt` timestamp on the request. The approve endpoint also accepts an optional `note` stored as `approvalNote` on the request.
10. **Active employee only:** Submitting or cancelling a request (mobile endpoints) requires the caller to have an `ACTIVE` employee profile. Inactive employees get `403 EMPLOYEE_INACTIVE`.
13. **Cancellation payroll lock:** Cancelling an `APPROVED` request is blocked when the payroll for any month the leave spans has been approved, finalized, or paid. Returns `409 PAYROLL_PERIOD_LOCKED`.
11. **Company isolation:** All queries are scoped to the authenticated user's company. Users cannot see or act on requests from other companies.
12. **Payroll exclusion:** Only `APPROVED` leave records affect payroll. `PENDING`, `REJECTED`, and `CANCELLED` records are completely excluded from all payroll computations.

---

## Error Codes

All errors follow the standard API envelope. The `code` field is machine-readable for the frontend to show specific messages.

```json
{
  "status": "error",
  "message": "Human-readable description",
  "code": "ERROR_CODE"
}
```

| HTTP | Code | When it occurs |
|------|------|----------------|
| `400` | `INVALID_DATE_RANGE` | `endDate` is before `startDate` |
| `400` | `MISSING_MEDICAL_REPORT` | SICK leave submitted without `medicalReportDocumentId` |
| `400` | `(validation errors)` | Missing required fields or constraint violations |
| `403` | `EMPLOYEE_PROFILE_NOT_FOUND` | Authenticated user has no employee record in this company |
| `403` | `EMPLOYEE_INACTIVE` | Employee's employment status is not `ACTIVE` |
| `403` | `FORBIDDEN` | Attempting to cancel another employee's leave request |
| `404` | `LEAVE_REQUEST_NOT_FOUND` | Request ID does not exist or belongs to a different company |
| `409` | `LEAVE_OVERLAP` | Submitted dates overlap an existing PENDING or APPROVED request |
| `409` | `LEAVE_NOT_PENDING` | Approve/reject called on a request that is not in PENDING state |
| `409` | `LEAVE_ALREADY_CANCELLED` | Cancel called on a request that is already CANCELLED |
| `409` | `LEAVE_CANNOT_BE_CANCELLED` | Cancel called on a REJECTED request (terminal state) |
| `409` | `PAYROLL_PERIOD_LOCKED` | Cancel of APPROVED leave blocked because payroll for that period is already approved/finalized/paid |

---

## Response Envelope

Every response is wrapped in this envelope (defined in `ApiResponse`):

```ts
interface ApiResponse<T> {
  status: 'success' | 'error';
  message: string;
  data: T | null;
}
```

Pagination responses wrap a Spring `Page<T>` object in `data`:

```ts
interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;   // current page (0-based)
  first: boolean;
  last: boolean;
}
```

---

## Implementation Checklist

### WorkNest-Web (Next.js)

- [ ] Update `src/features/leave/types/index.ts` — add `CANCELLED` to `LeaveStatus`, add `medicalReportDocumentId` to `LeaveRequestDto`, update `CreateLeaveRequestBody`
- [ ] Update `useLeaveRequests(params)` — add `CANCELLED` tab/filter option in the status dropdown
- [ ] Update leave detail view — display `medicalReportDocumentId` for SICK leave requests (link to document viewer)
- [ ] Remove any client-side logic that treated `INSUFFICIENT_LEAVE_BALANCE` as an approval error (that error no longer exists)
- [ ] Invalidate admin request list cache after approve/reject mutations

### WorkNest-Mobile (Expo / React Native)

- [ ] Update `src/features/leave/types/index.ts` — add `CANCELLED` to `LeaveStatus`, add `medicalReportDocumentId` to request body and DTO types
- [ ] Submit request screen: add `medicalReportDocumentId` field (document picker) shown only when `leaveType === 'SICK'`; validate it is non-empty before submit
- [ ] Request history screen: render `CANCELLED` status badge (grey); add a "Cancel" button on PENDING and APPROVED request cards that calls `POST /requests/:id/cancel`
- [ ] Show `rejectionReason` on rejected request detail

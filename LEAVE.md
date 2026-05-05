# Leave Management — Integration Guide

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
type LeaveType = 'VACATION' | 'SICK' | 'PERSONAL';
```

### LeaveStatus

```ts
type LeaveStatus = 'PENDING' | 'APPROVED' | 'REJECTED';
```

Status transitions are one-way: `PENDING → APPROVED` or `PENDING → REJECTED`. There is no way to revert an approved or rejected request through the API.

---

## TypeScript Types

Copy these into your `features/leave/types/` directory (or equivalent) in both Web and Mobile.

```ts
// ─── Enums ───────────────────────────────────────────────────────────────────

export type LeaveType = 'VACATION' | 'SICK' | 'PERSONAL';
export type LeaveStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

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
}

/** POST /api/v1/admin/leave/requests/:id/reject */
export interface RejectLeaveRequestBody {
  /** Required, max 500 characters */
  reason: string;
}

// ─── Response shapes ─────────────────────────────────────────────────────────

/** One entry per leave type, always returns all 3 types */
export interface LeaveBalanceDto {
  leaveType: LeaveType;
  totalDays: number;
  usedDays: number;
  /** Pre-calculated: Math.max(0, totalDays - usedDays) */
  availableDays: number;
}

export interface LeaveRequestDto {
  id: string;           // UUID
  employeeId: string;   // UUID
  employeeName: string; // displayName if set, else "firstName lastName"
  siteName: string | null;
  departmentName: string | null;
  leaveType: LeaveType;
  /** "YYYY-MM-DD" */
  startDate: string;
  /** "YYYY-MM-DD" */
  endDate: string;
  totalDays: number;
  status: LeaveStatus;
  note: string | null;
  rejectionReason: string | null; // only set when status === 'REJECTED'
  reviewedAt: string | null;      // ISO 8601 timestamp, null when PENDING
  createdAt: string;              // ISO 8601 timestamp
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

Returns the authenticated employee's leave balance for the **current calendar year**. All three leave types are always returned, even if a balance record has not been explicitly created yet — the API auto-initialises defaults on first read.

**Default allocations (if no balance record exists yet):**

| Type | Default days |
|------|-------------|
| `VACATION` | Employee's `leaveDaysPerYear` value, or `20` if not set |
| `SICK` | `10` |
| `PERSONAL` | `5` |

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
      "totalDays": 5,
      "usedDays": 2,
      "availableDays": 3
    }
  ]
}
```

**UI guidance:** Display as three cards or rows — one per leave type. Show a progress bar using `usedDays / totalDays`. Display `availableDays` prominently as the primary figure an employee cares about.

---

### GET /api/v1/mobile/leave/requests

Returns the authenticated employee's full leave request history, sorted newest first.

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
      "totalDays": 5,
      "status": "APPROVED",
      "note": "Summer holiday",
      "rejectionReason": null,
      "reviewedAt": "2026-05-10T09:00:00Z",
      "createdAt": "2026-05-04T14:30:00Z"
    }
  ]
}
```

**UI guidance:** Render as a list with status badges (colour-coded: yellow = PENDING, green = APPROVED, red = REJECTED). Show `rejectionReason` inline when status is `REJECTED`.

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

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `leaveType` | `LeaveType` | Yes | `VACATION`, `SICK`, or `PERSONAL` |
| `startDate` | `string` | Yes | ISO date `YYYY-MM-DD` |
| `endDate` | `string` | Yes | ISO date `YYYY-MM-DD`, must not be before `startDate` |
| `note` | `string` | No | Max 500 characters |

`totalDays` is **calculated by the server**: `endDate - startDate + 1` (inclusive, calendar days including weekends).

**Response `200`**

```json
{
  "status": "success",
  "message": "Leave request submitted successfully",
  "data": null
}
```

**Important:** The server does **not** validate against balance at submission time. Balance is only checked when an admin approves the request. Show a local warning in the UI if the requested days exceed available balance, but do not block submission.

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
| `status` | `LeaveStatus` | No | — | Filter by `PENDING`, `APPROVED`, or `REJECTED`. Omit for all statuses |
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

**UI guidance:** Default view should filter to `status=PENDING` so admins see the action queue first. Provide tabs or a dropdown to switch between PENDING / APPROVED / REJECTED / All. The `search` field should debounce (300 ms) before firing.

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
    "leaveType": "VACATION",
    "startDate": "2026-06-01",
    "endDate": "2026-06-05",
    "totalDays": 5,
    "status": "PENDING",
    "note": "Summer holiday",
    "rejectionReason": null,
    "reviewedAt": null,
    "createdAt": "2026-05-04T14:30:00Z"
  }
}
```

---

### POST /api/v1/admin/leave/requests/:id/approve

Approves a `PENDING` leave request. The server checks that the employee has sufficient balance for the leave type and year of the request's `startDate`. On success, `usedDays` is incremented in `leave_balances`.

**Request**

```
POST /api/v1/admin/leave/requests/3fa85f64-5717-4562-b3fc-2c963f66afa6/approve
Authorization: Bearer <token>
```

No request body.

**Response `200`**

```json
{
  "status": "success",
  "message": "Leave request approved",
  "data": null
}
```

**Error cases:**
- `409 LEAVE_NOT_PENDING` — request is already approved or rejected
- `409 INSUFFICIENT_LEAVE_BALANCE` — employee does not have enough days remaining

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
- `409 LEAVE_NOT_PENDING` — request is already approved or rejected

---

## Business Rules

1. **Date range:** `endDate` must not be before `startDate`. Single-day requests are valid (`startDate === endDate`).
2. **Total days:** calculated as `endDate - startDate + 1` (inclusive, all calendar days — weekends count).
3. **Overlap prevention:** A new request is rejected at submission if any existing `PENDING` or `APPROVED` request for the same employee overlaps the requested date range.
4. **Balance check on approval only:** Balance is not checked at submission. The admin sees the request and decides — if balance is insufficient at approval time, the API returns `409 INSUFFICIENT_LEAVE_BALANCE`.
5. **Balance auto-init:** If no balance record exists for an employee/year/type combination, the API creates one with defaults (see table above) transparently on first GET or on approval.
6. **State machine:** `PENDING → APPROVED` or `PENDING → REJECTED`. No other transitions exist.
7. **Reviewer recorded:** Both `approve` and `reject` record the calling admin's user ID and a `reviewedAt` timestamp on the request.
8. **Active employee only:** Submitting a request (mobile endpoints) requires the caller to have an `ACTIVE` employee profile. Inactive employees get `403 EMPLOYEE_INACTIVE`.
9. **Company isolation:** All queries are scoped to the authenticated user's company. Users cannot see or act on requests from other companies.

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
| `400` | `(validation errors)` | Missing required fields or constraint violations (`leaveType`, `startDate`, `endDate` are null; `note`/`reason` exceed 500 chars) |
| `403` | `EMPLOYEE_PROFILE_NOT_FOUND` | Authenticated user has no employee record in this company |
| `403` | `EMPLOYEE_INACTIVE` | Employee's employment status is not `ACTIVE` |
| `404` | `LEAVE_REQUEST_NOT_FOUND` | Request ID does not exist or belongs to a different company |
| `409` | `LEAVE_OVERLAP` | Submitted dates overlap an existing PENDING or APPROVED request |
| `409` | `LEAVE_NOT_PENDING` | Approve/reject called on a request that is already approved or rejected |
| `409` | `INSUFFICIENT_LEAVE_BALANCE` | Approval attempted but employee's available days < request's `totalDays` |

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

- [ ] Create `src/features/leave/types/index.ts` with the TypeScript types above
- [ ] Create `src/features/leave/api/` — TanStack Query hooks for each endpoint
  - `useLeaveRequests(params)` — admin list with search/status/pagination
  - `useLeaveRequest(id)` — single request detail
  - `useApproveLeave()` — mutation
  - `useRejectLeave()` — mutation (takes `{ id, reason }`)
- [ ] Admin leave list page: default filter to `PENDING`, tabs for status, debounced search
- [ ] Leave detail page / modal: show all fields, approve/reject buttons (disable if not PENDING)
- [ ] Reject dialog: required text area for `reason`, 500-char limit
- [ ] Invalidate admin request list cache after approve/reject mutations

### WorkNest-Mobile (Expo / React Native)

- [ ] Create `src/features/leave/types/index.ts` with the TypeScript types above
- [ ] Balance screen: fetch `GET /mobile/leave/balance`, three cards with progress bars
- [ ] Request history screen: fetch `GET /mobile/leave/requests`, list with status badges
- [ ] Submit request screen:
  - Leave type picker (VACATION / SICK / PERSONAL)
  - Date range picker (startDate, endDate)
  - Optional note field (500-char limit)
  - Local advisory warning if requested days > `availableDays` (do not block submit)
- [ ] Show `rejectionReason` on rejected request detail
- [ ] Redux slice (or Zustand, matching whatever pattern mobile uses) to cache balance locally
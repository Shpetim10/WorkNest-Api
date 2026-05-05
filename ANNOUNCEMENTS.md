# Announcements — Integration Guide

This document is the source of truth for implementing the announcements feature in **WorkNest-Web** and **WorkNest-Mobile**. It covers every API endpoint, all type definitions, business rules, and error codes.

---

## Table of Contents

1. [Enums](#enums)
2. [TypeScript Types](#typescript-types)
3. [API Overview](#api-overview)
4. [Admin Endpoints](#admin-endpoints)
   - [POST /announcements (create)](#post-apiv1companiescompanyidannouncements)
   - [GET /announcements (list)](#get-apiv1companiescompanyidannouncements)
   - [DELETE /announcements/:id](#delete-apiv1companiescompanyidannouncementsid)
5. [Mobile Endpoints](#mobile-endpoints)
   - [GET /announcements (list)](#get-apiv1mobileannouncements)
   - [GET /announcements/unread-count](#get-apiv1mobileannouncementsunread-count)
   - [GET /announcements/:id](#get-apiv1mobileannouncementsid)
   - [POST /announcements/:id/read](#post-apiv1mobileannouncementsidread)
6. [Business Rules](#business-rules)
7. [Error Codes](#error-codes)
8. [Response Envelope](#response-envelope)
9. [Implementation Checklist](#implementation-checklist)

---

## Enums

### AnnouncementAudience

```ts
type AnnouncementAudience = 'ALL_EMPLOYEES' | 'DEPARTMENT' | 'SPECIFIC_USERS';
```

| Value | Meaning |
|-------|---------|
| `ALL_EMPLOYEES` | Visible to every employee in the company |
| `DEPARTMENT` | Visible only to employees in the selected departments |
| `SPECIFIC_USERS` | Visible only to the hand-picked employees |

### AnnouncementPriority

```ts
type AnnouncementPriority = 'NORMAL' | 'IMPORTANT';
```

`IMPORTANT` announcements should be visually distinguished in the mobile list (e.g. a red/pink badge). `NORMAL` announcements use a neutral style.

---

## TypeScript Types

Copy these into your `features/announcements/types/` directory in both Web and Mobile.

```ts
// ─── Enums ───────────────────────────────────────────────────────────────────

export type AnnouncementAudience = 'ALL_EMPLOYEES' | 'DEPARTMENT' | 'SPECIFIC_USERS';
export type AnnouncementPriority = 'NORMAL' | 'IMPORTANT';

// ─── Request bodies ───────────────────────────────────────────────────────────

/** POST /api/v1/companies/:companyId/announcements */
export interface CreateAnnouncementBody {
  /** Required. Max 255 characters. */
  title: string;
  /** Required. The full announcement text. No length limit. */
  content: string;
  /** Required. Who should receive this announcement. */
  targetAudience: AnnouncementAudience;
  /**
   * Required when targetAudience === 'DEPARTMENT'.
   * List of department UUIDs to target. Must be non-empty.
   */
  targetDepartmentIds?: string[];
  /**
   * Required when targetAudience === 'SPECIFIC_USERS'.
   * List of employee UUIDs to target. Must be non-empty.
   */
  targetEmployeeIds?: string[];
  /** Optional. Defaults to 'NORMAL' if omitted. */
  priority?: AnnouncementPriority;
}

// ─── Response shapes ─────────────────────────────────────────────────────────

/** Used in both the admin list and the create response */
export interface AnnouncementListResponse {
  id: string;                        // UUID
  title: string;
  content: string;
  targetAudience: AnnouncementAudience;
  priority: AnnouncementPriority;
  /** displayName if set, else "firstName lastName". "Unknown" if author was deleted. */
  createdByName: string;
  createdAt: string;                 // ISO 8601 timestamp
}

/** One item in the mobile announcement list */
export interface MobileAnnouncementListItem {
  id: string;                        // UUID
  title: string;
  /** First 120 characters of the content, truncated server-side. */
  contentPreview: string;
  priority: AnnouncementPriority;
  createdAt: string;                 // ISO 8601 timestamp
  /** true if the current employee has already marked this announcement as read */
  read: boolean;
}

/** Full detail view for a single announcement on mobile */
export interface MobileAnnouncementDetail {
  id: string;                        // UUID
  title: string;
  /** Full, untruncated content */
  content: string;
  priority: AnnouncementPriority;
  createdAt: string;                 // ISO 8601 timestamp
  read: boolean;
}

/** Response of the unread-count endpoint */
export interface UnreadCountResponse {
  count: number;
}
```

---

## API Overview

| Method | Path | Who can call | Purpose |
|--------|------|--------------|---------|
| `POST` | `/api/v1/companies/:companyId/announcements` | ADMIN, SUPERADMIN | Create a new announcement |
| `GET` | `/api/v1/companies/:companyId/announcements` | ADMIN, SUPERADMIN | List all company announcements |
| `DELETE` | `/api/v1/companies/:companyId/announcements/:id` | ADMIN, SUPERADMIN | Delete an announcement |
| `GET` | `/api/v1/mobile/announcements` | EMPLOYEE, STAFF, ADMIN, SUPERADMIN | List announcements visible to me |
| `GET` | `/api/v1/mobile/announcements/unread-count` | EMPLOYEE, STAFF, ADMIN, SUPERADMIN | My unread announcement badge count |
| `GET` | `/api/v1/mobile/announcements/:id` | EMPLOYEE, STAFF, ADMIN, SUPERADMIN | Full detail of one announcement |
| `POST` | `/api/v1/mobile/announcements/:id/read` | EMPLOYEE, STAFF, ADMIN, SUPERADMIN | Mark an announcement as read |

All endpoints require a valid JWT in the `Authorization: Bearer <token>` header. The token encodes the user's company context — the mobile endpoints do not require a `:companyId` path parameter.

---

## Admin Endpoints

These are designed for the **web dashboard** (managers / HR admins). They manage announcements for the entire company.

---

### POST /api/v1/companies/:companyId/announcements

Creates a new company announcement. The authenticated user is recorded as the author.

**Request**

```
POST /api/v1/companies/9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d/announcements
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "Office Closure - Public Holiday",
  "content": "The office will be closed on April 10th for the public holiday.",
  "targetAudience": "ALL_EMPLOYEES",
  "priority": "NORMAL"
}
```

**Targeting a department:**

```json
{
  "title": "Engineering Sprint Review",
  "content": "All engineers must attend the Q2 sprint review on Friday at 3pm.",
  "targetAudience": "DEPARTMENT",
  "targetDepartmentIds": [
    "dept-uuid-1",
    "dept-uuid-2"
  ],
  "priority": "IMPORTANT"
}
```

**Targeting specific employees:**

```json
{
  "title": "Contract Renewal Reminder",
  "content": "Your contract is due for renewal. Please contact HR.",
  "targetAudience": "SPECIFIC_USERS",
  "targetEmployeeIds": [
    "employee-uuid-1",
    "employee-uuid-2"
  ],
  "priority": "NORMAL"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `title` | `string` | Yes | Non-blank, max 255 characters |
| `content` | `string` | Yes | Non-blank |
| `targetAudience` | `AnnouncementAudience` | Yes | One of the three valid values |
| `targetDepartmentIds` | `string[]` | Conditional | Required and non-empty when `targetAudience === 'DEPARTMENT'` |
| `targetEmployeeIds` | `string[]` | Conditional | Required and non-empty when `targetAudience === 'SPECIFIC_USERS'` |
| `priority` | `AnnouncementPriority` | No | Defaults to `'NORMAL'` |

**Response `201`**

```json
{
  "success": true,
  "message": "Announcement created successfully",
  "data": {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "title": "Office Closure - Public Holiday",
    "content": "The office will be closed on April 10th for the public holiday.",
    "targetAudience": "ALL_EMPLOYEES",
    "priority": "NORMAL",
    "createdByName": "Admin",
    "createdAt": "2026-04-01T10:00:00Z"
  },
  "timestamp": "2026-04-01T10:00:00Z"
}
```

**Error cases:**
- `400 MISSING_TARGET_DEPARTMENTS` — `targetAudience` is `DEPARTMENT` but `targetDepartmentIds` is empty or missing
- `400 MISSING_TARGET_EMPLOYEES` — `targetAudience` is `SPECIFIC_USERS` but `targetEmployeeIds` is empty or missing

---

### GET /api/v1/companies/:companyId/announcements

Returns all announcements for the company, sorted newest first.

**Request**

```
GET /api/v1/companies/9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d/announcements
Authorization: Bearer <token>
```

**Response `200`**

```json
{
  "success": true,
  "message": "Announcements retrieved successfully",
  "data": [
    {
      "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "title": "Office Closure - Public Holiday",
      "content": "The office will be closed on April 10th for the public holiday.",
      "targetAudience": "ALL_EMPLOYEES",
      "priority": "NORMAL",
      "createdByName": "Admin",
      "createdAt": "2026-04-01T10:00:00Z"
    },
    {
      "id": "9a3cd801-1234-4aee-b123-abc123def456",
      "title": "New Health Insurance Policy",
      "content": "We are introducing a new health insurance plan starting May 1st.",
      "targetAudience": "ALL_EMPLOYEES",
      "priority": "IMPORTANT",
      "createdByName": "Admin",
      "createdAt": "2026-03-28T08:30:00Z"
    }
  ],
  "timestamp": "2026-05-05T12:00:00Z"
}
```

**UI guidance:** Render each announcement as a card. Show `targetAudience` as a label badge in the top-right corner of the card (e.g. "All Employees", "Department", "Specific Users"). Format `createdAt` as `YYYY-MM-DD` and display as `By {createdByName} • {date}`. Provide a delete icon per card.

---

### DELETE /api/v1/companies/:companyId/announcements/:id

Permanently deletes an announcement. All read records and targeting records for this announcement are deleted automatically (cascaded by the database).

**Request**

```
DELETE /api/v1/companies/9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d/announcements/3fa85f64-5717-4562-b3fc-2c963f66afa6
Authorization: Bearer <token>
```

**Response `200`**

```json
{
  "success": true,
  "message": "Announcement deleted successfully",
  "data": null,
  "timestamp": "2026-05-05T12:00:00Z"
}
```

**Error cases:**
- `404 ANNOUNCEMENT_NOT_FOUND` — ID does not exist or belongs to a different company

---

## Mobile Endpoints

These are designed for the **employee mobile app**. They always operate on the currently authenticated user's employee profile and only return announcements targeted at that employee.

**Visibility rules** (applied server-side):
- `ALL_EMPLOYEES` announcements are always visible.
- `DEPARTMENT` announcements are visible if the employee belongs to one of the targeted departments.
- `SPECIFIC_USERS` announcements are visible if the employee is in the targeted employees list.

---

### GET /api/v1/mobile/announcements

Returns all announcements visible to the current employee, sorted newest first. Each item includes a `read` flag indicating whether the employee has already read it.

**Request**

```
GET /api/v1/mobile/announcements
Authorization: Bearer <token>
```

**Response `200`**

```json
{
  "success": true,
  "message": "Announcements loaded",
  "data": [
    {
      "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "title": "Office Maintenance Notice",
      "contentPreview": "The office will be temporarily closed on April 10th due to scheduled maintenance.",
      "priority": "IMPORTANT",
      "createdAt": "2026-04-08T09:00:00Z",
      "read": false
    },
    {
      "id": "a1b2c3d4-0000-4abc-8abc-000000000001",
      "title": "New Health Insurance Policy",
      "contentPreview": "We are introducing a new health insurance plan starting May 1st.",
      "priority": "NORMAL",
      "createdAt": "2026-03-28T08:30:00Z",
      "read": true
    }
  ],
  "timestamp": "2026-05-05T12:00:00Z"
}
```

**UI guidance:**
- Show each announcement as a card with an icon, title, `contentPreview`, relative time (`2 days ago`), and a priority badge.
- `IMPORTANT` → red/pink badge labelled "Important".
- `NORMAL` → neutral/grey badge (can show "General" or omit the badge for normal priority).
- Unread announcements should appear visually distinct (e.g. bold title, highlighted background).
- The list header should show a `{count} new` badge pulled from the unread-count endpoint.

---

### GET /api/v1/mobile/announcements/unread-count

Returns the number of announcements visible to the current employee that have not been marked as read. Use this to populate the notification badge in the app header or tab bar.

**Request**

```
GET /api/v1/mobile/announcements/unread-count
Authorization: Bearer <token>
```

**Response `200`**

```json
{
  "success": true,
  "message": "Unread count loaded",
  "data": {
    "count": 2
  },
  "timestamp": "2026-05-05T12:00:00Z"
}
```

**UI guidance:** Poll or refetch this on app foreground. Display `count` as a badge on the Announcements nav item. Hide the badge when `count === 0`.

---

### GET /api/v1/mobile/announcements/:id

Returns the full content of a single announcement visible to the current employee.

**Request**

```
GET /api/v1/mobile/announcements/3fa85f64-5717-4562-b3fc-2c963f66afa6
Authorization: Bearer <token>
```

**Response `200`**

```json
{
  "success": true,
  "message": "Announcement loaded",
  "data": {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "title": "Office Maintenance Notice",
    "content": "The office will be temporarily closed on April 10th due to scheduled maintenance.\n\nPlease plan your work accordingly. Remote work is recommended during this time.\n\nFor any urgent matters, contact your manager.",
    "priority": "IMPORTANT",
    "createdAt": "2026-04-08T09:00:00Z",
    "read": false
  },
  "timestamp": "2026-05-05T12:00:00Z"
}
```

**UI guidance:** Show the `priority` badge at the top of the detail sheet. Render `content` as multi-paragraph text (split on `\n`). Show a "Mark as read" button when `read === false`. After marking as read, either dismiss the sheet or update the `read` field locally.

**Error cases:**
- `404 ANNOUNCEMENT_NOT_FOUND` — ID does not exist, belongs to a different company, or is not targeted at the current employee

---

### POST /api/v1/mobile/announcements/:id/read

Marks an announcement as read for the current employee. Calling this endpoint more than once on the same announcement is idempotent — no error is returned if it has already been read.

**Request**

```
POST /api/v1/mobile/announcements/3fa85f64-5717-4562-b3fc-2c963f66afa6/read
Authorization: Bearer <token>
```

No request body.

**Response `200`**

```json
{
  "success": true,
  "message": "Announcement marked as read",
  "data": null,
  "timestamp": "2026-05-05T12:00:00Z"
}
```

**UI guidance:** Call this automatically when the employee opens the announcement detail screen (not just when they tap a "Mark as read" button). After a successful call, refetch or invalidate the unread-count and the announcement list to keep badges in sync.

**Error cases:**
- `404 ANNOUNCEMENT_NOT_FOUND` — ID does not exist or is not targeted at the current employee

---

## Business Rules

1. **Audience targeting:** An announcement targets exactly one audience type. Changing the audience type after creation is not supported — delete and re-create.
2. **Department visibility:** An employee in department A will only see `DEPARTMENT` announcements that include department A. Employees with no assigned department never see `DEPARTMENT` announcements.
3. **Read status is per-employee:** Marking an announcement as read does not affect other employees' read status.
4. **Idempotent mark-as-read:** Calling `POST /:id/read` multiple times has no side effects — the server ignores the call if already read.
5. **Cascade delete:** Deleting an announcement removes all associated read records and targeting records immediately. This cannot be undone.
6. **Author snapshot:** The author is stored as a foreign key reference. If the author's user account is later deleted, `createdByName` returns `"Unknown"` for that announcement.
7. **Company isolation:** All queries are scoped to the authenticated user's company. Employees cannot see or act on announcements from other companies.
8. **Active employee only:** Mobile endpoints require the caller to have an `ACTIVE` employee profile. Inactive employees receive `403 EMPLOYEE_INACTIVE`.

---

## Error Codes

All errors follow the standard API envelope. The `code` field is machine-readable.

```json
{
  "success": false,
  "code": "ERROR_CODE",
  "message": "Human-readable description",
  "path": "/api/v1/...",
  "timestamp": "2026-05-05T12:00:00Z"
}
```

| HTTP | Code | When it occurs |
|------|------|----------------|
| `400` | `MISSING_TARGET_DEPARTMENTS` | `targetAudience` is `DEPARTMENT` but `targetDepartmentIds` is absent or empty |
| `400` | `MISSING_TARGET_EMPLOYEES` | `targetAudience` is `SPECIFIC_USERS` but `targetEmployeeIds` is absent or empty |
| `400` | `VALIDATION_ERROR` | Required fields missing or constraint violations (e.g. title blank, title > 255 chars) |
| `403` | `EMPLOYEE_PROFILE_NOT_FOUND` | Authenticated user has no employee record in this company |
| `403` | `EMPLOYEE_INACTIVE` | Employee's employment status is not `ACTIVE` |
| `404` | `ANNOUNCEMENT_NOT_FOUND` | Announcement ID does not exist, belongs to a different company, or is not visible to the current employee |

---

## Response Envelope

Every response is wrapped in this envelope (defined in `ApiResponse`):

```ts
interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T | null;
  timestamp: string; // ISO 8601
}
```

---

## Implementation Checklist

### WorkNest-Web (Next.js)

- [ ] Create `src/features/announcements/types/index.ts` with the TypeScript types above
- [ ] Create `src/features/announcements/api/` — TanStack Query hooks:
  - `useAnnouncements(companyId)` — admin list
  - `useCreateAnnouncement()` — mutation (POST)
  - `useDeleteAnnouncement()` — mutation (DELETE), invalidates list on success
- [ ] Announcements list page:
  - Render each card with title, `By {createdByName} • {date}`, content, and audience badge
  - "Create Announcement" button in top-right opens the create dialog
  - Delete icon per card (confirm before calling DELETE)
- [ ] Create Announcement dialog:
  - Title field (required, max 255)
  - Description/content textarea (required)
  - Target Audience radio group (`ALL_EMPLOYEES` / `DEPARTMENT` / `SPECIFIC_USERS`)
    - When `DEPARTMENT` selected: show list of departments from the existing `/lookup` endpoint; allow multi-select
    - When `SPECIFIC_USERS` selected: show searchable list of employees; allow multi-select
  - Priority radio group (`NORMAL` / `IMPORTANT`), defaults to `NORMAL`
  - "Back" and "Add" buttons
- [ ] Invalidate announcement list cache after create and delete mutations
- [ ] Use existing department lookup endpoint for the department picker

### WorkNest-Mobile (Expo / React Native)

- [ ] Create `src/features/announcements/types/index.ts` with the TypeScript types above
- [ ] Announcement list screen:
  - Show `{count} new` badge in the header from `GET /unread-count`
  - Each card: icon, title, `contentPreview`, relative time, priority badge
  - `IMPORTANT` → red/pink badge; `NORMAL` → grey/neutral or no badge
  - Unread cards visually distinct (bold title or highlight)
  - Tap opens the detail sheet
- [ ] Announcement detail bottom sheet:
  - Priority badge at the top
  - Full `content` rendered as multi-paragraph text
  - "Mark as read" button (visible when `read === false`)
  - Automatically call `POST /:id/read` on open (before the button tap)
- [ ] After marking as read: invalidate/refetch unread-count and announcement list
- [ ] Poll or refetch `unread-count` when the app comes to the foreground
- [ ] Redux slice (matching the existing mobile Redux pattern):
  - Store announcements list and unread count
  - Actions: `setAnnouncements`, `markRead`, `setUnreadCount`
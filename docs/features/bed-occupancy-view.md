# Feature: Bed Occupancy View

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-04-27 | @author | Initial draft |
| 1.1 | 2026-04-27 | @author | Align permission `action` with hyphen convention (`occupancy-view`); correct role names to actual seed data (`ADMINISTRATIVE_STAFF`, drop non-existent `RECEPTIONIST`); call out admission wizard `?roomId=` support as an explicit deliverable; clarify new typed repository projection (existing `findRoomsWithAvailabilityAndCount()` is not extended); spec stale-data and search-highlight UX with i18n keys. |

---

## Overview

A read-only screen that gives staff and nurses an at-a-glance view of every room's bed occupancy across the hospital, showing which beds are free and which are in use (and by which patient). It exists to replace ad-hoc "who's in which room?" coordination with a single visual source of truth, layered on top of existing room/admission data without changing the underlying clinical model.

---

## Use Case / User Story

> *As a nurse, I want to see at a glance which beds are free and which are occupied across the hospital, so I can quickly answer bed-availability questions without checking each admission individually.*

> *As an admissions/staff member, I want to filter free beds by gender and room type, so I can place a new patient in the correct room without violating gender-segregation rules.*

> *As an admissions/staff member, I want to start a new admission directly from a free bed slot, so I don't have to re-enter the room when I already know where the patient should go.*

> *As a nurse, I want the view to refresh on its own, so the bed map I'm looking at stays accurate while I work.*

---

## Authorization / Role Access

| Action | Required Role(s) | Permission | Notes |
|--------|------------------|------------|-------|
| View bed-occupancy screen | ADMIN, NURSE, ADMINISTRATIVE_STAFF | `room:occupancy-view` | **Not granted to DOCTOR by default.** A user with both DOCTOR and an admin/staff role inherits access via the second role. Role names match seed data (`V005`, `V014`); `RECEPTIONIST` is **not** a real role in this codebase. |
| See occupant patient names | Same as above | `room:occupancy-view` | No separate gate; included in the screen permission. |
| "Admit here" deep-link from a free bed | Roles allowed to create admissions | `admission:create` (existing) | Button hidden/disabled if user lacks this permission. |

The new `room:occupancy-view` permission is introduced specifically to gate this screen. Reusing `room:read` is rejected because doctors already hold `room:read` for clinical context, and the screen must not be visible to doctor-only users.

---

## Functional Requirements

- The screen lists **all non-deleted rooms** the hospital has configured.
- For each room, render **one bed slot per unit of `room.capacity`**, each marked clearly as **Free** or **Occupied**.
- Occupied slots display: occupant patient name, admission date, and a link to the admission/patient detail page.
- Each room card shows: room number, type (PRIVATE/SHARED), gender (MALE/FEMALE), total capacity, free count.
- A summary header shows totals across all rooms: total beds, occupied beds, free beds, occupancy %.
- Filters: by **gender** (primary), by room type, by occupancy status (all / only free / only occupied).
- Free-text search: by room number or by occupant patient name.
- **Default visual grouping is by gender** (men's rooms vs. women's rooms) — the only hard segregation rule in the domain.
- **Auto-refresh** every 30 seconds (default), plus a manual refresh button. In-flight filters and search state are preserved across refreshes.
- The screen is **read-only for clinical state** — no admit, transfer, or discharge actions are exposed.
- Each **Free** bed exposes an "Admit here" action that deep-links to the existing admission wizard with the target room pre-selected. Hidden if the user lacks `admission:create`.
- Soft-deleted rooms are excluded; soft-deleted/discharged admissions do not count as occupying.
- Only `HOSPITALIZATION` admissions count as occupying a bed (other admission types do not require a room — see `AdmissionType.requiresRoom()`).

### Out of scope

- Gender mismatch warnings on the screen itself (filter handles this).
- Showing expected discharge dates on occupied beds.
- Floor/wing concepts (not modeled in `Room` today).
- Editing/transferring patients between beds from this screen.

---

## Acceptance Criteria / Scenarios

### Happy path

1. When a user with `room:occupancy-view` opens `/bed-occupancy`, the screen loads showing every non-deleted room with one card per bed slot (count = `room.capacity`), each marked **Free** or **Occupied**.
2. When a bed is occupied by an active admission, its card shows the patient's name, admission date, and a link to the admission/patient detail page.
3. When the screen loads, the summary header shows totals: total beds, occupied, free, occupancy %.
4. When a user toggles the **gender** filter to "Female", only female-designated rooms render; men's rooms are hidden.
5. When a user clicks **Admit here** on a Free bed and has `admission:create`, they are routed to the admission wizard with the room pre-selected.
6. When auto-refresh fires (default 30s), the view updates without a page reload; current filters and search are preserved.
7. When a user searches by room number, only rooms whose `number` contains the query render. When searching by patient name, only rooms with at least one matching occupant render, with matching beds highlighted.

### Edge cases & negative paths

8. When a user without `room:occupancy-view` navigates to `/bed-occupancy`, the route guard redirects them (dashboard or 403). The menu entry is not visible to them.
9. When a doctor-only user logs in, the menu entry and route are inaccessible. The same user with an additional admin/staff role sees the screen.
10. When a user lacks `admission:create`, **Admit here** buttons are hidden on free beds.
11. When a room has `capacity = 3` and 1 active admission, exactly 1 Occupied slot and 2 Free slots render.
12. When an admission is soft-deleted or transitions to `DISCHARGED`, its bed becomes Free on the next refresh without a page reload.
13. When a room is soft-deleted, it is excluded from the screen entirely.
14. When the backend call fails (network/500), the screen shows an inline error state with a retry button. It does not crash. Stale data, if shown, is clearly marked.
15. When zero rooms exist, the screen shows an empty state with guidance ("No rooms configured — ask an admin to add rooms").
16. When data anomalies result in a single patient appearing in two active admissions, each admission still occupies its own bed. No client-side dedup hides the issue.

---

## Non-Functional Requirements (Optional)

None specified beyond standard project conventions.

---

## API Contract

A single new aggregated endpoint is introduced. Existing room/admission endpoints are not modified.

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| GET | `/api/v1/rooms/occupancy` | - | `BedOccupancyResponse` | `room:occupancy-view` | Returns all rooms with per-bed occupancy detail and an aggregate summary. Used to render the entire screen in one call. |

### Request/Response Example

```json
// GET /api/v1/rooms/occupancy - Response
{
  "summary": {
    "totalBeds": 24,
    "occupiedBeds": 17,
    "freeBeds": 7,
    "occupancyPercent": 70.83
  },
  "rooms": [
    {
      "id": 12,
      "number": "201",
      "type": "PRIVATE",
      "gender": "FEMALE",
      "capacity": 1,
      "occupiedBeds": 1,
      "availableBeds": 0,
      "occupants": [
        {
          "admissionId": 884,
          "patientId": 312,
          "patientName": "María González",
          "admissionDate": "2026-04-22"
        }
      ]
    },
    {
      "id": 13,
      "number": "202",
      "type": "SHARED",
      "gender": "MALE",
      "capacity": 3,
      "occupiedBeds": 1,
      "availableBeds": 2,
      "occupants": [
        {
          "admissionId": 901,
          "patientId": 401,
          "patientName": "Carlos Pérez",
          "admissionDate": "2026-04-25"
        }
      ]
    }
  ]
}
```

### DTO Definitions

```kotlin
// dto/response/BedOccupancyResponse.kt
data class BedOccupancyResponse(
    val summary: OccupancySummary,
    val rooms: List<RoomOccupancyItem>
)

data class OccupancySummary(
    val totalBeds: Int,
    val occupiedBeds: Int,
    val freeBeds: Int,
    val occupancyPercent: Double
)

data class RoomOccupancyItem(
    val id: Long,
    val number: String,
    val type: RoomType,
    val gender: RoomGender,
    val capacity: Int,
    val occupiedBeds: Int,
    val availableBeds: Int,
    val occupants: List<BedOccupant>
)

data class BedOccupant(
    val admissionId: Long,
    val patientId: Long,
    val patientName: String,
    val admissionDate: LocalDate
)
```

Notes:
- `occupants.size == occupiedBeds`. The frontend renders `capacity - occupiedBeds` Free slots in addition to one Occupied slot per occupant.
- Only `Admission` rows where `status = ACTIVE`, `deletedAt IS NULL`, `type = HOSPITALIZATION` are counted.
- Implementation should use a single repository query (joined fetch) to avoid N+1 — extend `RoomRepository.findRoomsWithAvailabilityAndCount()` to also project active admission rows.

---

## Database Changes

### New Entities

None. The feature is a read-projection over existing `Room` and `Admission` entities.

### New Migrations

| Migration | Description |
|-----------|-------------|
| `V091__add_room_occupancy_permission.sql` | Insert `room:occupancy-view` permission and grant to ADMIN, NURSE, and admin/staff roles. **Do not** grant to DOCTOR. |

### Schema Example

```sql
-- V091__add_room_occupancy_permission.sql
-- Hyphen in `action` matches existing convention (see V005 `user:reset-password`,
-- V025 `admission:upload-consent`). Codes use `<resource>:<action>` form.
INSERT INTO permissions (code, resource, action, created_at, updated_at)
VALUES ('room:occupancy-view', 'room', 'occupancy-view', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Grant to non-doctor staff roles. Role names match V005/V014 seed data.
-- Explicitly NOT granted to DOCTOR.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE p.code = 'room:occupancy-view'
  AND r.name IN ('ADMIN', 'NURSE', 'ADMINISTRATIVE_STAFF');
```

### Index Requirements

- [ ] No new indexes needed. The existing `idx_admissions_room_id`, `idx_admissions_status`, and `idx_admissions_deleted_at` indexes (assumed present from V001-V089) cover the aggregation query.

---

## Frontend Changes

### Components

| Component | Location | Description |
|-----------|----------|-------------|
| `BedOccupancyView.vue` | `web/src/views/operations/` | Page-level view at `/bed-occupancy`. Hosts summary, filters, room grid, auto-refresh. |
| `BedOccupancySummary.vue` | `web/src/components/bedOccupancy/` | Header counters: total / occupied / free / occupancy %. |
| `BedOccupancyFilters.vue` | `web/src/components/bedOccupancy/` | Gender, type, occupancy-status filters and search input. |
| `RoomOccupancyCard.vue` | `web/src/components/bedOccupancy/` | One card per room. Renders capacity-many bed slots. |
| `BedSlot.vue` | `web/src/components/bedOccupancy/` | One bed slot — Free (with "Admit here") or Occupied (with patient name + admission link). |
| `AdmissionFormView.vue` (modified) | `web/src/views/admissions/` | Extend the existing admission wizard to read `?roomId=` from the route and pre-select that room in the form. Currently only `?patientId=` is supported — `roomId` does not exist yet and must be added. |

### Pinia Stores

| Store | Location | Description |
|-------|----------|-------------|
| `useBedOccupancyStore` | `web/src/stores/bedOccupancy.ts` | Holds `rooms`, `summary`, `loading`, `error`, `lastFetchedAt`. Action `fetchOccupancy()` calls `GET /api/v1/rooms/occupancy`. Auto-refresh interval handle is owned by the view, not the store. |

### Routes

| Path | Component | Auth Required | Roles / Permission |
|------|-----------|---------------|-------|
| `/bed-occupancy` | `BedOccupancyView` | Yes | Requires `room:occupancy-view` |

The route guard uses the existing permission-check pattern (`authStore.hasPermission('room:occupancy-view')`). The sidebar/menu entry is conditionally rendered with the same check.

### Validation (Zod Schemas)

```typescript
// No write operations from this screen — no Zod schemas required.
// Filter state is local component state with TypeScript types only.
```

### i18n Keys (excerpt)

```
bedOccupancy.title
bedOccupancy.summary.total
bedOccupancy.summary.occupied
bedOccupancy.summary.free
bedOccupancy.summary.percent
bedOccupancy.filter.gender.all | male | female
bedOccupancy.filter.type.all | private | shared
bedOccupancy.filter.status.all | free | occupied
bedOccupancy.search.placeholder
bedOccupancy.bed.free
bedOccupancy.bed.occupied
bedOccupancy.bed.admitHere
bedOccupancy.empty
bedOccupancy.error.loadFailed
bedOccupancy.error.stale          # "Last updated {time} — could not refresh"
bedOccupancy.error.retry
bedOccupancy.refresh
bedOccupancy.search.matchHighlight  # aria-label for highlighted matching bed slot
```

---

## Implementation Notes

- **Reuse existing model**: The codebase already has `Room` (with `capacity`, `type`, `gender`), `Admission` (with `status`, `type`, `room` relation), and `RoomAvailabilityResponse` (computes `availableBeds = capacity - activeAdmissions`). This feature is a presentation-layer aggregation — no new entities.
- **Single aggregated endpoint**: Render the whole screen from one network call to avoid N+1 and keep auto-refresh cheap. Add a service method `RoomService.getBedOccupancy(): BedOccupancyResponse` that issues one query joining `Room` ⇆ active `Admission` ⇆ `Patient`, then projects into the response DTOs.
- **New typed repository projection**: Add a new method on `RoomRepository` (e.g., `findRoomsWithActiveAdmissions(): List<RoomOccupancyProjection>`) using a JPA interface projection or constructor expression. The existing `findRoomsWithAvailabilityAndCount(): List<Array<Any>>` returns untyped arrays and is **not** suitable to extend — leave it untouched and introduce a typed query for this feature.
- **Active admission filter**: Only `Admission.status = ACTIVE`, `deletedAt IS NULL`, and `type = HOSPITALIZATION` count as occupying. This matches `AdmissionType.requiresRoom()` semantics (see `entity/AdmissionType.kt`).
- **"Admit here" deep-link — wizard change required**: The admission wizard (`web/src/views/admissions/AdmissionFormView.vue`) currently reads only `?patientId=` from the route and does **not** pre-select a room. This feature must add support for `?roomId={id}`: parse the query param on mount, look up the room, and pre-fill the room field in the wizard. Treat this as a deliverable, not a verification step (see Frontend Changes table).
- **Auto-refresh**: Implement with `setInterval` inside `BedOccupancyView.vue`, cleared in `onBeforeUnmount`. Default 30s. Pause refresh when the tab is hidden (`document.visibilitychange`) to avoid wasted calls.
- **Stale-data UX**: When auto-refresh fails after the initial load succeeded, keep the last successful payload visible and render a non-blocking banner above the room grid: "Last updated {relativeTime} — could not refresh" with a retry button. Use i18n keys `bedOccupancy.error.stale` and `bedOccupancy.error.retry`. The error state with no prior data (initial load failure) renders the full-screen error state instead.
- **Search-highlight UX**: When the search input matches by patient name, matching `BedSlot.vue` instances render with a highlight ring (PrimeVue `outline` token or a Tailwind ring utility — pick one and apply consistently). Non-matching slots in the same room render normally. When matching by room number, no per-slot highlight is needed (the whole card is the match).
- **Performance**: Aggregation query should not exceed ~50ms for a few hundred rooms. If it does, add a covering index or precompute per-refresh.
- **Audit logging**: Read-only screen — no audit-log writes required. Existing controller-level logging (if any) is sufficient.

---

## QA Checklist

### Backend
- [ ] `GET /api/v1/rooms/occupancy` endpoint implemented and gated by `room:occupancy-view`
- [ ] Aggregation query uses a single fetch (no N+1)
- [ ] Only ACTIVE, non-deleted, HOSPITALIZATION admissions count
- [ ] Soft-deleted rooms excluded
- [ ] DTOs (`BedOccupancyResponse`, `RoomOccupancyItem`, `BedOccupant`, `OccupancySummary`) used in controller (no entity exposure)
- [ ] V090 migration creates permission and grants to correct roles, **not** DOCTOR
- [ ] Unit tests for `RoomService.getBedOccupancy()` cover: empty rooms, partial occupancy, full occupancy, mixed admission types
- [ ] Integration test (Testcontainers) verifying the endpoint end-to-end including soft-delete exclusions
- [ ] Authorization test: doctor-only user gets 403; nurse/admin gets 200
- [ ] Detekt passes (no new violations)
- [ ] OWASP dependency-check passes

### Frontend
- [ ] `BedOccupancyView.vue` and child components implemented
- [ ] `useBedOccupancyStore` Pinia store implemented
- [ ] Route `/bed-occupancy` guarded by `room:occupancy-view`
- [ ] Sidebar/menu entry conditionally rendered with the same permission
- [ ] Filters (gender, type, status) and search work client-side over the fetched data
- [ ] Auto-refresh runs every 30s, pauses on hidden tab, cleared on unmount
- [ ] "Admit here" hidden when user lacks `admission:create`; navigates with `roomId` query param when present
- [ ] `AdmissionFormView.vue` reads `?roomId=` from the route and pre-selects the room in the wizard form
- [ ] Loading, error (with retry), empty, and **stale-after-refresh-failure** states implemented
- [ ] Patient-name search highlights matching `BedSlot` instances; room-number search filters the grid without per-slot highlight
- [ ] ESLint/oxlint passes
- [ ] All user-facing strings use i18n keys
- [ ] Vitest unit tests for store actions and component rendering (free vs. occupied slots)

### E2E Tests (Playwright)
- [ ] Nurse user sees the screen and can filter by gender
- [ ] Doctor-only user is redirected/blocked from `/bed-occupancy` and does not see the menu entry
- [ ] Doctor + admin/staff dual-role user can access the screen
- [ ] Clicking "Admit here" on a free bed opens the admission wizard with the room pre-selected
- [ ] Discharging an admission elsewhere causes the bed to flip to Free on next refresh
- [ ] Backend error surfaces an inline error state with a working retry
- [ ] Empty state renders when no rooms exist

### General
- [ ] API contract documented in this file
- [ ] V090 migration tested locally and on a CI-like seed
- [ ] CLAUDE.md "Implemented Features" updated
- [ ] Reviewed by project owner

---

## Documentation Updates Required

### Must Update

- [ ] **[CLAUDE.md](../../CLAUDE.md)**
  - Add **Bed Occupancy View** to "Implemented Features" (Backend and Frontend lists)
  - Bump migration count to include V090

### Must Update (cont.)

- [ ] **[docs/features/patient-admission.md](./patient-admission.md)**
  - Document the new `?roomId=` query-param support on `AdmissionFormView.vue` (added by this feature) alongside the existing `?patientId=` deep-link.

### Code Documentation

- [ ] **`api/src/main/kotlin/com/insidehealthgt/hms/service/RoomService.kt`**
  - KDoc on the new `getBedOccupancy()` method describing filter rules
- [ ] **`api/src/main/kotlin/com/insidehealthgt/hms/dto/response/BedOccupancyResponse.kt`**
  - KDoc clarifying that `occupants.size == occupiedBeds`

---

## Related Docs/Commits/Issues

- Related feature: [patient-admission.md](./patient-admission.md) — admission wizard target for "Admit here" deep-link
- Related feature: [admission-types.md](./admission-types.md) — only HOSPITALIZATION admissions occupy beds
- Existing code: `RoomController`, `RoomService`, `RoomAvailabilityResponse` — already compute per-room availability; this feature extends the projection to per-bed-slot detail
- GitHub Issue: *to be filled in*

# Feature: Patient Admissions History

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-06-01 | @ramonster | Initial draft |
| 1.1 | 2026-06-01 | @ramonster | Fix authorization model to match the existing patient-detail visibility rules (no direct-access leak); use `formatDateTime`; thread `currentUser` through service/controller; add denied-access acceptance criteria; spell out pagination/loading/error UI; make row links conditional on `admission:read`; note `idx_admissions_admission_date` already exists |

---

## Overview

Show the complete history of admissions for a single patient inside the Patient Detail
view. The data model already supports multiple admissions per patient (a patient is
admitted, discharged, and may later be re-admitted), but there is currently **no way to
retrieve or view a patient's prior admissions** — only the current global admissions
list and individual admission detail exist. This feature adds a patient-scoped admissions
list to the API and surfaces it as a new "Admissions history" section on the patient
detail page.

---

## Use Case / User Story

> *As any clinician or staff member who can view patient records, I want to see all of a
> patient's past and current admissions in one place so that I can understand the
> patient's history with the hospital (readmissions, prior stays, discharge dates,
> treating physicians) without hunting through the global admissions list.*

Concrete scenarios:

- A resident opens a patient's detail page and sees that the patient was admitted 3 times
  over the last year, each with its admission/discharge dates, type, room, and treating
  physician.
- An administrative staff member confirms whether a returning patient has a prior stay
  before starting a new admission.
- Clicking a row in the history navigates to the existing admission detail view.

---

## Authorization / Role Access

The customer requirement is: **the history is visible to anyone who can see that patient's
records.** The key word is *that patient's* — "can see patient records" is not a flat
`patient:read` check in this codebase. The patient detail endpoint
(`GET /api/v1/patients/{id}`, `PatientService.getPatient`) gates on `patient:read` **and**
applies **patient-level visibility rules**:

- a **standalone `DOCTOR`** (not also ADMIN/RESIDENT_DOCTOR) may only open patients
  assigned to them (`isPatientAssignedToDoctor`) — otherwise `AccessDenied`;
- a **`PSYCHOLOGIST`** (not also ADMIN) may only open patients who currently have an
  ACTIVE admission — otherwise `AccessDenied`.

| Action | Required Role(s) | Permission | Notes |
|--------|------------------|------------|-------|
| View a patient's admissions history | Any role holding `patient:read` (ADMIN, DOCTOR, RESIDENT_DOCTOR, NURSE, AUXILIARY_NURSE, CHIEF_NURSE, PSYCHOLOGIST, ADMINISTRATIVE_STAFF, …) **whose patient-level visibility allows opening that patient** | `patient:read` | Must enforce the **same patient-level visibility** as the patient detail endpoint |

### The rule: patient-level visibility IS enforced; per-admission row filtering is NOT

The endpoint must answer two separate questions in order:

1. **Can this caller see this patient at all?** — Yes/No, using the *same* gate as the
   patient detail page (`patient:read` + the `resolveDoctorId` doctor-assignment check +
   the `resolveActiveAdmissionsOnly` psychologist active-admission check). If No →
   `403 Forbidden` (or `404` for an unknown patient).
2. **If yes, which of that patient's admissions are returned?** — **All of them**
   (ACTIVE and DISCHARGED, every type). Once the caller is allowed to see the patient, we
   do *not* additionally filter the admission rows by treating-physician or status.

This is the correct reading of the customer intent ("see all admissions for a patient")
without weakening the existing access model. The contrast with the **global** admissions
list is deliberate:

- **Global list** (`GET /api/v1/admissions`): applies `resolveDoctorId` /
  `resolveActiveAdmissionsOnly` as **per-row filters** so a doctor sees only their own
  patients' admissions and a psychologist sees only ACTIVE ones.
- **Per-patient history** (this feature): applies those same predicates as a **one-time
  patient-access gate**, then returns the full set for the (now authorized) patient.

> ⚠️ Why this matters — the v1.0 draft proposed gating on `patient:read` alone with **no**
> visibility check. That would have been a **direct-access data leak**: a standalone
> `DOCTOR` who cannot even open a patient's detail page today could, knowing only the
> patient ID, fetch that patient's entire admission history. The patient-access gate
> (step 1) closes that hole. A `PSYCHOLOGIST` is likewise blocked from a patient with no
> active admission, exactly as on the detail page.

> Implementation note: **reuse the exact `resolveDoctorId` / `resolveActiveAdmissionsOnly`
> helpers** (they already exist identically on both `PatientController` and
> `AdmissionController`) and route the access decision through `PatientService.getPatient(...)`
> (or an `assertPatientAccessible(...)` extracted from it) so it stays in one place and
> cannot drift from the patient detail page. The point is the opposite of v1.0: do **not**
> skip these helpers — apply them as a gate, not as a row filter.

---

## Functional Requirements

- The API must expose all admissions (ACTIVE and DISCHARGED) for a given patient,
  **including none** (a patient with zero admissions returns an empty page, not 404).
- **All admission types** are returned (HOSPITALIZATION, AMBULATORY, ELECTROSHOCK_THERAPY,
  KETAMINE_INFUSION, EMERGENCY). No type filtering.
- Results are ordered **most-recent-first** (by `admissionDate DESC`, tie-broken by
  `id DESC`) so the current/most recent admission is at the top.
- Soft-deleted admissions (`deleted_at IS NOT NULL`) are excluded (enforced by the
  entity's `@SQLRestriction`).
- The endpoint must accept pagination parameters (`page`, `size`) with defaults `0` and
  `20`, consistent with the existing admissions list.
- The endpoint must enforce **patient-level visibility** before returning any data: it
  applies the same `patient:read` + doctor-assignment + psychologist-active-admission gate
  as the patient detail endpoint. A caller who could not open the patient's detail page
  must receive `403 Forbidden` here too (see Authorization).
- The endpoint must return `404 Not Found` if the patient ID does not exist (so the UI can
  distinguish "no such patient" from "patient with no admissions").
- The response reuses the existing `AdmissionListResponse` DTO (id, patient, triageCode,
  room, treatingPhysician, resident, admissionDate, dischargeDate, status, type,
  hasConsentDocument, createdAt) — no new DTO needed.
- The frontend must render an "Admissions history" section on the Patient Detail view that
  lists each admission with: admission date, discharge date (or an "Active" badge when
  null), type, room, and treating physician. Each row links to the admission detail view
  **only when the current user holds `admission:read`** (see Frontend Changes); otherwise
  the row is shown but non-navigable.
- The history section is visible to every user who can open the patient detail page (the
  endpoint is reached only after the same patient-access gate succeeds); it is **not**
  hidden behind an additional permission beyond what the detail page already requires.
- The section must surface its own **loading** and **error** states and provide
  **pagination controls** when the patient has more admissions than one page.
- When the patient has no admissions, the section shows an empty-state message rather than
  being hidden.

### Out of scope

- No editing/discharging from the history list (those actions stay on the admission detail
  view).
- No new permission is introduced.
- No changes to the global admissions list or its role scoping.
- No cross-patient "readmission analytics" / metrics.

---

## Acceptance Criteria / Scenarios

1. **Patient with multiple admissions** — Given a patient with 2 discharged admissions and
   1 active admission, when a user allowed to open that patient (e.g. ADMIN) calls
   `GET /api/v1/admissions/patients/{patientId}/admissions`, the system returns `200 OK`
   with all 3 admissions, ordered most-recent-first, the active one first.
2. **All types returned** — Given a patient with one HOSPITALIZATION and one
   ELECTROSHOCK_THERAPY admission, both are present in the response (no type filtering).
3. **Empty history** — Given a patient (whom the caller may access) with no admissions, the
   endpoint returns `200 OK` with an empty `content` array and `totalElements = 0` (not 404).
4. **Unknown patient** — Given a non-existent patient ID, the endpoint returns
   `404 Not Found`.
5. **Pagination** — Given a patient with 25 admissions, `?page=0&size=20` returns 20 items
   and `totalElements = 25`; `?page=1&size=20` returns the remaining 5.
6. **No per-row filtering once access is granted** — Given a user who *is* allowed to open
   the patient (e.g. ADMIN, or the assigned doctor, or a psychologist while the patient has
   an active admission), the endpoint returns **all** of that patient's admissions —
   including DISCHARGED ones and admissions where the caller is not the treating physician.
   That is, the doctor/psychologist predicates are NOT applied as per-admission row filters.
7. **Patient-access gate — doctor** — Given a **standalone `DOCTOR`** who is NOT assigned to
   the patient (cannot open the patient detail page today), the endpoint returns
   `403 Forbidden` and leaks no admission data — matching `GET /api/v1/patients/{id}`.
8. **Patient-access gate — psychologist** — Given a **`PSYCHOLOGIST`** (not ADMIN) and a
   patient with **no active admission**, the endpoint returns `403 Forbidden` — matching
   the patient detail page. (When the same patient has an active admission, the psychologist
   gets `200 OK` with the patient's full history including prior DISCHARGED admissions.)
9. **Authorization denied — no permission** — Given a user WITHOUT `patient:read`, the
   endpoint returns `403 Forbidden`.
10. **Soft-deleted excluded** — A soft-deleted admission for the patient does not appear in
    the history.
11. **Frontend render** — On the patient detail page, the "Admissions history" section lists
    each admission with admission date, discharge date / Active badge, type, room, and
    treating physician.
12. **Frontend navigation (with `admission:read`)** — For a user holding `admission:read`,
    clicking an admission row navigates to `/admissions/:id`.
13. **Frontend navigation (without `admission:read`)** — For a user lacking `admission:read`,
    the rows render but are non-navigable (no broken link into a 403).
14. **Frontend loading & error states** — The section shows a loading indicator while
    fetching and a recoverable error state (via `useErrorHandler`) on failure.
15. **Frontend empty state** — For a patient with no admissions, the section shows the
    empty-state message (e.g. "No admissions recorded").
16. **Datetime formatting** — Admission and discharge timestamps render via `formatDateTime`
    (`dd/MM/yyyy - HH:mm`); an active admission shows no discharge date.

---

## Non-Functional Requirements

- Performance: List response time < 200 ms; the query is indexed on `patient_id`
  (`idx_admissions_patient_id` already exists). Relations are fetched eagerly via a single
  `LEFT JOIN FETCH` query to avoid N+1, mirroring the existing `findAll*WithRelations`
  queries.
- Security: Enforce the patient-access gate (`patient:read` + doctor-assignment +
  psychologist-active-admission) **before** querying admissions; no raw/dynamic SQL — use a
  parameterized JPQL query. No patient data leakage across patients (query is always bound
  to the path `patientId`) and no direct-access bypass of the patient detail visibility
  rules.
- Reliability: Read-only endpoint; no audit-log entry required (consistent with the
  existing read-only admissions list, which is not audited).

---

## Date / Time Conformance

Confirm the feature follows the platform-wide date/time standard documented in `CLAUDE.md`
§ "Date / Time Formatting" and `docs/architecture/ARCHITECTURE.md` § "Date and Time
Handling".

- [x] Backend date-only fields use `LocalDate` + `DATE`; event timestamps use
  `LocalDateTime` + `TIMESTAMP`. No `String`-stored dates, no `TIMESTAMPTZ`. *(Reuses
  existing `admissionDate` / `dischargeDate` `LocalDateTime` columns — no new columns.)*
- [x] All frontend date/time rendering goes through `formatDate` / `formatTime` /
  `formatDateTime` from `@/utils/format`. The history rows use **`formatDateTime`** for the
  `admissionDate` / `dischargeDate` timestamps — these are `LocalDateTime` event timestamps,
  and the existing admissions UI (`AdmissionsListSection.vue`) renders them with
  `formatDateTime` (`dd/MM/yyyy - HH:mm`); this view matches that for consistency. (Use
  `getRelativeTime` for any "ago" label, also matching `AdmissionsListSection`.) The time
  component is intentionally **kept**, not discarded.
- [x] No `<DatePicker>` is introduced by this feature (read-only view).
- [x] No `Date → API string` conversions are introduced.
- [x] Relative time strings (if used) use `getRelativeTime` from
  `@/composables/useRelativeTime`.

---

## API Contract

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| GET | `/api/v1/admissions/patients/{patientId}/admissions` | - | `PageResponse<AdmissionListResponse>` | `patient:read` + patient-access gate | List all admissions for a patient (most-recent-first) once the caller is allowed to open that patient |

> Auth is two-step: `@PreAuthorize("hasAuthority('patient:read')")` on the method, then the
> same patient-level access gate as `GET /api/v1/patients/{id}` inside the service. Returns
> `403` if the caller cannot open the patient, `404` if the patient does not exist.
>
> Endpoint placement: nested under the existing `/api/v1/admissions/patients/{patientId}`
> route in `AdmissionController` (which already returns the patient *summary*). This keeps
> all patient-admission lookups on the admissions controller and reuses
> `AdmissionListResponse`. (Alternative `/api/v1/patients/{id}/admissions` was considered
> but rejected to avoid spreading admission concerns into `PatientController`.)

### Request/Response Examples

```jsonc
// GET /api/v1/admissions/patients/42/admissions?page=0&size=20 - Response
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 310,
        "patient": { "id": 42, "firstName": "María", "lastName": "López", "hasActiveAdmission": true },
        "triageCode": { "id": 2, "code": "II", "color": "#FF0000", "description": "Emergencia" },
        "room": { "id": 7, "number": "204", "type": "INDIVIDUAL" },
        "treatingPhysician": { "id": 9, "firstName": "Juan", "lastName": "Pérez", "username": "jperez" },
        "resident": { "id": 15, "firstName": "Ana", "lastName": "Ruiz", "username": "aruiz" },
        "admissionDate": "2026-05-20T14:30:00",
        "dischargeDate": null,
        "status": "ACTIVE",
        "type": "HOSPITALIZATION",
        "hasConsentDocument": true,
        "createdAt": "2026-05-20T14:31:02"
      },
      {
        "id": 188,
        "patient": { "id": 42, "firstName": "María", "lastName": "López", "hasActiveAdmission": true },
        "triageCode": null,
        "room": null,
        "treatingPhysician": { "id": 9, "firstName": "Juan", "lastName": "Pérez", "username": "jperez" },
        "resident": { "id": 15, "firstName": "Ana", "lastName": "Ruiz", "username": "aruiz" },
        "admissionDate": "2026-01-10T09:00:00",
        "dischargeDate": "2026-01-18T11:00:00",
        "status": "DISCHARGED",
        "type": "AMBULATORY",
        "hasConsentDocument": false,
        "createdAt": "2026-01-10T09:02:10"
      }
    ],
    "page": { "number": 0, "size": 20, "totalElements": 2, "totalPages": 1 }
  }
}
```

---

## Database Changes

**None.** No new entities, columns, or migrations are required.

The capability already exists in the schema:

- `Admission` is `@ManyToOne` → `Patient` (`admissions.patient_id BIGINT NOT NULL`).
- `idx_admissions_patient_id` already indexes `patient_id` for efficient lookup.
- Discharge already flips `status ACTIVE → DISCHARGED` and sets `dischargeDate`, naturally
  accumulating history; `createAdmission` blocks a second concurrent ACTIVE admission via
  `existsActiveByPatientId`, so at most one row per patient is ACTIVE.

### Index Requirements

- [x] `deleted_at` — already present (`idx_admissions_deleted_at`); soft-delete handled by
  `@SQLRestriction`.
- [x] Foreign key `patient_id` — already indexed (`idx_admissions_patient_id`).
- [x] Sort column `admission_date` — **already indexed** (`idx_admissions_admission_date`,
  V023 line 26). Between that and the `patient_id` index, the filter-and-sort is covered; no
  new index required.

---

## Backend Changes

### Repository — `AdmissionRepository.kt`

Add a patient-scoped paged query that fetches the same relations as the existing list
queries and orders most-recent-first:

```kotlin
@Query(
    """
    SELECT a FROM Admission a
    LEFT JOIN FETCH a.patient
    LEFT JOIN FETCH a.triageCode
    LEFT JOIN FETCH a.room
    LEFT JOIN FETCH a.treatingPhysician
    LEFT JOIN FETCH a.resident
    LEFT JOIN FETCH a.consentDocument
    WHERE a.patient.id = :patientId
    ORDER BY a.admissionDate DESC, a.id DESC
    """,
    countQuery = "SELECT COUNT(a) FROM Admission a WHERE a.patient.id = :patientId",
)
fun findByPatientIdWithRelations(
    @Param("patientId") patientId: Long,
    pageable: Pageable,
): Page<Admission>
```

### Service — `AdmissionService.kt`

Add a read-only method that (1) enforces the **same patient-access gate** as the patient
detail page, then (2) returns the full set for that patient. The access decision is
delegated to the patient layer so it cannot drift from `GET /api/v1/patients/{id}`:

```kotlin
@Transactional(readOnly = true)
fun findAdmissionsByPatient(
    patientId: Long,
    doctorId: Long?,             // resolveDoctorId(currentUser) — non-null only for standalone DOCTOR
    activeAdmissionsOnly: Boolean, // resolveActiveAdmissionsOnly(currentUser) — true only for PSYCHOLOGIST
    pageable: Pageable,
): Page<AdmissionListResponse> {
    // Patient-access GATE: 404 if patient does not exist; 403 (AccessDenied) if this caller
    // cannot open the patient — identical rules to PatientService.getPatient(...).
    // Prefer extracting an `assertPatientAccessible(patientId, doctorId, activeAdmissionsOnly)`
    // from getPatient() and calling it from both places (single source of truth).
    patientService.assertPatientAccessible(patientId, doctorId, activeAdmissionsOnly)

    // Once access is granted, return ALL admissions for the patient — no per-row filtering.
    return admissionRepository.findByPatientIdWithRelations(patientId, pageable)
        .map { AdmissionListResponse.from(it) }
}
```

> The `doctorId` / `activeAdmissionsOnly` arguments are used **only** for the access gate,
> never as predicates on the admission query — see Authorization.

### Controller — `AdmissionController.kt`

```kotlin
@GetMapping("/patients/{patientId}/admissions")
@PreAuthorize("hasAuthority('patient:read')")
fun listPatientAdmissions(
    @PathVariable patientId: Long,
    @PageableDefault(size = 20) pageable: Pageable,
    @AuthenticationPrincipal currentUser: CustomUserDetails,
): ResponseEntity<ApiResponse<PageResponse<AdmissionListResponse>>> {
    // Same visibility rules as the patient DETAIL page, applied as a one-time access GATE
    // (not as per-admission row filters). resolveDoctorId / resolveActiveAdmissionsOnly are
    // the existing helpers already present on this controller.
    val admissions = admissionService.findAdmissionsByPatient(
        patientId = patientId,
        doctorId = resolveDoctorId(currentUser),
        activeAdmissionsOnly = resolveActiveAdmissionsOnly(currentUser),
        pageable = pageable,
    )
    return ResponseEntity.ok(ApiResponse.success(PageResponse.from(admissions)))
}
```

---

## Frontend Changes

### Components

| Component | Location | Description |
|-----------|----------|-------------|
| `PatientAdmissionsHistory.vue` | `web/src/components/patients/` | New section: paged table of the patient's admissions, most-recent-first. Shows admission date + discharge date via `formatDateTime` (or an "Active" `Tag` when `dischargeDate` is null), type (reuse `AdmissionTypeBadge`), room, treating physician. **Loading** state (`ProgressSpinner`), **error** state (via `useErrorHandler`), **`Paginator`** when `totalPatientAdmissions` exceeds the page size, and an **empty state** when none. Each row links to `/admissions/:id` **only if `authStore.hasPermission('admission:read')`** — otherwise the row is non-navigable (no cursor/click). |
| `PatientDetailView.vue` | `web/src/views/patients/` | Add the new `PatientAdmissionsHistory` section/card below the existing demographic/emergency-contact cards. |

> The existing `AdmissionsListSection.vue` is tightly coupled to the global
> `admissionStore.admissions` + `fetchAdmissions` and the grouping-preferences store, so a
> dedicated lightweight component is preferred over overloading it. `AdmissionTypeBadge`
> and the status `Tag` styling can be reused directly.

### Pinia Store

| Store | Location | Change |
|-------|----------|--------|
| `useAdmissionStore` | `web/src/stores/admission.ts` | Add `fetchPatientAdmissions(patientId, page = 0, size = 20)` returning a `PageResponse<AdmissionListItem>`. Store the result in a dedicated `patientAdmissions` ref (do **not** overwrite the global `admissions` ref used by the dashboard / list views). |

```ts
const patientAdmissions = ref<AdmissionListItem[]>([])
const totalPatientAdmissions = ref(0)

async function fetchPatientAdmissions(patientId: number, page = 0, size = 20): Promise<void> {
  const response = await api.get<ApiResponse<PageResponse<AdmissionListItem>>>(
    `/v1/admissions/patients/${patientId}/admissions`,
    { params: { page, size } }
  )
  if (response.data.success && response.data.data) {
    patientAdmissions.value = response.data.data.content
    totalPatientAdmissions.value = response.data.data.page.totalElements
  }
}
```

### Routes

No new routes. The history is rendered inline on the existing `patient-detail` route
(`/patients/:id`). Rows link to the existing `admission-detail` route (`/admissions/:id`).

> ⚠️ The `admission-detail` route / `GET /api/v1/admissions/{id}` requires
> **`admission:read`**, which is **not held by every `patient:read` role**. Confirmed
> holders: ADMIN, DOCTOR, NURSE, CHIEF_NURSE, PSYCHOLOGIST, AUXILIARY_NURSE
> (V015/V025/V071/V117). **`ADMINISTRATIVE_STAFF`** holds `patient:read` but its
> `admission:read` grant is **unconfirmed** and must be verified during implementation. To
> stay safe regardless, gate the row link on `authStore.hasPermission('admission:read')`
> (see component note) so a user without it sees the history but is not sent into a 403.

### Validation (Zod Schemas)

None — read-only feature, no forms.

### i18n

Add keys (en + es) under a `patient.admissionsHistory.*` namespace, e.g.:

- `patient.admissionsHistory.title` → "Admissions history" / "Historial de ingresos"
- `patient.admissionsHistory.empty` → "No admissions recorded" / "Sin ingresos registrados"
- `patient.admissionsHistory.active` → "Active" / "Activo"
- column headers: `admissionDate`, `dischargeDate`, `type`, `room`, `treatingPhysician`

---

## Implementation Notes

- This follows the existing `AdmissionController` / `AdmissionService` /
  `AdmissionRepository` pattern; the new query mirrors `findAllByStatusWithRelations`
  (same `LEFT JOIN FETCH` set) but filters by `patient.id` and adds an explicit
  `ORDER BY`.
- Reuse `AdmissionListResponse` — no new DTO. The `patient` block is redundant on a
  patient-scoped call but keeps the DTO shared with the global list (cheap, already
  fetched).
- The endpoint sits beside the existing `GET /api/v1/admissions/patients/{patientId}`
  (patient summary). Be careful not to create a path ambiguity — the new path has the
  `/admissions` suffix so it is unambiguous.
- Keep the **gate-not-filter** decision explicit in a code comment on the controller method:
  `resolveDoctorId` / `resolveActiveAdmissionsOnly` are passed in, but only to authorize
  *access to the patient* — they must NOT be added as predicates on the admission query.
  Every other admission read path uses them as row filters, so a future maintainer might
  otherwise wire them the wrong way in either direction (drop them → leak; apply as filter →
  hide valid history).
- `PatientService.getPatient` currently inlines the access check
  (`isPatientAssignedToDoctor` + active-admission throw). Extracting an
  `assertPatientAccessible(patientId, doctorId, activeAdmissionsOnly)` and having both
  `getPatient` and `findAdmissionsByPatient` call it keeps the two endpoints from drifting.

---

## Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| **Direct-access data leak** — endpoint omits the patient-access gate, letting an unassigned standalone DOCTOR (or a psychologist with no active-admission patient) fetch a patient's history by ID, bypassing the detail-page visibility rules | Medium | **High** | Mandatory patient-access gate reusing `PatientService.assertPatientAccessible`; AC #7/#8 assert `403`; integration tests for unassigned-doctor and no-active-admission-psychologist |
| A maintainer wires `resolveDoctorId` / `resolveActiveAdmissionsOnly` as **row filters** (as on the global list), hiding valid history (e.g. a psychologist losing the patient's prior DISCHARGED admissions) | Medium | Medium | Explicit gate-not-filter comment on controller + service; AC #6 asserts a permitted caller sees ALL admissions incl. DISCHARGED |
| Path collision between `/patients/{patientId}` (summary) and `/patients/{patientId}/admissions` (history) | Low | Medium | Distinct suffix; covered by a controller test hitting both |
| N+1 query when listing admissions with relations | Low | Medium | Single `LEFT JOIN FETCH` query (mirrors existing list queries); verified by Hibernate statistics in integration test if needed |
| Frontend accidentally overwrites the global `admissions` ref (used by dashboard/list), causing cross-view state bleed | Medium | Medium | Use a dedicated `patientAdmissions` ref in the store; unit test asserts the global ref is untouched |
| Row link sends an `ADMINISTRATIVE_STAFF` user (no confirmed `admission:read`) into a 403 on the admission detail page | Medium | Low | Confirm the `admission:read` grant during implementation; gate the row link on `authStore.hasPermission('admission:read')` regardless |
| Large pages on a high-volume patient | Very Low | Low | Paginated (default size 20); existing `patient_id` + `admission_date` indexes cover filter and sort |

---

## QA Checklist

### Backend
- [ ] Patient-scoped query returns all admissions (all statuses + all types), most-recent-first
- [ ] Endpoint enforces the patient-access gate (`patient:read` + doctor-assignment +
  psychologist-active-admission), reusing `PatientService` so it cannot drift from the detail page
- [ ] Once access is granted, NO per-admission row filtering (assigned doctor / psychologist
  see all of the patient's admissions, incl. DISCHARGED)
- [ ] `403` for a caller who cannot open the patient (unassigned standalone doctor;
  psychologist + patient with no active admission); `403` for a caller without `patient:read`
- [ ] Existing entity `Admission` already extends `BaseEntity` and has `@SQLRestriction` (no change)
- [ ] `AdmissionListResponse` DTO reused (no entity exposure)
- [ ] `404` for unknown patient; empty page (not 404) for an accessible patient with no admissions
- [ ] Pagination parameters honored (defaults `page=0`, `size=20`)
- [ ] Unit tests (service) written and passing
- [ ] Integration tests (controller + Testcontainers) written and passing
- [ ] Detekt passes (no new violations)
- [ ] OWASP dependency-check passes

### Frontend
- [ ] `PatientAdmissionsHistory.vue` created and rendered on `PatientDetailView`
- [ ] `fetchPatientAdmissions` added to the admission store with a dedicated `patientAdmissions` ref
  (global `admissions` ref left untouched)
- [ ] Timestamps rendered via `formatDateTime`; active admission shows "Active" badge, no discharge date
- [ ] Rows link to `/admissions/:id` only when the user holds `admission:read`; otherwise non-navigable
- [ ] Loading state, error state (via `useErrorHandler`), and `Paginator` present
- [ ] Empty state shown when patient has no admissions
- [ ] ESLint/oxlint passes
- [ ] i18n keys added (en + es) for all user-facing text
- [ ] Unit tests (Vitest) written and passing

### E2E Tests (Playwright)

Covered by `web/e2e/patient-admissions-history.spec.ts` (backend mocked via `page.route`;
the server-side patient-access gate is simulated by the mocked status of
`GET /api/v1/patients/{id}` — 200 = openable, 403 = denied).

- [x] **Happy path** — log in as ADMIN, open a patient that has ≥2 admissions (1 active +
  1 discharged), assert the history section lists both most-recent-first with correct
  dates/type/room/physician
- [x] **Navigation (with `admission:read`)** — click a history row, assert navigation to the
  admission detail view
- [x] **Navigation suppressed (without `admission:read`)** — as a role lacking
  `admission:read` (ADMINISTRATIVE_STAFF), assert rows render but are non-navigable
- [x] **Empty state** — open an accessible patient with no admissions, assert the empty-state message
- [x] **Patient-access allowed** — a CHIEF_NURSE with `patient:read` sees the full history
- [x] **Patient-access denied — doctor** — a standalone DOCTOR who is NOT assigned to the
  patient cannot reach the history (the patient is not openable; `403` → redirect to the
  patient list), matching the detail page
- [x] **Patient-access denied — psychologist** — a PSYCHOLOGIST viewing a patient with no
  active admission is denied; with an active admission, sees the full history incl. prior
  DISCHARGED admissions
- [x] **Loading & error handling** — navigating to a non-existent patient surfaces the
  not-found flow; a 500 on the history fetch leaves the section recoverable (no stale rows)

### General
- [ ] API contract documented (this file)
- [ ] No DB migration required (verified)
- [ ] Feature documentation updated (`CLAUDE.md` implemented-features list)
- [ ] Reviewed by project owner

---

## Documentation Updates Required

### Must Update

- [ ] **[CLAUDE.md](../../CLAUDE.md)**
  - Add "Patient Admissions History" to the Implemented Features (backend + frontend)
    lists once shipped. Note the `GET /api/v1/admissions/patients/{patientId}/admissions`
    endpoint, that it reuses the patient-detail access gate (`patient:read` +
    doctor-assignment + psychologist-active-admission) and then returns the patient's full
    admission set without per-row filtering.
- [ ] **[ARCHITECTURE.md](../architecture/ARCHITECTURE.md)** (if exists)
  - No architectural pattern change; update only if endpoint inventory is maintained there.

### Review for Consistency

- [ ] **[patient-admission.md](patient-admission.md)** — cross-link this history feature
  from the admission feature doc.

### Code Documentation
- [ ] **`AdmissionController.listPatientAdmissions`** — KDoc explaining the gate-not-filter
  decision (the doctor/psychologist predicates authorize patient access only).
- [ ] **`AdmissionService.findAdmissionsByPatient`** — document the patient-access gate
  (403/404 semantics) and that no per-row filtering is applied once access is granted.
- [ ] **`PatientService.assertPatientAccessible`** (if extracted) — document that it is the
  single source of truth shared with `getPatient`.

---

## Related Docs/Commits/Issues

- Related feature: [`patient-admission.md`](patient-admission.md)
- Branch: `feature/patient-admissions-history`
- Design discussion: this Claude session (2026-06-01)

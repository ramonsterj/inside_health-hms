# Feature: Discharge Protection (Read-Only Past Admissions)

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-06-01 | @ramonster | Initial spec. Consolidates the discharge-immutability rule and extends it across the **entire** admission record (clinical history, medical orders + state transitions + result documents, psychotherapy activities, consent + admission documents, consulting physicians) — previously only progress notes, nursing notes, and vital signs were protected. |
| 1.1 | 2026-06-01 | @ramonster | Close the by-state dashboard gap: `MedicalOrderListItemResponse` now carries `admissionStatus`, and `/medical-orders` hides per-row action buttons for discharged admissions. |

---

## Overview

Once a patient's admission is **discharged** (`status = DISCHARGED`), that admission's record
becomes **immutable**: nobody — including ADMIN — may create, edit, delete, upload, or
otherwise mutate any clinical or administrative record attached to it. A discharged
admission is a **past admission** and is read-only.

This is the single source of truth for the platform-wide "past admissions are read-only"
policy. Individual module docs ([medical-psychiatric-record.md](./medical-psychiatric-record.md),
[nursing-module.md](./nursing-module.md), [psychotherapeutic-activities.md](./psychotherapeutic-activities.md),
[multi-document-upload.md](./multi-document-upload.md), [patient-admission.md](./patient-admission.md))
defer to this rule.

### Why

Medical records for discharged patients must be immutable for **legal and compliance**
reasons (the record reflects the care delivered during the stay and may not be altered
afterward). Operationally, the clinical events a record captures cannot happen after
discharge: you cannot take a discharged patient's vitals, administer their medication, or
register psychotherapy for them. Per the customer decision (2026-06-01):

- **Lab/imaging results never arrive after discharge** → result-document upload on medical
  orders is blocked too (no exception).
- **Medical orders placed after discharge are not the hospital's responsibility** → order
  creation and all order state transitions are blocked after discharge.

There are **no exceptions**: strict immutability.

---

## Authorization / Behaviour

Discharge protection is a **gate applied before any write**, independent of role or
permission. A caller who holds the relevant `*:create` / `*:update` / `*:delete` permission
still receives **`400 Bad Request`** when the parent admission is discharged. Reads are
never affected — the full record stays viewable forever.

Error contract (reused everywhere):

- i18n key: **`error.admission.discharged.records`**
- EN: "Cannot modify records for discharged admissions"
- ES: "No se pueden modificar registros de admisiones dadas de alta"
- `MessageService.errorAdmissionDischargedRecords()` → thrown as `BadRequestException` (→ 400)

> Two pre-existing variants remain for their specific surfaces and are unchanged:
> `error.admission.update.discharged` (editing admission fields), `error.admission.already.discharged`
> (double discharge), and `error.medication.admission.discharged` (medication administration).
> All express the same immutability principle.

---

## Coverage Matrix

"Blocked" = service-layer guard throws `400` on a discharged admission. "Read" = always
allowed (history stays viewable). ✅ = newly added by this feature; ◼ = already enforced
before this feature.

| Domain | Operation | Blocked on discharge |
|--------|-----------|----------------------|
| Admission | update fields / change consulting-physician slot | ◼ (`error.admission.update.discharged`) |
| Admission | discharge (again) | ◼ (`error.admission.already.discharged`) |
| Admission | **upload consent document** | ✅ |
| Admission | **add / remove consulting physician** | ✅ |
| Admission documents | **upload / delete** | ✅ |
| Clinical history | **create / update** | ✅ |
| Progress notes | create / update | ◼ |
| Nursing notes | create / update | ◼ |
| Vital signs | create / update | ◼ |
| Medication administration | create | ◼ (`error.medication.admission.discharged`) |
| Medical orders | **create / update** (incl. lab provider + line items) | ✅ |
| Medical orders | **authorize / emergency-authorize / reject** | ✅ |
| Medical orders | **mark-in-progress / discontinue** | ✅ |
| Medical order result documents | **upload / delete** | ✅ |
| Psychotherapy activities | **create / delete** | ✅ |
| **All read/list/download** endpoints | — | Never blocked |

---

## Functional Requirements

- Every mutating endpoint on data owned by an admission MUST verify the admission's status
  **before** performing the write, and return `400` with `error.admission.discharged.records`
  (or the surface's existing discharge variant) when the admission is discharged.
- The guard is enforced **at the service layer**, not only the UI — it holds for ADMIN and for
  any client calling the API directly.
- Read, list, download, thumbnail, and export operations are **never** blocked by discharge.
- The frontend MUST hide all write affordances (create / edit / delete / upload / administer /
  authorize / reject / discontinue / mark-in-progress) for a discharged admission, and surface
  a clear read-only banner so the absence of buttons is explained rather than mysterious.

### Out of scope

- **Admission soft-delete** (`admission:delete`, ADMIN-only) remains available on a discharged
  admission: it removes an erroneous record rather than mutating record *content*. It is a
  distinct administrative capability, not a clinical write.
- **PDF export** of a discharged admission remains available (read-only render).
- **Cross-admission "Medical Orders by State" dashboard** (`/medical-orders`): its row DTO
  (`MedicalOrderListItemResponse`) carries the parent admission's `admissionStatus`, and the
  view's per-row action predicates (`isAuthorizableRow`, `isEmergencyAuthorizeRow`,
  `isMarkInProgressRow`, `isDiscontinueRow`, `isUploadResultRow`) all gate on
  `admissionStatus === ACTIVE`. Discharged admissions' orders remain **listed** (reads are never
  blocked) but expose no action buttons. The backend guard is the backstop.

---

## Acceptance Criteria / Scenarios

1. **Clinical history** — Creating or updating a clinical history on a discharged admission
   returns `400`; reading it returns `200`.
2. **Medical order create** — Creating a medical order on a discharged admission returns `400`.
3. **Medical order transitions** — `authorize`, `emergency-authorize`, `reject`,
   `mark-in-progress`, and `discontinue` on a discharged admission's order each return `400`.
4. **Medical order documents** — Uploading or deleting a result document on a discharged
   admission's order returns `400` (lab results never arrive post-discharge).
5. **Psychotherapy activities** — Creating or deleting an activity on a discharged admission
   returns `400`.
6. **Consent / admission documents** — Uploading a consent document, or uploading/deleting an
   admission document, on a discharged admission returns `400`.
7. **Consulting physicians** — Adding or removing a consulting physician on a discharged
   admission returns `400`.
8. **Reads unaffected** — All list/get/download endpoints for the above return `200` on a
   discharged admission.
9. **ADMIN not exempt** — An ADMIN performing any of the above writes still receives `400`.
10. **UI read-only** — On the admission detail page of a discharged admission, the Medical
    Record section shows a read-only banner and exposes **no** create/edit/delete/upload/
    authorize/administer buttons in any section; the Edit and Discharge buttons in the page header
    are also hidden (pre-existing).

---

## Backend Changes

All guards follow the established `validateAdmissionActive(admission)` pattern (throws
`BadRequestException(messageService.errorAdmissionDischargedRecords())` when
`admission.isDischarged()`), mirroring `ProgressNoteService` / `NursingNoteService` /
`VitalSignService`.

| Service | Methods guarded |
|---------|-----------------|
| `ClinicalHistoryService` | `createClinicalHistory`, `updateClinicalHistory` |
| `MedicalOrderService` | `createMedicalOrder`, `updateMedicalOrder`, `discontinueMedicalOrder`, `authorize`, `emergencyAuthorize`, `reject`, `markInProgress` (the last four route through `loadAuthorizableOrder` / `validateAdmissionActive(admissionId)`) |
| `MedicalOrderDocumentService` | `uploadDocument`, `deleteDocument` |
| `PsychotherapyActivityService` | `createActivity`, `deleteActivity` |
| `AdmissionService` | `uploadConsentDocument`, `addConsultingPhysician`, `removeConsultingPhysician` |
| `AdmissionDocumentService` | `uploadDocument`, `deleteDocument` (gains a `MessageService` dependency for the localized error) |

Read methods continue to use the lightweight `verifyAdmissionExists` (existence only).

## Frontend Changes

`MedicalRecordHub.vue` threads `admissionStatus` into every section that can author content and
renders a read-only banner (`medicalRecord.dischargedReadOnly`) when discharged. Each section
component derives `isActive = admissionStatus === AdmissionStatus.ACTIVE` and ANDs it into its
write computeds:

- `ClinicalHistoryView.vue` — `canCreate`, `canUpdate`
- `ProgressNoteList.vue` — `canCreate` (per-note edit already gated by server `canEdit`)
- `MedicalOrderList.vue` — `canCreate`, `canUpdate`; passes `:admissionActive="isActive"` to
  `MedicalOrderCard.vue`, which gates `canAuthorize`, `canReject`, `canEmergencyAuthorize`,
  `canMarkInProgress`, `canDiscontinue`, `canUploadDocument`, `canDeleteDocument`, `canAdminister`
- `PsychotherapyActivityList.vue` — `canCreate`, `canDelete`
- `DocumentList.vue` — `canUpload`, `canDelete`

`NursingNoteList.vue` and `VitalSignTable.vue` already followed this pattern; the
`ConsultingPhysiciansPanel` is gated via the parent's `canUpdateAdmission` (already
discharge-aware).

### i18n

- `medicalRecord.dischargedReadOnly` (en + es) — read-only banner copy.

## Tests

- **Backend (integration, Testcontainers):** discharge-protection tests added to
  `ClinicalHistoryControllerTest`, `MedicalOrderControllerTest` (create/update/discontinue/
  authorize), `MedicalOrderDocumentControllerTest` (upload/delete),
  `PsychotherapyActivityControllerTest` (create/delete), `AdmissionConsultingPhysicianControllerTest`
  (add/remove), `AdmissionDocumentControllerTest` (consent upload). Each asserts `400` after
  `dischargeAdmission(...)`. Progress note / nursing note / vital sign coverage pre-existed.
- **Frontend (Vitest):** `web/src/components/medical-record/dischargeProtection.spec.ts` asserts
  the create/upload affordance is present when ACTIVE and absent when DISCHARGED for
  `ProgressNoteList`, `ClinicalHistoryView`, `DocumentList`, and `PsychotherapyActivityList`.

---

## Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| A future mutating endpoint is added without the discharge guard | Medium | High | This doc is the canonical checklist; the `validateAdmissionActive` pattern is uniform and easy to copy. Code review should verify new write paths consult it. |
| "Medical Orders by State" dashboard shows action buttons for a discharged admission's order | — | — | **Resolved (v1.1):** `MedicalOrderListItemResponse.admissionStatus` added; row actions gate on it. Backend guard remains the backstop. |
| A maintainer mistakes the read paths for write paths and over-blocks reads | Low | Medium | Reads deliberately use `verifyAdmissionExists`; the guard helpers are only called from mutations. |

---

## Documentation Updates

- [x] **This file** — canonical policy.
- [x] **[medical-psychiatric-record.md](./medical-psychiatric-record.md)** — note clinical
  history + medical orders are now discharge-protected (previously only `update` was
  admin-restricted).
- [x] **[nursing-module.md](./nursing-module.md)** — correct the "Why block writes" note that
  previously said clinical history and medical orders were *not* blocked.
- [x] **[CLAUDE.md](../../CLAUDE.md)** — add Discharge Protection to the implemented-features list.

---

## Related Docs

- [patient-admission.md](./patient-admission.md) — discharge flow
- [patient-admissions-history.md](./patient-admissions-history.md) — the past-admissions view that
  surfaced this gap
- [medical-psychiatric-record.md](./medical-psychiatric-record.md), [nursing-module.md](./nursing-module.md)

# Feature: Nursing Roles Split (Graduate vs Auxiliary)

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-05-28 | @author | Initial draft. Introduces `AUXILIARY_NURSE` as a new restricted nursing role and clarifies that the existing `NURSE` role represents the graduate nurse (enfermero graduado). Captures customer feedback from 2026-05-26. |

---

## Overview

Today the system has one operational nursing role (`NURSE`) plus a supervisory role (`CHIEF_NURSE`). Customer feedback is that this conflates two distinct hospital roles:

- **Enfermero graduado (graduate nurse)** — administers medications, executes / marks-in-progress medical orders, records vital signs, writes nursing notes.
- **Enfermero auxiliar (auxiliary nurse)** — can record vital signs and write nursing notes only. May **not** administer medications, may **not** mark medical orders in progress, may **not** upload result documents.

This spec introduces `AUXILIARY_NURSE` as a new system role. The existing `NURSE` role is retained unchanged and is hereby clarified to mean the *graduate* nurse. `CHIEF_NURSE` is unaffected.

The reason we are not renaming `NURSE → GRADUATE_NURSE` is purely pragmatic: `NURSE` is the most heavily granted role in the codebase (V045, V066, V073, V091, V096, V097, V104 …) and renaming it would touch every nursing-adjacent migration and seed file without changing behavior. The matrix and i18n labels carry the "graduate nurse" semantics where it matters to humans.

---

## Use Case / User Story

1. As a **chief nurse**, I want to assign auxiliary nurses to a shift so that they can register vital signs and nursing observations without being able to dispense medication or mark lab orders in progress, matching their professional scope.
2. As an **auxiliary nurse**, I want to log into the system and see the same nursing kardex / patient list a graduate nurse sees, so that I have context for the patient I am caring for — but the dispense / mark-in-progress / upload buttons must not be available to me.
3. As a **graduate nurse**, my workflow does not change: I keep every permission `NURSE` has today.
4. As an **administrator**, I want existing `NURSE` user accounts to keep working unchanged after the migration, so we do not break the production deployment when the new role lands.

---

## Authorization / Role Access

### Permission grants (defaults)

| Permission | NURSE (graduate) | AUXILIARY_NURSE | CHIEF_NURSE | Notes |
|---|---|---|---|---|
| `nursing-note:read` | ✓ | ✓ | ✓ | Both see the same kardex. |
| `nursing-note:create` | ✓ | ✓ | ✓ | Auxiliary writes notes. |
| `nursing-note:update` | — | — | — | ADMIN-only per V096. |
| `vital-sign:read` | ✓ | ✓ | ✓ | |
| `vital-sign:create` | ✓ | ✓ | ✓ | Auxiliary records vitals. |
| `vital-sign:update` | — | — | — | ADMIN-only per V097. |
| `progress-note:read` | ✓ | ✓ | ✓ | Read-only context. |
| `progress-note:create` | ✓ | — | ✓ | **Auxiliary does NOT author evoluciones** — only graduate / chief / doctor / admin. |
| `medication-administration:create` | ✓ | — | ✓ | **Restricted from auxiliary.** |
| `medication-administration:read` | ✓ | ✓ | ✓ | Auxiliary can see what was administered. |
| `medical-order:read` | ✓ | ✓ | ✓ | |
| `medical-order:mark-in-progress` | ✓ | — | ✓ | **Restricted from auxiliary.** |
| `medical-order:upload-document` | ✓ | — | ✓ | **Restricted from auxiliary** (V073 already excludes CHIEF_NURSE; only NURSE / DOCTOR / ADMIN can upload today — the auxiliary stays excluded). |
| `clinical-history:read` | ✓ | ✓ | ✓ | |
| `patient:read`, `admission:read`, `admission:update`, `room:occupancy-view` | inherit current NURSE grants | inherit current NURSE grants | inherit | The kardex/patient/admission visibility surface is intentionally the same so the auxiliary can do her job. |

### Explicit denials (rejected by service-layer guard even if a custom role somehow grants the underlying permission to AUXILIARY_NURSE)

- `MedicationAdministrationService.create` rejects with `error.nursing.auxiliary.denied` when the calling user's only nursing role is `AUXILIARY_NURSE` and the user is not also `NURSE` / `CHIEF_NURSE` / `DOCTOR` / `ADMIN`.
- `MedicalOrderService.markInProgress` and `MedicalOrderDocumentService.upload` apply the same guard.

This mirrors the way `PsychotherapyActivityService.create` already enforces the `PSYCHOLOGIST` role name in addition to the permission check (see `roles-functionality-matrix.md` § Mismatches and Gaps).

### Frontend gating

- `NursingKardexView` continues to load for both `NURSE` and `AUXILIARY_NURSE` (and `CHIEF_NURSE`).
- Quick-action buttons (`Administer`, `Mark in progress`, `Upload result`) on kardex cards and on `MedicalOrderCard` must be hidden when the user has *only* the `AUXILIARY_NURSE` role. The check uses `auth.hasRole('NURSE') || auth.hasRole('CHIEF_NURSE') || auth.hasRole('DOCTOR') || auth.hasRole('ADMIN')` — purely additive; if the user holds any of those, the auxiliary restriction does not apply.
- The `/dashboard → /nursing-kardex` redirect already in `router/index.ts` gains `AUXILIARY_NURSE` in its allow-list.
- Side-nav: pharmacy section stays hidden from auxiliary (no `medication:read` grant). Medications-by-state dashboard remains gated by `medical-order:read`, which the auxiliary holds — but the action buttons inside are hidden as above.

---

## Functional Requirements

- A new system role `AUXILIARY_NURSE` exists in the `roles` table with `is_system = TRUE`.
- The role is granted exactly the permissions listed above. Existing `NURSE` users keep every permission they have today; no row in `user_roles` is modified.
- Trying to call any of the three denied endpoints (administer medication, mark-in-progress, upload result) as a user whose only nursing role is `AUXILIARY_NURSE` returns **403** with body code `error.nursing.auxiliary.denied`.
- The seeded "test users" set (`V016__add_clinical_test_users.sql` and the dev seed) gains one `aux_nurse` user wired to `AUXILIARY_NURSE` so QA can exercise the restricted flow without manually granting the role.
- The roles/functionality matrix (`docs/roles-functionality-matrix.md`) is updated to document the new role alongside `NURSE` and `CHIEF_NURSE`.

---

## Acceptance Criteria / Scenarios

- **AC-1 — Grant default.** After running the migration, `SELECT code FROM roles WHERE code='AUXILIARY_NURSE'` returns one row.
- **AC-2 — Permission set.** The AUXILIARY_NURSE role has `nursing-note:read/create`, `vital-sign:read/create`, `medication-administration:read`, `medical-order:read`, `progress-note:read`, `clinical-history:read`, `patient:read`, `admission:read`, `admission:update`, `room:occupancy-view`. It does **not** have `medication-administration:create`, `medical-order:mark-in-progress`, `medical-order:upload-document`, or `progress-note:create`.
- **AC-3 — Service guard on administer.** Logged in as a user whose only nursing role is `AUXILIARY_NURSE`, `POST /api/v1/admissions/{id}/medical-orders/{orderId}/medication-administrations` returns 403 `error.nursing.auxiliary.denied`. (Even if a custom role accidentally grants the underlying permission, the service guard still blocks.)
- **AC-4 — Service guard on mark-in-progress.** Same role, `POST /api/v1/admissions/{id}/medical-orders/{orderId}/mark-in-progress` returns 403.
- **AC-5 — Service guard on upload-document.** Same role, document upload endpoint returns 403.
- **AC-6 — Kardex is visible.** GET `/api/v1/nursing-kardex/...` returns 200 for the auxiliary; the response is identical to what a NURSE sees.
- **AC-7 — Quick-action buttons hidden.** The auxiliary sees the kardex but no "Administer" / "Mark in progress" / "Upload result" buttons on cards.
- **AC-8 — Note + vital write through.** The auxiliary can `POST` nursing notes and vital signs successfully.
- **AC-9 — Auxiliary cannot author progress notes.** `POST` to the progress-note endpoint returns 403 (no `progress-note:create` grant).
- **AC-10 — Existing NURSE users unaffected.** A pre-existing user with role `NURSE` retains every permission they had before the migration.
- **AC-11 — Stacked roles work.** A user holding both `NURSE` and `AUXILIARY_NURSE` (e.g. a graduate nurse covering an auxiliary shift) can administer, because the service guard checks "has any of {NURSE, CHIEF_NURSE, DOCTOR, ADMIN}", not "lacks AUXILIARY_NURSE".

---

## Date / Time Conformance

Not applicable — this feature changes only permissions and authorization gating; no new date/time fields are introduced.

- [x] Backend date-only fields use `LocalDate` + `DATE`; event timestamps use `LocalDateTime` + `TIMESTAMP`. (Unchanged.)
- [x] No new frontend date rendering.
- [x] No new `<DatePicker>` instances.
- [x] No `Date → API string` conversions added.
- [x] No relative-time strings added.

---

## API Contract

No new endpoints. The change is observable as new 403 responses on three existing endpoints when called by an AUXILIARY_NURSE-only user:

| Method | Endpoint | New behavior |
|--------|----------|--------------|
| POST | `/api/v1/admissions/{id}/medical-orders/{orderId}/medication-administrations` | Returns 403 `error.nursing.auxiliary.denied` for AUXILIARY_NURSE-only users. |
| POST | `/api/v1/admissions/{id}/medical-orders/{orderId}/mark-in-progress` | Returns 403 `error.nursing.auxiliary.denied` for AUXILIARY_NURSE-only users. |
| POST | `/api/v1/admissions/{id}/medical-orders/{orderId}/documents` | Returns 403 `error.nursing.auxiliary.denied` for AUXILIARY_NURSE-only users. |

---

## Database Changes

### New Migrations

| Migration | Description |
|-----------|-------------|
| `V116__add_auxiliary_nurse_role.sql` | Inserts `AUXILIARY_NURSE` into `roles` (idempotent) and grants the permissions listed in the table above. Pattern mirrors V114 (`add_resident_doctor_role.sql`). |

### Schema Example

```sql
-- V116 (shape; final wording during implementation)
INSERT INTO roles (code, name, description, is_system, created_at, updated_at)
VALUES ('AUXILIARY_NURSE',
        'Auxiliary Nurse',
        'Enfermero auxiliar — notes and vital signs only',
        TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT (SELECT id FROM roles WHERE code='AUXILIARY_NURSE'),
       p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM permissions p
WHERE p.code IN (
    'nursing-note:read', 'nursing-note:create',
    'vital-sign:read',   'vital-sign:create',
    'medication-administration:read',
    'medical-order:read',
    'progress-note:read',
    'clinical-history:read',
    'patient:read',
    'admission:read', 'admission:update',
    'room:occupancy-view'
)
ON CONFLICT DO NOTHING;
```

No table schema changes. No new columns. No data backfill required for existing `user_roles` rows — the migration only creates the new role and its grants.

### Dev seed

- Add `AUXILIARY_NURSE` to the dev role-permission seed file (`R__seed_01_reset_and_base.sql`) following the [[reference_migrations_guide]] dev/prod split.
- Add one `aux_nurse` test user to `R__seed_02_*` (or the test-users file).

---

## Frontend Changes

### Stores

No new stores. `auth` store's existing `hasRole(roleCode)` is sufficient.

### Components

| Component | Change |
|---|---|
| `web/src/components/nursing/MedicationAdministerButton.vue` (or wherever the Administer quick-action lives in `NursingKardexCard` / `MedicalOrderCard`) | Hide when user has `AUXILIARY_NURSE` and none of `{NURSE, CHIEF_NURSE, DOCTOR, ADMIN}`. |
| `web/src/components/medical-record/MedicalOrderCard.vue` | Hide "Mark in progress" + "Upload result" buttons under the same condition. |
| `web/src/router/index.ts` | Add `AUXILIARY_NURSE` to the `/dashboard → /nursing-kardex` redirect list. |
| `web/src/layout/AppMenu.vue` | No change — auxiliary already inherits visibility from the existing permissions (`admission:read`, etc.). Verify no medication-administration entry leaks. |

### i18n

- `roles.AUXILIARY_NURSE` → `Auxiliary Nurse` (en) / `Enfermero auxiliar` (es).
- `error.nursing.auxiliary.denied` → `Auxiliary nurses cannot administer medications, mark orders in progress, or upload result documents.` (en) / `Los enfermeros auxiliares no pueden administrar medicamentos, marcar órdenes en proceso, ni adjuntar resultados.` (es).

### Validation

No new Zod schemas — the change is permission-only.

---

## Implementation Notes

- The service-layer guards are necessary because Spring's `@PreAuthorize` operates on permissions, and a custom role created in the platform could grant `medication-administration:create` to an AUXILIARY_NURSE without violating any DB constraint. The guard is a belt-and-suspenders check that the *role name* is graduate-or-better.
- The `NURSE` ↔ "graduate nurse" semantic equivalence should be reflected in i18n labels (`roles.NURSE` → "Enfermero graduado" in Spanish, "Graduate Nurse" in English) so the customer's mental model lines up with the UI. This is a label-only change; the role code stays `NURSE`.
- Consider whether `CHIEF_NURSE` should automatically include the auxiliary scope. It already does, by virtue of being a superset of `NURSE`. No change needed.
- This spec deliberately does *not* introduce a "ward / floor assignment" concept (auxiliary nurses limited to specific patients). That is out of scope; current visibility rules (whole-hospital for nurses) are preserved.

---

## QA Checklist

### Backend
- [ ] V116 migration runs against a fresh DB and against an existing dev DB (idempotency).
- [ ] AUXILIARY_NURSE row exists in `roles` after migration.
- [ ] Permission grants match AC-2 exactly (write a query test).
- [ ] Service-layer guard on `MedicationAdministrationService.create` — unit test with mocked `SecurityContextHolder`.
- [ ] Service-layer guard on `MedicalOrderService.markInProgress`.
- [ ] Service-layer guard on `MedicalOrderDocumentService.upload`.
- [ ] Integration test: AUXILIARY_NURSE user → 403 on each of the three endpoints (Testcontainers).
- [ ] Integration test: NURSE user → 200 on the same endpoints (regression).
- [ ] Integration test: user with both NURSE and AUXILIARY_NURSE → 200 on all three (stacked role).
- [ ] Detekt passes.

### Frontend
- [ ] Quick-action buttons hidden for AUXILIARY_NURSE-only user on `NursingKardexView`.
- [ ] Same buttons hidden on `MedicalOrderCard` / `MedicalOrdersByStateView`.
- [ ] `/dashboard` redirect lands AUXILIARY_NURSE on `/nursing-kardex`.
- [ ] Nursing note creation and vital sign creation both work for AUXILIARY_NURSE.
- [ ] i18n keys present in `en.json` and `es.json`.
- [ ] ESLint passes.
- [ ] Vitest unit test for `auth.hasRole` interactions in the affected components.

### E2E (Playwright)
- [ ] Auxiliary nurse logs in, opens kardex, records a vital sign — success.
- [ ] Auxiliary nurse attempts to administer a medication via the quick action — button not visible.
- [ ] Auxiliary nurse calls the medication-administration endpoint directly via fetch — gets 403 with the right error code.
- [ ] Graduate nurse (NURSE role) regression — administer / mark-in-progress / upload still work.

### General
- [ ] Roles matrix updated (`docs/roles-functionality-matrix.md`).
- [ ] CLAUDE.md "Implemented Features" updated.
- [ ] Migration recorded in [[reference_migrations_guide]].

---

## Documentation Updates Required

### Must Update

- [ ] **CLAUDE.md** — Add AUXILIARY_NURSE to the migration list (V116 entry) and to the "Implemented Features" backend bullet for roles.
- [ ] **docs/roles-functionality-matrix.md** — Add `AUXILIARY_NURSE` column; explicitly mark each row as None / Read / Work according to the table above. Note that `NURSE` is conceptually "graduate nurse".
- [ ] **docs/roles-functionality-matrix.es.md** — Same in Spanish.
- [ ] **docs/features/nursing-module.md** — Cross-link to this spec from the Authorization section; clarify that the create/read grants on nursing-note and vital-sign apply to **both** `NURSE` and `AUXILIARY_NURSE`.
- [ ] **docs/features/medical-psychiatric-record.md** — Note that AUXILIARY_NURSE does *not* hold `medical-order:mark-in-progress` or `medical-order:upload-document`.

### Review for Consistency

- [ ] **docs/testing/07-NURSING-MODULE.md** — Add an "Auxiliary Nurse" persona section.
- [ ] **docs/testing/11-CROSS-MODULE-SCENARIOS.md** — Add a graduate-vs-auxiliary scenario.

---

## Related Docs/Commits/Issues

- Customer feedback (WhatsApp audio, 2026-05-26 22:08).
- Companion spec: [[warehouse-inventory-management]] (multi-warehouse work from the same feedback round).
- Related: [`docs/features/nursing-module.md`](./nursing-module.md), [`docs/features/medical-psychiatric-record.md`](./medical-psychiatric-record.md), [`docs/roles-functionality-matrix.md`](../roles-functionality-matrix.md).
- Pattern reference: `V114__add_resident_doctor_role.sql` for the role-add migration shape.

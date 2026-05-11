# Feature: Medical/Psychiatric Record (Expediente Médico/Psiquiátrico)

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-02-04 | @author | Initial draft |
| 1.1 | 2026-04-27 | @author | Add medical order workflow states (solicitado, no autorizado, autorizado, reclamar resultados, reclamado), authorize/reject/claim-results endpoints, and a cross-admission orders-by-state screen |
| 1.2 | 2026-04-28 | @author | Replace single workflow with three category-driven shapes: directive (no flow), authorize-only (medications), authorize+execute+results (labs / referrals / psychometric tests). Replace manual `RECLAMAR_RESULTADOS` with explicit `EN_PROCESO` (sample taken / patient referred / test administered) milestone owned by nursing. Add emergency-authorize action for doctors. Rename `DISCONTINUED` to `DESCONTINUADO` for Spanish consistency. |
| 1.3 | 2026-05-09 | @author | (superseded by 1.4) Aligned progress-note edit policy with nursing notes' creator+24h+admin-override pattern. Replaced before reaching production by 1.4. |
| 1.4 | 2026-05-09 | @author | **Strict admin-only update** for progress notes (Evoluciones). Doctors, nurses, and chief nurses can `create` and `read` only; only ADMIN can update existing notes. ADMIN updates are still blocked once the admission is discharged. The 24-hour creator-edit window from 1.3 is removed; this matches the rule originally requested for both nursing notes and progress notes. CHIEF_NURSE gains `progress-note:create` (V096); the unused `progress-note:update` grants on CHIEF_NURSE (seed) and the V095 grants to DOCTOR/NURSE are removed. `ProgressNoteResponse.canEdit` now means `isAdmin && admission.isActive`. The service-layer guard reduces to an admin role check plus discharge protection. Vital signs retain their existing 24h creator window pattern (out of scope). |
| 1.5 | 2026-05-11 | @author | **Rich-text rendering fix** for Progress Note SOAP fields and Medical Order observations. Both surfaces previously rendered the saved HTML via plain `{{ text }}` interpolation, so any formatting from the rich-text editor (paragraphs, bullet lists, bold) appeared as literal tags on a single running line. Display must use sanitized `v-html` (DOMPurify), matching the existing pattern in `ClinicalHistoryView.vue` and `NursingNoteCard.vue`. The `RichTextEditor` (PrimeVue Editor / Quill) must additionally sanitize pasted content to strip inline `style` attributes and tags outside the supported toolbar set (`p`, `br`, `strong`/`b`, `em`/`i`, `u`, `ul`, `ol`, `li`) — eliminating the `<span style="background-color: transparent; color: rgb(0, 0, 0);">` and similar fragments customers were seeing from Word/Chrome/Google Docs pastes. The i18n label for the medical-order inventory link is also renamed from "Servicio/Artículo vinculado" → "**Insumo/servicio a facturar**" (es) and "Linked Service/Item" → "**Billable supply / service**" (en) to make it clear the field drives billing. No data model or API changes. |

---

## Overview

This feature provides a comprehensive medical/psychiatric record system for hospitalized patients. It includes three main sections: Clinical History (Historia Clínica) for initial patient assessment, Progress Notes (Evoluciones) for ongoing daily observations by nurses and doctors, and Medical Orders (Órdenes Médicas) for prescriptions, lab requests, and care instructions. This record is displayed as part of the admission details view. Full audit trail is maintained for all operations.

**Edit policy** (per record type):

- **Clinical History** — append-only for non-admins; only ADMIN can edit existing records.
- **Progress Notes (Evoluciones)** — append-only for non-admins. DOCTOR, NURSE, and CHIEF_NURSE can `create` and `read`. Only ADMIN can edit existing notes. ADMIN edits are blocked once the admission is discharged. There is no creator-edit window — a doctor or nurse who needs to correct their own note must request the change from an administrator. This matches the policy originally specified for the medical record (only admin can modify existing entries).
- **Medical Orders** — append-only for non-admins; only ADMIN can free-form-edit. State transitions (authorize, mark-in-progress, discontinue, etc.) are exposed as their own permissioned endpoints, not as `update`.

> **Note** — Nursing notes and vital signs follow the same admin-only update rule as progress notes (see [`nursing-module.md`](./nursing-module.md) revisions 1.3 and 1.4 — V096 + V097). Doctors, nurses, and chief nurses are append-only on all three record types and must request edits from an administrator.

---

## Use Case / User Story

**Clinical History**
1. As a **doctor**, I want to create a clinical history for an admitted patient so that I can document their initial medical/psychiatric assessment.
2. As a **doctor or nurse**, I want to view the clinical history of an admitted patient so that I can understand their medical background.
3. As an **admin**, I want to modify a clinical history entry so that I can correct errors or update information.

**Progress Notes (Evoluciones)**
4. As a **doctor, nurse, or chief nurse**, I want to add progress notes to an admission so that I can document the patient's daily status and observations.
5. As a **doctor, nurse, or chief nurse**, I want to view all progress notes for an admission (with timestamps and author) so that I can track the patient's evolution over time.
6. As an **admin**, I want to modify any progress note so that I can correct errors when the original author cannot. (No edit affordance is granted to the author themselves; they must request the change from an administrator.)
7. As any user, I should not be able to modify progress notes for a patient that has already been discharged, so the medical record is immutable post-discharge for legal/compliance reasons.

**Medical Orders (Órdenes Médicas)**
8. As a **doctor**, I want to create medical orders (medications, labs, diet, etc.) for an admitted patient so that nurses can follow the treatment plan.
9. As a **nurse**, I want to view all active medical orders for an admission so that I can administer the correct care.
10. As an **admin**, I want to modify or cancel a medical order so that I can correct errors.

**Audit**
11. As a **user with access**, I want to see who created or modified each entry and when so that there is full accountability for medical records.

---

## Authorization / Role Access

### Clinical History

| Action | Required Role(s) | Permission | Notes |
|--------|------------------|------------|-------|
| View | DOCTOR, NURSE, ADMIN | `clinical-history:read` | All medical staff can view |
| Create | DOCTOR, ADMIN | `clinical-history:create` | Only doctors initiate |
| Update | ADMIN | `clinical-history:update` | Append-only for doctors |

### Progress Notes (Evoluciones)

| Action | Required Role(s) | Permission | Notes |
|--------|------------------|------------|-------|
| View | DOCTOR, NURSE, CHIEF_NURSE, ADMIN | `progress-note:read` | All medical staff can view |
| Create | DOCTOR, NURSE, CHIEF_NURSE, ADMIN | `progress-note:create` | Active admissions only. CHIEF_NURSE gains this in V096. |
| Update | ADMIN | `progress-note:update` | Admin-only. No creator-edit window. Active admissions only — discharge blocks even ADMIN. Doctors and nurses must request edits from an administrator. |

All write operations (create/update) are blocked for discharged admissions. Nursing notes follow the same admin-only update rule (see [`nursing-module.md`](./nursing-module.md)).

### Medical Orders (Órdenes Médicas)

| Action | Required Role(s) | Permission | Notes |
|--------|------------------|------------|-------|
| View | DOCTOR, NURSE, CHIEF_NURSE, ADMIN | `medical-order:read` | All medical staff can view |
| Create | DOCTOR, ADMIN | `medical-order:create` | Initial state depends on category (see below) |
| Update | ADMIN | `medical-order:update` | Append-only for doctors |
| Discontinue | DOCTOR, ADMIN | `medical-order:discontinue` | Allowed from `ACTIVA`, `SOLICITADO`, `AUTORIZADO`. **Not** allowed from `EN_PROCESO` (sample is at the lab) or any terminal state. |
| Authorize / Reject | ADMIN, ADMINISTRATIVE_STAFF | `medical-order:authorize` | Transitions `SOLICITADO → AUTORIZADO` or `NO_AUTORIZADO`. Only valid for auth-required categories. For results-bearing categories the order then awaits `mark-in-progress` (it does **not** auto-skip to `EN_PROCESO`). |
| Emergency authorize | DOCTOR, ADMIN | `medical-order:emergency-authorize` | Lets a doctor self-authorize their own order when no admin staff is available (after-hours, patient in crisis, family unreachable). Requires a structured reason. Stamps `emergency_*` audit fields. Same state transition as normal authorize. |
| Mark in progress | NURSE, DOCTOR, ADMIN | `medical-order:mark-in-progress` | Transitions `AUTORIZADO → EN_PROCESO`. Only valid for results-bearing categories (labs / referencias / pruebas psicométricas). Means "sample taken / patient referred / test administered" — after this, discontinue is no longer allowed. |
| Upload result document | DOCTOR, NURSE, ADMIN | `medical-order:upload-document` | Allowed only on results-bearing categories and only while the order is in `AUTORIZADO`, `EN_PROCESO`, or `RESULTADOS_RECIBIDOS`. The first upload auto-transitions the order to `RESULTADOS_RECIBIDOS` (skipping `EN_PROCESO` if the order was still in `AUTORIZADO`). |

---

## Functional Requirements

### Clinical History (Historia Clínica)

- **One record per admission**, created during hospitalization
- Displayed as a dedicated tab/section in admission detail view
- All fields are rich text (WYSIWYG editor) for formatted content
- **Sections and fields:**

  1. **Motivo de la consulta** - Single rich text field
  2. **Problema o padecimiento actual** - Single rich text field
  3. **Salud mental previa** - Single rich text field
  4. **Situación familiar, laboral, social y económica** - Single rich text field
  5. **Antecedentes heredofamiliares** - Group with sub-fields:
     - Médicos (rich text)
     - Psiquiátricos (rich text)
  6. **Antecedentes patológicos personales** - Group with sub-fields:
     - Médicos (rich text)
     - Quirúrgicos (rich text)
     - Alérgicos (rich text)
     - Psiquiátricos (rich text)
     - Gineco-obstétricos (rich text)
  7. **Consumo de sustancias tóxicas y problemas relacionados** - Single rich text field (includes tabaquismo, alcohol, adicciones)
  8. **Desarrollo psicomotor** - Single rich text field
  9. **Examen del estado mental** - Single rich text field
  10. **Temperamento y carácter** - Single rich text field
  11. **Resultados de estudios previos de laboratorio y gabinete** - Single rich text field
  12. **Tratamientos previos y respuesta a los mismos** - Single rich text field
  13. **Observaciones generales** - Single rich text field
  14. **Diagnóstico** - Single rich text field
  15. **Plan de manejo** - Single rich text field
  16. **Pronóstico** - Single rich text field

- Display created by, created at, modified by, modified at on the form

### Progress Notes (Evoluciones)

- **Multiple entries allowed** per admission (multiple per day permitted)
- Each entry is a separate record with its own timestamp
- **Fields per entry (all rich text):**
  1. **Datos subjetivos** - What the patient reports
  2. **Datos objetivos** - Observable/measurable findings
  3. **Análisis** - Assessment/interpretation
  4. **Planes de acción** - Action plans

- Display as chronological list (newest first option, or oldest first)
- Each entry shows: author (name + role), timestamp, all four fields
- Filter by date range, author
- Doctors, nurses, and chief nurses can create new entries on active admissions
- **Append-only for non-admins**: there is no edit affordance for the author; once a note is saved, only an administrator can change it
- Edit existing progress notes — ADMIN only
- Track creator (`createdBy`) and editor (`updatedBy`) for audit trail
- **Discharge protection**: nobody — including ADMIN — can create or edit progress notes for discharged admissions. This protects the medical record post-discharge.
- **canEdit logic** (computed server-side and returned on every `ProgressNoteResponse`):

  ```
  canEdit = currentUser.isAdmin AND admission.isActive
  ```

  Doctors, nurses, and chief nurses always receive `canEdit = false`. The frontend must hide the edit button when this is false; it must not infer edit visibility from role or permission alone.

### Medical Orders (Órdenes Médicas)

- **Multiple orders per admission**, organized by category
- **Categories:**
  1. Órdenes médicas (general medical orders)
  2. Medicamentos (medications)
  3. Laboratorios, exámenes de gabinete (lab tests, imaging)
  4. Referencias médicas (medical referrals)
  5. Pruebas psicométricas (psychometric tests)
  6. Actividad física (physical activity)
  7. Cuidados especiales (special care)
  8. Dieta (diet)
  9. Restricciones de movilidad (mobility restrictions)
  10. Permisos de visita (visit permissions)
  11. Otras (other)

- **Fields per order:**
  1. **Categoría** - Selected from predefined list (required)
  2. **Fecha de inicio** - Start date (required)
  3. **Medicamento** - Medication name (optional, primarily for Medicamentos category)
  4. **Dosis** - Dosage (optional)
  5. **Vía** - Route of administration (optional: oral, IV, IM, etc.)
  6. **Frecuencia** - Frequency (optional: every 8 hours, daily, etc.)
  7. **Horario** - Schedule (optional: specific times)
  8. **Observaciones** - Observations/notes (rich text, optional)

- Display grouped by category
- Each order shows: all fields, author, timestamp, current workflow state
- Doctors can create; only admins can edit existing orders
- Option to mark orders as discontinued (soft status, not delete) from any non-terminal state

#### Workflow States

Every medical order has a workflow state, and the **shape of that workflow depends on the category**. The underlying principle: *authorization gates things the family pays extra for*. Internal directives are part of the bed cost — the doctor's word is enough. Medications and external services need explicit consent before they hit the bill.

Three category shapes:

| Shape | Categories | Why |
|-------|------------|-----|
| **Directive** (no flow) | `DIETA`, `CUIDADOS_ESPECIALES`, `ACTIVIDAD_FISICA`, `RESTRICCIONES_MOVILIDAD`, `PERMISOS_VISITA`, `ORDENES_MEDICAS`, `OTRAS` | Internal clinical instructions. No external cost, no consent step, no results to wait for. |
| **Authorize-only** | `MEDICAMENTOS` | Costs money (medication is line-itemed on the bill) but is administered internally; nothing to wait for after authorization. |
| **Authorize + execute + results** | `LABORATORIOS`, `REFERENCIAS_MEDICAS`, `PRUEBAS_PSICOMETRICAS` | Costs money AND involves an external service (lab, specialist, psychometrist) that produces a result document. |

States:

| State | Spanish label | Used by | Terminal? | Description |
|-------|---------------|---------|-----------|-------------|
| `ACTIVA` | Activa | Directive | No | Initial + only state for directive orders. |
| `SOLICITADO` | Solicitado | Auth-required | No | Initial state for authorize-only and results-bearing orders. Awaiting authorization. |
| `NO_AUTORIZADO` | No autorizado | Auth-required | Yes | Authorization rejected. |
| `AUTORIZADO` | Autorizado | Auth-required | Terminal for `MEDICAMENTOS`; intermediate for results-bearing | Authorization granted. For meds: nurses can administer. For results-bearing: nursing's worklist — sample needs to be taken / patient referred / test administered. |
| `EN_PROCESO` | En proceso | Results-bearing only | No | Sample has been taken / patient has been referred / psychometric test has been administered. Now waiting on the external party for the result document. **Discontinue is no longer allowed once an order reaches this state — the action has already been initiated externally.** |
| `RESULTADOS_RECIBIDOS` | Resultados recibidos | Results-bearing only | Yes | A result document has been uploaded. Reached automatically by the document-upload endpoint. |
| `DESCONTINUADO` | Descontinuado | All | Yes | Order was cancelled. Only allowed from `ACTIVA`, `SOLICITADO`, or `AUTORIZADO`. |

Transitions (only the listed transitions are allowed; any other request returns 400):

```
Directive (DIETA, CUIDADOS_ESPECIALES, ACTIVIDAD_FISICA, RESTRICCIONES_MOVILIDAD,
           PERMISOS_VISITA, ORDENES_MEDICAS, OTRAS):

  ┌─────────┐  discontinue   ┌──────────────────┐
  │ ACTIVA  │ ─────────────► │  DESCONTINUADO   │ (terminal)
  └─────────┘                └──────────────────┘


Authorize-only (MEDICAMENTOS):

  ┌────────────┐  authorize  ┌─────────────┐  discontinue  ┌──────────────────┐
  │ SOLICITADO │ ──────────► │ AUTORIZADO  │ ────────────► │  DESCONTINUADO   │
  └─────┬──────┘             └─────────────┘               └──────────────────┘
        │ reject
        ▼
  ┌────────────────┐
  │ NO_AUTORIZADO  │ (terminal)
  └────────────────┘


Authorize + execute + results (LABORATORIOS, REFERENCIAS_MEDICAS, PRUEBAS_PSICOMETRICAS):

  ┌────────────┐  authorize  ┌─────────────┐  mark-in-progress  ┌──────────────┐  upload doc  ┌──────────────────────┐
  │ SOLICITADO │ ──────────► │ AUTORIZADO  │ ─────────────────► │  EN_PROCESO  │ ───────────► │ RESULTADOS_RECIBIDOS │ (terminal)
  └─────┬──────┘             └──────┬──────┘                    └──────────────┘              └──────────────────────┘
        │ reject                    │ discontinue       (upload from AUTORIZADO is also allowed
        ▼                           ▼                    and skips straight to RESULTADOS_RECIBIDOS)
  ┌────────────────┐         ┌──────────────────┐
  │ NO_AUTORIZADO  │         │  DESCONTINUADO   │ (terminal — only reachable before EN_PROCESO)
  └────────────────┘         └──────────────────┘
```

Rules:

- **Initial state is category-driven.** Directive orders are created in `ACTIVA`. Auth-required orders (medications, labs, referrals, psychometric tests) are created in `SOLICITADO`.
- **The authorize endpoint is rejected for directive categories.** They have no authorization step. Calling `POST /authorize` on a directive returns 400.
- **`mark-in-progress` is results-bearing-only.** It transitions `AUTORIZADO → EN_PROCESO`. The endpoint returns 400 for medications or directive orders. The UI label is category-specific: "Muestra tomada" for labs, "Paciente referido" for referrals, "Prueba administrada" for psychometric tests.
- **Discontinue is allowed from `ACTIVA`, `SOLICITADO`, and `AUTORIZADO` only.** It is **not** allowed from `EN_PROCESO`: once the sample is at the lab (or the patient has gone to the specialist), the work has been initiated externally and the order can no longer be cancelled. Trying to discontinue from `EN_PROCESO` or any terminal state returns 400.
- **`RESULTADOS_RECIBIDOS` cannot be set manually.** It is reached only by uploading a result document. The upload endpoint transitions the order in the same transaction.
- **The upload-document endpoint** allows uploads on results-bearing categories whose state is `AUTORIZADO`, `EN_PROCESO`, or `RESULTADOS_RECIBIDOS`. Uploads from `SOLICITADO`, `NO_AUTORIZADO`, or `DESCONTINUADO`, or against non-results categories, are rejected. Uploading from `AUTORIZADO` skips `EN_PROCESO` — this handles the case where results arrive without anyone formally clicking "mark in progress" (e.g., family brings a PDF from a private lab).
- **Audit columns:**
  - `authorized_at` / `authorized_by` — set by the authorize and emergency-authorize endpoints.
  - `rejected_at` / `rejected_by` — set by the reject endpoint. Distinct from `authorized_*` so reports filtering "orders authorized by X" do not double-count rejections (V094, issue #56).
  - `in_progress_at` / `in_progress_by` — set by mark-in-progress.
  - `results_received_at` / `results_received_by` — set when the first document is uploaded (replaces v1.1's `results_claimed_*`).
  - `discontinued_at` / `discontinued_by` — unchanged.
  - `rejection_reason` — unchanged.
  - Emergency authorization fields (see below).

#### Emergency authorization

In a psychiatric hospital, a patient in crisis may need a stat sedative at 2am — when no admin staff is on duty to record family consent. The doctor needs to authorize the order themselves, with audit. Same applies to a stat blood gas or any other auth-required order.

| Field | Type | Notes |
|-------|------|-------|
| `emergency_authorized` | bool | True when emergency-authorize was used; false for normal authorize. |
| `emergency_reason` | enum | `PATIENT_IN_CRISIS`, `AFTER_HOURS_NO_ADMIN`, `FAMILY_UNREACHABLE`, `OTHER`. |
| `emergency_reason_note` | text? | Required when reason is `OTHER`; optional otherwise. |
| `emergency_at` | timestamp | When emergency-authorize fired. |
| `emergency_by` | bigint (user) | Doctor who pressed the button. |

Behavior:

- New endpoint `POST /api/v1/admissions/{id}/medical-orders/{orderId}/emergency-authorize`, gated by `medical-order:emergency-authorize` (granted to `DOCTOR` and `ADMIN`).
- Same state transition as normal authorize. For meds: `SOLICITADO → AUTORIZADO`. For results-bearing: `SOLICITADO → AUTORIZADO` (still requires the nurse to mark-in-progress separately).
- Billing fires the same as normal authorize. The family is billed, but the audit trail clearly shows the doctor pressed the override and why.
- Returns 400 if the category is directive (no authorization step at all) or if the order is not in `SOLICITADO`.
- The cross-admission dashboard surfaces emergency-authorized orders distinctly so admin staff can review them the next day and confirm family was informed. (No additional approval step is required — this is post-hoc reconciliation, not gating.)
- Free-text `emergency_reason_note` is *optional* unless reason is `OTHER`. The structured reason enum is what reports query against.

#### Billing integration

Billing charges are created when a **results-bearing** order (`LABORATORIOS`, `REFERENCIAS_MEDICAS`, `PRUEBAS_PSICOMETRICAS`) with a linked inventory item transitions to `AUTORIZADO` — both via the normal authorize endpoint and via emergency-authorize. This avoids billing rejected (`NO_AUTORIZADO`) orders. The `MedicalOrderAuthorizedEvent` is published from both authorize paths.

`MEDICAMENTOS` is intentionally **excluded** from this path even though it is auth-required and may carry an inventory item: medications bill per-administration via `InventoryDispensedEvent` from the medication administration flow, so authorizing a medication order on its own does not create a charge. Directive orders never produce charges via this path either (their billing, if any, is handled by other mechanisms — e.g., daily diet charges through the scheduler).

#### Orders by State (cross-admission view)

A new top-level screen lists medical orders across all admissions, grouped/filterable by workflow state. It is intended for:

- Administrative staff to authorize or reject pending requests
- Nursing/admin staff to chase pending external results
- Doctors to see their own orders' status at a glance

| Element | Behavior |
|---------|----------|
| Route | `/medical-orders` |
| Sidebar entry | Visible to roles that hold `medical-order:read` |
| Default filter | `SOLICITADO`, `AUTORIZADO`, `EN_PROCESO` (the action-needed buckets) |
| Other filters | Status (multi-select), category (multi-select) |
| Columns | Patient, category, summary (medication / observations), state badge, requested by, requested at, document count |
| Row actions | Authorize, reject, emergency-authorize, mark in progress (`EN_PROCESO`), upload result document, discontinue, open admission detail (visibility gated by permission and current state) |

### General Requirements

- All three sections displayed as tabs within admission detail view
- Rich text editor for all text fields (TipTap or PrimeVue Editor)
- Full audit trail: created_by, created_at, updated_by, updated_at visible on all entries
- Print-friendly view for medical records

#### Rich-text rendering and paste sanitization (spec v1.5)

All rich-text fields — Clinical History (16+ fields), Progress Note SOAP fields (`subjectiveData`, `objectiveData`, `analysis`, `actionPlans`), and Medical Order `observations` — are authored via the shared `RichTextEditor` component and stored as HTML on the backend. They have two strict rules:

1. **Display side — sanitized `v-html`, never plain interpolation.** Components that render saved content must pass it through `sanitizeHtml` from `@/utils/sanitize` (DOMPurify) and bind via `v-html`. Plain `{{ text }}` interpolation is forbidden for these fields: it shows the HTML tags as literal characters and collapses paragraphs/lists into a single line, which is what customers report as "texto corrido en una misma línea". The reference implementations are `ClinicalHistoryView.vue` and `NursingNoteCard.vue`. `ProgressNoteCard.vue` and `MedicalOrderCard.vue` must be brought in line with them.

2. **Editor side — sanitize on paste.** `RichTextEditor` must register a Quill clipboard matcher that, on every paste, strips:
   - All inline `style` attributes (the source of `style="background-color: transparent; color: rgb(0,0,0);"` noise from Word/Chrome/Google Docs).
   - Any tag outside the supported toolbar set: `p`, `br`, `strong`/`b`, `em`/`i`, `u`, `ul`, `ol`, `li`. Disallowed tags are unwrapped (kept text, dropped tag) rather than removed (no silent content loss).
   - `class` attributes and Word/Google-specific wrappers (`<o:p>`, `<w:*>`, MS-Office namespaced tags).

   Sanitization runs at paste time, not just on save, so the user sees clean formatting in the editor while typing — this is what the customer means by "mantener el mismo formato de edición": what they paste and edit must equal what gets displayed.

3. **Allowed tag inventory must match the toolbar.** If a new formatting button is added to the toolbar, the allow-list in both the paste matcher and `sanitizeHtml` must be updated together. Mismatch means content the user can author gets stripped on display.

The two rules are inseparable: display-side sanitization without paste-side sanitization still stores junk HTML (slower queries, larger payloads, eventually visible in any consumer that doesn't sanitize — e.g. PDF export, future search index). Paste-side sanitization without display-side `v-html` still shows literal `<p>` tags. Both must ship together.

---

## Acceptance Criteria / Scenarios

### Clinical History - Happy Path

- When a doctor creates a clinical history for an admission, all fields are saved and associated with the admission.
- When a user views an admission, the clinical history tab displays all documented fields with rich text formatting preserved.
- When an admin edits a clinical history, changes are saved and the audit trail reflects the modification.

### Clinical History - Edge Cases

- When a non-doctor (nurse) attempts to create a clinical history, return 403 Forbidden.
- When a doctor/nurse attempts to edit an existing clinical history, return 403 Forbidden.
- When creating a clinical history for an admission that already has one, return 400 Bad Request (one per admission).
- When viewing a clinical history that doesn't exist yet, display empty state with "Create" button (for doctors only).

### Progress Notes - Happy Path

- When a doctor, nurse, or chief nurse creates a progress note on an active admission, it is saved with timestamp and author information.
- When viewing progress notes, they are displayed in chronological order with author and timestamp visible.
- Multiple progress notes can be created on the same day by different users.
- When an admin edits any progress note on an active admission, the system updates the note and returns 200 OK; the original `createdBy` is preserved.
- The `ProgressNoteResponse` includes a server-computed `canEdit` field that the frontend uses to show or hide the edit button. `canEdit` is `true` only for admins on active admissions; it is always `false` for doctors, nurses, and chief nurses.

### Progress Notes - Rich-Text Formatting (spec v1.5)

- When a user types **bold text**, a **bullet list**, and **multiple paragraphs** into any SOAP field and saves, reloads the admission, and re-opens the progress notes tab, the rendered card shows the same visual structure (bold characters bold, list items as bullets on separate lines, paragraphs separated). The displayed HTML must include `<strong>`, `<ul><li>`, `<p>` etc. as **rendered DOM**, never as visible literal text.
- When a user **pastes** content from Word, Google Docs, or Chrome that contains inline `style="..."` attributes, `<span>` wrappers, or unsupported tags into a SOAP field and saves, the persisted value must not contain those `style` attributes, `<span>` wrappers, or unsupported tags. The visible text content is preserved.
- The collapsed (truncated) view of a long progress note may use a plain-text projection for the preview, but the **expanded** view must render the HTML.

### Progress Notes - Edge Cases

- When a doctor, nurse, or chief nurse attempts to edit any progress note (including one they authored), return 403 Forbidden — both because they lack `progress-note:update` and because the service-layer guard enforces ADMIN-only.
- When **any user (including admin)** attempts to create or edit a progress note on a discharged admission, return 400 Bad Request with message "Cannot modify records for discharged admissions" / "No se pueden modificar registros de admisiones dadas de alta".
- When editing a non-existent progress note, return 404 Not Found.
- When submitting a progress note with all empty fields, return 400 Bad Request with validation error.

### Medical Orders - Happy Path

- When a doctor creates a medical order, they select a category and fill in the order details.
- When viewing medical orders, they are grouped by category and display all relevant fields.
- Multiple orders can exist under the same category.

### Medical Orders - Edge Cases

- When a nurse attempts to create a medical order, return 403 Forbidden.
- When a doctor/nurse attempts to edit a medical order, return 403 Forbidden.
- When creating an order without required fields (category, fecha de inicio), return 400 Bad Request.
- When an order has a start date in the past, allow it (backdating for documentation purposes).

### Medical Orders - Rich-Text Observations (spec v1.5)

- When a doctor creates a medical order whose `observations` field contains bold text, a bullet list, and multiple paragraphs, the order card on the admission detail and on the cross-admission orders-by-state screen renders the same visual structure (rendered DOM, not literal tags).
- When a doctor pastes formatted content from Word / Google Docs / Chrome into `observations`, the saved value must not contain inline `style` attributes or unsupported tags; the visible text content is preserved.
- The `observations` summary shown in the orders-by-state list column may project to plain text for the table cell (to keep rows compact), but the order detail / card view must render HTML.

### Medical Order Inventory Link Label (spec v1.5)

- The form field that links a medical order to an inventory item is labelled **"Insumo/servicio a facturar"** (es) and **"Billable supply / service"** (en) in the form dialog. The placeholder remains the existing explanatory text. This is a pure i18n change; the field name in the API contract is unchanged (`inventoryItemId`).

### Medical Orders - State Transitions

- A newly created order's initial state is **category-driven**: `ACTIVA` for directive categories (DIETA, CUIDADOS_ESPECIALES, ACTIVIDAD_FISICA, RESTRICCIONES_MOVILIDAD, PERMISOS_VISITA, ORDENES_MEDICAS, OTRAS); `SOLICITADO` for auth-required categories (MEDICAMENTOS, LABORATORIOS, REFERENCIAS_MEDICAS, PRUEBAS_PSICOMETRICAS).
- When admin staff authorizes a `SOLICITADO` order, state becomes `AUTORIZADO` and `authorized_at` / `authorized_by` are stamped.
- When admin staff rejects a `SOLICITADO` order, state becomes `NO_AUTORIZADO` (terminal).
- A doctor can emergency-authorize a `SOLICITADO` order. State becomes `AUTORIZADO`, `authorized_at`/`authorized_by` are stamped, and `emergency_authorized=true` plus `emergency_reason`/`emergency_at`/`emergency_by` (and optional `emergency_reason_note`) are recorded.
- A nurse / doctor / admin can mark an `AUTORIZADO` order as `EN_PROCESO` only when the category is `LABORATORIOS`, `REFERENCIAS_MEDICAS`, or `PRUEBAS_PSICOMETRICAS`. Other categories return 400. `in_progress_at` / `in_progress_by` are stamped.
- When the first document is uploaded against an order in `AUTORIZADO` or `EN_PROCESO`, the order transitions to `RESULTADOS_RECIBIDOS` in the same transaction; `results_received_at` / `results_received_by` are stamped from the uploader.
- Trying to authorize/reject/emergency-authorize a directive order, or any order not in `SOLICITADO`, returns 400.
- Trying to mark in-progress on a non-results category, or on an order not in `AUTORIZADO`, returns 400.
- Trying to manually set state `RESULTADOS_RECIBIDOS` (without an uploaded document) returns 400 — the only path is via document upload.
- Trying to discontinue an order in `EN_PROCESO` returns 400 ("sample is at the lab — cannot cancel"). Discontinue is allowed only from `ACTIVA`, `SOLICITADO`, or `AUTORIZADO`.
- Trying to discontinue an order in a terminal state (`NO_AUTORIZADO`, `RESULTADOS_RECIBIDOS`, `DESCONTINUADO`) returns 400.
- Billing charge for a results-bearing order (`LABORATORIOS`, `REFERENCIAS_MEDICAS`, `PRUEBAS_PSICOMETRICAS`) with a linked inventory item is created on `AUTORIZADO` (both via normal authorize and emergency-authorize). `MEDICAMENTOS` does not produce a charge on `AUTORIZADO` (it bills per-administration via `InventoryDispensedEvent`). Rejected orders never produce a charge.
- Emergency-authorize requires reason `PATIENT_IN_CRISIS`, `AFTER_HOURS_NO_ADMIN`, `FAMILY_UNREACHABLE`, or `OTHER`. When `OTHER`, a `reasonNote` is required; for the other three, the note is optional.

### Audit Trail

- All create/edit operations record the user ID and timestamp.
- Edit operations preserve the original creator and record the modifier.
- Audit information is visible on each entry (created by, created at, modified by, modified at).

### Authorization

- When an unauthenticated user attempts any action, return 401 Unauthorized.
- When a user without appropriate role attempts a restricted action, return 403 Forbidden.

---

## Non-Functional Requirements

- **Security**: Clinical history, progress notes, and medical orders are all append-only for non-admin users — only ADMIN can call their `update` endpoints. For progress notes the policy is enforced at two layers: (1) `progress-note:update` is granted only to ADMIN at the role-permission level, blocking the request at `@PreAuthorize`; (2) `ProgressNoteService.updateProgressNote()` independently asserts ADMIN role and throws `ForbiddenException` otherwise, so the rule continues to hold if a future migration accidentally widens the permission. Discharge protection (`BadRequestException` on discharged admissions) is enforced unconditionally at the service layer for both create and update, including for ADMIN.
- **Audit**: Full audit trail on all entries (required for medical compliance)
- **Performance**: Progress notes list should paginate for admissions with many entries
- **Rich Text**: Support basic formatting (bold, italic, lists, headings) in all text fields

---

## API Contract

### Clinical History Endpoints

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| GET | `/api/v1/admissions/{admissionId}/clinical-history` | - | `ClinicalHistoryResponse` | Yes | Get clinical history for admission |
| POST | `/api/v1/admissions/{admissionId}/clinical-history` | `CreateClinicalHistoryRequest` | `ClinicalHistoryResponse` | Yes | Create clinical history |
| PUT | `/api/v1/admissions/{admissionId}/clinical-history` | `UpdateClinicalHistoryRequest` | `ClinicalHistoryResponse` | Yes | Update clinical history (admin only) |

### Progress Notes Endpoints

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| GET | `/api/v1/admissions/{admissionId}/progress-notes` | - | `PagedResponse<ProgressNoteResponse>` | Yes | List progress notes with pagination |
| GET | `/api/v1/admissions/{admissionId}/progress-notes/{id}` | - | `ProgressNoteResponse` | Yes | Get single progress note |
| POST | `/api/v1/admissions/{admissionId}/progress-notes` | `CreateProgressNoteRequest` | `ProgressNoteResponse` | Yes | Create progress note |
| PUT | `/api/v1/admissions/{admissionId}/progress-notes/{id}` | `UpdateProgressNoteRequest` | `ProgressNoteResponse` | Yes | Update progress note. Allowed only for ADMIN on an active admission. Returns 403 for any non-admin role (including the original author). Returns 400 if the admission is discharged (even for ADMIN). |

### Medical Orders Endpoints

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| GET | `/api/v1/admissions/{admissionId}/medical-orders` | - | `List<MedicalOrderResponse>` | Yes | List all medical orders for an admission (grouped by category) |
| GET | `/api/v1/admissions/{admissionId}/medical-orders/{id}` | - | `MedicalOrderResponse` | Yes | Get single medical order |
| POST | `/api/v1/admissions/{admissionId}/medical-orders` | `CreateMedicalOrderRequest` | `MedicalOrderResponse` | Yes | Create medical order. Initial state is `ACTIVA` for directive categories, `SOLICITADO` otherwise. |
| PUT | `/api/v1/admissions/{admissionId}/medical-orders/{id}` | `UpdateMedicalOrderRequest` | `MedicalOrderResponse` | Yes | Update medical order (admin only) |
| POST | `/api/v1/admissions/{admissionId}/medical-orders/{id}/discontinue` | - | `MedicalOrderResponse` | Yes | Mark order as discontinued. Allowed from `ACTIVA`, `SOLICITADO`, `AUTORIZADO`. Returns 400 from `EN_PROCESO` or any terminal state. |
| POST | `/api/v1/admissions/{admissionId}/medical-orders/{id}/authorize` | - | `MedicalOrderResponse` | Yes | Transition `SOLICITADO → AUTORIZADO`. Returns 400 for directive categories. Triggers billing charge for billable categories with linked inventory. |
| POST | `/api/v1/admissions/{admissionId}/medical-orders/{id}/reject` | `RejectMedicalOrderRequest` (optional reason) | `MedicalOrderResponse` | Yes | Transition `SOLICITADO → NO_AUTORIZADO`. Returns 400 for directive categories. |
| POST | `/api/v1/admissions/{admissionId}/medical-orders/{id}/emergency-authorize` | `EmergencyAuthorizeRequest` (reason enum + optional note) | `MedicalOrderResponse` | Yes | Doctor self-authorization for crisis / after-hours / family-unreachable cases. Same state transition as authorize. Stamps `emergency_*` audit fields. Returns 400 for directive categories or when reason is `OTHER` without a note. |
| POST | `/api/v1/admissions/{admissionId}/medical-orders/{id}/mark-in-progress` | - | `MedicalOrderResponse` | Yes | Transition `AUTORIZADO → EN_PROCESO` for results-bearing categories only (sample taken / patient referred / test administered). Returns 400 for non-results categories or when not in `AUTORIZADO`. |
| GET | `/api/v1/medical-orders` | query: `status` (repeated), `category` (repeated), `page`, `size` | `PagedResponse<MedicalOrderListItemResponse>` | Yes | Cross-admission listing for the orders-by-state screen. `status` and `category` may be specified multiple times for multi-select. Includes patient + admission summary fields. |

### Request/Response Examples

```json
// POST /api/v1/admissions/{admissionId}/clinical-history - Request
{
  "consultationReason": "<p>Patient presents with...</p>",
  "currentCondition": "<p>Currently experiencing...</p>",
  "previousMentalHealth": "<p>History of depression...</p>",
  "familySocialEconomicSituation": "<p>Lives with spouse...</p>",
  "familyMedicalHistory": "<p>Father had diabetes...</p>",
  "familyPsychiatricHistory": "<p>Mother had anxiety...</p>",
  "personalMedicalHistory": "<p>Hypertension diagnosed 2020...</p>",
  "personalSurgicalHistory": "<p>Appendectomy 2015...</p>",
  "personalAllergies": "<p>Penicillin allergy...</p>",
  "personalPsychiatricHistory": "<p>Previous depressive episode...</p>",
  "personalGynecologicalHistory": "<p>N/A</p>",
  "substanceUse": "<p>Social alcohol use...</p>",
  "psychomotorDevelopment": "<p>Normal development...</p>",
  "mentalStatusExam": "<p>Alert and oriented...</p>",
  "temperamentCharacter": "<p>Introverted...</p>",
  "previousLabResults": "<p>CBC normal...</p>",
  "previousTreatments": "<p>Sertraline 50mg for 6 months...</p>",
  "generalObservations": "<p>Patient cooperative...</p>",
  "diagnosis": "<p>Major Depressive Disorder, recurrent...</p>",
  "managementPlan": "<p>1. Start medication...</p>",
  "prognosis": "<p>Good with adherence to treatment...</p>"
}

// Response - ClinicalHistoryResponse
{
  "id": 1,
  "admissionId": 123,
  "consultationReason": "<p>Patient presents with...</p>",
  "currentCondition": "<p>Currently experiencing...</p>",
  // ... all other fields ...
  "createdAt": "2026-02-04T10:30:00",
  "createdBy": {
    "id": 5,
    "salutation": "Dr.",
    "firstName": "Maria",
    "lastName": "Garcia"
  },
  "updatedAt": "2026-02-04T10:30:00",
  "updatedBy": {
    "id": 5,
    "salutation": "Dr.",
    "firstName": "Maria",
    "lastName": "Garcia"
  }
}

// POST /api/v1/admissions/{admissionId}/progress-notes - Request
{
  "subjectiveData": "<p>Patient reports feeling better today...</p>",
  "objectiveData": "<p>BP: 120/80, HR: 72, Temp: 36.5C...</p>",
  "analysis": "<p>Improvement noted in mood...</p>",
  "actionPlans": "<p>Continue current medication...</p>"
}

// Response - ProgressNoteResponse
{
  "id": 1,
  "admissionId": 123,
  "subjectiveData": "<p>Patient reports feeling better today...</p>",
  "objectiveData": "<p>BP: 120/80, HR: 72, Temp: 36.5C...</p>",
  "analysis": "<p>Improvement noted in mood...</p>",
  "actionPlans": "<p>Continue current medication...</p>",
  "createdAt": "2026-02-04T14:00:00",
  "createdBy": {
    "id": 8,
    "salutation": "Lic.",
    "firstName": "Ana",
    "lastName": "Rodriguez",
    "role": "NURSE"
  },
  "updatedAt": "2026-02-04T14:00:00",
  "updatedBy": null,
  "canEdit": true
}

// POST /api/v1/admissions/{admissionId}/medical-orders - Request
{
  "category": "MEDICAMENTOS",
  "startDate": "2026-02-04",
  "medication": "Sertraline",
  "dosage": "50mg",
  "route": "ORAL",
  "frequency": "Once daily",
  "schedule": "Morning with breakfast",
  "observations": "<p>Monitor for side effects...</p>"
}

// Response - MedicalOrderResponse
{
  "id": 1,
  "admissionId": 123,
  "category": "MEDICAMENTOS",
  "startDate": "2026-02-04",
  "medication": "Sertraline",
  "dosage": "50mg",
  "route": "ORAL",
  "frequency": "Once daily",
  "schedule": "Morning with breakfast",
  "observations": "<p>Monitor for side effects...</p>",
  "status": "SOLICITADO",
  "authorizedAt": null,
  "authorizedBy": null,
  "inProgressAt": null,
  "inProgressBy": null,
  "resultsReceivedAt": null,
  "resultsReceivedBy": null,
  "rejectionReason": null,
  "emergencyAuthorized": false,
  "emergencyReason": null,
  "emergencyReasonNote": null,
  "emergencyAt": null,
  "emergencyBy": null,
  "discontinuedAt": null,
  "discontinuedBy": null,
  "createdAt": "2026-02-04T10:35:00",
  "createdBy": {
    "id": 5,
    "salutation": "Dr.",
    "firstName": "Maria",
    "lastName": "Garcia"
  },
  "updatedAt": "2026-02-04T10:35:00",
  "updatedBy": null
}

// POST /api/v1/admissions/{admissionId}/medical-orders/{id}/authorize - no request body
// Response: MedicalOrderResponse with status="AUTORIZADO" and authorizedAt/authorizedBy populated.
// Returns 400 if order is not in SOLICITADO, or if category is directive.

// POST /api/v1/admissions/{admissionId}/medical-orders/{id}/reject - Request
{
  "reason": "Pendiente de cobertura del seguro"   // optional
}

// POST /api/v1/admissions/{admissionId}/medical-orders/{id}/emergency-authorize - Request
{
  "reason": "AFTER_HOURS_NO_ADMIN",                    // required; enum value
  "reasonNote": "Patient agitated, no admin on call"   // optional; required when reason = "OTHER"
}
// Response: MedicalOrderResponse with status="AUTORIZADO", emergencyAuthorized=true,
// emergencyReason populated, emergencyAt/emergencyBy stamped.

// POST /api/v1/admissions/{admissionId}/medical-orders/{id}/mark-in-progress - no request body
// Response: MedicalOrderResponse with status="EN_PROCESO" and inProgressAt/inProgressBy populated.
// Returns 400 if category is not results-bearing or order is not in AUTORIZADO.

// GET /api/v1/medical-orders?status=SOLICITADO&status=AUTORIZADO&page=0&size=20 - Response (paged, cross-admission)
{
  "content": [
    {
      "id": 12,
      "admissionId": 45,
      "patient": { "id": 9, "firstName": "Juan", "lastName": "Pérez" },
      "category": "LABORATORIOS",
      "status": "SOLICITADO",
      "summary": "Hemograma completo",
      "createdBy": { "id": 5, "firstName": "Maria", "lastName": "Garcia" },
      "createdAt": "2026-04-27T08:15:00"
    }
  ],
  "totalElements": 17,
  "totalPages": 1,
  "page": 0,
  "size": 20
}

// GET /api/v1/admissions/{admissionId}/medical-orders - Response (grouped)
{
  "MEDICAMENTOS": [
    { "id": 1, "medication": "Sertraline", "dosage": "50mg", ... },
    { "id": 2, "medication": "Lorazepam", "dosage": "1mg", ... }
  ],
  "LABORATORIOS": [
    { "id": 3, "observations": "<p>Complete blood count...</p>", ... }
  ],
  "DIETA": [
    { "id": 4, "observations": "<p>Low sodium diet...</p>", ... }
  ]
}
```

---

## Database Changes

### New Entities

| Entity | Table | Extends | Description |
|--------|-------|---------|-------------|
| `ClinicalHistory` | `clinical_histories` | `BaseEntity` | Clinical history record (one per admission) |
| `ProgressNote` | `progress_notes` | `BaseEntity` | Progress note entries |
| `MedicalOrder` | `medical_orders` | `BaseEntity` | Medical order entries |

### New Migrations

| Migration | Description |
|-----------|-------------|
| `V029__create_clinical_histories_table.sql` | Creates clinical_histories table with all text fields |
| `V030__create_progress_notes_table.sql` | Creates progress_notes table |
| `V031__create_medical_orders_table.sql` | Creates medical_orders table with category enum |
| `V032__add_medical_record_permissions.sql` | Adds permissions for clinical-history, progress-note, medical-order resources |
| `V092__expand_medical_order_workflow_states.sql` | Migrates `medical_orders.status` to the category-driven state machine: `ACTIVA`, `SOLICITADO`, `NO_AUTORIZADO`, `AUTORIZADO`, `EN_PROCESO`, `RESULTADOS_RECIBIDOS`, `DESCONTINUADO`. Adds workflow audit columns (`authorized_at/by`, `in_progress_at/by`, `results_received_at/by`, `rejection_reason`) and emergency-authorization columns (`emergency_authorized`, `emergency_reason`, `emergency_reason_note`, `emergency_at`, `emergency_by`). Backfills existing `ACTIVE` rows: directive categories → `ACTIVA`, all others → `AUTORIZADO`. Renames `DISCONTINUED` → `DESCONTINUADO` for Spanish consistency. Default for new rows is set application-side based on category. |
| `V093__add_medical_order_workflow_permissions.sql` | Adds `medical-order:authorize` (ADMIN, ADMINISTRATIVE_STAFF), `medical-order:emergency-authorize` (ADMIN, DOCTOR), and `medical-order:mark-in-progress` (ADMIN, DOCTOR, NURSE) permissions and role grants. |
| `V094__add_medical_order_rejection_audit.sql` | Adds dedicated `rejected_at` / `rejected_by` columns so the reject endpoint no longer overloads `authorized_at` / `authorized_by`. Backfills existing `NO_AUTORIZADO` rows by moving the timestamp/user from the authorize fields into the rejection fields and clearing the authorize fields (issue #56). |

### Schema

```sql
-- V029__create_clinical_histories_table.sql
CREATE TABLE clinical_histories (
    id BIGSERIAL PRIMARY KEY,
    admission_id BIGINT NOT NULL UNIQUE REFERENCES admissions(id),

    -- Section 1: Motivo de la consulta
    consultation_reason TEXT,

    -- Section 2: Problema o padecimiento actual
    current_condition TEXT,

    -- Section 3: Salud mental previa
    previous_mental_health TEXT,

    -- Section 4: Situación familiar, laboral, social y económica
    family_social_economic_situation TEXT,

    -- Section 5: Antecedentes heredofamiliares
    family_medical_history TEXT,
    family_psychiatric_history TEXT,

    -- Section 6: Antecedentes patológicos personales
    personal_medical_history TEXT,
    personal_surgical_history TEXT,
    personal_allergies TEXT,
    personal_psychiatric_history TEXT,
    personal_gynecological_history TEXT,

    -- Section 7: Consumo de sustancias tóxicas
    substance_use TEXT,

    -- Section 8: Desarrollo psicomotor
    psychomotor_development TEXT,

    -- Section 9: Examen del estado mental
    mental_status_exam TEXT,

    -- Section 10: Temperamento y carácter
    temperament_character TEXT,

    -- Section 11: Resultados de estudios previos
    previous_lab_results TEXT,

    -- Section 12: Tratamientos previos
    previous_treatments TEXT,

    -- Section 13: Observaciones generales
    general_observations TEXT,

    -- Section 14: Diagnóstico
    diagnosis TEXT,

    -- Section 15: Plan de manejo
    management_plan TEXT,

    -- Section 16: Pronóstico
    prognosis TEXT,

    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_clinical_histories_deleted_at ON clinical_histories(deleted_at);
CREATE INDEX idx_clinical_histories_admission_id ON clinical_histories(admission_id);

-- V030__create_progress_notes_table.sql
CREATE TABLE progress_notes (
    id BIGSERIAL PRIMARY KEY,
    admission_id BIGINT NOT NULL REFERENCES admissions(id),

    -- SOAP-like structure
    subjective_data TEXT NOT NULL,
    objective_data TEXT NOT NULL,
    analysis TEXT NOT NULL,
    action_plans TEXT NOT NULL,

    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_progress_notes_deleted_at ON progress_notes(deleted_at);
CREATE INDEX idx_progress_notes_admission_id ON progress_notes(admission_id);
CREATE INDEX idx_progress_notes_created_at ON progress_notes(created_at);

-- V031__create_medical_orders_table.sql
CREATE TYPE medical_order_category AS ENUM (
    'ORDENES_MEDICAS',
    'MEDICAMENTOS',
    'LABORATORIOS',
    'REFERENCIAS_MEDICAS',
    'PRUEBAS_PSICOMETRICAS',
    'ACTIVIDAD_FISICA',
    'CUIDADOS_ESPECIALES',
    'DIETA',
    'RESTRICCIONES_MOVILIDAD',
    'PERMISOS_VISITA',
    'OTRAS'
);

-- Note: V092 migrates this enum to the v1.2 category-driven state machine:
--   'ACTIVA', 'SOLICITADO', 'NO_AUTORIZADO', 'AUTORIZADO', 'EN_PROCESO',
--   'RESULTADOS_RECIBIDOS', 'DESCONTINUADO'
-- Existing 'ACTIVE' rows are backfilled per category: directive categories → 'ACTIVA',
-- others → 'AUTORIZADO'. The default is set application-side based on category.
CREATE TYPE medical_order_status AS ENUM (
    'ACTIVE',
    'DISCONTINUED'
);

CREATE TYPE administration_route AS ENUM (
    'ORAL',
    'IV',
    'IM',
    'SC',
    'TOPICAL',
    'INHALATION',
    'RECTAL',
    'SUBLINGUAL',
    'OTHER'
);

CREATE TABLE medical_orders (
    id BIGSERIAL PRIMARY KEY,
    admission_id BIGINT NOT NULL REFERENCES admissions(id),

    category medical_order_category NOT NULL,
    start_date DATE NOT NULL,
    medication VARCHAR(255),
    dosage VARCHAR(100),
    route administration_route,
    frequency VARCHAR(100),
    schedule VARCHAR(255),
    observations TEXT,

    status medical_order_status NOT NULL DEFAULT 'ACTIVE',  -- V092 changes default to 'SOLICITADO'
    discontinued_at TIMESTAMP,
    discontinued_by BIGINT REFERENCES users(id),

    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_medical_orders_deleted_at ON medical_orders(deleted_at);
CREATE INDEX idx_medical_orders_admission_id ON medical_orders(admission_id);
CREATE INDEX idx_medical_orders_category ON medical_orders(category);
CREATE INDEX idx_medical_orders_status ON medical_orders(status);
CREATE INDEX idx_medical_orders_start_date ON medical_orders(start_date);

-- V092__expand_medical_order_workflow_states.sql
-- Migrates medical_orders.status to the category-driven state machine and adds
-- audit columns for the new transitions plus emergency-authorization tracking.
ALTER TABLE medical_orders
    ADD COLUMN authorized_at TIMESTAMP,
    ADD COLUMN authorized_by BIGINT REFERENCES users(id),
    ADD COLUMN in_progress_at TIMESTAMP,
    ADD COLUMN in_progress_by BIGINT REFERENCES users(id),
    ADD COLUMN results_received_at TIMESTAMP,
    ADD COLUMN results_received_by BIGINT REFERENCES users(id),
    ADD COLUMN rejection_reason TEXT,
    ADD COLUMN emergency_authorized BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN emergency_reason VARCHAR(40),
    ADD COLUMN emergency_reason_note TEXT,
    ADD COLUMN emergency_at TIMESTAMP,
    ADD COLUMN emergency_by BIGINT REFERENCES users(id);

-- Drop and recreate the status check constraint with the new allowed values.
ALTER TABLE medical_orders DROP CONSTRAINT IF EXISTS medical_orders_status_check;
ALTER TABLE medical_orders ADD CONSTRAINT medical_orders_status_check
    CHECK (status IN (
        'ACTIVA','SOLICITADO','NO_AUTORIZADO','AUTORIZADO',
        'EN_PROCESO','RESULTADOS_RECIBIDOS','DESCONTINUADO'
    ));

-- Backfill existing ACTIVE rows per category. Directive categories → ACTIVA;
-- everything else → AUTORIZADO (closest semantic — they were already in effect).
UPDATE medical_orders SET status = 'ACTIVA'
    WHERE status = 'ACTIVE' AND category IN (
        'DIETA','CUIDADOS_ESPECIALES','ACTIVIDAD_FISICA',
        'RESTRICCIONES_MOVILIDAD','PERMISOS_VISITA','ORDENES_MEDICAS','OTRAS'
    );
UPDATE medical_orders SET status = 'AUTORIZADO' WHERE status = 'ACTIVE';

-- Rename existing DISCONTINUED rows to DESCONTINUADO for Spanish consistency.
UPDATE medical_orders SET status = 'DESCONTINUADO' WHERE status = 'DISCONTINUED';

-- Constrain emergency_reason to its enum domain.
ALTER TABLE medical_orders ADD CONSTRAINT medical_orders_emergency_reason_check
    CHECK (emergency_reason IS NULL OR emergency_reason IN (
        'PATIENT_IN_CRISIS','AFTER_HOURS_NO_ADMIN','FAMILY_UNREACHABLE','OTHER'
    ));

-- The default for new rows is set application-side based on category.
ALTER TABLE medical_orders ALTER COLUMN status DROP DEFAULT;

CREATE INDEX idx_medical_orders_emergency ON medical_orders(emergency_authorized)
    WHERE emergency_authorized = TRUE;

-- V093__add_medical_order_workflow_permissions.sql
INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('medical-order:authorize',           'Authorize Medical Order',           'Approve or reject medical orders',                                'medical-order', 'authorize',           CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('medical-order:emergency-authorize', 'Emergency Authorize Medical Order', 'Doctor self-authorization for crisis or after-hours scenarios',   'medical-order', 'emergency-authorize', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('medical-order:mark-in-progress',    'Mark Medical Order In Progress',    'Mark a results-bearing order as executed (sample taken / referred / administered)', 'medical-order', 'mark-in-progress', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- V094__add_medical_order_rejection_audit.sql
-- Splits rejection audit out of authorized_at/by; backfills existing NO_AUTORIZADO rows.
ALTER TABLE medical_orders
    ADD COLUMN rejected_at TIMESTAMP,
    ADD COLUMN rejected_by BIGINT REFERENCES users(id);

UPDATE medical_orders
   SET rejected_at   = authorized_at,
       rejected_by   = authorized_by,
       authorized_at = NULL,
       authorized_by = NULL
 WHERE status = 'NO_AUTORIZADO';

-- ADMIN gets all three via the standard role-permission grant.
-- ADMINISTRATIVE_STAFF gets authorize + read (so the dashboard renders).
-- DOCTOR gets emergency-authorize + mark-in-progress (in addition to existing create/read/discontinue).
-- NURSE gets mark-in-progress (sample-taking worklist owner).
-- See V093 in api/src/main/resources/db/migration/V093__add_medical_order_workflow_permissions.sql
-- for the canonical SQL.

-- V032__add_medical_record_permissions.sql
-- Clinical History permissions
INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('clinical-history:create', 'Create Clinical History', 'Create clinical history records', 'clinical-history', 'create', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('clinical-history:read', 'Read Clinical History', 'View clinical history records', 'clinical-history', 'read', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('clinical-history:update', 'Update Clinical History', 'Modify clinical history records', 'clinical-history', 'update', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Progress Note permissions
INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('progress-note:create', 'Create Progress Note', 'Create progress notes', 'progress-note', 'create', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('progress-note:read', 'Read Progress Note', 'View progress notes', 'progress-note', 'read', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('progress-note:update', 'Update Progress Note', 'Modify progress notes', 'progress-note', 'update', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Medical Order permissions
INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('medical-order:create', 'Create Medical Order', 'Create medical orders', 'medical-order', 'create', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('medical-order:read', 'Read Medical Order', 'View medical orders', 'medical-order', 'read', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('medical-order:update', 'Update Medical Order', 'Modify medical orders', 'medical-order', 'update', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Assign ADMIN full access
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMIN' AND p.resource IN ('clinical-history', 'progress-note', 'medical-order');

-- Assign DOCTOR: create/read clinical-history, create/read progress-note, create/read medical-order
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'DOCTOR' AND p.code IN (
    'clinical-history:create', 'clinical-history:read',
    'progress-note:create', 'progress-note:read',
    'medical-order:create', 'medical-order:read'
);

-- Assign NURSE: read clinical-history, create/read progress-note, read medical-order
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'NURSE' AND p.code IN (
    'clinical-history:read',
    'progress-note:create', 'progress-note:read',
    'medical-order:read'
);

-- Assign CHIEF_NURSE: read clinical-history, create/read progress-note, read medical-order
-- NOTE: progress-note:create is added by V096; older versions of the seed file may
-- have granted progress-note:update to CHIEF_NURSE — V096 also revokes that grant.
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'CHIEF_NURSE' AND p.code IN (
    'clinical-history:read',
    'progress-note:create', 'progress-note:read',
    'medical-order:read'
);
```

> **Note** — Only ADMIN holds `progress-note:update`. The controller-level `@PreAuthorize` blocks every other role, and the service layer additionally asserts `ADMIN` so the rule continues to hold if the permission is later widened. ADMIN updates are still subject to discharge protection.

### Migrations Required

| Version | Filename | Purpose |
|---------|----------|---------|
| V029-V031 | (existing) | Create `clinical_histories`, `progress_notes`, `medical_orders` tables |
| V032 / V038 | `add_medical_record_permissions.sql` | Define `clinical-history:*`, `progress-note:*`, `medical-order:*` permissions and assign to roles (`progress-note:update` for ADMIN only — final state) |
| V092 / V093 / V094 | (existing) | Medical order workflow states + workflow permissions + rejection audit columns |
| ~~V095~~ | ~~`add_progress_note_update_to_doctor_nurse.sql`~~ | **Removed before merge.** Briefly granted `progress-note:update` to DOCTOR and NURSE under the v1.3 spec; superseded by v1.4 which keeps update strictly with ADMIN. The migration file is deleted from the working tree. |
| **V096** | **`lock_progress_note_update_to_admin_only.sql`** | **(1)** Grant `progress-note:create` to CHIEF_NURSE so they can author notes. **(2)** Revoke any existing `progress-note:update` grant from CHIEF_NURSE (legacy seed grant). **(3)** Revoke any existing `nursing-note:update` grants from DOCTOR, NURSE, and CHIEF_NURSE so nursing notes match progress notes (admin-only update). All revocations are idempotent (`DELETE … WHERE …`). |

### Index Requirements

- [x] `deleted_at` - Required for soft delete queries on all tables
- [x] `admission_id` - FK lookup for all medical record queries
- [x] `created_at` on progress_notes - Sorting by date
- [x] `category` on medical_orders - Grouping by category
- [x] `status` on medical_orders - Filter by workflow state (powers the orders-by-state screen)
- [x] `start_date` on medical_orders - Date filtering
- [ ] `(status, created_at DESC)` composite on medical_orders - Cross-admission sort by status (V092)

---

## Frontend Changes

### Components

| Component | Location | Description |
|-----------|----------|-------------|
| `MedicalRecordTabs.vue` | `src/components/admissions/` | Tab container for all three sections |
| `ClinicalHistoryView.vue` | `src/components/medical-record/` | View/display clinical history |
| `ClinicalHistoryForm.vue` | `src/components/medical-record/` | Create/edit clinical history form |
| `ProgressNoteList.vue` | `src/components/medical-record/` | List of progress notes with pagination |
| `ProgressNoteForm.vue` | `src/components/medical-record/` | Create progress note form |
| `ProgressNoteCard.vue` | `src/components/medical-record/` | Single progress note display card |
| `MedicalOrderList.vue` | `src/components/medical-record/` | List of medical orders grouped by category |
| `MedicalOrderForm.vue` | `src/components/medical-record/` | Create medical order form |
| `MedicalOrderCard.vue` | `src/components/medical-record/` | Single medical order display card. Shows the workflow state badge and exposes the contextual transition buttons gated by permission, category shape, and current state: Authorize / Reject / Emergency authorize (auth-required only) / Mark in progress (results-bearing in `AUTORIZADO` only, label varies by category) / Upload result document / Discontinue. Discontinue is hidden for `EN_PROCESO`. |
| `MedicalOrderStateBadge.vue` | `src/components/medical-record/` | Shared status pill with color mapping per state (green activa, gray solicitado, red no_autorizado, green autorizado, amber en_proceso, blue resultados_recibidos, secondary descontinuado). Used by the card and the dashboard. |
| `MedicalOrderEmergencyAuthorizeDialog.vue` | `src/components/medical-record/` | Modal that prompts for the emergency-authorization reason (radio group of structured reasons + optional note). Required note when reason = `OTHER`. Posts to the emergency-authorize endpoint. |
| `MedicalOrdersByStateView.vue` | `src/views/medical-orders/` | Cross-admission dashboard at `/medical-orders`. Filters by status (multi-select) and category (multi-select). Default filter selects the action-needed buckets: `SOLICITADO`, `AUTORIZADO`, `EN_PROCESO`. |
| `RichTextEditor.vue` | `src/components/common/` | Reusable rich text editor wrapper (PrimeVue Editor or TipTap) |

### Pinia Stores

| Store | Location | Description |
|-------|----------|-------------|
| `useClinicalHistoryStore` | `src/stores/clinicalHistory.ts` | Clinical history CRUD |
| `useProgressNoteStore` | `src/stores/progressNote.ts` | Progress notes CRUD, pagination |
| `useMedicalOrderStore` | `src/stores/medicalOrder.ts` | Medical orders CRUD, grouping |

### Routes

| Route | Component | Notes |
|-------|-----------|-------|
| `/admissions/:id` | existing admission detail | Clinical history, progress notes, and per-admission medical orders remain here as tabs |
| `/medical-orders` | `MedicalOrdersByStateView.vue` | New top-level page. Sidebar entry visible to roles holding `medical-order:read`. Default filter is `SOLICITADO`. |

### Validation (Zod Schemas)

```typescript
// src/schemas/clinicalHistory.ts
import { z } from 'zod'

export const clinicalHistorySchema = z.object({
  consultationReason: z.string().optional(),
  currentCondition: z.string().optional(),
  previousMentalHealth: z.string().optional(),
  familySocialEconomicSituation: z.string().optional(),
  familyMedicalHistory: z.string().optional(),
  familyPsychiatricHistory: z.string().optional(),
  personalMedicalHistory: z.string().optional(),
  personalSurgicalHistory: z.string().optional(),
  personalAllergies: z.string().optional(),
  personalPsychiatricHistory: z.string().optional(),
  personalGynecologicalHistory: z.string().optional(),
  substanceUse: z.string().optional(),
  psychomotorDevelopment: z.string().optional(),
  mentalStatusExam: z.string().optional(),
  temperamentCharacter: z.string().optional(),
  previousLabResults: z.string().optional(),
  previousTreatments: z.string().optional(),
  generalObservations: z.string().optional(),
  diagnosis: z.string().optional(),
  managementPlan: z.string().optional(),
  prognosis: z.string().optional(),
})

// src/schemas/progressNote.ts
export const progressNoteSchema = z.object({
  subjectiveData: z.string().min(1, 'Subjective data is required'),
  objectiveData: z.string().min(1, 'Objective data is required'),
  analysis: z.string().min(1, 'Analysis is required'),
  actionPlans: z.string().min(1, 'Action plans are required'),
})

// src/schemas/medicalOrder.ts
export const medicalOrderCategoryEnum = z.enum([
  'ORDENES_MEDICAS',
  'MEDICAMENTOS',
  'LABORATORIOS',
  'REFERENCIAS_MEDICAS',
  'PRUEBAS_PSICOMETRICAS',
  'ACTIVIDAD_FISICA',
  'CUIDADOS_ESPECIALES',
  'DIETA',
  'RESTRICCIONES_MOVILIDAD',
  'PERMISOS_VISITA',
  'OTRAS',
])

export const administrationRouteEnum = z.enum([
  'ORAL',
  'IV',
  'IM',
  'SC',
  'TOPICAL',
  'INHALATION',
  'RECTAL',
  'SUBLINGUAL',
  'OTHER',
])

export const medicalOrderSchema = z.object({
  category: medicalOrderCategoryEnum,
  startDate: z.string().date('Invalid date format'),
  medication: z.string().max(255).optional(),
  dosage: z.string().max(100).optional(),
  route: administrationRouteEnum.optional(),
  frequency: z.string().max(100).optional(),
  schedule: z.string().max(255).optional(),
  observations: z.string().optional(),
})

export const medicalOrderStatusEnum = z.enum([
  'ACTIVA',
  'SOLICITADO',
  'NO_AUTORIZADO',
  'AUTORIZADO',
  'EN_PROCESO',
  'RESULTADOS_RECIBIDOS',
  'DESCONTINUADO',
])

export const rejectMedicalOrderSchema = z.object({
  reason: z.string().max(500).optional(),
})

export const emergencyAuthorizationReasonEnum = z.enum([
  'PATIENT_IN_CRISIS',
  'AFTER_HOURS_NO_ADMIN',
  'FAMILY_UNREACHABLE',
  'OTHER',
])

export const emergencyAuthorizeMedicalOrderSchema = z.object({
  reason: emergencyAuthorizationReasonEnum,
  reasonNote: z.string().max(500).optional(),
}).refine(
  (data) => data.reason !== 'OTHER' || (data.reasonNote != null && data.reasonNote.trim() !== ''),
  { message: 'reasonNote is required when reason is OTHER', path: ['reasonNote'] },
)
```

---

## Implementation Notes

- **Rich Text Editor**: Use PrimeVue's `Editor` component (Quill-based) for rich text fields. Alternatively, consider TipTap for more control.
- **Tab Structure**: Add a `MedicalRecordTabs` component to the existing `AdmissionDetail.vue` that contains three tabs: Clinical History, Progress Notes, Medical Orders.
- **Append-Only Pattern (Clinical History, Progress Notes, Medical Orders)**: Services check user role before allowing updates. Only users with ADMIN role can call update endpoints for these record types. For progress notes specifically, `ProgressNoteService.updateProgressNote()` must call an `assertAdmin()` helper that throws `ForbiddenException` when `!currentUser.hasRole("ADMIN")`, independently of the controller's `@PreAuthorize`. Independently, the service must throw `BadRequestException` if the admission is discharged — even for ADMIN. `ProgressNoteResponse.canEdit` is computed as `currentUser.isAdmin && admission.isActive` so the frontend can hide the edit button without re-deriving the rule. **Do not** rely on `@PreAuthorize` alone — these are domain rules, not just role rules.
- **Audit Display**: Show `createdBy`, `createdAt`, `updatedBy`, `updatedAt` on all entries using a reusable `AuditInfo` component.
- **Grouping Medical Orders**: The API returns medical orders grouped by category. Frontend can use PrimeVue's `Accordion` or `TabView` to display categories.
- **PostgreSQL Enums**: Use `CREATE TYPE ... AS ENUM` for category and route fields. In Kotlin, map with `@Enumerated(EnumType.STRING)` and `@Column(columnDefinition = "...")`.
- **Existing Patterns**: Follow `AdmissionController`/`AdmissionService` patterns for new controllers and services.
- **i18n**: All category names and field labels need Spanish translations in locale files.

---

## QA Checklist

### Backend
- [ ] All functional requirements implemented
- [ ] Entities extend `BaseEntity`
- [ ] Entities have `@SQLRestriction("deleted_at IS NULL")`
- [ ] DTOs used in controllers (no entity exposure)
- [ ] Input validation in place
- [ ] Clinical history: one per admission constraint enforced
- [ ] Progress notes: doctor / nurse / chief nurse can create on active admission
- [ ] Progress notes: doctor / nurse / chief nurse update returns 403 (every role except ADMIN)
- [ ] Progress notes: ADMIN can update any note on active admission
- [ ] Progress notes: create denied on discharged admission for every role (400)
- [ ] Progress notes: update denied on discharged admission for every role including ADMIN (400)
- [ ] Progress notes: `canEdit` returned on every `ProgressNoteResponse` and equals `isAdmin && admissionActive`
- [ ] Progress notes: service-layer admin assertion holds even when `progress-note:update` is granted to a non-admin role (defense-in-depth test)
- [ ] Medical orders: append-only for non-admins
- [ ] Medical orders: directive categories created in `ACTIVA`, auth-required in `SOLICITADO`
- [ ] Medical orders: authorize endpoint rejects directive categories with 400
- [ ] Medical orders: discontinue allowed from `ACTIVA`/`SOLICITADO`/`AUTORIZADO`, blocked from `EN_PROCESO` and terminal states
- [ ] Medical orders: authorize / reject endpoints enforce `SOLICITADO` precondition
- [ ] Medical orders: emergency-authorize endpoint stamps `emergency_*` fields, requires `reasonNote` when reason is `OTHER`, transitions `SOLICITADO → AUTORIZADO`
- [ ] Medical orders: mark-in-progress endpoint enforces `AUTORIZADO` + results-bearing category, stamps `in_progress_at/by`
- [ ] Medical orders: uploading a document on `AUTORIZADO` or `EN_PROCESO` auto-transitions to `RESULTADOS_RECIBIDOS` (in same transaction, with `results_received_at/by` stamped)
- [ ] Medical orders: billing charge fires on `AUTORIZADO` for results-bearing categories with linked inventory only — `MEDICAMENTOS` is excluded (bills via `InventoryDispensedEvent`); both normal authorize and emergency-authorize trigger the charge, not on creation
- [ ] Cross-admission `GET /api/v1/medical-orders` paged endpoint with status/category/date filters
- [ ] Audit trail (createdBy, updatedBy) properly tracked
- [ ] Permission checks on all endpoints
- [ ] Unit tests written and passing
- [ ] Integration tests written and passing (Testcontainers)
- [ ] Detekt passes (no new violations)
- [ ] OWASP dependency-check passes

### Frontend
- [ ] MedicalRecordTabs component integrated into AdmissionDetail
- [ ] Clinical history form with all 16+ rich text fields
- [ ] Clinical history view displays formatted content
- [ ] Progress notes list with pagination
- [ ] Progress note form with rich text editors
- [ ] Medical orders grouped by category
- [ ] Medical order form with category dropdown
- [ ] Medical order card shows current state badge with the right color per state
- [ ] State transition buttons appear/disappear based on current state and permission
- [ ] Orders-by-state screen at `/medical-orders` with status and category filters
- [ ] Sidebar entry for orders-by-state visible only to roles with `medical-order:read`
- [ ] Uploading a result document from the orders-by-state screen also flips status to `RESULTADOS_RECIBIDOS`
- [ ] Rich text editor component working
- [ ] `RichTextEditor` strips inline `style` attributes, `class` attributes, MS-Office namespaced tags, and any tag outside the supported toolbar set on paste (Quill clipboard matcher)
- [ ] `ProgressNoteCard.vue` renders SOAP fields with sanitized `v-html` (no `{{ text }}` interpolation for rich-text fields)
- [ ] `MedicalOrderCard.vue` renders `observations` with sanitized `v-html` (no `{{ text }}` interpolation)
- [ ] i18n label `medicalRecord.medicalOrder.fields.inventoryItem` reads "Insumo/servicio a facturar" (es) / "Billable supply / service" (en)
- [ ] Audit info displayed on all entries
- [ ] Create buttons hidden for unauthorized users
- [ ] Clinical-history, progress-note, and medical-order edit buttons shown only for ADMIN
- [ ] Progress-note edit button reads `canEdit` from the server response (true only for ADMIN on active admissions); always hidden for doctors / nurses / chief nurses
- [ ] Pinia stores implemented
- [ ] Form validation with VeeValidate + Zod
- [ ] Error handling implemented
- [ ] ESLint/oxlint passes
- [ ] i18n keys added for all user-facing text (Spanish)
- [ ] Unit tests written and passing (Vitest)

### E2E Tests (Playwright)
- [ ] Doctor creates clinical history
- [ ] Nurse cannot create clinical history
- [ ] Doctor/nurse views clinical history
- [ ] Admin edits clinical history
- [ ] Doctor/nurse cannot edit clinical history
- [ ] Doctor creates progress note
- [ ] Nurse creates progress note
- [ ] Chief nurse creates progress note (V096 grant)
- [ ] Doctor cannot edit any progress note (own or others') — 403
- [ ] Nurse cannot edit any progress note (own or others') — 403
- [ ] Chief nurse cannot edit any progress note — 403
- [ ] Admin edits any progress note on an active admission — 200
- [ ] Create progress note denied on discharged admission for every role — 400
- [ ] Update progress note denied on discharged admission for ADMIN — 400
- [ ] Edit button hidden in the UI for non-admins; visible for admin on active admissions; hidden for admin on discharged admissions
- [ ] Doctor creates medical order
- [ ] Nurse cannot create medical order
- [ ] Doctor/nurse cannot edit medical orders
- [ ] Admin edits medical order
- [ ] Medical order discontinue flow
- [ ] Admin staff authorizes a `SOLICITADO` order
- [ ] Admin staff rejects a `SOLICITADO` order with reason
- [ ] Doctor emergency-authorizes a `SOLICITADO` order (with reason); audit fields stamped
- [ ] Emergency-authorize with reason `OTHER` and no `reasonNote` is denied (400)
- [ ] Authorize on a directive order (e.g. DIETA) is denied (400)
- [ ] Nurse marks an `AUTORIZADO` lab order as `EN_PROCESO`
- [ ] Marking in-progress on a non-results category is denied
- [ ] Marking in-progress on a `SOLICITADO` order is denied
- [ ] Discontinue from `EN_PROCESO` is denied (sample at the lab)
- [ ] Doctor uploads a result document on an `EN_PROCESO` order and it auto-transitions to `RESULTADOS_RECIBIDOS`
- [ ] Doctor uploads a result document directly on an `AUTORIZADO` order (skips `EN_PROCESO`) and it auto-transitions to `RESULTADOS_RECIBIDOS`
- [ ] Authorizing an order that is not `SOLICITADO` is denied
- [ ] Orders-by-state screen lists orders across admissions and links back to admission detail
- [ ] Emergency-authorized orders are visually distinguishable in the dashboard
- [ ] Permission denied scenarios displayed correctly
- [ ] Rich text formatting preserved end-to-end: type bold + bullet list + paragraphs into a progress note SOAP field, save, reload, and assert the rendered DOM contains `<strong>`, `<ul><li>`, `<p>` (not literal tags as text)
- [ ] Rich text formatting preserved end-to-end: same assertion for a medical order `observations` field on the admission detail card and on the cross-admission orders-by-state screen
- [ ] Paste sanitization: pasting HTML with inline `style` attributes and unsupported tags into a progress note or medical-order observations field strips the inline styles and unsupported tags from both the editor view and the saved payload

### General
- [ ] API contract documented
- [ ] Database migrations tested
- [ ] Feature documentation updated
- [ ] Reviewed by project owner

---

## Documentation Updates Required

### Must Update

- [ ] **[CLAUDE.md](../../CLAUDE.md)**
  - Add Medical/Psychiatric Record to "Implemented Features" section
  - Update migration count (V029-V032, V092-V093)
  - Add bullet for "Medical Order Workflow (three category-driven shapes: directive, authorize-only, authorize+execute+results) with emergency-authorize override for doctors and cross-admission orders-by-state dashboard"

- [ ] **[docs/roles-functionality-matrix.md](../roles-functionality-matrix.md)** and **[.es.md](../roles-functionality-matrix.es.md)**
  - Add `medical-order:authorize`, `medical-order:emergency-authorize`, and `medical-order:mark-in-progress` to the matrix
  - Note that ADMINISTRATIVE_STAFF gains visibility into medical orders for the authorization workflow
  - Note that NURSE gains `mark-in-progress` for the sample-taking worklist

### Review for Consistency

- [ ] **[ARCHITECTURE.md](../architecture/ARCHITECTURE.md)**
  - Add new entities to entity diagram if exists

### Code Documentation

- [ ] **`ClinicalHistory.kt`** - Document one-per-admission constraint
- [ ] **`ProgressNote.kt`** - Document SOAP structure
- [ ] **`MedicalOrder.kt`** - Document categories and discontinue flow
- [ ] **`ClinicalHistoryService.kt` / `ProgressNoteService.kt` / `MedicalOrderService.kt`** - Document admin-only update behavior and discharge protection. `ProgressNoteService` should call out the `canEdit = isAdmin && admissionActive` rule and that the service-layer admin assertion is intentional defense-in-depth on top of `@PreAuthorize`.

---

## Related Docs/Commits/Issues

- Related feature: [Patient Admission](./patient-admission.md)
- Related feature: [Admission PDF Export](./admission-export.md) - Server-side export sanitizes stored rich text again at render time before writing the PDF.
- Related entity: `Admission` (parent entity for all medical records)
- Related pattern: Append-only with admin override (used by Clinical History, Progress Notes, Medical Orders, Nursing Notes, and Vital Signs — see [`nursing-module.md`](./nursing-module.md) revisions 1.3 and 1.4 for nursing notes and vital signs; similar to audit log patterns)

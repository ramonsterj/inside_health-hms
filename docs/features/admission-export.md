# Feature: Admission PDF Export

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-05-11 | @paniagua | Initial draft |
| 1.1 | 2026-05-11 | @paniagua | Drop async path, job entity, and server-side retention — generation is synchronous and the PDF is streamed once, never stored on disk |
| 1.2 | 2026-05-11 | Codex | Resolve review findings: temp-file render/hash flow, concrete audit-log schema changes, current locale source, current attachment types, billing adjustment model, and repeatable-read snapshot semantics |

---

## Overview

Generate a single PDF document containing **everything captured for a patient from the moment of admission up to the time the report is requested**, with uploaded PDF/image files (consent forms, ID documents, medical-order results, multi-document uploads) included as an *Additional Documents* appendix.

The export is initiated on demand by Administrative Staff. The PDF is the canonical printable record of an admission, suitable for archival, transfer to another facility, or fulfilling a patient request for their record.

---

## Use Case / User Story

> *As Administrative Staff, I want to download a complete PDF of an admission — including clinical history, progress notes, medical orders and their results, nursing notes, vital signs, billing, and every uploaded file — so that I can hand the patient or an external party a single, self-contained record.*

Concrete cases:

- A patient is discharged; admin staff exports the full record for the family.
- An insurance audit requests the complete clinical and billing trail for an admission.
- A patient is transferred to another hospital and the receiving facility needs the full chart.

---

## Authorization / Role Access

| Action | Required Role(s) | Permission | Notes |
|--------|------------------|------------|-------|
| Export admission PDF | `ADMINISTRATIVE_STAFF`, `ADMIN` | `admission:export-pdf` | Sole permission; not granted to DOCTOR, NURSE, CHIEF_NURSE, PSYCHOLOGIST, TREASURY by default |
| View export audit trail | `ADMIN` | existing `audit:read` | Each export is recorded in `audit_logs` |

Notes:

- `ADMIN` always has the permission via the seed admin role. Administrative Staff get it through the migration that introduces the permission.
- The button must be hidden — not just disabled — from users that lack the permission. Backend rejection (`403`) is still authoritative; the UI hide is just to avoid confusion.

---

## Functional Requirements

### Scope of data included

The PDF must include, in this order, every record attached to the admission that exists at the moment of generation. Sections that have no records render a one-line "No records" placeholder so the structure of the PDF is predictable and auditable.

1. **Cover page**
   - Hospital name and logo (i18n `app.hospital.name`)
   - Patient full name, date of birth, sex, marital status, ID document number, address
   - Admission ID, admission type, admission date+time, discharge date+time (if discharged), treating physician, room, triage code
   - **Generation metadata**: generated-at timestamp, generated-by user, report version (`v1.0`)
   - Note: the report is a point-in-time snapshot — explicit watermark/header "Generated on dd/MM/yyyy - HH:mm"

2. **Patient demographics & emergency contacts**
   - All `Patient` fields including derived age (from `dateOfBirth`)
   - All `EmergencyContact` rows (name, relationship, phone)

3. **Consulting physicians** (from `AdmissionConsultingPhysician`)

4. **Clinical history** (`ClinicalHistory` — single record per admission)
   - All rich-text sections rendered as formatted HTML (sanitized — see § Rich-Text Sanitization)

5. **Progress notes** (`ProgressNote`, chronological ascending)
   - SOAP fields (`subjectiveData`, `objectiveData`, `analysis`, `actionPlans`), author, timestamp

6. **Medical orders** (`MedicalOrder`, chronological ascending), grouped by category
   - Order body, category, state, authorization metadata (authorized by/at, rejected by/at and reason, emergency-authorization reason if any)
   - For orders that produced results: results metadata + reference to the file in the appendix
   - Linked `MedicationAdministration` rows under their parent order
   - Linked `MedicalOrderDocument` rows referenced into the appendix

7. **Psychotherapy activities** (`PsychotherapyActivity`)

8. **Nursing notes** (`NursingNote`, chronological ascending)

9. **Vital signs** (`VitalSign`, chronological ascending) — rendered as a table

10. **Medication administrations** (`MedicationAdministration`) — separate flat list in addition to grouped view under orders, for auditability

11. **Billing**
    - All `PatientCharge` rows attached to the admission
    - Adjustment charges are represented as `PatientCharge` rows with `ChargeType.ADJUSTMENT`; there is no separate `BillingAdjustment` entity in v1.
    - All `Invoice` rows generated for this admission (per `hospital-billing-system.md` and `clinical-event-billing-automation.md`)

12. **Uploaded documents — index**
    - A table of every file attached to the admission with: source (consent / patient ID / admission document / medical-order document), original filename, uploaded-by, uploaded-at, MIME type, byte size, SHA-256 checksum, and appendix page number.
    - The checksum and page number are required so a reader can verify nothing was tampered with during merge.

13. **Appendix — Additional Documents**
    - All uploaded PDF/image files appended in the order they appear in the index.
    - Each appendix entry is preceded by a one-page separator with the original filename, source, and checksum.
    - **PDFs** and current supported images (**PNG, JPG, JPEG**) are embedded inline. PDFs are merged page-by-page; images are placed centered on a page sized to fit.
    - v1 does not add new upload types. If future migrations allow non-PDF/image attachments, those files must be represented by a separator page with a clear "This file is not a PDF or image and could not be embedded" notice plus the original filename, checksum, and MIME type; the file itself must not be smuggled into the PDF.

### Snapshot semantics

- The database read phase runs inside a single read-only transaction with `Isolation.REPEATABLE_READ`; render inputs are copied into export DTOs inside that transaction. Rendering and file merging happen after the read transaction closes.
- The cover page records the generation timestamp; that timestamp is the "as-of" date for every section.
- Soft-deleted rows are excluded (the existing `@SQLRestriction("deleted_at IS NULL")` on every entity is sufficient — no additional filtering needed).

### Delivery

- **Generation is always synchronous and server-side.** The endpoint streams `application/pdf` directly to the browser as a binary response. There is no async/job path in v1.
- **The PDF is never retained on the server.** Generation writes to a request-scoped OS temp file, computes byte size and SHA-256 from that finalized file, sets response headers, streams the file to the browser, and deletes the temp directory in a `finally` block. No generated PDF is written under `app.file-storage.base-path`.
- Because the response is one-shot, **there is no concept of a download history** in v1. If administrative staff need the PDF again, they regenerate it. This is acceptable because every generation attempt is recorded in `audit_logs` with the SHA-256 of the produced PDF when generation succeeds.
- Filename: `admission-{admissionId}-{patientLastName}-{yyyyMMdd-HHmm}.pdf`, sanitized for filesystem-unsafe characters.
- **Admission state**: the export is allowed regardless of admission status (`ACTIVE`, `DISCHARGED`, future states). The PDF is a point-in-time snapshot and the cover page makes this explicit.

### i18n

- Backend PDF labels, section headings, field names, and error messages are message-source keys under `admission.export.*` in `api/src/main/resources/messages/messages.properties` and `messages_es.properties`.
- The generated PDF language resolves from the authenticated user's `localePreference`; if not set, fall back to the request `Accept-Language`, then to the backend default locale. Patient locale is not used in v1 because `Patient` has no locale field.
- The frontend button/error labels use matching keys under `admission.export.*` in `web/src/i18n/locales/en.json` and `es.json`.

### Audit logging

- V099 extends the existing audit model with `AuditAction.ADMISSION_EXPORT`, nullable `status VARCHAR(20)`, and nullable `details JSONB`.
- Every successful PDF generation writes an `audit_logs` row with `action = ADMISSION_EXPORT`, `entity_type = 'Admission'`, `entity_id = admissionId`, `status = 'SUCCESS'`, and `details` containing generated-at timestamp, byte size, SHA-256, section counts, attachment counts, and any skipped/failed attachment ids. No PHI is stored in `details`.
- Expected pre-generation denials (`401`, `403`) do not write export audit rows because no export attempt has begun. `404` and `413` write `FAILED` rows only after an authenticated user with `admission:export-pdf` reaches export service logic.
- Unexpected generation failures write `status = 'FAILED'` with the exception class and failure phase only. Do not include rich-text content, patient names, file names, or other PHI in audit details.
- The SHA-256 is computed from the finalized request-scoped temp file before headers are sent; this is what makes `X-Admission-Export-Sha256` and the audit detail reliable.

---

## Acceptance Criteria / Scenarios

1. `ADMINISTRATIVE_STAFF` requests an export for an `ACTIVE` admission with full data → server returns `200 OK`, `Content-Type: application/pdf`, body is a valid PDF, every section listed above is present.
2. `ADMINISTRATIVE_STAFF` requests an export for a `DISCHARGED` admission → behaves identically to (1). Admission state does not gate the export in v1.
3. `ADMINISTRATIVE_STAFF` requests an export for an admission with no progress notes → the *Progress Notes* section renders the localized "No records" placeholder; the rest of the PDF is unaffected.
4. `DOCTOR`, `NURSE`, `CHIEF_NURSE`, `PSYCHOLOGIST` request an export → `403 Forbidden`, no PDF generated, no export audit row.
5. Unauthenticated request → `401 Unauthorized`.
6. Request for a non-existent admission → `404 Not Found`.
7. Request for a soft-deleted admission → `404 Not Found` (existing `@SQLRestriction` behavior).
8. Rich-text content stored with a `<script>` tag (planted via a hypothetical bypass of frontend paste sanitization) is rendered as plain text in the PDF, not executed and not rendered as a tag. Verified by an integration test that inserts a poisoned `ClinicalHistory` row and asserts the PDF text content.
9. Admission with one PDF attachment, one PNG attachment, and one JPEG attachment → PDF appendix contains the PDF pages merged and both images on their own fitted pages. The index table lists all three with their checksums.
10. Successful generation audit log contains one `ADMISSION_EXPORT` row with the correct user id, admission id, `SUCCESS` status, byte size, and PDF SHA-256. Auth/permission denials do not create export audit rows.
11. Filename matches `admission-{id}-{lastName}-{yyyyMMdd-HHmm}.pdf`.
12. Generated dates in the PDF use `dd/MM/yyyy` and `HH:mm`; no ISO strings leak into rendered content.
13. After the response is flushed, no PDF artifact remains under `app.file-storage.base-path` for the admission. Verified by an integration test that snapshots the storage tree before and after the call.

---

## Non-Functional Requirements

- **Performance**: a 90-day admission with ~300 progress notes, ~1000 vital-sign rows, ~50 medical orders and 20 attachments (total ≤ 50 MB of appendix payload) generates and finishes streaming in < 15 s on the production instance.
- **Memory**: rendering must use file-backed scratch storage — render the body PDF and merged appendix into a request-scoped OS temp directory, compute SHA-256 from the finalized file, then stream that file to the servlet `OutputStream`. Peak heap stays under 512 MB regardless of appendix size. The temp scratch directory is deleted in a `finally` block.
- **Request timeout**: because the export is synchronous, the controller method runs inside a single HTTP request. Configure a generous server-level timeout (e.g. Tomcat `connectionTimeout` / `asyncTimeout`) and document the operational expectation that exports finish within ~60 s. Admissions that exceed this are a v2 problem — see § Risks (R2).
- **Size cap**: hard limit of 500 MB per generated PDF. The pre-flight estimate from the attachment index decides up-front; if it exceeds the cap the endpoint returns `413 Payload Too Large` with an i18n error message and writes a `FAILED` audit row. Generation never starts in that case.
- **Security**: every input is the admission id only; no user-supplied content reaches the PDF generator unsanitized. All rich-text fields pass through server-side sanitization (see § Risks).
- **Reliability**: failures during merge (corrupt attachment, missing file on disk) must not abort the whole export. The faulty attachment is replaced by a separator page noting the failure; the generation continues. The failure is recorded in the audit-log detail by id/source only, without PHI.
- **No server-side retention**: no PDF artifact, partial or complete, may remain on the server after the request completes or fails. Enforced by the `finally` block that wipes the scratch dir.

---

## Date / Time Conformance

- [x] Backend date-only fields use `LocalDate` + `DATE`; event timestamps use `LocalDateTime` + `TIMESTAMP`. No `String`-stored dates, no `TIMESTAMPTZ`. No new schema fields are required for this feature, but the renderer uses `DateTimeFormatter.ofPattern("dd/MM/yyyy")` and `ofPattern("HH:mm")` exclusively.
- [x] The PDF cover-page timestamp ("Generated on dd/MM/yyyy - HH:mm") and the filename timestamp use the same formatter. No `toString()` / `Instant` rendering leaks into user-facing output.
- [x] The frontend has no timestamp rendering for this feature beyond the download filename (server-supplied) and an inline loading state — no `toLocaleString` / `d(...)` calls.
- [x] No `<DatePicker>` is used by this feature (no date input).
- [x] No `Date → API string` conversions are needed.

A new Kotlin helper, `AdmissionExportDateFormatter`, centralizes the `dd/MM/yyyy` and `HH:mm` patterns so the PDF renderer never reimplements them.

---

## API Contract

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| GET | `/api/v1/admissions/{id}/export.pdf` | - | `application/pdf` (binary stream) | Yes (`admission:export-pdf`) | Generates and streams the full admission PDF in one synchronous call. Nothing is stored on the server. |

### Response headers

```
Content-Type: application/pdf
Content-Disposition: attachment; filename="admission-1234-perez-20260511-1430.pdf"
X-Admission-Export-Sha256: 9f86d0...
```

### Error responses

| Status | Trigger |
|--------|---------|
| `401 Unauthorized` | Missing/expired access token |
| `403 Forbidden` | Authenticated user lacks `admission:export-pdf` |
| `404 Not Found` | Admission id does not exist or is soft-deleted |
| `413 Payload Too Large` | Pre-flight estimate exceeds the 500 MB hard cap |
| `500 Internal Server Error` | Unexpected rendering failure (audit-logged as `FAILED`) |

Note: `GET` is chosen because the call does not mutate admission domain data. It does write an audit row for authorized attempts. The SPA must call it through the authenticated axios wrapper; a plain browser address-bar request will not include the JWT authorization header.

---

## Database Changes

### New Entities

**None.** Because generation is synchronous and the PDF is never retained, there is no job row to track. The audit-log row is the only durable record of an export, and `audit_logs` already exists.

### New Permissions

| Code | Description | Resource | Action |
|------|-------------|----------|--------|
| `admission:export-pdf` | Export full admission PDF | `admission` | `export-pdf` |

Granted to `ADMIN` and `ADMINISTRATIVE_STAFF` roles. No other role receives it by default.

### New Migrations

| Migration | Description |
|-----------|-------------|
| `V099__add_admission_export_permission_and_audit_fields.sql` | Inserts `admission:export-pdf` permission, grants it to `ADMIN` and `ADMINISTRATIVE_STAFF`, and adds nullable `status VARCHAR(20)` plus nullable `details JSONB` columns to `audit_logs`. |

### Domain / audit model changes

The export does **not** add new fields to admission-domain entities and does **not** introduce any new entity. Everything it needs is already on `Admission`, `Patient`, `EmergencyContact`, `ClinicalHistory`, `ProgressNote`, `MedicalOrder`, `MedicalOrderDocument`, `MedicationAdministration`, `NursingNote`, `VitalSign`, `PsychotherapyActivity`, `PatientCharge`, `Invoice`, `AdmissionDocument`, `AdmissionConsentDocument`, `PatientIdDocument`, and `AdmissionConsultingPhysician`.

The existing `AuditAction` enum gains `ADMISSION_EXPORT`, and `AuditLog` gains nullable `status` and `details` fields. This intentionally keeps the export as a read-side concern with zero write footprint outside `audit_logs`.

---

## Backend Implementation

### Package layout

```
com.insidehealthgt.hms.export/
├── controller/AdmissionExportController.kt
├── service/AdmissionExportService.kt          # orchestration, temp-file lifecycle, audit logging
├── service/AdmissionExportSnapshotService.kt  # repeatable-read fetch into DTO snapshot
├── service/AdmissionExportRenderer.kt         # produces the body PDF (no appendix)
├── service/AdmissionExportAppendixBuilder.kt  # merges attachments into the temp PDF
└── service/AdmissionExportHtmlSanitizer.kt    # jsoup-based sanitizer (see Risks)
```

No export entity, repository, or scheduler is introduced. The controller delegates to `AdmissionExportService`, which creates a temp directory with `Files.createTempDirectory(...)`, renders and merges the PDF there, computes SHA-256 and byte size, writes the audit row, sets headers, streams the finalized file to the HTTP response, and deletes the temp directory in a `finally` block.

### Libraries

Add to `api/build.gradle.kts`:

- `io.github.openhtmltopdf:openhtmltopdf-pdfbox:1.1.37` — HTML → PDF rendering with PDFBox 3 support, on top of PDFBox which is already present.
- `org.jsoup:jsoup:1.18.1` — server-side HTML sanitizer.

PDFBox itself (`org.apache.pdfbox:pdfbox:3.0.7`) is already a dependency.

### Rich-text sanitization

A dedicated `AdmissionExportHtmlSanitizer` runs every rich-text field through `Jsoup.clean(...)` with a whitelist of exactly the tags the platform's rich-text editor emits: `p`, `br`, `strong`, `em`, `u`, `s`, `ol`, `ul`, `li`, `h1`–`h4`, `blockquote`, `code`, `pre`, `span` with a constrained `style` attribute. Everything else (including all `<script>`, `<iframe>`, event handlers, javascript: URLs) is stripped. This is enforced **at PDF rendering time**, independently of the frontend paste-time sanitization documented in `medical-psychiatric-record.md:322`. See § Risks.

### Concurrency model

- Regular Spring MVC; no async/job executor in v1.
- `AdmissionExportSnapshotService` fetches every database-backed export DTO inside `@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)`, then returns detached DTOs to the renderer. The renderer must not lazily access JPA entities after this point.
- The success audit-log write happens after the temp PDF is fully generated and hashed but before the response body is committed, in a separate `REQUIRES_NEW` transaction. If audit persistence fails, return `500` and delete the temp file; do not stream an unaudited successful export.
- If streaming the finalized file to the client fails after headers/body have begun, the server cannot reliably change the response. Log the delivery failure without PHI; do not rewrite the already-created `SUCCESS` generation audit row.
- No bounded async pool, no rate limiter in v1. If the platform later observes export thundering-herd from administrative staff, add a per-user throttle.

---

## Frontend Changes

### Components

| Component | Location | Description |
|-----------|----------|-------------|
| `AdmissionExportButton.vue` | `web/src/components/admissions/` | Button on the admission detail screen. Hidden when the user lacks `admission:export-pdf`. On click, calls the API via the authenticated axios instance with `responseType: 'blob'`, then triggers a download from the response blob. Shows a spinner while the request is in flight and an inline error if the server responds with 4xx/5xx. |

### Pinia store

No new store is required. The download is a one-shot fetch; component-local state is sufficient for the loading spinner and error message.

### Routes

No new routes. The button and dialog live within the existing admission detail view.

### i18n keys (excerpt — full list to be filled in during implementation)

```
admission.export.button.label
admission.export.button.loading
admission.export.error.forbidden
admission.export.error.not_found
admission.export.error.too_large
admission.export.error.generic
admission.export.appendix.title
admission.export.appendix.embedded_notice
admission.export.appendix.not_embeddable_notice
admission.export.sections.cover
admission.export.sections.demographics
admission.export.sections.clinical_history
admission.export.sections.progress_notes
admission.export.sections.medical_orders
admission.export.sections.nursing_notes
admission.export.sections.vital_signs
admission.export.sections.medication_administrations
admission.export.sections.psychotherapy
admission.export.sections.billing
admission.export.sections.documents_index
admission.export.no_records
```

Keys must exist in both `en.json` and `es.json`. `es.json` is the canonical source (the platform targets a Guatemala hospital).

### Validation

No new Zod schemas — the export takes no client-side input beyond the admission id from the URL.

---

## Implementation Notes

- Follow the existing pattern in `MedicalOrderDocumentController` for binary downloads and `PatientChargeService` for read-only aggregated views.
- The renderer should treat each section as a function that takes the detached export snapshot and returns an HTML fragment. The fragments are concatenated and rendered to PDF once. This keeps the renderer testable section-by-section without spinning up a real PDF parser for every assertion.
- All entity fetches inside the snapshot transaction must be paginated, stream-fetched, or constrained by `admission_id`. Avoid `findAll(...)` without bounds on `VitalSign`, `MedicationAdministration`, or `PatientCharge`, since long admissions can have thousands of rows. Use Spring Data's `Stream<T>` return type plus `try-with-resources` where appropriate.
- Do not log rich-text content. Audit-log detail must contain only ids, counts, timestamps, and the SHA-256.
- Set `Content-Disposition: attachment` (not `inline`). Because the PDF is finalized before streaming starts, flush the response only during the final file copy; do not call `flushBuffer()` during render/merge.

---

## QA Checklist

### Backend
- [x] All functional requirements implemented
- [x] No export/job entity is introduced (verified by code review)
- [x] `AuditAction.ADMISSION_EXPORT`, `AuditLog.status`, and `AuditLog.details` are implemented and exposed safely in admin audit views if needed
- [x] DTOs / streamed binaries used in controller (no entity exposure)
- [x] Input validation in place (admission id existence, permission check)
- [x] Snapshot fetch uses `Isolation.REPEATABLE_READ` and detached DTOs; renderer does not lazy-load JPA entities
- [x] Unit tests for `AdmissionExportRenderer` per section
- [x] Unit tests for `AdmissionExportHtmlSanitizer` covering `<script>`, `<iframe>`, `on*` handlers, `javascript:` URLs, mixed nested HTML
- [x] Integration test: full export of a seed admission, asserts PDF is valid, contains expected text, embeds expected attachments, and reports the SHA-256 in the `X-Admission-Export-Sha256` header
- [x] Integration test: same SHA-256 appears in the matching `audit_logs` row
- [x] Integration test: each unauthorized role returns 403 and unauthenticated returns 401, with no export audit rows for those denials
- [x] Integration test: authenticated `404` and `413` failures create `FAILED` export audit rows without PHI in `details`
- [x] Integration test: poisoned rich-text input is rendered as inert text
- [x] Integration test: storage tree under `app.file-storage.base-path` is byte-for-byte identical before and after a successful export
- [x] Integration test: request-scoped OS temp directory is deleted after success and after forced mid-render failure
- [x] Integration test: export works for `ACTIVE` and `DISCHARGED` admissions
- [x] Integration test: cross-admission isolation (R12) — sibling admission progress note must not leak into the exported PDF
- [x] Integration test: locale resolution — user `localePreference` overrides `Accept-Language`; falls back to `Accept-Language` when preference is null
- [x] Detekt passes (no new violations)
- [ ] OWASP dependency-check passes (note: openhtmltopdf and jsoup are new dependencies — verify CVEs)

### Frontend
- [ ] `AdmissionExportButton` hidden when user lacks `admission:export-pdf`
- [ ] Download triggers a browser save dialog with the correct filename
- [ ] Loading spinner shown while the request is in flight
- [ ] Error handling for 401, 403, 404, 413, 500 with localized messages
- [ ] ESLint/oxlint passes
- [ ] i18n keys present in both `en.json` and `es.json`
- [ ] Unit tests for the component (Vitest)

### E2E Tests (Playwright)
- [ ] Administrative staff exports an admission and the file is downloaded
- [ ] Doctor user cannot see the export button
- [ ] Localized labels render correctly in both Spanish and English

### General
- [ ] API contract documented
- [ ] Database migration tested (Flyway clean apply on Testcontainers)
- [ ] Feature documentation updated
- [ ] Reviewed by project owner

---

## Documentation Updates Required

### Must Update

- [ ] **[CLAUDE.md](../../CLAUDE.md)**
  - Add "Admission PDF Export" to "Implemented Features → Backend" and "Frontend"
  - Append "admission PDF export — `admission:export-pdf` permission granted to ADMIN and ADMINISTRATIVE_STAFF; audit logs gain `ADMISSION_EXPORT`, `status`, and `details`; no export table, generation is synchronous with request-scoped temp files (V099)" to the migrations list
- [ ] **[ARCHITECTURE.md](../architecture/ARCHITECTURE.md)**
  - Cross-reference: PDF rendering uses openhtmltopdf + jsoup; generation is synchronous, finalized in an OS temp directory for hash/header/audit, and never written to `app.file-storage.base-path`
- [ ] **[roles-functionality-matrix.md](../roles-functionality-matrix.md)** and **[roles-functionality-matrix.es.md](../roles-functionality-matrix.es.md)**
  - Add the `admission:export-pdf` row

### Review for Consistency

- [ ] **[medical-psychiatric-record.md](medical-psychiatric-record.md)**
  - Reference this feature from the "PDF export" mention at line 322 so the two stay in sync.
- [ ] **[multi-document-upload.md](multi-document-upload.md)** and **[file-system-storage.md](file-system-storage.md)**
  - Link to this spec so readers know where the appendix logic lives.

---

## Risks and Mitigations

| # | Risk | Likelihood | Impact | Mitigation |
|---|------|------------|--------|------------|
| R1 | **Stored XSS in rich-text fields** reaches the PDF renderer. Frontend paste-sanitization (`medical-psychiatric-record.md:322`) is the only current defense — a future bug or a direct API call could bypass it. | Medium | High (PHI exfiltration via the rendered PDF, or DoS via a payload that loops the parser) | Server-side `Jsoup.clean(...)` with a strict whitelist runs at render time, independent of paste-time sanitization. Defense in depth. Unit-tested against known XSS payloads (OWASP cheat sheet). |
| R2 | **Long-running synchronous request**: a large admission ties up a worker thread for tens of seconds and risks hitting Tomcat / load-balancer / browser timeouts. Because there is no async fallback in v1, the user just sees a failed download. | Medium | Medium | Hard cap of 500 MB enforced from the pre-flight estimate so the worst case is bounded. The PDF is finalized before response streaming so headers are reliable; Tomcat/load-balancer request timeouts must be raised above the documented 60 s expectation. Operationally, monitor p95 export duration and revisit if it climbs. |
| R3 | **Inconsistent snapshot**: a user edits a progress note while the export runs, producing a PDF whose summary header counts and section bodies disagree. | Low | Medium | Fetch detached export DTOs inside `@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)`, then render from the DTO snapshot only. Tested with a concurrent-mutation integration test. |
| R4 | **Attachment file missing on disk** (e.g. file storage out of sync with DB) aborts the entire export. | Low | Medium | Per-attachment try/catch in `AdmissionExportAppendixBuilder`; failure renders a separator page noting the missing file and the export continues. Failure recorded in the audit-log detail. |
| R5 | **Permission drift**: future role additions accidentally inherit `admission:export-pdf`. | Medium | High (unauthorized PHI access) | Permission is granted only to `ADMIN` and `ADMINISTRATIVE_STAFF` in V099; no role template grants it transitively. Add an integration test asserting that the set of roles holding `admission:export-pdf` is exactly `{ADMIN, ADMINISTRATIVE_STAFF}` to catch accidental future grants. |
| R6 | **PHI in logs or audit detail blob**: a careless `logger.info(...)` includes rendered rich-text content. | Medium | High | Code-review rule + a Detekt custom rule (or a simple grep test) that flags `logger.*` calls that interpolate from `ClinicalHistory`, `ProgressNote`, `NursingNote`, `MedicalOrder`. Audit-log detail contract limited to ids, counts, timestamps, SHA-256. |
| R7 | **Future non-embeddable attachments** (XLSX, DOCX, CSV, TXT) leave a gap in the "complete record" promise if upload rules expand later. | Low in v1 | Low–Medium | v1 upload rules only allow PDF/JPEG/PNG for admission and medical-order documents. If future migrations allow other types, represent them by a separator page with checksum and require staff to download the source file separately via the existing document endpoints. Future enhancement: server-side conversion of common Office formats to PDF (LibreOffice headless). |
| R8 | **Scratch-file leak on a mid-render failure** — the OS temp dir keeps partial PDF / attachment merger state if an exception bypasses the `finally`. | Low | Medium | All scratch I/O lives inside a single `Files.createTempDirectory("admission-export-")` deleted recursively in a `finally` block. Integration test (see QA checklist) asserts the temp dir is gone after success and forced failure. The OS temp dir is outside `app.file-storage.base-path`, so even a leak does not pollute the application storage tree. |
| R9 | **CVEs in new dependencies** (`openhtmltopdf`, `jsoup`). | Low | Medium | OWASP dependency-check already runs in CI. The first PR must show a clean dependency-check report; future bumps are gated by the same check. |
| R10 | **No re-download path**: because nothing is stored, a user whose download fails mid-stream has to regenerate. For a large admission this doubles the server load and the wait. | Medium | Low | Acceptable for v1 given the simplification trade-off explicitly chosen by the project owner. Mitigated by the streaming progress and the size cap. Revisit if support requests show this is a recurring frustration. |
| R11 | **Locale fallback wrong** — the generated PDF language does not match the requesting staff user's configured language. | Medium | Low | Document the resolution order (authenticated user's `localePreference` → `Accept-Language` → backend default locale) and add an integration test for each branch. |
| R12 | **Cross-admission data leak**: query that joins on `admission_id` accidentally pulls rows from a sibling admission of the same patient. | Low | High | Every query in the renderer is parameterized by `admission_id` exclusively, never `patient_id`. Integration test creates two admissions for the same patient and asserts the export of admission A contains zero rows from admission B. |
| R13 | **Active-admission expectation gap**: exporting an `ACTIVE` admission gives a snapshot that may be mistaken for the final record. | Medium | Low | Cover page renders a localized "Snapshot generated on dd/MM/yyyy - HH:mm — admission status: ACTIVE" banner. Filename includes the timestamp so two snapshots of the same admission never collide. |

---

## Related Docs/Commits/Issues

- Related feature: [`patient-admission.md`](patient-admission.md)
- Related feature: [`medical-psychiatric-record.md`](medical-psychiatric-record.md) — § "PDF export" note at line 322
- Related feature: [`multi-document-upload.md`](multi-document-upload.md) — source of admission attachments
- Related feature: [`file-system-storage.md`](file-system-storage.md) — disk layout
- Related feature: [`nursing-module.md`](nursing-module.md), [`clinical-event-billing-automation.md`](clinical-event-billing-automation.md), [`hospital-billing-system.md`](hospital-billing-system.md) — sections of the report
- Conversation: this spec was drafted with Claude on 2026-05-11

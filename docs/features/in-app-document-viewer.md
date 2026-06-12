# Feature: In-App Document Viewer

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-06-12 | @author | Initial draft |

---

## Overview

Documents and images attached to the medical record (admission documents, medical order result
documents, patient ID documents) currently open PDFs in a **new browser tab** via `window.open`.
The customer runs the application on Chromebooks, where every extra tab is a separate renderer
process and the duplicated app context is prohibitively resource-intensive. This feature replaces
all new-tab/inconsistent document opening with a single, shared **in-app viewer dialog** that
renders PDFs (via pdf.js) and images inline, with page navigation, zoom, and a download fallback —
no navigation away from the current screen, no new tabs.

---

## Use Case / User Story

> *As a clinician using a Chromebook, I want lab-result PDFs and other attached documents to open
> in a viewer inside the application so that my device does not slow down from extra browser tabs
> and I never lose the screen I was working on.*

> *As any staff member viewing patient or admission documents, I want one consistent viewer for
> images and PDFs — with zoom and page navigation — so that I can read a document end-to-end
> without leaving the page.*

> *As a user reviewing a document in the viewer, I want a download button so that I can still save
> the original file locally when I need to.*

---

## Authorization / Role Access

**No new permissions.** The viewer is a presentation-layer change; every flow keeps its existing
server-enforced permission. The viewer only ever calls the same blob endpoints the current code
already calls.

| Action | Required Role(s) | Permission | Notes |
|--------|------------------|------------|-------|
| View admission document in viewer | per existing grants | `admission:download-documents` (file), `admission:view-documents` (list/thumbnail) | Unchanged |
| View medical order document in viewer | per existing grants | `medical-order:read` | Unchanged; PSICOLOGO stays scoped to `PRUEBAS_PSICOMETRICAS` server-side |
| View patient ID document in viewer | per existing grants | `patient:view-id` | Unchanged |
| Download from viewer | same as view | same as view | The viewer reuses the already-fetched blob |

---

## Functional Requirements

### In scope

- **FR-1 — No new tabs.** Remove every `window.open` for document viewing. PDFs render inside the
  application in a modal viewer dialog.
- **FR-2 — Shared viewer component.** One generic `FileViewerDialog` replaces the three current
  divergent implementations:
  - `web/src/components/documents/DocumentViewer.vue` (admission documents — PDFs → new tab today)
  - `web/src/components/medical-record/MedicalOrderDocumentViewer.vue` (order result documents —
    PDFs → new tab today; near-duplicate of the above)
  - the inline ID-document dialog in `PatientDetailView.vue` / `PatientFormView.vue`
    (PDF → `<iframe>` today)
- **FR-3 — PDF rendering.** PDFs render page-by-page on a `<canvas>` via `pdfjs-dist`, with:
  - page navigation (previous/next buttons + "page X of Y" indicator + keyboard ←/→),
  - zoom controls (in / out / fit-to-width / fit-to-page),
  - only the **current page** rendered at any time (Chromebook memory constraint).
- **FR-4 — Image rendering.** Images (`image/jpeg`, `image/png`) render inline with zoom
  in/out/fit controls (today they render fit-only with no zoom).
- **FR-5 — Download from viewer.** A download button in the viewer header saves the original file
  with its `displayName`/original filename (existing anchor-click pattern, reusing the fetched blob).
- **FR-6 — Unsupported types fallback.** Any other content type shows the existing
  "unsupported type" message with a download button (no rendering attempt).
- **FR-7 — Authenticated blob fetching only.** All file bytes are fetched through axios
  (`responseType: 'blob'`) so the JWT interceptor applies. No direct `src` URLs to API endpoints
  (they would 401 — all download endpoints require the `Authorization` header).
- **FR-8 — Resource hygiene.** Every object URL is revoked when the viewer closes or the document
  changes; the pdf.js document and any in-flight render task are destroyed/cancelled on close.
- **FR-9 — Lazy loading.** `pdfjs-dist` is loaded via dynamic `import()` on first PDF open so the
  main bundle (and non-PDF users) pay nothing.
- **FR-10 — Regression guard.** An ESLint `no-restricted-properties`/`no-restricted-syntax` rule
  bans `window.open` in `web/src` (with a documented escape hatch comment if a legitimate future
  use appears), matching the project's existing "ESLint blocks the wrong patterns" convention.
- **FR-11 — i18n.** All viewer chrome (page indicator, zoom, navigation, errors) uses vue-i18n
  keys in both `es.json` and `en.json` (Spanish-default app).

### Out of scope (explicit)

- **Admission PDF export** (`AdmissionExportButton.vue`) stays a direct download — it is an export
  artifact, not a stored document. (An optional "preview before download" can be a follow-up.)
- **Consent documents**: the store has `downloadConsentDocument` but **no UI flow renders it
  today**; adding a consent-view affordance is a separate feature. The new viewer is built so it
  can be reused there with a one-line fetcher when that feature lands.
- **Treasury files** (doctor fee invoices, expense invoices, bank statements): upload-only today —
  the backend has **no download endpoints** for them, so there is nothing to view. Follow-up
  feature if the customer asks.
- **Backend changes**: none. No migration, no new endpoints, no Content-Disposition changes
  (irrelevant to blob fetching), no thumbnail changes.
- Annotation, printing toolbar, OCR, multi-document gallery (prev/next across documents) — not
  requested; the thumbnail grid remains the navigation surface.

---

## Acceptance Criteria / Scenarios

- **AC-1 (admission document PDF, happy path):** From the admission detail → Documents section,
  clicking *view* on a PDF document opens the in-app viewer dialog showing page 1 rendered on
  canvas with a "page 1 / N" indicator. **No new browser tab or window is opened** (Playwright
  asserts the browser context page count stays 1).
- **AC-2 (medical order result PDF):** From a medical order card's document grid, clicking *view*
  on a PDF result opens the same in-app viewer. No new tab.
- **AC-3 (multi-page navigation):** For a multi-page PDF, next/previous buttons and ←/→ keys move
  between pages, the indicator updates, and only the current page's canvas exists in the DOM.
  Next is disabled on the last page; previous on the first.
- **AC-4 (zoom):** Zoom in/out/fit controls work for both PDFs and images; zoom state resets when
  a different document is opened.
- **AC-5 (patient ID document):** Viewing a PDF ID document from `PatientDetailView` /
  `PatientFormView` uses the same viewer component (the `<iframe>` is removed); image ID documents
  render in the same viewer with zoom.
- **AC-6 (download):** The viewer's download button saves the file with the document's
  `displayName` (or original filename for ID documents) without re-fetching the bytes.
- **AC-7 (unsupported type):** A document whose `contentType` is neither `image/*` nor
  `application/pdf` shows the unsupported-type message and a working download button.
- **AC-8 (fetch failure):** If the blob fetch fails (403/404/network), the error is surfaced via
  `useErrorHandler` and the dialog closes with no dangling object URL; the underlying page remains
  fully functional.
- **AC-9 (corrupt PDF):** If pdf.js fails to parse the blob, the viewer shows an i18n'd render
  error with a download button (the user can still retrieve the file) — it does not spin forever
  or crash the page.
- **AC-10 (resource cleanup):** Opening and closing the viewer repeatedly (≥10×) revokes every
  created object URL and destroys each pdf.js document/loading task (asserted in Vitest via spies
  on `URL.revokeObjectURL` and the mocked pdf.js `destroy`).
- **AC-11 (discharged admission):** Documents of a `DISCHARGED` admission open in the viewer
  normally — discharge protection blocks writes, never reads.
- **AC-12 (regression guard):** `window.open` no longer appears anywhere in `web/src`; the new
  ESLint rule fails the build if it is reintroduced.

---

## Non-Functional Requirements

- **Memory (Chromebook target):** never more than one full-size rendered page/canvas in memory per
  open viewer; render scale capped at `min(devicePixelRatio, 2)`; blob and object URL released on
  close. Target: viewer open/close leaves no detached canvases or unreleased blobs.
- **Bundle size:** `pdfjs-dist` (core + worker) ships as a lazily-loaded async chunk; initial app
  bundle growth ≈ 0. The pdf.js worker is registered via Vite `?url` asset import so parsing runs
  off the main thread.
- **Responsiveness:** spinner while fetching/parsing; first page of a typical lab PDF (≤ 5 MB)
  visible in < 2 s on a Chromebook-class device; render tasks cancelled when the user pages
  quickly (no queue buildup).
- **Security:** blobs only ever come from the authenticated API via axios; object URLs are
  short-lived and revoked; no document bytes are written to `localStorage`/IndexedDB; no change to
  server-side authorization.
- **Accessibility:** dialog keeps PrimeVue Dialog focus-trap/Esc-to-close; viewer controls are
  buttons with `aria-label`s; canvas has an accessible label with the document name.

---

## Date / Time Conformance

No new date/time fields or rendering are introduced; existing document metadata display is
untouched.

- [x] Backend date-only fields use `LocalDate` + `DATE`; event timestamps use `LocalDateTime` + `TIMESTAMP`. — N/A, no backend change
- [x] All frontend date/time rendering goes through `formatDate` / `formatTime` / `formatDateTime` from `@/utils/format`. — no new date rendering
- [x] All `<DatePicker>` instances rely on the global `dd/mm/yy` default. — N/A, no pickers
- [x] All `Date → API string` conversions use `toApiDate(...)`. — N/A
- [x] Relative time strings use `getRelativeTime`. — N/A

---

## API Contract

**No API changes.** The viewer consumes existing endpoints unchanged:

| Method | Endpoint | Response | Auth | Used for |
|--------|----------|----------|------|----------|
| GET | `/api/v1/admissions/{id}/documents/{docId}/file` | bytes (stored content type, `attachment`) | JWT + `admission:download-documents` | Admission document bytes |
| GET | `/api/v1/admissions/{admissionId}/medical-orders/{orderId}/documents/{docId}/file` | bytes | JWT + `medical-order:read` | Order result bytes |
| GET | `/api/v1/patients/{id}/id-document` | bytes | JWT + `patient:view-id` | ID document bytes |
| GET | `…/thumbnail` endpoints | PNG | JWT + view permission | Thumbnail grids (unchanged) |

Note: the `Content-Disposition: attachment` header on these endpoints is irrelevant to the viewer —
bytes are fetched as a blob and rendered client-side, never navigated to.

---

## Database Changes

**N/A — frontend-only feature.** No new entities, no migrations.

---

## Frontend Changes

### New dependency

| Package | Version | Notes |
|---------|---------|-------|
| `pdfjs-dist` | latest stable (pin exact minor) | Loaded via dynamic `import()`; worker registered with `new URL('pdfjs-dist/build/pdf.worker.min.mjs', import.meta.url)` / Vite `?url` import. Standard (non-legacy) build — target devices are evergreen Chrome. |

### New components / composables

| Item | Location | Description |
|-----------|----------|-------------|
| `FileViewerDialog.vue` | `src/components/viewer/` | Generic modal viewer. Props: `visible`, `fileName`, `contentType`, `fetchBlob: () => Promise<Blob>`. Emits `close`. Owns blob lifecycle (fetch on open, revoke on close), picks a pane by content type, hosts header download button + error/loading/unsupported states. |
| `PdfViewerPane.vue` | `src/components/viewer/` | Canvas renderer around `pdfjs-dist`: single-page render, page nav (buttons + ←/→), zoom (in/out/fit-width/fit-page), render-task cancellation, `destroy()` on unmount, parse-error state. |
| `ImageViewerPane.vue` | `src/components/viewer/` | `<img>` from object URL with zoom in/out/fit (extracted from today's image branch). |
| `useBlobObjectUrl.ts` | `src/composables/` | Tiny composable: blob → object URL ref with guaranteed revocation on change/unmount (replaces the four hand-rolled `createObjectURL`/`setTimeout(revoke)` sites). |

### Modified components (migration to the shared viewer)

| Component | Change |
|-----------|--------|
| `src/components/documents/DocumentViewer.vue` | Becomes a thin adapter: keeps the `useDocumentStore().viewerDocument` trigger contract, delegates rendering to `FileViewerDialog` with a fetcher closing over `(admissionId, documentId)`. **Deletes `openPdfInNewTab`.** |
| `src/components/medical-record/MedicalOrderDocumentViewer.vue` | Same adapter treatment over `useMedicalOrderDocumentStore`. **Deletes `openPdfInNewTab`.** (Optionally collapse both adapters into one once both are thin.) |
| `src/views/patients/PatientDetailView.vue` | Replace the inline ID-document dialog (`<iframe>`/`<img>`) with `FileViewerDialog` + `patientStore.downloadIdDocument` fetcher. |
| `src/views/patients/PatientFormView.vue` | Same replacement as the detail view; keep the existing download action. |
| `web/eslint` config | Add `window.open` restriction for `web/src` (FR-10). |

Unchanged: `DocumentThumbnail.vue`, `DocumentList.vue`, `MedicalOrderCard.vue` event wiring
(`view` → `setViewerDocument`), all Pinia stores' fetch functions, `AdmissionExportButton.vue`.

### Pinia stores

No new stores. Existing `document.ts`, `medicalOrderDocument.ts`, `patient.ts` blob functions are
reused as fetchers. The `viewerDocument` trigger state in the two document stores is kept as-is to
avoid touching list/card components.

### Routes

N/A — the viewer is a dialog, never a route (that is the point of the feature).

### Validation (Zod Schemas)

N/A — no forms.

### i18n keys (both `es.json` and `en.json`)

`document.viewer.pageIndicator` ("Página {current} de {total}"), `document.viewer.nextPage`,
`document.viewer.previousPage`, `document.viewer.zoomIn`, `document.viewer.zoomOut`,
`document.viewer.fitWidth`, `document.viewer.fitPage`, `document.viewer.renderError`,
plus reuse of existing `document.download`, `document.unsupportedType`, `common.close`.

---

## Implementation Notes

- **Why pdf.js canvas instead of an embedded `<iframe>`:** an iframe with a blob URL invokes
  Chrome's native PDF viewer, which spawns the same heavyweight plugin/renderer machinery the
  customer is trying to avoid, offers no app-controlled UI, and is the inconsistent pattern
  `PatientDetailView` already has. pdf.js renders one page to one canvas inside the existing tab —
  bounded, controllable memory.
- **Render loop discipline:** keep a reference to the current `RenderTask` and `cancel()` it before
  starting another (fast paging); `await loadingTask.destroy()` on close/unmount. Re-render on zoom
  change only for the current page.
- **Suggested order of work:**
  1. `useBlobObjectUrl` + `FileViewerDialog` + `ImageViewerPane` (pure refactor, no behavior change
     for images) and migrate `DocumentViewer.vue`;
  2. `PdfViewerPane` + pdfjs-dist wiring; PDFs now in-app for admission documents;
  3. migrate `MedicalOrderDocumentViewer.vue` and the two patient views;
  4. ESLint `window.open` guard, i18n sweep, tests.
- **Testing pdf.js in Vitest:** mock `pdfjs-dist` at the module boundary (`vi.mock`) with a fake
  `getDocument → { promise: { numPages, getPage, destroy } }`; assert page/zoom logic, cancellation
  and destroy calls. Real rendering is covered by Playwright with a small fixture PDF.
- **Playwright new-tab assertion:** `const pages = context.pages().length` before/after clicking
  *view* on a PDF fixture; also assert the viewer dialog and canvas are visible.
- **Verify on real lab uploads:** the 25 MB max document is the stress case; confirm first-page
  render time and memory on a low-end device profile (Chrome DevTools CPU/memory throttling).
- Existing tests touching `DocumentViewer` / `MedicalOrderDocumentViewer` (if any) must be updated,
  not deleted.

---

## QA Checklist

### Backend
- N/A — no backend changes (no entities, endpoints, or migrations).

### Frontend
- [x] All functional requirements implemented (FR-1 … FR-11)
- [x] `pdfjs-dist` added, worker wired for Vite, loaded lazily (async chunk verified in build output)
- [x] No `window.open` left in `web/src`; ESLint guard active and failing on reintroduction
- [x] Blob URL revocation + pdf.js destroy verified (Vitest spies)
- [x] Error handling implemented (fetch failure, corrupt PDF)
- [x] ESLint/oxlint passes; `vue-tsc -b` type-check passes
- [x] i18n keys added to **both** `es.json` and `en.json`
- [x] Unit tests written and passing (Vitest)

### E2E Tests (Playwright)
- [ ] PDF admission document opens in-app; page count of browser context stays 1
- [ ] PDF medical order document opens in-app; multi-page navigation works
- [ ] Image document opens with zoom controls
- [ ] Unsupported type shows fallback + download
- [ ] Download from viewer produces the file
- [ ] Discharged admission documents remain viewable

### General
- [x] Feature documentation updated (CLAUDE.md feature bullet + this spec)
- [ ] Reviewed by project owner

---

## Documentation Updates Required

### Must Update

- [ ] **[CLAUDE.md](../../CLAUDE.md)** — update the Frontend "Implemented Features" entries for
  Medical Order Document Attachments UI / admission documents to note the in-app viewer (no new
  tabs), and add this feature line.
- [ ] **[ARCHITECTURE.md](../architecture/ARCHITECTURE.md)** — note the shared `FileViewerDialog`
  pattern and the `window.open` lint ban if frontend patterns are documented there.

### Review for Consistency

- [ ] **[web/README.md](../../web/README.md)** — no setup change expected (`npm install` picks up
  `pdfjs-dist`); confirm.

### Code Documentation

- [ ] `web/src/components/viewer/PdfViewerPane.vue` — brief comment on render-task cancellation and
  destroy lifecycle (the only non-obvious constraint).

---

## Related Docs/Commits/Issues

- Related feature: `docs/features/medical-order-documents.md` (if present) — result document upload/thumbnails
- Related feature: [discharge-protection.md](discharge-protection.md) — reads are never blocked
- Customer context: application runs on Chromebooks; new-tab document opening is resource-prohibitive

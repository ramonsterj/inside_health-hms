# Feature: Laboratory Orders with Providers and a Canonical Test Catalog

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-06-03 | @author | Initial draft |
| 1.1 | 2026-06-03 | @author | Order-form review fixes: (a) prices are **not** surfaced anywhere on the clinician-facing order form, card, or by-state dashboard — pricing stays a billing/finance concern; (b) `LABORATORIOS` orders **hide** the generic start-date / end-date / *Horario* (schedule) fields — a lab requisition has no treatment window, its request date is the creation timestamp, and results arrive via document upload; (c) clarified that the source PDF defines **exactly two** providers (CLONY, HOSPITAL HERRERA LLERANDI) and all 21 canonical tests + 3 panels — V126 already seeds the complete set. |

---

## Overview

Laboratory orders (`MedicalOrder` with `category = LABORATORIOS`) become **provider-aware, multi-test requisitions** backed by a **canonical test catalog**. At admission a doctor orders the full standard kit as a single requisition against one external provider (e.g. CLONY, Hospital Herrera Llerandi); during the stay the doctor picks individual tests from a chosen provider's list. Each provider exposes its own name and pricing for the same canonical test, and the whole catalog (providers, canonical tests, per-provider tests, panels) is admin-managed so new providers and tests can be added without a deploy.

This replaces the current model where a lab order references a single `InventoryItem` and is billed from `InventoryItem.unitPrice`.

---

## Use Case / User Story

> *As a doctor, I want to order the full standard admission lab kit as a single requisition after choosing a provider, so that admission labs are sent in one request with the right provider-specific test names.*

> *As a doctor, I want to pick individual tests from a chosen provider's list during the stay, so that I can order follow-up / control labs (e.g. litemia) as needed.*

> *As a doctor, when a chosen provider doesn't offer some tests in a panel, I want to be told which tests were dropped, so that I can order them from another provider in a separate requisition.*

> *As an admin, I want to manage lab providers, canonical tests, each provider's named/priced catalog, and panels, so that new providers and tests can be added without a code deploy.*

> *As the billing system, I want a lab order's charge to equal the sum of its line items' sales prices on authorization, so that the patient is billed correctly per order while each test's cost/margin is tracked.*

---

## Authorization / Role Access

Two coarse permissions are introduced for the catalog. Ordering reuses the existing `medical-order:*` permissions.

| Action | Required Role(s) | Permission | Notes |
|--------|------------------|------------|-------|
| Create a lab order (requisition) | DOCTOR, RESIDENT_DOCTOR, ADMIN | `medical-order:create` (existing) | Provider + ≥1 line required for `LABORATORIOS` |
| Authorize / reject / emergency-authorize / mark-in-progress / discontinue | existing holders | `medical-order:authorize` / `:emergency-authorize` / `:mark-in-progress` etc. (existing) | Workflow unchanged |
| Read lab catalog (providers, canonical tests, provider-tests, panels) | DOCTOR, RESIDENT_DOCTOR, ADMIN | `lab-catalog:read` (new) | Feeds the order form. Do **not** grant by broad `medical-order:read`; that permission is intentionally held by roles that must not see/order labs. |
| Manage providers / canonical tests / provider-tests / panels (full CRUD) | ADMIN | `lab-catalog:manage` (new) | Soft delete only |

`PSYCHOLOGIST` remains scoped to `PRUEBAS_PSICOMETRICAS` and gains no lab access. `NURSE`, `CHIEF_NURSE`, `AUXILIARY_NURSE`, and `ADMINISTRATIVE_STAFF` keep their existing medical-order workflow permissions but are not granted `lab-catalog:read` unless a separate workflow change explicitly requires it. Discharge protection (`admission:update` independence) is enforced at the service layer on every mutating lab-order endpoint, exactly as today.

---

## Functional Requirements

### Catalog (admin-managed CRUD)

- **LabProvider**: name, optional code, active flag. Soft-deleted. Seed: `CLONY`, `HOSPITAL HERRERA LLERANDI` — these are the **only two** providers in the source PDF; there are no other providers to seed. Additional providers are added later through the admin catalog UI, not the seed.
- **LabTest (canonical)**: name, active flag. Soft-deleted. Seed: the union of all distinct tests in the PDF (Hematología completa, BUN, Creatinina, TGO, TGP, Sodio (Na), Potasio (K), Perfil de lípidos, T3/T4/TSH, Vitamina D, Vitamina B12, Panel de drogas en orina, Panel de drogas en sangre (Ketamina y Fentanilo), Orina simple, Glucosa al azar, Prueba de embarazo, Niveles de litio (litemia), Niveles de ácido valproico, Niveles de amonio).
- **LabProviderTest**: `(provider, lab_test, display_name, cost, sales_price, active)`. The provider-specific name and pricing for a canonical test. `UNIQUE(provider_id, lab_test_id)` among non-deleted rows. Seed each provider's offered tests from the PDF (CLONY offers the full ingreso set incl. *Panel de drogas en sangre*; Herrera Llerandi's ingreso set omits *Panel de drogas en sangre*; both offer the control set litemia / ácido valproico / amonio). The PDF identifies providers/tests/panels but does **not** contain cost or sales-price values; those values must be supplied by finance/customer data before `V126` can be finalized.
- **LabPanel**: name, active flag, with **canonical** `LabPanelItem` children. Soft-deleted. Seed three panels in canonical tests:
  - `Laboratorios de ingreso`
  - `Laboratorios de ingreso — mujeres en edad fértil` (adds *Prueba de embarazo*)
  - `Laboratorios control` (litemia, ácido valproico, amonio)

### Ordering

- A `LABORATORIOS` `MedicalOrder` carries a **single** `labProvider` and **one or more** `MedicalOrderLabTest` line items.
- Each line references a `LabProviderTest` and **snapshots** its `display_name`, `cost`, and `sales_price` at creation time, so later catalog edits never retroactively change a recorded order or its bill.
- Order creation must reject:
  - zero line items (`400`);
  - any line whose `LabProviderTest` belongs to a **different** provider than the order's provider (`400`) — one order = one provider;
  - any line referencing an **inactive / soft-deleted** `LabProviderTest` (`400`).
- Lab provider/test lines may be changed only while the order is still `SOLICITADO`. Once authorized, the order has already produced its bill and the provider/line snapshots are immutable: an update that actually changes the provider or the set of tests on a non-`SOLICITADO` lab order is rejected (`400`), but **scalar/observation edits** (observations, etc.) are still allowed on an `AUTORIZADO` / `EN_PROCESO` order and leave the snapshotted lines untouched. Admin corrections to pricing/lines after authorization must use the existing billing adjustment workflow, not silent line rewrites.
- **Category is immutable on edit.** Changing a medical order's `category` on update returns `400` — the category is the order's identity, and changing it would replace the order rather than edit it. Consequently the old "changing a lab order to another category clears its provider/lines" path no longer applies on edit; the UI also disables the category select in edit mode.
- **Terminal orders are not editable.** `NO_AUTORIZADO` / `RESULTADOS_RECIBIDOS` / `DESCONTINUADO` orders reject any update (`400`), and the medical-record UI hides the "Editar" affordance for them so the (provider/test-locked) edit dialog is never reachable for a merely-rejected order.
- **Panel expansion** (helper endpoint): given a panel + a provider, resolve each canonical panel item to that provider's active `LabProviderTest`. Return the resolved provider-tests **and** the list of `unmatchedTests` (canonical tests the provider does not offer). The client pre-fills the form with the matched lines and surfaces the unmatched ones; nothing unmatched is added or billed.
- Non-`LABORATORIOS` results-bearing categories (`REFERENCIAS_MEDICAS`, `PRUEBAS_PSICOMETRICAS`) keep the existing single-`inventoryItem` path unchanged.
- **Form shape for labs**: the `LABORATORIOS` branch of the order form does **not** show the generic start-date, end-date, or *Horario* (schedule) fields — a lab requisition is not a time-windowed treatment directive. The order's `startDate` is still persisted (set to the creation/request date) to satisfy the existing non-null column; `endDate` and `schedule` stay null. The implicit "requested" date is this creation timestamp and the "received" date is the result-document upload time (`RESULTADOS_RECIBIDOS`); no new date columns are introduced.
- **No pricing on the clinical surface**: per-test sales prices and the requisition total are **not** displayed on the clinician-facing order form, the medical-record order card, or the by-state dashboard. Prices/cost are billing/finance data — they are still snapshotted on each line and drive charge creation on authorization, but the ordering doctor sees only test names. Finance reviews/edits pricing through the admin lab catalog (`/lab/catalog`).

### Billing

- On **authorization** of a `LABORATORIOS` order, **one `PatientCharge` is created per requested test** (`ChargeType.LAB`, amount = that line's `sales_price` snapshot) so every billed test is itemized on the patient account; the charges sum to **Σ of line `sales_price` snapshots**. Cost snapshots are retained per line for margin reporting.
- The authorization billing path replaces the current single-`inventoryItem` pricing **for `LABORATORIOS` only**. New provider-catalog lab orders publish a lab-specific billing payload/event with the computed line total and per-line detail. Other results-bearing categories keep the existing `MedicalOrderAuthorizedEvent` shape with `inventoryItemId` + `unitPrice`.

### Unchanged

- Workflow states: `SOLICITADO → AUTORIZADO → EN_PROCESO → RESULTADOS_RECIBIDOS` / `NO_AUTORIZADO`, plus `DESCONTINUADO`.
- Result-document upload still transitions `AUTORIZADO`/`EN_PROCESO` → `RESULTADOS_RECIBIDOS` (results attach at the order level, not per line).
- Discharge protection on all mutating endpoints.

---

## Acceptance Criteria / Scenarios

**Happy path**

- **AC1** — Creating a `LABORATORIOS` order with a provider + ≥1 provider-test line returns `201`, status `SOLICITADO`, all lines persisted with snapshotted name/cost/sales_price, and the order's billable total = Σ line sales prices.
- **AC2** — Applying the `Laboratorios de ingreso` panel with provider `CLONY` expands to CLONY's provider-tests for every canonical test it offers, submittable as one order.
- **AC3** — Authorizing a lab order creates **one `ChargeType.LAB` `PatientCharge` per test line** (each itemized at its snapshotted sales price; the charges sum to Σ line sales prices) and transitions to `AUTORIZADO`.
- **AC4** — Result-document upload transitions `AUTORIZADO`/`EN_PROCESO` → `RESULTADOS_RECIBIDOS` (unchanged).
- **AC5** — Admin CRUD on provider / canonical test / provider-test / panel works and performs soft deletes.

**Edge cases**

- **AC6** — Applying a panel against a provider missing some canonical tests resolves the offered ones and returns the **unmatched canonical tests**; none of the missing ones are billed or added silently.
- **AC7** — Creating a lab order with a line whose provider-test belongs to a **different** provider than the order → `400`.
- **AC8** — Creating a lab order with **zero** line items → `400`.
- **AC9** — Creating or authorizing a lab order on a **DISCHARGED** admission → `400 error.admission.discharged.records`.
- **AC10** — A user without `medical-order:create` ordering labs → `403`; a non-ADMIN hitting catalog management → `403`.
- **AC11** — Referencing an **inactive / soft-deleted** `LabProviderTest` when creating an order → `400`.
- **AC12** — Deactivating or repricing a `LabProviderTest` referenced by existing orders does **not** alter those orders' recorded line prices or already-created charges (price is captured on the line at order time).
- **AC13** — A `PSYCHOLOGIST`, `NURSE`, `AUXILIARY_NURSE`, `CHIEF_NURSE`, or `ADMINISTRATIVE_STAFF` user with `medical-order:read` but without `lab-catalog:read` cannot read lab catalog endpoints (`403`).
- **AC14** — Changing the **provider or set of tests** on an already `AUTORIZADO`, `EN_PROCESO`, or `RESULTADOS_RECIBIDOS` lab order is rejected (`400`) so line snapshots cannot diverge from the previously-created charge. Scalar/observation edits (e.g. observations) on such an order are allowed and leave the snapshotted lines untouched. Terminal orders (`NO_AUTORIZADO` / `RESULTADOS_RECIBIDOS` / `DESCONTINUADO`) reject **any** update (`400`), and the UI hides "Editar" for them.
- **AC15** — There is **no legacy lab-order path**: every `LABORATORIOS` order on create must carry a `lab_provider_id` and ≥1 provider-test line, otherwise `400`. A medical order's `category` is **immutable on edit** (changing it on update returns `400`), so the old "changing a lab order to another category clears its provider/lines" behaviour no longer applies on edit and the category select is disabled in edit mode. (The single-`inventory_item` path is retained only for the other results-bearing categories, `REFERENCIAS_MEDICAS` / `PRUEBAS_PSICOMETRICAS`.)
- **AC16** — The clinical order card surfaces authorization status **unambiguously and primarily from `status`**: a pending (`SOLICITADO`) order shows an explicit "Pendiente de autorización" indicator with a neutral accent — never the green "approved" cue; an `AUTORIZADO` order shows "Autorizado por …" with a green accent; a `NO_AUTORIZADO` order shows the rejection block with a danger (red) accent. The "Autorizado por" audit line remains visible through normal post-authorization states such as `EN_PROCESO` and `RESULTADOS_RECIBIDOS`, but is suppressed for an order authorized and later `DESCONTINUADO` so it no longer presents as currently authorized. Rejecting a lab order bills nothing (cross-ref AC3).

---

## Non-Functional Requirements

- **Performance**: order creation resolves and validates all lines in a single transaction; panel expansion is one query per panel (join lab_panel_items → lab_provider_tests filtered by provider). The by-state dashboard must not regress into N+1 over line items — line counts/totals should be projected or batch-loaded.
- **Security**: all inputs validated; catalog management gated by `lab-catalog:manage`; no raw SQL. Discharge protection enforced server-side (not just UI).
- **Reliability**: billing on authorization runs in the existing event/transaction pattern (`AFTER_COMMIT` / `REQUIRES_NEW` audit as applicable). A lab order with no resolvable lines must never produce a zero/garbage charge.

---

## Date / Time Conformance

- [x] Backend date-only fields use `LocalDate` + `DATE` (`startDate`, `endDate` on the order — unchanged at the column level; the lab branch of the form simply hides them and auto-sets `startDate` to the request date); event timestamps use `LocalDateTime` + `TIMESTAMP` (`BaseEntity` audit fields). No `String`-stored dates, no `TIMESTAMPTZ`.
- [x] All frontend date/time rendering goes through `formatDate` / `formatTime` / `formatDateTime` from `@/utils/format`.
- [x] `<DatePicker>` instances rely on the global `dd/mm/yy` default; any datetime picker carries `hourFormat="24"`.
- [x] All `Date → API string` conversions use `toApiDate(...)`.
- [x] Relative time strings use `getRelativeTime`.

*(This feature adds no new date fields; the conformance items apply to the existing order start/end dates reused by the form.)*

---

## API Contract

### Lab catalog (admin / read)

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| GET | `/api/v1/lab/providers` | - | `List<LabProviderResponse>` | `lab-catalog:read` | List providers (active filter optional) |
| POST | `/api/v1/lab/providers` | `CreateLabProviderRequest` | `LabProviderResponse` | `lab-catalog:manage` | Create provider |
| PUT | `/api/v1/lab/providers/{id}` | `UpdateLabProviderRequest` | `LabProviderResponse` | `lab-catalog:manage` | Update provider |
| DELETE | `/api/v1/lab/providers/{id}` | - | - | `lab-catalog:manage` | Soft delete |
| GET | `/api/v1/lab/tests` | - | `List<LabTestResponse>` | `lab-catalog:read` | List canonical tests |
| POST | `/api/v1/lab/tests` | `CreateLabTestRequest` | `LabTestResponse` | `lab-catalog:manage` | Create canonical test |
| PUT | `/api/v1/lab/tests/{id}` | `UpdateLabTestRequest` | `LabTestResponse` | `lab-catalog:manage` | Update canonical test |
| DELETE | `/api/v1/lab/tests/{id}` | - | - | `lab-catalog:manage` | Soft delete |
| GET | `/api/v1/lab/providers/{providerId}/tests` | - | `List<LabProviderTestResponse>` | `lab-catalog:read` | A provider's named/priced catalog (feeds the order multiselect) |
| POST | `/api/v1/lab/providers/{providerId}/tests` | `CreateLabProviderTestRequest` | `LabProviderTestResponse` | `lab-catalog:manage` | Add a provider-test |
| PUT | `/api/v1/lab/provider-tests/{id}` | `UpdateLabProviderTestRequest` | `LabProviderTestResponse` | `lab-catalog:manage` | Update name/cost/price/active |
| DELETE | `/api/v1/lab/provider-tests/{id}` | - | - | `lab-catalog:manage` | Soft delete |
| GET | `/api/v1/lab/panels` | - | `List<LabPanelResponse>` | `lab-catalog:read` | List panels with canonical items |
| POST | `/api/v1/lab/panels` | `CreateLabPanelRequest` | `LabPanelResponse` | `lab-catalog:manage` | Create panel + items |
| PUT | `/api/v1/lab/panels/{id}` | `UpdateLabPanelRequest` | `LabPanelResponse` | `lab-catalog:manage` | Update panel + items |
| DELETE | `/api/v1/lab/panels/{id}` | - | - | `lab-catalog:manage` | Soft delete |
| GET | `/api/v1/lab/panels/{panelId}/resolve?providerId={id}` | - | `PanelResolutionResponse` | `lab-catalog:read` | Resolve panel against a provider → matched provider-tests + `unmatchedTests` |

### Ordering (existing endpoints, extended payload)

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| POST | `/api/v1/admissions/{admissionId}/medical-orders` | `CreateMedicalOrderRequest` (extended) | `MedicalOrderResponse` (extended) | `medical-order:create` | Lab orders carry `labProviderId` + `labProviderTestIds[]` |
| PUT | `/api/v1/admissions/{admissionId}/medical-orders/{orderId}` | `UpdateMedicalOrderRequest` | `MedicalOrderResponse` | existing | Same extension; lab provider/tests only while `SOLICITADO` |
| (authorize / reject / emergency-authorize / mark-in-progress / discontinue) | existing | existing | extended `MedicalOrderResponse` | existing | Workflow unchanged; authorize now bills Σ line prices for labs |

### Request/Response Examples

```jsonc
// POST /api/v1/lab/providers/12/tests  (lab-catalog:manage)
{ "labTestId": 3, "displayName": "Hemograma completo", "cost": 35.00, "salesPrice": 60.00, "active": true }

// GET /api/v1/lab/panels/1/resolve?providerId=12  -> PanelResolutionResponse
{
  "panelId": 1,
  "providerId": 12,
  "matched": [
    { "labProviderTestId": 101, "labTestId": 3, "displayName": "Hemograma completo", "salesPrice": 60.00 }
  ],
  "unmatchedTests": [
    { "labTestId": 14, "name": "Panel de drogas en sangre (Ketamina y Fentanilo)" }
  ]
}

// POST /api/v1/admissions/55/medical-orders  (LABORATORIOS)
{
  "category": "LABORATORIOS",
  "startDate": "2026-06-03",
  "labProviderId": 12,
  "labProviderTestIds": [101, 102, 103],
  "observations": "Laboratorios de ingreso"
}

// MedicalOrderResponse (extended, lab fields)
{
  "id": 900, "category": "LABORATORIOS", "status": "SOLICITADO",
  "labProvider": { "id": 12, "name": "CLONY" },
  "labTests": [
    { "id": 5001, "labProviderTestId": 101, "displayName": "Hemograma completo", "salesPrice": 60.00, "cost": 35.00 }
  ],
  "labTotal": 180.00,
  "inventoryItemId": null, "inventoryItemName": null,
  "documentCount": 0
}
```

---

## Database Changes

### New Entities

| Entity | Table | Extends | Description |
|--------|-------|---------|-------------|
| `LabProvider` | `lab_providers` | `BaseEntity` | External lab provider (CLONY, Herrera Llerandi) |
| `LabTest` | `lab_tests` | `BaseEntity` | Canonical test concept |
| `LabProviderTest` | `lab_provider_tests` | `BaseEntity` | Per-provider name + cost + sales price for a canonical test |
| `LabPanel` | `lab_panels` | `BaseEntity` | Named preset defined in canonical tests |
| `LabPanelItem` | `lab_panel_items` | `BaseEntity` | Canonical test membership in a panel |
| `MedicalOrderLabTest` | `medical_order_lab_tests` | `BaseEntity` | Line item on a lab order; snapshots name/cost/sales_price |

`MedicalOrder` gains nullable `lab_provider_id` (FK, meaningful only for `LABORATORIOS`).

### New Migrations (next free version is V123)

| Migration | Description |
|-----------|-------------|
| `V123__create_lab_catalog.sql` | `lab_providers`, `lab_tests`, `lab_provider_tests`, `lab_panels`, `lab_panel_items` + FK/`deleted_at` indexes + partial unique indexes |
| `V124__add_lab_order_lines.sql` | Adds `medical_orders.lab_provider_id` (FK + index) and creates `medical_order_lab_tests` |
| `V125__seed_lab_catalog_permissions.sql` | Inserts `lab-catalog:read`, `lab-catalog:manage`; grants read to ADMIN/DOCTOR/RESIDENT_DOCTOR only, manage to ADMIN |
| `V126__seed_lab_catalog_data.sql` | Seeds the two providers, canonical tests, each provider's offered tests, and the three panels from the LABORATORIOS PDF, plus provider-specific cost/sales-price values supplied separately by finance/customer data (prod reference data, mirrors the V111 workbook-load approach) |

> **Seed placement note:** base catalog data is production reference data, so it lives in a versioned `db/migration` SQL file (like V111's workbook load), not in `db/seed`. Per the MIGRATIONS guide, confirm whether the SEED-BUNDLE-VERSION rule applies if any of this is also surfaced through dev seeds.

### Schema Example

```sql
CREATE TABLE lab_providers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    code VARCHAR(50),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT, updated_by BIGINT, deleted_at TIMESTAMP
);
CREATE INDEX idx_lab_providers_deleted_at ON lab_providers(deleted_at);
CREATE UNIQUE INDEX uq_lab_providers_name_active
  ON lab_providers(LOWER(name)) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_lab_providers_code_active
  ON lab_providers(LOWER(code)) WHERE code IS NOT NULL AND deleted_at IS NULL;

CREATE TABLE lab_tests (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT, updated_by BIGINT, deleted_at TIMESTAMP
);
CREATE INDEX idx_lab_tests_deleted_at ON lab_tests(deleted_at);
CREATE UNIQUE INDEX uq_lab_tests_name_active
  ON lab_tests(LOWER(name)) WHERE deleted_at IS NULL;

CREATE TABLE lab_provider_tests (
    id BIGSERIAL PRIMARY KEY,
    provider_id BIGINT NOT NULL REFERENCES lab_providers(id),
    lab_test_id BIGINT NOT NULL REFERENCES lab_tests(id),
    display_name VARCHAR(200) NOT NULL,
    cost NUMERIC(12,2) NOT NULL CHECK (cost >= 0),
    sales_price NUMERIC(12,2) NOT NULL CHECK (sales_price > 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT, updated_by BIGINT, deleted_at TIMESTAMP
);
CREATE INDEX idx_lab_provider_tests_deleted_at ON lab_provider_tests(deleted_at);
CREATE INDEX idx_lab_provider_tests_provider ON lab_provider_tests(provider_id);
CREATE INDEX idx_lab_provider_tests_test ON lab_provider_tests(lab_test_id);
-- one non-deleted offering per (provider, canonical test)
CREATE UNIQUE INDEX uq_lab_provider_test_active
  ON lab_provider_tests(provider_id, lab_test_id) WHERE deleted_at IS NULL;

CREATE TABLE lab_panels (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT, updated_by BIGINT, deleted_at TIMESTAMP
);
CREATE INDEX idx_lab_panels_deleted_at ON lab_panels(deleted_at);
CREATE UNIQUE INDEX uq_lab_panels_name_active
  ON lab_panels(LOWER(name)) WHERE deleted_at IS NULL;

CREATE TABLE lab_panel_items (
    id BIGSERIAL PRIMARY KEY,
    panel_id BIGINT NOT NULL REFERENCES lab_panels(id),
    lab_test_id BIGINT NOT NULL REFERENCES lab_tests(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT, updated_by BIGINT, deleted_at TIMESTAMP
);
CREATE INDEX idx_lab_panel_items_deleted_at ON lab_panel_items(deleted_at);
CREATE INDEX idx_lab_panel_items_panel ON lab_panel_items(panel_id);
CREATE INDEX idx_lab_panel_items_test ON lab_panel_items(lab_test_id);
CREATE UNIQUE INDEX uq_lab_panel_items_active
  ON lab_panel_items(panel_id, lab_test_id) WHERE deleted_at IS NULL;

-- V124
ALTER TABLE medical_orders ADD COLUMN lab_provider_id BIGINT REFERENCES lab_providers(id);
CREATE INDEX idx_medical_orders_lab_provider ON medical_orders(lab_provider_id);

CREATE TABLE medical_order_lab_tests (
    id BIGSERIAL PRIMARY KEY,
    medical_order_id BIGINT NOT NULL REFERENCES medical_orders(id),
    lab_provider_test_id BIGINT NOT NULL REFERENCES lab_provider_tests(id),
    lab_test_id BIGINT NOT NULL REFERENCES lab_tests(id),
    display_name VARCHAR(200) NOT NULL,     -- snapshot
    cost NUMERIC(12,2) NOT NULL CHECK (cost >= 0),           -- snapshot
    sales_price NUMERIC(12,2) NOT NULL CHECK (sales_price > 0), -- snapshot
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT, updated_by BIGINT, deleted_at TIMESTAMP
);
CREATE INDEX idx_mo_lab_tests_deleted_at ON medical_order_lab_tests(deleted_at);
CREATE INDEX idx_mo_lab_tests_order ON medical_order_lab_tests(medical_order_id);
CREATE INDEX idx_mo_lab_tests_provider_test ON medical_order_lab_tests(lab_provider_test_id);
CREATE INDEX idx_mo_lab_tests_test ON medical_order_lab_tests(lab_test_id);
```

### Index Requirements

- [x] `deleted_at` on every new table
- [x] FK indexes (`provider_id`, `lab_test_id`, `panel_id`, `medical_order_id`, `lab_provider_id`, `lab_provider_test_id`)
- [x] Partial unique index on `(provider_id, lab_test_id)` for non-deleted provider-tests
- [x] Partial unique indexes on provider/test/panel names and panel membership
- [x] CHECK constraints for non-negative costs and positive sales prices

---

## Frontend Changes

### Components

| Component | Location | Description |
|-----------|----------|-------------|
| `MedicalOrderFormDialog.vue` | `web/src/components/medical-record/` | **Modify**: for `LABORATORIOS`, replace the single inventory picker with **provider `<Select>` → that provider's test `<MultiSelect>`** (option label = test `displayName` only, **no price**), plus an "Apply panel" action (panel select → resolve → pre-fill lines; show unmatched-tests notice). The lab branch **hides** the start-date / end-date / *Horario* fields and shows **no** per-test price or running total. |
| `LabProviderList.vue` | `web/src/views/lab/` | Admin CRUD for providers |
| `LabTestList.vue` | `web/src/views/lab/` | Admin CRUD for canonical tests |
| `LabProviderTestPanel.vue` | `web/src/views/lab/` | Per-provider catalog CRUD (name/cost/sales price/active) |
| `LabPanelList.vue` + `LabPanelFormDialog.vue` | `web/src/views/lab/` | Panel CRUD (compose from canonical tests) |
| `MedicalOrderDetail` / by-state dashboard rows | `web/src/views/medical-orders/`, `web/src/components/medical-record/` | **Modify**: render provider + list of requested tests for lab orders (no prices / total on these clinician-facing views) |

### Pinia Stores

| Store | Location | Description |
|-------|----------|-------------|
| `useLabCatalogStore` | `web/src/stores/labCatalog.ts` | Providers, canonical tests, provider-tests, panels, panel resolution |
| `useMedicalOrderStore` | `web/src/stores/medicalOrder.ts` | **Modify**: create/update payloads carry `labProviderId` + `labProviderTestIds`; responses carry `labProvider`, `labTests`, `labTotal` |

### Routes

| Path | Component | Auth Required | Roles |
|------|-----------|---------------|-------|
| `/lab/catalog` (tabbed: providers / tests / panels) | lab admin views | Yes | `lab-catalog:manage` (read for view) |

Side-nav "Laboratorios" / catalog section gated by `lab-catalog:read`; management affordances gated by `lab-catalog:manage`.

### Validation (Zod Schemas)

```typescript
// web/src/schemas/lab.ts
export const createLabProviderTestSchema = z.object({
  labTestId: z.number().int().positive(),
  displayName: z.string().min(1).max(200),
  cost: z.number().nonnegative(),
  salesPrice: z.number().positive(),
  active: z.boolean().default(true),
})

// medical order (lab branch) — provider + at least one test required when category === LABORATORIOS
export const labOrderSchema = z.object({
  labProviderId: z.number().int().positive(),
  labProviderTestIds: z.array(z.number().int().positive()).min(1),
})
```

### Types

`web/src/types/medicalRecord.ts` (or a new `web/src/types/lab.ts`): `LabProvider`, `LabTest`, `LabProviderTest`, `LabPanel`, `PanelResolution`, and extended `MedicalOrderResponse` (`labProvider`, `labTests`, `labTotal`). `RESULTS_BEARING_CATEGORIES` unchanged.

---

## Implementation Notes

- **Service layer**: extract a `LabCatalogService` (provider/test/provider-test/panel CRUD + `resolvePanel(panelId, providerId)`). `MedicalOrderService.createMedicalOrder` branches on `category == LABORATORIOS`: resolve provider, validate every `labProviderTestId` belongs to that provider and is active, snapshot name/cost/sales_price into `MedicalOrderLabTest` rows. Reuse the existing `validateAdmissionActive` discharge-protection pattern on all mutating paths.
- **Billing rebuild**: `authorize()` / `emergencyAuthorize()` currently publish `MedicalOrderAuthorizedEvent(admissionId, category, inventoryItemId, itemName, unitPrice)`. Keep that event shape for `REFERENCIAS_MEDICAS` / `PRUEBAS_PSICOMETRICAS`, and add a distinct lab billing payload/event for `LABORATORIOS` carrying `admissionId`, `medicalOrderId`, provider snapshot/name, line snapshots, and the summed line total. The billing listener creates one `PatientCharge` of type `LAB`, quantity `1`, unit/total price equal to the line total, and `inventoryItem = null` for new provider-catalog lab charges. The charge description should include the provider and requested test names from snapshots.
- **`MedicalOrderListItemResponse`**: add lab total / provider name; project line aggregates to avoid N+1 on the cross-admission dashboard.
- **Legacy lab `InventoryItem(kind=SERVICE)` rows & old billing path**: leave historical lab orders untouched (no `lab_provider_id`, no lines). They keep their already-created charges and remain readable through the existing `inventoryItemId` / `inventoryItemName` fields. Only *new* `LABORATORIOS` orders use the provider-line model. Do not backfill existing lab orders because the old rows lack provider identity and line-level tests. Retiring/relabeling legacy lab `InventoryItem(kind=SERVICE)` rows is a separate cleanup migration after confirming inventory/billing reports no longer depend on them.
- **Panel/provider mismatch UX**: `resolvePanel` returns `unmatchedTests`; the form shows a non-blocking notice ("CLONY does not offer: Panel de drogas en sangre — order separately from another provider") and proceeds with matched lines.
- **Seed-data prerequisite**: before implementing `V126`, obtain the provider-specific cost and sales-price table for every seeded `LabProviderTest`. The attached `LABORATORIOS.pdf` is enough for providers/tests/panels, but not for pricing.

---

## QA Checklist

### Backend
- [ ] All functional requirements implemented
- [ ] New entities extend `BaseEntity` and carry `@SQLRestriction("deleted_at IS NULL")`
- [ ] DTOs used in controllers (no entity exposure); `@Enumerated(STRING)` where applicable
- [ ] Input validation (provider/line consistency, ≥1 line, active provider-test, snapshotting)
- [ ] Lab provider/test updates rejected after authorization to prevent charge divergence
- [ ] Lab catalog read denied to `medical-order:read` holders without explicit `lab-catalog:read`
- [ ] Unit tests written and passing
- [ ] Integration tests (Testcontainers) for catalog CRUD, order create, panel resolution, billing-on-authorize
- [ ] Detekt passes (no new violations)
- [ ] OWASP dependency-check passes

### Frontend
- [ ] Lab order form provider→tests→apply-panel flow + unmatched notice
- [ ] Catalog admin CRUD components + `useLabCatalogStore`
- [ ] Routes guarded by `lab-catalog:read` / `:manage`
- [ ] VeeValidate + Zod validation (provider required, ≥1 test)
- [ ] Error handling (403/400) surfaced
- [ ] ESLint/oxlint passes
- [ ] i18n keys for all new text (provider, tests, panels, unmatched notice)
- [ ] Unit tests (Vitest) for store + form

### E2E (Playwright)
- [ ] Order admission kit as one requisition → authorize → single charge = Σ prices
- [ ] Apply panel against provider missing a test → unmatched notice; matched lines submitted
- [ ] Catalog management allowed for ADMIN, denied for non-admin
- [ ] Lab catalog read allowed for DOCTOR/RESIDENT_DOCTOR/ADMIN and denied for PSYCHOLOGIST/NURSE/AUXILIARY_NURSE/CHIEF_NURSE/ADMINISTRATIVE_STAFF unless separately granted
- [ ] Discharged admission blocks lab order create/authorize

### General
- [ ] API contract documented
- [ ] Migrations V123–V126 tested (schema validates; seed loads)
- [ ] Feature documentation updated
- [ ] Reviewed by project owner

---

## Documentation Updates Required

### Must Update
- [ ] **[CLAUDE.md](../../CLAUDE.md)** — add Laboratory Orders with Providers to Implemented Features (backend + frontend); record migrations V123–V126 in the migration list; note the two new permissions.
- [ ] **[ARCHITECTURE.md](../architecture/ARCHITECTURE.md)** — document the lab catalog + line-item billing path if it diverges from the existing single-inventoryItem order billing.
- [ ] **[docs/architecture/MIGRATIONS.md](../architecture/MIGRATIONS.md)** — confirm seed placement / SEED-BUNDLE-VERSION handling for the catalog reference data.

### Review for Consistency
- [ ] **[discharge-protection.md](discharge-protection.md)** — add lab-order line/provider mutations to the protected surface.
- [ ] **[clinical-event-billing-automation.md](clinical-event-billing-automation.md)** — update the medical-order authorization billing description for the lab line-total path.
- [ ] **[medical-psychiatric-record.md](medical-psychiatric-record.md)** — note the new lab order shape.

### Code Documentation
- [ ] KDoc on `LabCatalogService.resolvePanel` (matched/unmatched semantics) and the billing branch in `MedicalOrderService`.

---

## Related Docs/Commits/Issues

- Related feature: `docs/features/medical-psychiatric-record.md` (medical order workflow), `docs/features/clinical-event-billing-automation.md` (authorization billing), `docs/features/discharge-protection.md`.
- Source document: `LABORATORIOS.pdf` (providers CLONY / Hospital Herrera Llerandi; ingreso, mujeres-en-edad-fértil, and control panels).
- GitHub Issue: `#XXX - add link here`
- Design discussion: this Claude session.

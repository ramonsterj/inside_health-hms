# Feature: Pharmacy & Inventory Evolution (Composition + Lots)

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-05-13 | Juan Ramón Paniagua | Initial draft — composition redesign of `InventoryItem`, `MedicationDetails` satellite, `InventoryLot` aggregate, FEFO dispensing, expiry dashboard, customer-file SKU preservation |
| 1.1 | 2026-05-13 | Codex | Tightened invariants: explicit lot-tracking flag, SKU-only import matching, legacy-lot replacement rules, mandatory medication-details backfill, corrected medication-administration FK model, and removed `@MapsId`/`BaseEntity` ambiguity |
| 1.2 | 2026-05-13 | Juan Ramón Paniagua | Closed implementation-ready gaps: removed factually-wrong `doctor_fees.inventory_item_id` references, defined billing semantics for `quantity > 1` administrations, added `ON CONFLICT` upsert rule for ENTRY movements, defined soft-delete cascade behavior, added AC for the `NEEDS_REVIEW → CONFIRMED` workflow, added a second partial index for the SUM recompute, simplified FR-6 step 3, and recorded Q-LOTREQ as resolved |
| 1.3 | 2026-05-14 | Juan Ramón Paniagua | Removed the category selector from medication create/edit: the drug inventory category is now resolved server-side via a new `inventory_categories.default_for_kind` column (V113). Hides a UX footgun that let pharmacists file drugs under unrelated buckets like "Ingredientes de Cocina." Adds service-layer guards preventing deletion or deactivation of a kind-default category. |
| 1.4 | 2026-05-14 | Juan Ramón Paniagua | Closes the symmetric footgun on the **general inventory form**: users could previously pick the "Medicamentos" category from the supply/equipment form, producing a `kind=SUPPLY` item filed under the drug category. Frontend now filters `default_for_kind`-flagged categories out of the create/edit dropdown, and `InventoryItemService` rejects writes that target a kind-routed category with a mismatching `kind`. |

---

## Relationship to existing `inventory-module.md`

This spec is the **successor** to [`inventory-module.md`](inventory-module.md) (v1.1) for the **medication and lot scope only**. The v1 spec remains authoritative for the **Billing Catalog base** — `InventoryItem` identity, pricing (`FLAT` / `TIME_BASED`), categories, restock level, movement history, room pricing.

What this spec adds on top of v1:

1. A **`kind` discriminator**, a **`sku`** column, and a **`lotTrackingEnabled`** flag on `InventoryItem`, so the same row can route to kind-specific satellites, carry the customer's printed SKU (`A12`, `C123`, …), and declare whether stock is lot-managed.
2. A **Pharmacy bounded context** decorating `InventoryItem` via composition: `MedicationDetails` (1:1) + `InventoryLot` (1:N).
3. **FEFO dispensing** on `MedicationAdministration` so debits target the soonest-to-expire non-empty lot.
4. An **expiry dashboard** with green / yellow / red windows that mirrors the customer's color-coded spreadsheet.
5. A **one-shot Flyway migration** (V111) that absorbs the customer's 20-page workbook from a checked-in CSV. No runtime bulk-import endpoint or UI; the catalog is then maintained through the regular inventory/pharmacy screens.

What this spec **does not** rewrite:

- `InventoryItem.name`, `description`, `price`, `cost`, `restockLevel`, `pricingType`, `timeUnit`, `timeInterval`, `active` — unchanged.
- The category model from v1 — unchanged (no breaking renames). Medication classification is layered on top via `MedicationDetails.section`, not by splitting the categories table.
- Existing FKs to `inventory_items.id` (`medical_orders.inventory_item_id`, `patient_charges.inventory_item_id`) — **must stay intact**. `medication_administrations` does not currently point directly to `inventory_items`; it reaches the item through `medical_orders.inventory_item_id`, and this remains unchanged. `doctor_fees` has no direct FK to `inventory_items` either — it reaches inventory transitively through `patient_charge_id` — so it is unaffected by this spec.

The v1 spec gets a one-line entry in its Revision History pointing here.

---

## Overview

Move the inventory module from a flat single-table catalog to a **composition model** that distinguishes between kinds of inventoried things (drugs, supplies, equipment, services, personnel, food) and that gives medications the lot/expiration tracking the customer's operational reality demands. The `InventoryItem` aggregate stays as the **Billing Catalog** identity and price book; **satellite aggregates** (`MedicationDetails`, `InventoryLot`, eventually `EquipmentUnit`) carry kind-specific concerns. Medication administration becomes **FEFO** (First-Expired-First-Out), and pharmacists gain an expiry dashboard and lot recall. The customer's 20-page workbook is loaded once at deploy time by a one-shot Flyway migration (V111) from a checked-in CSV — there is no permanent bulk-import surface.

---

## Use Case / User Story

1. **As a pharmacist (ADMIN)**, I want to register a medication with its generic name, commercial name, strength, dosage form, and route so that physicians and nurses see the structured drug identity instead of a single concatenated string.
2. **As a pharmacist**, I want to register a new lot for a medication with its lot number, expiration date, and received quantity so that the same SKU can carry several lots with different expirations at the same time ("varias fechas" rows in the customer file).
3. **As a pharmacist**, I want an expiry dashboard that shows each medication's lots colored green (>90 days), yellow (≤90 days), or red (≤30 days or expired) so that I can plan reorders the way the customer's printed spreadsheet does today.
4. **As a nurse (NURSE / CHIEF_NURSE)**, I want medication administration to automatically debit from the soonest-to-expire non-empty lot (FEFO) so that I don't have to pick a lot manually and so that older stock leaves the pharmacy first.
5. **As a pharmacist**, I want to recall a lot (flag it as not-dispensable) so that an affected batch is removed from FEFO selection without losing its history.
6. **As a pharmacist**, I want to mark a medication as **controlled** so that it can later be subject to stricter dispensing rules (e.g. dual signature — out of scope here but the flag is captured).
7. **As an operator**, I want the customer's workbook (~615 rows across sections A/B/C/D/E) loaded once at deploy time so the catalog ships pre-populated; ongoing maintenance happens through the existing inventory and pharmacy UIs. (Implemented as a one-shot Flyway Java migration — see FR-8.)
8. **As an administrator**, I want existing items (those seeded in V052) to keep working — their charges, their orders, their administrations — even after the schema gains the new `kind` column and the satellites.

---

## Authorization / Role Access

### New permissions (proposed)

| Code | Description | Resource | Action |
|------|-------------|----------|--------|
| `medication:read` | View `MedicationDetails` and structured drug fields | `medication` | `read` |
| `medication:create` | Create a new `MedicationDetails` row (attach drug attributes to an item) | `medication` | `create` |
| `medication:update` | Edit drug attributes (generic / brand / strength / form / route / controlled flag / section) | `medication` | `update` |
| `inventory-lot:read` | List and view lots for an item | `inventory-lot` | `read` |
| `inventory-lot:create` | Register a new lot (entry) | `inventory-lot` | `create` |
| `inventory-lot:update` | Edit lot metadata (lot number, expiration, supplier, notes), recall a lot | `inventory-lot` | `update` |
| `medication:expiry-report` | Read the expiry dashboard | `medication` | `expiry-report` |

FEFO dispensing is not a separate user action. It runs inside the existing medication-administration flow and is gated by `medication-administration:create`. Manual lot override additionally requires `inventory-lot:update`.

### Role grants (proposed; see Risks R-PERM)

| Role | Permissions granted |
|------|---------------------|
| `ADMIN` | All of the above |
| `ADMINISTRATIVE_STAFF` | `medication:read`, `inventory-lot:read`, `medication:expiry-report` |
| `CHIEF_NURSE` | `medication:read`, `inventory-lot:read`, `medication:expiry-report` |
| `NURSE` | `medication:read`, `inventory-lot:read` |
| `DOCTOR` | `medication:read`, `inventory-lot:read` |
| `PSYCHOLOGIST` | (none) |
| `TREASURY` | (none — billing already references items through existing permissions) |

This matrix is **proposed**, not final. It must be reviewed against [`roles-functionality-matrix.md`](../roles-functionality-matrix.md) before implementation. See § Open Questions Q-PERM.

---

## Functional Requirements

### FR-1. `InventoryItem` gains `kind`, `sku`, and `lotTrackingEnabled`

- New column `inventory_items.kind VARCHAR(20) NOT NULL` backed by Kotlin enum `InventoryKind`:
  - `DRUG` — medications (oral, injectable, syrup, drops). Always lot-tracked after backfill.
  - `SUPPLY` — medical consumables (syringes, gasas, sondas, mascarillas). Lot-tracked only when `lotTrackingEnabled = TRUE`.
  - `EQUIPMENT` — billable devices (cardiac monitor, infusion pump, oxygen). Not lot-tracked. **`EquipmentUnit` satellite is reserved for a future spec — see § Out of Scope.**
  - `SERVICE` — intangible billable services. No stock, no movements.
  - `PERSONNEL` — billable personnel rates. No stock, no movements.
  - `FOOD` — kitchen ingredients and meals. Lot-tracked only when `lotTrackingEnabled = TRUE` (existing daily-diet billing keeps working).
- New column `inventory_items.sku VARCHAR(20) NULL`, `UNIQUE` when not null. Holds the customer's printed SKU (`A1`, `B17`, `C123`, `D047`, `E160`) so paper rounds and the system stay in sync. Auto-generation of SKUs is **not** in scope; the field is admin-managed.
- New column `inventory_items.lot_tracking_enabled BOOLEAN NOT NULL DEFAULT FALSE`.
  - Must be `TRUE` for `kind=DRUG`.
  - May be `TRUE` for `kind ∈ {SUPPLY, FOOD}`.
  - Must be `FALSE` for `kind ∈ {EQUIPMENT, SERVICE, PERSONNEL}`.
  - The service layer rejects changes that would disable lot tracking while active non-deleted lots exist.
- `kind` is **mutable** by ADMIN. Reclassification preserves the item id and therefore every existing FK (charges, orders, administrations, doctor fees). Service-layer guards enforce satellite consistency on reclassification (see FR-3 and Risks R-RECLASSIFY).
- **Soft-delete cascade.** Soft-deleting an `InventoryItem` (setting `deleted_at`) cascades to its `MedicationDetails` row (1:1 invariant — never orphan a row that points to a hidden parent). It does **not** cascade to `InventoryLot` rows: lots are preserved so historical `inventory_movements` and `medication_administrations` keep resolving, and the parent's `deleted_at` is enough to hide the lots from Pharmacy queries. Restoring an item (clearing `deleted_at`) restores its `MedicationDetails` row in the same transaction.
- **Resolved Q-LOTREQ.** Lots are mandatory for every `kind=DRUG` item by invariant: `lot_tracking_enabled` is `TRUE` for all DRUGs, and V106 seeds a synthetic legacy lot for every DRUG with `quantity > 0` so FEFO always has a lot to debit. There is no "DRUG without lots" state in v1.

### FR-2. `MedicationDetails` (1:1 satellite, kind = DRUG)

- New table `medication_details` with its own `BaseEntity.id` and a required unique FK `item_id` to `inventory_items(id)`.
  - The relationship is logically 1:1 and identity-preserving from the domain perspective, but it does **not** use `@MapsId` because the project-wide `BaseEntity.id` is `@GeneratedValue`.
  - DB invariant: `UNIQUE(item_id)`.
- Fields:
  - `genericName VARCHAR(150) NOT NULL` — "Nombre genérico" in the customer file.
  - `commercialName VARCHAR(150) NULL` — "Nombre comercial".
  - `strength VARCHAR(50) NULL` — "Miligramaje" as raw display string (preserves customer formats like `15MG/3ML`, `100 MG/300 MG`, `30 mg/2.5 mL`, `1MG/ML`).
  - `dosageForm VARCHAR(30) NOT NULL` enum `DosageForm`: `TABLET`, `CAPSULE`, `AMPOULE`, `SYRUP`, `DROPS`, `CREAM`, `INJECTION`, `POWDER`, `PATCH`, `OTHER`.
  - `route VARCHAR(20) NULL` enum `AdministrationRoute`: `PO`, `IM`, `IV`, `SC`, `TOPICAL`, `INHALED`, `OTHER`.
  - `controlled BOOLEAN NOT NULL DEFAULT FALSE` — controlled-substance flag (placeholder for future dual-signature rules).
  - `atcCode VARCHAR(10) NULL` — optional ATC classification, future-friendly.
  - `section VARCHAR(30) NOT NULL` enum `MedicationSection`: `PSIQUIATRICO`, `NO_PSIQUIATRICO`, `JARABE_GOTAS`, `AMPOLLA`. Mirrors the customer's A/B/C/D split. (E = "Insumos" is not a medication section — those rows live as `kind=SUPPLY` without a `MedicationDetails` row.)
  - `reviewStatus VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED'` enum `MedicationReviewStatus`: `CONFIRMED`, `NEEDS_REVIEW`. Used by the V105 backfill for rows parsed from legacy free text.
  - `reviewNotes VARCHAR(500) NULL` — optional explanation for `NEEDS_REVIEW` rows.
- An `InventoryItem` with `kind=DRUG` **must** have a `MedicationDetails` row. Service layer enforces this on create/reclassify; a DB CHECK constraint or partial-unique trigger is **not** introduced in v1 (see Risks R-INVARIANT).
- Backfill must create a `MedicationDetails` row for **every** existing `kind=DRUG` item. Low-confidence parses use best-effort values and `reviewStatus=NEEDS_REVIEW`; they are never left without a details row.
- An item with any other `kind` **must not** have a `MedicationDetails` row.

### FR-3. `InventoryLot` (1:N satellite, `lotTrackingEnabled = TRUE`)

- New table `inventory_lots`:

```sql
CREATE TABLE inventory_lots (
    id BIGSERIAL PRIMARY KEY,
    item_id BIGINT NOT NULL REFERENCES inventory_items(id),
    lot_number VARCHAR(50) NULL,
    expiration_date DATE NOT NULL,
    quantity_on_hand INT NOT NULL CHECK (quantity_on_hand >= 0),
    received_at DATE NOT NULL,
    supplier VARCHAR(150) NULL,
    notes VARCHAR(500) NULL,
    recalled BOOLEAN NOT NULL DEFAULT FALSE,
    recalled_reason VARCHAR(500) NULL,
    synthetic_legacy BOOLEAN NOT NULL DEFAULT FALSE,
    -- BaseEntity
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_inventory_lots_item_id ON inventory_lots(item_id);
CREATE INDEX idx_inventory_lots_expiration_date ON inventory_lots(expiration_date);
CREATE INDEX idx_inventory_lots_deleted_at ON inventory_lots(deleted_at);
CREATE INDEX idx_inventory_lots_item_fefo
    ON inventory_lots(item_id, expiration_date)
    WHERE deleted_at IS NULL AND recalled = FALSE AND quantity_on_hand > 0;
CREATE UNIQUE INDEX ux_inventory_lots_item_lot_expiration_active
    ON inventory_lots(item_id, lot_number, expiration_date) NULLS NOT DISTINCT
    WHERE deleted_at IS NULL;
CREATE INDEX idx_inventory_lots_item_active
    ON inventory_lots(item_id)
    WHERE deleted_at IS NULL AND recalled = FALSE;
```

- `lot_number` is nullable because the customer's spreadsheet does not always record one. Identity within an item is `(item_id, lot_number, expiration_date)`, enforced by a PostgreSQL 17 `UNIQUE ... NULLS NOT DISTINCT` partial index over active rows so duplicate imports cannot create duplicate lots even when `lot_number` is NULL.
- `expiration_date` is mandatory. A sentinel value (e.g. `9999-12-31`) is reserved for the synthetic "legacy" lot created during backfill (see § Migration / Backfill), and those rows carry `synthetic_legacy=TRUE`.
- `recalled = TRUE` removes the lot from FEFO selection and from "available stock" rollups but **does not** delete the lot or its history. Soft delete (`deleted_at`) is only for genuine data-entry mistakes that have no movements.
- Lots **may not be soft-deleted** if any `inventory_movements` row references them. Service-layer guard.
- Lots may only be created for items where `lotTrackingEnabled = TRUE`.

### FR-4. Derived `quantity` for lot-tracked items

- For items where `lotTrackingEnabled = TRUE`, `inventory_items.quantity` is computed as `SUM(inventory_lots.quantity_on_hand WHERE deleted_at IS NULL AND recalled = FALSE)`.
- v1 keeps `quantity` as a **stored** column on `inventory_items` and **recomputes it in the service layer** after every movement that touches a lot. Reasons:
  - Existing callers (`BillingService`, low-stock reports, `InventoryListView`) already read `quantity` directly; switching to a derived view would touch every read path.
  - Stored value + recompute keeps the existing column index strategy intact.
- A reconciliation job (manual endpoint `POST /api/v1/inventory/items/{id}/reconcile-quantity` gated by ADMIN) lets ops recompute on demand if drift is suspected. Drift detection is logged but not auto-healed (see Risks R-DRIFT).

### FR-5. `inventory_movements` gains `lot_id`

- New column `inventory_movements.lot_id BIGINT NULL REFERENCES inventory_lots(id)`.
- Required (service-layer check) when `item.lotTrackingEnabled = TRUE`.
- Nullable for scalar-stock items (`lotTrackingEnabled = FALSE`).
- For `EXIT` movements on lot-tracked items, the service layer:
  1. Resolves the target lot via FEFO (or accepts an explicit `lotId` when admin overrides).
  2. Acquires a row-level lock on the lot (`SELECT … FOR UPDATE`).
  3. Decrements `lot.quantity_on_hand`; rejects if it would go negative.
  4. Recomputes `item.quantity`.
- For `ENTRY` movements, the service layer either creates a new lot (preferred path) or credits an existing lot identified by `(item_id, lot_number, expiration_date)`. The upsert is implemented as `INSERT … ON CONFLICT (item_id, lot_number, expiration_date) WHERE deleted_at IS NULL DO UPDATE SET quantity_on_hand = inventory_lots.quantity_on_hand + EXCLUDED.quantity_on_hand` so concurrent ENTRY movements for the same `(item, lot_number, expiration)` cannot violate the partial unique index. The conflict target relies on `NULLS NOT DISTINCT` so NULL `lot_number` collapses correctly.

### FR-6. FEFO dispensing on medication administration

- `MedicationAdministrationService.createAdministration(...)` gains a lot-selection step before debiting stock:
  1. Load the parent `MedicalOrder.inventoryItem`.
  2. If `item.kind == DRUG` and `item.lotTrackingEnabled == TRUE`:
     a. Find the soonest-to-expire `InventoryLot` with `deleted_at IS NULL AND recalled = FALSE AND quantity_on_hand >= requestedQuantity`.
     b. If found, lock and debit that lot. Record the `lot_id` on the resulting `inventory_movements` row.
     c. If no single lot has enough, the v1 behavior is to **fail with a localized error** ("No hay un lote con suficiente existencia; registre un nuevo lote o disperse manualmente"). Split-lot dispensing is out of scope — see Q-SPLIT.
  3. Medication administration only handles `kind=DRUG` orders. Lot debits for `SUPPLY` / `FOOD` items go through `InventoryMovementService` directly, not through this path.
- Manual lot override is allowed for `ADMIN` via an optional `lotId` parameter; nurses cannot override.
- The selected `lot_id` is also persisted on `medication_administrations` itself (`lot_id BIGINT NULL REFERENCES inventory_lots(id)`) so audit and the eventual PDF export can show which lot was used.
- `CreateMedicationAdministrationRequest` gains optional `quantity INT` (default `1`, required positive for `GIVEN`; ignored for non-`GIVEN`) and optional `lotId Long?` for admin override only.
- **Billing semantics for `quantity > 1`.** A `MedicationAdministration` with `quantity = N` produces a **single** `PatientCharge` with `quantity = N` (not `N` separate charges) and `amount = item.price * N`. The lot is debited by `N` in the same transaction. The existing daily-balance and invoice rollups treat the row identically; no separate consolidation step is needed. This keeps the charge-per-event model in [`clinical-event-billing-automation.md`](clinical-event-billing-automation.md) intact while letting nurses record realistic doses.

### FR-7. Expiry dashboard

- New endpoint `GET /api/v1/medications/expiry-report` (auth: `medication:expiry-report`).
- Query params:
  - `window` (optional, default `90`) — yellow window in days.
  - `urgentWindow` (optional, default `30`) — red window in days.
  - `section` (optional) — filter by `MedicationSection`.
  - `controlled` (optional boolean) — filter to controlled substances only.
- Response shape (one row per **lot**, not per item):

```json
{
  "generatedAt": "2026-05-13T10:15:00",
  "totals": { "red": 12, "yellow": 38, "green": 412, "expired": 4, "noExpiry": 2 },
  "items": [
    {
      "lotId": 1042,
      "itemId": 87,
      "sku": "A12",
      "genericName": "OLANZAPINA",
      "commercialName": "ZYPREXA",
      "strength": "5 MG",
      "section": "PSIQUIATRICO",
      "lotNumber": "L-2025-08",
      "expirationDate": "2026-07-01",
      "daysToExpiry": 49,
      "status": "YELLOW",
      "quantityOnHand": 14,
      "recalled": false
    }
  ]
}
```

- `status` values: `EXPIRED` (date < today), `RED` (≤ `urgentWindow` days), `YELLOW` (≤ `window` days), `GREEN` (> `window` days), `NO_EXPIRY` (sentinel `9999-12-31` legacy lots — see § Migration / Backfill).
- Sort default: `status` (worst first) then `expirationDate` ascending.

### FR-9. Kind-default inventory category (auto-resolved on medication create)

The `MedicationFormDialog` no longer exposes a category selector. Pharmacists picking from the full `inventory_categories` list — which mixes "Medicamentos", "Material y Equipo", "Laboratorios", "Servicios", "Personal Especial", "Ingredientes de Cocina", … — could (and did) misfile drugs under unrelated buckets. The system already knows the right answer because `kind=DRUG` is non-negotiable; making the user supply the category is redundant noise.

Backend changes:

- New column `inventory_categories.default_for_kind VARCHAR(20) NULL`, validated against the `InventoryKind` enum at the application layer.
- Partial unique index `ux_inventory_categories_default_for_kind ON inventory_categories(default_for_kind) WHERE default_for_kind IS NOT NULL AND deleted_at IS NULL`: at most one active category may be the default for any given kind.
- V113 migration backfills the existing `Medicamentos` row to `default_for_kind = 'DRUG'`. The CSV loader (V111) is unaffected — it already picks the same row by name.
- `InventoryCategoryRepository.findByDefaultForKindAndDeletedAtIsNullAndActiveTrue(InventoryKind)` returns the resolved category, or `null` if no admin has flagged one.
- `PharmacyService.createMedication` no longer reads `categoryId` from the request body — it calls the repo above and throws `409 Conflict` ("No hay una categoría predeterminada para medicamentos; configure `default_for_kind=DRUG` en una categoría de inventario") if the lookup returns null.
- `MedicationDetailsService.update` likewise drops `categoryId` from its input shape. Editing a drug never changes its category through the pharmacy form. (An admin who needs to re-shelve a drug under a different category can still do so through the generic inventory edit screen — out of scope here.)
- `CreateMedicationRequest` and `UpdateMedicationRequest` lose the `categoryId` field. The `InventoryCategoryResponse` gains a `defaultForKind` field so the admin UI can render the badge / disable destructive actions on default categories.

Guard rails on `InventoryCategoryService`:

- `deleteCategory` returns `400 Bad Request` if the target row has a non-null `default_for_kind`.
- `updateCategory` returns `400 Bad Request` if a request would set `active = false` on a row whose `default_for_kind` is non-null. (Renaming is allowed — it does not affect routing.)
- `default_for_kind` is **not** user-editable through the admin form in v1: it ships with one seeded value (`Medicamentos → DRUG`) and is changed via SQL migration if and only if the customer ever asks for a different routing. Exposing it as an admin-editable field would invite the same "select the wrong one" failure mode this change is meant to prevent.

Symmetric guard on the **general inventory form** (`InventoryItemFormView`):

- The category select filters `categoryStore.activeCategories` down to rows where `defaultForKind == null`. Kind-routed buckets are write-protected — items belonging in them are created through their dedicated form (drugs via `/pharmacy`).
- `InventoryItemService.createItem` and `InventoryItemService.updateItem` enforce the same invariant server-side: when the request targets a category whose `defaultForKind` is non-null, the effective `kind` of the item must equal `defaultForKind`. Otherwise the call returns `400 Bad Request` ("Esta categoría está reservada para artículos de tipo {kind}; cree el artículo desde el formulario correspondiente"). The check is belt-and-braces for the UI filter and protects API clients (workbook loaders, integration tests, future bulk imports).
- The category **filter** on `InventoryItemsView` (the list-page dropdown that narrows the read view) keeps showing every active category, default-routed or not — a drug listed under "Medicamentos" is a legitimate read-time slice. Only the write path is restricted.

Frontend changes:

- `MedicationFormDialog.vue` removes the `Categoría` select and the `categoryStore.fetchActiveCategories()` call. `categoryId` is dropped from `createMedicationSchema` and from the payload submitted to `/api/v1/medications`.
- The admin Inventory Categories list (`InventoryCategoriesView.vue`) shows a "Predeterminada para Medicamentos" badge on the default-for-DRUG row and disables the delete + deactivate actions for it. Tooltip explains why.

### FR-8. Initial catalog load (one-shot Flyway migration)

The customer workbook (~615 SKUs, sections A/B/C/D drugs + E supplies) is
loaded **once** by Flyway migration `V111__seed_pharmacy_from_workbook.kt`
from the checked-in CSV at
`api/src/main/resources/db/migration/data/pharmacy-initial-load.csv`. After
the migration completes the catalog is maintained manually through the
existing inventory and pharmacy UIs — there is no permanent bulk-import
endpoint or page (the original `POST /api/v1/medications/bulk-import` was
removed, and `medication:bulk-import` is dropped by V112).

CSV columns: `sku, genericName, commercialName, strength, expiration,
section, dosageForm, lotNumber, quantityOnHand, receivedAt, supplier, kind`.

Load semantics:

- DRUG rows (sections A/B/C/D) → `inventory_items` (`kind='DRUG'`,
  `lot_tracking_enabled=TRUE`, `sku`, `category_id=Medicamentos`,
  `quantity=0`, `price=0`, `cost=0`, `pricing_type='FLAT'`, `active=TRUE`)
  plus a `medication_details` satellite with `review_status='CONFIRMED'`.
  Section is derived from the SKU prefix (A→PSIQUIATRICO, B→JARABE_GOTAS,
  C→NO_PSIQUIATRICO, D→AMPOLLA).
- SUPPLY rows (section E) → `inventory_items` (`kind='SUPPLY'`,
  `lot_tracking_enabled=FALSE`, `sku`, `category_id=Material y Equipo`).
- No initial lots: the pharmacist registers real stock via the UI on first
  restock. The synthetic-legacy lots V106 used to fabricate from V052
  scalar quantities are removed by V110 along with the V052 drug rows.
- Malformed rows abort the migration and fail Spring startup. The fix
  loop is "edit the CSV, redeploy" — there is no per-row outcome report.

---

## Acceptance Criteria / Scenarios

### Schema & invariants

1. After V100+ migrations run, `SELECT kind FROM inventory_items` returns a non-null value for every existing row, backfilled per § Migration / Backfill.
2. Every existing `medical_orders.inventory_item_id` and `patient_charges.inventory_item_id` reference still resolves to a valid `inventory_items` row (no FK churn). Every existing `medication_administrations.medical_order_id` still resolves, and that order still resolves to the same item it did before migration. `doctor_fees.patient_charge_id` is unchanged.
3. Creating an `InventoryItem` with `kind=DRUG` without a `MedicationDetails` row returns `400 Bad Request` from `InventoryItemService`.
4. Creating a `MedicationDetails` row for an item whose `kind != DRUG` returns `400 Bad Request`.
5. Reclassifying an item from `DRUG` to `SUPPLY` removes the `MedicationDetails` row in the same transaction; reclassifying from `SUPPLY` to `DRUG` requires a `MedicationDetails` payload in the request.
6. Soft-deleting a lot that has at least one `inventory_movements` row referencing it returns `409 Conflict`.
7. Setting `quantity_on_hand` to a negative value on a lot is rejected by the DB CHECK constraint.
8. Setting `lotTrackingEnabled=false` for a `DRUG`, or for any item with non-deleted lots, returns `400 Bad Request`.

### FEFO dispensing

9. With two lots for the same drug (Lot A expiring 2026-06-30 qty=5, Lot B expiring 2026-12-31 qty=10), administering 3 units debits Lot A and records `lot_id = A` on both the movement and the `medication_administrations` row.
10. With two lots (Lot A qty=5 exp 2026-06-30, Lot B qty=10 exp 2026-12-31), administering **7** units fails with the "no single lot has enough" error and **does not** debit either lot.
11. With one lot recalled and one fresh, FEFO selects the fresh lot even if the recalled lot would expire sooner.
12. Two nurses administering the same medication concurrently must produce two correct decrements of the lot (no lost update). Integration-tested with two threads racing on the same lot — the lock guarantees serialization (see Risks R-FEFO).
13. Admin can override the FEFO selection by passing an explicit `lotId`; nurses receive `403` if they pass one.

### Expiry dashboard

14. A lot expiring 25 days from today returns `status=RED`. A lot expiring 80 days from today returns `status=YELLOW`. A lot expiring 365 days from today returns `status=GREEN`. A lot whose date is yesterday returns `status=EXPIRED`. A lot with the sentinel date `9999-12-31` returns `status=NO_EXPIRY`.
15. The `totals` block sums to the number of lots in `items[]` (no double-counting).
16. With `section=PSIQUIATRICO`, only lots whose item has `MedicationDetails.section = PSIQUIATRICO` appear.
17. Medications with `MedicationDetails.reviewStatus=NEEDS_REVIEW` still appear in the Pharmacy UI and expiry dashboard with a review badge.

### Backfill review workflow

18. Editing a medication with `MedicationDetails.reviewStatus = NEEDS_REVIEW`, correcting its fields, and saving sets `reviewStatus = CONFIRMED` and clears `reviewNotes`; the row no longer renders the review badge in the Pharmacy list or expiry dashboard.
19. The `pharmacy_backfill_review` table/view returns zero rows for items whose `MedicationDetails.reviewStatus` has been transitioned to `CONFIRMED`.

### Initial workbook load (V111)

20. Running migrations against a fresh database loads ≥400 DRUG rows and ≥100 SUPPLY rows from `pharmacy-initial-load.csv` (sections A/B/C/D drugs + E supplies).
21. Every loaded DRUG row has exactly one `medication_details` satellite with `review_status='CONFIRMED'`. No `synthetic_legacy` lots remain after V110 + V111.
22. A malformed CSV row aborts the migration and fails Spring startup — there is no per-row outcome report; the fix loop is "edit the CSV, redeploy".
23. V112 has dropped the `medication:bulk-import` permission and its `role_permissions` rows.

### Kind-default category (FR-9)

24a. V113 has added `default_for_kind` to `inventory_categories` and backfilled the `Medicamentos` row to `'DRUG'`. The partial unique index rejects a second category being flagged `default_for_kind='DRUG'` while one already exists.
24b. `POST /api/v1/medications` no longer accepts `categoryId` in the request body; passing one is silently ignored. The created item's category is the row returned by `inventory_categories WHERE default_for_kind='DRUG' AND active=true AND deleted_at IS NULL`. If no such row exists, the endpoint returns `409 Conflict`.
24c. `PUT /api/v1/medications/{id}` no longer accepts `categoryId`. The drug's category is unchanged.
24d. `DELETE /api/v1/admin/inventory-categories/{id}` against the default-for-DRUG row returns `400 Bad Request` with an explanatory message.
24e. `PUT /api/v1/admin/inventory-categories/{id}` with `active=false` against the default-for-DRUG row returns `400 Bad Request`. Renaming the row is allowed.
24f. `InventoryCategoryResponse` exposes `defaultForKind`; the admin list view renders a badge and disables the delete/deactivate buttons on the default-for-DRUG row.
24g. `POST /api/v1/admin/inventory-items` with a `categoryId` whose `defaultForKind == 'DRUG'` and `kind != 'DRUG'` (or omitted, since the DTO default is `SUPPLY`) returns `400 Bad Request` with the kind-mismatch message.
24h. `PUT /api/v1/admin/inventory-items/{id}` that changes the category to a default-for-DRUG row when the item's effective kind is not `DRUG` returns `400 Bad Request`.
24i. `InventoryItemFormView` (the general inventory create/edit page) hides every category whose `defaultForKind` is non-null from its category select. The same rows remain visible in `InventoryItemsView`'s **filter** dropdown.

### Medication administration & billing

26. Administering a medication with `quantity = 3` produces exactly **one** `PatientCharge` with `quantity = 3` and `amount = item.price * 3`, debits the selected lot by 3, and writes one `inventory_movements` row with `quantity = 3`.
27. Administering with `quantity = 0` or a negative value returns `400 Bad Request` and produces no charge or movement.
28. Administering with `quantity = N` against a lot that has fewer than `N` units remaining fails with the "no single lot has enough" error and produces no charge, no movement, and no debit.

### Backwards-compatibility

29. After migration, every charge already on disk renders identically in the existing billing screens.
30. Existing `MedicationAdministration` rows show `lotId = NULL` (legacy) and are otherwise unchanged.
31. The existing low-stock report (`inventory-module.md` § FR-5) returns the same items as before for scalar-stock items; for lot-tracked items it reads the recomputed `quantity` value (still the source-of-truth column).

---

## Non-Functional Requirements

- **Performance**:
  - The expiry dashboard returns in < 500 ms for ≤ 5 000 lots (the customer's catalog is ~700 SKUs, so ~1 500 active lots at peak).
  - FEFO lot selection is a single indexed query (`idx_inventory_lots_item_fefo`) and must complete in < 20 ms p95.
  - Bulk import processes ≥ 50 rows/s.
- **Reliability**:
  - Every dispensing call writes one `inventory_movements` row in the same transaction as the lot debit. A failed dispense leaves no orphan movement.
  - Stored `inventory_items.quantity` and `SUM(inventory_lots.quantity_on_hand)` must agree after every service call; a drift detection scheduled job (daily) logs any mismatch to `audit_logs` with `status=FAILED`.
- **Security**:
  - All endpoints are gated by the permissions in § Authorization.

---

## Date / Time Conformance

- [x] Backend: `expiration_date`, `received_at` are `LocalDate` + `DATE`. No `String`-stored dates, no `TIMESTAMPTZ`, no `TIME`-only columns.
- [x] Frontend: all expiry / received dates render through `formatDate` from `@/utils/format`. No `toLocaleString` / `d(...)`.
- [x] Workbook expirations are parsed from `MM/YY` and `MM/YYYY` strings via a dedicated `MedicationExpirationParser` that normalizes to `LocalDate(year, month, lastDayOfMonth)` — the customer's convention treats expiration as end-of-month. The V111 loader and lot-create UI both use the parser. Inline `.toISOString().substring(0, 10)` is forbidden.
- [x] `<DatePicker>` instances for lot expiration / received-at inherit the global `dd/mm/yy` default. No per-component override.
- [x] No datetime pickers introduced; if a future "received at time" is added, it must carry `hourFormat="24"`.
- [x] `Date → API` conversions in the lot dialog use `toApiDate(...)`.

---

## API Contract

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| GET | `/api/v1/medications` | - | `PagedResponse<MedicationResponse>` | `medication:read` | List medications (joined view of `InventoryItem` + `MedicationDetails`). Filters: `section`, `controlled`, `search` |
| GET | `/api/v1/medications/{id}` | - | `MedicationResponse` | `medication:read` | Get one medication with its lots |
| POST | `/api/v1/medications` | `CreateMedicationRequest` | `MedicationResponse` | `medication:create` | Create item + medication_details in one call |
| PUT | `/api/v1/medications/{id}` | `UpdateMedicationRequest` | `MedicationResponse` | `medication:update` | Update drug attributes |
| GET | `/api/v1/medications/expiry-report` | - | `ExpiryReportResponse` | `medication:expiry-report` | Color-coded expiry dashboard |
| GET | `/api/v1/inventory/items/{itemId}/lots` | - | `List<InventoryLotResponse>` | `inventory-lot:read` | List lots for an item |
| POST | `/api/v1/inventory/items/{itemId}/lots` | `CreateInventoryLotRequest` | `InventoryLotResponse` | `inventory-lot:create` | Register new lot |
| PUT | `/api/v1/inventory/lots/{lotId}` | `UpdateInventoryLotRequest` | `InventoryLotResponse` | `inventory-lot:update` | Edit lot metadata, recall, restore |
| DELETE | `/api/v1/inventory/lots/{lotId}` | - | - | `inventory-lot:update` | Soft delete a lot with zero movements |
| POST | `/api/v1/inventory/items/{itemId}/reconcile-quantity` | - | `InventoryItemResponse` | `inventory-item:update` (existing) | Force-recompute `quantity` from lots |

### Request/Response shapes (illustrative)

```json
// POST /api/v1/medications
// Note: categoryId is intentionally absent — the server resolves the
// category via inventory_categories.default_for_kind='DRUG'.
{
  "sku": "A12",
  "name": "OLANZAPINA (ZYPREXA 5 MG)",
  "lotTrackingEnabled": true,
  "price": 136.27,
  "cost": 45.43,
  "restockLevel": 5,
  "pricingType": "FLAT",
  "medication": {
    "genericName": "OLANZAPINA",
    "commercialName": "ZYPREXA",
    "strength": "5 MG",
    "dosageForm": "TABLET",
    "route": "PO",
    "controlled": false,
    "section": "PSIQUIATRICO",
    "reviewStatus": "CONFIRMED"
  }
}

// POST /api/v1/inventory/items/{itemId}/lots
{
  "lotNumber": "L-2025-08",
  "expirationDate": "2026-07-31",
  "quantityOnHand": 30,
  "receivedAt": "2026-04-01",
  "supplier": "Distribuidora ABC"
}
```

### Error responses

| Status | Trigger |
|--------|---------|
| `400 Bad Request` | Invariant violation (drug without details, lot on non-lot-tracked item, negative quantity, FEFO no-single-lot-enough) |
| `401 Unauthorized` | Missing/expired token |
| `403 Forbidden` | Missing permission |
| `404 Not Found` | Unknown item/lot id |
| `409 Conflict` | Soft-delete on a lot with movements; duplicate SKU on create; medication create when no `default_for_kind='DRUG'` category exists |
| `413 Payload Too Large` | Bulk import file > 5 MB |
| `429 Too Many Requests` | Bulk import rate limit hit |

---

## Database Changes

### New entities

| Entity | Table | Extends | Description |
|--------|-------|---------|-------------|
| `MedicationDetails` | `medication_details` | `BaseEntity` | 1:1 satellite of `InventoryItem` (`item_id` unique FK) carrying drug-specific attributes |
| `InventoryLot` | `inventory_lots` | `BaseEntity` | 1:N satellite of `InventoryItem` tracking a single lot's quantity and expiration |

### Modified entities

| Entity | Change |
|--------|--------|
| `InventoryItem` | New columns: `kind VARCHAR(20) NOT NULL`, `sku VARCHAR(20) NULL UNIQUE`, `lot_tracking_enabled BOOLEAN NOT NULL DEFAULT FALSE` |
| `InventoryMovement` | New column: `lot_id BIGINT NULL REFERENCES inventory_lots(id)` |
| `MedicationAdministration` | New column: `lot_id BIGINT NULL REFERENCES inventory_lots(id)` |
| `InventoryCategory` | New column: `default_for_kind VARCHAR(20) NULL` — at most one active non-deleted row per kind (partial unique index). Flags the system-routing category for a given `InventoryKind`. |

### New enums (Kotlin)

- `InventoryKind`: `DRUG | SUPPLY | EQUIPMENT | SERVICE | PERSONNEL | FOOD`
- `DosageForm`: `TABLET | CAPSULE | AMPOULE | SYRUP | DROPS | CREAM | INJECTION | POWDER | PATCH | OTHER`
- `AdministrationRoute`: `PO | IM | IV | SC | TOPICAL | INHALED | OTHER`
- `MedicationSection`: `PSIQUIATRICO | NO_PSIQUIATRICO | JARABE_GOTAS | AMPOLLA`
- `MedicationReviewStatus`: `CONFIRMED | NEEDS_REVIEW`
- `LotExpiryStatus`: `EXPIRED | RED | YELLOW | GREEN | NO_EXPIRY` (returned only by the dashboard; not persisted)

### New migrations

| Migration | Description |
|-----------|-------------|
| `V100__add_kind_sku_and_lot_tracking_to_inventory_items.sql` | Adds `kind` (NOT NULL, default backfilled from category — see below), `sku` (NULL UNIQUE), and `lot_tracking_enabled`. Drops the `kind` default after backfill. |
| `V101__create_medication_details_table.sql` | Creates `medication_details` with generated `id` and unique `item_id`. |
| `V102__create_inventory_lots_table.sql` | Creates `inventory_lots` with all indexes including the partial FEFO index. |
| `V103__add_lot_id_to_movements_and_administrations.sql` | Adds nullable `lot_id` to `inventory_movements` and `medication_administrations`. |
| `V104__seed_pharmacy_permissions.sql` | Inserts the new permissions and grants them per the matrix in § Authorization. |
| `V105__backfill_medication_details_from_v052_items.sql` | Best-effort parse of the 75 medication rows seeded in V052 into structured `medication_details` rows. Items the parser cannot confidently parse still receive a details row with `review_status = 'NEEDS_REVIEW'` and are surfaced in a `pharmacy_backfill_review` view (see Backfill plan). |
| `V106__seed_legacy_lots.sql` | For each `lot_tracking_enabled = TRUE` item with `quantity > 0`, inserts one synthetic legacy lot with `expiration_date = '9999-12-31'`, `lot_number = NULL`, `quantity_on_hand = item.quantity`, `received_at = item.created_at::date`, `synthetic_legacy = TRUE`. |
| `V113__add_default_for_kind_to_inventory_categories.sql` | Adds nullable `default_for_kind VARCHAR(20)` to `inventory_categories`, partial unique index over active non-deleted rows, and backfills the `Medicamentos` row to `'DRUG'`. |

### Backfill rules for `inventory_items.kind`

| Existing category | Backfilled `kind` |
|-------------------|-------------------|
| `Medicamentos` | `DRUG` |
| `Material y Equipo` with `pricingType = FLAT` | `SUPPLY` |
| `Material y Equipo` with `pricingType = TIME_BASED` | `EQUIPMENT` |
| `Laboratorios` | `SERVICE` |
| `Servicios` | `SERVICE` |
| `Personal Especial` | `PERSONNEL` |
| `Ingredientes de Cocina` | `FOOD` |
| `Alimentación` | `FOOD` |

This mapping is good-enough for the existing 7 seeded categories. Items added later under future categories get `kind` explicitly from the create request — the `NOT NULL` constraint enforces it post-V100.

### Backfill rules for `inventory_items.lot_tracking_enabled`

| Backfilled `kind` | `lot_tracking_enabled` |
|-------------------|------------------------|
| `DRUG` | `TRUE` |
| `SUPPLY` | `FALSE` by default; admin can enable per item before registering lots |
| `FOOD` | `FALSE` by default; admin can enable per item before registering lots |
| `EQUIPMENT` | `FALSE` |
| `SERVICE` | `FALSE` |
| `PERSONNEL` | `FALSE` |

### Index requirements

- [x] `inventory_lots.deleted_at` — soft-delete queries
- [x] `inventory_lots.item_id` — fetch lots for an item
- [x] `inventory_lots.expiration_date` — expiry dashboard sort/filter
- [x] Partial `(item_id, expiration_date) WHERE deleted_at IS NULL AND recalled = FALSE AND quantity_on_hand > 0` — FEFO selection
- [x] Partial unique `(item_id, lot_number, expiration_date) NULLS NOT DISTINCT WHERE deleted_at IS NULL` — import idempotency and duplicate-lot defense
- [x] Partial `(item_id) WHERE deleted_at IS NULL AND recalled = FALSE` — SUM recompute for derived `inventory_items.quantity` (covers active lots including zero-on-hand rows that the FEFO partial index excludes)
- [x] `medication_details.deleted_at` — soft-delete query
- [x] `medication_details.item_id` unique index — 1:1 satellite invariant
- [x] `inventory_movements.lot_id` — reverse-lookup for lot history
- [x] `medication_administrations.lot_id` — audit and PDF export

---

## Frontend Changes

### Components

| Component | Location | Description |
|-----------|----------|-------------|
| `PharmacyListView.vue` | `web/src/views/pharmacy/` | New top-level Pharmacy screen. Tabs per `MedicationSection` (A/B/C/D) mirroring the customer's workbook |
| `MedicationFormDialog.vue` | `web/src/components/pharmacy/` | Create/edit medication (item + details in one form). VeeValidate + Zod |
| `LotListPanel.vue` | `web/src/components/pharmacy/` | Lots tab inside the medication detail drawer, with color-coded expiry chips |
| `LotFormDialog.vue` | `web/src/components/pharmacy/` | Register new lot or edit metadata; recall toggle gated by `inventory-lot:update` |
| `ExpiryDashboardView.vue` | `web/src/views/pharmacy/` | Color-coded report (red/yellow/green/expired) with section filter |
| `InventoryListView.vue` (existing, updated) | `web/src/views/inventory/` | Gains a `kind` filter; rows with `kind=DRUG` link to the Pharmacy view rather than the inventory form |
| `MedicationAdministrationDialog.vue` (existing, updated) | `web/src/components/nursing/` | Shows the FEFO-selected lot before confirmation; nurses see read-only, admins can override |
| `KardexView.vue` (existing, updated) | `web/src/views/nursing/` | Active medication cards show the soonest-to-expire lot's date with the same color chip |

### Pinia stores

| Store | Location | Description |
|-------|----------|-------------|
| `usePharmacyStore` | `web/src/stores/pharmacy.ts` | List/filter medications, CRUD `MedicationDetails` |
| `useInventoryLotStore` | `web/src/stores/inventoryLot.ts` | Per-item lot list, create/update/recall |
| `useExpiryReportStore` | `web/src/stores/expiryReport.ts` | Dashboard query state |

### Routes

| Path | Component | Auth | Roles |
|------|-----------|------|-------|
| `/pharmacy` | `PharmacyListView` | Yes | `medication:read` |
| `/pharmacy/medications/:id` | `PharmacyDetailView` | Yes | `medication:read` |
| `/pharmacy/expiry-report` | `ExpiryDashboardView` | Yes | `medication:expiry-report` |

### Validation (Zod sketches)

```typescript
// web/src/schemas/medication.ts
// Note: categoryId is absent — the backend assigns the kind-default
// category (see FR-9). The form does not render a category select.
export const createMedicationSchema = z.object({
  sku: z.string().max(20).optional().nullable(),
  name: z.string().min(1).max(150),
  lotTrackingEnabled: z.literal(true),
  price: z.number().min(0),
  cost: z.number().min(0),
  restockLevel: z.number().int().min(0).default(0),
  pricingType: z.enum(['FLAT', 'TIME_BASED']).default('FLAT'),
  medication: z.object({
    genericName: z.string().min(1).max(150),
    commercialName: z.string().max(150).optional().nullable(),
    strength: z.string().max(50).optional().nullable(),
    dosageForm: z.enum(['TABLET', 'CAPSULE', 'AMPOULE', 'SYRUP', 'DROPS',
                        'CREAM', 'INJECTION', 'POWDER', 'PATCH', 'OTHER']),
    route: z.enum(['PO', 'IM', 'IV', 'SC', 'TOPICAL', 'INHALED', 'OTHER'])
      .optional().nullable(),
    controlled: z.boolean().default(false),
    atcCode: z.string().max(10).optional().nullable(),
    section: z.enum(['PSIQUIATRICO', 'NO_PSIQUIATRICO', 'JARABE_GOTAS', 'AMPOLLA']),
    reviewStatus: z.enum(['CONFIRMED', 'NEEDS_REVIEW']).default('CONFIRMED'),
    reviewNotes: z.string().max(500).optional().nullable(),
  }),
});

// web/src/schemas/inventoryLot.ts
export const createLotSchema = z.object({
  lotNumber: z.string().max(50).optional().nullable(),
  expirationDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/),  // ISO from toApiDate
  quantityOnHand: z.number().int().min(0),
  receivedAt: z.string().regex(/^\d{4}-\d{2}-\d{2}$/),
  supplier: z.string().max(150).optional().nullable(),
  notes: z.string().max(500).optional().nullable(),
});
```

### i18n keys (excerpt)

```
pharmacy.title
pharmacy.tabs.psiquiatrico
pharmacy.tabs.no_psiquiatrico
pharmacy.tabs.jarabe_gotas
pharmacy.tabs.ampolla
pharmacy.medication.generic_name
pharmacy.medication.commercial_name
pharmacy.medication.strength
pharmacy.medication.dosage_form
pharmacy.medication.route
pharmacy.medication.controlled
pharmacy.medication.section
pharmacy.lot.number
pharmacy.lot.expiration_date
pharmacy.lot.quantity_on_hand
pharmacy.lot.received_at
pharmacy.lot.supplier
pharmacy.lot.recall
pharmacy.expiry.status.expired
pharmacy.expiry.status.red
pharmacy.expiry.status.yellow
pharmacy.expiry.status.green
pharmacy.expiry.status.no_expiry
pharmacy.import.title
pharmacy.import.report.created
pharmacy.import.report.updated
pharmacy.import.report.skipped
pharmacy.import.report.failed
pharmacy.administration.fefo_selected_lot
pharmacy.administration.no_single_lot_enough
```

Spanish (`es.json`) is canonical; English mirrors it. Dosage form and route enum labels need Spanish translations (e.g. `TABLET → Tableta`, `PO → Oral`).

---

## Domain Model Changes

### Aggregate map (textual)

```
                   ┌─────────────────────────────────────┐
                   │           InventoryItem             │   (Billing Catalog aggregate root)
                   │  id (PK)                            │
                   │  kind: InventoryKind                │  <- NEW (discriminator, mutable)
                   │  sku: String?                       │  <- NEW (unique when present)
                   │  lotTrackingEnabled: Boolean        │  <- NEW (controls lot-derived quantity)
                   │  name, description                  │
                   │  category → InventoryCategory       │
                   │  price, cost, pricingType           │
                   │  timeUnit, timeInterval             │
                   │  quantity (derived when lot-tracked)│
                   │  restockLevel, active               │
                   └──────────┬──────────────────────────┘
                              │
        ┌─────────────────────┼──────────────────────────┐
        │ 1:1 unique item_id  │ 1:N FK                   │ N:1 (existing)
        │ (only DRUG)         │ (lotTrackingEnabled)     │
        ▼                     ▼                          ▼
┌────────────────────┐  ┌────────────────────────┐   InventoryMovement
│ MedicationDetails  │  │     InventoryLot       │   (entry/exit log;
│ id (PK)            │  │  id (PK)               │    gains lot_id)
│ item_id (unique FK)│  │  item_id (FK)          │
│ genericName        │  │  lotNumber             │
│ commercialName     │  │  expirationDate        │
│ strength           │  │  quantityOnHand        │
│ dosageForm         │  │  receivedAt            │
│ route              │  │  supplier              │
│ controlled         │  │  recalled              │
│ atcCode            │  │  notes                 │
│ section            │  │  syntheticLegacy       │
│ reviewStatus       │  │                         │
└────────────────────┘  └────────────────────────┘

(Reserved for a future spec, NOT in v1):
EquipmentUnit  ─ 1:N satellite of InventoryItem for kind=EQUIPMENT,
                  tracking serial numbers and device status.

Pharmacy bounded context owns: MedicationDetails, InventoryLot,
                                MedicationSection, FEFO logic.
Billing Catalog context owns:   InventoryItem, InventoryCategory,
                                pricing.
Movement context spans both:    InventoryMovement (lot_id when applicable).
```

### Identity model

- `MedicationDetails` uses the standard project `BaseEntity.id` and a `UNIQUE NOT NULL item_id` FK to its parent `InventoryItem`. This preserves the existing generated-id base class and still guarantees one details row per item.
- `InventoryLot` has its own surrogate id. Lots are first-class aggregates referenced from `inventory_movements.lot_id` and `medication_administrations.lot_id`.
- `MedicationAdministration` continues to reference the parent `MedicalOrder`; the ordered item remains available through `medical_orders.inventory_item_id`. The new `lotId` is **additional**, not a replacement.

### Why composition, not inheritance

(Recorded here for future readers — the alternatives were considered and rejected.)

- **JPA `SINGLE_TABLE` inheritance** would force one wide sparse table with NULLs for every kind-specific column — what we have today, only worse.
- **JPA `JOINED` inheritance** (`Medication extends InventoryItem`) preserves the FK but freezes `kind` as a Java class, making reclassification a `DELETE`+`INSERT` that loses the id and orphans every existing charge. It also forces `InventoryLot` into a sibling table because lots are shared by all `lotTrackingEnabled` items — i.e. composition has to exist anyway.
- **Bounded-context split** (separate `medications` table, polymorphic charge FK) would change the FK shape of `medical_orders`, `patient_charges`, `medication_administrations`, `doctor_fees`. Too expensive for the value delivered.
- **Composition + discriminator** (chosen): keeps the billing FKs stable, lets `kind` be data, isolates pharmacy invariants in the Pharmacy bounded context, and gives `InventoryLot` a natural home.

---

## Impact

### Affected backend code paths

| Path | Change |
|------|--------|
| `entity/InventoryCategory.kt` | Add `defaultForKind: InventoryKind?` field (FR-9) |
| `repository/InventoryCategoryRepository.kt` | Add `findByDefaultForKindAndActiveTrue(kind)` — backend uses this to resolve the drug category on medication create |
| `service/InventoryCategoryService.kt` | Reject `deleteCategory` and `updateCategory(active=false)` when the row has a non-null `defaultForKind` (FR-9) |
| `service/InventoryItemService.kt` | `createItem` / `updateItem` reject writes whose target category has `defaultForKind != null` and `effectiveKind != defaultForKind` (FR-9 symmetric guard) |
| `service/PharmacyService.kt` | `createMedication` no longer reads `categoryId` — resolves the category from `defaultForKind=DRUG`; throws 409 if none exists |
| `service/MedicationDetailsService.kt` | `update` no longer reads `categoryId` — the drug's category is invariant under pharmacy-form edits |
| `dto/request/CreateMedicationRequest.kt` | Remove `categoryId` field |
| `dto/request/UpdateMedicationRequest.kt` | Remove `categoryId` field |
| `dto/response/InventoryCategoryResponse.kt` | Add `defaultForKind: InventoryKind?` |
| `db/migration/V113__add_default_for_kind_to_inventory_categories.sql` | **New.** Adds column, partial unique index, backfills `Medicamentos → DRUG` |
| `entity/InventoryItem.kt` | Add `kind` (enum), `sku` (nullable unique), `lotTrackingEnabled`. No change to existing fields |
| `entity/MedicationDetails.kt` | **New**, 1:1 satellite with generated id + unique `item_id` |
| `entity/InventoryLot.kt` | **New**, 1:N child |
| `entity/InventoryMovement.kt` | Add nullable `lot: InventoryLot?` |
| `entity/MedicationAdministration.kt` | Add nullable `lot: InventoryLot?` |
| `service/InventoryItemService.kt` | Enforce kind/details invariants on create/update; enforce `lotTrackingEnabled` rules on quantity recompute |
| `service/InventoryItemService.kt` movement path (or extracted `InventoryMovementService.kt`) | Require `lotId` on ENTRY/EXIT for `lotTrackingEnabled` items; recompute item.quantity from lots |
| `service/MedicationAdministrationService.kt` | **FEFO lot selection step** before debit; persist `lot_id` on the administration row; new error code "no single lot has enough" |
| `service/MedicalOrderService.kt` | No structural change; medical orders continue referencing `itemId`. Order creation for a `DRUG` should warn if the item has zero on-hand quantity (existing low-stock pattern) |
| `service/BillingService.kt` | No structural change. Charges reference `inventoryItemId`, not the satellites |
| `scheduler/DailyChargeScheduler.kt` | No change (diet charges still by item) |
| `controller/PharmacyController.kt` | **New**, exposes the medication endpoints |
| `controller/InventoryLotController.kt` | **New**, exposes lot endpoints |
| `controller/InventoryItemController.kt` | Add `kind` to filters and response; existing endpoints stay backward-compatible |
| `service/MedicationExpirationParser.kt` | Parses `MM/YY`, `MM/YYYY`, `dd/MM/yyyy` into end-of-month `LocalDate`. Reused by V111 |
| `db/migration/V110__hard_delete_v052_drug_catalog.sql` | **New.** Hard-deletes V052 drug rows + their `medication_details` + synthetic-legacy lots (aborts if clinical refs exist) |
| `db/migration/V111__seed_pharmacy_from_workbook.kt` | **New** Flyway Java migration. Loads `pharmacy-initial-load.csv` into `inventory_items` + `medication_details` |
| `db/migration/V112__remove_medication_bulk_import_permission.sql` | **New.** Drops the `medication:bulk-import` permission seeded by V104 |
| `db/migration/data/pharmacy-initial-load.csv` | **New** committed artifact (~615 rows) — the customer workbook export read by V111 |

### Affected frontend code paths

| Path | Change |
|------|--------|
| `views/inventory/InventoryListView.vue` | Adds `kind` filter; for `kind=DRUG` rows links to `/pharmacy/medications/:id` instead of opening the inventory form |
| `views/pharmacy/*` | **New** views (list, detail, expiry dashboard) |
| `components/pharmacy/MedicationFormDialog.vue` | Remove the category `<Select>`, drop `categoryId` from Zod schema and POST payload, stop calling `categoryStore.fetchActiveCategories()` (FR-9) |
| `views/admin/InventoryCategoriesView.vue` | Show "Predeterminada" badge on default-for-kind rows; disable delete/deactivate actions on them with explanatory tooltip (FR-9) |
| `views/inventory/InventoryItemFormView.vue` | Filter the category select to hide `defaultForKind != null` rows so the general inventory form cannot file a non-drug under a system-routed bucket (FR-9 symmetric guard) |
| `types/inventoryCategory.ts` | Add `defaultForKind: InventoryKind | null` to `InventoryCategory` |
| `types/pharmacy.ts` | Drop `categoryId` from `Medication`, `CreateMedicationRequest`, `UpdateMedicationRequest` |
| `validation/pharmacy.ts` | Drop `categoryId` from `createMedicationSchema` |
| `components/pharmacy/*` | **New** form / list / dialog components |
| `components/medical-record/MedicationAdministrationDialog.vue` | Reads FEFO-selected lot from the API; admin override toggle |
| `components/nursing/kardex/*` | Show soonest-to-expire lot date + color chip per active medication |
| `stores/pharmacy.ts`, `stores/inventoryLot.ts`, `stores/expiryReport.ts` | **New** Pinia stores |
| `i18n/locales/{es,en}.json` | Add the `pharmacy.*` keys above |
| `router/index.ts` | Add `/pharmacy/*` routes guarded by the new permissions |

### Affected documents

- [`inventory-module.md`](inventory-module.md) — add a Revision History entry pointing here.
- [`nursing-module.md`](nursing-module.md) and [`nursing-kardex.md`](nursing-kardex.md) — note FEFO selection and the lot column on medication administration.
- [`clinical-event-billing-automation.md`](clinical-event-billing-automation.md) — note that medication billing continues to fire on administration; the lot id is now also recorded for audit.
- [`medical-psychiatric-record.md`](medical-psychiatric-record.md) — medical orders for medications can show selected lot in audit views.
- [`admission-export.md`](admission-export.md) — the PDF export should include the lot id alongside each medication administration row (small extension to its DTO; tracked here, implemented in a follow-up).
- [`roles-functionality-matrix.md`](../roles-functionality-matrix.md) and [`roles-functionality-matrix.es.md`](../roles-functionality-matrix.es.md) — add the new permissions per role.

---

## Migration / Backfill Plan

### Order of operations

1. **V100** — add `kind`, `sku`, and `lot_tracking_enabled` columns. `kind` gets a temporary default `'SERVICE'` so the column can be created NOT NULL; we backfill `kind` and `lot_tracking_enabled` in the same migration via `UPDATE` per the tables in § Database Changes; finally `ALTER COLUMN kind DROP DEFAULT`.
2. **V101–V103** — create `medication_details`, `inventory_lots`, add `lot_id` columns. All new columns nullable so the migration is non-disruptive.
3. **V104** — seed permissions and role grants. The new permissions are inert until controllers exist; this migration is safe to ship before the code that uses them.
4. **V105** — best-effort parse of existing medication names (the 75 rows from V052) into structured `MedicationDetails` rows.
5. **V106** — for every item with `lot_tracking_enabled = TRUE` and `quantity > 0`, create a **single synthetic legacy lot** with:
   - `expiration_date = '9999-12-31'` (sentinel, surfaces as `NO_EXPIRY` in the dashboard)
   - `lot_number = NULL`
   - `quantity_on_hand = inventory_items.quantity`
   - `received_at = inventory_items.created_at::date`
   - `synthetic_legacy = TRUE`
   - `notes = 'Legacy backfill — replace with real lots'`
   This guarantees every existing on-hand unit lives in a lot, so FEFO has something to bite into and movement history is not orphaned.
6. **V110–V112** — hard-delete the V052 legacy drug catalog (`kind='DRUG' AND sku IS NULL`) plus its synthetic-legacy lots, run the workbook loader, and drop the bulk-import permission. After these migrations the customer workbook is the single source of truth for the medication and supply catalog.

### V105 parser strategy

The 75 names in V052 follow a few patterns:

- `'AMPOLLA MIDAZOLAM (DORMICUM 15MG/3ML)'`
- `'TABLETA INDIVIDUAL OLANZAPINA (ZYPREXA 5 MG)'`
- `'CAJA DE OLANZAPINA (ZYPREXA 5 MG X 14 TABLETAS)'`
- `'SOLUCION SALINA DE 500 ML'`
- `'AMPOLLA DE CLORURO DE POTASIO 5 ML (200 MG/ML)'`

The parser:

1. Extracts a leading `dosageForm` hint (`AMPOLLA`, `TABLETA`, `CAJA`, `SOLUCION`, `FRASCO`, `SOBRE`, `LOCION`) → `DosageForm`.
2. Reads `(BRAND ...)` parenthetical → `commercialName`, `strength`.
3. Treats the remaining capitalized words as `genericName` (first noun phrase up to the parenthesis). If this confidence is low, falls back to the full legacy `inventory_items.name`.
4. Assigns `section` from the existing category + `dosageForm`:
   - `AMPOLLA` → `AMPOLLA`
   - `SYRUP`/`LOCION`/`FRASCO` → `JARABE_GOTAS`
   - everything else under `Medicamentos` → `NO_PSIQUIATRICO` by default (operator overrides PSIQUIATRICO manually for the relevant rows after backfill)

The parser is conservative: rows whose confidence is low still get a `MedicationDetails` row with `review_status='NEEDS_REVIEW'`, best-effort field values, and `review_notes`. They are also inserted into a `pharmacy_backfill_review` view/table (DDL in V105) with `medication_details_id`, `parsed_generic_name`, `parsed_brand`, `parsed_strength`, `confidence_score`, and `raw_name` so admin can correct them later. The dedicated review UI is out of scope for v1, but the Pharmacy list must surface a review badge and allow normal edit.

### Reversibility

- Every new column is nullable or has a backfilled default; rollback removes the new pharmacy columns/tables without touching pre-existing catalog, order, charge, or administration rows.
- `medication_details` and `inventory_lots` are isolated new tables; rollback drops them.
- `medication_administrations.lot_id` is nullable; rollback drops the column without touching existing rows.

---

## QA Checklist

### Backend
- [ ] All functional requirements implemented
- [ ] `MedicationDetails` extends `BaseEntity` with generated `id` and a unique non-null `item_id`
- [ ] `InventoryLot` extends `BaseEntity`, has `@SQLRestriction("deleted_at IS NULL")`, has the partial FEFO index
- [ ] DTOs used in controllers (no entity exposure)
- [ ] Service-layer invariants: DRUG ⇒ MedicationDetails + `lotTrackingEnabled=true`; non-DRUG ⇒ no MedicationDetails; `lotTrackingEnabled` ⇒ movements carry `lot_id`
- [ ] FEFO selection acquires `SELECT … FOR UPDATE` on the lot before debit
- [ ] Concurrent-dispense integration test (two threads racing on the same lot, both succeed in order, lot count is consistent)
- [ ] Drift detection job logs `quantity` vs `SUM(lots.quantity_on_hand)` mismatches as `audit_logs.status=FAILED`
- [ ] V111 loads the workbook CSV against a fresh database with no per-row failures
- [ ] V110 aborts loudly if any clinical record references a V052 drug row
- [ ] `MedicationExpirationParser` accepts `MM/YY`, `MM/YYYY`, `dd/MM/yyyy`; rejects everything else
- [ ] Detekt passes
- [ ] OWASP dependency-check passes (no new third-party libs introduced for CSV-only import; XLSX remains follow-up unless dependency review approves a parser)

### Frontend
- [ ] Pharmacy views guarded by `medication:read`
- [ ] Lot dialog guarded by `inventory-lot:create` / `inventory-lot:update`
- [ ] Expiry dashboard color chips match the API `status` values
- [ ] `MedicationAdministrationDialog` shows the FEFO-selected lot and explains it (i18n)
- [ ] Kardex shows soonest-to-expire lot date per active medication
- [ ] Form validation with VeeValidate + Zod
- [ ] ESLint/oxlint passes
- [ ] i18n keys present in `es.json` and `en.json`
- [ ] Vitest unit tests for the new stores and forms

### E2E (Playwright)
- [ ] Admin creates a medication + lot, nurse administers it, FEFO debits the right lot
- [ ] Admin recalls a lot; FEFO skips it
- [ ] Doctor and Treasury users cannot see the lot-create / medication-edit actions
- [ ] Expiry dashboard renders with section filter

### General
- [ ] API contract documented
- [ ] Flyway migrations clean-apply on Testcontainers
- [ ] Reviewed by project owner

---

## Documentation Updates Required

### Must update

- [ ] **[CLAUDE.md](../../CLAUDE.md)** — add Pharmacy module to "Implemented Features → Backend" and "Frontend"; append the new migrations to the migrations list
- [ ] **[ARCHITECTURE.md](../architecture/ARCHITECTURE.md)** — record the composition + discriminator decision; cross-link this spec from the Inventory section
- [ ] **[inventory-module.md](inventory-module.md)** — add a Revision History entry pointing here as the successor for medication/lot scope
- [ ] **[roles-functionality-matrix.md](../roles-functionality-matrix.md)** and **[roles-functionality-matrix.es.md](../roles-functionality-matrix.es.md)** — add the new permission rows

### Review for consistency

- [ ] **[nursing-module.md](nursing-module.md)** — mention FEFO selection in the medication-administration flow
- [ ] **[nursing-kardex.md](nursing-kardex.md)** — mention the expiry chip on active-medication cards
- [ ] **[clinical-event-billing-automation.md](clinical-event-billing-automation.md)** — note the `lot_id` audit field on administrations
- [ ] **[admission-export.md](admission-export.md)** — note the lot id appears in the medication-administration section of the PDF

---

## Risks and Mitigations

| # | Risk | Likelihood | Impact | Mitigation |
|---|------|------------|--------|------------|
| R-BACKFILL | **Data-quality on V105 parse**: the V052 medication names are free-text strings. The parser will mis-classify some (e.g. `'1 SOBRE DE CONTUMAX POLVO 17G'` has no clear generic/brand split). | High | Medium | Conservative parser; every DRUG still gets `MedicationDetails`. Low-confidence rows use best-effort values, `reviewStatus=NEEDS_REVIEW`, and a `pharmacy_backfill_review` entry. They remain visible/editable in Pharmacy with a review badge. |
| R-FEFO | **FEFO concurrency**: two nurses administer the same drug at the same moment; without locking, both could debit the same lot quantity, going negative. | Medium | High | `SELECT … FOR UPDATE` on the selected lot row inside the dispensing transaction. CHECK constraint `quantity_on_hand >= 0` is a final defense — a violating commit becomes a 500 and a `FAILED` audit log, not a corrupted lot. Integration test races two threads on the same lot. |
| R-DRIFT | **Derived quantity drift**: a code path that bypasses the lot service (e.g. a migration, a manual SQL fix, a future feature) updates `inventory_items.quantity` directly, desynchronizing it from `SUM(lots)`. | Medium | Medium | Daily reconciliation job logs mismatches. Manual `POST /reconcile-quantity` endpoint for ops. Long-term: trigger-based maintenance — explicitly deferred (see § Open Questions Q-PERF). |
| R-RECLASSIFY | **Kind reclassification mid-charge**: a drug is reclassified to supply while an admission has open charges that referenced it as a drug; reports treat it inconsistently. | Low | Medium | Reclassification is admin-only and requires confirmation. Existing `patient_charges` rows are immutable historical records; their displayed `kind` is the value **at charge time** (we copy `kind` into `patient_charges` only if a future audit requires it — out of scope here, captured as Q-CHARGEKIND). For v1, accept that historical reports of a now-reclassified item show the current kind. |
| R-LOTDEL | **Soft-delete of a lot with movements**: deleting a lot that has dispensing history breaks the FK chain and the audit trail. | Low | High | Service-layer guard: lots with any referencing movement or administration row return `409 Conflict` on delete. To remove a lot from circulation use `recalled=true` instead, which preserves all FKs. |
| R-PERM | **Permission matrix drift**: future roles inadvertently inherit `medication:*` permissions via a role template, granting unintended access to drug data. | Medium | Medium | V104 grants permissions explicitly per role, no transitive grants. Integration test asserts the set of roles holding each new permission matches the matrix in § Authorization. |
| R-I18N | **Dosage form / route enum labels** are English-coded but Spanish-displayed; mismatches between `DosageForm.TABLET` and the customer's "Tableta", or `AdministrationRoute.PO` and "Oral", break the UI. | Low | Low | Enum keys stay English in code; all display goes through `pharmacy.medication.dosage_form.*` and `route.*` i18n keys with Spanish as canonical. Lint rule (or a unit test): every enum value has both `en` and `es` translations. |
| R-PERF | **Quantity recompute cost**: every movement triggers a `SUM(lots.quantity_on_hand)` for the parent item. For an item with hundreds of historical lots this is fast (the FEFO partial index covers it), but the spec deliberately defers the trigger-based version to keep v1 simple. | Low | Low | Indexed query is cheap (≤ 5 ms for 1 000 lots in PostgreSQL 17). If profile shows otherwise, switch to a database trigger in a follow-up — explicitly deferred. |
| R-SCOPE | **Scope creep**: expiry alerts → push notifications → cron jobs → SMS gateway. The customer's spreadsheet is read by hand today; a real notification path is a bigger feature. | High | Medium | This spec ships the dashboard + the data model. Proactive notifications are listed under § Out of Scope. Any PR adding notifications must reference a follow-up spec. |
| R-EQUIPMENT | **`EquipmentUnit` kind is reserved but unimplemented.** Items reclassified to `EQUIPMENT` today get no satellite. | Medium | Low | The `EQUIPMENT` enum value exists so backfill is correct (cardiac monitor, infusion pump, oxygen rows from V051 land there). The `EquipmentUnit` satellite is explicitly **out of scope** here. Until it ships, equipment items behave exactly like today: scalar `quantity`, no serial tracking, time-based pricing intact. |
| R-LEGACY | **V052 catalog superseded by workbook**: the seeded V052 medication rows are hard-deleted by V110 once the workbook is the canonical source. | High | Medium | V110 asserts no clinical record (medical order, charge, administration, movement) references a V052 drug before deletion. The system is not yet in production, so this assertion is expected to pass. Aborts loudly otherwise. |
| R-INVARIANT | **DB does not enforce DRUG ⇔ MedicationDetails**: the service layer enforces it, but a direct SQL insert could create a DRUG without details. | Low | Medium | Acceptable v1. A `CHECK` constraint requiring a `medication_details` row exists for `kind=DRUG` is hard to express portably in SQL. The nightly integrity-check job reports violations, including DRUG without details, non-DRUG with details, and invalid `lot_tracking_enabled`/`kind` combinations. |

---

## Open Questions

| ID | Question |
|----|----------|
| Q-LOTREQ | **(Resolved in v1.2 — see FR-1.)** Are lots mandatory for every `DRUG`? Yes: `lot_tracking_enabled` is `TRUE` for all DRUGs, and V106 seeds a synthetic legacy lot for every DRUG with `quantity > 0`. There is no "DRUG without lots" state. |
| Q-FEFO-CONFIG | Should FEFO be configurable per item (e.g. some pharmacies dispense LIFO for stability)? Spec assumes a single global FEFO policy. |
| Q-DUALSIG | Should controlled-substance dispensing require dual signature (nurse + chief nurse / admin)? Spec captures the `controlled` flag but does not enforce dual-sig. |
| Q-SPLIT | How are **partial-unit** dispensings tracked (half an ampoule, 0.4 of a tablet)? Spec treats `quantity_on_hand` as integer; fractional dispensing is rejected. |
| Q-SPLITLOT | If a single dispense needs to draw from two lots (e.g. 7 units when Lot A has 5 and Lot B has 10), do we **split** the administration across two movements, or fail? Spec says fail in v1 — split-lot dispensing is a v2 problem. |
| Q-NOTIFY | Should the expiry dashboard send proactive notifications (email / in-app push / SMS) at the YELLOW/RED thresholds? Out of scope for v1; flagged in R-SCOPE. |
| Q-SUPPLIER | How do we model multiple suppliers for the same lot? Lot ↔ supplier is 1:1 in v1; multi-supplier lots are a v2 problem. |
| Q-CHARGEKIND | Should `patient_charges` snapshot the `kind` at charge time so reclassification cannot rewrite history? Captured in R-RECLASSIFY; deferred. |
| Q-PERM | Final sign-off on the proposed role/permission matrix in § Authorization. In particular: should `ADMINISTRATIVE_STAFF` see the expiry dashboard? Should `DOCTOR` see lot information at all? |
| Q-PERF | Should `inventory_items.quantity` be maintained by a database trigger instead of the service layer? Spec defers; revisit if drift incidents accumulate. |

---

## Out of Scope

The following are intentionally deferred to follow-up specs:

- `EquipmentUnit` satellite and equipment serial tracking. The `EQUIPMENT` enum value exists for completeness, but the satellite, serial numbers, maintenance status, and utilization metrics are a separate feature.
- **Proactive expiry notifications** (email / push / SMS). The dashboard is the v1 surface.
- **Supplier / purchase-order management.** Lots carry a free-text supplier; full PO workflow is a separate module.
- **Barcode scanning** for lot intake and administration.
- **Multi-warehouse** (the customer operates a single pharmacy).
- **Multi-currency** drug cost.
- **Split-lot dispensing** when a single administration draws from two lots (Q-SPLITLOT).
- **Dual-signature** dispensing for controlled substances (Q-DUALSIG).
- **Fractional / partial-unit** dispensing (Q-SPLIT).
- **Backfill-review UI** for low-confidence V105 rows. Captured in the backfill plan; ships as a follow-up.
- **PDF export integration** — adding the lot id to the medication-administration section in [`admission-export.md`](admission-export.md) is a small follow-up on that spec, not part of this one.

---

## Related Docs/Commits/Issues

- Predecessor: [`inventory-module.md`](inventory-module.md) — Billing Catalog v1
- Related: [`nursing-module.md`](nursing-module.md) — medication administration flow that gains FEFO
- Related: [`nursing-kardex.md`](nursing-kardex.md) — Kardex card may surface lot expiry chip
- Related: [`clinical-event-billing-automation.md`](clinical-event-billing-automation.md) — medication billing fires on administration; gains lot id for audit
- Related: [`medical-psychiatric-record.md`](medical-psychiatric-record.md) — medical orders reference items, unchanged
- Related: [`admission-export.md`](admission-export.md) — future extension to surface lot id in the PDF
- Customer source: `.context/attachments/Inventario de Medicamentos - Hoja 1_page_{1..20}.png` (provided 2026-05-13)
- Design discussion: Claude session on 2026-05-13 — composition vs inheritance trade-off recorded under § Domain Model Changes

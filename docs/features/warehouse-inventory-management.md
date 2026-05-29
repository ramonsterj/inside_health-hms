# Feature: Warehouse-Scoped Inventory Management (Bodegas)

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-05-28 | @author | Initial draft. Introduces named warehouses (bodegas) with **strict isolation**: each warehouse owns its own stock and can only dispense from itself. Adds inter-warehouse transfers, a new `MAINTENANCE` role, and the ability to charge non-medical consumables (e.g. broken towel) against an admission. Captures customer feedback from 2026-05-26. |
| 1.1 | 2026-05-29 | @author | Migrations renumbered to **V119–V121** because V117 became AUXILIARY_NURSE (nursing-roles-split) and V118 became the RESIDENT_DOCTOR occupancy-view grant (bed-occupancy-view). Implemented. V119 also seeds the `role_default_warehouses` mapping table and adds `warehouse_id` + `transfer_id` columns to `inventory_movements` (not in the original v1.0 DDL). Maintenance / non-medical warehouse charges bill as `ChargeType.SERVICE`. |
| 1.2 | 2026-05-29 | @author | **Stock-display transparency fix (FR-11).** The pharmacy catalog (list + medication detail) was showing a single `quantity` = system-wide total across all six bodegas with no label, while dispensing is warehouse-scoped — misleading the clinical roles that hold `medication:read` (NURSE / CHIEF_NURSE / DOCTOR), who could read "50 in stock" yet hit `error.warehouse.out-of-stock` because ENFERMERIA holds 0. The medication **detail** response now carries a per-warehouse `warehouseStock` breakdown (every active warehouse, including 0-on-hand ones) and the UI presents `quantity` explicitly as the all-bodegas total. No dispense-path behavior changes; this is a read-model + labeling fix. |
| 1.3 | 2026-05-29 | @author | **Read-scope parity fix (FR-6 / FR-7 / AC-13).** v1.0–1.2 only enforced the per-warehouse read scope on the dedicated stock view (`/warehouses/{id}/stock`), the expiry report, and the FEFO preview. The catalog list / low-stock endpoints (`/inventory-items?warehouseId=…`) returned a warehouse's per-item on-hand gated by `inventory-item:read` alone — a permission also granted to MAINTENANCE / NURSE / PSYCHOLOGIST for unrelated reasons (picking items on forms), so a bodega-scoped user could read another bodega's stock counts by passing its id. Clarified that **every** warehouse-scoped read facet enforces `WarehouseScopeService.assertCanView` when a `warehouseId` is supplied. The system-wide (no `warehouseId`) view is unchanged. |

---

## Overview

Today, every inventory row carries a single global stock count (`inventory_items.quantity`) and every lot carries a single global on-hand count (`inventory_lots.quantity_on_hand`). The customer operates **multiple physical warehouses** ("bodegas") and needs the system to mirror that reality:

- Stock lives in a **warehouse**, not in the catalog row.
- Each warehouse **owns its stock** and can **only dispense from itself**.
- Moving stock between warehouses is an **explicit transfer movement**, audited and visible.
- Some warehouses serve clinical floors (`enfermería`, `psicología`); others serve operations (`mantenimiento`, `cocina`, `administración`). A new **`MAINTENANCE`** role manages the maintenance warehouses and can charge non-medical consumables (cleaning supplies, replacement towels, etc.) against an admission.

This is the largest of the four feature areas from the 2026-05-26 customer feedback round. Companion specs: [[nursing-roles-split]], and updates inside [[medical-psychiatric-record]] v1.6/1.7.

### Relationship to existing inventory specs

This spec is **additive** on top of:

- [`inventory-module.md`](inventory-module.md) v1.x — `InventoryItem` identity, pricing, categories, restock, room pricing. **Unchanged.**
- [`pharmacy-and-inventory-evolution.md`](pharmacy-and-inventory-evolution.md) v1.x — `kind`, `sku`, `lotTrackingEnabled`, `MedicationDetails`, `InventoryLot`, FEFO, expiry dashboard. **Adjusted**: FEFO becomes warehouse-scoped; expiry dashboard gains a warehouse filter.

The catalog stays single (one row per SKU, system-wide). What becomes warehouse-scoped is **stock** (counts), **lots** (where they physically sit), and **movements** (where they entered / left from).

---

## Use Case / User Story

1. **As a pharmacist (ADMIN) operating the central warehouse**, I want to receive a delivery into the `administración` warehouse and then transfer part of it to the `enfermería` warehouse so that nurses can dispense from their own bodega.
2. **As a nurse (NURSE / CHIEF_NURSE)**, I want medication administration to dispense from the **`enfermería` warehouse only**, and to fail with a clear "out of stock in your warehouse" error if the drug is exhausted there — even if the central warehouse still has stock. The customer's operating model is that nursing must request a transfer rather than reach into another bodega.
3. **As a maintenance user**, I want my own login that lets me transfer cleaning kits, replacement towels, and other supplies from the `mantenimiento` warehouse into another warehouse (typically `enfermería`).
4. **As a maintenance user**, I want to charge a non-medical consumable directly to an admission (e.g. "patient stained / broke a towel — charge it to room 203"), so that breakages don't disappear into the noise.
5. **As an administrator**, I want a per-warehouse stock view (catalog filtered by warehouse) and per-warehouse low-stock report so that each bodega owner can plan their own restock.
6. **As a chief nurse**, I want to request a transfer **from** the maintenance or administration warehouse **into** my own warehouse, and have the request show up as a pending transfer that the source warehouse acts on. (Phase 2 — see Out of Scope.)
7. **As an auditor**, I want every transfer to record: source warehouse, destination warehouse, item, lot (when applicable), quantity, who initiated, who fulfilled, and the timestamp of each.

### Warehouses on day-1 (seeded)

| Code | Spanish label | English label | Notes |
|------|---------------|---------------|-------|
| `ADMINISTRACION` | Bodega de Administración | Administration warehouse | **Master / receiving warehouse.** All deliveries land here unless explicitly routed elsewhere. V111 catalog stays linked here. |
| `ENFERMERIA` | Bodega de Enfermería | Nursing warehouse | The only warehouse from which medication administration debits, by default. |
| `MANTENIMIENTO_1` | Bodega de Mantenimiento 1 | Maintenance warehouse 1 | Operated by MAINTENANCE users. |
| `MANTENIMIENTO_2` | Bodega de Mantenimiento 2 | Maintenance warehouse 2 | Customer operates two physical maintenance storage rooms. |
| `COCINA` | Bodega de Cocina | Kitchen warehouse | Food supplies. Diet charges and food-served items resolve here. |
| `PSICOLOGIA` | Bodega de Psicología | Psychology warehouse | Psychometric test forms, therapy materials. |

The warehouse list is **seeded but editable**: admins can add, rename, or deactivate warehouses through a new admin screen.

---

## Authorization / Role Access

### New permissions

| Code | Description | Resource | Action |
|------|-------------|----------|--------|
| `warehouse:read` | List warehouses and view per-warehouse stock | `warehouse` | `read` |
| `warehouse:create` | Create a new warehouse | `warehouse` | `create` |
| `warehouse:update` | Rename / activate / deactivate a warehouse | `warehouse` | `update` |
| `warehouse:delete` | Soft-delete a warehouse (only if empty of stock) | `warehouse` | `delete` |
| `warehouse-transfer:create` | Issue a transfer **from** a warehouse you have access to | `warehouse-transfer` | `create` |
| `warehouse-transfer:read` | View transfer history (all warehouses) | `warehouse-transfer` | `read` |
| `warehouse-transfer:receive` | Acknowledge receipt of an inbound transfer into a warehouse you own | `warehouse-transfer` | `receive` |
| `warehouse-charge:create` | Charge a non-medical consumable from a warehouse to an admission | `warehouse-charge` | `create` |

`inventory-movement:create` is **not** repurposed for transfers — transfers are their own audited aggregate (see Database Changes) so that the inventory-movement table continues to mean "single-warehouse entry/exit."

### New role: `MAINTENANCE`

| Permission | Granted? |
|------------|----------|
| `warehouse:read` | ✓ (limited to assigned warehouses by service-layer scope — see below) |
| `warehouse-transfer:create` | ✓ (only from assigned source warehouses) |
| `warehouse-transfer:read` | ✓ |
| `warehouse-charge:create` | ✓ |
| `inventory-item:read` | ✓ — needed to pick items on the transfer / charge form |
| `inventory-lot:read` | — (maintenance items are typically not lot-tracked; if a maintenance item is lot-tracked, the read is granted indirectly via warehouse stock view) |
| `admission:read` | ✓ — needed to pick the admission when charging a broken towel |
| `patient:read` | ✓ — to show "Room 203 — Juan Pérez" on the charge dialog |
| `medication:*`, `medical-order:*`, `nursing-note:*`, etc. | None |

`MAINTENANCE` users see only their assigned warehouses in the stock view (see "Warehouse assignment" below).

### Role grants for the new warehouse permissions

| Role | warehouse:read | warehouse:create / update / delete | warehouse-transfer:create | warehouse-transfer:read | warehouse-transfer:receive | warehouse-charge:create |
|---|---|---|---|---|---|---|
| ADMIN | ✓ | ✓ | ✓ (any source) | ✓ | ✓ (any dest) | ✓ |
| ADMINISTRATIVE_STAFF | ✓ | — | ✓ (admin/maintenance/cocina/psicología sources) | ✓ | ✓ | ✓ |
| CHIEF_NURSE | ✓ | — | ✓ (enfermería source only) | ✓ | ✓ (enfermería dest) | — |
| NURSE | ✓ (enfermería only) | — | — | ✓ (own warehouse only) | ✓ (enfermería dest) | — |
| AUXILIARY_NURSE | ✓ (enfermería only) | — | — | ✓ (own warehouse only) | — | — |
| DOCTOR | ✓ | — | — | — | — | — |
| RESIDENT_DOCTOR | ✓ | — | — | — | — | — |
| PSYCHOLOGIST | ✓ (psicología only) | — | ✓ (psicología source only) | ✓ (psicología only) | ✓ (psicología dest) | — |
| MAINTENANCE | ✓ (assigned only) | — | ✓ (assigned source only) | ✓ | — (out of scope for v1; ADMIN/ADMINISTRATIVE_STAFF receive on their behalf if needed) | ✓ |
| USER | — | — | — | — | — | — |

### Warehouse assignment (service-layer scope)

A new join table `user_warehouses(user_id, warehouse_id)` lists which warehouses a `MAINTENANCE` user owns. The service layer scopes their `warehouse-transfer:create` and stock view to assigned warehouses. Nurses are implicitly scoped to `ENFERMERIA`, psychologists to `PSICOLOGIA`, etc., via the role → default-warehouse mapping below.

| Role | Default-owned warehouse(s) |
|---|---|
| NURSE / AUXILIARY_NURSE / CHIEF_NURSE | `ENFERMERIA` |
| PSYCHOLOGIST | `PSICOLOGIA` |
| MAINTENANCE | rows in `user_warehouses` (typically one or both `MANTENIMIENTO_*`) |
| ADMIN / ADMINISTRATIVE_STAFF | all warehouses |
| DOCTOR / RESIDENT_DOCTOR | none (read-only on all) |

---

## Functional Requirements

### FR-1: Warehouses as first-class entities

- New `warehouses` table (`BaseEntity`-derived). Seeded with the six warehouses listed in "Warehouses on day-1".
- Warehouses are soft-deletable (cannot be deleted if stock > 0).
- Each warehouse has a `code` (immutable, unique), a localized `name`, an optional `description`, and an `active` flag.

### FR-2: Per-warehouse stock

- New `inventory_warehouse_stock` table holds the per-(item, warehouse) and per-(item, warehouse, lot) on-hand quantity. The lot field is nullable: for non-lot-tracked items, one row per (item, warehouse); for lot-tracked items, one row per (item, warehouse, lot).
- The legacy `inventory_items.quantity` and `inventory_lots.quantity_on_hand` columns are **dropped** at the end of the migration sequence. Read paths that previously summed those columns now sum `inventory_warehouse_stock` (and surface a `quantity` field that is the system-wide total, for backwards-compatible responses).
- Migration backfill: every existing `inventory_items.quantity` is copied into `inventory_warehouse_stock(item_id, warehouse_id=ADMINISTRACION, quantity)`; every existing `inventory_lots.quantity_on_hand` is copied to `inventory_warehouse_stock(item_id, warehouse_id=ADMINISTRACION, lot_id, quantity)`. The customer operates from scratch on this feature in production (V111 catalog has `quantity=0`), but dev seed data must round-trip.

### FR-3: Strict warehouse isolation on dispensing (customer-specified)

- `MedicationAdministrationService.create` resolves the dispensing warehouse from the **calling user's default warehouse** (NURSE / CHIEF_NURSE / AUXILIARY_NURSE → `ENFERMERIA`; DOCTOR / ADMIN → must pass `warehouseId` explicitly, default `ENFERMERIA`).
- FEFO selection runs **only against lots present in that warehouse**. If the medication is out of stock in the user's warehouse — even when stock exists elsewhere — the service returns 422 with code `error.warehouse.out-of-stock` and a message that names the user's warehouse and the medication.
- Same rule applies to `inventory-movement:EXIT` for non-medication items: an exit decrements the user's warehouse only.

### FR-4: Inter-warehouse transfers

- New endpoint: `POST /api/v1/warehouse-transfers` with body `{ sourceWarehouseId, destinationWarehouseId, itemId, lotId?, quantity, notes? }`.
- Gated by `warehouse-transfer:create` plus a service-layer scope check that the caller owns the source warehouse.
- A transfer is **atomic in a single transaction**: it writes two `inventory_warehouse_stock` deltas (source `-quantity`, destination `+quantity` for the same lot when lot-tracked) and one `inventory_transfer` aggregate row.
- The destination row is created if it does not exist (`ON CONFLICT (item_id, warehouse_id, lot_id) DO UPDATE`).
- Source-row decrement uses `SELECT … FOR UPDATE` to avoid the same race condition documented in pharmacy-and-inventory-evolution.md FR-4.
- Transfers carry their own state: `PENDING` (rare — only when v1 of the feature is later extended to support multi-step approval), `COMPLETED`, `CANCELLED`. **v1 ships with one-step transfers only — the create endpoint immediately marks them `COMPLETED`.** The `status` field is present from day 1 so v2 can add the approval workflow without a schema change.

### FR-5: Charging non-medical consumables

- New endpoint: `POST /api/v1/warehouse-charges` with body `{ warehouseId, itemId, lotId?, admissionId, quantity, reason, notes? }`.
- Gated by `warehouse-charge:create` plus service-layer scope on the source warehouse.
- Behavior: decrement the warehouse stock by `quantity` (same exit semantics as FR-3), record an `inventory_movement` of type `EXIT` linked to the warehouse and to the admission, and emit a `WarehouseChargeCreatedEvent` (handled `AFTER_COMMIT` in a `REQUIRES_NEW` transaction) that the billing module listens for to create a `PatientCharge` of type `ChargeType.SERVICE` (using the item's `price` × `quantity`).
- The `reason` field is a free-text string at minimum; we may later promote to an enum (`BREAKAGE`, `LOSS`, `CONSUMABLE_USE`, `OTHER`).
- This is the customer's "load a broken/stained towel to room 203" flow.

### FR-6: Per-warehouse stock view

- `GET /api/v1/admin/inventory-items?warehouseId={id}` extends the existing list endpoint with a warehouse filter. The returned `quantity` for each item is the on-hand in that warehouse (not the system-wide total).
- When a `warehouseId` is supplied, the endpoint enforces the same warehouse-view scope as the dedicated stock view (`WarehouseScopeService.assertCanView`): a caller restricted to certain bodegas (NURSE / CHIEF_NURSE / AUXILIARY_NURSE / PSYCHOLOGIST / MAINTENANCE) gets 403 `error.warehouse.view.denied` for a warehouse they do not own, even though they hold `inventory-item:read`. ADMIN / ADMINISTRATIVE_STAFF / DOCTOR / RESIDENT_DOCTOR see any warehouse. The system-wide list (no `warehouseId`) is unchanged.
- A new dashboard view `/warehouses/:code/stock` lists items in one warehouse with paging, search, and low-stock filter, gated by `warehouse:read` plus the user's warehouse scope.

### FR-7: Per-warehouse low-stock report

- `GET /api/v1/admin/inventory-items/low-stock?warehouseId={id}` — same shape as today's low-stock report, scoped to one warehouse, comparing `inventory_warehouse_stock.quantity` against `inventory_items.restock_level`.
- When `warehouseId` is supplied, the same `assertCanView` scope as FR-6 applies. The system-wide report (no `warehouseId`) is unchanged.

### FR-8: Lot expiry dashboard gains a warehouse facet

- The existing `GET /api/v1/medications/expiry-report` accepts an optional `warehouseId` query param. Without it, the report aggregates across all warehouses (existing behavior — admin-only).
- Color-coding logic unchanged.

### FR-9: Default-warehouse auto-resolution

- Whenever an authenticated request needs an implicit warehouse and does not pass one, the resolver consults: explicit role → default mapping (FR-3 table); else `user_warehouses` rows for MAINTENANCE; else error `error.warehouse.unassigned` (422). Document the precedence in `WarehouseScopeService`.

### FR-10: Maintenance user management

- `MAINTENANCE` is a new system role (V119), seeded with zero users in production. Admins assign warehouses to maintenance users via the existing user-edit screen (new "Assigned warehouses" multi-select section, visible when the user has the `MAINTENANCE` role).
- Removing the `MAINTENANCE` role from a user does not delete the `user_warehouses` rows (they go dormant). Re-adding the role restores access.

### FR-11: Pharmacy catalog stock transparency (v1.2)

Because stock is warehouse-scoped but the catalog row is single, any UI that shows a bare "quantity" risks misleading a viewer into thinking that number is available to dispense. The medication-administration dialog and Kardex already avoid this (they show only the **warehouse-scoped FEFO preview** and surface `error.warehouse.out-of-stock` on failure). The pharmacy **catalog** must be equally honest:

- The medication **detail** response (`GET /api/v1/medications/{itemId}`) carries a `warehouseStock` array: one entry per **active** warehouse with the item's on-hand there, **including warehouses with 0 on-hand**, so a nurse always sees "Enfermería: 0" explicitly rather than inferring availability from the total. The detail view renders this breakdown.
- The `quantity` field remains the **system-wide total** (sum across all bodegas) for backward compatibility (see NFR "Backward compatibility"), but every surface that displays it labels it as the all-bodegas total — never an unqualified "Quantity".
- The medication **list** (`GET /api/v1/medications`) does **not** compute the breakdown (avoids N+1 across a paged catalog); it only relabels its total column. The breakdown is a detail-view concern.
- This is a read-model + labeling change only. Dispensing, FEFO, transfers, and charges are unaffected.

---

## Out of Scope (Phase 2 candidates)

- **Transfer-request approval workflow.** v1 transfers are one-step (issuer says go → done). A future Phase 2 will let nurses request a transfer that maintenance/admin must accept (`PENDING` status path).
- **Per-warehouse pricing.** The catalog still has one `price` per item. Different warehouses cannot charge different prices.
- **Stock reservation / hold.** No "reserve 10 units for a planned surgery" feature.
- **Cycle counts / discrepancy adjustments.** Quantity adjustments still go through the existing `inventory-movement` flow with an `EXIT` or `ENTRY` and a reason note. A formal cycle-count UI is not in scope.
- **Multi-currency, multi-supplier price lists.** Out.
- **Per-warehouse reorder thresholds.** v1 uses a single `restock_level` from the catalog row; the low-stock report compares it against the warehouse's quantity. Per-warehouse thresholds are Phase 2.

---

## Acceptance Criteria / Scenarios

- **AC-1 — Seeded warehouses.** After V119 (warehouses + per-warehouse stock + permissions + MAINTENANCE role) and V120 (backfill) run on a fresh DB, `SELECT code FROM warehouses ORDER BY code` returns exactly the six seeded codes.
- **AC-2 — Stock backfill.** After migration, for every catalog row that had `quantity > 0`, there is exactly one matching `inventory_warehouse_stock` row with `warehouse_id = ADMINISTRACION` and the same quantity. For every lot with `quantity_on_hand > 0`, same.
- **AC-3 — Legacy columns gone.** `inventory_items.quantity` and `inventory_lots.quantity_on_hand` are dropped at the end of the migration; reads route through the new table.
- **AC-4 — Nurse dispense uses ENFERMERIA only.** A NURSE administers a medication that has stock = 5 in ENFERMERIA and stock = 100 in ADMINISTRACION. The administration succeeds and decrements ENFERMERIA. If ENFERMERIA stock is 0, the call returns 422 `error.warehouse.out-of-stock` even though ADMINISTRACION has 100.
- **AC-5 — FEFO is per-warehouse.** ENFERMERIA has lot A expiring 2026-08-01 and lot B expiring 2026-06-15. ADMINISTRACION has lot C expiring 2026-05-30. A nurse dispense picks lot B (earliest in ENFERMERIA), not lot C.
- **AC-6 — Transfer happy path.** ADMIN POSTs a transfer of 20 units of item X from ADMINISTRACION to ENFERMERIA. After commit: ADMINISTRACION stock for X is `-20`, ENFERMERIA stock for X is `+20`, one `inventory_transfer` row with `status='COMPLETED'`, two `inventory_movement` rows (one EXIT on source, one ENTRY on destination, both linked to the transfer).
- **AC-7 — Transfer source-out-of-stock.** Transfer of 100 units when source has 50 returns 422 `error.warehouse.out-of-stock`, no rows written.
- **AC-8 — Transfer scope.** NURSE attempting to issue a transfer from ADMINISTRACION (not their warehouse) gets 403 `error.warehouse.transfer.source.denied`.
- **AC-9 — Maintenance charges towel.** A MAINTENANCE user POSTs a warehouse-charge for one towel from MANTENIMIENTO_1 against admission 42 with reason "Stained — patient repainting". After commit: MANTENIMIENTO_1 stock for the towel is `-1`, one `inventory_movement` row (EXIT), one `patient_charges` row with amount = price × 1, audit log of the transaction.
- **AC-10 — Charge scope.** A NURSE attempting `warehouse-charge:create` from ENFERMERIA gets 403 (NURSE does not have `warehouse-charge:create`).
- **AC-11 — Cannot delete non-empty warehouse.** DELETE on a warehouse that has any `inventory_warehouse_stock` row with quantity > 0 returns 409 `error.warehouse.not-empty`.
- **AC-12 — Cannot deactivate without empty stock OR consent.** Deactivating a warehouse (`active=false`) is allowed even with stock present (it just hides it from dropdowns); deleting requires empty. Document this distinction in the admin UI copy.
- **AC-13 — Maintenance assignment.** A MAINTENANCE user with `user_warehouses` row for MANTENIMIENTO_1 only sees that warehouse on the stock view; GET on MANTENIMIENTO_2 returns 403. The scope is enforced uniformly across **every** warehouse-scoped read facet — the dedicated stock view (`/warehouses/{id}/stock`), the catalog list and low-stock report when filtered (`/inventory-items?warehouseId=…`), the expiry report (`?warehouseId=…`), and the FEFO preview — so the user cannot inspect another bodega's stock by passing its id to any of them.
- **AC-14 — Low-stock per warehouse.** Item X has restock_level=10, ENFERMERIA stock=5, ADMINISTRACION stock=200. The low-stock report scoped to ENFERMERIA includes X; the same report scoped to ADMINISTRACION does not. The system-wide report (no warehouse filter) compares `SUM(quantity)` against restock_level — unchanged behavior.
- **AC-15 — Expiry report per warehouse.** The expiry report filtered to a warehouse only lists lots that have stock > 0 in that warehouse.
- **AC-16 — Concurrent dispenses don't oversell.** Two nurses on two devices dispense the last unit of a medication from ENFERMERIA at the same time. One succeeds, the other returns 422 (proved by integration test under `Testcontainers` with explicit two-transaction overlap).
- **AC-17 — Audit row on every transfer and charge.** Each transfer writes one `audit_logs` row (`WAREHOUSE_TRANSFER`); each charge writes one (`WAREHOUSE_CHARGE`).
- **AC-18 — Medication detail shows per-warehouse breakdown (FR-11).** Item X has ENFERMERIA stock=0 and ADMINISTRACION stock=50. `GET /api/v1/medications/{itemId}` returns `quantity=50` **and** a `warehouseStock` array that includes both `{ code: "ENFERMERIA", quantity: 0 }` and `{ code: "ADMINISTRACION", quantity: 50 }`. The detail view shows the total plus the breakdown; the list column for the same item is labelled as the all-bodegas total, not a bare "Quantity". The medication **list** response does not include `warehouseStock`.

---

## Non-Functional Requirements

- **Performance.** Per-warehouse stock view: < 300 ms for 1,000-row catalog at p95.
- **Concurrency.** All stock-mutating operations (administration, transfer, charge) take an `SELECT … FOR UPDATE` on the source `inventory_warehouse_stock` row before mutating. The same row may be touched by FEFO (administration), explicit transfer, and charge — they all serialize on the row lock.
- **Reliability.** Transfers and charges are written in `REQUIRES_NEW` transactions for the audit row so that an audit failure cannot silently drop a stock movement (same pattern as the admission PDF export, V099).
- **Backward compatibility.** Existing endpoints that returned `quantity` continue to do so — the value now means "stock in the resolved warehouse" when the request carries an implicit or explicit warehouse, or "system-wide total" when no warehouse context exists.

---

## Date / Time Conformance

Confirm the feature follows the platform-wide date/time standard documented in `CLAUDE.md` § "Date / Time Formatting" and `docs/architecture/ARCHITECTURE.md` § "Date and Time Handling".

- [x] Backend timestamps (`transferred_at`, `received_at`, audit `*_at`) use `LocalDateTime` + `TIMESTAMP`.
- [x] No date-only fields are introduced by this feature.
- [x] All frontend date rendering uses `formatDate` / `formatDateTime` from `@/utils/format`.
- [x] No new `<DatePicker>` instances.
- [x] No `Date → API string` conversions added.
- [x] Relative time (e.g. "Transferred 2h ago" on the transfer log) uses `getRelativeTime`.

---

## API Contract

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| GET | `/api/v1/warehouses` | - | `List<WarehouseResponse>` | `warehouse:read` | List warehouses visible to the caller (scoped by role). |
| POST | `/api/v1/warehouses` | `CreateWarehouseRequest` | `WarehouseResponse` | `warehouse:create` | Admin-only. |
| PUT | `/api/v1/warehouses/{id}` | `UpdateWarehouseRequest` | `WarehouseResponse` | `warehouse:update` | Admin-only. |
| DELETE | `/api/v1/warehouses/{id}` | - | - | `warehouse:delete` | 409 if not empty. |
| GET | `/api/v1/warehouses/{id}/stock` | - | `PagedResponse<WarehouseStockResponse>` | `warehouse:read` + scope | Per-warehouse catalog view. |
| POST | `/api/v1/warehouse-transfers` | `CreateTransferRequest` | `TransferResponse` | `warehouse-transfer:create` + scope | One-step transfer. |
| GET | `/api/v1/warehouse-transfers` | (filters) | `PagedResponse<TransferResponse>` | `warehouse-transfer:read` | History. |
| POST | `/api/v1/warehouse-charges` | `CreateChargeRequest` | `WarehouseChargeResponse` | `warehouse-charge:create` + scope | Non-medical consumable charge. |
| GET | `/api/v1/admin/inventory-items?warehouseId=…` | - | `PagedResponse<InventoryItemResponse>` | `inventory-item:read` | Extended with `warehouseId` filter; `quantity` is per-warehouse when provided. |
| GET | `/api/v1/medications/expiry-report?warehouseId=…` | - | `ExpiryReportResponse` | `medication:expiry-report` | Extended with `warehouseId` filter. |
| GET | `/api/v1/medications/{itemId}` | - | `MedicationResponse` | `medication:read` | Detail now carries a `warehouseStock` per-warehouse breakdown (FR-11). `quantity` stays the system-wide total. |

### Request/Response Examples

```json
// POST /api/v1/warehouse-transfers
{
  "sourceWarehouseId": 1,            // ADMINISTRACION
  "destinationWarehouseId": 2,       // ENFERMERIA
  "itemId": 42,
  "lotId": 17,                       // required when item is lot-tracked
  "quantity": 20,
  "notes": "Restock for the night shift"
}
// 201 Created
{
  "id": 100,
  "status": "COMPLETED",
  "sourceWarehouse": { "id": 1, "code": "ADMINISTRACION", "name": "Bodega de Administración" },
  "destinationWarehouse": { "id": 2, "code": "ENFERMERIA", "name": "Bodega de Enfermería" },
  "item": { "id": 42, "name": "Risperidona 2 mg", "sku": "A12" },
  "lot": { "id": 17, "lotNumber": "L-2026-04", "expirationDate": "2027-04-30" },
  "quantity": 20,
  "issuedBy": { "id": 3, "username": "admin" },
  "issuedAt": "2026-05-28T14:30:00",
  "completedAt": "2026-05-28T14:30:00"
}
```

```json
// POST /api/v1/warehouse-charges
{
  "warehouseId": 4,                  // MANTENIMIENTO_1
  "itemId": 88,                      // Toalla de baño
  "admissionId": 42,
  "quantity": 1,
  "reason": "Manchada — el paciente la pintó",
  "notes": null
}
// 201 Created
{
  "id": 500,
  "warehouse": { "id": 4, "code": "MANTENIMIENTO_1", "name": "Bodega de Mantenimiento 1" },
  "item": { "id": 88, "name": "Toalla de baño" },
  "admission": { "id": 42, "patientName": "Juan Pérez", "roomNumber": "203" },
  "quantity": 1,
  "amount": 75.00,
  "reason": "Manchada — el paciente la pintó",
  "chargeId": 1234,                  // resulting patient_charges.id
  "createdBy": { "id": 9, "username": "maint_user" },
  "createdAt": "2026-05-28T14:35:00"
}
```

---

## Database Changes

### New Entities

| Entity | Table | Extends | Description |
|---|---|---|---|
| `Warehouse` | `warehouses` | `BaseEntity` | A physical or logical bodega. |
| `InventoryWarehouseStock` | `inventory_warehouse_stock` | `BaseEntity` | Per-(item, warehouse, optional lot) on-hand quantity. |
| `InventoryTransfer` | `inventory_transfers` | `BaseEntity` | Atomic transfer aggregate (source, destination, item, lot, quantity, status, audit). |
| `WarehouseCharge` | `warehouse_charges` | `BaseEntity` | Audit row for non-medical consumable charges. Links to `patient_charges`. |
| `UserWarehouse` | `user_warehouses` | `BaseEntity` | Join table assigning MAINTENANCE users to warehouses they operate. |
| `RoleDefaultWarehouse` | `role_default_warehouses` | `BaseEntity` | Data-driven role → default-warehouse mapping (see Implementation Notes). |

### New Migrations (as shipped)

> **v1.1 renumbering.** V117 was claimed by AUXILIARY_NURSE (nursing-roles-split) and V118 by the RESIDENT_DOCTOR occupancy-view grant (bed-occupancy-view) before this feature shipped, so the actual sequence is **V119 → V120 → V121**.

| Migration | Description |
|-----------|-------------|
| `V119__add_warehouses_and_permissions.sql` | Create `warehouses`, `inventory_warehouse_stock`, `inventory_transfers`, `warehouse_charges`, `user_warehouses`, `role_default_warehouses`. Add `warehouse_id` + `transfer_id` columns to `inventory_movements`. Seed six warehouses. Seed the eight new permissions plus the `MAINTENANCE` role and its grants. Seed the role → default-warehouse mapping rows. |
| `V120__backfill_warehouse_stock.sql` | Copy every `inventory_items.quantity` and `inventory_lots.quantity_on_hand` into `inventory_warehouse_stock` with `warehouse_id = ADMINISTRACION`. |
| `V121__drop_legacy_stock_columns.sql` | Drop `inventory_items.quantity` and `inventory_lots.quantity_on_hand`. (Final step — only after backend code has switched to the new tables.) |

The V119/V120/V121 split exists so the cut-over from legacy stock columns can be staged. V119 + V120 are deploy-safe (additive). V121 is the irreversible cut.

### Schema Example (V119)

```sql
CREATE TABLE warehouses (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(50)  NOT NULL UNIQUE,
    name          VARCHAR(255) NOT NULL,
    description   TEXT,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    BIGINT,
    updated_by    BIGINT,
    deleted_at    TIMESTAMP
);
CREATE INDEX idx_warehouses_deleted_at ON warehouses(deleted_at);
CREATE INDEX idx_warehouses_active     ON warehouses(active) WHERE deleted_at IS NULL;

CREATE TABLE inventory_warehouse_stock (
    id            BIGSERIAL PRIMARY KEY,
    item_id       BIGINT       NOT NULL REFERENCES inventory_items(id),
    warehouse_id  BIGINT       NOT NULL REFERENCES warehouses(id),
    lot_id        BIGINT                REFERENCES inventory_lots(id),
    quantity      NUMERIC(14,3) NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    BIGINT,
    updated_by    BIGINT,
    deleted_at    TIMESTAMP
);
CREATE INDEX idx_iws_item_wh           ON inventory_warehouse_stock(item_id, warehouse_id);
CREATE INDEX idx_iws_warehouse         ON inventory_warehouse_stock(warehouse_id);
CREATE INDEX idx_iws_deleted_at        ON inventory_warehouse_stock(deleted_at);
-- One row per (item, warehouse, lot). lot_id IS NULL for non-lot-tracked items;
-- use NULLS NOT DISTINCT to keep the upsert tight (same pattern as inventory_lots).
CREATE UNIQUE INDEX uq_iws_item_wh_lot
    ON inventory_warehouse_stock (item_id, warehouse_id, COALESCE(lot_id, -1))
    WHERE deleted_at IS NULL;

CREATE TABLE inventory_transfers (
    id                       BIGSERIAL PRIMARY KEY,
    source_warehouse_id      BIGINT       NOT NULL REFERENCES warehouses(id),
    destination_warehouse_id BIGINT       NOT NULL REFERENCES warehouses(id),
    item_id                  BIGINT       NOT NULL REFERENCES inventory_items(id),
    lot_id                   BIGINT                REFERENCES inventory_lots(id),
    quantity                 NUMERIC(14,3) NOT NULL CHECK (quantity > 0),
    status                   VARCHAR(20)  NOT NULL DEFAULT 'COMPLETED',
    notes                    TEXT,
    issued_at                TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    issued_by                BIGINT       NOT NULL,
    completed_at             TIMESTAMP,
    completed_by             BIGINT,
    cancelled_at             TIMESTAMP,
    cancelled_by             BIGINT,
    created_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by               BIGINT,
    updated_by               BIGINT,
    deleted_at               TIMESTAMP,
    CHECK (source_warehouse_id <> destination_warehouse_id)
);
CREATE INDEX idx_transfers_source      ON inventory_transfers(source_warehouse_id);
CREATE INDEX idx_transfers_destination ON inventory_transfers(destination_warehouse_id);
CREATE INDEX idx_transfers_item        ON inventory_transfers(item_id);
CREATE INDEX idx_transfers_status      ON inventory_transfers(status);
CREATE INDEX idx_transfers_deleted_at  ON inventory_transfers(deleted_at);

CREATE TABLE warehouse_charges (
    id            BIGSERIAL PRIMARY KEY,
    warehouse_id  BIGINT       NOT NULL REFERENCES warehouses(id),
    item_id       BIGINT       NOT NULL REFERENCES inventory_items(id),
    lot_id        BIGINT                REFERENCES inventory_lots(id),
    admission_id  BIGINT       NOT NULL REFERENCES admissions(id),
    quantity      NUMERIC(14,3) NOT NULL CHECK (quantity > 0),
    amount        NUMERIC(14,2) NOT NULL,
    reason        VARCHAR(500) NOT NULL,
    notes         TEXT,
    charge_id     BIGINT                REFERENCES patient_charges(id),
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    BIGINT,
    updated_by    BIGINT,
    deleted_at    TIMESTAMP
);
CREATE INDEX idx_wc_warehouse   ON warehouse_charges(warehouse_id);
CREATE INDEX idx_wc_admission   ON warehouse_charges(admission_id);
CREATE INDEX idx_wc_deleted_at  ON warehouse_charges(deleted_at);

CREATE TABLE user_warehouses (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES users(id),
    warehouse_id  BIGINT       NOT NULL REFERENCES warehouses(id),
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    BIGINT,
    updated_by    BIGINT,
    deleted_at    TIMESTAMP,
    UNIQUE (user_id, warehouse_id)
);
CREATE INDEX idx_uw_user        ON user_warehouses(user_id);
CREATE INDEX idx_uw_deleted_at  ON user_warehouses(deleted_at);

CREATE TABLE role_default_warehouses (
    id            BIGSERIAL PRIMARY KEY,
    role_id       BIGINT       NOT NULL REFERENCES roles(id),
    warehouse_id  BIGINT       NOT NULL REFERENCES warehouses(id),
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    BIGINT,
    updated_by    BIGINT,
    deleted_at    TIMESTAMP,
    UNIQUE (role_id, warehouse_id)
);
CREATE INDEX idx_rdw_role        ON role_default_warehouses(role_id);
CREATE INDEX idx_rdw_deleted_at  ON role_default_warehouses(deleted_at);

-- inventory_movements gains warehouse + transfer linkage (not in the v1.0 DDL).
ALTER TABLE inventory_movements ADD COLUMN warehouse_id BIGINT REFERENCES warehouses(id);
ALTER TABLE inventory_movements ADD COLUMN transfer_id  BIGINT REFERENCES inventory_transfers(id);
CREATE INDEX idx_movements_warehouse ON inventory_movements(warehouse_id);
CREATE INDEX idx_movements_transfer  ON inventory_movements(transfer_id);
```

### Index Requirements

- [x] `deleted_at` on every new table.
- [x] FK indexes on all reference columns.
- [x] Hot-path composite index on `inventory_warehouse_stock(item_id, warehouse_id)` for stock reads.
- [x] Partial unique index on `inventory_warehouse_stock` over `(item_id, warehouse_id, COALESCE(lot_id, -1))` for upsert correctness.

### Dev seed

- `R__seed_01_reset_and_base.sql` gains the six warehouse rows, the eight permissions, and the MAINTENANCE role with its grants.
- `R__seed_02b_pharmacy_from_workbook.sql` (the dev override that pre-stocks 50 of each DRUG via synthetic-legacy lots) writes those quantities into `inventory_warehouse_stock` with `warehouse_id = ADMINISTRACION` so the catalog is consistent with the new model.
- A new `R__seed_04_warehouse_transfers.sql` (optional dev convenience) pre-transfers a small subset (say 10 units of the 50 most-common drugs) from ADMINISTRACION to ENFERMERIA so QA can exercise nurse dispense without first running a manual transfer.

---

## Frontend Changes

### Stores

| Store | Location | Description |
|-------|----------|-------------|
| `useWarehouseStore` | `src/stores/warehouse.ts` | CRUD on warehouses + stock view + transfer list. |
| `useWarehouseTransferStore` | `src/stores/warehouseTransfer.ts` | Create/list transfers. |
| `useWarehouseChargeStore` | `src/stores/warehouseCharge.ts` | Create/list maintenance charges. |

### Components & Views

| Component | Location | Description |
|-----------|----------|-------------|
| `WarehouseList.vue` | `src/views/warehouse/` | Admin warehouse CRUD list. |
| `WarehouseFormDialog.vue` | `src/components/warehouse/` | Create/edit warehouse. |
| `WarehouseStockView.vue` | `src/views/warehouse/` | Per-warehouse catalog + low-stock filter. |
| `TransferFormDialog.vue` | `src/components/warehouse/` | Issue a transfer (source/destination/item/lot/qty). |
| `TransferListView.vue` | `src/views/warehouse/` | Transfer history with filters. |
| `WarehouseChargeDialog.vue` | `src/components/warehouse/` | "Charge this consumable to an admission" maintenance flow. |
| `MaintenanceDashboardView.vue` | `src/views/warehouse/` | MAINTENANCE landing page: assigned warehouses, recent transfers, charge button. |
| `UserWarehousesAssignmentField.vue` | `src/components/users/` | Multi-select shown on user-edit when role includes MAINTENANCE. |

### Routes

| Path | Component | Auth Required | Roles |
|------|-----------|---------------|-------|
| `/warehouses` | `WarehouseList` | Yes | ADMIN |
| `/warehouses/:code/stock` | `WarehouseStockView` | Yes | warehouse:read + scope |
| `/warehouse-transfers` | `TransferListView` | Yes | warehouse-transfer:read |
| `/warehouse-charges` | `MaintenanceDashboardView` | Yes | MAINTENANCE / ADMIN |

### Navigation

- New side-nav section "Bodegas" (Warehouses), visible to any user with `warehouse:read`. Sub-items: My warehouse stock, Transfers, Maintenance dashboard (last one MAINTENANCE-only).
- The existing "Inventory" section stays put — it represents the catalog (single source of truth) and remains ADMIN-only.

### Validation

```ts
// src/validation/warehouse.ts
export const createTransferSchema = z.object({
  sourceWarehouseId: z.number().positive(),
  destinationWarehouseId: z.number().positive(),
  itemId: z.number().positive(),
  lotId: z.number().positive().optional().nullable(),
  quantity: z.number().positive().max(1_000_000),
  notes: z.string().max(2000).optional().nullable(),
}).refine(v => v.sourceWarehouseId !== v.destinationWarehouseId, {
  message: 'validation.transfer.source-equals-destination',
  path: ['destinationWarehouseId'],
})

export const createWarehouseChargeSchema = z.object({
  warehouseId: z.number().positive(),
  itemId: z.number().positive(),
  lotId: z.number().positive().optional().nullable(),
  admissionId: z.number().positive(),
  quantity: z.number().positive(),
  reason: z.string().min(1).max(500),
  notes: z.string().max(2000).optional().nullable(),
})
```

### i18n keys (excerpts)

- `warehouse.title`, `warehouse.code`, `warehouse.name`, `warehouse.active`
- `warehouse.transfer.title`, `warehouse.transfer.source`, `warehouse.transfer.destination`, `warehouse.transfer.quantity`
- `warehouse.charge.title`, `warehouse.charge.reason`, `warehouse.charge.admission`
- `error.warehouse.out-of-stock`, `error.warehouse.transfer.source.denied`, `error.warehouse.not-empty`, `error.warehouse.unassigned`
- `roles.MAINTENANCE`

---

## Implementation Notes

- **Staged cutover.** V119 + V120 are deploy-safe and additive: stock can be written to both legacy columns and the new table during a transitional window if needed. V121 (drop legacy columns) is the irreversible step — ship it only after monitoring confirms no read path falls back to `inventory_items.quantity`. The intermediate state is messy but safer than a single big-bang migration.
- **Service refactor scope.** `InventoryMovementService` becomes warehouse-aware: every EXIT must resolve a warehouse, every ENTRY must specify one. `MedicationAdministrationService` becomes the canonical consumer of `WarehouseScopeService.resolveDispensingWarehouse(user)`. `PharmacyService.fefoPreview` and the FEFO branch of `InventoryMovementService.exit` both accept a `warehouseId` (or fall back to the resolved default).
- **Pattern reference for the MAINTENANCE role**: V114 (RESIDENT_DOCTOR add) for the role shape; the per-user warehouse assignment is novel and warrants a small in-repo design note in `WarehouseScopeService.kt`.
- **Transfer is not just two movements.** Even though the net effect on `inventory_warehouse_stock` is `-q` and `+q`, modeling the transfer as its own aggregate gives us: (a) atomicity in the audit trail ("show me all moves of item X" should not require joining two `inventory_movement` rows by timestamp), (b) a place to land the Phase 2 approval flow without schema churn, (c) cleaner financial reporting later.
- **Charging from a warehouse vs. from a medical order.** A medication administration charges against the medical order's linked inventory item (existing flow). A warehouse-charge is for items that have no medical order — the towel doesn't belong to a doctor's prescription. The two paths converge into `patient_charges` but originate differently. Document this clearly in `docs/features/hospital-billing-system.md` after this ships.
- **Audit log actions.** Add `WAREHOUSE_TRANSFER` and `WAREHOUSE_CHARGE` to the `AuditAction` enum. The audit row is written in `REQUIRES_NEW` so an audit failure does not roll back the underlying stock change (matches V099 pattern).
- **Default-warehouse mapping is data-driven**, not code-driven. The role → default-warehouse mapping in FR-3 / FR-9 is implemented as a small lookup table (`role_default_warehouses(role_id, warehouse_id)`, seeded by V119) so adding a role doesn't require a code change. Maintenance users use `user_warehouses` instead.

---

## QA Checklist

### Backend
- [ ] V119 migration runs against a fresh DB and against a dev DB with V111 catalog data; no row count changes outside the new tables.
- [ ] V120 backfills every `inventory_items.quantity > 0` row and every `inventory_lots.quantity_on_hand > 0` row.
- [ ] V121 drops the two legacy columns; subsequent reads route through the new table.
- [ ] Six warehouses seeded; codes match exactly.
- [ ] MAINTENANCE role exists with the granted permissions.
- [ ] Unit tests for `WarehouseScopeService.resolveDispensingWarehouse` across all role combinations.
- [x] Integration test for AC-4 (nurse dispense blocked when own warehouse is empty). — `WarehouseDispenseIT`
- [x] Integration test for AC-5 (FEFO per warehouse). — `WarehouseDispenseIT`
- [x] Integration test for AC-6 (transfer happy path). — `WarehouseIntegrationTest`
- [x] Integration test for AC-7 / AC-8 (out-of-stock / scope denial). — `WarehouseIntegrationTest`
- [x] Integration test for AC-9 (maintenance charge end-to-end including billing event). — `WarehouseIntegrationTest`
- [x] Integration test for AC-13 (maintenance user scope). — `WarehouseIntegrationTest`
- [x] Integration test for AC-16 (two-transaction overlap, only one succeeds). — `WarehouseDispenseIT`
- [ ] Audit rows for `WAREHOUSE_TRANSFER` / `WAREHOUSE_CHARGE` written in `REQUIRES_NEW`.
- [ ] Detekt passes.
- [ ] OWASP dependency-check passes.

### Frontend
- [ ] WarehouseStockView renders for all roles with `warehouse:read`, scoped to their warehouses.
- [ ] TransferFormDialog respects the role's source-warehouse scope (the source dropdown shows only allowed sources).
- [ ] WarehouseChargeDialog: admission picker filters to admissions the user can see; item picker filters to the source warehouse's stock.
- [ ] MaintenanceDashboardView: MAINTENANCE-only login sees only assigned warehouses.
- [ ] Side-nav "Bodegas" section visibility matches permission grants.
- [ ] ESLint + oxlint pass.
- [ ] i18n keys present in en + es.
- [ ] Vitest unit tests for the three new stores.

### E2E (Playwright)
All four scenarios live in `web/e2e/warehouse.spec.ts` (mock-driven, the repo's E2E convention).
- [x] Admin happy path: create warehouse → transfer to ENFERMERIA → nurse dispenses successfully.
- [x] Nurse out-of-stock path: nurse dispense surfaces the warehouse-scoped error toast naming her warehouse (`ENFERMERIA`) and the dialog stays open.
- [x] Maintenance happy path: log in as MAINTENANCE → charge a consumable to an admission → admission's bill shows the SERVICE line item.
- [x] Scope denials: NURSE cannot issue a transfer (New Transfer hidden) and is redirected from the warehouse admin screen; MAINTENANCE only sees its assigned warehouse.

### General
- [ ] Roles matrix updated.
- [ ] CLAUDE.md "Implemented Features" updated (backend + frontend bullets).
- [ ] Migration entries recorded in [[reference_migrations_guide]].
- [ ] Customer demo: walk through the six warehouses, a transfer, and a maintenance charge.

---

## Documentation Updates Required

### Must Update

- [ ] **CLAUDE.md** — Migration entries for V119–V121; feature bullets for the warehouse module; MAINTENANCE role mention.
- [ ] **docs/roles-functionality-matrix.md** — Add MAINTENANCE column; add Warehouse / Transfer / Charge rows.
- [ ] **docs/roles-functionality-matrix.es.md** — Same in Spanish.
- [ ] **docs/features/inventory-module.md** — One-line note in Revision History pointing here, plus a sentence in Overview clarifying that stock is now per-warehouse.
- [ ] **docs/features/pharmacy-and-inventory-evolution.md** — Note in Revision History that FEFO is now warehouse-scoped (FR-4 amendment).
- [ ] **docs/features/hospital-billing-system.md** — Document the new "warehouse charge → patient charge" pathway.
- [ ] **docs/features/nursing-module.md** — Cross-reference the warehouse-scope error nurses will see on dispense failure.

### Review for Consistency

- [ ] **docs/testing/09-INVENTORY-MANAGEMENT.md** — Add warehouse-scoped scenarios.
- [ ] **docs/testing/10-BILLING-INVOICING.md** — Add warehouse-charge scenario.
- [ ] **docs/testing/11-CROSS-MODULE-SCENARIOS.md** — Nurse dispense blocked by warehouse scope; maintenance charge billed to admission.

### Code Documentation

- [ ] **`api/.../service/WarehouseScopeService.kt`** — KDoc the resolution precedence and the rationale for data-driven defaults.
- [ ] **`api/.../service/WarehouseTransferService.kt`** — KDoc the atomicity rules (single transaction, FOR UPDATE on source row).

---

## Related Docs/Commits/Issues

- Customer feedback (WhatsApp audio, 2026-05-26 22:08): strict warehouse isolation explicitly confirmed by user on 2026-05-28.
- Companion spec: [[nursing-roles-split]] (AUXILIARY_NURSE) — same feedback round.
- Related spec updates: [[medical-psychiatric-record]] v1.6 (psychology execution boundary) and v1.7 (inventory-picker bug fix).
- Related: [`docs/features/inventory-module.md`](inventory-module.md), [`docs/features/pharmacy-and-inventory-evolution.md`](pharmacy-and-inventory-evolution.md), [`docs/features/hospital-billing-system.md`](hospital-billing-system.md).
- Pattern reference: V099 (request-scoped audit in `REQUIRES_NEW` tx), V103/V104 (lot + permissions migration shape), V114 (role-add).

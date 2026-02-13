# Feature: Clinical Event Billing Automation

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-02-12 | @author | Initial draft |
| 1.1 | 2026-02-12 | @author | Clarify `billable` field (was `chargeCreated`), add `DIET` charge type, fix frontend paths, add permission migration details, clarify phase dependencies, add pagination, fix Zod schemas |

---

## Overview

Automatically generate billing charges and inventory deductions when clinical events occur across the hospital — medication administration, psychotherapy sessions, lab orders, procedures, and more. Currently most charges require manual entry, creating delays, missed charges, and billing inaccuracies. This system hooks into existing clinical workflows so that billable actions automatically flow to the patient's account.

---

## Use Case / User Story

1. **As a nurse**, I want to mark a medication dose as administered so that the medication is automatically deducted from inventory and charged to the patient's bill.

2. **As a nurse**, I want to record when a dose is missed, refused, or held so that there is a complete medication administration record without triggering billing.

3. **As a psychologist**, I want my registered psychotherapy activities to automatically generate a billing charge so that sessions are never missed on the patient's bill.

4. **As a doctor**, I want lab orders, referrals, and procedure orders to automatically generate billing charges so that I can focus on patient care instead of billing paperwork.

5. **As an admin staff member**, I want to configure prices on psychotherapy categories, medical order types, and daily meal rates so that auto-generated charges use correct pricing.

6. **As an admin staff member**, I want the system to automatically generate a final room charge and invoice when a patient is discharged so that billing is complete without manual intervention.

7. **As the system**, I want to charge patients daily for meals (like room charges) so that food service costs are captured automatically.

8. **As an admin staff member**, I want all auto-generated charges to be clearly traceable to the clinical event that triggered them so that I can audit and verify billing accuracy.

---

## Authorization / Role Access

| Action | Required Role(s) | Permission | Notes |
|--------|------------------|------------|-------|
| Administer medication (MAR) | NURSE, ADMIN | `medication-administration:create` | Marks dose as given/missed/refused/held |
| View administration records | NURSE, DOCTOR, ADMIN | `medication-administration:read` | All clinical + admin staff |
| Create psychotherapy activity | PSYCHOLOGIST | `psychotherapy-activity:create` | Existing permission — only PSYCHOLOGIST role can create activities (enforced by PsychotherapyActivityService) |
| Configure category prices | ADMIN | `psychotherapy-category:update` | Price field on PsychotherapyCategory |
| Configure service prices | ADMIN | `inventory:update` | Price on inventory items used as service catalog |
| Configure daily meal rate | ADMIN | `billing:configure` | System-level meal rate setting |
| View auto-generated charges | ADMIN, DOCTOR, NURSE | `billing:read` | Existing permission, no change |
| Adjust auto-charges | ADMIN | `billing:adjust` | Existing permission, no change |

**Notes:**
- Automated billing hooks run at system level — triggered by existing clinical workflow permissions (e.g., a psychologist creating an activity triggers billing without needing `billing:create`).
- Medical order billing hooks fire when doctors create orders using their existing `medical-order:create` permission.
- The daily diet scheduler runs as a system process with no user permission required.
- Only users with the `PSYCHOLOGIST` role can create psychotherapy activities (existing enforcement in `PsychotherapyActivityService`). The billing hook fires as a consequence of activity creation — no additional billing permission needed.

---

## Functional Requirements

### Phase Dependencies

> **Important**: Phase 1 (MAR) requires the `inventory_item_id` FK on `medical_orders` (V065) to link medication orders to inventory items. This migration must ship with Phase 1 even though it is also used by Phase 3. The doctor or admin sets the inventory item link when creating/editing the order; the nurse then uses it during medication administration.

### Phase 1: Medication Administration Record (MAR)

- New `MedicationAdministration` entity tracks individual dose delivery linked to a `MedicalOrder` (category `MEDICAMENTOS`).
- Supported statuses: `GIVEN`, `MISSED`, `REFUSED`, `HELD`.
- When status is `GIVEN`:
  - Create an inventory EXIT movement for the linked medication (deducts stock).
  - The existing `InventoryDispensedEvent` fires → `BillingEventListener` creates a `MEDICATION` charge.
- When status is `MISSED`, `REFUSED`, or `HELD`:
  - No inventory deduction or billing charge occurs.
  - Record is saved for compliance and audit purposes.
- A `MedicalOrder` must have a linked `InventoryItem` (via new `inventoryItem` FK added in V065) for auto-dispensing to work.
- Nurses can only administer medications for ACTIVE orders on ACTIVE admissions.
- Each administration record is immutable (append-only, no edits).
- The response includes a `billable` boolean indicating whether a billing charge will be generated (deterministic based on status and inventory dispensing — see [Billable Field](#billable-field) in Implementation Notes).

### Phase 2: Psychotherapy Activity → Billing

- Add `price` (BigDecimal, nullable) and `cost` (BigDecimal, nullable) fields to `PsychotherapyCategory`.
- When a `PsychotherapyActivity` is created and the category has a non-null, non-zero price:
  - Publish a `PsychotherapyActivityCreatedEvent`.
  - `BillingEventListener` creates a `SERVICE` charge with the category price.
- When the category has no price configured, no charge is created.
- **Role restriction**: Only users with `PSYCHOLOGIST` role can create activities (existing enforcement). The billing hook fires as a consequence — no separate billing permission is needed.

### Phase 3: Medical Order → Billing

- The `inventoryItem` FK on `MedicalOrder` (added in V065, shipped with Phase 1) is now used for automatic billing.
- When a `MedicalOrder` is created with one of the following categories AND has a linked inventory item:
  - `LABORATORIOS` → creates `LAB` charge
  - `CUIDADOS_ESPECIALES` → creates `PROCEDURE` charge
  - `REFERENCIAS_MEDICAS` → creates `SERVICE` charge
  - `PRUEBAS_PSICOMETRICAS` → creates `SERVICE` charge
  - `ACTIVIDAD_FISICA` → creates `SERVICE` charge
- Publish a `MedicalOrderCreatedEvent` with category, admission, and pricing info.
- `BillingEventListener` maps the category to the appropriate `ChargeType` and creates the charge.
- Orders without a linked inventory item generate no automatic charge (log info message).

### Phase 4: Procedure Admission Types → Billing + Inventory

- When an admission of type `ELECTROSHOCK_THERAPY` or `KETAMINE_INFUSION` is created:
  - Publish an `AdmissionCreatedEvent` with the admission type.
  - `BillingEventListener` creates a `PROCEDURE` charge using a configurable base price.
- Optional: define "supply packages" (future enhancement) — a list of inventory items auto-dispensed per procedure type.
- **Configuration note**: Base prices (`electroshock-base-price`, `ketamine-base-price`) are set in `application.yml` and require a redeploy to change. There is no admin UI for these values. If runtime configurability is needed later, migrate these to a database-backed settings table.

### Phase 5: Daily Diet Charges

- Add a system configuration for daily meal rate (e.g., `app.billing.daily-meal-rate` in `application.yml`).
- Add `DIET` to the `ChargeType` enum (see [ChargeType Enum Change](#chargetype-enum-change)).
- Extend `DailyChargeScheduler` to generate `DIET` charges for meals alongside existing `ROOM` charges.
- Only for ACTIVE admissions with type `HOSPITALIZATION`.
- Same idempotency pattern as room charges: dual-layer protection with application-level existence check + unique partial index on `(admission_id, charge_type, charge_date) WHERE charge_type = 'DIET'`.
- Skip if no meal rate is configured.

### Phase 6: Discharge → Auto-Invoice

- Enhance `PatientDischargedEvent` handler in `BillingEventListener`:
  - Create final-day room charge (for discharge date, preventing gap from scheduler).
  - Create final-day diet charge (if applicable, using `ChargeType.DIET`).
  - Auto-generate invoice with all unbilled charges.
- If invoice generation fails, log error but do not block the discharge.

---

## Acceptance Criteria / Scenarios

### Medication Administration Record (MAR)

- When a nurse marks a medication dose as GIVEN for an active order, the medication is deducted from inventory and a MEDICATION charge appears on the patient's bill.
- When a nurse marks a dose as MISSED, REFUSED, or HELD, no inventory deduction or billing charge occurs and the record is saved.
- When the medication is out of stock, return 400 Bad Request and do not record the administration as GIVEN.
- When the medical order is DISCONTINUED, return 400 Bad Request preventing new administrations.
- When the admission is DISCHARGED, return 400 Bad Request preventing new administrations.
- When a nurse tries to administer a medication from another admission's order, return 403 Forbidden.
- When the medical order has no linked inventory item, return 400 Bad Request with a message indicating the order must be linked to an inventory item first.
- When billing charge creation fails after a GIVEN administration, the inventory deduction and MAR record are still saved (AFTER_COMMIT isolation).
- The response `billable` field is `true` when status is GIVEN and inventory was successfully dispensed; `false` for all other statuses.

### Psychotherapy Activity → Billing

- When a psychologist creates an activity with a priced category (e.g., Q500), a SERVICE charge of Q500 is auto-created on the patient's bill.
- When the category has no price (null) or price is zero, no charge is created and the activity is saved normally.
- When billing charge creation fails, the psychotherapy activity is still saved successfully.
- When a non-PSYCHOLOGIST user attempts to create an activity, return 403 Forbidden (existing behavior, unchanged).

### Lab/Procedure/Referral Orders → Billing

- When a doctor creates a LABORATORIOS order linked to a lab inventory item (e.g., "Complete Blood Count" at Q150), a LAB charge of Q150 is auto-created.
- When a doctor creates a CUIDADOS_ESPECIALES order linked to an inventory item, a PROCEDURE charge is auto-created.
- When a doctor creates a REFERENCIAS_MEDICAS order linked to an inventory item, a SERVICE charge is auto-created.
- When an order has no linked inventory item, no charge is created and an info-level log message is recorded.
- When the order is later DISCONTINUED, no automatic refund occurs (admin uses existing ADJUSTMENT flow).
- Duplicate charges are prevented — creating the same order type twice for the same admission does not create duplicate charges (event only fires on creation).

### Electroshock/Ketamine Procedures

- When an admission of type ELECTROSHOCK_THERAPY is created, a PROCEDURE charge is auto-created using the configured base price.
- When an admission of type KETAMINE_INFUSION is created, a PROCEDURE charge is auto-created using the configured base price.
- When no base price is configured for the procedure type, no charge is created and a warning is logged.
- Other admission types (HOSPITALIZATION, AMBULATORY, EMERGENCY) do not trigger procedure charges.

### Daily Diet Charges

- The scheduler creates a daily `DIET` charge (description: "Daily Meals") for all active hospitalized admissions with the configured meal rate.
- Running the scheduler twice for the same date produces no duplicate charges (dual-layer idempotency: application check + unique index).
- Non-hospitalization admissions are excluded from diet charges.
- When no meal rate is configured, no diet charges are generated.

### Discharge → Auto-Invoice

- When a patient is discharged, a final-day room charge is created for the discharge date.
- When a patient is discharged, a final-day diet charge is created for the discharge date (if applicable).
- When a patient is discharged, an invoice is auto-generated with all unbilled charges.
- When a final-day room charge already exists (scheduler ran earlier that day), no duplicate is created.
- When a final-day diet charge already exists, no duplicate is created (same idempotency as room charges).
- When the admission has no charges, an empty invoice is still generated.
- When invoice generation fails, the discharge still completes and an error is logged.

### General / Cross-Cutting

- All auto-generated charges appear in the existing charge list and balance views without UI changes to those views.
- Billing failures never roll back the triggering clinical action (AFTER_COMMIT + REQUIRES_NEW pattern).
- Admin can create ADJUSTMENT charges to correct any auto-generated charge using the existing adjustment flow.
- All auto-generated charges have descriptive `description` fields traceable to the source event.
- The new `DIET` charge type appears in the charge type filter dropdown and is styled with a Tag in the charge list (same pattern as existing charge types).

---

## Non-Functional Requirements

- **Idempotency**: All scheduled charges use unique indexes or existence checks to prevent duplicate charges (matching existing room charge pattern).
- **Transaction Isolation**: Billing failures must never roll back clinical actions. All event listeners use `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` with `@Transactional(propagation = Propagation.REQUIRES_NEW)`.
- **Audit Trail**: All auto-generated charges are traceable to the triggering clinical event. JPA auditing tracks `createdBy` and `createdAt` on every charge.
- **Permission-Based Pricing Configuration**: Only ADMIN role can set/modify prices on categories, inventory items, and system configuration.
- **Performance**: Event-driven charge creation adds negligible latency to clinical workflows since it runs after the originating transaction commits in a separate thread.

---

## API Contract

### Phase 1: Medication Administration Record (MAR)

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| GET | `/api/v1/admissions/{admissionId}/medical-orders/{orderId}/administrations?page=0&size=20` | - | `Page<MedicationAdministrationResponse>` | Yes | List administrations for a medical order (paginated) |
| POST | `/api/v1/admissions/{admissionId}/medical-orders/{orderId}/administrations` | `CreateMedicationAdministrationRequest` | `MedicationAdministrationResponse` | Yes | Record a medication administration |

> **Note**: The GET endpoint returns a Spring `Page` response with standard pagination fields (`content`, `totalElements`, `totalPages`, `number`, `size`). Default page size is 20, sorted by `administeredAt` descending.

### Phase 2: Psychotherapy Category Pricing

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| PUT | `/api/v1/psychotherapy-categories/{id}` | `UpdatePsychotherapyCategoryRequest` | `PsychotherapyCategoryResponse` | Yes | Update category (now includes price/cost) |

> **Note**: The existing update endpoint is extended — no new endpoint needed.

### Phase 3: Medical Order Inventory Link

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| POST | `/api/v1/admissions/{admissionId}/medical-orders` | `CreateMedicalOrderRequest` | `MedicalOrderResponse` | Yes | Create order (now includes optional inventoryItemId) |

> **Note**: The existing create endpoint is extended — no new endpoint needed.

### Request/Response Examples

```json
// POST /api/v1/admissions/42/medical-orders/7/administrations - GIVEN dose
// Request
{
  "status": "GIVEN",
  "notes": "Patient tolerated well, no adverse reactions"
}

// Response (201 Created)
{
  "id": 1,
  "medicalOrderId": 7,
  "admissionId": 42,
  "medication": "Sertraline",
  "dosage": "50mg",
  "route": "ORAL",
  "status": "GIVEN",
  "notes": "Patient tolerated well, no adverse reactions",
  "administeredAt": "2026-02-12T08:15:00",
  "administeredByName": "Nurse Maria Garcia",
  "inventoryItemName": "Sertraline 50mg tablets",
  "billable": true
}
```

```json
// POST /api/v1/admissions/42/medical-orders/7/administrations - Refused dose
// Request
{
  "status": "REFUSED",
  "notes": "Patient refused medication, complained of nausea"
}

// Response (201 Created)
{
  "id": 2,
  "medicalOrderId": 7,
  "admissionId": 42,
  "medication": "Sertraline",
  "dosage": "50mg",
  "route": "ORAL",
  "status": "REFUSED",
  "notes": "Patient refused medication, complained of nausea",
  "administeredAt": "2026-02-12T20:00:00",
  "administeredByName": "Nurse Maria Garcia",
  "inventoryItemName": "Sertraline 50mg tablets",
  "billable": false
}
```

> **Note on `inventoryItemName`**: This field is always populated from the `MedicalOrder`'s linked inventory item, regardless of administration status. It shows which medication the order is linked to. It is `null` only when the order has no linked inventory item (which would prevent GIVEN administrations).

> **Note on `billable`**: This field is computed deterministically at response time. It is `true` when `status == GIVEN` and the inventory EXIT movement succeeded. It does **not** confirm the billing charge was created — billing runs asynchronously via `@TransactionalEventListener(AFTER_COMMIT)`. In practice, if `billable` is `true`, the charge will be created unless the billing listener encounters an unexpected error (which is caught and logged).

```json
// Error responses follow the existing pattern:
// 400 Bad Request - business rule violation
{
  "success": false,
  "message": "Medical order must be linked to an inventory item before medication can be administered",
  "data": null
}

// 400 Bad Request - out of stock
{
  "success": false,
  "message": "Insufficient stock for Sertraline 50mg tablets",
  "data": null
}

// 400 Bad Request - discontinued order
{
  "success": false,
  "message": "Cannot administer medication for a discontinued order",
  "data": null
}

// 403 Forbidden - wrong admission
{
  "success": false,
  "message": "Access denied",
  "data": null
}
```

```json
// PUT /api/v1/psychotherapy-categories/3 - Add pricing
{
  "name": "Individual Session",
  "description": "One-on-one psychotherapy session",
  "displayOrder": 2,
  "active": true,
  "price": 500.00,
  "cost": 200.00
}
```

```json
// POST /api/v1/admissions/42/medical-orders - With inventory link
{
  "category": "LABORATORIOS",
  "startDate": "2026-02-12",
  "observations": "Fasting required",
  "inventoryItemId": 15
}
```

---

## Database Changes

### New Entities

| Entity | Table | Extends | Description |
|--------|-------|---------|-------------|
| `MedicationAdministration` | `medication_administrations` | `BaseEntity` | Individual dose administration records |

### Modified Entities

| Entity | Table | Changes |
|--------|-------|---------|
| `PsychotherapyCategory` | `psychotherapy_categories` | Add `price` and `cost` columns |
| `MedicalOrder` | `medical_orders` | Add `inventory_item_id` FK |
| `PatientCharge` | `patient_charges` | Add `DIET` to `ChargeType` enum (Kotlin only — no migration needed) |

### ChargeType Enum Change

Add `DIET` to the `ChargeType` enum in Kotlin:

```kotlin
enum class ChargeType {
    MEDICATION,
    ROOM,
    PROCEDURE,
    LAB,
    SERVICE,
    DIET,        // NEW — daily meal charges
    ADJUSTMENT,
}
```

> **No migration needed**: `charge_type` is stored as `VARCHAR` with `@Enumerated(EnumType.STRING)` and has no database-level CHECK constraint. Adding a new enum value to the Kotlin enum is sufficient.

### New Migrations

| Migration | Description | Ships With |
|-----------|-------------|------------|
| `V063__create_medication_administrations_table.sql` | Creates medication_administrations table with all BaseEntity fields | Phase 1 |
| `V064__add_price_to_psychotherapy_categories.sql` | Adds price and cost columns to psychotherapy_categories | Phase 2 |
| `V065__add_inventory_item_to_medical_orders.sql` | Adds inventory_item_id FK to medical_orders | Phase 1 (prerequisite for MAR) |
| `V066__add_medication_administration_permissions.sql` | Seeds medication-administration permissions (see details below) | Phase 1 |
| `V067__add_billing_configure_permission.sql` | Seeds billing:configure permission (see details below) | Phase 5 |
| `V068__add_diet_charge_unique_index.sql` | Adds unique partial index for daily diet charges | Phase 5 |

> **Deployment note**: V065 must be deployed with Phase 1 (not Phase 3). Phase 1 (MAR) requires the `inventory_item_id` FK to exist on `medical_orders` so nurses can administer linked medications. Phase 3 then adds the billing event logic that uses the same FK.

### Schema: medication_administrations

```sql
CREATE TABLE medication_administrations (
    id BIGSERIAL PRIMARY KEY,
    medical_order_id BIGINT NOT NULL REFERENCES medical_orders(id),
    admission_id BIGINT NOT NULL REFERENCES admissions(id),
    status VARCHAR(20) NOT NULL,
    notes VARCHAR(1000),
    administered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP,
    CONSTRAINT chk_medication_administration_status
        CHECK (status IN ('GIVEN', 'MISSED', 'REFUSED', 'HELD'))
);

CREATE INDEX idx_medication_administrations_deleted_at ON medication_administrations(deleted_at);
CREATE INDEX idx_medication_administrations_medical_order_id ON medication_administrations(medical_order_id);
CREATE INDEX idx_medication_administrations_admission_id ON medication_administrations(admission_id);
CREATE INDEX idx_medication_administrations_status ON medication_administrations(status);
CREATE INDEX idx_medication_administrations_administered_at ON medication_administrations(administered_at);
```

### Schema: psychotherapy_categories changes

```sql
ALTER TABLE psychotherapy_categories
    ADD COLUMN price DECIMAL(12, 2),
    ADD COLUMN cost DECIMAL(12, 2);
```

### Schema: medical_orders changes

```sql
ALTER TABLE medical_orders
    ADD COLUMN inventory_item_id BIGINT REFERENCES inventory_items(id);

CREATE INDEX idx_medical_orders_inventory_item_id ON medical_orders(inventory_item_id);
```

### Schema: diet charge idempotency index

```sql
-- Unique partial index for daily diet charges (same pattern as room charges)
CREATE UNIQUE INDEX idx_patient_charges_daily_diet_unique
    ON patient_charges (admission_id, charge_type, charge_date)
    WHERE charge_type = 'DIET'
    AND deleted_at IS NULL;
```

### V066: Medication Administration Permissions (detailed)

```sql
-- Step 1: Insert permissions
INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('medication-administration:create', 'Create Medication Administration', 'Record a medication administration', 'medication-administration', 'create', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('medication-administration:read', 'View Medication Administrations', 'View medication administration records', 'medication-administration', 'read', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Step 2: ADMIN gets full access
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMIN' AND p.resource = 'medication-administration';

-- Step 3: NURSE gets create + read
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'NURSE'
AND p.code IN ('medication-administration:create', 'medication-administration:read');

-- Step 4: CHIEF_NURSE gets create + read
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'CHIEF_NURSE'
AND p.code IN ('medication-administration:create', 'medication-administration:read');

-- Step 5: DOCTOR gets read only
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'DOCTOR'
AND p.code = 'medication-administration:read';
```

### V067: Billing Configure Permission (detailed)

```sql
-- Step 1: Insert permission
INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('billing:configure', 'Configure Billing Settings', 'Configure system-level billing settings', 'billing', 'configure', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Step 2: ADMIN only
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMIN' AND p.code = 'billing:configure';
```

### Index Requirements

- [x] `deleted_at` — Required for soft delete queries
- [x] Foreign keys — `medical_order_id`, `admission_id`, `inventory_item_id`
- [x] `status` — Frequently filtered (GIVEN vs other statuses)
- [x] `administered_at` — For chronological queries and reporting
- [x] Diet charge unique index — Idempotency for daily diet charges (uses `ChargeType.DIET`, no string matching)

---

## New Events

| Event | Published By | Handled By | Trigger |
|-------|-------------|------------|---------|
| `PsychotherapyActivityCreatedEvent` | `PsychotherapyActivityService` | `BillingEventListener` | Activity created with priced category |
| `MedicalOrderCreatedEvent` | `MedicalOrderService` | `BillingEventListener` | Order created with linked inventory item |
| `AdmissionCreatedEvent` | `AdmissionService` | `BillingEventListener` | Admission of type ELECTROSHOCK_THERAPY or KETAMINE_INFUSION |

### Event Definitions

```kotlin
// PsychotherapyActivityCreatedEvent
data class PsychotherapyActivityCreatedEvent(
    val admissionId: Long,
    val categoryName: String,
    val price: BigDecimal,
)

// MedicalOrderCreatedEvent
data class MedicalOrderCreatedEvent(
    val admissionId: Long,
    val category: MedicalOrderCategory,
    val inventoryItemId: Long,
    val itemName: String,
    val unitPrice: BigDecimal,
)

// AdmissionCreatedEvent
data class AdmissionCreatedEvent(
    val admissionId: Long,
    val admissionType: AdmissionType,
)
```

---

## Frontend Changes

### Components

| Component | Location | Description |
|-----------|----------|-------------|
| `MedicationAdministrationDialog.vue` | `src/components/medical-record/` | Dialog to record administration (status + notes) |
| `MedicationAdministrationHistory.vue` | `src/components/medical-record/` | Paginated list of administrations for a medical order |
| `MedicationAdministrationBadge.vue` | `src/components/medical-record/` | Status badge (GIVEN/MISSED/REFUSED/HELD) |

> **Note on component paths**: All MAR components live in `src/components/medical-record/` alongside existing medical order components (`MedicalOrderList.vue`, `MedicalOrderCard.vue`, `MedicalOrderFormDialog.vue`).

> **Note on psychotherapy category pricing**: No new component needed. Extend the existing `PsychotherapyCategoryFormView.vue` (`src/views/admin/`) to add `price` and `cost` fields to the form. Extend the existing `PsychotherapyCategoriesView.vue` to show a price column in the DataTable.

### Pinia Stores

| Store | Location | Description |
|-------|----------|-------------|
| `useMedicationAdministrationStore` | `src/stores/medicationAdministration.ts` | State management for MAR records |

### Routes

No new routes needed. MAR is accessed from within the existing medical order accordion in `MedicalRecordTabs`. Specifically, `MedicalOrderCard.vue` will gain an "Administer" button for `MEDICAMENTOS` orders, which opens `MedicationAdministrationDialog.vue`. The administration history is shown inline below each medication order card.

### Validation (Zod Schemas)

```typescript
// src/validation/medicationAdministration.ts
import { z } from 'zod'

export const createMedicationAdministrationSchema = z.object({
  status: z.enum(['GIVEN', 'MISSED', 'REFUSED', 'HELD'], {
    required_error: 'validation.medicationAdministration.status.required',
    invalid_type_error: 'validation.medicationAdministration.status.required',
  }),
  notes: z
    .string()
    .max(1000, 'validation.medicationAdministration.notes.max')
    .optional()
    .or(z.literal('')),
})

export type MedicationAdministrationFormData = z.infer<typeof createMedicationAdministrationSchema>
```

```typescript
// Updated: src/validation/psychotherapy.ts
// Add price and cost to existing psychotherapyCategorySchema
export const psychotherapyCategorySchema = z.object({
  name: z
    .string()
    .min(1, 'validation.psychotherapy.category.name.required')
    .max(100, 'validation.psychotherapy.category.name.max'),
  description: z
    .string()
    .max(255, 'validation.psychotherapy.category.description.max')
    .optional()
    .or(z.literal('')),
  displayOrder: z
    .number({ invalid_type_error: 'validation.psychotherapy.category.displayOrder.invalid' })
    .int('validation.psychotherapy.category.displayOrder.integer')
    .min(0, 'validation.psychotherapy.category.displayOrder.min')
    .default(0),
  active: z.boolean().default(true),
  price: z.number().min(0, 'validation.psychotherapy.category.price.min').optional().nullable(),
  cost: z.number().min(0, 'validation.psychotherapy.category.cost.min').optional().nullable(),
})
```

```typescript
// Updated: src/validation/medicalRecord.ts
// Add inventoryItemId to existing medicalOrderSchema
export const medicalOrderSchema = z.object({
  // ... existing fields ...
  inventoryItemId: z.number().int().positive().optional().nullable(),
})
```

> **Note on Zod conventions**: All optional string fields use `.optional().or(z.literal(''))` to handle empty strings from form inputs. Validation error messages use i18n keys (not hardcoded English). Schemas live in `src/validation/`, not `src/schemas/`.

### UI Changes to Existing Views

| View | Change |
|------|--------|
| `MedicalOrderCard.vue` | Add "Administer" button for MEDICAMENTOS orders with linked inventory item; show `MedicationAdministrationHistory` inline |
| `MedicalOrderFormDialog.vue` | Add optional inventory item dropdown (Select component) for billable categories (LABORATORIOS, CUIDADOS_ESPECIALES, REFERENCIAS_MEDICAS, PRUEBAS_PSICOMETRICAS, ACTIVIDAD_FISICA, MEDICAMENTOS) |
| `PsychotherapyCategoryFormView.vue` | Add price (InputNumber, currency format) and cost (InputNumber, currency format) fields |
| `PsychotherapyCategoriesView.vue` | Add price column to DataTable |
| `AdmissionCharges.vue` | Add `DIET` to the charge type filter dropdown; add Tag severity for DIET type |
| Charge list / Balance view | No other changes — auto-charges appear alongside manual charges |

### i18n Keys to Add

```json
// Add to both en.json and es.json
{
  "medicationAdministration": {
    "title": "Medication Administration",
    "administer": "Administer",
    "history": "Administration History",
    "status": "Status",
    "notes": "Notes",
    "statuses": {
      "GIVEN": "Given",
      "MISSED": "Missed",
      "REFUSED": "Refused",
      "HELD": "Held"
    },
    "billable": "Billable",
    "created": "Medication administration recorded successfully.",
    "confirmGiven": "Confirm medication was given to the patient?"
  },
  "billing": {
    "chargeTypes": {
      "DIET": "Diet"
    }
  },
  "psychotherapy": {
    "category": {
      "price": "Price",
      "cost": "Cost"
    }
  },
  "validation": {
    "medicationAdministration": {
      "status": { "required": "Status is required" },
      "notes": { "max": "Notes must be at most 1000 characters" }
    },
    "psychotherapy": {
      "category": {
        "price": { "min": "Price cannot be negative" },
        "cost": { "min": "Cost cannot be negative" }
      }
    }
  }
}
```

---

## Implementation Notes

### Follow Existing Patterns
- All new events follow the `InventoryDispensedEvent` → `BillingEventListener` pattern exactly.
- All event listeners use `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` with `@Transactional(propagation = Propagation.REQUIRES_NEW)`.
- All failures in event handlers are caught and logged, never propagated.

### Billable Field

The `billable` field in `MedicationAdministrationResponse` is computed **deterministically at response time** — it does not wait for the async billing event. The logic is:

```kotlin
val billable = (request.status == AdministrationStatus.GIVEN)
// If status is GIVEN and we reach this point, the inventory EXIT movement
// succeeded (otherwise createMovement() would have thrown). The existing
// InventoryDispensedEvent will fire AFTER_COMMIT and create the charge.
```

This is reliable because: if the inventory movement succeeded, the `InventoryDispensedEvent` **will** fire and the billing listener **will** create the charge. The only failure case is an unexpected exception in the listener, which is caught and logged. The field tells the client "this administration will produce a billing charge" rather than "a charge was created."

### MAR + Inventory Integration
- The MAR `GIVEN` flow reuses `InventoryItemService.createMovement()` internally, which already publishes `InventoryDispensedEvent`. This means the billing charge is created by the existing listener — no new billing code needed for MAR.
- The `MedicalOrder` must have a linked `inventoryItem` (V065 FK) for the nurse to administer. This is set by the doctor when creating the order (via `inventoryItemId` in the create request), or by admin afterwards via the order edit form.

### Configuration
```yaml
# application.yml additions
app:
  billing:
    daily-meal-rate: 150.00        # Set to null or 0 to disable
    electroshock-base-price: 2500.00
    ketamine-base-price: 3000.00
```

> **Note**: These values require a redeploy to change. There is no admin UI or API endpoint for runtime configuration. If runtime configurability is needed, these should be migrated to a database-backed settings table with a `billing:configure` permission-gated API.

### ChargeType Enum Change

Add `DIET` to the existing `ChargeType` enum. This gives diet charges the same structural idempotency as room charges — the unique partial index filters on `charge_type = 'DIET'` instead of matching a description string.

### Category → ChargeType Mapping
```kotlin
// In BillingEventListener
fun mapCategoryToChargeType(category: MedicalOrderCategory): ChargeType = when (category) {
    MedicalOrderCategory.LABORATORIOS -> ChargeType.LAB
    MedicalOrderCategory.CUIDADOS_ESPECIALES -> ChargeType.PROCEDURE
    MedicalOrderCategory.REFERENCIAS_MEDICAS -> ChargeType.SERVICE
    MedicalOrderCategory.PRUEBAS_PSICOMETRICAS -> ChargeType.SERVICE
    MedicalOrderCategory.ACTIVIDAD_FISICA -> ChargeType.SERVICE
    else -> ChargeType.SERVICE
}
```

### Phased Implementation Order
1. **Phase 1 — MAR** (+ V065 inventory link): Highest value; unlocks nurse medication workflow + auto-billing + auto-inventory
2. **Phase 2 — Psychotherapy Billing**: Low complexity; add price to category + one new event
3. **Phase 3 — Medical Order Billing**: Medium complexity; add event publishing for orders with inventory link (FK already exists from Phase 1)
4. **Phase 4 — Procedure Admissions**: Medium complexity; add admission event + config
5. **Phase 5 — Daily Diet Charges**: Low complexity; add `DIET` charge type + extend existing scheduler
6. **Phase 6 — Discharge Auto-Invoice**: Low complexity; enhance existing event handler

---

## QA Checklist

### Backend
- [ ] All functional requirements implemented per phase
- [ ] `MedicationAdministration` entity extends `BaseEntity`
- [ ] `MedicationAdministration` entity has `@SQLRestriction("deleted_at IS NULL")`
- [ ] `DIET` added to `ChargeType` enum
- [ ] DTOs used in all controllers (no entity exposure)
- [ ] Input validation on all new endpoints
- [ ] Idempotency verified for all scheduled charges (diet uses same dual-layer pattern as room)
- [ ] Event-driven charge creation verified for all event types
- [ ] Transaction isolation verified (billing failure doesn't roll back clinical action)
- [ ] Inventory deduction verified for MAR GIVEN status
- [ ] No inventory deduction for MAR MISSED/REFUSED/HELD statuses
- [ ] Out-of-stock scenario handled gracefully for MAR
- [ ] `billable` field correctly computed in MAR response
- [ ] MAR GET endpoint returns paginated results
- [ ] V066 permissions seeded correctly for ADMIN, NURSE, CHIEF_NURSE, DOCTOR
- [ ] V067 billing:configure permission seeded for ADMIN only
- [ ] Unit tests written and passing for all new services
- [ ] Integration tests written and passing (Testcontainers) for event flows
- [ ] Detekt passes (no new violations)
- [ ] OWASP dependency-check passes

### Frontend
- [ ] MedicationAdministrationDialog functional with status selection
- [ ] Administration history displays correctly per medical order (paginated)
- [ ] Psychotherapy category price/cost fields added to existing `PsychotherapyCategoryFormView.vue`
- [ ] Psychotherapy category list shows price column in existing `PsychotherapyCategoriesView.vue`
- [ ] Medical order inventory item dropdown functional in existing `MedicalOrderFormDialog.vue`
- [ ] Pinia store for medication administrations implemented
- [ ] Form validation with VeeValidate + Zod (using `.optional().or(z.literal(''))` pattern)
- [ ] Error handling for out-of-stock and other failure scenarios
- [ ] `DIET` charge type added to filter dropdown and Tag severity in `AdmissionCharges.vue`
- [ ] ESLint/oxlint passes
- [ ] i18n keys added for all user-facing text (es + en)
- [ ] Unit tests written and passing (Vitest)

### E2E Tests (Playwright)
- [ ] Nurse administers medication → inventory deducted + charge created
- [ ] Nurse records refused dose → no inventory or billing impact
- [ ] Psychologist creates activity with priced category → charge created
- [ ] Non-psychologist cannot create activity → 403 Forbidden
- [ ] Doctor creates lab order with inventory link → charge created
- [ ] Discharge triggers auto-invoice
- [ ] Permission/authorization flows tested (allowed and denied)
- [ ] Form validation errors displayed correctly
- [ ] Out-of-stock error handled in UI

### General
- [ ] API contract documented and verified
- [ ] All database migrations tested (forward-only — no down migrations in this project)
- [ ] Feature documentation updated
- [ ] Reviewed by project owner

---

## Documentation Updates Required

### Must Update

- [ ] **[CLAUDE.md](../../CLAUDE.md)**
  - Add "Clinical Event Billing Automation" to Implemented Features
  - Update migration range (V063-V068)
  - Add MAR to implemented backend features
  - Add `DIET` to ChargeType enum documentation
  - Add MAR UI to implemented frontend features
- [ ] **[ARCHITECTURE.md](../architecture/ARCHITECTURE.md)**
  - Document new event types and their flows
  - Update event-driven architecture section

### Review for Consistency

- [ ] **[hospital-billing-system.md](hospital-billing-system.md)**
  - Reference this feature as an extension of the billing system
  - Document `DIET` charge type addition
- [ ] **[nursing-module.md](nursing-module.md)**
  - Reference MAR as a new nursing capability
- [ ] **[psychotherapeutic-activities.md](psychotherapeutic-activities.md)**
  - Reference auto-billing from activities

### Code Documentation

- [ ] **`MedicationAdministration.kt`** — Document status enum behavior
- [ ] **`MedicationAdministrationService.kt`** — Document GIVEN flow (inventory + billing), `billable` field computation
- [ ] **`BillingEventListener.kt`** — Document new event handlers
- [ ] **`DailyChargeScheduler.kt`** — Document diet charge generation
- [ ] **`ChargeType.kt`** — Document new `DIET` value

---

## Related Docs/Commits/Issues

- Related feature: [Hospital Billing System](hospital-billing-system.md)
- Related feature: [Nursing Module](nursing-module.md)
- Related feature: [Psychotherapeutic Activities](psychotherapeutic-activities.md)
- Related feature: [Inventory Module](inventory-module.md)
- Related feature: [Medical/Psychiatric Record](medical-psychiatric-record.md)
- PR #21: Hospital billing system with charge capture and invoicing
- PR #20: Inventory management module
- PR #18: Nursing module
- PR #17: Psychotherapeutic activities module

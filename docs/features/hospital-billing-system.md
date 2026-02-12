# Feature: Hospital Billing System

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-02-07 | @author | Initial draft |

---

## Overview

A hybrid billing system that combines real-time charge capture for ad-hoc services (medications, procedures, lab tests) with scheduled daily processes for recurring charges (room rates), providing patients with transparent daily balance visibility and consolidated invoice generation at discharge.

---

## Use Case / User Story

1. **As a billing/admin staff member**, I want to view a patient's daily charge breakdown and running balance so that I can provide transparent billing information at any time during their stay.

2. **As a clinical staff member**, I want charges to be automatically captured when I dispense medications or record procedures so that billable events aren't missed or require manual entry.

3. **As a billing/admin staff member**, I want to manually create charges for ad-hoc services so that I can bill for items not captured through automated workflows.

4. **As a billing/admin staff member**, I want to apply adjustments and credits with a documented reason so that billing errors can be corrected while maintaining an audit trail.

5. **As a billing/admin staff member**, I want to generate a consolidated invoice when a patient is discharged so that the patient receives a complete final bill.

6. **As an administrator**, I want daily recurring charges (room rates) to be automatically generated for all admitted patients so that routine charges don't require manual intervention.

---

## Authorization / Role Access

| Action | Required Role(s) | Permission | Notes |
|--------|------------------|------------|-------|
| View charges & balances | ADMIN, DOCTOR, NURSE | `billing:read` | All clinical + admin staff |
| Create manual charges | ADMIN | `billing:create` | Admin staff only |
| Create adjustments/credits | ADMIN | `billing:adjust` | Requires reason field |
| View invoices | ADMIN | `invoice:read` | Admin staff only |
| Generate invoices | ADMIN | `invoice:create` | At discharge only |

**Notes:**
- Automated charge capture (from inventory EXIT movements) runs at system level — triggered by existing clinical workflow permissions.
- The daily scheduled job runs as a system process with no user permission required.
- Clinical staff (DOCTOR, NURSE) have read-only visibility into charges for their patients.

---

## Functional Requirements

### Real-Time Charge Capture
- Automatically create a `PatientCharge` when an inventory EXIT movement is created with an associated admission ID.
- Support manual charge creation for ad-hoc services not captured through automated workflows.
- Track charge type via enum: `MEDICATION`, `ROOM`, `PROCEDURE`, `LAB`, `SERVICE`, `ADJUSTMENT`.
- Record quantity, unit price, and computed total amount per charge.
- Link every charge to an admission; optionally reference source inventory item or room.
- Charges are immutable once created — corrections are handled via adjustment charges only.

### Scheduled Daily Charges
- A `@Scheduled` cron job runs daily at 1:00 AM to generate recurring charges for all ACTIVE admissions.
- Room daily rates: use the room's `price` field from the `Room` entity assigned to the admission.
- Idempotent execution: use a date + admission + charge type composite check to prevent duplicates.
- Skip admissions with no room assigned or rooms with no price configured (log a warning).
- Only process admissions with status `ACTIVE`.

### Daily Patient Balance
- Query all charges for a given admission.
- Group charges by date with daily subtotals.
- Return a running cumulative total.
- Include adjustments (negative amounts) in all calculations.
- Return empty balance (total = 0) when no charges exist.

### Invoice Generation (at Discharge)
- Collect all unbilled charges (`invoiced = false`) for an admission.
- Create an `Invoice` record linked to the admission with total amount and line item count.
- Mark all included charges with the invoice reference (`invoice_id`).
- Only allow invoice generation for DISCHARGED admissions.
- Only one invoice per admission (return 409 Conflict if one already exists).
- Reject invoice generation when no unbilled charges exist (return 400).

### Adjustments & Credits
- Create adjustment charges with negative amounts.
- Require a non-blank `reason` field for all adjustments (audit trail).
- Adjustments are included in balance calculations like any other charge.
- Adjustments on already-invoiced charges create supplementary records on the existing invoice.

### Integration Points (Existing Code Modifications)
- **InventoryItemService.createMovement()**: Publish an `InventoryDispensedEvent` when an EXIT movement is created with an admission ID.
- **CreateInventoryMovementRequest**: Add optional `admissionId: Long?` field.
- **InventoryMovement entity**: Add optional `admission` ManyToOne relationship.
- **AdmissionService.dischargePatient()**: Publish a `PatientDischargedEvent` after discharge to trigger billing finalization workflows.
- **Application config**: Add `@EnableScheduling` to a configuration class.

---

## Acceptance Criteria / Scenarios

### Real-Time Charge Capture
- When an inventory EXIT movement is created with a valid admission ID, a `PatientCharge` is automatically created with the correct item reference, quantity, and unit price from the inventory item, and the admission balance reflects the new charge.
- When an inventory EXIT movement is created without an admission ID (non-patient usage), no charge is created.
- When admin staff creates a manual charge with valid data (admission ID, charge type, amount), the charge is saved and returns 201 Created.
- When a manual charge request is missing required fields (amount, charge type, admission ID), the system returns 400 Bad Request with validation errors.

### Scheduled Daily Charges
- When the daily job runs at 1:00 AM, room charges are created for all ACTIVE admissions that have an assigned room with a configured price.
- When the daily job runs twice for the same date, no duplicate charges are created (idempotency verified).
- When a patient was discharged before the job runs, no daily charges are generated for that admission.
- When a room has no price configured (`price IS NULL`), no room charge is created and a warning is logged.

### Patient Balance
- When querying a patient's balance for an admission with multiple charges across multiple days, charges are grouped by date with correct daily subtotals and a cumulative running total.
- When querying a balance for an admission with adjustments (negative amounts), the total correctly reflects the deductions.
- When querying a balance for an admission with zero charges, an empty result with total = 0 is returned.
- When a user without `billing:read` permission attempts to view charges, the system returns 403 Forbidden.

### Invoice Generation
- When admin staff generates an invoice for a DISCHARGED admission with unbilled charges, an invoice is created, all charges are marked as invoiced, and the response includes the invoice summary with 201 Created.
- When attempting to generate an invoice for an ACTIVE (non-discharged) admission, the system returns 400 Bad Request.
- When attempting to generate an invoice for an admission with no unbilled charges, the system returns 400 Bad Request with an appropriate message.
- When an invoice already exists for the admission, the system returns 409 Conflict.

### Adjustments & Credits
- When an adjustment is created with a negative amount and a non-blank reason, the charge is saved and the balance is recalculated.
- When an adjustment is submitted without a reason, the system returns 400 Bad Request with a validation error.

### Authorization
- When a clinical staff member (DOCTOR/NURSE) attempts to create a manual charge, the system returns 403 Forbidden.
- When a clinical staff member views charges for an admission, the system returns 200 OK with charge data.
- When an unauthenticated user accesses any billing endpoint, the system returns 401 Unauthorized.

### Data Integrity
- Charges are immutable — no PUT/PATCH endpoint exists for charges. Corrections use adjustment charges only.
- When a referenced admission or inventory item is soft-deleted, existing charges remain intact as historical records.

---

## Non-Functional Requirements

- **Idempotency**: The daily charge scheduler must be safe to re-run without creating duplicates.
- **Immutability**: Charges cannot be modified after creation; only adjustments are allowed.
- **Decimal Precision**: All monetary fields use `DECIMAL(12,2)` — consistent with existing inventory pricing.
- **Audit Trail**: All charge creation and invoice generation is tracked via the existing `AuditEntityListener`.
- **Transactional Safety**: Event-driven charge capture uses `@TransactionalEventListener(phase = AFTER_COMMIT)` to ensure charges are only created when the source operation succeeds.
- **Security**: All inputs validated; no raw SQL in application code; use parameterized queries only.

---

## API Contract

### Charge Endpoints

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| GET | `/api/v1/admissions/{admissionId}/charges` | - | `ApiResponse<List<PatientChargeResponse>>` | `billing:read` | List all charges for admission |
| GET | `/api/v1/admissions/{admissionId}/balance` | - | `ApiResponse<AdmissionBalanceResponse>` | `billing:read` | Get daily balance breakdown |
| POST | `/api/v1/admissions/{admissionId}/charges` | `CreateChargeRequest` | `ApiResponse<PatientChargeResponse>` | `billing:create` | Create manual charge |
| POST | `/api/v1/admissions/{admissionId}/adjustments` | `CreateAdjustmentRequest` | `ApiResponse<PatientChargeResponse>` | `billing:adjust` | Create adjustment/credit |

### Invoice Endpoints

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| GET | `/api/v1/admissions/{admissionId}/invoice` | - | `ApiResponse<InvoiceResponse>` | `invoice:read` | Get invoice for admission |
| POST | `/api/v1/admissions/{admissionId}/invoice` | - | `ApiResponse<InvoiceResponse>` | `invoice:create` | Generate invoice at discharge |

### Modified Existing Endpoint

| Method | Endpoint | Change | Description |
|--------|----------|--------|-------------|
| POST | `/api/v1/admin/inventory-items/{itemId}/movements` | Add optional `admissionId` to request body | Link inventory EXIT to admission for billing |

### Request/Response Examples

```json
// POST /api/v1/admissions/{admissionId}/charges - Create Manual Charge
{
  "chargeType": "SERVICE",
  "description": "Physical therapy session",
  "quantity": 1,
  "unitPrice": 150.00,
  "inventoryItemId": null
}

// Response - PatientChargeResponse
{
  "success": true,
  "data": {
    "id": 42,
    "admissionId": 5,
    "chargeType": "SERVICE",
    "description": "Physical therapy session",
    "quantity": 1,
    "unitPrice": 150.00,
    "totalAmount": 150.00,
    "inventoryItemName": null,
    "roomNumber": null,
    "invoiced": false,
    "chargeDate": "2026-02-07",
    "createdAt": "2026-02-07T14:30:00",
    "createdByName": "Admin User"
  },
  "timestamp": "2026-02-07T14:30:00"
}
```

```json
// GET /api/v1/admissions/{admissionId}/balance - Daily Balance
{
  "success": true,
  "data": {
    "admissionId": 5,
    "patientName": "Juan Perez",
    "admissionDate": "2026-02-01",
    "totalBalance": 3250.00,
    "dailyBreakdown": [
      {
        "date": "2026-02-01",
        "charges": [
          {
            "id": 1,
            "chargeType": "ROOM",
            "description": "Room 101 - Daily Rate",
            "totalAmount": 500.00
          },
          {
            "id": 2,
            "chargeType": "MEDICATION",
            "description": "Amoxicillin 500mg",
            "quantity": 3,
            "unitPrice": 25.00,
            "totalAmount": 75.00
          }
        ],
        "dailyTotal": 575.00,
        "cumulativeTotal": 575.00
      },
      {
        "date": "2026-02-02",
        "charges": [ ... ],
        "dailyTotal": 500.00,
        "cumulativeTotal": 1075.00
      }
    ]
  },
  "timestamp": "2026-02-07T14:30:00"
}
```

```json
// POST /api/v1/admissions/{admissionId}/adjustments - Create Adjustment
{
  "description": "Billing correction - duplicate medication charge",
  "amount": -75.00,
  "reason": "Duplicate charge for Amoxicillin on 2026-02-01, correcting."
}

// Response - same PatientChargeResponse with chargeType: "ADJUSTMENT"
```

```json
// POST /api/v1/admissions/{admissionId}/invoice - Generate Invoice
// No request body needed

// Response - InvoiceResponse
{
  "success": true,
  "data": {
    "id": 10,
    "invoiceNumber": "INV-2026-0010",
    "admissionId": 5,
    "patientName": "Juan Perez",
    "admissionDate": "2026-02-01",
    "dischargeDate": "2026-02-07",
    "totalAmount": 3250.00,
    "chargeCount": 18,
    "chargeSummary": [
      { "chargeType": "ROOM", "count": 6, "subtotal": 3000.00 },
      { "chargeType": "MEDICATION", "count": 10, "subtotal": 350.00 },
      { "chargeType": "ADJUSTMENT", "count": 2, "subtotal": -100.00 }
    ],
    "generatedAt": "2026-02-07T15:00:00",
    "generatedByName": "Admin User"
  },
  "timestamp": "2026-02-07T15:00:00"
}
```

```json
// POST /api/v1/admin/inventory-items/{itemId}/movements - Modified Request
{
  "type": "EXIT",
  "quantity": 3,
  "notes": "Dispensed to patient",
  "admissionId": 5
}
```

---

## Database Changes

### New Entities

| Entity | Table | Extends | Description |
|--------|-------|---------|-------------|
| `PatientCharge` | `patient_charges` | `BaseEntity` | Every billable event — real-time and scheduled |
| `Invoice` | `invoices` | `BaseEntity` | Consolidated bill generated at discharge |

### Modified Entities

| Entity | Change | Description |
|--------|--------|-------------|
| `InventoryMovement` | Add `admission` ManyToOne | Optional link to admission for billing context |

### New Migrations

| Migration | Description |
|-----------|-------------|
| `V058__create_patient_charges_table.sql` | Creates `patient_charges` table with FKs, indexes, and CHECK constraints |
| `V059__create_invoices_table.sql` | Creates `invoices` table with FK to admissions |
| `V060__add_admission_to_inventory_movements.sql` | Adds `admission_id` FK column to `inventory_movements` |
| `V061__seed_billing_permissions.sql` | Seeds `billing:read`, `billing:create`, `billing:adjust`, `invoice:read`, `invoice:create` permissions and assigns to ADMIN role |

### Schema

```sql
-- V058__create_patient_charges_table.sql
CREATE TABLE patient_charges (
    id BIGSERIAL PRIMARY KEY,
    admission_id BIGINT NOT NULL REFERENCES admissions(id),
    charge_type VARCHAR(30) NOT NULL,
    description VARCHAR(500) NOT NULL,
    quantity INT NOT NULL DEFAULT 1 CHECK (quantity > 0),
    unit_price DECIMAL(12,2) NOT NULL CHECK (unit_price >= 0),
    total_amount DECIMAL(12,2) NOT NULL,
    charge_date DATE NOT NULL DEFAULT CURRENT_DATE,
    inventory_item_id BIGINT REFERENCES inventory_items(id),
    room_id BIGINT REFERENCES rooms(id),
    invoice_id BIGINT REFERENCES invoices(id),
    reason VARCHAR(500),
    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_patient_charges_deleted_at ON patient_charges(deleted_at);
CREATE INDEX idx_patient_charges_admission_id ON patient_charges(admission_id);
CREATE INDEX idx_patient_charges_charge_date ON patient_charges(charge_date);
CREATE INDEX idx_patient_charges_invoice_id ON patient_charges(invoice_id);
CREATE INDEX idx_patient_charges_charge_type ON patient_charges(charge_type);
-- Idempotency index for daily scheduler: prevent duplicate daily charges
CREATE UNIQUE INDEX idx_patient_charges_daily_unique
    ON patient_charges(admission_id, charge_type, charge_date, COALESCE(room_id, 0))
    WHERE charge_type IN ('ROOM') AND deleted_at IS NULL;
```

```sql
-- V059__create_invoices_table.sql
CREATE TABLE invoices (
    id BIGSERIAL PRIMARY KEY,
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    admission_id BIGINT NOT NULL REFERENCES admissions(id),
    total_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    charge_count INT NOT NULL DEFAULT 0,
    notes VARCHAR(1000),
    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_invoices_deleted_at ON invoices(deleted_at);
CREATE INDEX idx_invoices_admission_id ON invoices(admission_id);
CREATE UNIQUE INDEX idx_invoices_admission_unique
    ON invoices(admission_id) WHERE deleted_at IS NULL;
```

```sql
-- V060__add_admission_to_inventory_movements.sql
ALTER TABLE inventory_movements
    ADD COLUMN admission_id BIGINT REFERENCES admissions(id);

CREATE INDEX idx_inventory_movements_admission_id ON inventory_movements(admission_id);
```

### Index Requirements

- [x] `deleted_at` — Required on all new tables for soft delete queries
- [x] `admission_id` — FK on patient_charges, invoices, inventory_movements
- [x] `charge_date` — For daily balance grouping queries
- [x] `invoice_id` — For invoice line item lookups
- [x] `charge_type` — For filtered queries and scheduler idempotency
- [x] Daily unique constraint — Prevents duplicate scheduled charges per admission+type+date

---

## Frontend Changes

### Components

| Component | Location | Description |
|-----------|----------|-------------|
| `AdmissionCharges.vue` | `src/views/billing/` | Charge list for an admission (table with filters) |
| `AdmissionBalance.vue` | `src/views/billing/` | Daily balance breakdown with running totals |
| `CreateChargeDialog.vue` | `src/components/billing/` | Dialog for manual charge creation |
| `CreateAdjustmentDialog.vue` | `src/components/billing/` | Dialog for adjustments/credits (requires reason) |
| `InvoiceView.vue` | `src/views/billing/` | Invoice summary with charge type breakdown |
| `GenerateInvoiceDialog.vue` | `src/components/billing/` | Confirmation dialog for invoice generation |

### Pinia Stores

| Store | Location | Description |
|-------|----------|-------------|
| `useBillingStore` | `src/stores/billing.ts` | State management for charges, balances, and invoices |

### Routes

| Path | Component | Auth Required | Permission |
|------|-----------|---------------|------------|
| `/admissions/:id/charges` | `AdmissionCharges` | Yes | `billing:read` |
| `/admissions/:id/balance` | `AdmissionBalance` | Yes | `billing:read` |
| `/admissions/:id/invoice` | `InvoiceView` | Yes | `invoice:read` |

### Validation (Zod Schemas)

```typescript
// src/schemas/billing.ts
import { z } from 'zod'

export const createChargeSchema = z.object({
  chargeType: z.enum(['MEDICATION', 'PROCEDURE', 'LAB', 'SERVICE']),
  description: z.string().min(1).max(500),
  quantity: z.number().int().positive(),
  unitPrice: z.number().min(0),
  inventoryItemId: z.number().int().positive().optional(),
})

export const createAdjustmentSchema = z.object({
  description: z.string().min(1).max(500),
  amount: z.number().negative(),
  reason: z.string().min(1).max(500),
})
```

---

## Implementation Notes

### Architecture Pattern: Spring Domain Events (NOT AOP)

This module uses **Spring Domain Events** for real-time charge capture, consistent with the existing audit system pattern (`AuditEntityListener` -> `ApplicationEventPublisher` -> `@TransactionalEventListener`).

**Why not AOP?**
- AOP couples billing to method signatures — renaming a service method silently breaks billing.
- AOP hides behavior — developers reading a service class have no idea billing is happening.
- The codebase already has a proven event-driven pattern via the audit system.
- Events are explicit, testable, and debuggable.

**Event Flow:**
1. `InventoryItemService.createMovement()` publishes `InventoryDispensedEvent` for EXIT movements with an admission ID.
2. `AdmissionService.dischargePatient()` publishes `PatientDischargedEvent` after status change.
3. `BillingEventListener` handles both events via `@TransactionalEventListener(phase = AFTER_COMMIT)` with `@Transactional(propagation = REQUIRES_NEW)`.

### New Domain Events

```kotlin
// com.insidehealthgt.hms.event
data class InventoryDispensedEvent(
    val admissionId: Long,
    val inventoryItemId: Long,
    val itemName: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
)

data class PatientDischargedEvent(
    val admissionId: Long,
    val patientId: Long,
)
```

### Daily Scheduler

- Requires `@EnableScheduling` on a `@Configuration` class (new addition).
- `DailyChargeScheduler` component with `@Scheduled(cron = "0 0 1 * * *")`.
- Idempotency enforced at both application level (check before insert) and database level (unique partial index).

### Patterns to Follow

- Entities extend `BaseEntity`, annotated with `@SQLRestriction("deleted_at IS NULL")`.
- Services use `@Transactional(readOnly = true)` for reads, `@Transactional` for writes.
- Controllers return `ApiResponse<T>` wrapper. Use `@PreAuthorize("hasAuthority('...')")` for authorization.
- DTOs for all API contracts — never expose entities.
- Batch fetch users with `userRepository.findAllById(ids)` to avoid N+1 queries.
- Use existing exception classes: `ResourceNotFoundException`, `BadRequestException`, `ConflictException`.

### Invoice Number Generation

Format: `INV-{YEAR}-{ZERO_PADDED_SEQUENCE}` (e.g., `INV-2026-0001`). Sequence derived from the invoice table's auto-increment ID.

---

## QA Checklist

### Backend
- [ ] All functional requirements implemented
- [ ] `PatientCharge` entity extends `BaseEntity` with `@SQLRestriction`
- [ ] `Invoice` entity extends `BaseEntity` with `@SQLRestriction`
- [ ] `InventoryMovement` entity updated with optional admission FK
- [ ] DTOs used in all controllers (no entity exposure)
- [ ] Input validation on all request DTOs
- [ ] Charge immutability enforced (no update endpoint)
- [ ] Daily scheduler idempotency verified (re-run produces no duplicates)
- [ ] Event-driven charge capture tested (inventory EXIT -> charge created)
- [ ] Invoice generation tested (discharge flow -> single invoice)
- [ ] Adjustment reason required and validated
- [ ] Unit tests written and passing
- [ ] Integration tests with Testcontainers written and passing
- [ ] Detekt passes (no new violations)
- [ ] OWASP dependency-check passes

### Frontend
- [ ] Balance view component with daily grouping and running totals
- [ ] Manual charge creation dialog with validation
- [ ] Adjustment dialog with required reason field
- [ ] Invoice view with charge type summary
- [ ] Pinia store implemented with all actions
- [ ] Routes configured with proper permission guards
- [ ] Form validation with VeeValidate + Zod
- [ ] Error handling for all API failures
- [ ] ESLint/oxlint passes
- [ ] i18n keys added for all user-facing text (es/en)
- [ ] Unit tests written and passing (Vitest)

### E2E Tests (Playwright)
- [ ] Create manual charge and verify balance update
- [ ] Apply adjustment and verify balance recalculation
- [ ] Generate invoice at discharge and verify charge marking
- [ ] Permission denied flows (clinical staff cannot create charges)
- [ ] Form validation errors displayed correctly
- [ ] Daily balance view displays correct grouping and totals

### General
- [ ] API contract documented (this spec)
- [ ] All database migrations tested (up and validate)
- [ ] Feature documentation updated
- [ ] Reviewed by project owner

---

## Documentation Updates Required

### Must Update

- [ ] **[CLAUDE.md](../../CLAUDE.md)**
  - Add "Hospital Billing System" to "Implemented Features" backend and frontend sections
  - Update migration range (V058-V061)
  - Add billing permissions to security section
- [ ] **[ARCHITECTURE.md](../architecture/ARCHITECTURE.md)** (if exists)
  - Document event-driven billing architecture
  - Document `@EnableScheduling` addition
  - Add billing module to system architecture diagram

### Review for Consistency

- [ ] **[README.md](../../web/README.md)**
  - Check if setup instructions need updates (e.g., scheduler config)

### Code Documentation

- [ ] **`PatientCharge.kt`** — Document charge immutability rule
- [ ] **`BillingEventListener.kt`** — Document event handling pattern
- [ ] **`DailyChargeScheduler.kt`** — Document idempotency strategy

---

## Related Docs/Commits/Issues

- **Depends on**: Inventory Management Module (#20 — `feat: add inventory management module with room pricing`)
- **Depends on**: Patient Admission Module (admissions, rooms, discharge flow)
- **References**: Existing audit event pattern (`AuditEntityListener`, `AuditEventHandler`)
- **Design discussion**: Original billing system hybrid approach spec (attached document)

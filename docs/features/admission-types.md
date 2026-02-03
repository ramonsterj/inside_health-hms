# Feature: Admission Types

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-02-02 | @author | Initial draft |

---

## Overview

Add admission type classification to distinguish between different kinds of patient admissions (Hospitalization, Ambulatory, Electroshock Therapy, Ketamine Infusion, Emergency). This enables proper categorization, filtering, and different validation rules based on admission type. Some admission types (ambulatory, procedures) do not require physical room assignment.

---

## Use Case / User Story

1. **As an Administrative Staff member**, I want to select an admission type when creating an admission so that the hospital can properly categorize and track different kinds of patient visits.

2. **As an Administrative Staff member**, I want to create ambulatory or procedure-based admissions without assigning a physical room so that outpatient visits and day procedures don't consume bed capacity.

3. **As a user with admission read access**, I want to filter the admissions list by type so that I can quickly find specific categories of admissions (e.g., all ER admissions, all ketamine infusions).

---

## Authorization / Role Access

No new permissions required. This feature extends existing admission functionality.

| Action | Required Role(s) | Permission | Notes |
|--------|------------------|------------|-------|
| View admissions (with type) | ADMINISTRATIVE_STAFF, DOCTOR, NURSE, CHIEF_NURSE, ADMIN | `admission:read` | All users with read access can see type |
| Create admission (with type) | ADMINISTRATIVE_STAFF, ADMIN | `admission:create` | Type is required on creation |
| Update admission type | ADMINISTRATIVE_STAFF, ADMIN | `admission:update` | Only while ACTIVE |
| Filter by type | ADMINISTRATIVE_STAFF, DOCTOR, NURSE, CHIEF_NURSE, ADMIN | `admission:read` | Query parameter filter |

**Note:** Resident Doctors who need to manage admissions should be assigned both `DOCTOR` and `ADMINISTRATIVE_STAFF` roles.

---

## Functional Requirements

1. **Admission Type Enum**: Add admission type field with values:
   - `HOSPITALIZATION` - Traditional in-patient stay (requires room, requires triage code)
   - `AMBULATORY` - Outpatient visit (no room, no triage code)
   - `ELECTROSHOCK_THERAPY` - ECT procedure (no room, no triage code)
   - `KETAMINE_INFUSION` - Ketamine treatment (no room, no triage code)
   - `EMERGENCY` - ER admission (no room, requires triage code)

2. **Type Selection**: Admission type is required when creating an admission.

3. **Type Update**: Admission type can be updated while admission is ACTIVE (not DISCHARGED).

4. **List Filtering**: Support filtering admissions list by type via query parameter.

5. **Room Requirement Rules**:
   - `HOSPITALIZATION` → Room is **required**
   - `AMBULATORY`, `ELECTROSHOCK_THERAPY`, `KETAMINE_INFUSION`, `EMERGENCY` → Room is **hidden** (not available)

6. **Triage Code Requirement Rules**:
   - `HOSPITALIZATION` and `EMERGENCY` → Triage code is **required**
   - `AMBULATORY`, `ELECTROSHOCK_THERAPY`, `KETAMINE_INFUSION` → Triage code is **hidden** (not available)

6. **Migration Default**: Existing admissions default to `HOSPITALIZATION`.

7. **Display**: Admission type is displayed in list view (column) and detail view.

---

## Acceptance Criteria / Scenarios

### Happy Path

1. **Create with type**: When creating an admission with a valid admission type, the admission is created and the type is stored correctly.

2. **Update type**: When updating an active admission's type, the change is persisted and returned in the response.

3. **Filter by type**: When filtering admissions list by type (e.g., `?type=AMBULATORY`), only admissions of that type are returned.

4. **No room for procedure types**: When creating an AMBULATORY, ELECTROSHOCK_THERAPY, or KETAMINE_INFUSION admission, room assignment is optional.

5. **Display type**: Admission type is displayed in list view (as a column/tag) and detail view.

6. **Migration default**: After migration, all existing admissions have type = `HOSPITALIZATION`.

### Edge Cases

7. **Missing type on create**: When creating an admission without specifying type, return 400 Bad Request with validation error.

8. **Invalid type value**: When creating/updating with an invalid type value (e.g., "INVALID"), return 400 Bad Request.

9. **Room required for hospitalization/emergency**: When creating a HOSPITALIZATION or EMERGENCY admission without a room, return 400 Bad Request with message indicating room is required for this admission type.

10. **Cannot update discharged admission**: When attempting to change the type of a DISCHARGED admission, return 400 Bad Request.

11. **Filter with invalid type**: When filtering with an invalid type parameter, return 400 Bad Request with validation error.

12. **Room capacity still enforced**: When room is provided for any admission type, room capacity validation still applies.

---

## Non-Functional Requirements

- **Backward Compatibility**: Existing admissions seamlessly gain `HOSPITALIZATION` type via migration default.
- **Performance**: Type filter should use indexed column for efficient queries.
- **Audit**: Type changes are tracked via existing JPA auditing (updatedAt, updatedBy).

---

## API Contract

### Modified Endpoints

No new endpoints. The following existing endpoints are modified:

| Method | Endpoint | Changes |
|--------|----------|---------|
| GET | `/api/v1/admissions` | Add optional `type` query parameter for filtering |
| GET | `/api/v1/admissions/{id}` | Response includes `type` field |
| POST | `/api/v1/admissions` | Request requires `type` field |
| PUT | `/api/v1/admissions/{id}` | Request includes optional `type` field |

### Request/Response Changes

```json
// POST /api/v1/admissions - Updated Request
{
  "patientId": 1,
  "triageCodeId": 2,
  "roomId": 3,                    // Now optional for certain types
  "treatingPhysicianId": 4,
  "admissionDate": "2026-02-02T10:30:00",
  "type": "HOSPITALIZATION",      // NEW - Required
  "inventory": "Wallet, phone"
}

// POST /api/v1/admissions - Ambulatory Example (no room)
{
  "patientId": 1,
  "triageCodeId": 2,
  "roomId": null,                 // Optional for AMBULATORY
  "treatingPhysicianId": 4,
  "admissionDate": "2026-02-02T10:30:00",
  "type": "AMBULATORY",
  "inventory": null
}

// PUT /api/v1/admissions/{id} - Updated Request
{
  "triageCodeId": 2,
  "roomId": 3,
  "treatingPhysicianId": 4,
  "type": "HOSPITALIZATION",      // NEW - Optional (can change type)
  "inventory": "Wallet, phone, glasses"
}

// GET /api/v1/admissions?type=AMBULATORY - Filter by type
// GET /api/v1/admissions?status=ACTIVE&type=EMERGENCY - Combined filters

// Response - AdmissionListResponse (updated)
{
  "id": 1,
  "patient": { "id": 1, "firstName": "Juan", "lastName": "Perez" },
  "triageCode": { "id": 2, "code": "B", "color": "#FFA500" },
  "room": { "id": 3, "number": "201", "type": "PRIVATE" },  // null if no room
  "treatingPhysician": { "id": 4, "salutation": "Dr.", "firstName": "Maria", "lastName": "Garcia" },
  "admissionDate": "2026-02-02T10:30:00",
  "dischargeDate": null,
  "status": "ACTIVE",
  "type": "HOSPITALIZATION",      // NEW
  "hasConsentDocument": true,
  "createdAt": "2026-02-02T10:35:00"
}

// Response - AdmissionDetailResponse (updated)
{
  "id": 1,
  "patient": { ... },
  "triageCode": { ... },
  "room": { ... },                // null if no room assigned
  "treatingPhysician": { ... },
  "admissionDate": "2026-02-02T10:30:00",
  "dischargeDate": null,
  "status": "ACTIVE",
  "type": "HOSPITALIZATION",      // NEW
  "inventory": "Wallet, phone",
  "hasConsentDocument": true,
  "consultingPhysicians": [ ... ],
  "createdAt": "2026-02-02T10:35:00",
  "createdBy": { ... },
  "updatedAt": "2026-02-02T10:35:00",
  "updatedBy": { ... }
}

// Validation Error - Room required for type
{
  "status": 400,
  "error": "Bad Request",
  "message": "Room is required for HOSPITALIZATION admissions"
}

// Validation Error - Invalid type
{
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid admission type: INVALID. Valid types: HOSPITALIZATION, AMBULATORY, ELECTROSHOCK_THERAPY, KETAMINE_INFUSION, EMERGENCY"
}
```

### Admission Type Values

| Value | Display Name | Room Required | Triage Code Required |
|-------|--------------|---------------|----------------------|
| `HOSPITALIZATION` | Hospitalization | Yes | Yes |
| `AMBULATORY` | Ambulatory | No (hidden) | No (hidden) |
| `ELECTROSHOCK_THERAPY` | Electroshock Therapy | No (hidden) | No (hidden) |
| `KETAMINE_INFUSION` | Ketamine Infusion | No (hidden) | No (hidden) |
| `EMERGENCY` | Emergency | No (hidden) | Yes |

---

## Database Changes

### Entity Changes

**AdmissionType.kt** (New Enum)
```kotlin
package com.insidehealthgt.hms.entity

enum class AdmissionType {
    HOSPITALIZATION,
    AMBULATORY,
    ELECTROSHOCK_THERAPY,
    KETAMINE_INFUSION,
    EMERGENCY;

    fun requiresRoom(): Boolean = this == HOSPITALIZATION

    fun requiresTriageCode(): Boolean = this in listOf(HOSPITALIZATION, EMERGENCY)
}
```

**Admission.kt** (Modified)
```kotlin
// Add new field
@Column(name = "type", nullable = false, length = 30)
@Enumerated(EnumType.STRING)
var type: AdmissionType = AdmissionType.HOSPITALIZATION

// Make room nullable
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "room_id")  // Remove nullable = false
var room: Room? = null
```

### New Migrations

| Migration | Description |
|-----------|-------------|
| `V029__add_admission_type.sql` | Add type column, update room_id to nullable, create index |

### Schema

```sql
-- V029__add_admission_type.sql

-- Add admission_type column with default for existing records
ALTER TABLE admissions
ADD COLUMN type VARCHAR(30) NOT NULL DEFAULT 'HOSPITALIZATION';

-- Make room_id nullable (for ambulatory/procedure admissions)
ALTER TABLE admissions
ALTER COLUMN room_id DROP NOT NULL;

-- Create index on type for filtering
CREATE INDEX idx_admissions_type ON admissions(type);

-- Create composite index for common filter combinations
CREATE INDEX idx_admissions_status_type ON admissions(status, type) WHERE deleted_at IS NULL;
```

### Index Requirements

- [x] `type` - Filter by admission type
- [x] `(status, type)` - Composite index for combined filters (partial index excluding soft-deleted)

---

## Frontend Changes

### TypeScript Types

**admission.ts** (Updated)
```typescript
// New enum
export enum AdmissionType {
  HOSPITALIZATION = 'HOSPITALIZATION',
  AMBULATORY = 'AMBULATORY',
  ELECTROSHOCK_THERAPY = 'ELECTROSHOCK_THERAPY',
  KETAMINE_INFUSION = 'KETAMINE_INFUSION',
  EMERGENCY = 'EMERGENCY'
}

// Helper to check if room is required
export function admissionTypeRequiresRoom(type: AdmissionType): boolean {
  return type === AdmissionType.HOSPITALIZATION
}

// Helper to check if triage code is required
export function admissionTypeRequiresTriageCode(type: AdmissionType): boolean {
  return type === AdmissionType.HOSPITALIZATION || type === AdmissionType.EMERGENCY
}

// Updated interfaces
export interface AdmissionListItem {
  id: number
  patient: PatientSummary
  triageCode: TriageCodeSummary
  room: RoomSummary | null      // Now nullable
  treatingPhysician: Doctor
  admissionDate: string
  dischargeDate: string | null
  status: AdmissionStatus
  type: AdmissionType           // NEW
  hasConsentDocument: boolean
  createdAt: string | null
}

export interface AdmissionDetail {
  // ... existing fields ...
  room: RoomSummary | null      // Now nullable
  type: AdmissionType           // NEW
}

export interface CreateAdmissionRequest {
  patientId: number
  triageCodeId: number
  roomId: number | null         // Now nullable
  treatingPhysicianId: number
  admissionDate: string
  type: AdmissionType           // NEW - Required
  inventory?: string | null
}

export interface UpdateAdmissionRequest {
  triageCodeId: number
  roomId: number | null         // Now nullable
  treatingPhysicianId: number
  type?: AdmissionType          // NEW - Optional
  inventory?: string | null
}
```

### Components

| Component | Location | Changes |
|-----------|----------|---------|
| `AdmissionsView.vue` | `src/views/admissions/` | Add type column, add type filter dropdown |
| `AdmissionWizardView.vue` | `src/views/admissions/` | Add type selector to Step 2, conditional room requirement |
| `AdmissionDetailView.vue` | `src/views/admissions/` | Display admission type |
| `AdmissionTypeBadge.vue` | `src/components/admissions/` | **NEW** - Display type with icon/color |

### Pinia Store

**admission.ts** (Updated)
```typescript
// Update fetchAdmissions to accept type filter
async fetchAdmissions(
  page: number = 0,
  size: number = 20,
  status?: AdmissionStatus,
  type?: AdmissionType          // NEW parameter
): Promise<void>
```

### Validation (Zod Schemas)

**admission.ts** (Updated)
```typescript
import { z } from 'zod'
import { AdmissionType } from '@/types/admission'

// Admission type enum for Zod
const admissionTypeEnum = z.enum([
  'HOSPITALIZATION',
  'AMBULATORY',
  'ELECTROSHOCK_THERAPY',
  'KETAMINE_INFUSION',
  'EMERGENCY'
])

export const createAdmissionSchema = z.object({
  patientId: z.number().positive('Patient is required'),
  triageCodeId: z.number().positive('Triage code is required'),
  roomId: z.number().positive().nullable(),  // Now nullable
  treatingPhysicianId: z.number().positive('Treating physician is required'),
  admissionDate: z.string().min(1, 'Admission date is required'),
  type: admissionTypeEnum,                    // NEW - Required
  inventory: z.string().max(2000).optional().nullable(),
}).refine(
  (data) => {
    // Room required for HOSPITALIZATION and EMERGENCY
    const requiresRoom = data.type === 'HOSPITALIZATION' || data.type === 'EMERGENCY'
    return !requiresRoom || data.roomId !== null
  },
  {
    message: 'Room is required for this admission type',
    path: ['roomId'],
  }
)

export const updateAdmissionSchema = z.object({
  triageCodeId: z.number().positive('Triage code is required'),
  roomId: z.number().positive().nullable(),
  treatingPhysicianId: z.number().positive('Treating physician is required'),
  type: admissionTypeEnum.optional(),         // NEW - Optional on update
  inventory: z.string().max(2000).optional().nullable(),
})
```

### i18n Keys

```json
{
  "admission": {
    "type": {
      "label": "Admission Type",
      "HOSPITALIZATION": "Hospitalization",
      "AMBULATORY": "Ambulatory",
      "ELECTROSHOCK_THERAPY": "Electroshock Therapy",
      "KETAMINE_INFUSION": "Ketamine Infusion",
      "EMERGENCY": "Emergency",
      "filter": {
        "all": "All Types",
        "placeholder": "Filter by type..."
      }
    },
    "validation": {
      "typeRequired": "Admission type is required",
      "roomRequiredForType": "Room is required for {type} admissions"
    }
  }
}
```

### UI Changes Summary

1. **Admission Wizard Step 2**:
   - Add admission type dropdown (required)
   - Room dropdown becomes conditional based on type
   - Show/hide room field or mark as optional based on selected type

2. **Admissions List View**:
   - Add "Type" column after patient name
   - Add type filter dropdown next to status filter
   - Display type as badge/tag with appropriate styling

3. **Admission Detail View**:
   - Display admission type in the header/summary section
   - Show "No Room" or similar indicator when room is null

---

## Implementation Notes

- **Enum vs Table**: Using enum for simplicity. If admin-configurable types are needed later, migrate to a `admission_types` table.
- **Room Validation**: Validation happens in both frontend (Zod refine) and backend (service layer) for defense in depth.
- **Backward Compatibility**: The migration sets `HOSPITALIZATION` as default, so existing admissions continue to work.
- **Follow Existing Patterns**:
  - Use `AdmissionStatus` filter pattern for the new type filter
  - Follow `TriageCodeBadge` pattern for `AdmissionTypeBadge` component
- **Room Capacity**: When room is null, skip room capacity validation entirely.

---

## QA Checklist

### Backend
- [ ] `AdmissionType` enum created with `requiresRoom()` helper
- [ ] `Admission` entity updated with `type` field
- [ ] `Admission.room` is now nullable
- [ ] `CreateAdmissionRequest` requires `type` field
- [ ] `UpdateAdmissionRequest` includes optional `type` field
- [ ] `AdmissionListResponse` includes `type` field
- [ ] `AdmissionDetailResponse` includes `type` field
- [ ] Room validation is conditional based on admission type
- [ ] Cannot update type of DISCHARGED admission
- [ ] List endpoint supports `?type=` query parameter filter
- [ ] Invalid type values return 400 Bad Request
- [ ] Migration V029 applies successfully
- [ ] Existing admissions have type = HOSPITALIZATION after migration
- [ ] Unit tests for type validation logic
- [ ] Integration tests for type filtering
- [ ] Detekt passes
- [ ] OWASP dependency-check passes

### Frontend
- [ ] `AdmissionType` enum added to types
- [ ] `admissionTypeRequiresRoom()` helper function works
- [ ] Admission wizard has type selector in Step 2
- [ ] Room field is conditional based on type selection
- [ ] Validation schema enforces room requirement by type
- [ ] Admissions list shows type column
- [ ] Admissions list has type filter dropdown
- [ ] Admission detail view displays type
- [ ] `AdmissionTypeBadge` component created
- [ ] i18n keys added for all type labels
- [ ] Store updated with type filter parameter
- [ ] ESLint/oxlint passes
- [ ] Unit tests for type-related logic

### E2E Tests (Playwright)
- [ ] Create HOSPITALIZATION admission with room (happy path)
- [ ] Create AMBULATORY admission without room (happy path)
- [ ] Create HOSPITALIZATION without room shows validation error
- [ ] Filter admissions by type
- [ ] Combined status + type filter works
- [ ] Update admission type
- [ ] Cannot update type of discharged admission
- [ ] Type displayed correctly in list and detail views

### General
- [ ] API contract documented
- [ ] Database migration tested (fresh + upgrade from existing)
- [ ] Feature documentation updated
- [ ] Reviewed by project owner

---

## Documentation Updates Required

### Must Update

- [ ] **[CLAUDE.md](../../CLAUDE.md)**
  - Add admission types to "Implemented Features" section
  - Note the `AdmissionType` enum values

- [ ] **[patient-admission.md](./patient-admission.md)**
  - Reference this feature spec for type information
  - Update functional requirements to mention type selection

### Code Documentation

- [ ] **`AdmissionType.kt`** - Document enum values and `requiresRoom()` method
- [ ] **`AdmissionService.kt`** - Document type-based room validation logic

---

## Related Docs/Commits/Issues

- Related feature: [Patient Admission](./patient-admission.md)
- Related entity: `Admission` ([Admission.kt](../../api/src/main/kotlin/com/insidehealthgt/hms/entity/Admission.kt))
- Related migration pattern: `V023__create_admissions_table.sql`

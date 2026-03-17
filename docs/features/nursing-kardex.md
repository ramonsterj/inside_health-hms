# Feature: Nursing Kardex Dashboard

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-03-16 | @claude | Initial draft |
| 1.1 | 2026-03-16 | @claude | Resolve gaps and contradictions from spec review |

---

## Overview

The Nursing Kardex is a specialized dashboard for nursing staff (NURSE, CHIEF_NURSE) that replaces the generic admission list with a clinical workflow view. For each active admission, it shows a compact summary card with the patient's latest vital signs, active medication orders with pending administration status, active care instructions (diet, special care, mobility restrictions), and the most recent nursing note — giving nurses immediate visibility into what needs to be done without navigating into individual admission records.

---

## Use Case / User Story

1. **As a nurse**, I want to see all my active patients with their pending medications, latest vitals, and active care orders at a glance so that I can prioritize my work without clicking into each admission.

2. **As a nurse**, I want to quickly record a medication administration directly from the kardex so that I can work through my medication rounds efficiently.

3. **As a nurse**, I want to quickly record vital signs for a patient directly from the kardex so that I don't have to navigate into the full admission detail for routine tasks.

4. **As a nurse**, I want to quickly add a nursing note from the kardex so that I can document observations in real time during rounds.

5. **As a nurse**, I want to see how long ago the last vital signs were recorded for each patient so that I can identify patients who are overdue for vitals.

6. **As a nurse**, I want to see active diet and special care instructions prominently so that I don't miss critical care requirements.

7. **As a chief nurse**, I want an overview of all active admissions with clinical summaries so that I can supervise nursing staff and identify patients needing attention.

8. **As a nurse**, I want to filter the kardex by admission type so that I can focus on hospitalized patients during my ward rounds.

---

## Authorization / Role Access

| Action | Required Role(s) | Permission | Notes |
|--------|------------------|------------|-------|
| View kardex dashboard | NURSE, CHIEF_NURSE, ADMIN | `admission:read` + `nursing-note:read` + `vital-sign:read` + `medical-order:read` | Combines existing permissions; no new permissions needed |
| Quick-add nursing note | NURSE, CHIEF_NURSE, ADMIN | `nursing-note:create` | Uses existing nursing note creation flow |
| Quick-record vital signs | NURSE, CHIEF_NURSE, ADMIN | `vital-sign:create` | Uses existing vital signs creation flow |
| Quick-administer medication | NURSE, CHIEF_NURSE, ADMIN | `medication-administration:create` | Uses existing MAR creation flow |
| View admission detail | NURSE, CHIEF_NURSE, ADMIN | `admission:read` | Navigate to full admission detail |

**Notes:**
- No new permissions or migrations required. The kardex aggregates data accessible through existing permissions.
- DOCTOR role is excluded from the kardex — doctors retain their existing dashboard with their own patient list. Doctors can view all this data through admission detail.
- If a user lacks a specific sub-permission (e.g., `medical-order:read`), that section is hidden on their kardex cards rather than blocking the entire view.
- ADMIN users can access `/nursing-kardex` directly but are NOT redirected there from `/dashboard`.

---

## Functional Requirements

### Kardex Dashboard View

- Serves as the default landing page for users with NURSE or CHIEF_NURSE roles (via redirect from /dashboard)
- Displays all ACTIVE admissions as expandable summary cards
- Supports filtering by admission type (Hospitalization, Ambulatory, etc.)
- Supports text search by patient name
- Sorted by room number (ascending), then by admission date (ascending) — mirrors physical ward layout
- Paginated (default 20 patients per page)

### Patient Summary Card (Collapsed)

Each card shows a compact row with:

| Field | Source | Display |
|-------|--------|---------|
| Patient name | `admission.patient` | Full name |
| Room | `admission.room` | Room number or "—" if unassigned |
| Triage code | `admission.triageCode` | Colored badge (existing pattern) |
| Admission type | `admission.type` | Badge (existing pattern) |
| Days admitted | `admission.admissionDate` | Calculated relative to today |
| Treating physician | `admission.treatingPhysician` | Doctor name |
| Active medication count | Count of ACTIVE medical orders with category=MEDICAMENTOS | Numeric badge |
| Hours since last vitals | Most recent `VitalSign.recordedAt` | e.g., "2h ago", "8h ago"; red if > 8h, yellow if > 4h |
| Active alerts count | Count of ACTIVE orders in: CUIDADOS_ESPECIALES, DIETA, RESTRICCIONES_MOVILIDAD | Numeric badge; highlighted if > 0 |
| Last nursing note preview | Most recent `NursingNote.description` | First 80 characters, truncated |

### Patient Summary Card (Expanded)

When a card is expanded, it shows three sections:

**Sorting rules:**
- **Medications**: by schedule time (next due first), then alphabetically. PRN medications sort after scheduled ones.
- **Care instructions**: grouped by category in display order (DIETA → CUIDADOS_ESPECIALES → RESTRICCIONES_MOVILIDAD → PERMISOS_VISITA), then by startDate ascending within group.

#### Section 1: Active Medications

- List of all ACTIVE medical orders with category `MEDICAMENTOS`
- Per medication: name, dosage, route, frequency, schedule
- Per medication: last administration timestamp, status (GIVEN/MISSED/REFUSED/HELD), and who administered
- Per medication: a quick "Administer" button that opens the existing `MedicationAdministrationDialog`
- Empty state: "No active medication orders"

#### Section 2: Active Care Instructions

- Grouped list of ACTIVE medical orders in the following categories:
  - **DIETA** — Diet instructions
  - **CUIDADOS_ESPECIALES** — Special care instructions
  - **RESTRICCIONES_MOVILIDAD** — Mobility restrictions
  - **PERMISOS_VISITA** — Visit permissions
- Per order: start date, observations/description
- Empty state: "No active care instructions"

#### Section 3: Latest Vital Signs

- Most recent vital sign record displayed as a compact row:
  - BP (systolic/diastolic), HR, RR, Temp, SpO2
  - Recorded at (timestamp)
  - Recorded by (staff name)
- Quick "Record Vitals" button that opens the existing `VitalSignFormDialog`
- Empty state: "No vital signs recorded"

### Quick Actions (Always Visible on Expanded Card)

- **Record Vitals** — Opens `VitalSignFormDialog` pre-filled with admission ID
- **Add Note** — Opens `NursingNoteFormDialog` pre-filled with admission ID
- **View Detail** — Navigates to the full admission detail view (`/admissions/:id`)

### Auto-refresh

- The kardex data refreshes automatically every 5 minutes (configurable interval)
- Manual refresh button available in the header

---

## Acceptance Criteria / Scenarios

### Kardex View - Happy Path
- When a nurse logs in, the system renders the kardex dashboard instead of the generic admission table.
- When the kardex loads, each active admission appears as a summary card with all specified fields populated.
- When a nurse expands a patient card, the medication list, care instructions, and latest vitals are displayed.
- When a nurse clicks "Administer" on a medication, the existing MAR dialog opens pre-filled with the correct admission and order.
- When a nurse clicks "Record Vitals", the existing vital signs form dialog opens for the correct admission.
- When a nurse clicks "Add Note", the existing nursing note form dialog opens for the correct admission.
- When a nurse completes a quick action (administer, record vitals, add note), the card refreshes to reflect the updated data.
- When a nurse filters by admission type, only matching admissions are displayed.
- When a nurse searches by patient name, only matching patients are shown.

### Kardex View - Edge Cases
- When a patient has no medication orders, the medication count badge shows "0" and the expanded medication section shows "No active medication orders".
- When a patient has no vital signs recorded, "Hours since last vitals" shows "Never" in red, and the expanded vitals section shows "No vital signs recorded".
- When a patient has no room assigned, the room column shows "—".
- When there are no active admissions, the kardex shows an empty state message.
- When a user without NURSE or CHIEF_NURSE role accesses `/nursing-kardex`, they are redirected to the standard dashboard.

### Summary Endpoint - Performance
- The kardex summary endpoint returns data for up to 50 admissions in a single request within 500ms.
- The endpoint batches all sub-queries (vitals, notes, orders) to avoid N+1 problems.

---

## Non-Functional Requirements

- **Performance**: Kardex summary endpoint responds in < 500ms for up to 50 admissions (max configurable page size). Default page size is 20; the 500ms target covers worst-case.
- **Performance**: Summary data uses batch queries (one query per data type, not per admission)
- **Security**: No new permissions introduced; reuses existing permission checks
- **Reliability**: If a sub-query fails (e.g., vital signs fetch), the endpoint still returns HTTP 200 with affected fields set to empty defaults (empty list, null, or 0). A warning log is emitted server-side. No per-section error indicator is included in the DTO.
- **Localization**: Full EN/ES support for all new UI text
- **Responsiveness**: Cards adapt to mobile screen widths (stacked layout on small screens)

---

## API Contract

### New Endpoints

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| GET | `/api/v1/nursing-kardex` | - | `Page<KardexAdmissionSummary>` | Yes | Aggregated kardex view for all active admissions |
| GET | `/api/v1/nursing-kardex/{admissionId}` | - | `KardexAdmissionSummary` | Yes | Single admission summary (for post-action card refresh) |

### Existing Endpoints (Reused by Quick Actions)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/admissions/{id}/nursing-notes` | Create nursing note |
| POST | `/api/v1/admissions/{id}/vital-signs` | Record vital signs |
| POST | `/api/v1/admissions/{id}/medical-orders/{orderId}/administrations` | Record medication administration |

### Query Parameters

**Kardex Summary**:
- `type` (optional): `AdmissionType` enum filter (e.g., `HOSPITALIZATION`)
- `search` (optional): Patient name search (case-insensitive, accent-insensitive using existing `unaccent` extension)
- `page` (default: 0): Page number
- `size` (default: 20): Page size
- `sort` (default: `room,asc`): Sort field and direction

### Response DTOs

```kotlin
data class KardexAdmissionSummary(
    // Admission info
    val admissionId: Long,
    val patientId: Long,
    val patientName: String,
    val roomNumber: String?,          // null if no room assigned
    val triageCode: String?,
    val triageColorCode: String?,     // hex color for badge
    val admissionType: AdmissionType,
    val admissionDate: LocalDateTime,
    val daysAdmitted: Int,            // calculated server-side
    val treatingPhysicianName: String,

    // Medication summary
    val activeMedicationCount: Int,
    val medications: List<KardexMedicationSummary>,

    // Care instructions summary
    val activeCareInstructionCount: Int,
    val careInstructions: List<KardexCareInstruction>,

    // Vital signs summary
    val latestVitalSigns: KardexVitalSignSummary?,    // null if none recorded
    val hoursSinceLastVitals: Double?,                 // null if none recorded

    // Nursing note summary
    val lastNursingNotePreview: String?,     // first 80 chars, null if none
    val lastNursingNoteAt: LocalDateTime?,   // timestamp of most recent note
)

data class KardexMedicationSummary(
    val orderId: Long,
    val medication: String,
    val dosage: String?,
    val route: AdministrationRoute?,
    val frequency: String?,
    val schedule: String?,
    val inventoryItemId: Long?,
    val inventoryItemName: String?,
    val observations: String?,
    val lastAdministration: KardexLastAdministration?,  // null if never administered
)

data class KardexLastAdministration(
    val administeredAt: LocalDateTime,
    val status: AdministrationStatus,
    val administeredByName: String?,
)

data class KardexCareInstruction(
    val orderId: Long,
    val category: MedicalOrderCategory,   // DIETA, CUIDADOS_ESPECIALES, RESTRICCIONES_MOVILIDAD, PERMISOS_VISITA
    val startDate: LocalDate,
    val observations: String?,
)

data class KardexVitalSignSummary(
    val recordedAt: LocalDateTime,
    val systolicBp: Int,
    val diastolicBp: Int,
    val heartRate: Int,
    val respiratoryRate: Int,
    val temperature: BigDecimal,
    val oxygenSaturation: Int,
    val recordedByName: String?,
)
```

### Request/Response Examples

```json
// GET /api/v1/nursing-kardex?type=HOSPITALIZATION&page=0&size=20 - Response
{
  "content": [
    {
      "admissionId": 12,
      "patientId": 5,
      "patientName": "Juan Pérez García",
      "roomNumber": "201-A",
      "triageCode": "II",
      "triageColorCode": "#FFA500",
      "admissionType": "HOSPITALIZATION",
      "admissionDate": "2026-03-10T08:30:00",
      "daysAdmitted": 6,
      "treatingPhysicianName": "Dr. María López",
      "activeMedicationCount": 3,
      "medications": [
        {
          "orderId": 45,
          "medication": "Haloperidol",
          "dosage": "5mg",
          "route": "IM",
          "frequency": "Every 8 hours",
          "schedule": "06:00, 14:00, 22:00",
          "inventoryItemId": 22,
          "inventoryItemName": "Haloperidol 5mg/ml Amp",
          "observations": null,
          "lastAdministration": {
            "administeredAt": "2026-03-16T06:15:00",
            "status": "GIVEN",
            "administeredByName": "Ana Rodríguez"
          }
        },
        {
          "orderId": 46,
          "medication": "Diazepam",
          "dosage": "10mg",
          "route": "ORAL",
          "frequency": "Every 12 hours",
          "schedule": "08:00, 20:00",
          "inventoryItemId": 18,
          "inventoryItemName": "Diazepam 10mg Tab",
          "observations": null,
          "lastAdministration": null
        },
        {
          "orderId": 50,
          "medication": "Lorazepam",
          "dosage": "2mg",
          "route": "ORAL",
          "frequency": "PRN",
          "schedule": "As needed for agitation",
          "inventoryItemId": null,
          "inventoryItemName": null,
          "observations": "As needed for agitation",
          "lastAdministration": {
            "administeredAt": "2026-03-15T23:30:00",
            "status": "GIVEN",
            "administeredByName": "Carlos Méndez"
          }
        }
      ],
      "activeCareInstructionCount": 2,
      "careInstructions": [
        {
          "orderId": 47,
          "category": "DIETA",
          "startDate": "2026-03-10",
          "observations": "Dieta blanda, sin restricción de líquidos"
        },
        {
          "orderId": 48,
          "category": "RESTRICCIONES_MOVILIDAD",
          "startDate": "2026-03-12",
          "observations": "No salidas sin acompañamiento. Supervisión continua."
        }
      ],
      "latestVitalSigns": {
        "recordedAt": "2026-03-16T06:00:00",
        "systolicBp": 125,
        "diastolicBp": 78,
        "heartRate": 72,
        "respiratoryRate": 16,
        "temperature": 36.5,
        "oxygenSaturation": 97,
        "recordedByName": "Ana Rodríguez"
      },
      "hoursSinceLastVitals": 4.5,
      "lastNursingNotePreview": "Paciente descansó durante la noche sin incidentes. Se administró medicación de 06:00 sin...",
      "lastNursingNoteAt": "2026-03-16T06:30:00"
    }
  ],
  "totalElements": 15,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

---

## Database Changes

### New Entities

No new database tables are required. The kardex endpoint aggregates existing data from:
- `admissions` (with `patients`, `rooms`, `triage_codes`, `users` joins)
- `medical_orders` (filtered by status=ACTIVE)
- `medication_administrations` (latest per order)
- `vital_signs` (latest per admission)
- `nursing_notes` (latest per admission)

### New Migrations

No new migrations required. All data, permissions, and indexes already exist.

### Query Strategy

The backend service should execute the following batch queries to avoid N+1:

1. **Active admissions** — Single paginated query with patient, room, triage code, and physician joins
2. **Active medical orders** — Single query: `WHERE admission_id IN (:ids) AND status = 'ACTIVE' AND category IN ('MEDICAMENTOS', 'DIETA', 'CUIDADOS_ESPECIALES', 'RESTRICCIONES_MOVILIDAD', 'PERMISOS_VISITA') AND deleted_at IS NULL`
3. **Latest medication administrations** — Single query with `ROW_NUMBER() OVER (PARTITION BY medical_order_id ORDER BY administered_at DESC)` to get only the most recent per order
4. **Latest vital signs** — Single query with `ROW_NUMBER() OVER (PARTITION BY admission_id ORDER BY recorded_at DESC)` to get only the most recent per admission
5. **Latest nursing notes** — Single query with `ROW_NUMBER() OVER (PARTITION BY admission_id ORDER BY created_at DESC)` to get only the most recent per admission
6. **User names** — Batch-fetch all referenced user IDs for display names

### Index Requirements

All required indexes already exist:
- [x] `idx_medical_orders_admission_id` — For filtering orders by admission
- [x] `idx_medication_administrations_medical_order_id` — For latest administration lookup
- [x] `idx_vital_signs_admission_id` + `idx_vital_signs_recorded_at` — For latest vitals lookup
- [x] `idx_nursing_notes_admission_id` + `idx_nursing_notes_created_at` — For latest note lookup
- [x] All `deleted_at` indexes — For soft delete filtering

---

## Frontend Changes

### Components

| Component | Location | Description |
|-----------|----------|-------------|
| `NursingKardexView.vue` | `src/views/nursing/` | Main kardex dashboard page |
| `KardexPatientCard.vue` | `src/components/nursing/kardex/` | Collapsed/expanded patient summary card |
| `KardexMedicationList.vue` | `src/components/nursing/kardex/` | Active medications with last administration and quick-administer |
| `KardexCareInstructions.vue` | `src/components/nursing/kardex/` | Active diet/special care/restrictions display |
| `KardexVitalsSummary.vue` | `src/components/nursing/kardex/` | Latest vital signs compact display |

### Pinia Stores

| Store | Location | Description |
|-------|----------|-------------|
| `useKardexStore` | `src/stores/kardex.ts` | State management for kardex data (fetch, pagination, filtering, refresh) |

### Routes

| Path | Component | Auth Required | Roles |
|------|-----------|---------------|-------|
| `/nursing-kardex` | `NursingKardexView` | Yes | NURSE, CHIEF_NURSE, ADMIN |

**Role-based routing**: When a user with NURSE or CHIEF_NURSE role logs in, the router redirects `/dashboard` to `/nursing-kardex`. Other roles continue to see the standard `DashboardView`.

### Validation (Zod Schemas)

No new validation schemas required. Quick actions reuse existing schemas:
- `nursingNoteSchema` (from `src/schemas/nursingNote.ts`)
- `vitalSignSchema` (from `src/schemas/vitalSign.ts`)
- Medication administration uses the existing `CreateMedicationAdministrationRequest` flow

### TypeScript Types

```typescript
// src/types/kardex.ts

export interface KardexAdmissionSummary {
  admissionId: number
  patientId: number
  patientName: string
  roomNumber: string | null
  triageCode: string | null
  triageColorCode: string | null
  admissionType: AdmissionType
  admissionDate: string
  daysAdmitted: number
  treatingPhysicianName: string

  activeMedicationCount: number
  medications: KardexMedicationSummary[]

  activeCareInstructionCount: number
  careInstructions: KardexCareInstruction[]

  latestVitalSigns: KardexVitalSignSummary | null
  hoursSinceLastVitals: number | null

  lastNursingNotePreview: string | null
  lastNursingNoteAt: string | null
}

export interface KardexMedicationSummary {
  orderId: number
  medication: string
  dosage: string | null
  route: string | null
  frequency: string | null
  schedule: string | null
  inventoryItemId: number | null
  inventoryItemName: string | null
  observations: string | null
  lastAdministration: KardexLastAdministration | null
}

export interface KardexLastAdministration {
  administeredAt: string
  status: AdministrationStatus
  administeredByName: string | null
}

export interface KardexCareInstruction {
  orderId: number
  category: string
  startDate: string
  observations: string | null
}

export interface KardexVitalSignSummary {
  recordedAt: string
  systolicBp: number
  diastolicBp: number
  heartRate: number
  respiratoryRate: number
  temperature: number
  oxygenSaturation: number
  recordedByName: string | null
}
```

### i18n Keys

```json
// English (en.json) — under "kardex" top-level key
{
  "kardex": {
    "title": "Nursing Kardex",
    "subtitle": "Active patient overview",
    "searchPlaceholder": "Search by patient name...",
    "filterByType": "Filter by type",
    "allTypes": "All Types",
    "refresh": "Refresh",
    "autoRefresh": "Auto-refresh every 5 min",
    "noAdmissions": "No active admissions",
    "daysAdmitted": "{count} day | {count} days",
    "treatingPhysician": "Physician",
    "noRoom": "No room",

    "medications": {
      "title": "Active Medications",
      "count": "{count} active medication | {count} active medications",
      "empty": "No active medication orders",
      "dose": "Dose",
      "route": "Route",
      "frequency": "Frequency",
      "schedule": "Schedule",
      "administer": "Administer",
      "lastAdmin": "Last administered",
      "neverAdministered": "Not yet administered",
      "noInventoryLink": "No inventory item linked"
    },

    "careInstructions": {
      "title": "Care Instructions",
      "count": "{count} active instruction | {count} active instructions",
      "empty": "No active care instructions",
      "since": "Since"
    },

    "vitals": {
      "title": "Latest Vital Signs",
      "empty": "No vital signs recorded",
      "recordedAt": "Recorded",
      "recordedBy": "By",
      "hoursAgo": "{hours}h ago",
      "never": "Never",
      "overdue": "Overdue",
      "recordVitals": "Record Vitals"
    },

    "notes": {
      "lastNote": "Last Note",
      "noNotes": "No nursing notes",
      "addNote": "Add Note"
    },

    "actions": {
      "viewDetail": "View Detail",
      "expand": "Expand",
      "collapse": "Collapse"
    }
  }
}
```

```json
// Spanish (es.json) — under "kardex" top-level key
{
  "kardex": {
    "title": "Kardex de Enfermería",
    "subtitle": "Vista general de pacientes activos",
    "searchPlaceholder": "Buscar por nombre de paciente...",
    "filterByType": "Filtrar por tipo",
    "allTypes": "Todos los Tipos",
    "refresh": "Actualizar",
    "autoRefresh": "Actualización automática cada 5 min",
    "noAdmissions": "No hay admisiones activas",
    "daysAdmitted": "{count} día | {count} días",
    "treatingPhysician": "Médico",
    "noRoom": "Sin habitación",

    "medications": {
      "title": "Medicamentos Activos",
      "count": "{count} medicamento activo | {count} medicamentos activos",
      "empty": "No hay órdenes de medicamentos activas",
      "dose": "Dosis",
      "route": "Vía",
      "frequency": "Frecuencia",
      "schedule": "Horario",
      "administer": "Administrar",
      "lastAdmin": "Última administración",
      "neverAdministered": "Aún no administrado",
      "noInventoryLink": "Sin artículo de inventario vinculado"
    },

    "careInstructions": {
      "title": "Indicaciones de Cuidado",
      "count": "{count} indicación activa | {count} indicaciones activas",
      "empty": "No hay indicaciones de cuidado activas",
      "since": "Desde"
    },

    "vitals": {
      "title": "Últimos Signos Vitales",
      "empty": "No hay signos vitales registrados",
      "recordedAt": "Registrado",
      "recordedBy": "Por",
      "hoursAgo": "hace {hours}h",
      "never": "Nunca",
      "overdue": "Atrasado",
      "recordVitals": "Registrar Signos Vitales"
    },

    "notes": {
      "lastNote": "Última Nota",
      "noNotes": "Sin notas de enfermería",
      "addNote": "Agregar Nota"
    },

    "actions": {
      "viewDetail": "Ver Detalle",
      "expand": "Expandir",
      "collapse": "Contraer"
    }
  }
}
```

---

## Implementation Notes

### Backend Architecture

- **New Controller**: `NursingKardexController` with a single `GET /api/v1/nursing-kardex` endpoint
- **New Service**: `NursingKardexService` that orchestrates the batch queries
- **Permission**: Secured with `@PreAuthorize("hasAuthority('admission:read')")` — the endpoint itself only needs admission read; sub-data visibility can be controlled in the service layer
- **No new entities, repositories, or migrations** — this feature is purely an aggregation layer

### Batch Query Pattern

```kotlin
@Service
class NursingKardexService(
    private val admissionRepository: AdmissionRepository,
    private val medicalOrderRepository: MedicalOrderRepository,
    private val medicationAdministrationRepository: MedicationAdministrationRepository,
    private val vitalSignRepository: VitalSignRepository,
    private val nursingNoteRepository: NursingNoteRepository,
    private val userRepository: UserRepository,
) {
    fun getKardexSummaries(type: AdmissionType?, search: String?, pageable: Pageable): Page<KardexAdmissionSummary> {
        // 1. Fetch active admissions (paginated, with joins)
        // 2. Collect admission IDs
        // 3. Batch-fetch: active orders, latest vitals, latest notes, latest administrations
        // 4. Assemble summaries in-memory
        // 5. Return Page<KardexAdmissionSummary>
    }
}
```

### New Repository Methods Needed

The following methods need to be added to **existing** repositories:

```kotlin
// MedicalOrderRepository — add:
@Query("""
    SELECT mo FROM MedicalOrder mo
    LEFT JOIN FETCH mo.inventoryItem
    WHERE mo.admission.id IN :admissionIds
    AND mo.status = 'ACTIVE'
    AND mo.category IN :categories
""")
fun findActiveByAdmissionIdsAndCategories(
    admissionIds: List<Long>,
    categories: List<MedicalOrderCategory>
): List<MedicalOrder>

// VitalSignRepository — add:
@Query(value = """
    SELECT vs.* FROM vital_signs vs
    INNER JOIN (
        SELECT admission_id, MAX(recorded_at) as max_recorded_at
        FROM vital_signs
        WHERE admission_id IN (:admissionIds) AND deleted_at IS NULL
        GROUP BY admission_id
    ) latest ON vs.admission_id = latest.admission_id AND vs.recorded_at = latest.max_recorded_at
    WHERE vs.deleted_at IS NULL
""", nativeQuery = true)
fun findLatestByAdmissionIds(admissionIds: List<Long>): List<VitalSign>

// NursingNoteRepository — add:
@Query(value = """
    SELECT nn.* FROM nursing_notes nn
    INNER JOIN (
        SELECT admission_id, MAX(created_at) as max_created_at
        FROM nursing_notes
        WHERE admission_id IN (:admissionIds) AND deleted_at IS NULL
        GROUP BY admission_id
    ) latest ON nn.admission_id = latest.admission_id AND nn.created_at = latest.max_created_at
    WHERE nn.deleted_at IS NULL
""", nativeQuery = true)
fun findLatestByAdmissionIds(admissionIds: List<Long>): List<NursingNote>

// MedicationAdministrationRepository — add:
@Query(value = """
    SELECT ma.* FROM medication_administrations ma
    INNER JOIN (
        SELECT medical_order_id, MAX(administered_at) as max_administered_at
        FROM medication_administrations
        WHERE medical_order_id IN (:orderIds) AND deleted_at IS NULL
        GROUP BY medical_order_id
    ) latest ON ma.medical_order_id = latest.medical_order_id AND ma.administered_at = latest.max_administered_at
    WHERE ma.deleted_at IS NULL
""", nativeQuery = true)
fun findLatestByMedicalOrderIds(orderIds: List<Long>): List<MedicationAdministration>
```

> **Note on soft deletes in queries**: JPQL queries automatically apply the `@SQLRestriction("deleted_at IS NULL")` filter. Native queries must include explicit `deleted_at IS NULL` conditions (as shown above).

### Patient Name Search

Use the existing `unaccent` PostgreSQL extension (added in V070) for accent-insensitive search:
```sql
WHERE unaccent(lower(p.first_name || ' ' || p.last_name)) LIKE unaccent(lower('%' || :search || '%'))
```

### Role-Based Dashboard Routing (Frontend)

In the router, add conditional redirect logic:

```typescript
// In router/index.ts — modify the /dashboard route
{
  path: '/dashboard',
  name: 'dashboard',
  beforeEnter: (to, from, next) => {
    const authStore = useAuthStore()
    const userRoles = authStore.user?.roles ?? []
    if (userRoles.some(r => ['NURSE', 'CHIEF_NURSE'].includes(r.code))) {
      next({ name: 'nursing-kardex' })
    } else {
      next()
    }
  },
  component: () => import('@/views/DashboardView.vue'),
}
```

### Frontend Component Strategy

- `KardexPatientCard` uses PrimeVue `Panel` or `Accordion` for expand/collapse
- Quick-action dialogs reuse existing form dialog components with `visible` prop control
- After a quick action completes, the store calls `GET /nursing-kardex/{admissionId}` to refresh one card and patches local page data
- Use PrimeVue `Badge` for count indicators and `Tag` for status/type badges
- Use PrimeVue `InputText` with icon for patient name search
- Use PrimeVue `Select` for admission type filter

### Vital Signs Freshness Indicator

```typescript
function getVitalsFreshnessClass(hoursSinceLastVitals: number | null): string {
  if (hoursSinceLastVitals === null) return 'text-red-500'  // never recorded
  if (hoursSinceLastVitals > 8) return 'text-red-500'       // overdue
  if (hoursSinceLastVitals > 4) return 'text-yellow-500'    // approaching
  return 'text-green-500'                                     // recent
}
```

> **Client-side freshness**: The frontend computes `hoursSinceVitals` from `latestVitalSigns.recordedAt` client-side to stay accurate between 5-minute auto-refreshes. The server-provided `hoursSinceLastVitals` value is used for initial render only.

---

## QA Checklist

### Backend
- [ ] `NursingKardexController` created with `GET /api/v1/nursing-kardex` endpoint
- [ ] `GET /nursing-kardex/{admissionId}` returns single summary
- [ ] `GET /nursing-kardex/{admissionId}` returns 404 if admission is not ACTIVE
- [ ] `NursingKardexService` created with batch query logic
- [ ] Response DTOs created (`KardexAdmissionSummary`, `KardexMedicationSummary`, etc.)
- [ ] Batch query for active medical orders by admission IDs
- [ ] Batch query for latest vital signs per admission
- [ ] Batch query for latest nursing notes per admission
- [ ] Batch query for latest medication administrations per order
- [ ] Batch user name resolution
- [ ] Pagination working correctly
- [ ] Admission type filter working
- [ ] Patient name search working (accent-insensitive)
- [ ] `daysAdmitted` calculated correctly
- [ ] `hoursSinceLastVitals` calculated correctly
- [ ] Permission check enforced (`admission:read`)
- [ ] No N+1 queries (verified via SQL logging)
- [ ] DTOs used in controller (no entity exposure)
- [ ] Unit tests written and passing
- [ ] Integration tests written and passing (Testcontainers)
- [ ] Detekt passes (no new violations)
- [ ] OWASP dependency-check passes

### Frontend
- [ ] `NursingKardexView.vue` created
- [ ] `KardexPatientCard.vue` with expand/collapse
- [ ] `KardexMedicationList.vue` with administer button
- [ ] `KardexCareInstructions.vue` with grouped display
- [ ] `KardexVitalsSummary.vue` with freshness indicator
- [ ] `useKardexStore` Pinia store implemented
- [ ] TypeScript types created (`src/types/kardex.ts`)
- [ ] Route `/nursing-kardex` configured
- [ ] Role-based redirect from `/dashboard` for NURSE/CHIEF_NURSE
- [ ] Admission type filter working
- [ ] Patient name search working
- [ ] Pagination working
- [ ] Quick-administer medication opens existing MAR dialog
- [ ] Quick-record vitals opens existing vital signs dialog
- [ ] Quick-add note opens existing nursing note dialog
- [ ] Card refreshes after quick action completion
- [ ] Auto-refresh every 5 minutes
- [ ] Manual refresh button works
- [ ] Empty states displayed correctly
- [ ] Vitals freshness color indicators (green/yellow/red)
- [ ] Error handling implemented
- [ ] ESLint/oxlint passes
- [ ] i18n keys added for EN and ES
- [ ] Unit tests written and passing (Vitest)

### E2E Tests (Playwright)
- [ ] Nurse login redirects to kardex dashboard
- [ ] Kardex displays active admissions with summary data
- [ ] Expanding a card shows medications, care instructions, and vitals
- [ ] Filter by admission type works
- [ ] Search by patient name works
- [ ] Quick-administer medication flow
- [ ] Quick-record vitals flow
- [ ] Quick-add nursing note flow
- [ ] Navigate to admission detail from kardex
- [ ] Empty state when no admissions
- [ ] Non-nurse role cannot access kardex (redirected)
- [ ] i18n verified (ES and EN)

### General
- [ ] API contract documented
- [ ] No new migrations needed (verified)
- [ ] Feature documentation updated
- [ ] Reviewed by project owner

---

## Documentation Updates Required

### Must Update

- [ ] **[CLAUDE.md](../../CLAUDE.md)**
  - Add "Nursing Kardex Dashboard" to "Implemented Features" backend and frontend sections
  - Add `/nursing-kardex` route description

### Review for Consistency

- [ ] **[README.md](../../web/README.md)**
  - Check if setup instructions need updates

### Code Documentation

- [ ] **`NursingKardexService.kt`** — Document batch query strategy and performance considerations
- [ ] **`NursingKardexController.kt`** — Document endpoint parameters and role routing

---

## Design Decisions

### Why a dedicated endpoint instead of multiple frontend calls?

The kardex needs data from 5+ tables for each admission. If the frontend called existing endpoints per admission (medical orders, vital signs, nursing notes, etc.), it would make N x 5 HTTP requests for N patients. A single aggregated endpoint reduces this to 1 request with ~6 SQL queries total, regardless of patient count.

### Why no new database tables?

The kardex is a read-only aggregation view. All data already exists in the system. Adding denormalized summary tables would create consistency risks and maintenance burden. The batch-query approach keeps data authoritative and avoids sync issues.

### Why role-based routing instead of a nav menu item?

Nurses should land on the kardex by default — it's their primary workspace. Adding it as a nav item they must manually navigate to defeats the purpose. The `/dashboard` redirect ensures they always start in the right place while keeping the standard dashboard available for other roles.

### Why expand/collapse cards instead of a table?

A table row cannot hold the medication list, care instructions, and vital signs detail. Cards provide a two-level information hierarchy: scan the collapsed rows for quick triage, then expand specific patients for action. This mirrors how nurses actually work — they scan all patients, then focus on one at a time.

### Why auto-refresh every 5 minutes?

Hospital situations change frequently. Medications get administered by other nurses, vitals get recorded, new orders are written by doctors. A 5-minute refresh interval keeps the kardex current without excessive server load. Manual refresh is available for immediate updates after a known change.

### Why no duplicate prevention for medication administration?

Each medication administration is a separate event (different time, different nurse, different observation). The kardex is a display/convenience layer, not the enforcement point for medication safety rules. Strict duplicate prevention (e.g., "cannot administer same medication within X hours") would require clinical workflow rules that are out of scope for this feature and would be a separate future feature if needed.

### Why patient name search uses unaccent?

The hospital operates in Guatemala where patient names frequently include accented characters (á, é, í, ó, ú, ñ). Accent-insensitive search ensures nurses can find patients regardless of accent accuracy when typing quickly.

---

## Related Docs/Commits/Issues

- Related feature: [Nursing Module](nursing-module.md) — Nursing notes and vital signs (data sources)
- Related feature: [Clinical Event Billing Automation](clinical-event-billing-automation.md) — Medication administration (quick-administer flow)
- Related feature: [Medical/Psychiatric Record](medical-psychiatric-record.md) — Medical orders (data source)
- Related feature: [Patient Admission](patient-admission.md) — Admission entity (primary data source)

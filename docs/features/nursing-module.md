# Feature: Nursing Module

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-02-05 | @author | Initial draft |
| 1.1 | 2026-02-05 | @claude | Added recordedAt field, BP physiological validation, discharge checks, chart endpoint, fixed permission migration, documented canEdit logic |

---

## Overview

The Nursing Module allows nursing staff and doctors to document patient care during hospital stays. It includes two key components: (1) **Nursing Notes** - free-text observations and care documentation recorded multiple times daily, and (2) **Vital Signs** - structured physiological measurements (blood pressure, heart rate, respiratory rate, temperature, oxygen saturation) recorded throughout the day, displayed in both tabular and chart formats. All records are auditable and immutable after a 24-hour edit window.

---

## Use Case / User Story

### Nursing Notes
1. As a nurse, I want to create nursing notes for a patient's admission so that I can document observations and care provided throughout their stay.
2. As a nurse, I want to view all nursing notes for an admission in chronological order so that I can review the patient's care history.
3. As a nurse, I want to edit my nursing notes within 24 hours so that I can correct or add information if needed.
4. As a doctor, I want to view and create nursing notes so that I can document and understand the patient's ongoing care and condition.

### Vital Signs
5. As a nurse, I want to record vital signs for a patient with a specific measurement time so that I can accurately document when vitals were taken (vs. when entered into system).
6. As a nurse, I want to view vital signs in a table format so that I can quickly review recent measurements.
7. As a nurse, I want to view vital signs as charts so that I can visualize trends in the patient's condition.
8. As a doctor, I want to view vital sign trends in chart format so that I can assess the patient's progress and make clinical decisions.
9. As a nurse, I want to filter vital signs by date range so that I can focus on a specific time period.

### Audit
10. As an administrator, I want to see who recorded each nursing note and vital sign entry so that I can maintain accountability and audit trails.

---

## Authorization / Role Access

| Action | Required Role(s) | Permission | Notes |
|--------|------------------|------------|-------|
| View nursing notes | NURSE, DOCTOR, CHIEF_NURSE, ADMIN | `nursing-note:read` | All with permission |
| Create nursing note | NURSE, DOCTOR, CHIEF_NURSE, ADMIN | `nursing-note:create` | Active admissions only |
| Edit nursing note | NURSE, DOCTOR, CHIEF_NURSE | `nursing-note:update` | Own records only, within 24h, active admissions only |
| Edit nursing note (any) | ADMIN | `nursing-note:update` | Can edit any record, anytime, active admissions only |
| View vital signs | NURSE, DOCTOR, CHIEF_NURSE, ADMIN | `vital-sign:read` | All with permission |
| Create vital signs | NURSE, DOCTOR, CHIEF_NURSE, ADMIN | `vital-sign:create` | Active admissions only |
| Edit vital signs | NURSE, DOCTOR, CHIEF_NURSE | `vital-sign:update` | Own records only, within 24h, active admissions only |
| Edit vital signs (any) | ADMIN | `vital-sign:update` | Can edit any record, anytime, active admissions only |

**Notes:**
- No delete operations are allowed to maintain medical record integrity.
- All write operations (create/update) are blocked for discharged admissions.

---

## Functional Requirements

### Nursing Notes
- Create nursing notes with timestamp and description (required, max 5000 characters)
- View list of all nursing notes for an admission, sorted by creation date (newest first)
- Edit existing nursing notes (creator only, within 24 hours; admins can edit anytime)
- Track creator (`createdBy`) and editor (`updatedBy`) for audit trail
- Pagination support (default 20 per page)
- No delete functionality
- **Discharge protection**: Cannot create or edit notes for discharged admissions

### Vital Signs
- Record vital signs with measurements and timestamps:
  - **Recorded At** (Fecha/Hora de Registro): optional datetime, defaults to current time if not provided. Represents when the measurement was physically taken (may differ from system entry time).
  - **Systolic BP** (PA Sistólica): required, integer, 60-250 mmHg
  - **Diastolic BP** (PA Diastólica): required, integer, 30-150 mmHg
  - **Blood Pressure Validation**: systolic must be greater than diastolic (physiological constraint)
  - **Heart Rate** (Frecuencia Cardiaca): required, integer, 20-250 bpm
  - **Respiratory Rate** (Frecuencia Respiratoria): required, integer, 5-60 breaths/min
  - **Temperature** (Temperatura): required, decimal (1 decimal place), 30.0-45.0 °C
  - **Oxygen Saturation** (Oximetría): required, integer, 50-100 %
  - **Other** (Otros): optional, max 1000 characters
- View vital signs in tabular format (chronological by `recordedAt`, newest first)
- View vital signs in chart format (line graphs over time for each measurement)
- Display blood pressure as combined "systolic/diastolic" format in table
- Edit existing vital sign records (creator only, within 24 hours; admins can edit anytime)
- Track creator and editor for audit trail
- Pagination support for table view (default 20 per page)
- **Chart data endpoint**: Non-paginated endpoint to fetch all vital signs for charting
- **Date range filtering**: Optional `fromDate` and `toDate` query parameters
- No delete functionality
- **Discharge protection**: Cannot create or edit vital signs for discharged admissions

### Edit Time Limit
- 24-hour edit window from creation time (based on `createdAt`, not `recordedAt`)
- After 24 hours, records become read-only (except for admins)
- UI should display "edit window closed" message after time limit
- All edits logged via AuditEntityListener

### canEdit Logic
The `canEdit` field in API responses is computed as follows:
```
canEdit = (
  admission.status == ACTIVE
  AND (
    currentUser.isAdmin
    OR (
      entity.createdBy == currentUser.id
      AND entity.createdAt + 24 hours > now()
    )
  )
)
```

---

## Acceptance Criteria / Scenarios

### Nursing Notes - Happy Path
- When a nurse creates a nursing note with valid description, the system returns 201 Created with the note data including `createdBy` info.
- When a user views nursing notes for an admission, the system returns a paginated list sorted by `createdAt` descending.
- When the creator edits their note within 24 hours, the system updates the note and returns 200 OK with `updatedBy` info.
- When an admin edits any note at any time, the system updates the note and returns 200 OK.

### Nursing Notes - Edge Cases
- When creating a note with empty description, return 400 Bad Request with localized message "Description is required" / "La descripción es requerida".
- When creating a note with description > 5000 characters, return 400 Bad Request with localized message.
- When editing a note after 24 hours (non-admin), return 403 Forbidden with message "This record can no longer be edited (24-hour limit exceeded)" / "Este registro ya no puede ser editado (límite de 24 horas excedido)".
- When editing a note created by another user (non-admin), return 403 Forbidden with message "You can only edit records you created" / "Solo puede editar registros que usted creó".
- When editing a non-existent note, return 404 Not Found.
- When a user without permission attempts access, return 403 Forbidden.
- **When creating or editing a note for a discharged admission, return 400 Bad Request with message "Cannot modify records for discharged admissions" / "No se pueden modificar registros de admisiones dadas de alta".**

### Vital Signs - Happy Path
- When a nurse records vital signs with all valid measurements, the system returns 201 Created with the vital signs data.
- When recording vital signs with a specific `recordedAt` time, the system stores that time separately from `createdAt`.
- When recording vital signs without `recordedAt`, the system defaults to the current timestamp.
- When viewing vital signs in table format, the system returns a paginated list with blood pressure displayed as "systolic/diastolic".
- When viewing vital signs charts, the system returns all records (non-paginated) for the admission.
- When the creator edits their vital signs within 24 hours, the system updates and returns 200 OK.
- When filtering by date range, only records with `recordedAt` within the range are returned.

### Vital Signs - Edge Cases
- When systolic BP is outside 60-250 range, return 400 Bad Request with message "Systolic blood pressure must be between 60-250 mmHg" / "La presión arterial sistólica debe estar entre 60-250 mmHg".
- When diastolic BP is outside 30-150 range, return 400 Bad Request with localized message.
- **When systolic BP <= diastolic BP, return 400 Bad Request with message "Systolic blood pressure must be greater than diastolic" / "La presión arterial sistólica debe ser mayor que la diastólica".**
- When heart rate is outside 20-250 range, return 400 Bad Request with localized message.
- When respiratory rate is outside 5-60 range, return 400 Bad Request with localized message.
- When temperature is outside 30.0-45.0 range, return 400 Bad Request with localized message.
- When oxygen saturation is outside 50-100 range, return 400 Bad Request with localized message.
- When a required vital sign field is missing, return 400 Bad Request with localized message.
- When editing vital signs after 24 hours (non-admin), return 403 Forbidden with time limit message.
- When editing vital signs created by another user (non-admin), return 403 Forbidden.
- **When creating or editing vital signs for a discharged admission, return 400 Bad Request with discharge message.**
- **When `recordedAt` is in the future, return 400 Bad Request with message "Recorded time cannot be in the future" / "La hora de registro no puede ser en el futuro".**

### Charts
- Line charts display correctly for each vital sign type over time (6 separate charts: BP combined, HR, RR, Temp, SpO2).
- Blood pressure chart displays both systolic and diastolic as two lines on the same chart.
- X-axis shows `recordedAt` timestamps, Y-axis shows measurement values with appropriate units.
- Charts handle a single data point gracefully (display as a point marker).
- Charts display "No data available" / "No hay datos disponibles" message when empty.
- Charts use different colors for each measurement type for visual distinction.
- Default view shows last 7 days of data; user can expand to full admission history.

---

## Non-Functional Requirements

- **Performance**: List endpoints respond in < 200ms for typical page sizes
- **Performance**: Chart endpoint responds in < 500ms for up to 1000 data points
- **Security**: All inputs validated; audit logging for all create/update operations
- **Reliability**: All operations logged via existing AuditEntityListener
- **Localization**: Full EN/ES support for all UI text and error messages
- **Concurrency**: No optimistic locking (consistent with existing codebase patterns)

---

## API Contract

### Nursing Notes

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| GET | `/api/v1/admissions/{admissionId}/nursing-notes` | - | `Page<NursingNoteResponse>` | Yes | List nursing notes (paginated) |
| GET | `/api/v1/admissions/{admissionId}/nursing-notes/{noteId}` | - | `NursingNoteResponse` | Yes | Get note by ID |
| POST | `/api/v1/admissions/{admissionId}/nursing-notes` | `CreateNursingNoteRequest` | `NursingNoteResponse` | Yes | Create nursing note |
| PUT | `/api/v1/admissions/{admissionId}/nursing-notes/{noteId}` | `UpdateNursingNoteRequest` | `NursingNoteResponse` | Yes | Update nursing note |

### Vital Signs

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| GET | `/api/v1/admissions/{admissionId}/vital-signs` | - | `Page<VitalSignResponse>` | Yes | List vital signs (paginated, for table view) |
| GET | `/api/v1/admissions/{admissionId}/vital-signs/chart` | - | `List<VitalSignResponse>` | Yes | List all vital signs (non-paginated, for charts) |
| GET | `/api/v1/admissions/{admissionId}/vital-signs/{vitalSignId}` | - | `VitalSignResponse` | Yes | Get vital sign by ID |
| POST | `/api/v1/admissions/{admissionId}/vital-signs` | `CreateVitalSignRequest` | `VitalSignResponse` | Yes | Record vital signs |
| PUT | `/api/v1/admissions/{admissionId}/vital-signs/{vitalSignId}` | `UpdateVitalSignRequest` | `VitalSignResponse` | Yes | Update vital signs |

### Query Parameters

**Vital Signs List (both endpoints)**:
- `fromDate` (optional): ISO date, filter records with `recordedAt >= fromDate`
- `toDate` (optional): ISO date, filter records with `recordedAt <= toDate`

**Paginated endpoint only**:
- `page` (default: 0): Page number
- `size` (default: 20): Page size
- `sort` (default: `recordedAt,desc`): Sort field and direction

### Request/Response Examples

```json
// POST /api/v1/admissions/1/nursing-notes - Request
{
  "description": "Patient resting comfortably. Administered pain medication at 14:00. No complaints."
}

// Response
{
  "id": 1,
  "description": "Patient resting comfortably. Administered pain medication at 14:00. No complaints.",
  "createdAt": "2026-02-05T14:30:00Z",
  "updatedAt": "2026-02-05T14:30:00Z",
  "createdBy": {
    "id": 5,
    "fullName": "Maria García"
  },
  "updatedBy": {
    "id": 5,
    "fullName": "Maria García"
  },
  "canEdit": true
}
```

```json
// POST /api/v1/admissions/1/vital-signs - Request
{
  "recordedAt": "2026-02-05T08:00:00Z",
  "systolicBp": 130,
  "diastolicBp": 70,
  "heartRate": 54,
  "respiratoryRate": 18,
  "temperature": 36.2,
  "oxygenSaturation": 94,
  "other": "Patient alert and oriented"
}

// Response
{
  "id": 1,
  "recordedAt": "2026-02-05T08:00:00Z",
  "systolicBp": 130,
  "diastolicBp": 70,
  "heartRate": 54,
  "respiratoryRate": 18,
  "temperature": 36.2,
  "oxygenSaturation": 94,
  "other": "Patient alert and oriented",
  "createdAt": "2026-02-05T08:15:00Z",
  "updatedAt": "2026-02-05T08:15:00Z",
  "createdBy": {
    "id": 5,
    "fullName": "Maria García"
  },
  "updatedBy": {
    "id": 5,
    "fullName": "Maria García"
  },
  "canEdit": true
}
```

```json
// GET /api/v1/admissions/1/vital-signs/chart?fromDate=2026-02-01&toDate=2026-02-05 - Response
[
  {
    "id": 1,
    "recordedAt": "2026-02-05T08:00:00Z",
    "systolicBp": 130,
    "diastolicBp": 70,
    "heartRate": 54,
    "respiratoryRate": 18,
    "temperature": 36.2,
    "oxygenSaturation": 94,
    "other": "Patient alert and oriented",
    "createdAt": "2026-02-05T08:15:00Z",
    "updatedAt": "2026-02-05T08:15:00Z",
    "createdBy": {
      "id": 5,
      "fullName": "Maria García"
    },
    "updatedBy": {
      "id": 5,
      "fullName": "Maria García"
    },
    "canEdit": true
  }
]
```

---

## Database Changes

### New Entities

| Entity | Table | Extends | Description |
|--------|-------|---------|-------------|
| `NursingNote` | `nursing_notes` | `BaseEntity` | Nursing observations and care notes |
| `VitalSign` | `vital_signs` | `BaseEntity` | Patient vital sign measurements |

### New Migrations

| Migration | Description |
|-----------|-------------|
| `V043__create_nursing_notes_table.sql` | Creates nursing_notes table |
| `V044__create_vital_signs_table.sql` | Creates vital_signs table with recorded_at field |
| `V045__add_nursing_permissions.sql` | Adds nursing-note and vital-sign permissions with role assignments |

### Schema

```sql
-- V043__create_nursing_notes_table.sql
CREATE TABLE nursing_notes (
    id BIGSERIAL PRIMARY KEY,
    admission_id BIGINT NOT NULL,
    description TEXT NOT NULL,
    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_nursing_notes_admission_id FOREIGN KEY (admission_id) REFERENCES admissions(id),
    CONSTRAINT fk_nursing_notes_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_nursing_notes_updated_by FOREIGN KEY (updated_by) REFERENCES users(id)
);

CREATE INDEX idx_nursing_notes_admission_id ON nursing_notes(admission_id);
CREATE INDEX idx_nursing_notes_created_at ON nursing_notes(created_at);
CREATE INDEX idx_nursing_notes_deleted_at ON nursing_notes(deleted_at);

-- V044__create_vital_signs_table.sql
CREATE TABLE vital_signs (
    id BIGSERIAL PRIMARY KEY,
    admission_id BIGINT NOT NULL,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    systolic_bp INTEGER NOT NULL,
    diastolic_bp INTEGER NOT NULL,
    heart_rate INTEGER NOT NULL,
    respiratory_rate INTEGER NOT NULL,
    temperature DECIMAL(4,1) NOT NULL,
    oxygen_saturation INTEGER NOT NULL,
    other VARCHAR(1000),
    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_vital_signs_admission_id FOREIGN KEY (admission_id) REFERENCES admissions(id),
    CONSTRAINT fk_vital_signs_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_vital_signs_updated_by FOREIGN KEY (updated_by) REFERENCES users(id),
    CONSTRAINT chk_vital_signs_bp CHECK (systolic_bp > diastolic_bp)
);

CREATE INDEX idx_vital_signs_admission_id ON vital_signs(admission_id);
CREATE INDEX idx_vital_signs_recorded_at ON vital_signs(recorded_at);
CREATE INDEX idx_vital_signs_deleted_at ON vital_signs(deleted_at);

-- V045__add_nursing_permissions.sql
-- Create permissions with code column (following existing pattern)
INSERT INTO permissions (code, name, description, created_at, updated_at) VALUES
('nursing-note:read', 'nursing-note:read', 'View nursing notes', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('nursing-note:create', 'nursing-note:create', 'Create nursing notes', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('nursing-note:update', 'nursing-note:update', 'Update nursing notes', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('vital-sign:read', 'vital-sign:read', 'View vital signs', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('vital-sign:create', 'vital-sign:create', 'Record vital signs', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('vital-sign:update', 'vital-sign:update', 'Update vital signs', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Assign permissions to ADMIN role (all permissions)
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMIN' AND p.code IN (
    'nursing-note:read', 'nursing-note:create', 'nursing-note:update',
    'vital-sign:read', 'vital-sign:create', 'vital-sign:update'
);

-- Assign permissions to DOCTOR role
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'DOCTOR' AND p.code IN (
    'nursing-note:read', 'nursing-note:create', 'nursing-note:update',
    'vital-sign:read', 'vital-sign:create', 'vital-sign:update'
);

-- Assign permissions to NURSE role
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'NURSE' AND p.code IN (
    'nursing-note:read', 'nursing-note:create', 'nursing-note:update',
    'vital-sign:read', 'vital-sign:create', 'vital-sign:update'
);

-- Assign permissions to CHIEF_NURSE role
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'CHIEF_NURSE' AND p.code IN (
    'nursing-note:read', 'nursing-note:create', 'nursing-note:update',
    'vital-sign:read', 'vital-sign:create', 'vital-sign:update'
);
```

### Index Requirements

- [x] `deleted_at` - Required for soft delete queries
- [x] `admission_id` - Foreign key, frequently queried
- [x] `recorded_at` (vital_signs) - For chronological sorting and date range filtering
- [x] `created_at` (nursing_notes) - For chronological sorting

### Database Constraints

- [x] `chk_vital_signs_bp` - Ensures systolic_bp > diastolic_bp at database level

---

## Frontend Changes

### Components

| Component | Location | Description |
|-----------|----------|-------------|
| `NursingTab.vue` | `src/components/admission/nursing/` | Container tab for nursing module |
| `NursingNoteList.vue` | `src/components/admission/nursing/` | List of nursing notes with pagination |
| `NursingNoteFormDialog.vue` | `src/components/admission/nursing/` | Create/Edit dialog for nursing notes |
| `VitalSignTable.vue` | `src/components/admission/nursing/` | Tabular view of vital signs |
| `VitalSignFormDialog.vue` | `src/components/admission/nursing/` | Create/Edit dialog for vital signs |
| `VitalSignCharts.vue` | `src/components/admission/nursing/` | Line charts for vital sign trends |
| `VitalSignDateFilter.vue` | `src/components/admission/nursing/` | Date range picker for filtering |

### Pinia Stores

| Store | Location | Description |
|-------|----------|-------------|
| `useNursingNoteStore` | `src/stores/nursingNote.ts` | State management for nursing notes |
| `useVitalSignStore` | `src/stores/vitalSign.ts` | State management for vital signs (includes separate chart data fetch) |

### Routes

No new routes required. The nursing module is integrated as a tab within the existing Admission Detail view (`/admissions/:id`).

### Validation (Zod Schemas)

```typescript
// src/schemas/nursingNote.ts
import { z } from 'zod'

export const nursingNoteSchema = z.object({
  description: z.string()
    .min(1, 'validation.required')
    .max(5000, 'validation.maxLength'),
})

// src/schemas/vitalSign.ts
import { z } from 'zod'

export const vitalSignSchema = z.object({
  recordedAt: z.string().datetime().optional(),
  systolicBp: z.number()
    .int()
    .min(60, 'nursing.vitalSigns.validation.systolicBpMin')
    .max(250, 'nursing.vitalSigns.validation.systolicBpMax'),
  diastolicBp: z.number()
    .int()
    .min(30, 'nursing.vitalSigns.validation.diastolicBpMin')
    .max(150, 'nursing.vitalSigns.validation.diastolicBpMax'),
  heartRate: z.number()
    .int()
    .min(20, 'nursing.vitalSigns.validation.heartRateMin')
    .max(250, 'nursing.vitalSigns.validation.heartRateMax'),
  respiratoryRate: z.number()
    .int()
    .min(5, 'nursing.vitalSigns.validation.respiratoryRateMin')
    .max(60, 'nursing.vitalSigns.validation.respiratoryRateMax'),
  temperature: z.number()
    .min(30.0, 'nursing.vitalSigns.validation.temperatureMin')
    .max(45.0, 'nursing.vitalSigns.validation.temperatureMax'),
  oxygenSaturation: z.number()
    .int()
    .min(50, 'nursing.vitalSigns.validation.oxygenSaturationMin')
    .max(100, 'nursing.vitalSigns.validation.oxygenSaturationMax'),
  other: z.string().max(1000).optional(),
}).refine(
  (data) => data.systolicBp > data.diastolicBp,
  {
    message: 'nursing.vitalSigns.validation.systolicGreaterThanDiastolic',
    path: ['systolicBp'],
  }
)
```

### i18n Keys

```json
// English (en.json)
{
  "nursing": {
    "title": "Nursing",
    "tabs": {
      "notes": "Nursing Notes",
      "vitalSigns": "Vital Signs"
    },
    "notes": {
      "title": "Nursing Notes",
      "add": "Add Note",
      "edit": "Edit Note",
      "description": "Description",
      "noNotes": "No nursing notes recorded",
      "createdBy": "Recorded by",
      "editWindowClosed": "Edit window closed (24-hour limit exceeded)",
      "cannotEditOthers": "You can only edit records you created",
      "admissionDischarged": "Cannot modify records for discharged admissions"
    },
    "vitalSigns": {
      "title": "Vital Signs",
      "add": "Record Vital Signs",
      "edit": "Edit Vital Signs",
      "table": "Table View",
      "charts": "Charts",
      "noData": "No vital signs recorded",
      "fields": {
        "recordedAt": "Recorded At",
        "bloodPressure": "Blood Pressure (s/d)",
        "systolicBp": "Systolic BP",
        "diastolicBp": "Diastolic BP",
        "heartRate": "Heart Rate",
        "respiratoryRate": "Respiratory Rate",
        "temperature": "Temperature",
        "oxygenSaturation": "Oxygen Saturation",
        "other": "Other Observations"
      },
      "units": {
        "mmHg": "mmHg",
        "bpm": "bpm",
        "breathsPerMin": "breaths/min",
        "celsius": "°C",
        "percent": "%"
      },
      "validation": {
        "systolicBpMin": "Systolic BP must be at least 60 mmHg",
        "systolicBpMax": "Systolic BP must be at most 250 mmHg",
        "diastolicBpMin": "Diastolic BP must be at least 30 mmHg",
        "diastolicBpMax": "Diastolic BP must be at most 150 mmHg",
        "systolicGreaterThanDiastolic": "Systolic BP must be greater than diastolic BP",
        "heartRateMin": "Heart rate must be at least 20 bpm",
        "heartRateMax": "Heart rate must be at most 250 bpm",
        "respiratoryRateMin": "Respiratory rate must be at least 5 breaths/min",
        "respiratoryRateMax": "Respiratory rate must be at most 60 breaths/min",
        "temperatureMin": "Temperature must be at least 30.0°C",
        "temperatureMax": "Temperature must be at most 45.0°C",
        "oxygenSaturationMin": "Oxygen saturation must be at least 50%",
        "oxygenSaturationMax": "Oxygen saturation must be at most 100%",
        "recordedAtFuture": "Recorded time cannot be in the future"
      },
      "editWindowClosed": "Edit window closed (24-hour limit exceeded)",
      "cannotEditOthers": "You can only edit records you created",
      "admissionDischarged": "Cannot modify records for discharged admissions",
      "filter": {
        "dateRange": "Date Range",
        "from": "From",
        "to": "To",
        "last7Days": "Last 7 Days",
        "last30Days": "Last 30 Days",
        "allTime": "All Time",
        "apply": "Apply Filter",
        "clear": "Clear"
      },
      "charts": {
        "bloodPressure": "Blood Pressure",
        "heartRate": "Heart Rate",
        "respiratoryRate": "Respiratory Rate",
        "temperature": "Temperature",
        "oxygenSaturation": "Oxygen Saturation",
        "systolic": "Systolic",
        "diastolic": "Diastolic"
      }
    }
  }
}
```

```json
// Spanish (es.json)
{
  "nursing": {
    "title": "Enfermería",
    "tabs": {
      "notes": "Notas de Enfermería",
      "vitalSigns": "Signos Vitales"
    },
    "notes": {
      "title": "Notas de Enfermería",
      "add": "Agregar Nota",
      "edit": "Editar Nota",
      "description": "Descripción",
      "noNotes": "No hay notas de enfermería registradas",
      "createdBy": "Registrado por",
      "editWindowClosed": "Ventana de edición cerrada (límite de 24 horas excedido)",
      "cannotEditOthers": "Solo puede editar registros que usted creó",
      "admissionDischarged": "No se pueden modificar registros de admisiones dadas de alta"
    },
    "vitalSigns": {
      "title": "Signos Vitales",
      "add": "Registrar Signos Vitales",
      "edit": "Editar Signos Vitales",
      "table": "Vista de Tabla",
      "charts": "Gráficos",
      "noData": "No hay signos vitales registrados",
      "fields": {
        "recordedAt": "Fecha/Hora de Registro",
        "bloodPressure": "Presión Arterial (s/d)",
        "systolicBp": "PA Sistólica",
        "diastolicBp": "PA Diastólica",
        "heartRate": "Frecuencia Cardiaca",
        "respiratoryRate": "Frecuencia Respiratoria",
        "temperature": "Temperatura",
        "oxygenSaturation": "Oximetría",
        "other": "Otras Observaciones"
      },
      "units": {
        "mmHg": "mmHg",
        "bpm": "lpm",
        "breathsPerMin": "resp/min",
        "celsius": "°C",
        "percent": "%"
      },
      "validation": {
        "systolicBpMin": "La PA sistólica debe ser al menos 60 mmHg",
        "systolicBpMax": "La PA sistólica debe ser máximo 250 mmHg",
        "diastolicBpMin": "La PA diastólica debe ser al menos 30 mmHg",
        "diastolicBpMax": "La PA diastólica debe ser máximo 150 mmHg",
        "systolicGreaterThanDiastolic": "La PA sistólica debe ser mayor que la PA diastólica",
        "heartRateMin": "La frecuencia cardiaca debe ser al menos 20 lpm",
        "heartRateMax": "La frecuencia cardiaca debe ser máximo 250 lpm",
        "respiratoryRateMin": "La frecuencia respiratoria debe ser al menos 5 resp/min",
        "respiratoryRateMax": "La frecuencia respiratoria debe ser máximo 60 resp/min",
        "temperatureMin": "La temperatura debe ser al menos 30.0°C",
        "temperatureMax": "La temperatura debe ser máximo 45.0°C",
        "oxygenSaturationMin": "La oximetría debe ser al menos 50%",
        "oxygenSaturationMax": "La oximetría debe ser máximo 100%",
        "recordedAtFuture": "La hora de registro no puede ser en el futuro"
      },
      "editWindowClosed": "Ventana de edición cerrada (límite de 24 horas excedido)",
      "cannotEditOthers": "Solo puede editar registros que usted creó",
      "admissionDischarged": "No se pueden modificar registros de admisiones dadas de alta",
      "filter": {
        "dateRange": "Rango de Fechas",
        "from": "Desde",
        "to": "Hasta",
        "last7Days": "Últimos 7 Días",
        "last30Days": "Últimos 30 Días",
        "allTime": "Todo el Historial",
        "apply": "Aplicar Filtro",
        "clear": "Limpiar"
      },
      "charts": {
        "bloodPressure": "Presión Arterial",
        "heartRate": "Frecuencia Cardiaca",
        "respiratoryRate": "Frecuencia Respiratoria",
        "temperature": "Temperatura",
        "oxygenSaturation": "Oximetría",
        "systolic": "Sistólica",
        "diastolic": "Diastólica"
      }
    }
  }
}
```

---

## Implementation Notes

### Backend Patterns to Follow
- Follow existing patterns in `ProgressNoteController`, `ProgressNoteService`, `ProgressNoteRepository`
- Entities must extend `BaseEntity` and use `@SQLRestriction("deleted_at IS NULL")`
- Use DTOs with companion object `from()` factory functions
- Batch-fetch users to avoid N+1 queries in list endpoints
- Use `@PreAuthorize` for permission checks

### Admission Discharge Check Implementation
```kotlin
// In service layer - check before create/update
private fun validateAdmissionActive(admission: Admission) {
    if (admission.isDischarged()) {
        throw BadRequestException(messageService.getMessage("error.admission.discharged"))
    }
}
```

### Edit Permission Check Implementation
```kotlin
// In service layer
private fun validateEditPermission(entity: BaseEntity, currentUserId: Long, isAdmin: Boolean) {
    if (isAdmin) return

    if (entity.createdBy != currentUserId) {
        throw ForbiddenException(messageService.getMessage("error.cannotEditOthers"))
    }

    val editWindowHours = 24L
    if (!entity.createdAt.plusHours(editWindowHours).isAfter(LocalDateTime.now())) {
        throw ForbiddenException(messageService.getMessage("error.editWindowClosed"))
    }
}

// For computing canEdit in DTO
fun canEdit(entity: BaseEntity, currentUserId: Long, isAdmin: Boolean, admissionActive: Boolean): Boolean {
    if (!admissionActive) return false
    if (isAdmin) return true
    if (entity.createdBy != currentUserId) return false
    val editWindowHours = 24L
    return entity.createdAt.plusHours(editWindowHours).isAfter(LocalDateTime.now())
}
```

### Blood Pressure Validation (Backend)
```kotlin
// In CreateVitalSignRequest / UpdateVitalSignRequest
fun validate() {
    if (systolicBp <= diastolicBp) {
        throw BadRequestException(messageService.getMessage("error.bp.systolicGreaterThanDiastolic"))
    }
}
```

### RecordedAt Validation (Backend)
```kotlin
// In service layer
private fun validateRecordedAt(recordedAt: LocalDateTime?) {
    if (recordedAt != null && recordedAt.isAfter(LocalDateTime.now())) {
        throw BadRequestException(messageService.getMessage("error.recordedAtFuture"))
    }
}
```

### Frontend Patterns to Follow
- Follow existing patterns in `MedicalRecordTabs.vue`, `ProgressNoteFormDialog.vue`
- Add "Nursing" tab to `MedicalRecordTabs.vue`
- Use PrimeVue DataTable for tabular view
- Use PrimeVue Chart component (Chart.js wrapper) for vital sign charts
- Check `canEdit` field from API response to show/hide edit buttons
- Hide create/edit buttons when admission is discharged

### Chart Configuration
- Use existing PrimeVue Chart component (wraps Chart.js)
- 5 separate chart panels:
  1. Blood Pressure (two lines: systolic in red, diastolic in blue)
  2. Heart Rate (single line, green)
  3. Respiratory Rate (single line, orange)
  4. Temperature (single line, purple)
  5. Oxygen Saturation (single line, cyan)
- X-axis: `recordedAt` timestamp, formatted as "MM/DD HH:mm"
- Y-axis: measurement value with unit label
- Include data point markers for easy reading
- Responsive design for different screen sizes

---

## QA Checklist

### Backend
- [ ] All functional requirements implemented
- [ ] `NursingNote` entity extends `BaseEntity`
- [ ] `VitalSign` entity extends `BaseEntity`
- [ ] Both entities have `@SQLRestriction("deleted_at IS NULL")`
- [ ] `VitalSign` has `recordedAt` field
- [ ] DTOs used in controllers (no entity exposure)
- [ ] Input validation in place (ranges, required fields)
- [ ] Blood pressure physiological validation (systolic > diastolic)
- [ ] RecordedAt future validation
- [ ] Edit time limit (24h) enforced
- [ ] Creator-only edit restriction enforced
- [ ] Admin override for edit restrictions
- [ ] **Discharge protection enforced for create/update**
- [ ] Chart endpoint returns non-paginated list
- [ ] Date range filtering works correctly
- [ ] Unit tests written and passing
- [ ] Integration tests written and passing (Testcontainers)
- [ ] Detekt passes (no new violations)
- [ ] OWASP dependency-check passes

### Frontend
- [ ] `NursingTab.vue` component created
- [ ] `NursingNoteList.vue` component created
- [ ] `NursingNoteFormDialog.vue` component created
- [ ] `VitalSignTable.vue` component created
- [ ] `VitalSignFormDialog.vue` component created
- [ ] `VitalSignCharts.vue` component created
- [ ] `VitalSignDateFilter.vue` component created
- [ ] Pinia stores implemented (`nursingNote`, `vitalSign`)
- [ ] Nursing tab added to `MedicalRecordTabs.vue`
- [ ] Form validation with VeeValidate + Zod
- [ ] Blood pressure cross-field validation (systolic > diastolic)
- [ ] Error handling implemented with localized messages
- [ ] Edit button visibility based on `canEdit` response field
- [ ] **Create/Edit buttons hidden for discharged admissions**
- [ ] Date range filter component works
- [ ] Charts render correctly with multiple data points
- [ ] Charts handle single data point
- [ ] Charts show empty state message
- [ ] ESLint/oxlint passes
- [ ] i18n keys added for EN and ES
- [ ] Unit tests written and passing (Vitest)

### E2E Tests (Playwright)
- [ ] Create nursing note flow
- [ ] Edit nursing note within 24h (as creator)
- [ ] View nursing notes list
- [ ] Record vital signs flow (with custom recordedAt)
- [ ] Record vital signs flow (without recordedAt - defaults to now)
- [ ] Edit vital signs within 24h (as creator)
- [ ] View vital signs in table format
- [ ] View vital signs in chart format
- [ ] Date range filter works correctly
- [ ] Validation error messages displayed correctly (both languages)
- [ ] Blood pressure validation (systolic > diastolic)
- [ ] Edit denied after 24h (non-admin)
- [ ] Edit denied for non-creator (non-admin)
- [ ] Admin can edit any record anytime
- [ ] **Create denied for discharged admission**
- [ ] **Edit denied for discharged admission**

### General
- [ ] API contract documented
- [ ] Database migrations tested
- [ ] **Database CHECK constraint for BP validated**
- [ ] Permissions assigned to appropriate roles (ADMIN, DOCTOR, NURSE, CHIEF_NURSE)
- [ ] Feature documentation updated

---

## Documentation Updates Required

### Must Update

- [ ] **[CLAUDE.md](../../CLAUDE.md)**
  - Add "Nursing Module (nursing notes, vital signs)" to "Implemented Features" backend section
  - Add "Nursing Module UI (notes list, vital signs table/charts)" to frontend section
  - Update migration count to V045

### Review for Consistency

- [ ] **[README.md](../../web/README.md)**
  - Check if setup instructions need updates

### Code Documentation

- [ ] **`NursingNote.kt`** - Add KDoc for entity
- [ ] **`VitalSign.kt`** - Add KDoc for entity, document recordedAt vs createdAt
- [ ] **`NursingNoteService.kt`** - Document edit time limit and discharge check logic
- [ ] **`VitalSignService.kt`** - Document edit time limit, discharge check, and BP validation logic

---

## Design Decisions

### Why `recordedAt` vs `createdAt`?
In clinical settings, vital signs are often recorded at the bedside on paper or a mobile device, then entered into the system later. The `recordedAt` field captures when the measurement was actually taken, while `createdAt` (from BaseEntity) captures when it was entered into the system. This distinction is important for clinical accuracy and charting.

### Why no optimistic locking?
The existing codebase does not use `@Version` for optimistic locking. For consistency, this feature follows the same pattern. The 24-hour edit window and creator-only restriction significantly reduce the likelihood of concurrent edit conflicts.

### Why database-level CHECK constraint for blood pressure?
While the application validates systolic > diastolic, adding a database constraint provides defense-in-depth. This ensures data integrity even if validation is bypassed (e.g., direct database access, future API changes).

### Why separate chart endpoint?
The paginated list endpoint is optimized for table view (default 20 records). Charts need all data points to display trends accurately. A separate endpoint avoids overloading the paginated endpoint with a "fetch all" mode and makes the API contract clearer.

### Why block writes for discharged admissions?
Medical records for discharged patients should be immutable for legal and compliance reasons. While the existing codebase doesn't consistently enforce this (Progress Notes don't check), this feature implements it correctly as a best practice.

---

## Related Docs/Commits/Issues

- Related feature: Progress Notes (similar pattern for admission-related notes)
- Related feature: Clinical History (admission detail integration pattern)
- Related feature: Psychotherapy Activities (tab integration in MedicalRecordTabs)
- Related migration: V038 (permission assignment pattern for clinical roles)

# Feature: Medical/Psychiatric Record (Expediente Médico/Psiquiátrico)

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-02-04 | @author | Initial draft |

---

## Overview

This feature provides a comprehensive medical/psychiatric record system for hospitalized patients. It includes three main sections: Clinical History (Historia Clínica) for initial patient assessment, Progress Notes (Evoluciones) for ongoing daily observations by nurses and doctors, and Medical Orders (Órdenes Médicas) for prescriptions, lab requests, and care instructions. This record is displayed as part of the admission details view. All entries are append-only for doctors/nurses; only admins can modify existing records. Full audit trail is maintained for all operations.

---

## Use Case / User Story

**Clinical History**
1. As a **doctor**, I want to create a clinical history for an admitted patient so that I can document their initial medical/psychiatric assessment.
2. As a **doctor or nurse**, I want to view the clinical history of an admitted patient so that I can understand their medical background.
3. As an **admin**, I want to modify a clinical history entry so that I can correct errors or update information.

**Progress Notes (Evoluciones)**
4. As a **doctor or nurse**, I want to add progress notes to an admission so that I can document the patient's daily status and observations.
5. As a **doctor or nurse**, I want to view all progress notes for an admission (with timestamps and author) so that I can track the patient's evolution over time.
6. As an **admin**, I want to modify a progress note so that I can correct errors if needed.

**Medical Orders (Órdenes Médicas)**
7. As a **doctor**, I want to create medical orders (medications, labs, diet, etc.) for an admitted patient so that nurses can follow the treatment plan.
8. As a **nurse**, I want to view all active medical orders for an admission so that I can administer the correct care.
9. As an **admin**, I want to modify or cancel a medical order so that I can correct errors.

**Audit**
10. As a **user with access**, I want to see who created or modified each entry and when so that there is full accountability for medical records.

---

## Authorization / Role Access

### Clinical History

| Action | Required Role(s) | Permission | Notes |
|--------|------------------|------------|-------|
| View | DOCTOR, NURSE, ADMIN | `clinical-history:read` | All medical staff can view |
| Create | DOCTOR, ADMIN | `clinical-history:create` | Only doctors initiate |
| Update | ADMIN | `clinical-history:update` | Append-only for doctors |

### Progress Notes (Evoluciones)

| Action | Required Role(s) | Permission | Notes |
|--------|------------------|------------|-------|
| View | DOCTOR, NURSE, ADMIN | `progress-note:read` | All medical staff can view |
| Create | DOCTOR, NURSE, ADMIN | `progress-note:create` | Both can add notes |
| Update | ADMIN | `progress-note:update` | Append-only for doctors/nurses |

### Medical Orders (Órdenes Médicas)

| Action | Required Role(s) | Permission | Notes |
|--------|------------------|------------|-------|
| View | DOCTOR, NURSE, ADMIN | `medical-order:read` | All medical staff can view |
| Create | DOCTOR, ADMIN | `medical-order:create` | Only doctors prescribe |
| Update | ADMIN | `medical-order:update` | Append-only for doctors |

---

## Functional Requirements

### Clinical History (Historia Clínica)

- **One record per admission**, created during hospitalization
- Displayed as a dedicated tab/section in admission detail view
- All fields are rich text (WYSIWYG editor) for formatted content
- **Sections and fields:**

  1. **Motivo de la consulta** - Single rich text field
  2. **Problema o padecimiento actual** - Single rich text field
  3. **Salud mental previa** - Single rich text field
  4. **Situación familiar, laboral, social y económica** - Single rich text field
  5. **Antecedentes heredofamiliares** - Group with sub-fields:
     - Médicos (rich text)
     - Psiquiátricos (rich text)
  6. **Antecedentes patológicos personales** - Group with sub-fields:
     - Médicos (rich text)
     - Quirúrgicos (rich text)
     - Alérgicos (rich text)
     - Psiquiátricos (rich text)
     - Gineco-obstétricos (rich text)
  7. **Consumo de sustancias tóxicas y problemas relacionados** - Single rich text field (includes tabaquismo, alcohol, adicciones)
  8. **Desarrollo psicomotor** - Single rich text field
  9. **Examen del estado mental** - Single rich text field
  10. **Temperamento y carácter** - Single rich text field
  11. **Resultados de estudios previos de laboratorio y gabinete** - Single rich text field
  12. **Tratamientos previos y respuesta a los mismos** - Single rich text field
  13. **Observaciones generales** - Single rich text field
  14. **Diagnóstico** - Single rich text field
  15. **Plan de manejo** - Single rich text field
  16. **Pronóstico** - Single rich text field

- Display created by, created at, modified by, modified at on the form

### Progress Notes (Evoluciones)

- **Multiple entries allowed** per admission (multiple per day permitted)
- Each entry is a separate record with its own timestamp
- **Fields per entry (all rich text):**
  1. **Datos subjetivos** - What the patient reports
  2. **Datos objetivos** - Observable/measurable findings
  3. **Análisis** - Assessment/interpretation
  4. **Planes de acción** - Action plans

- Display as chronological list (newest first option, or oldest first)
- Each entry shows: author (name + role), timestamp, all four fields
- Filter by date range, author
- Doctors and nurses can create; only admins can edit existing entries

### Medical Orders (Órdenes Médicas)

- **Multiple orders per admission**, organized by category
- **Categories:**
  1. Órdenes médicas (general medical orders)
  2. Medicamentos (medications)
  3. Laboratorios, exámenes de gabinete (lab tests, imaging)
  4. Referencias médicas (medical referrals)
  5. Pruebas psicométricas (psychometric tests)
  6. Actividad física (physical activity)
  7. Cuidados especiales (special care)
  8. Dieta (diet)
  9. Restricciones de movilidad (mobility restrictions)
  10. Permisos de visita (visit permissions)
  11. Otras (other)

- **Fields per order:**
  1. **Categoría** - Selected from predefined list (required)
  2. **Fecha de inicio** - Start date (required)
  3. **Medicamento** - Medication name (optional, primarily for Medicamentos category)
  4. **Dosis** - Dosage (optional)
  5. **Vía** - Route of administration (optional: oral, IV, IM, etc.)
  6. **Frecuencia** - Frequency (optional: every 8 hours, daily, etc.)
  7. **Horario** - Schedule (optional: specific times)
  8. **Observaciones** - Observations/notes (rich text, optional)

- Display grouped by category
- Each order shows: all fields, author, timestamp
- Doctors can create; only admins can edit existing orders
- Option to mark orders as discontinued (soft status, not delete)

### General Requirements

- All three sections displayed as tabs within admission detail view
- Rich text editor for all text fields (TipTap or PrimeVue Editor)
- Full audit trail: created_by, created_at, updated_by, updated_at visible on all entries
- Print-friendly view for medical records

---

## Acceptance Criteria / Scenarios

### Clinical History - Happy Path

- When a doctor creates a clinical history for an admission, all fields are saved and associated with the admission.
- When a user views an admission, the clinical history tab displays all documented fields with rich text formatting preserved.
- When an admin edits a clinical history, changes are saved and the audit trail reflects the modification.

### Clinical History - Edge Cases

- When a non-doctor (nurse) attempts to create a clinical history, return 403 Forbidden.
- When a doctor/nurse attempts to edit an existing clinical history, return 403 Forbidden.
- When creating a clinical history for an admission that already has one, return 400 Bad Request (one per admission).
- When viewing a clinical history that doesn't exist yet, display empty state with "Create" button (for doctors only).

### Progress Notes - Happy Path

- When a doctor or nurse creates a progress note, it is saved with timestamp and author information.
- When viewing progress notes, they are displayed in chronological order with author and timestamp visible.
- Multiple progress notes can be created on the same day by different users.

### Progress Notes - Edge Cases

- When a doctor/nurse attempts to edit their own progress note, return 403 Forbidden (append-only).
- When submitting a progress note with all empty fields, return 400 Bad Request with validation error.
- When an admin edits a progress note, the original author is preserved but "modified by" is tracked.

### Medical Orders - Happy Path

- When a doctor creates a medical order, they select a category and fill in the order details.
- When viewing medical orders, they are grouped by category and display all relevant fields.
- Multiple orders can exist under the same category.

### Medical Orders - Edge Cases

- When a nurse attempts to create a medical order, return 403 Forbidden.
- When a doctor/nurse attempts to edit a medical order, return 403 Forbidden.
- When creating an order without required fields (category, fecha de inicio), return 400 Bad Request.
- When an order has a start date in the past, allow it (backdating for documentation purposes).

### Audit Trail

- All create/edit operations record the user ID and timestamp.
- Edit operations preserve the original creator and record the modifier.
- Audit information is visible on each entry (created by, created at, modified by, modified at).

### Authorization

- When an unauthenticated user attempts any action, return 401 Unauthorized.
- When a user without appropriate role attempts a restricted action, return 403 Forbidden.

---

## Non-Functional Requirements

- **Security**: All entries are append-only for non-admin users; only admins can modify existing records
- **Audit**: Full audit trail on all entries (required for medical compliance)
- **Performance**: Progress notes list should paginate for admissions with many entries
- **Rich Text**: Support basic formatting (bold, italic, lists, headings) in all text fields

---

## API Contract

### Clinical History Endpoints

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| GET | `/api/v1/admissions/{admissionId}/clinical-history` | - | `ClinicalHistoryResponse` | Yes | Get clinical history for admission |
| POST | `/api/v1/admissions/{admissionId}/clinical-history` | `CreateClinicalHistoryRequest` | `ClinicalHistoryResponse` | Yes | Create clinical history |
| PUT | `/api/v1/admissions/{admissionId}/clinical-history` | `UpdateClinicalHistoryRequest` | `ClinicalHistoryResponse` | Yes | Update clinical history (admin only) |

### Progress Notes Endpoints

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| GET | `/api/v1/admissions/{admissionId}/progress-notes` | - | `PagedResponse<ProgressNoteResponse>` | Yes | List progress notes with pagination |
| GET | `/api/v1/admissions/{admissionId}/progress-notes/{id}` | - | `ProgressNoteResponse` | Yes | Get single progress note |
| POST | `/api/v1/admissions/{admissionId}/progress-notes` | `CreateProgressNoteRequest` | `ProgressNoteResponse` | Yes | Create progress note |
| PUT | `/api/v1/admissions/{admissionId}/progress-notes/{id}` | `UpdateProgressNoteRequest` | `ProgressNoteResponse` | Yes | Update progress note (admin only) |

### Medical Orders Endpoints

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| GET | `/api/v1/admissions/{admissionId}/medical-orders` | - | `List<MedicalOrderResponse>` | Yes | List all medical orders (grouped by category) |
| GET | `/api/v1/admissions/{admissionId}/medical-orders/{id}` | - | `MedicalOrderResponse` | Yes | Get single medical order |
| POST | `/api/v1/admissions/{admissionId}/medical-orders` | `CreateMedicalOrderRequest` | `MedicalOrderResponse` | Yes | Create medical order |
| PUT | `/api/v1/admissions/{admissionId}/medical-orders/{id}` | `UpdateMedicalOrderRequest` | `MedicalOrderResponse` | Yes | Update medical order (admin only) |
| POST | `/api/v1/admissions/{admissionId}/medical-orders/{id}/discontinue` | - | `MedicalOrderResponse` | Yes | Mark order as discontinued |

### Request/Response Examples

```json
// POST /api/v1/admissions/{admissionId}/clinical-history - Request
{
  "consultationReason": "<p>Patient presents with...</p>",
  "currentCondition": "<p>Currently experiencing...</p>",
  "previousMentalHealth": "<p>History of depression...</p>",
  "familySocialEconomicSituation": "<p>Lives with spouse...</p>",
  "familyMedicalHistory": "<p>Father had diabetes...</p>",
  "familyPsychiatricHistory": "<p>Mother had anxiety...</p>",
  "personalMedicalHistory": "<p>Hypertension diagnosed 2020...</p>",
  "personalSurgicalHistory": "<p>Appendectomy 2015...</p>",
  "personalAllergies": "<p>Penicillin allergy...</p>",
  "personalPsychiatricHistory": "<p>Previous depressive episode...</p>",
  "personalGynecologicalHistory": "<p>N/A</p>",
  "substanceUse": "<p>Social alcohol use...</p>",
  "psychomotorDevelopment": "<p>Normal development...</p>",
  "mentalStatusExam": "<p>Alert and oriented...</p>",
  "temperamentCharacter": "<p>Introverted...</p>",
  "previousLabResults": "<p>CBC normal...</p>",
  "previousTreatments": "<p>Sertraline 50mg for 6 months...</p>",
  "generalObservations": "<p>Patient cooperative...</p>",
  "diagnosis": "<p>Major Depressive Disorder, recurrent...</p>",
  "managementPlan": "<p>1. Start medication...</p>",
  "prognosis": "<p>Good with adherence to treatment...</p>"
}

// Response - ClinicalHistoryResponse
{
  "id": 1,
  "admissionId": 123,
  "consultationReason": "<p>Patient presents with...</p>",
  "currentCondition": "<p>Currently experiencing...</p>",
  // ... all other fields ...
  "createdAt": "2026-02-04T10:30:00",
  "createdBy": {
    "id": 5,
    "salutation": "Dr.",
    "firstName": "Maria",
    "lastName": "Garcia"
  },
  "updatedAt": "2026-02-04T10:30:00",
  "updatedBy": {
    "id": 5,
    "salutation": "Dr.",
    "firstName": "Maria",
    "lastName": "Garcia"
  }
}

// POST /api/v1/admissions/{admissionId}/progress-notes - Request
{
  "subjectiveData": "<p>Patient reports feeling better today...</p>",
  "objectiveData": "<p>BP: 120/80, HR: 72, Temp: 36.5C...</p>",
  "analysis": "<p>Improvement noted in mood...</p>",
  "actionPlans": "<p>Continue current medication...</p>"
}

// Response - ProgressNoteResponse
{
  "id": 1,
  "admissionId": 123,
  "subjectiveData": "<p>Patient reports feeling better today...</p>",
  "objectiveData": "<p>BP: 120/80, HR: 72, Temp: 36.5C...</p>",
  "analysis": "<p>Improvement noted in mood...</p>",
  "actionPlans": "<p>Continue current medication...</p>",
  "createdAt": "2026-02-04T14:00:00",
  "createdBy": {
    "id": 8,
    "salutation": "Lic.",
    "firstName": "Ana",
    "lastName": "Rodriguez",
    "role": "NURSE"
  },
  "updatedAt": "2026-02-04T14:00:00",
  "updatedBy": null
}

// POST /api/v1/admissions/{admissionId}/medical-orders - Request
{
  "category": "MEDICAMENTOS",
  "startDate": "2026-02-04",
  "medication": "Sertraline",
  "dosage": "50mg",
  "route": "ORAL",
  "frequency": "Once daily",
  "schedule": "Morning with breakfast",
  "observations": "<p>Monitor for side effects...</p>"
}

// Response - MedicalOrderResponse
{
  "id": 1,
  "admissionId": 123,
  "category": "MEDICAMENTOS",
  "startDate": "2026-02-04",
  "medication": "Sertraline",
  "dosage": "50mg",
  "route": "ORAL",
  "frequency": "Once daily",
  "schedule": "Morning with breakfast",
  "observations": "<p>Monitor for side effects...</p>",
  "status": "ACTIVE",
  "discontinuedAt": null,
  "discontinuedBy": null,
  "createdAt": "2026-02-04T10:35:00",
  "createdBy": {
    "id": 5,
    "salutation": "Dr.",
    "firstName": "Maria",
    "lastName": "Garcia"
  },
  "updatedAt": "2026-02-04T10:35:00",
  "updatedBy": null
}

// GET /api/v1/admissions/{admissionId}/medical-orders - Response (grouped)
{
  "MEDICAMENTOS": [
    { "id": 1, "medication": "Sertraline", "dosage": "50mg", ... },
    { "id": 2, "medication": "Lorazepam", "dosage": "1mg", ... }
  ],
  "LABORATORIOS": [
    { "id": 3, "observations": "<p>Complete blood count...</p>", ... }
  ],
  "DIETA": [
    { "id": 4, "observations": "<p>Low sodium diet...</p>", ... }
  ]
}
```

---

## Database Changes

### New Entities

| Entity | Table | Extends | Description |
|--------|-------|---------|-------------|
| `ClinicalHistory` | `clinical_histories` | `BaseEntity` | Clinical history record (one per admission) |
| `ProgressNote` | `progress_notes` | `BaseEntity` | Progress note entries |
| `MedicalOrder` | `medical_orders` | `BaseEntity` | Medical order entries |

### New Migrations

| Migration | Description |
|-----------|-------------|
| `V029__create_clinical_histories_table.sql` | Creates clinical_histories table with all text fields |
| `V030__create_progress_notes_table.sql` | Creates progress_notes table |
| `V031__create_medical_orders_table.sql` | Creates medical_orders table with category enum |
| `V032__add_medical_record_permissions.sql` | Adds permissions for clinical-history, progress-note, medical-order resources |

### Schema

```sql
-- V029__create_clinical_histories_table.sql
CREATE TABLE clinical_histories (
    id BIGSERIAL PRIMARY KEY,
    admission_id BIGINT NOT NULL UNIQUE REFERENCES admissions(id),

    -- Section 1: Motivo de la consulta
    consultation_reason TEXT,

    -- Section 2: Problema o padecimiento actual
    current_condition TEXT,

    -- Section 3: Salud mental previa
    previous_mental_health TEXT,

    -- Section 4: Situación familiar, laboral, social y económica
    family_social_economic_situation TEXT,

    -- Section 5: Antecedentes heredofamiliares
    family_medical_history TEXT,
    family_psychiatric_history TEXT,

    -- Section 6: Antecedentes patológicos personales
    personal_medical_history TEXT,
    personal_surgical_history TEXT,
    personal_allergies TEXT,
    personal_psychiatric_history TEXT,
    personal_gynecological_history TEXT,

    -- Section 7: Consumo de sustancias tóxicas
    substance_use TEXT,

    -- Section 8: Desarrollo psicomotor
    psychomotor_development TEXT,

    -- Section 9: Examen del estado mental
    mental_status_exam TEXT,

    -- Section 10: Temperamento y carácter
    temperament_character TEXT,

    -- Section 11: Resultados de estudios previos
    previous_lab_results TEXT,

    -- Section 12: Tratamientos previos
    previous_treatments TEXT,

    -- Section 13: Observaciones generales
    general_observations TEXT,

    -- Section 14: Diagnóstico
    diagnosis TEXT,

    -- Section 15: Plan de manejo
    management_plan TEXT,

    -- Section 16: Pronóstico
    prognosis TEXT,

    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_clinical_histories_deleted_at ON clinical_histories(deleted_at);
CREATE INDEX idx_clinical_histories_admission_id ON clinical_histories(admission_id);

-- V030__create_progress_notes_table.sql
CREATE TABLE progress_notes (
    id BIGSERIAL PRIMARY KEY,
    admission_id BIGINT NOT NULL REFERENCES admissions(id),

    -- SOAP-like structure
    subjective_data TEXT NOT NULL,
    objective_data TEXT NOT NULL,
    analysis TEXT NOT NULL,
    action_plans TEXT NOT NULL,

    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_progress_notes_deleted_at ON progress_notes(deleted_at);
CREATE INDEX idx_progress_notes_admission_id ON progress_notes(admission_id);
CREATE INDEX idx_progress_notes_created_at ON progress_notes(created_at);

-- V031__create_medical_orders_table.sql
CREATE TYPE medical_order_category AS ENUM (
    'ORDENES_MEDICAS',
    'MEDICAMENTOS',
    'LABORATORIOS',
    'REFERENCIAS_MEDICAS',
    'PRUEBAS_PSICOMETRICAS',
    'ACTIVIDAD_FISICA',
    'CUIDADOS_ESPECIALES',
    'DIETA',
    'RESTRICCIONES_MOVILIDAD',
    'PERMISOS_VISITA',
    'OTRAS'
);

CREATE TYPE medical_order_status AS ENUM (
    'ACTIVE',
    'DISCONTINUED'
);

CREATE TYPE administration_route AS ENUM (
    'ORAL',
    'IV',
    'IM',
    'SC',
    'TOPICAL',
    'INHALATION',
    'RECTAL',
    'SUBLINGUAL',
    'OTHER'
);

CREATE TABLE medical_orders (
    id BIGSERIAL PRIMARY KEY,
    admission_id BIGINT NOT NULL REFERENCES admissions(id),

    category medical_order_category NOT NULL,
    start_date DATE NOT NULL,
    medication VARCHAR(255),
    dosage VARCHAR(100),
    route administration_route,
    frequency VARCHAR(100),
    schedule VARCHAR(255),
    observations TEXT,

    status medical_order_status NOT NULL DEFAULT 'ACTIVE',
    discontinued_at TIMESTAMP,
    discontinued_by BIGINT REFERENCES users(id),

    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_medical_orders_deleted_at ON medical_orders(deleted_at);
CREATE INDEX idx_medical_orders_admission_id ON medical_orders(admission_id);
CREATE INDEX idx_medical_orders_category ON medical_orders(category);
CREATE INDEX idx_medical_orders_status ON medical_orders(status);
CREATE INDEX idx_medical_orders_start_date ON medical_orders(start_date);

-- V032__add_medical_record_permissions.sql
-- Clinical History permissions
INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('clinical-history:create', 'Create Clinical History', 'Create clinical history records', 'clinical-history', 'create', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('clinical-history:read', 'Read Clinical History', 'View clinical history records', 'clinical-history', 'read', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('clinical-history:update', 'Update Clinical History', 'Modify clinical history records', 'clinical-history', 'update', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Progress Note permissions
INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('progress-note:create', 'Create Progress Note', 'Create progress notes', 'progress-note', 'create', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('progress-note:read', 'Read Progress Note', 'View progress notes', 'progress-note', 'read', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('progress-note:update', 'Update Progress Note', 'Modify progress notes', 'progress-note', 'update', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Medical Order permissions
INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('medical-order:create', 'Create Medical Order', 'Create medical orders', 'medical-order', 'create', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('medical-order:read', 'Read Medical Order', 'View medical orders', 'medical-order', 'read', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('medical-order:update', 'Update Medical Order', 'Modify medical orders', 'medical-order', 'update', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Assign ADMIN full access
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMIN' AND p.resource IN ('clinical-history', 'progress-note', 'medical-order');

-- Assign DOCTOR: create/read clinical-history, create/read progress-note, create/read medical-order
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'DOCTOR' AND p.code IN (
    'clinical-history:create', 'clinical-history:read',
    'progress-note:create', 'progress-note:read',
    'medical-order:create', 'medical-order:read'
);

-- Assign NURSE: read clinical-history, create/read progress-note, read medical-order
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'NURSE' AND p.code IN (
    'clinical-history:read',
    'progress-note:create', 'progress-note:read',
    'medical-order:read'
);
```

### Index Requirements

- [x] `deleted_at` - Required for soft delete queries on all tables
- [x] `admission_id` - FK lookup for all medical record queries
- [x] `created_at` on progress_notes - Sorting by date
- [x] `category` on medical_orders - Grouping by category
- [x] `status` on medical_orders - Filter active/discontinued
- [x] `start_date` on medical_orders - Date filtering

---

## Frontend Changes

### Components

| Component | Location | Description |
|-----------|----------|-------------|
| `MedicalRecordTabs.vue` | `src/components/admissions/` | Tab container for all three sections |
| `ClinicalHistoryView.vue` | `src/components/medical-record/` | View/display clinical history |
| `ClinicalHistoryForm.vue` | `src/components/medical-record/` | Create/edit clinical history form |
| `ProgressNoteList.vue` | `src/components/medical-record/` | List of progress notes with pagination |
| `ProgressNoteForm.vue` | `src/components/medical-record/` | Create progress note form |
| `ProgressNoteCard.vue` | `src/components/medical-record/` | Single progress note display card |
| `MedicalOrderList.vue` | `src/components/medical-record/` | List of medical orders grouped by category |
| `MedicalOrderForm.vue` | `src/components/medical-record/` | Create medical order form |
| `MedicalOrderCard.vue` | `src/components/medical-record/` | Single medical order display card |
| `RichTextEditor.vue` | `src/components/common/` | Reusable rich text editor wrapper (PrimeVue Editor or TipTap) |

### Pinia Stores

| Store | Location | Description |
|-------|----------|-------------|
| `useClinicalHistoryStore` | `src/stores/clinicalHistory.ts` | Clinical history CRUD |
| `useProgressNoteStore` | `src/stores/progressNote.ts` | Progress notes CRUD, pagination |
| `useMedicalOrderStore` | `src/stores/medicalOrder.ts` | Medical orders CRUD, grouping |

### Routes

No new routes needed - all sections are displayed within the existing admission detail view (`/admissions/:id`) as tabs.

### Validation (Zod Schemas)

```typescript
// src/schemas/clinicalHistory.ts
import { z } from 'zod'

export const clinicalHistorySchema = z.object({
  consultationReason: z.string().optional(),
  currentCondition: z.string().optional(),
  previousMentalHealth: z.string().optional(),
  familySocialEconomicSituation: z.string().optional(),
  familyMedicalHistory: z.string().optional(),
  familyPsychiatricHistory: z.string().optional(),
  personalMedicalHistory: z.string().optional(),
  personalSurgicalHistory: z.string().optional(),
  personalAllergies: z.string().optional(),
  personalPsychiatricHistory: z.string().optional(),
  personalGynecologicalHistory: z.string().optional(),
  substanceUse: z.string().optional(),
  psychomotorDevelopment: z.string().optional(),
  mentalStatusExam: z.string().optional(),
  temperamentCharacter: z.string().optional(),
  previousLabResults: z.string().optional(),
  previousTreatments: z.string().optional(),
  generalObservations: z.string().optional(),
  diagnosis: z.string().optional(),
  managementPlan: z.string().optional(),
  prognosis: z.string().optional(),
})

// src/schemas/progressNote.ts
export const progressNoteSchema = z.object({
  subjectiveData: z.string().min(1, 'Subjective data is required'),
  objectiveData: z.string().min(1, 'Objective data is required'),
  analysis: z.string().min(1, 'Analysis is required'),
  actionPlans: z.string().min(1, 'Action plans are required'),
})

// src/schemas/medicalOrder.ts
export const medicalOrderCategoryEnum = z.enum([
  'ORDENES_MEDICAS',
  'MEDICAMENTOS',
  'LABORATORIOS',
  'REFERENCIAS_MEDICAS',
  'PRUEBAS_PSICOMETRICAS',
  'ACTIVIDAD_FISICA',
  'CUIDADOS_ESPECIALES',
  'DIETA',
  'RESTRICCIONES_MOVILIDAD',
  'PERMISOS_VISITA',
  'OTRAS',
])

export const administrationRouteEnum = z.enum([
  'ORAL',
  'IV',
  'IM',
  'SC',
  'TOPICAL',
  'INHALATION',
  'RECTAL',
  'SUBLINGUAL',
  'OTHER',
])

export const medicalOrderSchema = z.object({
  category: medicalOrderCategoryEnum,
  startDate: z.string().date('Invalid date format'),
  medication: z.string().max(255).optional(),
  dosage: z.string().max(100).optional(),
  route: administrationRouteEnum.optional(),
  frequency: z.string().max(100).optional(),
  schedule: z.string().max(255).optional(),
  observations: z.string().optional(),
})
```

---

## Implementation Notes

- **Rich Text Editor**: Use PrimeVue's `Editor` component (Quill-based) for rich text fields. Alternatively, consider TipTap for more control.
- **Tab Structure**: Add a `MedicalRecordTabs` component to the existing `AdmissionDetail.vue` that contains three tabs: Clinical History, Progress Notes, Medical Orders.
- **Append-Only Pattern**: Services should check user role before allowing updates. Only users with ADMIN role can call update endpoints.
- **Audit Display**: Show `createdBy`, `createdAt`, `updatedBy`, `updatedAt` on all entries using a reusable `AuditInfo` component.
- **Grouping Medical Orders**: The API returns medical orders grouped by category. Frontend can use PrimeVue's `Accordion` or `TabView` to display categories.
- **PostgreSQL Enums**: Use `CREATE TYPE ... AS ENUM` for category and route fields. In Kotlin, map with `@Enumerated(EnumType.STRING)` and `@Column(columnDefinition = "...")`.
- **Existing Patterns**: Follow `AdmissionController`/`AdmissionService` patterns for new controllers and services.
- **i18n**: All category names and field labels need Spanish translations in locale files.

---

## QA Checklist

### Backend
- [ ] All functional requirements implemented
- [ ] Entities extend `BaseEntity`
- [ ] Entities have `@SQLRestriction("deleted_at IS NULL")`
- [ ] DTOs used in controllers (no entity exposure)
- [ ] Input validation in place
- [ ] Clinical history: one per admission constraint enforced
- [ ] Progress notes: append-only for non-admins
- [ ] Medical orders: append-only for non-admins
- [ ] Medical orders: discontinue endpoint works
- [ ] Audit trail (createdBy, updatedBy) properly tracked
- [ ] Permission checks on all endpoints
- [ ] Unit tests written and passing
- [ ] Integration tests written and passing (Testcontainers)
- [ ] Detekt passes (no new violations)
- [ ] OWASP dependency-check passes

### Frontend
- [ ] MedicalRecordTabs component integrated into AdmissionDetail
- [ ] Clinical history form with all 16+ rich text fields
- [ ] Clinical history view displays formatted content
- [ ] Progress notes list with pagination
- [ ] Progress note form with rich text editors
- [ ] Medical orders grouped by category
- [ ] Medical order form with category dropdown
- [ ] Rich text editor component working
- [ ] Audit info displayed on all entries
- [ ] Create buttons hidden for unauthorized users
- [ ] Edit buttons shown only for admins
- [ ] Pinia stores implemented
- [ ] Form validation with VeeValidate + Zod
- [ ] Error handling implemented
- [ ] ESLint/oxlint passes
- [ ] i18n keys added for all user-facing text (Spanish)
- [ ] Unit tests written and passing (Vitest)

### E2E Tests (Playwright)
- [ ] Doctor creates clinical history
- [ ] Nurse cannot create clinical history
- [ ] Doctor/nurse views clinical history
- [ ] Admin edits clinical history
- [ ] Doctor/nurse cannot edit clinical history
- [ ] Doctor creates progress note
- [ ] Nurse creates progress note
- [ ] Doctor/nurse cannot edit progress notes
- [ ] Admin edits progress note
- [ ] Doctor creates medical order
- [ ] Nurse cannot create medical order
- [ ] Doctor/nurse cannot edit medical orders
- [ ] Admin edits medical order
- [ ] Medical order discontinue flow
- [ ] Permission denied scenarios displayed correctly
- [ ] Rich text formatting preserved

### General
- [ ] API contract documented
- [ ] Database migrations tested
- [ ] Feature documentation updated
- [ ] Reviewed by project owner

---

## Documentation Updates Required

### Must Update

- [ ] **[CLAUDE.md](../../CLAUDE.md)**
  - Add Medical/Psychiatric Record to "Implemented Features" section
  - Update migration count (V029-V032)

### Review for Consistency

- [ ] **[ARCHITECTURE.md](../architecture/ARCHITECTURE.md)**
  - Add new entities to entity diagram if exists

### Code Documentation

- [ ] **`ClinicalHistory.kt`** - Document one-per-admission constraint
- [ ] **`ProgressNote.kt`** - Document SOAP structure
- [ ] **`MedicalOrder.kt`** - Document categories and discontinue flow
- [ ] **`*Service.kt`** - Document append-only behavior for non-admins

---

## Related Docs/Commits/Issues

- Related feature: [Patient Admission](./patient-admission.md)
- Related entity: `Admission` (parent entity for all medical records)
- Related pattern: Append-only with admin override (similar to audit log patterns)

# Feature: New Patient Intake

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-21 | @author | Initial draft |
| 1.1 | 2026-01-24 | @author | Added "Admit" action to patient list (integration with admission flow) |

---

## Overview

This feature allows administrative staff to register new patients by capturing their general information, emergency contacts, and ID document. Clinical staff (doctors, nurses) can view patient records but cannot modify them. Patient records are never deleted to maintain medical record integrity.

**i18n**: Spanish - "Registro de nuevo paciente"

---

## Use Case / User Story

1. **As an administrative staff member**, I want to register a new patient with their general information so that we have their data on file for clinical care.

2. **As an administrative staff member**, I want to add multiple emergency contacts for a patient so that we can reach family members when needed.

3. **As an administrative staff member**, I want to upload a photo/scan of the patient's ID document so that we have identity verification on record.

4. **As an administrative staff member**, I want to edit existing patient data so that I can correct or update information as needed.

5. **As a clinical staff member**, I want to search for patients by name or other criteria so that I can quickly find the patient I need.

6. **As a clinical staff member**, I want to view patient general information including who registered them and when, so that I have full context about the patient record.

---

## Authorization / Role Access

### New Roles Required

| Role Code | Name | Description |
|-----------|------|-------------|
| `ADMINISTRATIVE_STAFF` | Administrative Staff | Front desk / registration staff |
| `DOCTOR` | Doctor | Medical doctors |
| `NURSE` | Nurse | Nursing staff |
| `CHIEF_NURSE` | Chief Nurse | Head of nursing |

### Permission Matrix

| Action | Administrative Staff | Doctor | Nurse | Chief Nurse | Permission Code |
|--------|---------------------|--------|-------|-------------|-----------------|
| Create patient | ✅ | ❌ | ❌ | ❌ | `patient:create` |
| View patient (general info) | ✅ | ✅ | ✅ | ✅ | `patient:read` |
| Edit patient | ✅ | ❌ | ❌ | ❌ | `patient:update` |
| Search patients | ✅ | ✅ | ✅ | ✅ | `patient:read` |
| Upload ID document | ✅ | ❌ | ❌ | ❌ | `patient:upload-id` |
| View ID document | ✅ | ❌ | ❌ | ❌ | `patient:view-id` |

---

## Functional Requirements

### Patient Data Fields (All Required)

| Field | Spanish Label | Type | Constraints |
|-------|---------------|------|-------------|
| `firstName` | Nombres | String | max 100 chars |
| `lastName` | Apellidos | String | max 100 chars |
| `age` | Edad | Integer | 0-150 |
| `sex` | Sexo | Enum | MALE, FEMALE |
| `gender` | Género | String | max 50 chars |
| `maritalStatus` | Estado Civil | Enum | SINGLE, MARRIED, DIVORCED, WIDOWED, SEPARATED, OTHER |
| `religion` | Religión | String | max 100 chars |
| `educationLevel` | Escolaridad | Enum | NONE, PRIMARY, SECONDARY, TECHNICAL, UNIVERSITY, POSTGRADUATE |
| `occupation` | Ocupación | String | max 100 chars |
| `address` | Dirección Domicilio | String | max 500 chars |
| `email` | Email | String | valid email format, max 255 chars |
| `notes` | Anotaciones | Text | optional, free-form text |
| `idDocumentNumber` | Número de Documento | String | max 50 chars, for duplicate detection |

### Emergency Contact Fields

| Field | Spanish Label | Type | Constraints |
|-------|---------------|------|-------------|
| `name` | Persona de Contacto | String | max 200 chars |
| `relationship` | Parentesco | String | max 100 chars |
| `phone` | Teléfono | String | max 20 chars |

### Requirements

- FR1: Create patient with all general data fields (all required)
- FR2: Support multiple emergency contacts (minimum 1 required)
- FR3: Upload/store patient ID document (image or PDF, max 5MB)
- FR4: Edit existing patient data
- FR5: Search patients by name, ID document number
- FR6: Display audit trail (created by, created at, updated by, updated at)
- FR7: Patient records are **never deleted** (no soft delete for patients)
- FR8: Detect potential duplicate patients (same name + age or same ID document number)

---

## Acceptance Criteria / Scenarios

### Happy Path - Create Patient

- ✅ When administrative staff submits a valid patient form with all required fields and at least one emergency contact, the patient is created and 201 Created is returned.
- ✅ When creating a patient, multiple emergency contacts can be added with name, relationship, and phone number.
- ✅ When a patient is created, the system records who created it and when (audit trail).

### Happy Path - Upload ID Document

- ✅ When administrative staff uploads a valid image/PDF of the patient's ID document (≤5MB), it is stored and associated with the patient.

### Happy Path - Edit Patient

- ✅ When administrative staff updates patient data, changes are saved and last modified info is updated.
- ✅ When editing, emergency contacts can be added, modified, or removed (minimum one must remain).

### Happy Path - View Patient

- ✅ When any clinical staff views a patient, they see all general information plus audit trail (created by, created at, modified by, modified at).
- ✅ When administrative staff views a patient, they also see the ID document.
- ✅ When non-administrative staff views a patient, the ID document is not visible/returned.

### Happy Path - Search

- ✅ When staff searches for patients by name, matching results are returned.
- ✅ When staff searches by ID document number, matching results are returned.
- ✅ When search has no matches, an empty list is returned (not an error).

### Edge Cases - Validation

- ❌ When required fields are missing, return 400 Bad Request with specific validation errors.
- ❌ When no emergency contact is provided, return 400 with "At least one emergency contact required" error.
- ❌ When email format is invalid, return 400 with validation error.
- ❌ When phone number format is invalid, return 400 with validation error.
- ❌ When age is out of range (0-150), return 400 with validation error.

### Edge Cases - File Upload

- ❌ When uploaded file exceeds 5MB, return 400 with appropriate error.
- ❌ When uploaded file type is not allowed (only images/PDF), return 400 with appropriate error.

### Edge Cases - Authorization

- ❌ When non-administrative staff attempts to create a patient, return 403 Forbidden.
- ❌ When non-administrative staff attempts to edit a patient, return 403 Forbidden.
- ❌ When non-administrative staff attempts to view/download ID document, return 403 Forbidden.
- ❌ When unauthenticated user attempts any action, return 401 Unauthorized.

### Edge Cases - Not Found

- ❌ When viewing/editing a patient that doesn't exist, return 404 Not Found.

### Edge Cases - Duplicate Prevention

- ⚠️ When creating a patient that may already exist (matching firstName + lastName + age, or matching ID document number), return 409 Conflict with a warning and reference to existing patient(s).

---

## Non-Functional Requirements

- All general data fields are required
- At least one emergency contact is required
- ID document upload max size: 5MB
- Allowed file types: JPEG, PNG, PDF

---

## API Contract

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| POST | `/api/v1/patients` | `CreatePatientRequest` | `PatientResponse` | `patient:create` | Create new patient |
| GET | `/api/v1/patients` | Query params | `PagedResponse<PatientSummaryResponse>` | `patient:read` | List/search patients |
| GET | `/api/v1/patients/{id}` | - | `PatientResponse` | `patient:read` | Get patient details |
| PUT | `/api/v1/patients/{id}` | `UpdatePatientRequest` | `PatientResponse` | `patient:update` | Update patient |
| POST | `/api/v1/patients/{id}/id-document` | Multipart file | `PatientResponse` | `patient:upload-id` | Upload ID document |
| GET | `/api/v1/patients/{id}/id-document` | - | Binary (image/PDF) | `patient:view-id` | Download ID document |
| DELETE | `/api/v1/patients/{id}/id-document` | - | `PatientResponse` | `patient:upload-id` | Remove ID document |

### Query Parameters for GET /api/v1/patients

| Parameter | Type | Description |
|-----------|------|-------------|
| `page` | Integer | Page number (default: 0) |
| `size` | Integer | Page size (default: 20) |
| `search` | String | Search by name or ID document number |

### Request/Response Examples

```json
// POST /api/v1/patients - Request
{
  "firstName": "Juan",
  "lastName": "Pérez García",
  "age": 45,
  "sex": "MALE",
  "gender": "Masculino",
  "maritalStatus": "MARRIED",
  "religion": "Católica",
  "educationLevel": "UNIVERSITY",
  "occupation": "Ingeniero",
  "address": "4a Calle 5-67 Zona 1, Guatemala",
  "email": "juan.perez@email.com",
  "idDocumentNumber": "1234567890101",
  "notes": "Paciente referido por Dr. López",
  "emergencyContacts": [
    {
      "name": "María de Pérez",
      "relationship": "Esposa",
      "phone": "+502 5555-1234"
    },
    {
      "name": "Carlos Pérez",
      "relationship": "Hijo",
      "phone": "+502 5555-5678"
    }
  ]
}

// Response - 201 Created
{
  "success": true,
  "data": {
    "id": 1,
    "firstName": "Juan",
    "lastName": "Pérez García",
    "age": 45,
    "sex": "MALE",
    "gender": "Masculino",
    "maritalStatus": "MARRIED",
    "religion": "Católica",
    "educationLevel": "UNIVERSITY",
    "occupation": "Ingeniero",
    "address": "4a Calle 5-67 Zona 1, Guatemala",
    "email": "juan.perez@email.com",
    "idDocumentNumber": "1234567890101",
    "notes": "Paciente referido por Dr. López",
    "hasIdDocument": false,
    "emergencyContacts": [
      {
        "id": 1,
        "name": "María de Pérez",
        "relationship": "Esposa",
        "phone": "+502 5555-1234"
      },
      {
        "id": 2,
        "name": "Carlos Pérez",
        "relationship": "Hijo",
        "phone": "+502 5555-5678"
      }
    ],
    "createdAt": "2026-01-21T10:00:00Z",
    "createdBy": {
      "id": 5,
      "username": "receptionist1",
      "firstName": "Ana",
      "lastName": "García"
    },
    "updatedAt": "2026-01-21T10:00:00Z",
    "updatedBy": {
      "id": 5,
      "username": "receptionist1",
      "firstName": "Ana",
      "lastName": "García"
    }
  }
}

// 409 Conflict - Potential duplicate
{
  "success": false,
  "message": "Potential duplicate patient found",
  "data": {
    "potentialDuplicates": [
      {
        "id": 42,
        "firstName": "Juan",
        "lastName": "Pérez García",
        "age": 45,
        "idDocumentNumber": "1234567890101"
      }
    ]
  }
}
```

---

## Database Changes

### New Entities

| Entity | Table | Extends | Description |
|--------|-------|---------|-------------|
| `Patient` | `patients` | `BaseEntity` | Main patient entity |
| `EmergencyContact` | `emergency_contacts` | `BaseEntity` | Patient emergency contacts |
| `PatientIdDocument` | `patient_id_documents` | `BaseEntity` | Stores ID document metadata/file |

### New Migrations

| Migration | Description |
|-----------|-------------|
| `V011__create_patients_table.sql` | Creates patients table |
| `V012__create_emergency_contacts_table.sql` | Creates emergency_contacts table |
| `V013__create_patient_id_documents_table.sql` | Creates patient_id_documents table |
| `V014__add_clinical_roles.sql` | Adds ADMINISTRATIVE_STAFF, DOCTOR, NURSE, CHIEF_NURSE roles |
| `V015__add_patient_permissions.sql` | Adds patient permissions and role assignments |

### Schema

```sql
-- V011__create_patients_table.sql
CREATE TABLE patients (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    age INTEGER NOT NULL CHECK (age >= 0 AND age <= 150),
    sex VARCHAR(10) NOT NULL,
    gender VARCHAR(50) NOT NULL,
    marital_status VARCHAR(20) NOT NULL,
    religion VARCHAR(100) NOT NULL,
    education_level VARCHAR(20) NOT NULL,
    occupation VARCHAR(100) NOT NULL,
    address VARCHAR(500) NOT NULL,
    email VARCHAR(255) NOT NULL,
    id_document_number VARCHAR(50),
    notes TEXT,
    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP  -- Will not be used, but included for BaseEntity compatibility
);

CREATE INDEX idx_patients_last_name ON patients(last_name);
CREATE INDEX idx_patients_first_name ON patients(first_name);
CREATE INDEX idx_patients_id_document_number ON patients(id_document_number);
CREATE INDEX idx_patients_deleted_at ON patients(deleted_at);

-- V012__create_emergency_contacts_table.sql
CREATE TABLE emergency_contacts (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id),
    name VARCHAR(200) NOT NULL,
    relationship VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_emergency_contacts_patient_id ON emergency_contacts(patient_id);
CREATE INDEX idx_emergency_contacts_deleted_at ON emergency_contacts(deleted_at);

-- V013__create_patient_id_documents_table.sql
CREATE TABLE patient_id_documents (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL UNIQUE REFERENCES patients(id),
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    file_data BYTEA NOT NULL,  -- Store file directly in DB for simplicity
    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_patient_id_documents_patient_id ON patient_id_documents(patient_id);
CREATE INDEX idx_patient_id_documents_deleted_at ON patient_id_documents(deleted_at);

-- V014__add_clinical_roles.sql
INSERT INTO roles (code, name, description, is_system, created_at, updated_at) VALUES
('ADMINISTRATIVE_STAFF', 'Administrative Staff', 'Front desk and registration staff', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('DOCTOR', 'Doctor', 'Medical doctors', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('NURSE', 'Nurse', 'Nursing staff', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CHIEF_NURSE', 'Chief Nurse', 'Head of nursing department', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- V015__add_patient_permissions.sql
INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('patient:create', 'Create Patient', 'Register new patients', 'patient', 'create', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('patient:read', 'Read Patient', 'View patient information', 'patient', 'read', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('patient:update', 'Update Patient', 'Modify patient information', 'patient', 'update', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('patient:upload-id', 'Upload Patient ID', 'Upload patient ID document', 'patient', 'upload-id', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('patient:view-id', 'View Patient ID', 'View patient ID document', 'patient', 'view-id', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Assign patient permissions to ADMIN (full access)
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMIN' AND p.resource = 'patient';

-- Assign permissions to ADMINISTRATIVE_STAFF (full patient access)
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMINISTRATIVE_STAFF' AND p.resource = 'patient';

-- Assign read-only permissions to clinical roles (DOCTOR, NURSE, CHIEF_NURSE)
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('DOCTOR', 'NURSE', 'CHIEF_NURSE') AND p.code = 'patient:read';
```

### Index Requirements

- [x] `deleted_at` - Required for soft delete queries (though not used for patients)
- [x] `patient_id` - Foreign key on emergency_contacts and patient_id_documents
- [x] `first_name`, `last_name` - For search functionality
- [x] `id_document_number` - For search and duplicate detection

---

## Frontend Changes

### Components

| Component | Location | Description |
|-----------|----------|-------------|
| `PatientList.vue` | `src/views/patients/` | List view with search, pagination, and row actions (view, edit, admit) |
| `PatientForm.vue` | `src/views/patients/` | Create/Edit patient form |
| `PatientDetail.vue` | `src/views/patients/` | View patient details (read-only for clinical staff) |
| `EmergencyContactForm.vue` | `src/components/patients/` | Reusable emergency contact sub-form |

### Pinia Stores

| Store | Location | Description |
|-------|----------|-------------|
| `usePatientStore` | `src/stores/patient.ts` | State management for patients |

### Routes

| Path | Component | Auth Required | Roles |
|------|-----------|---------------|-------|
| `/patients` | `PatientList` | Yes | All clinical roles |
| `/patients/new` | `PatientForm` | Yes | ADMINISTRATIVE_STAFF, ADMIN |
| `/patients/:id` | `PatientDetail` | Yes | All clinical roles |
| `/patients/:id/edit` | `PatientForm` | Yes | ADMINISTRATIVE_STAFF, ADMIN |

### Validation (Zod Schemas)

```typescript
// src/schemas/patient.ts
import { z } from 'zod'

export const emergencyContactSchema = z.object({
  id: z.number().optional(),
  name: z.string().min(1, 'contact.name.required').max(200),
  relationship: z.string().min(1, 'contact.relationship.required').max(100),
  phone: z.string().min(1, 'contact.phone.required').max(20)
})

export const patientSchema = z.object({
  firstName: z.string().min(1, 'patient.firstName.required').max(100),
  lastName: z.string().min(1, 'patient.lastName.required').max(100),
  age: z.number().min(0).max(150, 'patient.age.invalid'),
  sex: z.enum(['MALE', 'FEMALE']),
  gender: z.string().min(1, 'patient.gender.required').max(50),
  maritalStatus: z.enum(['SINGLE', 'MARRIED', 'DIVORCED', 'WIDOWED', 'SEPARATED', 'OTHER']),
  religion: z.string().min(1, 'patient.religion.required').max(100),
  educationLevel: z.enum(['NONE', 'PRIMARY', 'SECONDARY', 'TECHNICAL', 'UNIVERSITY', 'POSTGRADUATE']),
  occupation: z.string().min(1, 'patient.occupation.required').max(100),
  address: z.string().min(1, 'patient.address.required').max(500),
  email: z.string().email('patient.email.invalid').max(255),
  idDocumentNumber: z.string().max(50).optional(),
  notes: z.string().optional(),
  emergencyContacts: z.array(emergencyContactSchema).min(1, 'patient.emergencyContacts.required')
})

export type PatientFormData = z.infer<typeof patientSchema>
export type EmergencyContactFormData = z.infer<typeof emergencyContactSchema>
```

### i18n Keys Required

```json
// English (en.json)
{
  "patient": {
    "title": "Patients",
    "newPatient": "New Patient",
    "editPatient": "Edit Patient",
    "patientDetails": "Patient Details",
    "generalInfo": "General Information",
    "firstName": "First Name",
    "lastName": "Last Name",
    "age": "Age",
    "sex": "Sex",
    "gender": "Gender",
    "maritalStatus": "Marital Status",
    "religion": "Religion",
    "educationLevel": "Education Level",
    "occupation": "Occupation",
    "address": "Home Address",
    "email": "Email",
    "idDocumentNumber": "ID Document Number",
    "notes": "Notes",
    "emergencyContacts": "Emergency Contacts",
    "idDocument": "ID Document",
    "uploadIdDocument": "Upload ID Document",
    "viewIdDocument": "View ID Document",
    "noIdDocument": "No ID document uploaded",
    "registeredBy": "Registered by",
    "lastModifiedBy": "Last modified by",
    "search": "Search patients...",
    "sex": {
      "MALE": "Male",
      "FEMALE": "Female"
    },
    "maritalStatus": {
      "SINGLE": "Single",
      "MARRIED": "Married",
      "DIVORCED": "Divorced",
      "WIDOWED": "Widowed",
      "SEPARATED": "Separated",
      "OTHER": "Other"
    },
    "educationLevel": {
      "NONE": "None",
      "PRIMARY": "Primary",
      "SECONDARY": "Secondary",
      "TECHNICAL": "Technical",
      "UNIVERSITY": "University",
      "POSTGRADUATE": "Postgraduate"
    }
  },
  "contact": {
    "name": "Contact Name",
    "relationship": "Relationship",
    "phone": "Phone",
    "addContact": "Add Contact",
    "removeContact": "Remove Contact"
  }
}

// Spanish (es.json)
{
  "patient": {
    "title": "Pacientes",
    "newPatient": "Registro de nuevo paciente",
    "editPatient": "Editar Paciente",
    "patientDetails": "Datos del Paciente",
    "generalInfo": "Datos Generales",
    "firstName": "Nombres",
    "lastName": "Apellidos",
    "age": "Edad",
    "sex": "Sexo",
    "gender": "Género",
    "maritalStatus": "Estado Civil",
    "religion": "Religión",
    "educationLevel": "Escolaridad",
    "occupation": "Ocupación",
    "address": "Dirección Domicilio",
    "email": "Email",
    "idDocumentNumber": "Número de Documento",
    "notes": "Anotaciones",
    "emergencyContacts": "Contactos de Emergencia",
    "idDocument": "Documento de Identidad",
    "uploadIdDocument": "Subir Documento de Identidad",
    "viewIdDocument": "Ver Documento de Identidad",
    "noIdDocument": "No se ha subido documento de identidad",
    "registeredBy": "Registrado por",
    "lastModifiedBy": "Última modificación por",
    "search": "Buscar pacientes...",
    "sex": {
      "MALE": "Masculino",
      "FEMALE": "Femenino"
    },
    "maritalStatus": {
      "SINGLE": "Soltero/a",
      "MARRIED": "Casado/a",
      "DIVORCED": "Divorciado/a",
      "WIDOWED": "Viudo/a",
      "SEPARATED": "Separado/a",
      "OTHER": "Otro"
    },
    "educationLevel": {
      "NONE": "Ninguna",
      "PRIMARY": "Primaria",
      "SECONDARY": "Secundaria",
      "TECHNICAL": "Técnica",
      "UNIVERSITY": "Universitaria",
      "POSTGRADUATE": "Postgrado"
    }
  },
  "contact": {
    "name": "Persona de Contacto",
    "relationship": "Parentesco",
    "phone": "Teléfono",
    "addContact": "Agregar Contacto",
    "removeContact": "Eliminar Contacto"
  }
}
```

---

## Implementation Notes

- **Admission Integration**: PatientList includes an "Admit" action icon that navigates to `/admissions/new?patientId={id}`. This action should be visible only to users with `admission:create` permission and hidden if the patient already has an ACTIVE admission. See [Patient Admission](./patient-admission.md) for admission flow details.
- **Entity Pattern**: Follow existing `User` entity pattern - extend `BaseEntity`, use `@SQLRestriction("deleted_at IS NULL")` (even though patients won't be deleted)
- **File Storage**: ID documents stored as BYTEA in PostgreSQL for simplicity. Consider moving to object storage (S3/MinIO) if file sizes become an issue
- **Duplicate Detection**: Check for potential duplicates on create, return 409 with suggestions. Allow user to proceed if they confirm it's a new patient (future enhancement: add `forceCreate` flag)
- **Audit Display**: Use `createdBy`/`updatedBy` from BaseEntity, join with User to display username and full name
- **Emergency Contacts**: Use `@OneToMany` with cascade. When updating patient, handle contact additions/removals carefully
- **Permission Checks**: Use Spring Security `@PreAuthorize` annotations on controller methods

---

## QA Checklist

### Backend
- [ ] All functional requirements implemented
- [ ] `Patient` entity extends `BaseEntity`
- [ ] `Patient` entity has `@SQLRestriction("deleted_at IS NULL")`
- [ ] `EmergencyContact` entity extends `BaseEntity`
- [ ] `PatientIdDocument` entity extends `BaseEntity`
- [ ] DTOs used in controller (no entity exposure)
- [ ] Input validation in place (all required fields)
- [ ] At least one emergency contact validation
- [ ] File upload validation (size, type)
- [ ] Duplicate detection implemented
- [ ] Permission-based ID document visibility
- [ ] Unit tests written and passing
- [ ] Integration tests written and passing (Testcontainers)
- [ ] Detekt passes (no new violations)
- [ ] OWASP dependency-check passes

### Frontend
- [ ] `PatientList.vue` component with search/pagination
- [ ] `PatientList.vue` includes "Admit" action (requires `admission:create` permission)
- [ ] "Admit" action hidden for patients with ACTIVE admission
- [ ] `PatientForm.vue` component with validation
- [ ] `PatientDetail.vue` component with audit info display
- [ ] `EmergencyContactForm.vue` sub-component
- [ ] `usePatientStore` Pinia store implemented
- [ ] Routes configured with proper permission guards
- [ ] Form validation with VeeValidate + Zod
- [ ] File upload component for ID document
- [ ] Conditional ID document display (admin only)
- [ ] Error handling implemented
- [ ] ESLint/oxlint passes
- [ ] i18n keys added (English and Spanish)
- [ ] Unit tests written and passing (Vitest)

### E2E Tests (Playwright)
- [ ] **Create Patient Flow**: Admin staff can register a new patient with all required fields and emergency contacts
- [ ] **Edit Patient Flow**: Admin staff can edit existing patient data and add/remove emergency contacts
- [ ] **Admit Action**: Clicking "Admit" on patient row navigates to admission wizard with patient pre-selected
- [ ] **View Patient Flow**: Clinical staff (Doctor, Nurse, Chief Nurse) can view patient details but cannot edit
- [ ] **Search Patients**: Users can search patients by name and ID document number
- [ ] **ID Document Upload**: Admin staff can upload and view patient ID document
- [ ] **ID Document Restriction**: Non-admin staff cannot see/access ID document
- [ ] **Permission Denied**: Clinical staff attempting to create/edit patient sees 403 error
- [ ] **Validation Errors**: Form shows validation errors for missing required fields
- [ ] **Emergency Contact Minimum**: Form prevents saving without at least one emergency contact
- [ ] **Duplicate Detection**: Creating a duplicate patient shows warning with existing patient info
- [ ] **Audit Trail Display**: Patient detail view shows "Registered by" and "Last modified by" information

### General
- [ ] API contract documented
- [ ] Database migrations tested
- [ ] New roles created and tested
- [ ] Permissions assigned correctly
- [ ] Feature documentation updated
- [ ] Reviewed by project owner

---

## Documentation Updates Required

### Must Update

- [ ] **[CLAUDE.md](../../CLAUDE.md)**
  - Add Patient entities to "Implemented Features"
  - Add new roles (ADMINISTRATIVE_STAFF, DOCTOR, NURSE, CHIEF_NURSE)
- [ ] **[ARCHITECTURE.md](../architecture/ARCHITECTURE.md)** (if exists)
  - Document patient module structure

### Review for Consistency

- [ ] **[README.md](../../web/README.md)**
  - Check if setup instructions need updates for new roles

### Code Documentation

- [ ] **`api/src/main/kotlin/.../entity/Patient.kt`**
  - Add KDoc comments
- [ ] **`api/src/main/kotlin/.../service/PatientService.kt`**
  - Document public methods, especially duplicate detection logic

---

## Related Docs/Commits/Issues

- Related feature: User management (existing)
- Related feature: [Patient Admission](./patient-admission.md) (admission starts from patient list)
- Design discussion: This feature spec document

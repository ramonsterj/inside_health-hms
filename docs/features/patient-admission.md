# Feature: Patient Admission

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-23 | @author | Initial draft |
| 1.1 | 2026-01-24 | @author | Added Consulting Physicians (Interconsultas) |
| 1.2 | 2026-01-24 | @author | Added patient active admission validation |
| 1.3 | 2026-01-24 | @author | Changed to patient-centric flow (admission starts from patient list) |

---

## Overview

Register hospital admissions starting from the patient list view. Staff selects a patient and clicks "Admit" to begin the admission wizard with patient context already established. If the patient doesn't exist, they must be created first via the patient registration flow. The admission form captures triage code, room (with capacity tracking), treating physician, patient inventory, and informed consent documents. Supports simple discharge flow that automatically frees room capacity. After admission is registered, consulting physicians (interconsultas) can be added to request specialist consultations.

---

## Use Case / User Story

1. As an **admissions staff member**, I want to start an admission directly from a patient's row in the patient list so that I can quickly admit a known patient without searching.

2. As an **admissions staff member**, I want to register a new patient first (via patient registration) and then admit them so that patient data is complete before admission.

3. As an **admissions staff member**, I want to create a hospital admission with triage code, room, treating physician, inventory, and consent document so that the patient is properly registered in the system.

4. As an **admissions staff member**, I want to see only rooms with available beds so that I don't accidentally overbook a room.

5. As an **admissions staff member**, I want to discharge a patient so that their room capacity is freed for new patients.

6. As an **admin**, I want to manage triage codes (create, edit, delete) so that the admission form has the correct options.

7. As an **admin**, I want to manage rooms (create, edit, delete, set capacity) so that room availability is accurately tracked.

8. As an **admin**, I want to delete admissions so that erroneous records can be removed (soft delete only).

9. As an **admissions staff member**, I want to add consulting physicians (interconsultas) to an existing admission so that specialist consultations can be requested and tracked.

10. As an **admissions staff member**, I want to remove a consulting physician from an admission so that I can correct mistakes or cancel consultation requests.

---

## Authorization / Role Access

### Admission

| Action | Required Role(s) | Permission | Notes |
|--------|------------------|------------|-------|
| View admissions | ADMINISTRATIVE_STAFF, ADMIN | `admission:read` | List and view details |
| Create admission | ADMINISTRATIVE_STAFF, ADMIN | `admission:create` | Register new admission |
| Update admission | ADMINISTRATIVE_STAFF, ADMIN | `admission:update` | Edit admission details |
| Discharge patient | ADMINISTRATIVE_STAFF, ADMIN | `admission:update` | Change status to DISCHARGED |
| Delete admission | ADMIN | `admission:delete` | Soft delete only |
| Upload consent | ADMINISTRATIVE_STAFF, ADMIN | `admission:upload-consent` | Upload consent document |
| View consent | ADMINISTRATIVE_STAFF, ADMIN | `admission:view-consent` | Download consent document |

### Triage Code

| Action | Required Role(s) | Permission | Notes |
|--------|------------------|------------|-------|
| View triage codes | ADMINISTRATIVE_STAFF, ADMIN | `triage-code:read` | For dropdown selection |
| Create triage code | ADMIN | `triage-code:create` | |
| Update triage code | ADMIN | `triage-code:update` | |
| Delete triage code | ADMIN | `triage-code:delete` | Cannot delete if in use |

### Room

| Action | Required Role(s) | Permission | Notes |
|--------|------------------|------------|-------|
| View rooms | ADMINISTRATIVE_STAFF, ADMIN | `room:read` | For dropdown selection |
| Create room | ADMIN | `room:create` | |
| Update room | ADMIN | `room:update` | |
| Delete room | ADMIN | `room:delete` | Cannot delete if has active patients |

### Consulting Physicians (Interconsultas)

| Action | Required Role(s) | Permission | Notes |
|--------|------------------|------------|-------|
| View consulting physicians | ADMINISTRATIVE_STAFF, ADMIN | `admission:read` | Included in admission detail |
| Add consulting physician | ADMINISTRATIVE_STAFF, ADMIN | `admission:update` | Only for existing admissions |
| Remove consulting physician | ADMINISTRATIVE_STAFF, ADMIN | `admission:update` | Only for existing admissions |

---

## Functional Requirements

### Admission Flow (Multi-Step Form)

**Entry Point**: User clicks "Admit" action on a patient row in the patient list (`/patients`). This navigates to `/admissions/new?patientId={id}` with the patient pre-selected.

1. **Step 1 - Patient Confirmation**
   - Display read-only patient summary (name, age, ID document number, emergency contacts)
   - Show warning if patient already has an ACTIVE admission (block proceeding)
   - Confirm button to proceed to next step
   - "Cancel" returns to patient list

2. **Step 2 - Admission Details**
   - Admission date/time: pre-filled with current date/time, editable
   - Triage code: dropdown populated from TriageCode entity (show code + color indicator)
   - Room: dropdown showing only rooms with available capacity (show room number, type, available beds)
   - Treating physician: dropdown of users with DOCTOR role (show salutation + name)

3. **Step 3 - Additional Information**
   - Inventory: optional textarea for patient belongings
   - Consent document: file upload (max 25MB, PDF/images)

4. **Step 4 - Review & Submit**
   - Display summary of all entered data
   - **Validation**: Patient must not have an existing ACTIVE admission
   - Submit creates admission with status ACTIVE
   - Room available beds automatically decremented

### Discharge Flow

- Discharge button on admission detail view
- Sets status to DISCHARGED
- Sets dischargeDate to current timestamp
- Room available beds automatically incremented

### Room Availability Logic

- Available beds = capacity - count of ACTIVE admissions for that room
- Room appears in dropdown only if available beds > 0
- When admission created: available beds decrease by 1
- When admission discharged: available beds increase by 1
- When admission soft-deleted: if was ACTIVE, available beds increase by 1

### Triage Code Management (Admin)

- CRUD operations for triage codes
- Fields: code (A, B, C...), color (hex), description (optional), displayOrder
- Cannot delete triage code if referenced by active admissions

### Room Management (Admin)

- CRUD operations for rooms
- Fields: number, type (PRIVATE/SHARED), capacity
- Cannot delete room if has active admissions

### Audit Trail

- Display created by (user) and created at (timestamp) on admission detail
- Display last modified by (user) and modified at (timestamp) on admission detail
- BaseEntity handles this automatically via JPA auditing

### Consulting Physicians (Interconsultas)

- **Only available after admission is created** - cannot be added during the admission wizard
- Displayed on admission detail view as a list with add/remove actions
- Add consulting physician:
  - Dropdown of users with DOCTOR role (excluding the treating physician)
  - Optional reason/notes field (max 500 characters)
  - Optional requested date field (defaults to current date)
  - Same physician cannot be added twice to the same admission
- Remove consulting physician:
  - Soft delete (sets deleted_at on junction record)
  - Confirmation dialog before removal
- List display:
  - Show physician name (salutation + first + last)
  - Show reason/notes if provided
  - Show requested date
  - Show added by (user) and added at (timestamp)

---

## Acceptance Criteria / Scenarios

### Admission Creation

**Happy Path:**
- Given a valid patient, triage code, room with available capacity, and treating physician, when admission is created, then status is ACTIVE and room's available beds decrease by 1.
- Given admission date is not provided, when form loads, then current date/time is pre-filled.
- Given consent file under 25MB, when uploaded, then file is stored and linked to admission.

**Edge Cases:**
- When patientId query param is missing or invalid, then redirect to patient list with error message.
- When patient already has an ACTIVE admission, then Step 1 shows warning and blocks proceeding.
- When selected room has no available beds (concurrent booking), then return 400 with "Room is full" error.
- When consent file exceeds 25MB, then return 400 with file size validation error.
- When treating physician is not a user with doctor role, then return 400 with validation error.
- When required fields (patient, triage code, room, treating physician) are missing, then return 400 with validation errors.
- When user lacks `admission:create` permission, then return 403 Forbidden.

### Discharge

**Happy Path:**
- Given an ACTIVE admission, when discharged, then status becomes DISCHARGED, dischargeDate is set to now, and room's available beds increase by 1.

**Edge Cases:**
- When admission is already DISCHARGED, then return 400 with "Already discharged" error.
- When user lacks `admission:update` permission, then return 403 Forbidden.

### Delete (Admin Only)

**Happy Path:**
- Given an admission, when admin deletes it, then admission is soft-deleted (deleted_at set) and room capacity is freed if was ACTIVE.

**Edge Cases:**
- When user lacks `admission:delete` permission, then return 403 Forbidden.
- When admission doesn't exist, then return 404 Not Found.

### Room Availability

- When querying available rooms, only rooms where (capacity - active admissions) > 0 are returned.
- When admission is soft-deleted, room capacity is recalculated (freed if was ACTIVE).

### Triage Codes & Rooms Management (Admin)

- CRUD operations require respective permissions.
- When deleting a triage code in use by active admissions, then return 400 with "In use" error.
- When deleting a room with active admissions, then return 400 with "Has active patients" error.

### Consulting Physicians (Interconsultas)

**Happy Path:**
- Given an existing admission, when a consulting physician is added with valid data, then a new record is created and returned in the admission detail.
- Given an existing admission with consulting physicians, when viewing admission detail, then the list of consulting physicians is displayed.

**Edge Cases:**
- When attempting to add a consulting physician during admission creation (wizard), then the option is not available (UI does not show it).
- When attempting to add the same physician twice to an admission, then return 400 with "Physician already assigned" error.
- When attempting to add the treating physician as a consulting physician, then return 400 with "Cannot add treating physician as consultant" error.
- When the selected user is not a doctor, then return 400 with validation error.
- When user lacks `admission:update` permission, then return 403 Forbidden.
- When admission doesn't exist, then return 404 Not Found.
- When removing a consulting physician that doesn't exist, then return 404 Not Found.

---

## Non-Functional Requirements

- **File Size**: Consent documents max 25MB
- **Performance**: Room availability query should be efficient (use proper indexing)
- **Security**: All inputs validated; file type validation for consent uploads
- **Audit**: All changes logged via JPA auditing (BaseEntity)

---

## API Contract

### Admission Endpoints

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| GET | `/api/v1/admissions` | - | `PagedResponse<AdmissionListResponse>` | Yes | List admissions with pagination |
| GET | `/api/v1/admissions/{id}` | - | `AdmissionDetailResponse` | Yes | Get admission details |
| POST | `/api/v1/admissions` | `CreateAdmissionRequest` | `AdmissionDetailResponse` | Yes | Create new admission |
| PUT | `/api/v1/admissions/{id}` | `UpdateAdmissionRequest` | `AdmissionDetailResponse` | Yes | Update admission |
| POST | `/api/v1/admissions/{id}/discharge` | - | `AdmissionDetailResponse` | Yes | Discharge patient |
| DELETE | `/api/v1/admissions/{id}` | - | - | Yes | Soft delete admission |
| POST | `/api/v1/admissions/{id}/consent` | `MultipartFile` | `AdmissionDetailResponse` | Yes | Upload consent document |
| GET | `/api/v1/admissions/{id}/consent` | - | `byte[]` | Yes | Download consent document |

### Triage Code Endpoints

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| GET | `/api/v1/triage-codes` | - | `List<TriageCodeResponse>` | Yes | List all triage codes |
| GET | `/api/v1/triage-codes/{id}` | - | `TriageCodeResponse` | Yes | Get triage code by ID |
| POST | `/api/v1/triage-codes` | `CreateTriageCodeRequest` | `TriageCodeResponse` | Yes | Create triage code |
| PUT | `/api/v1/triage-codes/{id}` | `UpdateTriageCodeRequest` | `TriageCodeResponse` | Yes | Update triage code |
| DELETE | `/api/v1/triage-codes/{id}` | - | - | Yes | Delete triage code |

### Room Endpoints

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| GET | `/api/v1/rooms` | - | `List<RoomResponse>` | Yes | List all rooms |
| GET | `/api/v1/rooms/available` | - | `List<RoomAvailabilityResponse>` | Yes | List rooms with availability |
| GET | `/api/v1/rooms/{id}` | - | `RoomResponse` | Yes | Get room by ID |
| POST | `/api/v1/rooms` | `CreateRoomRequest` | `RoomResponse` | Yes | Create room |
| PUT | `/api/v1/rooms/{id}` | `UpdateRoomRequest` | `RoomResponse` | Yes | Update room |
| DELETE | `/api/v1/rooms/{id}` | - | - | Yes | Delete room |

### Patient Search Endpoint

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| GET | `/api/v1/patients/search?q={query}` | - | `List<PatientSearchResponse>` | Yes | Search patients by name/ID |

### Consulting Physician Endpoints

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| GET | `/api/v1/admissions/{id}/consulting-physicians` | - | `List<ConsultingPhysicianResponse>` | Yes | List consulting physicians for admission |
| POST | `/api/v1/admissions/{id}/consulting-physicians` | `AddConsultingPhysicianRequest` | `ConsultingPhysicianResponse` | Yes | Add consulting physician |
| DELETE | `/api/v1/admissions/{id}/consulting-physicians/{physicianId}` | - | - | Yes | Remove consulting physician (soft delete) |

### Request/Response Examples

```json
// POST /api/v1/admissions - Request
{
  "patientId": 1,
  "triageCodeId": 2,
  "roomId": 3,
  "treatingPhysicianId": 4,
  "admissionDate": "2026-01-23T10:30:00",
  "inventory": "Wallet, phone, glasses"
}

// Response - AdmissionDetailResponse
{
  "id": 1,
  "patient": {
    "id": 1,
    "firstName": "Juan",
    "lastName": "Perez",
    "idDocumentNumber": "1234567890"
  },
  "triageCode": {
    "id": 2,
    "code": "B",
    "color": "#FFA500",
    "description": "Urgent"
  },
  "room": {
    "id": 3,
    "number": "201",
    "type": "PRIVATE"
  },
  "treatingPhysician": {
    "id": 4,
    "salutation": "Dr.",
    "firstName": "Maria",
    "lastName": "Garcia"
  },
  "admissionDate": "2026-01-23T10:30:00",
  "dischargeDate": null,
  "status": "ACTIVE",
  "inventory": "Wallet, phone, glasses",
  "hasConsentDocument": true,
  "createdAt": "2026-01-23T10:35:00",
  "createdBy": {
    "id": 5,
    "username": "receptionist1"
  },
  "updatedAt": "2026-01-23T10:35:00",
  "updatedBy": {
    "id": 5,
    "username": "receptionist1"
  }
}

// GET /api/v1/rooms/available - Response
[
  {
    "id": 3,
    "number": "201",
    "type": "PRIVATE",
    "capacity": 1,
    "availableBeds": 1
  },
  {
    "id": 4,
    "number": "301",
    "type": "SHARED",
    "capacity": 3,
    "availableBeds": 2
  }
]

// POST /api/v1/triage-codes - Request
{
  "code": "A",
  "color": "#FF0000",
  "description": "Critical - Immediate attention required",
  "displayOrder": 1
}

// POST /api/v1/rooms - Request
{
  "number": "101",
  "type": "PRIVATE",
  "capacity": 1
}

// POST /api/v1/admissions/{id}/consulting-physicians - Request
{
  "physicianId": 7,
  "reason": "Cardiology consultation for arrhythmia evaluation",
  "requestedDate": "2026-01-24"
}

// Response - ConsultingPhysicianResponse
{
  "id": 1,
  "physician": {
    "id": 7,
    "salutation": "Dr.",
    "firstName": "Carlos",
    "lastName": "Rodriguez"
  },
  "reason": "Cardiology consultation for arrhythmia evaluation",
  "requestedDate": "2026-01-24",
  "createdAt": "2026-01-24T09:15:00",
  "createdBy": {
    "id": 5,
    "username": "receptionist1"
  }
}

// AdmissionDetailResponse now includes consultingPhysicians array
{
  "id": 1,
  // ... other admission fields ...
  "consultingPhysicians": [
    {
      "id": 1,
      "physician": {
        "id": 7,
        "salutation": "Dr.",
        "firstName": "Carlos",
        "lastName": "Rodriguez"
      },
      "reason": "Cardiology consultation for arrhythmia evaluation",
      "requestedDate": "2026-01-24",
      "createdAt": "2026-01-24T09:15:00",
      "createdBy": {
        "id": 5,
        "username": "receptionist1"
      }
    }
  ]
}
```

---

## Database Changes

### New Entities

| Entity | Table | Extends | Description |
|--------|-------|---------|-------------|
| `TriageCode` | `triage_codes` | `BaseEntity` | Triage classification codes |
| `Room` | `rooms` | `BaseEntity` | Hospital rooms with capacity |
| `Admission` | `admissions` | `BaseEntity` | Patient admission records |
| `AdmissionConsentDocument` | `admission_consent_documents` | `BaseEntity` | Consent file storage |
| `AdmissionConsultingPhysician` | `admission_consulting_physicians` | `BaseEntity` | Consulting physician assignments |

### New Migrations

| Migration | Description |
|-----------|-------------|
| `V021__create_triage_codes_table.sql` | Creates triage_codes table |
| `V022__create_rooms_table.sql` | Creates rooms table |
| `V023__create_admissions_table.sql` | Creates admissions table with FK constraints |
| `V024__create_admission_consent_documents_table.sql` | Creates consent document storage table |
| `V025__add_admission_permissions.sql` | Adds permissions for admission, triage-code, room resources |
| `V026__create_admission_consulting_physicians_table.sql` | Creates consulting physicians junction table |

### Schema

```sql
-- V021__create_triage_codes_table.sql
CREATE TABLE triage_codes (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    color VARCHAR(7) NOT NULL,  -- Hex color #RRGGBB
    description VARCHAR(255),
    display_order INT NOT NULL DEFAULT 0,
    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_triage_codes_deleted_at ON triage_codes(deleted_at);
CREATE INDEX idx_triage_codes_display_order ON triage_codes(display_order);

-- V022__create_rooms_table.sql
CREATE TABLE rooms (
    id BIGSERIAL PRIMARY KEY,
    number VARCHAR(50) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL,  -- PRIVATE, SHARED
    capacity INT NOT NULL DEFAULT 1,
    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_rooms_deleted_at ON rooms(deleted_at);
CREATE INDEX idx_rooms_type ON rooms(type);

-- V023__create_admissions_table.sql
CREATE TABLE admissions (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id),
    triage_code_id BIGINT NOT NULL REFERENCES triage_codes(id),
    room_id BIGINT NOT NULL REFERENCES rooms(id),
    treating_physician_id BIGINT NOT NULL REFERENCES users(id),
    admission_date TIMESTAMP NOT NULL,
    discharge_date TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, DISCHARGED
    inventory TEXT,
    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_admissions_deleted_at ON admissions(deleted_at);
CREATE INDEX idx_admissions_patient_id ON admissions(patient_id);
CREATE INDEX idx_admissions_room_id ON admissions(room_id);
CREATE INDEX idx_admissions_status ON admissions(status);
CREATE INDEX idx_admissions_treating_physician_id ON admissions(treating_physician_id);
CREATE INDEX idx_admissions_admission_date ON admissions(admission_date);

-- V024__create_admission_consent_documents_table.sql
CREATE TABLE admission_consent_documents (
    id BIGSERIAL PRIMARY KEY,
    admission_id BIGINT NOT NULL UNIQUE REFERENCES admissions(id),
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    file_data BYTEA NOT NULL,
    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_admission_consent_documents_deleted_at ON admission_consent_documents(deleted_at);
CREATE INDEX idx_admission_consent_documents_admission_id ON admission_consent_documents(admission_id);

-- V025__add_admission_permissions.sql
-- Triage Code permissions
INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('triage-code:create', 'Create Triage Code', 'Create new triage codes', 'triage-code', 'create', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('triage-code:read', 'Read Triage Code', 'View triage codes', 'triage-code', 'read', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('triage-code:update', 'Update Triage Code', 'Modify triage codes', 'triage-code', 'update', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('triage-code:delete', 'Delete Triage Code', 'Delete triage codes', 'triage-code', 'delete', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Room permissions
INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('room:create', 'Create Room', 'Create new rooms', 'room', 'create', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('room:read', 'Read Room', 'View rooms', 'room', 'read', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('room:update', 'Update Room', 'Modify rooms', 'room', 'update', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('room:delete', 'Delete Room', 'Delete rooms', 'room', 'delete', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Admission permissions
INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('admission:create', 'Create Admission', 'Register new admissions', 'admission', 'create', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('admission:read', 'Read Admission', 'View admissions', 'admission', 'read', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('admission:update', 'Update Admission', 'Modify admissions', 'admission', 'update', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('admission:delete', 'Delete Admission', 'Delete admissions', 'admission', 'delete', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('admission:upload-consent', 'Upload Consent', 'Upload consent documents', 'admission', 'upload-consent', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('admission:view-consent', 'View Consent', 'View consent documents', 'admission', 'view-consent', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Assign ADMIN full access to all new resources
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMIN' AND p.resource IN ('triage-code', 'room', 'admission');

-- Assign ADMINISTRATIVE_STAFF read access to triage codes and rooms
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMINISTRATIVE_STAFF' AND p.code IN ('triage-code:read', 'room:read');

-- Assign ADMINISTRATIVE_STAFF admission permissions (except delete)
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMINISTRATIVE_STAFF' AND p.resource = 'admission' AND p.action != 'delete';

-- V026__create_admission_consulting_physicians_table.sql
CREATE TABLE admission_consulting_physicians (
    id BIGSERIAL PRIMARY KEY,
    admission_id BIGINT NOT NULL REFERENCES admissions(id),
    physician_id BIGINT NOT NULL REFERENCES users(id),
    reason VARCHAR(500),
    requested_date DATE,
    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP,
    -- Unique constraint: same physician can only be added once per admission (excluding soft-deleted)
    CONSTRAINT uk_admission_consulting_physician UNIQUE (admission_id, physician_id) WHERE deleted_at IS NULL
);

CREATE INDEX idx_admission_consulting_physicians_deleted_at ON admission_consulting_physicians(deleted_at);
CREATE INDEX idx_admission_consulting_physicians_admission_id ON admission_consulting_physicians(admission_id);
CREATE INDEX idx_admission_consulting_physicians_physician_id ON admission_consulting_physicians(physician_id);
```

### Index Requirements

- [x] `deleted_at` - Required for soft delete queries on all tables
- [x] `patient_id` - FK lookup for patient admissions
- [x] `room_id` - FK lookup and availability calculations
- [x] `status` - Filter active vs discharged admissions
- [x] `treating_physician_id` - Filter admissions by doctor
- [x] `admission_date` - Sorting and date range queries
- [x] `display_order` - Sorting triage codes in dropdown
- [x] `admission_consulting_physicians.admission_id` - FK lookup for consulting physicians
- [x] `admission_consulting_physicians.physician_id` - FK lookup for physician assignments

---

## Frontend Changes

### Components

| Component | Location | Description |
|-----------|----------|-------------|
| `AdmissionList.vue` | `src/views/admissions/` | List view with pagination, filters |
| `AdmissionDetail.vue` | `src/views/admissions/` | View admission details, discharge action |
| `AdmissionWizard.vue` | `src/views/admissions/` | Multi-step admission form (requires patientId query param) |
| `PatientConfirmationStep.vue` | `src/components/admissions/` | Step 1: Read-only patient summary, active admission check |
| `AdmissionDetailsStep.vue` | `src/components/admissions/` | Step 2: Triage, room, physician |
| `AdmissionExtrasStep.vue` | `src/components/admissions/` | Step 3: Inventory, consent |
| `AdmissionReviewStep.vue` | `src/components/admissions/` | Step 4: Summary and submit |
| `TriageCodeList.vue` | `src/views/admin/` | Admin: Manage triage codes |
| `TriageCodeForm.vue` | `src/components/admin/` | Admin: Create/edit triage code |
| `RoomList.vue` | `src/views/admin/` | Admin: Manage rooms |
| `RoomForm.vue` | `src/components/admin/` | Admin: Create/edit room |
| `TriageCodeBadge.vue` | `src/components/common/` | Display triage code with color |
| `ConsultingPhysicianList.vue` | `src/components/admissions/` | List consulting physicians on admission detail |
| `AddConsultingPhysicianDialog.vue` | `src/components/admissions/` | Dialog to add consulting physician |

### Pinia Stores

| Store | Location | Description |
|-------|----------|-------------|
| `useAdmissionStore` | `src/stores/admission.ts` | Admission CRUD, discharge action |
| `useTriageCodeStore` | `src/stores/triageCode.ts` | Triage code management |
| `useRoomStore` | `src/stores/room.ts` | Room management, availability |

### Routes

| Path | Component | Auth Required | Permission | Notes |
|------|-----------|---------------|------------|-------|
| `/admissions` | `AdmissionList` | Yes | `admission:read` | |
| `/admissions/new?patientId={id}` | `AdmissionWizard` | Yes | `admission:create` | Requires patientId query param |
| `/admissions/:id` | `AdmissionDetail` | Yes | `admission:read` | |
| `/admissions/:id/edit` | `AdmissionWizard` | Yes | `admission:update` | |
| `/admin/triage-codes` | `TriageCodeList` | Yes | `triage-code:read` |
| `/admin/triage-codes/new` | `TriageCodeForm` | Yes | `triage-code:create` |
| `/admin/triage-codes/:id/edit` | `TriageCodeForm` | Yes | `triage-code:update` |
| `/admin/rooms` | `RoomList` | Yes | `room:read` |
| `/admin/rooms/new` | `RoomForm` | Yes | `room:create` |
| `/admin/rooms/:id/edit` | `RoomForm` | Yes | `room:update` |

### Validation (Zod Schemas)

```typescript
// src/schemas/admission.ts
import { z } from 'zod'

export const createAdmissionSchema = z.object({
  patientId: z.number().positive('Patient is required'),
  triageCodeId: z.number().positive('Triage code is required'),
  roomId: z.number().positive('Room is required'),
  treatingPhysicianId: z.number().positive('Treating physician is required'),
  admissionDate: z.string().datetime('Invalid date format'),
  inventory: z.string().max(2000).optional(),
})

export const updateAdmissionSchema = createAdmissionSchema.partial()

// src/schemas/triageCode.ts
export const triageCodeSchema = z.object({
  code: z.string().min(1).max(10, 'Code must be 1-10 characters'),
  color: z.string().regex(/^#[0-9A-Fa-f]{6}$/, 'Must be valid hex color'),
  description: z.string().max(255).optional(),
  displayOrder: z.number().int().min(0).default(0),
})

// src/schemas/room.ts
export const roomSchema = z.object({
  number: z.string().min(1).max(50, 'Number must be 1-50 characters'),
  type: z.enum(['PRIVATE', 'SHARED']),
  capacity: z.number().int().min(1, 'Capacity must be at least 1'),
})

// Consent file validation
export const consentFileSchema = z.object({
  file: z
    .instanceof(File)
    .refine((file) => file.size <= 25 * 1024 * 1024, 'File must be under 25MB')
    .refine(
      (file) => ['application/pdf', 'image/jpeg', 'image/png'].includes(file.type),
      'File must be PDF, JPEG, or PNG'
    ),
})

// src/schemas/consultingPhysician.ts
export const addConsultingPhysicianSchema = z.object({
  physicianId: z.number().positive('Physician is required'),
  reason: z.string().max(500, 'Reason must be 500 characters or less').optional(),
  requestedDate: z.string().date('Invalid date format').optional(),
})
```

---

## Implementation Notes

- **Patient-Centric Flow**: Admissions start from the patient list. The "Admit" action passes `patientId` as a query param. The wizard validates this param on mount and fetches patient data. If patient doesn't exist, user must create them first via patient registration.
- **File Storage Pattern**: Follow existing `PatientIdDocument` pattern - store file as binary in database with fileName, contentType, fileSize, fileData columns.
- **Room Availability Query**: Use a custom repository method with `@Query` to calculate available beds efficiently.
- **Multi-Step Form**: Use PrimeVue `Stepper` component for the admission wizard.
- **Treating Physician Dropdown**: Query users filtered by DOCTOR role.
- **Color Picker**: Use PrimeVue `ColorPicker` for triage code color selection.
- **Optimistic Locking**: Consider adding `@Version` to Room entity if concurrent booking issues arise.
- **Existing Patterns**: Follow `PatientController`/`PatientService` patterns for new controllers and services.
- **Consulting Physicians**: Use PrimeVue `Dialog` for add form, `DataTable` for list display on admission detail.
- **Physician Dropdown Filter**: Exclude treating physician from consulting physician dropdown to prevent duplicate assignment.
- **Partial Unique Constraint**: PostgreSQL supports `WHERE deleted_at IS NULL` in unique constraints for proper soft delete handling.

---

## QA Checklist

### Backend
- [ ] All functional requirements implemented
- [ ] Entities extend `BaseEntity`
- [ ] Entities have `@SQLRestriction("deleted_at IS NULL")`
- [ ] DTOs used in controllers (no entity exposure)
- [ ] Input validation in place
- [ ] Patient cannot be admitted if already has an ACTIVE admission
- [ ] Room availability logic tested
- [ ] Discharge flow frees room capacity
- [ ] Soft delete frees room capacity if ACTIVE
- [ ] Cannot delete triage code/room if in use
- [ ] Consulting physicians: add endpoint validates physician is DOCTOR role
- [ ] Consulting physicians: cannot add treating physician as consultant
- [ ] Consulting physicians: cannot add same physician twice (unique constraint)
- [ ] Consulting physicians: soft delete on remove
- [ ] Unit tests written and passing
- [ ] Integration tests written and passing (Testcontainers)
- [ ] Detekt passes (no new violations)
- [ ] OWASP dependency-check passes

### Frontend
- [ ] Multi-step wizard functional
- [ ] Patient confirmation step shows read-only patient data
- [ ] Active admission check blocks wizard on Step 1
- [ ] Missing/invalid patientId redirects to patient list with error
- [ ] Room dropdown shows only available rooms
- [ ] File upload with size/type validation
- [ ] Triage code badge displays color correctly
- [ ] Discharge action works
- [ ] Admin CRUD for triage codes
- [ ] Admin CRUD for rooms
- [ ] Consulting physicians: list displayed on admission detail
- [ ] Consulting physicians: add dialog functional
- [ ] Consulting physicians: physician dropdown excludes treating physician
- [ ] Consulting physicians: remove with confirmation dialog
- [ ] Consulting physicians: not available in admission wizard (only after creation)
- [ ] Pinia stores implemented
- [ ] Routes configured with proper guards
- [ ] Form validation with VeeValidate + Zod
- [ ] Error handling implemented
- [ ] ESLint/oxlint passes
- [ ] i18n keys added for all user-facing text
- [ ] Unit tests written and passing (Vitest)

### E2E Tests (Playwright)
- [ ] Complete admission flow (click Admit on patient → confirm patient → fill details → upload consent → submit)
- [ ] Admission from patient list navigates to wizard with patient pre-selected
- [ ] Missing patientId param redirects to patient list with error
- [ ] Discharge flow
- [ ] Room availability updates after admission/discharge
- [ ] Permission denied scenarios (non-admin delete)
- [ ] Triage code management (admin)
- [ ] Room management (admin)
- [ ] Form validation errors displayed correctly
- [ ] File upload validation (size, type)
- [ ] Consulting physicians: add after admission created
- [ ] Consulting physicians: remove with confirmation
- [ ] Consulting physicians: duplicate prevention error displayed
- [ ] Consulting physicians: treating physician not in dropdown

### General
- [ ] API contract documented
- [ ] Database migrations tested
- [ ] Feature documentation updated
- [ ] Reviewed by project owner

---

## Documentation Updates Required

### Must Update

- [ ] **[CLAUDE.md](../../CLAUDE.md)**
  - Add Patient Admission to "Implemented Features" section

### Review for Consistency

- [ ] **[ARCHITECTURE.md](../architecture/ARCHITECTURE.md)**
  - Add admission entities to entity diagram if exists

### Code Documentation

- [ ] **`Admission.kt`** - Document room availability side effects
- [ ] **`AdmissionService.kt`** - Document discharge and delete behaviors
- [ ] **`RoomRepository.kt`** - Document availability query

---

## Related Docs/Commits/Issues

- Related entity: `Patient` ([Patient.kt](../../api/src/main/kotlin/com/insidehealthgt/hms/entity/Patient.kt))
- Related pattern: `PatientIdDocument` (file storage pattern)
- Related migration: `V015__add_patient_permissions.sql` (permission seeding pattern)

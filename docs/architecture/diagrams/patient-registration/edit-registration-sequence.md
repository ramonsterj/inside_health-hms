# Edit Patient Registration - Sequence Diagram

## Happy Path: Update Existing Patient

```mermaid
sequenceDiagram
    actor Admin as Administrative Staff
    participant UI as PatientForm.vue
    participant Store as usePatientStore
    participant API as Axios Client
    participant Controller as PatientController
    participant Service as PatientService
    participant Repo as PatientRepository
    participant DB as PostgreSQL

    %% Load existing patient
    Admin->>UI: Navigate to /patients/:id/edit
    UI->>Store: fetchPatient(id)
    Store->>API: GET /api/v1/patients/:id
    API->>Controller: getPatient(id)
    Controller->>Service: findById(id, currentUser)
    Service->>Repo: findById(id)
    Repo->>DB: SELECT * FROM patients WHERE id = ?
    DB-->>Repo: Patient + EmergencyContacts
    Repo-->>Service: Patient entity
    Service-->>Controller: PatientResponse
    Controller-->>API: 200 OK
    API-->>Store: Patient data
    Store-->>UI: Populate form

    UI-->>Admin: Form displayed with existing data

    %% Edit patient data
    Admin->>UI: Modify patient fields
    Admin->>UI: Add/remove/edit emergency contacts
    Admin->>UI: Click "Save Changes"

    UI->>UI: Validate form (Zod schema)

    alt Validation Failed
        UI-->>Admin: Show validation errors
    else Validation Passed
        UI->>Store: updatePatient(id, formData)
        Store->>API: PUT /api/v1/patients/:id
        Note over API: Authorization: Bearer {accessToken}

        API->>Controller: updatePatient(id, request)
        Note over Controller: @PreAuthorize("hasPermission('patient:update')")

        Controller->>Controller: Validate request DTO

        alt Request Validation Failed
            Controller-->>API: 400 Bad Request
            API-->>Store: Error response
            Store-->>UI: Show error message
            UI-->>Admin: Display validation errors
        else Request Valid
            Controller->>Service: updatePatient(id, request, currentUser)

            %% Check patient exists
            Service->>Repo: findById(id)
            Repo->>DB: SELECT * FROM patients WHERE id = ?
            DB-->>Repo: Patient entity
            Repo-->>Service: Patient

            alt Patient Not Found
                Service-->>Controller: throw EntityNotFoundException
                Controller-->>API: 404 Not Found
                API-->>Store: Error response
                Store-->>UI: Show error
                UI-->>Admin: "Patient not found"
            else Patient Found
                %% Update patient fields
                Service->>Service: Update patient properties
                Note over Service: firstName, lastName, age, sex, etc.

                Service->>Service: Set updatedBy = currentUser.id
                Note over Service: updatedAt set automatically by @LastModifiedDate

                %% Handle emergency contacts
                Service->>Service: Process emergency contacts
                Note over Service: - Remove contacts not in request<br/>- Update existing contacts<br/>- Add new contacts

                Service->>Repo: findContactsToDelete(patientId, requestContactIds)
                Repo->>DB: SELECT * FROM emergency_contacts WHERE...
                DB-->>Repo: Contacts to soft delete
                Repo-->>Service: List<EmergencyContact>

                Service->>Service: Soft delete removed contacts
                Note over Service: Set deletedAt = now()

                Service->>Service: Update existing contacts
                Service->>Service: Create new contacts

                Service->>Repo: save(patient)
                Repo->>DB: UPDATE patients SET ... WHERE id = ?
                DB->>DB: Trigger: update updated_at
                DB-->>Repo: Updated patient

                Repo->>DB: UPDATE/INSERT emergency_contacts...
                DB-->>Repo: Updated contacts

                Repo-->>Service: Updated Patient entity

                Service->>Service: Convert to PatientResponse
                Note over Service: Include updatedBy user info

                Service-->>Controller: PatientResponse
                Controller-->>API: 200 OK
                API-->>Store: Success response
                Store->>Store: Update patient in state
                Store-->>UI: Patient updated
                UI-->>Admin: Show success message
                UI->>UI: Navigate to patient detail view
            end
        end
    end
```

---

## Emergency Contact Management Detail

```mermaid
sequenceDiagram
    participant Service as PatientService
    participant Repo as PatientRepository
    participant DB as PostgreSQL

    Note over Service: Processing emergency contacts update

    %% Existing contacts from DB
    Service->>Repo: Get existing emergency contacts
    Repo->>DB: SELECT * FROM emergency_contacts<br/>WHERE patient_id = ? AND deleted_at IS NULL
    DB-->>Repo: [Contact1(id=1), Contact2(id=2)]
    Repo-->>Service: existingContacts

    Note over Service: Request contains:<br/>[Contact1(id=1, modified), Contact3(new)]

    %% Identify operations
    Service->>Service: Determine operations
    Note over Service: - Update: Contact1 (id=1 exists)<br/>- Add: Contact3 (no id)<br/>- Delete: Contact2 (id=2 not in request)

    %% Delete Contact2 (soft delete)
    Service->>Service: Set Contact2.deletedAt = now()
    Service->>Repo: save(Contact2)
    Repo->>DB: UPDATE emergency_contacts<br/>SET deleted_at = NOW(), updated_at = NOW()<br/>WHERE id = 2
    DB-->>Repo: Updated

    %% Update Contact1
    Service->>Service: Update Contact1 properties
    Note over Service: name, relationship, phone
    Service->>Repo: save(Contact1)
    Repo->>DB: UPDATE emergency_contacts<br/>SET name=?, relationship=?, phone=?, updated_at=NOW()<br/>WHERE id = 1
    DB-->>Repo: Updated

    %% Add Contact3
    Service->>Service: Create new EmergencyContact
    Note over Service: Set patient_id, createdBy
    Service->>Repo: save(Contact3)
    Repo->>DB: INSERT INTO emergency_contacts<br/>(patient_id, name, relationship, phone, ...)
    DB-->>Repo: Created with id=3

    Service-->>Service: All contacts processed
```

---

## Permission Denied Scenario (Clinical Staff)

```mermaid
sequenceDiagram
    actor Doctor as Doctor (Clinical Staff)
    participant UI as PatientDetail.vue
    participant Router as Vue Router
    participant Store as usePatientStore
    participant API as Axios Client
    participant Controller as PatientController

    %% Doctor can view patient
    Doctor->>UI: View patient details
    UI->>Store: fetchPatient(id)
    Store->>API: GET /api/v1/patients/:id
    Note over API: Doctor has patient:read permission
    API->>Controller: getPatient(id)
    Controller-->>API: 200 OK (PatientResponse)
    API-->>Store: Patient data
    Store-->>UI: Display patient info (read-only)

    UI-->>Doctor: Patient details shown<br/>(no edit button visible)

    %% Doctor tries to edit (direct URL navigation)
    Doctor->>Router: Navigate to /patients/:id/edit

    Router->>Router: Check route permissions
    Note over Router: Requires: patient:update

    Router->>Store: Check user permissions
    Store-->>Router: User lacks patient:update
    Router-->>UI: Redirect to /patients/:id
    UI-->>Doctor: Show "Access Denied" message

    Note over UI,Doctor: Alternative: If somehow bypassing<br/>frontend checks and making API call

    Doctor->>API: PUT /api/v1/patients/:id
    API->>Controller: updatePatient(id, request)

    Controller->>Controller: @PreAuthorize check
    Note over Controller: hasPermission('patient:update') = false

    Controller-->>API: 403 Forbidden
    API-->>Store: { success: false, message: "Access denied" }
    Store-->>UI: Error response
    UI-->>Doctor: Show "You don't have permission" error
```

---

## Validation Error Scenario

```mermaid
sequenceDiagram
    actor Admin as Administrative Staff
    participant UI as PatientForm.vue
    participant Store as usePatientStore
    participant API as Axios Client
    participant Controller as PatientController
    participant Service as PatientService

    Admin->>UI: Edit patient form
    Admin->>UI: Remove all emergency contacts
    Admin->>UI: Click "Save Changes"

    UI->>UI: Validate form (Zod schema)
    Note over UI: emergencyContacts.length < 1

    UI-->>Admin: Show error:<br/>"At least one emergency contact required"

    Note over UI,Admin: Alternative: If validation bypassed

    UI->>Store: updatePatient(id, invalidData)
    Store->>API: PUT /api/v1/patients/:id
    API->>Controller: updatePatient(id, request)

    Controller->>Controller: Validate request DTO
    Note over Controller: @Valid CreatePatientRequest<br/>checks emergencyContacts.size() >= 1

    Controller-->>API: 400 Bad Request
    Note over Controller: ValidationError:<br/>"emergencyContacts: must have at least 1 element"

    API-->>Store: Error response
    Store-->>UI: Validation errors
    UI-->>Admin: Display error messages
```

---

## Patient Not Found Scenario

```mermaid
sequenceDiagram
    actor Admin as Administrative Staff
    participant UI as PatientForm.vue
    participant Store as usePatientStore
    participant API as Axios Client
    participant Controller as PatientController
    participant Service as PatientService
    participant Repo as PatientRepository
    participant DB as PostgreSQL

    Admin->>UI: Navigate to /patients/99999/edit
    UI->>Store: fetchPatient(99999)
    Store->>API: GET /api/v1/patients/99999
    API->>Controller: getPatient(99999)
    Controller->>Service: findById(99999)
    Service->>Repo: findById(99999)
    Repo->>DB: SELECT * FROM patients WHERE id = 99999
    DB-->>Repo: No result
    Repo-->>Service: Optional.empty()

    Service-->>Controller: throw EntityNotFoundException("Patient not found")
    Controller-->>API: 404 Not Found
    API-->>Store: Error response
    Store-->>UI: Patient not found
    UI-->>Admin: Show "Patient not found" error
    UI->>UI: Redirect to /patients
```

---

## Key Components

### Frontend
- **PatientForm.vue**: Patient edit form (same component as create, different mode)
- **PatientDetail.vue**: Read-only view for clinical staff
- **usePatientStore**: Manages patient state and API calls
- **Router Guards**: Check permissions before allowing navigation to edit routes

### Backend
- **PatientController**: REST endpoint (`PUT /api/v1/patients/{id}`)
- **PatientService**: Update logic, contact management, audit trail
- **PatientRepository**: JPA repository for database operations

### Security
- **JWT Access Token**: Required in Authorization header
- **Permission Check**: `patient:update` permission required (ADMIN, ADMINISTRATIVE_STAFF only)
- **Audit Trail**: `updatedBy` and `updatedAt` automatically set via JPA auditing

### Database Operations
- **Update patient**: Single UPDATE statement
- **Emergency contacts**:
  - Soft delete removed contacts (SET deleted_at)
  - Update existing contacts (UPDATE)
  - Insert new contacts (INSERT)
- **Transaction**: All operations in single transaction (rollback on error)

---

## Notes

1. **Emergency Contact Management**:
   - Existing contacts with IDs → UPDATE
   - New contacts without IDs → INSERT
   - Contacts missing from request → Soft DELETE (set `deleted_at`)
   - Minimum 1 contact enforced on both frontend and backend

2. **Audit Trail**:
   - `updatedBy` set from `SecurityContextHolder` current user
   - `updatedAt` set automatically by JPA `@LastModifiedDate`
   - Audit info returned in response for display

3. **Permission Model**:
   - **ADMIN, ADMINISTRATIVE_STAFF**: Can edit patients
   - **DOCTOR, NURSE, CHIEF_NURSE**: Read-only access
   - Frontend hides edit buttons for non-authorized users
   - Backend enforces with `@PreAuthorize` annotations

4. **Validation Layers**:
   - **Frontend**: Zod schema validation (immediate feedback)
   - **Backend**: Spring Validation annotations (server-side enforcement)
   - Both layers check emergency contact minimum requirement

5. **Optimistic Locking** (Future Enhancement):
   - Currently not implemented
   - Could add `@Version` field to prevent concurrent update conflicts
   - Would return 409 Conflict if version mismatch detected

6. **ID Document Handling**:
   - ID document upload/removal handled by separate endpoints
   - Not part of patient update flow
   - See `id-document-upload-sequence.md` for details

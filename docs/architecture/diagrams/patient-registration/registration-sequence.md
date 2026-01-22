# Patient Registration - Sequence Diagram

## Happy Path: Create New Patient

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

    Admin->>UI: Fill patient form + emergency contacts
    Admin->>UI: Click "Register Patient"

    UI->>UI: Validate form (Zod schema)

    alt Validation Failed
        UI-->>Admin: Show validation errors
    else Validation Passed
        UI->>Store: createPatient(formData)
        Store->>API: POST /api/v1/patients
        Note over API: Authorization: Bearer {accessToken}

        API->>Controller: createPatient(request)
        Note over Controller: @PreAuthorize("hasPermission('patient:create')")

        Controller->>Controller: Validate request DTO

        alt Request Validation Failed
            Controller-->>API: 400 Bad Request
            API-->>Store: Error response
            Store-->>UI: Show error message
            UI-->>Admin: Display validation errors
        else Request Valid
            Controller->>Service: createPatient(request, currentUser)

            %% Duplicate Detection
            Service->>Repo: findByFirstNameAndLastNameAndAge(...)
            Repo->>DB: SELECT * FROM patients WHERE...
            DB-->>Repo: Potential duplicates
            Repo-->>Service: List<Patient>

            Service->>Repo: findByIdDocumentNumber(...)
            Repo->>DB: SELECT * FROM patients WHERE id_document_number = ?
            DB-->>Repo: Potential duplicates by ID
            Repo-->>Service: Optional<Patient>

            alt Duplicates Found
                Service-->>Controller: 409 Conflict (DuplicatePatientResponse)
                Controller-->>API: 409 with duplicate info
                API-->>Store: Conflict response
                Store-->>UI: Show duplicate warning
                UI-->>Admin: Display potential duplicates
            else No Duplicates
                %% Create Patient
                Service->>Service: Create Patient entity
                Note over Service: Set audit fields (createdBy = currentUser.id)

                Service->>Service: Create EmergencyContact entities
                Service->>Service: Link contacts to patient

                Service->>Repo: save(patient)
                Repo->>DB: INSERT INTO patients (...)
                DB->>DB: Trigger: set created_at, updated_at
                DB-->>Repo: Patient record created

                Repo->>DB: INSERT INTO emergency_contacts (...)
                DB-->>Repo: Emergency contacts created

                Repo-->>Service: Patient entity with ID

                Service->>Service: Convert to PatientResponse
                Note over Service: Include createdBy user info

                Service-->>Controller: PatientResponse
                Controller-->>API: 201 Created
                API-->>Store: Success response
                Store->>Store: Add patient to state
                Store-->>UI: Patient created
                UI-->>Admin: Show success message
                UI->>UI: Navigate to patient detail view
            end
        end
    end
```

---

## Edge Case: Duplicate Detection

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

    Admin->>UI: Submit patient form
    UI->>Store: createPatient(formData)
    Store->>API: POST /api/v1/patients
    API->>Controller: createPatient(request)
    Controller->>Service: createPatient(request, currentUser)

    Service->>Repo: findByFirstNameAndLastNameAndAge(...)
    Repo->>DB: SELECT * FROM patients WHERE...
    DB-->>Repo: [Existing Patient]
    Repo-->>Service: List with 1 patient

    Service->>Service: Build DuplicatePatientResponse
    Note over Service: Include: id, firstName, lastName, age, idDocumentNumber

    Service-->>Controller: throw DuplicatePatientException
    Controller->>Controller: Handle exception
    Controller-->>API: 409 Conflict
    Note over Controller: Response includes potential duplicates

    API-->>Store: { success: false, data: { potentialDuplicates: [...] } }
    Store-->>UI: Error with duplicate info

    UI->>UI: Show duplicate warning dialog
    Note over UI: Display:<br/>- Existing patient info<br/>- "Proceed anyway" option (future)<br/>- "View existing" link

    UI-->>Admin: Duplicate warning displayed

    alt Admin Reviews Duplicate
        Admin->>UI: Click "View existing patient"
        UI->>UI: Navigate to existing patient detail
    else Admin Cancels
        Admin->>UI: Click "Cancel"
        UI->>UI: Stay on form, allow editing
    end
```

---

## Permission Denied Scenario

```mermaid
sequenceDiagram
    actor Nurse as Nurse (Clinical Staff)
    participant UI as PatientForm.vue
    participant Router as Vue Router
    participant Store as usePatientStore
    participant API as Axios Client
    participant Controller as PatientController

    Nurse->>UI: Navigate to /patients/new

    Router->>Router: Check route permissions
    Note over Router: Requires: patient:create

    alt No Permission
        Router->>Store: Check user permissions
        Store-->>Router: User lacks patient:create
        Router-->>UI: Redirect to /patients
        UI-->>Nurse: Show "Access Denied" message
    else Has Permission (shouldn't happen for nurse)
        Router-->>UI: Allow navigation
        Nurse->>UI: Submit patient form
        UI->>Store: createPatient(formData)
        Store->>API: POST /api/v1/patients
        API->>Controller: createPatient(request)

        Controller->>Controller: @PreAuthorize check
        Note over Controller: hasPermission('patient:create') = false

        Controller-->>API: 403 Forbidden
        API-->>Store: { success: false, message: "Access denied" }
        Store-->>UI: Error response
        UI-->>Nurse: Show "You don't have permission" error
    end
```

---

## Key Components

### Frontend
- **PatientForm.vue**: Patient registration form with validation
- **usePatientStore**: Pinia store managing patient state and API calls
- **Zod Schema**: Client-side validation for patient data

### Backend
- **PatientController**: REST endpoint (`POST /api/v1/patients`)
- **PatientService**: Business logic, duplicate detection, entity creation
- **PatientRepository**: JPA repository for database operations

### Security
- **JWT Access Token**: Required in Authorization header
- **Permission Check**: `patient:create` permission required
- **Audit Trail**: `createdBy` and `createdAt` automatically set via JPA auditing

### Database
- **patients**: Main patient table
- **emergency_contacts**: Related emergency contact records
- **patient_id_documents**: Optional ID document storage (added separately via file upload)

---

## Notes

1. **Duplicate Detection Logic**:
   - Match by `firstName + lastName + age` OR `idDocumentNumber`
   - Returns 409 Conflict with list of potential duplicates
   - Frontend shows warning dialog

2. **Validation Layers**:
   - **Frontend**: Zod schema validation (immediate feedback)
   - **Backend**: Spring Validation annotations (server-side enforcement)

3. **Audit Trail**:
   - `createdBy` set from `SecurityContextHolder` current user
   - `createdAt` set automatically by JPA `@CreatedDate`
   - Audit info returned in response for display

4. **Emergency Contacts**:
   - Minimum 1 required (validated on both frontend and backend)
   - Created in same transaction as patient
   - Cascade operations handled by JPA

5. **ID Document Upload**:
   - **Not** part of initial registration
   - Separate API call: `POST /api/v1/patients/{id}/id-document`
   - See `id-document-upload-sequence.md` for details

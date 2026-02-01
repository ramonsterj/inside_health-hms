# Feature: File System Storage

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-27 | @author | Initial draft |

---

## Overview

Move file storage from database BYTEA columns to the local file system. This change is needed because more files will be uploaded in future features and storing them in the database would impact performance. Files will be organized in patient-specific directories to ensure files are never mixed between patients.

---

## Use Case / User Story

1. **As a system administrator**, I want to configure where files are stored on the file system so that I can manage storage location according to infrastructure requirements.

2. **As a staff member**, I want to upload patient documents that are stored on the file system so that database performance is not impacted by file storage.

3. **As a staff member**, I want to download patient documents seamlessly so that the change in storage backend is transparent to my workflow.

4. **As a system administrator**, I want patient files organized in patient-specific directories so that files are never mixed between patients and can be easily located or audited.

5. **As a security officer**, I want files stored outside the web root so that uploaded files cannot be directly accessed via URL.

---

## Authorization / Role Access

No changes to existing permissions. This feature changes the storage mechanism, not access control.

| Action | Required Role(s) | Permission | Notes |
|--------|------------------|------------|-------|
| Upload Patient ID | Staff roles | `patient:upload-id` | Existing permission |
| View Patient ID | Staff roles | `patient:view-id` | Existing permission |
| Upload Consent | Staff roles | `admission:upload-consent` | Existing permission |
| View Consent | Staff roles | `admission:view-consent` | Existing permission |

**Future**: Resident Physician role (to be created separately) will also have document upload permissions.

---

## Functional Requirements

1. Store uploaded files on the local file system instead of database BYTEA columns
2. Organize files in patient-specific directories (files never mixed between patients)
3. Maintain existing file validation (size limits, allowed content types)
4. Maintain existing upload/download API contracts (no breaking changes for frontend)
5. Support soft deletes (keep metadata in DB, handle physical file cleanup)
6. Configurable storage path via application configuration
7. Storage path must be outside web root (security requirement)

---

## Acceptance Criteria / Scenarios

### Happy Path

- When a file is uploaded, it is stored on the file system in a patient-specific directory, and metadata is saved to the database
- When a file is downloaded, it is retrieved from the file system using the stored path
- When a file is deleted (soft delete), metadata is marked as deleted and physical file is handled according to cleanup policy
- When the application starts, it validates the configured storage path exists and is writable

### Configuration

- When `app.file-storage.base-path` is configured, files are stored under that directory
- When storage path is outside web root, files cannot be accessed directly via URL

### Directory Structure

- When uploading a file for a patient, the file is stored in `/base-path/patients/{patientId}/...`
- When uploading files for different patients, files are never mixed in the same directory

### Edge Cases - Validation

- When file exceeds size limit, return 400 with message: *"The file is too large. Maximum allowed size is X MB."*
- When file type is not allowed, return 400 with message: *"This file type is not supported. Please upload a JPEG, PNG, or PDF file."*
- When file is empty, return 400 with message: *"The selected file is empty. Please choose a valid file."*

### Edge Cases - Storage Errors

- When storage path does not exist at startup, application creates it or fails with clear error in logs
- When storage path is not writable, return 500 with message: *"Unable to save the file. Please contact support."* (details logged for admins)
- When disk is full during upload, return 500 with message: *"Unable to save the file. Please contact support."* and rollback database changes
- When physical file is missing but metadata exists (download), return 404 with message: *"The requested file could not be found. Please contact support."* (error logged for investigation)

### Edge Cases - Security

- When path traversal is attempted in filename (e.g., `../../../etc/passwd`), return 400 with message: *"Invalid filename. Please rename the file and try again."*
- When user lacks permission, return 403 with message: *"You don't have permission to perform this action."*

### Edge Cases - Concurrency

- When multiple files are uploaded for same patient simultaneously, each file is stored without collision

---

## Non-Functional Requirements

- **Security**: Storage path MUST be outside web root (essential)
- **Security**: Sanitize all filenames to prevent path traversal attacks
- **Reliability**: Database transaction must rollback if file system write fails
- **Reliability**: Log all storage errors with full details for admin investigation
- **Extensibility**: Design service to support future document types easily

---

## API Contract

No changes to existing API endpoints. The upload/download contracts remain the same.

| Method | Endpoint | Request | Response | Auth | Description |
|--------|----------|---------|----------|------|-------------|
| POST | `/api/v1/patients/{id}/id-document` | `multipart/form-data` | `PatientResponse` | Yes | Upload patient ID (unchanged) |
| GET | `/api/v1/patients/{id}/id-document` | - | `byte[]` | Yes | Download patient ID (unchanged) |
| DELETE | `/api/v1/patients/{id}/id-document` | - | `PatientResponse` | Yes | Delete patient ID (unchanged) |
| POST | `/api/v1/admissions/{id}/consent` | `multipart/form-data` | `AdmissionDetailResponse` | Yes | Upload consent (unchanged) |
| GET | `/api/v1/admissions/{id}/consent` | - | `byte[]` | Yes | Download consent (unchanged) |

**Note**: Frontend requires no changes as API contracts are preserved.

---

## Database Changes

### Modified Entities

| Entity | Table | Change |
|--------|-------|--------|
| `PatientIdDocument` | `patient_id_documents` | Replace `file_data: ByteArray` with `storage_path: String` |
| `AdmissionConsentDocument` | `admission_consent_documents` | Replace `file_data: ByteArray` with `storage_path: String` |

### New Migrations

| Migration | Description |
|-----------|-------------|
| `V028__add_storage_path_to_document_tables.sql` | Add `storage_path` column, drop `file_data` column |

### Schema Changes

```sql
-- V028__add_storage_path_to_document_tables.sql

-- Patient ID Documents
ALTER TABLE patient_id_documents
    ADD COLUMN storage_path VARCHAR(500);

-- Since we're starting fresh, we can drop file_data immediately
-- If there was existing data, we'd need a data migration step first
ALTER TABLE patient_id_documents
    DROP COLUMN file_data;

ALTER TABLE patient_id_documents
    ALTER COLUMN storage_path SET NOT NULL;

-- Admission Consent Documents
ALTER TABLE admission_consent_documents
    ADD COLUMN storage_path VARCHAR(500);

ALTER TABLE admission_consent_documents
    DROP COLUMN file_data;

ALTER TABLE admission_consent_documents
    ALTER COLUMN storage_path SET NOT NULL;
```

### Index Requirements

- [x] `deleted_at` - Already exists on both tables
- [ ] No new indexes required (storage_path is not queried directly)

---

## Backend Changes

### New Service

| Class | Location | Description |
|-------|----------|-------------|
| `FileStorageService` | `com.insidehealthgt.hms.service` | Handles file system operations |

```kotlin
// FileStorageService.kt
@Service
class FileStorageService(
    @Value("\${app.file-storage.base-path}")
    private val basePath: String
) {

    @PostConstruct
    fun init() {
        // Validate and create base directory if needed
    }

    /**
     * Store a file for a patient.
     * @return The relative storage path
     */
    fun storeFile(patientId: Long, documentType: String, file: MultipartFile): String

    /**
     * Load a file from storage.
     * @throws FileNotFoundException if file doesn't exist
     */
    fun loadFile(storagePath: String): ByteArray

    /**
     * Delete a file from storage.
     */
    fun deleteFile(storagePath: String)

    /**
     * Sanitize filename to prevent path traversal.
     */
    private fun sanitizeFilename(filename: String): String
}
```

### Directory Structure

```
{base-path}/
└── patients/
    └── {patientId}/
        ├── id-documents/
        │   └── {uuid}_{sanitized-filename}.{ext}
        └── consent-documents/
            └── {uuid}_{sanitized-filename}.{ext}
```

### Modified Services

| Class | Changes |
|-------|---------|
| `PatientService` | Inject `FileStorageService`, update `uploadIdDocument()`, `getIdDocument()`, `deleteIdDocument()` |
| `AdmissionService` | Inject `FileStorageService`, update `uploadConsentDocument()`, `getConsentDocument()` |

### Configuration

```yaml
# application.yml
app:
  file-storage:
    base-path: ${FILE_STORAGE_PATH:/var/data/hms/files}
```

```yaml
# application-dev.yml (for local development)
app:
  file-storage:
    base-path: ./data/files
```

### Modified Entities

```kotlin
// PatientIdDocument.kt - Replace fileData with storagePath
@Entity
@Table(name = "patient_id_documents")
@SQLRestriction("deleted_at IS NULL")
class PatientIdDocument(
    @Column(name = "file_name", nullable = false, length = 255)
    var fileName: String,

    @Column(name = "content_type", nullable = false, length = 100)
    var contentType: String,

    @Column(name = "file_size", nullable = false)
    var fileSize: Long,

    @Column(name = "storage_path", nullable = false, length = 500)
    var storagePath: String,  // NEW: replaces fileData

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false, unique = true)
    var patient: Patient? = null,
) : BaseEntity() {
    companion object {
        const val MAX_FILE_SIZE: Long = 5 * 1024 * 1024 // 5MB
        val ALLOWED_CONTENT_TYPES = setOf(
            "image/jpeg",
            "image/png",
            "application/pdf",
        )
    }
}
```

```kotlin
// AdmissionConsentDocument.kt - Same pattern
@Column(name = "storage_path", nullable = false, length = 500)
var storagePath: String,  // NEW: replaces fileData
```

---

## Frontend Changes

### Components

N/A - No frontend changes required. API contracts remain unchanged.

### Pinia Stores

N/A - Existing stores continue to work without modification.

### Routes

N/A - No route changes.

### Validation (Zod Schemas)

N/A - Existing validation unchanged.

---

## Implementation Notes

1. **Transaction Handling**: File write should happen before database commit. If file write fails, throw exception to rollback transaction. If database commit fails after file write, consider cleanup or accept orphaned file (logged for manual cleanup).

2. **UUID for Uniqueness**: Use UUID prefix on filenames to prevent collisions when same filename is uploaded multiple times.

3. **Testcontainers**: For integration tests, use a temp directory as the storage path. Clean up after tests.

4. **Soft Delete Behavior**: When a document is soft-deleted, the physical file can be:
   - Option A: Kept on disk (for audit/recovery) - RECOMMENDED initially
   - Option B: Deleted immediately
   - Option C: Scheduled for cleanup after retention period

   Start with Option A for safety. Implement cleanup job in future if storage becomes a concern.

5. **Error Messages**: All user-facing error messages should be in plain English. Technical details should be logged for administrators.

6. **Pattern Reference**: Follow patterns from existing services in the codebase (e.g., `PatientService`, `AdmissionService`).

---

## QA Checklist

### Backend
- [ ] All functional requirements implemented
- [ ] `FileStorageService` created with proper error handling
- [ ] Entity `PatientIdDocument` updated (storagePath replaces fileData)
- [ ] Entity `AdmissionConsentDocument` updated (storagePath replaces fileData)
- [ ] Entities have `@SQLRestriction("deleted_at IS NULL")`
- [ ] DTOs unchanged (no entity exposure)
- [ ] Input validation in place (file size, type, filename sanitization)
- [ ] Path traversal attacks prevented
- [ ] Storage path validated at startup
- [ ] Unit tests written and passing
- [ ] Integration tests written and passing (Testcontainers + temp directory)
- [ ] Detekt passes (no new violations)
- [ ] OWASP dependency-check passes

### Frontend
- [ ] N/A - No frontend changes (verify existing functionality still works)

### E2E Tests (Playwright)
- [ ] Patient ID document upload works
- [ ] Patient ID document download works
- [ ] Patient ID document delete works
- [ ] Admission consent upload works
- [ ] Admission consent download works
- [ ] File validation errors displayed correctly (size, type)
- [ ] Permission denied scenarios work as before

### General
- [ ] API contract unchanged (verified with existing tests)
- [ ] Database migration tested
- [ ] Configuration documented
- [ ] Feature documentation updated
- [ ] Reviewed by project owner

---

## Documentation Updates Required

### Must Update

- [ ] **[CLAUDE.md](../../CLAUDE.md)**
  - Add File System Storage to "Implemented Features"
  - Document configuration property `app.file-storage.base-path`

### Review for Consistency

- [ ] **[README.md](../../README.md)** or **[api/README.md](../../api/README.md)**
  - Add setup instructions for file storage path
  - Document environment variable `FILE_STORAGE_PATH`

### Code Documentation

- [ ] **`FileStorageService.kt`**
  - Add KDoc comments for all public methods
- [ ] **Entity changes**
  - Update any existing documentation referencing file storage

---

## Related Docs/Commits/Issues

- Related entities: `PatientIdDocument`, `AdmissionConsentDocument`
- Related services: `PatientService`, `AdmissionService`
- Related migrations: `V013__create_patient_id_documents_table.sql`, `V024__create_admission_consent_documents_table.sql`

# Feature: Multi-Document Upload System

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-02-03 | @author | Initial draft |

---

## Overview

Replace the single-document approach with a flexible multi-document system supporting various document types (consent forms, inventory lists, etc.). Each document gets an auto-generated thumbnail, and users can view PDFs and images through an integrated document viewer. This replaces the current text area inventory with document uploads and allows multiple consent documents of different types.

---

## Use Case / User Story

1. **As an Admin/Administrative Staff member**, I want to upload multiple documents of various types for a patient so that I can collect all required paperwork flexibly as it becomes available.

2. **As an Admin/Administrative Staff member**, I want to assign a custom name to each uploaded document so that I can easily identify and organize patient documents.

3. **As an Admin/Administrative Staff member**, I want to see thumbnail previews of all patient documents so that I can quickly identify documents without opening them.

4. **As an Admin/Administrative Staff member**, I want to view PDFs and images in an integrated document viewer so that I can review documents without downloading them.

5. **As an Admin/Administrative Staff member**, I want to download patient documents so that I can print or share them externally when needed.

6. **As an Admin**, I want to manage document types (create, edit, delete) so that the system can accommodate different kinds of patient documents.

---

## Authorization / Role Access

### Document Operations

| Action | Admin | Administrative Staff | Permission | Notes |
|--------|-------|---------------------|------------|-------|
| View documents | ✅ | ✅ | `admission:view-documents` | View thumbnails and open viewer |
| Upload documents | ✅ | ✅ | `admission:upload-documents` | Upload new documents |
| Download documents | ✅ | ✅ | `admission:download-documents` | Download original files |
| Delete documents | ✅ | ❌ | `admission:delete-documents` | Soft delete only |

### Document Type Management

| Action | Admin | Administrative Staff | Permission | Notes |
|--------|-------|---------------------|------------|-------|
| View document types | ✅ | ✅ | `document-type:read` | For dropdown selection |
| Create document type | ✅ | ❌ | `document-type:create` | |
| Update document type | ✅ | ❌ | `document-type:update` | |
| Delete document type | ✅ | ❌ | `document-type:delete` | Cannot delete if documents exist |

### Audit Logging

All document operations (upload, view, download, delete) are logged to the audit log with user, timestamp, and action details.

---

## Functional Requirements

### Document Upload

- FR1: Support multiple configurable document types (consent forms, inventory, ID documents, etc.)
- FR2: Allow multiple documents per admission (no limit on number of documents)
- FR3: All document types allow multiple uploads of the same type
- FR4: User can assign a custom display name when uploading (optional, defaults to original filename)
- FR5: Maximum file size: 25MB (consistent with existing consent document limit)
- FR6: Allowed file types: PDF, JPEG, PNG
- FR7: Documents are optional - can be uploaded as they become available

### Thumbnail Generation

- FR8: Generate thumbnail for each uploaded document automatically
- FR9: PDF thumbnails: Generate from first page
- FR10: Image thumbnails: Generate resized version (max 200x200 pixels)
- FR11: Store thumbnails on file system alongside original documents
- FR12: If thumbnail generation fails, use a generic placeholder icon (PDF icon or image icon)

### Document Viewer

- FR13: In-app document viewer that supports PDFs and images
- FR14: PDF viewer: Render all pages with navigation controls (prev/next, page number, zoom)
- FR15: Image viewer: Display image with zoom capabilities
- FR16: Viewer opens in a modal/dialog overlay

### Document Management

- FR17: Documents are downloadable with their assigned name
- FR18: Only Admins can delete documents (soft delete)
- FR19: Deleted documents are no longer visible but file is preserved on disk

### Document Type Management (Admin)

- FR20: CRUD operations for document types
- FR21: Document types have: code, name, description (optional), icon (optional), displayOrder
- FR22: Cannot delete document type if documents of that type exist
- FR23: Seed psychiatric hospital document types:
  - CONSENT_ADMISSION: General admission consent
  - CONSENT_ISOLATION: Consent for patient isolation/seclusion
  - CONSENT_RESTRAINT: Consent for physical restraints (immobilization)
  - CONSENT_SEDATION: Consent for sedation/medication administration
  - INVENTORY_LIST: Written inventory of patient belongings
  - INVENTORY_PHOTO: Photos of patient belongings
  - OTHER: Other admission-related documents

### Migration from Current System

- FR24: Replace text area inventory field with "Inventory" document type
- FR25: Existing consent documents migrated to new system with type CONSENT_GENERAL
- FR26: Existing patient ID documents remain separate (patient-level, not admission-level)

---

## Acceptance Criteria / Scenarios

### Happy Path - Document Upload

- When a user uploads a valid file with a custom name, the document is saved and a thumbnail is generated
- When a document is uploaded, an audit log entry is created with the action details
- When uploading multiple documents of the same type, all are saved separately
- When custom name is not provided, the original filename is used as the display name

### Happy Path - Document Viewing

- When a user clicks on a document thumbnail, the document viewer opens displaying the content
- When viewing a PDF, the viewer renders all pages with navigation controls
- When viewing an image, the viewer displays the image with zoom capabilities
- When viewing a document, an audit log entry is created

### Happy Path - Document Download

- When a user clicks download, the original file is downloaded with its assigned name
- When a document is downloaded, an audit log entry is created

### Happy Path - Document Deletion (Admin only)

- When an Admin deletes a document, it is soft-deleted and no longer visible
- When a document is deleted, an audit log entry is created

### Happy Path - Document Type Management (Admin only)

- When an Admin creates a new document type, it becomes available for uploads
- When an Admin edits a document type name, existing documents retain the updated type
- When an Admin deletes a document type with no documents, it is removed

### Edge Cases - Validation

- When file exceeds 25MB, return 400 with message: *"The file is too large. Maximum allowed size is 25MB."*
- When file type is not supported (not PDF/JPEG/PNG), return 400 with message: *"This file type is not supported. Please upload a PDF, JPEG, or PNG file."*
- When custom name is empty, use original filename as default
- When custom name exceeds 255 characters, return 400 with validation error
- When document type ID is invalid, return 400 with validation error

### Edge Cases - Authorization

- When Administrative Staff attempts to delete a document, return 403 Forbidden
- When Administrative Staff attempts to manage document types, return 403 Forbidden
- When unauthenticated user attempts any action, return 401 Unauthorized

### Edge Cases - Not Found

- When document ID doesn't exist, return 404 Not Found
- When admission ID doesn't exist, return 404 Not Found
- When document type ID doesn't exist, return 404 Not Found

### Edge Cases - Thumbnail Generation

- When thumbnail generation fails for a PDF, display a generic PDF icon placeholder
- When thumbnail generation fails for an image, display a generic image icon placeholder

### Edge Cases - Document Type Deletion

- When attempting to delete a document type that has existing documents, return 400 with message: *"Cannot delete document type. Documents of this type exist."*

---

## Non-Functional Requirements

- **File Size**: Maximum 25MB per document
- **Performance**: Thumbnail generation should not block the upload response (async processing acceptable)
- **Reliability**: All document operations logged to audit_logs table
- **Security**: Files stored outside web root (existing FileStorageService pattern)

---

## API Contract

### Admission Document Endpoints

| Method | Endpoint | Request | Response | Auth | Description |
|--------|----------|---------|----------|------|-------------|
| GET | `/api/v1/admissions/{id}/documents` | - | `List<AdmissionDocumentResponse>` | Yes | List all documents for admission |
| POST | `/api/v1/admissions/{id}/documents` | `multipart/form-data` | `AdmissionDocumentResponse` | Yes | Upload document |
| GET | `/api/v1/admissions/{id}/documents/{docId}` | - | `AdmissionDocumentResponse` | Yes | Get document metadata |
| GET | `/api/v1/admissions/{id}/documents/{docId}/file` | - | `byte[]` | Yes | Download original file |
| GET | `/api/v1/admissions/{id}/documents/{docId}/thumbnail` | - | `byte[]` | Yes | Get thumbnail |
| DELETE | `/api/v1/admissions/{id}/documents/{docId}` | - | - | Yes | Soft delete document |

### Document Type Endpoints (Admin)

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| GET | `/api/v1/document-types` | - | `List<DocumentTypeResponse>` | Yes | List all document types |
| GET | `/api/v1/document-types/{id}` | - | `DocumentTypeResponse` | Yes | Get document type by ID |
| POST | `/api/v1/document-types` | `CreateDocumentTypeRequest` | `DocumentTypeResponse` | Yes | Create document type |
| PUT | `/api/v1/document-types/{id}` | `UpdateDocumentTypeRequest` | `DocumentTypeResponse` | Yes | Update document type |
| DELETE | `/api/v1/document-types/{id}` | - | - | Yes | Delete document type |

### Request/Response Examples

```json
// POST /api/v1/admissions/{id}/documents - multipart/form-data
// Form fields:
//   file: (binary)
//   documentTypeId: 1
//   displayName: "Consent Form - Surgery" (optional)

// Response - AdmissionDocumentResponse
{
  "id": 1,
  "documentType": {
    "id": 3,
    "code": "CONSENT_RESTRAINT",
    "name": "Restraint Consent",
    "description": "Consent for physical restraints (immobilization)"
  },
  "displayName": "Restraint Consent - Signed",
  "fileName": "consent_restraint_20260203.pdf",
  "contentType": "application/pdf",
  "fileSize": 245678,
  "hasThumbnail": true,
  "thumbnailUrl": "/v1/admissions/1/documents/1/thumbnail",
  "downloadUrl": "/v1/admissions/1/documents/1/file",
  "createdAt": "2026-02-03T10:30:00Z",
  "createdBy": {
    "id": 5,
    "username": "receptionist1"
  }
}

// GET /api/v1/admissions/{id}/documents - Response
[
  {
    "id": 1,
    "documentType": {
      "id": 1,
      "code": "CONSENT_ADMISSION",
      "name": "Admission Consent"
    },
    "displayName": "General Admission Consent",
    "fileName": "consent_admission.pdf",
    "contentType": "application/pdf",
    "fileSize": 245678,
    "hasThumbnail": true,
    "thumbnailUrl": "/api/v1/admissions/1/documents/1/thumbnail",
    "downloadUrl": "/api/v1/admissions/1/documents/1/file",
    "createdAt": "2026-02-03T10:30:00Z",
    "createdBy": {
      "id": 5,
      "username": "receptionist1"
    }
  },
  {
    "id": 2,
    "documentType": {
      "id": 6,
      "code": "INVENTORY_PHOTO",
      "name": "Inventory Photos"
    },
    "displayName": "Patient Belongings - Wallet and Phone",
    "fileName": "belongings_photo.jpg",
    "contentType": "image/jpeg",
    "fileSize": 1234567,
    "hasThumbnail": true,
    "thumbnailUrl": "/v1/admissions/1/documents/2/thumbnail",
    "downloadUrl": "/v1/admissions/1/documents/2/file",
    "createdAt": "2026-02-03T10:35:00Z",
    "createdBy": {
      "id": 5,
      "username": "receptionist1"
    }
  },
  {
    "id": 3,
    "documentType": {
      "id": 3,
      "code": "CONSENT_RESTRAINT",
      "name": "Restraint Consent"
    },
    "displayName": "Restraint Authorization - Family Signed",
    "fileName": "consent_restraint_signed.pdf",
    "contentType": "application/pdf",
    "fileSize": 189234,
    "hasThumbnail": true,
    "thumbnailUrl": "/v1/admissions/1/documents/3/thumbnail",
    "downloadUrl": "/v1/admissions/1/documents/3/file",
    "createdAt": "2026-02-03T14:20:00Z",
    "createdBy": {
      "id": 5,
      "username": "receptionist1"
    }
  }
]

// POST /api/v1/document-types - Request
{
  "code": "CONSENT_ELECTROSHOCK",
  "name": "Electroconvulsive Therapy Consent",
  "description": "Consent form for ECT procedures",
  "displayOrder": 5
}

// Response - DocumentTypeResponse
{
  "id": 8,
  "code": "CONSENT_ELECTROSHOCK",
  "name": "Electroconvulsive Therapy Consent",
  "description": "Consent form for ECT procedures",
  "displayOrder": 5,
  "createdAt": "2026-02-03T10:00:00Z"
}
```

---

## Database Changes

### New Entities

| Entity | Table | Extends | Description |
|--------|-------|---------|-------------|
| `DocumentType` | `document_types` | `BaseEntity` | Configurable document types |
| `AdmissionDocument` | `admission_documents` | `BaseEntity` | Documents uploaded for admissions |

### Modified Entities

| Entity | Table | Change |
|--------|-------|--------|
| `Admission` | `admissions` | Remove `inventory` TEXT column (data migrated to documents) |

### Deprecated Entities

| Entity | Table | Notes |
|--------|-------|-------|
| `AdmissionConsentDocument` | `admission_consent_documents` | Migrate to `admission_documents`, then remove |

### New Migrations

| Migration | Description |
|-----------|-------------|
| `V029__create_document_types_table.sql` | Creates document_types table with seed data |
| `V030__create_admission_documents_table.sql` | Creates admission_documents table |
| `V031__migrate_consent_documents.sql` | Migrates existing consent documents to new table |
| `V032__migrate_inventory_to_documents.sql` | Creates inventory documents from text field |
| `V033__drop_legacy_columns.sql` | Removes inventory column and old consent table |
| `V034__add_document_permissions.sql` | Adds permissions for document operations |

### Schema

```sql
-- V029__create_document_types_table.sql
CREATE TABLE document_types (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    display_order INT NOT NULL DEFAULT 0,
    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_document_types_deleted_at ON document_types(deleted_at);
CREATE INDEX idx_document_types_code ON document_types(code);
CREATE INDEX idx_document_types_display_order ON document_types(display_order);

-- Seed psychiatric hospital document types
INSERT INTO document_types (code, name, description, display_order, created_at, updated_at) VALUES
('CONSENT_ADMISSION', 'Admission Consent', 'General admission consent form', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CONSENT_ISOLATION', 'Isolation Consent', 'Consent for patient isolation/seclusion', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CONSENT_RESTRAINT', 'Restraint Consent', 'Consent for physical restraints (immobilization)', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CONSENT_SEDATION', 'Sedation Consent', 'Consent for sedation/medication administration', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('INVENTORY_LIST', 'Inventory List', 'Written inventory of patient belongings', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('INVENTORY_PHOTO', 'Inventory Photos', 'Photos of patient belongings', 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('OTHER', 'Other Document', 'Other admission-related documents', 99, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- V030__create_admission_documents_table.sql
CREATE TABLE admission_documents (
    id BIGSERIAL PRIMARY KEY,
    admission_id BIGINT NOT NULL REFERENCES admissions(id),
    document_type_id BIGINT NOT NULL REFERENCES document_types(id),
    display_name VARCHAR(255) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    thumbnail_path VARCHAR(500),
    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_admission_documents_deleted_at ON admission_documents(deleted_at);
CREATE INDEX idx_admission_documents_admission_id ON admission_documents(admission_id);
CREATE INDEX idx_admission_documents_document_type_id ON admission_documents(document_type_id);

-- V031__migrate_consent_documents.sql
-- Migrate existing consent documents to new table (as general admission consent)
INSERT INTO admission_documents (
    admission_id, document_type_id, display_name, file_name, content_type,
    file_size, storage_path, created_at, updated_at, created_by, updated_by
)
SELECT
    acd.admission_id,
    (SELECT id FROM document_types WHERE code = 'CONSENT_ADMISSION'),
    acd.file_name,
    acd.file_name,
    acd.content_type,
    acd.file_size,
    acd.storage_path,
    acd.created_at,
    acd.updated_at,
    acd.created_by,
    acd.updated_by
FROM admission_consent_documents acd
WHERE acd.deleted_at IS NULL;

-- V032__migrate_inventory_to_documents.sql
-- Note: Inventory was a TEXT field. If there's text content, we'll need to handle
-- this in application code or create a document placeholder.
-- For now, we just prepare to remove the column.

-- V033__drop_legacy_columns.sql
-- Remove inventory column from admissions
ALTER TABLE admissions DROP COLUMN IF EXISTS inventory;

-- Drop old consent documents table (after verifying migration)
DROP TABLE IF EXISTS admission_consent_documents;

-- V034__add_document_permissions.sql
INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('admission:view-documents', 'View Admission Documents', 'View documents for admissions', 'admission', 'view-documents', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('admission:upload-documents', 'Upload Admission Documents', 'Upload documents for admissions', 'admission', 'upload-documents', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('admission:download-documents', 'Download Admission Documents', 'Download documents from admissions', 'admission', 'download-documents', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('admission:delete-documents', 'Delete Admission Documents', 'Delete documents from admissions', 'admission', 'delete-documents', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('document-type:create', 'Create Document Type', 'Create new document types', 'document-type', 'create', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('document-type:read', 'Read Document Type', 'View document types', 'document-type', 'read', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('document-type:update', 'Update Document Type', 'Modify document types', 'document-type', 'update', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('document-type:delete', 'Delete Document Type', 'Delete document types', 'document-type', 'delete', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Assign ADMIN full access to all new resources
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMIN' AND p.resource IN ('document-type')
   OR (r.code = 'ADMIN' AND p.code LIKE 'admission:%documents');

-- Assign ADMINISTRATIVE_STAFF document view/upload/download (not delete)
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMINISTRATIVE_STAFF'
  AND p.code IN ('admission:view-documents', 'admission:upload-documents', 'admission:download-documents', 'document-type:read');
```

### Index Requirements

- [x] `deleted_at` - Required for soft delete queries on all tables
- [x] `admission_id` - FK lookup for admission documents
- [x] `document_type_id` - FK lookup and filtering by type
- [x] `code` - Lookup document types by code
- [x] `display_order` - Sorting document types in dropdown

---

## Backend Changes

### New Services

| Class | Location | Description |
|-------|----------|-------------|
| `DocumentTypeService` | `com.insidehealthgt.hms.service` | CRUD operations for document types |
| `AdmissionDocumentService` | `com.insidehealthgt.hms.service` | Document upload, download, thumbnail management |
| `ThumbnailService` | `com.insidehealthgt.hms.service` | Thumbnail generation for PDFs and images |

### New Controllers

| Class | Location | Description |
|-------|----------|-------------|
| `DocumentTypeController` | `com.insidehealthgt.hms.controller` | Document type management endpoints |

### Modified Services

| Class | Changes |
|-------|---------|
| `AdmissionService` | Remove inventory handling, integrate with AdmissionDocumentService |
| `FileStorageService` | Add support for thumbnail storage, update DocumentType enum |

### New Entities

```kotlin
// DocumentType.kt
@Entity
@Table(name = "document_types")
@SQLRestriction("deleted_at IS NULL")
class DocumentType(
    @Column(nullable = false, unique = true, length = 50)
    var code: String,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(length = 255)
    var description: String? = null,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0,
) : BaseEntity()

// AdmissionDocument.kt
@Entity
@Table(name = "admission_documents")
@SQLRestriction("deleted_at IS NULL")
class AdmissionDocument(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admission_id", nullable = false)
    var admission: Admission,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_type_id", nullable = false)
    var documentType: DocumentType,

    @Column(name = "display_name", nullable = false, length = 255)
    var displayName: String,

    @Column(name = "file_name", nullable = false, length = 255)
    var fileName: String,

    @Column(name = "content_type", nullable = false, length = 100)
    var contentType: String,

    @Column(name = "file_size", nullable = false)
    var fileSize: Long,

    @Column(name = "storage_path", nullable = false, length = 500)
    var storagePath: String,

    @Column(name = "thumbnail_path", length = 500)
    var thumbnailPath: String? = null,
) : BaseEntity() {
    companion object {
        const val MAX_FILE_SIZE: Long = 25 * 1024 * 1024 // 25MB
        val ALLOWED_CONTENT_TYPES = setOf(
            "image/jpeg",
            "image/png",
            "application/pdf",
        )
    }
}
```

### Thumbnail Generation

```kotlin
// ThumbnailService.kt
@Service
class ThumbnailService {
    companion object {
        const val THUMBNAIL_MAX_WIDTH = 200
        const val THUMBNAIL_MAX_HEIGHT = 200
    }

    /**
     * Generate thumbnail for a document.
     * @param filePath Path to the original file
     * @param contentType MIME type of the file
     * @return Path to generated thumbnail, or null if generation failed
     */
    fun generateThumbnail(filePath: Path, contentType: String): Path?

    /**
     * Generate thumbnail for PDF (first page).
     */
    private fun generatePdfThumbnail(filePath: Path): Path?

    /**
     * Generate thumbnail for image (resized).
     */
    private fun generateImageThumbnail(filePath: Path): Path?
}
```

### Directory Structure Update

```
{base-path}/
└── admissions/
    └── {admissionId}/
        └── documents/
            ├── {uuid}_{filename}.pdf
            ├── {uuid}_{filename}_thumb.png
            ├── {uuid}_{filename}.jpg
            └── {uuid}_{filename}_thumb.png
```

### Dependencies for Thumbnail Generation

```kotlin
// build.gradle.kts - add for PDF rendering
implementation("org.apache.pdfbox:pdfbox:3.0.1")

// Image processing uses Java's built-in ImageIO
```

---

## Frontend Changes

### Components

| Component | Location | Description |
|-----------|----------|-------------|
| `DocumentList.vue` | `src/components/documents/` | Grid of document thumbnails with actions |
| `DocumentUploadDialog.vue` | `src/components/documents/` | Upload dialog with type selection and name input |
| `DocumentViewer.vue` | `src/components/documents/` | Modal viewer for PDFs and images |
| `PdfViewer.vue` | `src/components/documents/` | PDF rendering with page navigation |
| `ImageViewer.vue` | `src/components/documents/` | Image display with zoom |
| `DocumentThumbnail.vue` | `src/components/documents/` | Single thumbnail with hover actions |
| `DocumentTypeList.vue` | `src/views/admin/` | Admin: Manage document types |
| `DocumentTypeForm.vue` | `src/components/admin/` | Admin: Create/edit document type |

### Pinia Stores

| Store | Location | Description |
|-------|----------|-------------|
| `useDocumentStore` | `src/stores/document.ts` | Document CRUD, upload progress |
| `useDocumentTypeStore` | `src/stores/documentType.ts` | Document type management |

### Routes

| Path | Component | Auth Required | Permission |
|------|-----------|---------------|------------|
| `/admin/document-types` | `DocumentTypeList` | Yes | `document-type:read` |
| `/admin/document-types/new` | `DocumentTypeForm` | Yes | `document-type:create` |
| `/admin/document-types/:id/edit` | `DocumentTypeForm` | Yes | `document-type:update` |

### Validation (Zod Schemas)

```typescript
// src/schemas/document.ts
import { z } from 'zod'

export const uploadDocumentSchema = z.object({
  file: z
    .instanceof(File)
    .refine((file) => file.size <= 25 * 1024 * 1024, 'File must be under 25MB')
    .refine(
      (file) => ['application/pdf', 'image/jpeg', 'image/png'].includes(file.type),
      'File must be PDF, JPEG, or PNG'
    ),
  documentTypeId: z.number().positive('Document type is required'),
  displayName: z.string().max(255, 'Name must be 255 characters or less').optional(),
})

export const documentTypeSchema = z.object({
  code: z.string().min(1).max(50, 'Code must be 1-50 characters').regex(/^[A-Z_]+$/, 'Code must be uppercase with underscores'),
  name: z.string().min(1).max(100, 'Name must be 1-100 characters'),
  description: z.string().max(255).optional(),
  displayOrder: z.number().int().min(0).default(0),
})
```

### PDF Viewer Library

```json
// package.json - add dependency
{
  "dependencies": {
    "vue-pdf-embed": "^2.1.0"
  }
}
```

Alternative: Use `pdfjs-dist` directly for more control.

---

## Implementation Notes

1. **Thumbnail Generation Timing**: Generate thumbnails synchronously during upload for immediate availability. If this causes latency issues, move to async processing with placeholder display.

2. **PDF Rendering**: Use Apache PDFBox on backend for thumbnail generation. On frontend, use vue-pdf-embed or pdfjs-dist for viewing.

3. **Existing Pattern**: Follow `FileStorageService` pattern for file storage. Documents stored in `admissions/{admissionId}/documents/` directory.

4. **Migration Strategy**:
   - Phase 1: Create new tables and entities
   - Phase 2: Migrate existing consent documents
   - Phase 3: Handle inventory text (option to create placeholder document or discard)
   - Phase 4: Remove legacy columns/tables

5. **Audit Integration**: Use existing `AuditEntityListener` pattern. Add audit log entries for view/download actions that don't modify entities.

6. **Document Viewer State**: Use Pinia store to track which document is being viewed. Viewer component is rendered at app level (portal) for proper z-index.

7. **Error Handling**: If thumbnail generation fails, store `null` in `thumbnail_path` and show placeholder icon in UI.

---

## QA Checklist

### Backend
- [ ] All functional requirements implemented
- [ ] `DocumentType` entity extends `BaseEntity`
- [ ] `DocumentType` entity has `@SQLRestriction("deleted_at IS NULL")`
- [ ] `AdmissionDocument` entity extends `BaseEntity`
- [ ] `AdmissionDocument` entity has `@SQLRestriction("deleted_at IS NULL")`
- [ ] DTOs used in controllers (no entity exposure)
- [ ] File upload validation (size, type)
- [ ] Thumbnail generation for PDFs works
- [ ] Thumbnail generation for images works
- [ ] Thumbnail failure handled gracefully (placeholder)
- [ ] Document download returns correct content-type and filename
- [ ] Audit logging for all document operations
- [ ] Cannot delete document type with existing documents
- [ ] Migration scripts work correctly
- [ ] Unit tests written and passing
- [ ] Integration tests written and passing (Testcontainers)
- [ ] Detekt passes (no new violations)
- [ ] OWASP dependency-check passes

### Frontend
- [ ] `DocumentList` component displays thumbnails in grid
- [ ] `DocumentUploadDialog` with type selection and optional name
- [ ] `DocumentViewer` modal opens on thumbnail click
- [ ] `PdfViewer` renders PDFs with page navigation
- [ ] `ImageViewer` displays images with zoom
- [ ] Document download works
- [ ] Delete document works (Admin only)
- [ ] Document type management (Admin)
- [ ] Pinia stores implemented
- [ ] Routes configured with proper guards
- [ ] Form validation with VeeValidate + Zod
- [ ] Error handling implemented
- [ ] Upload progress indicator
- [ ] ESLint/oxlint passes
- [ ] i18n keys added for all user-facing text
- [ ] Unit tests written and passing (Vitest)

### E2E Tests (Playwright)
- [ ] Upload document with custom name
- [ ] Upload document without custom name (uses filename)
- [ ] Upload multiple documents of same type
- [ ] View PDF in document viewer
- [ ] View image in document viewer
- [ ] Navigate PDF pages
- [ ] Zoom image
- [ ] Download document
- [ ] Delete document (Admin)
- [ ] Delete document denied (Administrative Staff)
- [ ] Document type CRUD (Admin)
- [ ] Thumbnail displays for PDF
- [ ] Thumbnail displays for image
- [ ] File validation errors (size, type)

### General
- [ ] API contract documented
- [ ] Database migrations tested
- [ ] Migration from existing consent documents verified
- [ ] Feature documentation updated
- [ ] Reviewed by project owner

---

## Documentation Updates Required

### Must Update

- [ ] **[CLAUDE.md](../../CLAUDE.md)**
  - Add Multi-Document Upload System to "Implemented Features"
  - Update file storage section to mention documents directory

- [ ] **[patient-admission.md](./patient-admission.md)**
  - Remove inventory textarea references
  - Remove single consent document references
  - Add reference to this document for document management

- [ ] **[file-system-storage.md](./file-system-storage.md)**
  - Add `admissions/{id}/documents/` to directory structure
  - Add thumbnail storage pattern

- [ ] **[new-patient-intake.md](./new-patient-intake.md)**
  - Note: Patient ID documents remain separate (not affected by this feature)

### Review for Consistency

- [ ] **API documentation** (if using OpenAPI/Swagger)
  - Add new endpoints

### Code Documentation

- [ ] **`AdmissionDocumentService.kt`** - Document upload, thumbnail, and download logic
- [ ] **`ThumbnailService.kt`** - Document thumbnail generation methods
- [ ] **`DocumentTypeService.kt`** - Document type CRUD

---

## Related Docs/Commits/Issues

- Related feature: [File System Storage](./file-system-storage.md) - Storage pattern
- Related feature: [Patient Admission](./patient-admission.md) - Admission context
- Related entity: `AdmissionConsentDocument` (to be migrated/deprecated)
- Related migration: `V024__create_admission_consent_documents_table.sql` (legacy)

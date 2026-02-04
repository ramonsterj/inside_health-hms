-- Add permissions for document operations
INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('admission:view-documents', 'View Admission Documents', 'View documents for admissions', 'admission', 'view-documents', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('admission:upload-documents', 'Upload Admission Documents', 'Upload documents for admissions', 'admission', 'upload-documents', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('admission:download-documents', 'Download Admission Documents', 'Download documents from admissions', 'admission', 'download-documents', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('admission:delete-documents', 'Delete Admission Documents', 'Delete documents from admissions', 'admission', 'delete-documents', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('document-type:create', 'Create Document Type', 'Create new document types', 'document-type', 'create', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('document-type:read', 'Read Document Type', 'View document types', 'document-type', 'read', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('document-type:update', 'Update Document Type', 'Modify document types', 'document-type', 'update', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('document-type:delete', 'Delete Document Type', 'Delete document types', 'document-type', 'delete', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Assign ADMIN full access to document type management
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMIN'
  AND p.code IN (
    'document-type:create',
    'document-type:read',
    'document-type:update',
    'document-type:delete',
    'admission:view-documents',
    'admission:upload-documents',
    'admission:download-documents',
    'admission:delete-documents'
  );

-- Assign ADMINISTRATIVE_STAFF document view/upload/download (not delete) and document-type:read
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMINISTRATIVE_STAFF'
  AND p.code IN (
    'admission:view-documents',
    'admission:upload-documents',
    'admission:download-documents',
    'document-type:read'
  );

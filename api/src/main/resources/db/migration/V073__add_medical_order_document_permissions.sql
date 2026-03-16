-- Add permissions for medical order document management

INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('medical-order:upload-document', 'Upload Medical Order Document', 'Upload documents to medical orders', 'medical-order', 'upload-document', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('medical-order:delete-document', 'Delete Medical Order Document', 'Delete documents from medical orders', 'medical-order', 'delete-document', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ADMIN gets both permissions
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMIN'
  AND p.code IN (
    'medical-order:upload-document',
    'medical-order:delete-document'
  );

-- DOCTOR gets upload permission
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'DOCTOR'
  AND p.code IN (
    'medical-order:upload-document'
  );

-- NURSE gets upload permission
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'NURSE'
  AND p.code IN (
    'medical-order:upload-document'
  );

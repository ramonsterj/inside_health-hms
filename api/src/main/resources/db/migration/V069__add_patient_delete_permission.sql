-- Add patient:delete permission
INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at)
VALUES ('patient:delete', 'Delete Patient', 'Delete patients (soft delete)', 'patient', 'delete', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Grant to ADMIN role
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMIN' AND p.code = 'patient:delete'
  AND r.deleted_at IS NULL AND p.deleted_at IS NULL;

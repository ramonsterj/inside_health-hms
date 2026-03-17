-- V084: Treasury permissions
INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('treasury:read', 'View Treasury', 'View treasury records', 'treasury', 'read', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('treasury:write', 'Write Treasury', 'Create and edit treasury records', 'treasury', 'write', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('treasury:delete', 'Delete Treasury', 'Delete treasury records', 'treasury', 'delete', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('treasury:configure', 'Configure Treasury', 'Configure bank accounts and employees', 'treasury', 'configure', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('treasury:reconcile', 'Reconcile Treasury', 'Upload and reconcile bank statements', 'treasury', 'reconcile', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('treasury:report', 'Treasury Reports', 'View financial reports', 'treasury', 'report', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Assign all treasury permissions to ADMIN role
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMIN' AND p.resource = 'treasury';

-- Assign all treasury permissions to ADMINISTRATIVE_STAFF role
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMINISTRATIVE_STAFF' AND p.resource = 'treasury';

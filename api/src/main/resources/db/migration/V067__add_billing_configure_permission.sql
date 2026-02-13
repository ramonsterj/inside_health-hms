-- Step 1: Insert permission
INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('billing:configure', 'Configure Billing Settings', 'Configure system-level billing settings', 'billing', 'configure', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Step 2: ADMIN only
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMIN' AND p.code = 'billing:configure';

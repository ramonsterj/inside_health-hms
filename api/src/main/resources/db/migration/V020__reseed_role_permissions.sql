-- Re-seed role permissions to fix missing permission assignments
-- This ensures all roles have their correct permissions assigned

-- Clear existing role_permissions to avoid duplicates
DELETE FROM role_permissions;

-- Assign ALL permissions to ADMIN role
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMIN';

-- USER role has no special permissions (can only access own profile via /api/users/me)

-- Assign clinical permissions to DOCTOR role
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'DOCTOR' AND p.code IN (
    'user:read',
    'patient:read'
)
ON CONFLICT DO NOTHING;

-- Assign clinical permissions to NURSE role
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'NURSE' AND p.code IN (
    'user:read',
    'patient:read'
)
ON CONFLICT DO NOTHING;

-- Assign clinical permissions to CHIEF_NURSE role
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'CHIEF_NURSE' AND p.code IN (
    'user:read',
    'patient:read'
)
ON CONFLICT DO NOTHING;

-- Assign all patient permissions to ADMINISTRATIVE_STAFF role
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMINISTRATIVE_STAFF' AND p.code IN (
    'user:read',
    'patient:create',
    'patient:read',
    'patient:update',
    'patient:upload-id',
    'patient:view-id'
)
ON CONFLICT DO NOTHING;

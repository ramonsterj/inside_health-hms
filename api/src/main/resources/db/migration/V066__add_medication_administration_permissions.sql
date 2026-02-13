-- Step 1: Insert permissions
INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('medication-administration:create', 'Create Medication Administration', 'Record a medication administration', 'medication-administration', 'create', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('medication-administration:read', 'View Medication Administrations', 'View medication administration records', 'medication-administration', 'read', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Step 2: ADMIN gets full access
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMIN' AND p.resource = 'medication-administration';

-- Step 3: NURSE gets create + read
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'NURSE'
AND p.code IN ('medication-administration:create', 'medication-administration:read');

-- Step 4: CHIEF_NURSE gets create + read
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'CHIEF_NURSE'
AND p.code IN ('medication-administration:create', 'medication-administration:read');

-- Step 5: DOCTOR gets read only
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'DOCTOR'
AND p.code = 'medication-administration:read';

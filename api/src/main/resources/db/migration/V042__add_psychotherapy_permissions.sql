-- Add PSYCHOLOGIST role (if not exists)
INSERT INTO roles (code, name, description, is_system, created_at, updated_at)
SELECT 'PSYCHOLOGIST', 'Psychologist', 'Mental health psychologists', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'PSYCHOLOGIST');

-- Psychotherapy Activity permissions
INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('psychotherapy-activity:create', 'Create Psychotherapy Activity', 'Register psychotherapy activities', 'psychotherapy-activity', 'create', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('psychotherapy-activity:read', 'Read Psychotherapy Activity', 'View psychotherapy activities', 'psychotherapy-activity', 'read', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('psychotherapy-activity:delete', 'Delete Psychotherapy Activity', 'Delete psychotherapy activities', 'psychotherapy-activity', 'delete', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Psychotherapy Category permissions
INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('psychotherapy-category:create', 'Create Psychotherapy Category', 'Create activity categories', 'psychotherapy-category', 'create', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('psychotherapy-category:read', 'Read Psychotherapy Category', 'View activity categories', 'psychotherapy-category', 'read', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('psychotherapy-category:update', 'Update Psychotherapy Category', 'Modify activity categories', 'psychotherapy-category', 'update', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('psychotherapy-category:delete', 'Delete Psychotherapy Category', 'Delete activity categories', 'psychotherapy-category', 'delete', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Assign ADMIN full access to both resources
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMIN' AND p.resource IN ('psychotherapy-activity', 'psychotherapy-category');

-- Assign PSYCHOLOGIST: create/read activities, read categories
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'PSYCHOLOGIST' AND p.code IN (
    'psychotherapy-activity:create', 'psychotherapy-activity:read',
    'psychotherapy-category:read'
);

-- Assign DOCTOR: read activities, read categories
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'DOCTOR' AND p.code IN (
    'psychotherapy-activity:read',
    'psychotherapy-category:read'
);

-- Assign NURSE: read activities, read categories
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'NURSE' AND p.code IN (
    'psychotherapy-activity:read',
    'psychotherapy-category:read'
);

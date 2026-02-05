-- Add permissions for medical record operations (clinical history, progress notes, medical orders)

INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
-- Clinical History permissions
('clinical-history:create', 'Create Clinical History', 'Create clinical history for admissions', 'clinical-history', 'create', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('clinical-history:read', 'Read Clinical History', 'View clinical history for admissions', 'clinical-history', 'read', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('clinical-history:update', 'Update Clinical History', 'Modify clinical history for admissions', 'clinical-history', 'update', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Progress Note permissions
('progress-note:create', 'Create Progress Note', 'Create progress notes for admissions', 'progress-note', 'create', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('progress-note:read', 'Read Progress Note', 'View progress notes for admissions', 'progress-note', 'read', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('progress-note:update', 'Update Progress Note', 'Modify progress notes for admissions', 'progress-note', 'update', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Medical Order permissions
('medical-order:create', 'Create Medical Order', 'Create medical orders for admissions', 'medical-order', 'create', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('medical-order:read', 'Read Medical Order', 'View medical orders for admissions', 'medical-order', 'read', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('medical-order:update', 'Update Medical Order', 'Modify medical orders for admissions', 'medical-order', 'update', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('medical-order:discontinue', 'Discontinue Medical Order', 'Discontinue active medical orders', 'medical-order', 'discontinue', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ADMIN gets all permissions
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMIN'
  AND p.code IN (
    'clinical-history:create',
    'clinical-history:read',
    'clinical-history:update',
    'progress-note:create',
    'progress-note:read',
    'progress-note:update',
    'medical-order:create',
    'medical-order:read',
    'medical-order:update',
    'medical-order:discontinue'
  );

-- DOCTOR gets create/read clinical-history, create/read progress-note, create/read/discontinue medical-order
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'DOCTOR'
  AND p.code IN (
    'clinical-history:create',
    'clinical-history:read',
    'progress-note:create',
    'progress-note:read',
    'medical-order:create',
    'medical-order:read',
    'medical-order:discontinue'
  );

-- NURSE gets read clinical-history, create/read progress-note, read medical-order
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'NURSE'
  AND p.code IN (
    'clinical-history:read',
    'progress-note:create',
    'progress-note:read',
    'medical-order:read'
  );

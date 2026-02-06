-- Nursing Note permissions
INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('nursing-note:read', 'Read Nursing Note', 'View nursing notes', 'nursing-note', 'read', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('nursing-note:create', 'Create Nursing Note', 'Create nursing notes', 'nursing-note', 'create', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('nursing-note:update', 'Update Nursing Note', 'Update nursing notes', 'nursing-note', 'update', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Vital Sign permissions
INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('vital-sign:read', 'Read Vital Sign', 'View vital signs', 'vital-sign', 'read', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('vital-sign:create', 'Create Vital Sign', 'Record vital signs', 'vital-sign', 'create', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('vital-sign:update', 'Update Vital Sign', 'Update vital signs', 'vital-sign', 'update', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Assign all nursing permissions to ADMIN role
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMIN' AND p.resource IN ('nursing-note', 'vital-sign');

-- Assign all nursing permissions to DOCTOR role
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'DOCTOR' AND p.resource IN ('nursing-note', 'vital-sign');

-- Assign all nursing permissions to NURSE role
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'NURSE' AND p.resource IN ('nursing-note', 'vital-sign');

-- Assign all nursing permissions to CHIEF_NURSE role
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'CHIEF_NURSE' AND p.resource IN ('nursing-note', 'vital-sign');

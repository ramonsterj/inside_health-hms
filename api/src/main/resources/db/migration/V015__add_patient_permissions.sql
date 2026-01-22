-- Add patient-related permissions
INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('patient:create', 'Create Patient', 'Register new patients', 'patient', 'create', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('patient:read', 'Read Patient', 'View patient information', 'patient', 'read', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('patient:update', 'Update Patient', 'Modify patient information', 'patient', 'update', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('patient:upload-id', 'Upload Patient ID', 'Upload patient ID document', 'patient', 'upload-id', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('patient:view-id', 'View Patient ID', 'View patient ID document', 'patient', 'view-id', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Assign all patient permissions to ADMIN role
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMIN' AND p.resource = 'patient';

-- Assign all patient permissions to ADMINISTRATIVE_STAFF role
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMINISTRATIVE_STAFF' AND p.resource = 'patient';

-- Assign read-only permission to clinical roles (DOCTOR, NURSE, CHIEF_NURSE)
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('DOCTOR', 'NURSE', 'CHIEF_NURSE') AND p.code = 'patient:read';

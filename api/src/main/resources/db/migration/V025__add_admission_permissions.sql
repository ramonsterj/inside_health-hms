-- Add permissions for admission-related resources

-- Triage Code permissions
INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('triage-code:create', 'Create Triage Code', 'Create new triage codes', 'triage-code', 'create', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('triage-code:read', 'Read Triage Code', 'View triage codes', 'triage-code', 'read', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('triage-code:update', 'Update Triage Code', 'Modify triage codes', 'triage-code', 'update', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('triage-code:delete', 'Delete Triage Code', 'Delete triage codes', 'triage-code', 'delete', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Room permissions
INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('room:create', 'Create Room', 'Create new rooms', 'room', 'create', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('room:read', 'Read Room', 'View rooms', 'room', 'read', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('room:update', 'Update Room', 'Modify rooms', 'room', 'update', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('room:delete', 'Delete Room', 'Delete rooms', 'room', 'delete', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Admission permissions
INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('admission:create', 'Create Admission', 'Register new admissions', 'admission', 'create', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('admission:read', 'Read Admission', 'View admissions', 'admission', 'read', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('admission:update', 'Update Admission', 'Modify admissions', 'admission', 'update', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('admission:delete', 'Delete Admission', 'Delete admissions', 'admission', 'delete', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('admission:upload-consent', 'Upload Consent', 'Upload consent documents', 'admission', 'upload-consent', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('admission:view-consent', 'View Consent', 'View consent documents', 'admission', 'view-consent', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Assign ALL new permissions to ADMIN role
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMIN' AND p.resource IN ('triage-code', 'room', 'admission')
ON CONFLICT DO NOTHING;

-- Assign read access for triage codes and rooms to ADMINISTRATIVE_STAFF
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMINISTRATIVE_STAFF' AND p.code IN ('triage-code:read', 'room:read')
ON CONFLICT DO NOTHING;

-- Assign admission permissions (except delete) to ADMINISTRATIVE_STAFF
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMINISTRATIVE_STAFF' AND p.resource = 'admission' AND p.action != 'delete'
ON CONFLICT DO NOTHING;

-- Assign read-only admission access to clinical roles (DOCTOR, NURSE, CHIEF_NURSE)
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('DOCTOR', 'NURSE', 'CHIEF_NURSE') AND p.code IN ('admission:read', 'admission:view-consent', 'triage-code:read', 'room:read')
ON CONFLICT DO NOTHING;

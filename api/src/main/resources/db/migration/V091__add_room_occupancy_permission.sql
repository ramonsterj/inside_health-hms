-- Add the room:occupancy-view permission used by the bed occupancy screen.
-- Hyphen in `action` matches existing convention (e.g. V005 user:reset-password,
-- V025 admission:upload-consent). Permission codes use `<resource>:<action>` form.

INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('room:occupancy-view', 'View Bed Occupancy', 'View hospital-wide bed occupancy screen', 'room', 'occupancy-view', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Grant to ADMIN.
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMIN' AND p.code = 'room:occupancy-view'
ON CONFLICT DO NOTHING;

-- Grant to nursing and admin/staff roles. Explicitly NOT granted to DOCTOR:
-- the bed occupancy screen must not be visible to doctor-only users.
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('NURSE', 'CHIEF_NURSE', 'ADMINISTRATIVE_STAFF')
  AND p.code = 'room:occupancy-view'
ON CONFLICT DO NOTHING;


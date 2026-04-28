-- V093: Add medical-order workflow permissions for the v1.2 state machine.
-- Adds:
--   * medical-order:authorize           (ADMIN, ADMINISTRATIVE_STAFF)
--   * medical-order:emergency-authorize (ADMIN, DOCTOR)
--   * medical-order:mark-in-progress    (ADMIN, DOCTOR, NURSE, CHIEF_NURSE)
--
-- The RESULTADOS_RECIBIDOS terminal state is reached implicitly through
-- medical-order:upload-document, so no new permission is needed for it.

INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('medical-order:authorize',           'Authorize Medical Order',           'Approve or reject medical orders',                                                  'medical-order', 'authorize',           CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('medical-order:emergency-authorize', 'Emergency Authorize Medical Order', 'Doctor self-authorization for crisis or after-hours scenarios',                     'medical-order', 'emergency-authorize', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('medical-order:mark-in-progress',    'Mark Medical Order In Progress',    'Mark a results-bearing order as executed (sample taken / referred / administered)', 'medical-order', 'mark-in-progress',    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- ADMIN gets all three workflow permissions.
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMIN'
  AND p.code IN ('medical-order:authorize',
                 'medical-order:emergency-authorize',
                 'medical-order:mark-in-progress')
  AND r.deleted_at IS NULL AND p.deleted_at IS NULL
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ADMINISTRATIVE_STAFF gets authorize + read access (so the dashboard can render).
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMINISTRATIVE_STAFF'
  AND p.code IN ('medical-order:authorize', 'medical-order:read')
  AND r.deleted_at IS NULL AND p.deleted_at IS NULL
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- DOCTOR gets emergency-authorize and mark-in-progress.
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'DOCTOR'
  AND p.code IN ('medical-order:emergency-authorize',
                 'medical-order:mark-in-progress')
  AND r.deleted_at IS NULL AND p.deleted_at IS NULL
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- NURSE and CHIEF_NURSE get mark-in-progress (the sample-taking worklist owner).
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('NURSE', 'CHIEF_NURSE')
  AND p.code = 'medical-order:mark-in-progress'
  AND r.deleted_at IS NULL AND p.deleted_at IS NULL
ON CONFLICT (role_id, permission_id) DO NOTHING;

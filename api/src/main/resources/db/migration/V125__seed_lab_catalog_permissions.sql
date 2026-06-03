-- ============================================================================
-- V125: Lab catalog permissions.
--   lab-catalog:read   — read providers / tests / provider-tests / panels and
--                        resolve panels (feeds the order form).
--   lab-catalog:manage — full CRUD on the catalog (soft deletes only).
--
-- read   → ADMIN, DOCTOR, RESIDENT_DOCTOR only. Deliberately NOT granted by the
--          broad medical-order:read permission (held by PSYCHOLOGIST / NURSE /
--          CHIEF_NURSE / AUXILIARY_NURSE / ADMINISTRATIVE_STAFF), which must not
--          see or order labs (AC10 / AC13).
-- manage → ADMIN only.
-- ============================================================================

INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('lab-catalog:read',   'Read Lab Catalog',   'View lab providers, tests, provider-tests, and panels', 'lab-catalog', 'read',   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('lab-catalog:manage', 'Manage Lab Catalog', 'Full CRUD on lab providers, tests, provider-tests, and panels', 'lab-catalog', 'manage', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- read → ADMIN, DOCTOR, RESIDENT_DOCTOR
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'DOCTOR', 'RESIDENT_DOCTOR')
  AND p.code = 'lab-catalog:read'
  AND r.deleted_at IS NULL AND p.deleted_at IS NULL
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- manage → ADMIN only
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMIN'
  AND p.code = 'lab-catalog:manage'
  AND r.deleted_at IS NULL AND p.deleted_at IS NULL
ON CONFLICT (role_id, permission_id) DO NOTHING;

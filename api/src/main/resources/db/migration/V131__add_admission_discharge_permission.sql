-- ============================================================================
-- V131: Add dedicated admission discharge permission
-- ============================================================================
-- Discharge used to be gated by admission:update, which also allows admission
-- metadata edits and consulting-physician management. Discharge is now a
-- restricted action with its own permission so it can be granted independently
-- of that broader update authority.
--
-- Grant model (discharge is allowed for ADMINISTRADOR and MEDICO_RESIDENTE only):
--   * ADMINISTRADOR receives it through the "all permissions" grant below.
--   * MEDICO_RESIDENTE receives discharge access explicitly.
-- No other role (PERSONAL_ADMINISTRATIVO, JEFE_ENFERMERIA, etc.) may discharge,
-- even though some hold admission:update.
--
-- The permission text is stored in Spanish because seeded/reference data is
-- Spanish-default after V128; the frontend renders through i18n keys.
-- ============================================================================

INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at)
VALUES (
    'admission:discharge',
    'Dar Alta de Admisión',
    'Dar de alta a pacientes admitidos',
    'admission',
    'discharge',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (code) DO NOTHING;

-- Admin gets all permissions, including this one.
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMINISTRADOR'
  AND p.code = 'admission:discharge'
  AND r.deleted_at IS NULL
  AND p.deleted_at IS NULL
ON CONFLICT DO NOTHING;

-- Resident doctors can discharge (with a mandatory note), without granting the
-- broader admission:update permission.
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'MEDICO_RESIDENTE'
  AND p.code = 'admission:discharge'
  AND r.deleted_at IS NULL
  AND p.deleted_at IS NULL
ON CONFLICT DO NOTHING;

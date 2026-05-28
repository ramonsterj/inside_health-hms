-- ============================================================================
-- V114: Add RESIDENT_DOCTOR role and grant it every DOCTOR permission
-- ============================================================================
-- Residents are the medical professionals who register and run admissions in
-- the hospital workflow. Their permission set is intentionally identical to
-- DOCTOR; the role exists primarily so we can (a) auto-bind the admitting
-- resident on every admission and (b) scope listings differently from DOCTOR.
--
-- NOTE for operators: because V115 auto-binds the resident slot to the current
-- authenticated user, any account that needs to create admissions (including
-- the seeded `admin` user) must ALSO carry RESIDENT_DOCTOR. Dev seed grants
-- this to admin in R__seed_01_reset_and_base.sql.
-- ============================================================================

INSERT INTO roles (code, name, description, is_system, created_at, updated_at)
VALUES (
    'RESIDENT_DOCTOR',
    'Resident Doctor',
    'Medical resident in charge of admissions',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (code) DO NOTHING;

-- Clone every DOCTOR permission grant onto RESIDENT_DOCTOR.
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT
    (SELECT id FROM roles WHERE code = 'RESIDENT_DOCTOR'),
    rp.permission_id,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id
WHERE r.code = 'DOCTOR'
ON CONFLICT DO NOTHING;

-- Residents register admissions, so grant admission:create on top of the
-- DOCTOR base (DOCTOR itself does NOT have admission:create).
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT
    (SELECT id FROM roles WHERE code = 'RESIDENT_DOCTOR'),
    p.id,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM permissions p
WHERE p.code = 'admission:create'
ON CONFLICT DO NOTHING;

-- Lock progress-note and nursing-note update to ADMIN-only.
--
-- Rule (per `docs/features/medical-psychiatric-record.md` v1.4 and
-- `docs/features/nursing-module.md` v1.3):
--   Doctors, nurses, and chief nurses can CREATE and READ progress notes and
--   nursing notes, but cannot UPDATE them. Only ADMIN can update.
--   Discharge protection still blocks all writes (including ADMIN) — that
--   guard lives at the service layer, not in role-permission grants.
--
-- This migration:
--   1) Grants `progress-note:create` to CHIEF_NURSE so the chief nurse can
--      author notes (V038 originally only granted ADMIN/DOCTOR/NURSE create).
--   2) Revokes any existing `progress-note:update` grants from non-admin roles
--      (DOCTOR/NURSE — granted by the briefly-merged V095, deleted before
--      release; CHIEF_NURSE — granted by an older seed file).
--   3) Revokes any existing `nursing-note:update` grants from non-admin roles
--      (granted by V045 to DOCTOR/NURSE/CHIEF_NURSE).
--
-- All inserts and deletes are idempotent.

-- (1) Grant progress-note:create + progress-note:read to CHIEF_NURSE
--     (V038 didn't grant CHIEF_NURSE any progress-note permissions; the seed file
--     historically did, but we want migration-level parity so non-seeded environments
--     also have it.)
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'CHIEF_NURSE'
  AND p.code IN ('progress-note:create', 'progress-note:read')
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- (2) Revoke progress-note:update from non-admin roles
DELETE FROM role_permissions
WHERE role_id IN (SELECT id FROM roles WHERE code IN ('DOCTOR', 'NURSE', 'CHIEF_NURSE'))
  AND permission_id IN (SELECT id FROM permissions WHERE code = 'progress-note:update');

-- (3) Revoke nursing-note:update from non-admin roles
DELETE FROM role_permissions
WHERE role_id IN (SELECT id FROM roles WHERE code IN ('DOCTOR', 'NURSE', 'CHIEF_NURSE'))
  AND permission_id IN (SELECT id FROM permissions WHERE code = 'nursing-note:update');

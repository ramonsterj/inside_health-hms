-- Lock vital-sign update to ADMIN-only.
--
-- Rule (per `docs/features/nursing-module.md` v1.4):
--   Doctors, nurses, and chief nurses can CREATE and READ vital signs, but
--   cannot UPDATE them. Only ADMIN can update. The previous 24-hour creator
--   self-correction window is removed: vital signs are now append-only for
--   non-admins, matching the rule already in place for nursing notes (V096)
--   and progress notes.
--   Discharge protection still blocks all writes (including ADMIN) — that
--   guard lives at the service layer, not in role-permission grants.
--
-- This migration revokes any existing `vital-sign:update` grants from
-- non-admin roles (granted by V045 to DOCTOR / NURSE / CHIEF_NURSE).
--
-- Idempotent: safe to re-run.

DELETE FROM role_permissions
WHERE role_id IN (SELECT id FROM roles WHERE code IN ('DOCTOR', 'NURSE', 'CHIEF_NURSE'))
  AND permission_id IN (SELECT id FROM permissions WHERE code = 'vital-sign:update');

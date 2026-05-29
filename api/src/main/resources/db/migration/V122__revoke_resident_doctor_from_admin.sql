-- Revoke RESIDENT_DOCTOR from the seeded `admin` user.
--
-- Patient admissions may only be registered by a RESIDENT_DOCTOR (auto-bound to
-- the authenticated user). ADMIN is the sole exception: an admin registering an
-- admission must explicitly pick the resident doctor (residentId in the request),
-- handled in AdmissionService.resolveResident(). Admin therefore no longer needs
-- to "be" a resident — see docs/features/patient-admission.md.
--
-- admin keeps admission:create through the ADMIN role's own grant, so no permission
-- migration is needed here. Scoped to username = 'admin'; idempotent.
DELETE FROM user_roles
WHERE user_id = (SELECT id FROM users WHERE username = 'admin')
  AND role_id = (SELECT id FROM roles WHERE code = 'RESIDENT_DOCTOR');

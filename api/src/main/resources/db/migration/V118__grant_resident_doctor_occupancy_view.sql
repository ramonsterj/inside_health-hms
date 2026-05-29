-- Grant room:occupancy-view to RESIDENT_DOCTOR.
--
-- The bed occupancy screen is now the default dashboard (landing page) for
-- nurses and resident doctors (see docs/features/bed-occupancy-view.md).
-- RESIDENT_DOCTOR (V114) was created by cloning DOCTOR's grants, and DOCTOR is
-- deliberately excluded from room:occupancy-view (V091) — so residents did not
-- previously hold it. Without this grant the router would bounce a resident
-- from /dashboard → /bed-occupancy → (permission denied) → /dashboard in a loop.
--
-- Still NOT granted to the plain DOCTOR role: doctor-only users must not see
-- the bed occupancy screen.
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'RESIDENT_DOCTOR' AND p.code = 'room:occupancy-view'
ON CONFLICT DO NOTHING;

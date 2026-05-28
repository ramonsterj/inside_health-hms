-- ============================================================================
-- V115: Add resident_id FK to admissions
-- ============================================================================
-- Every admission must record the resident doctor who admitted the patient.
-- The service auto-binds resident_id from the authenticated user at create
-- time; only RESIDENT_DOCTOR users may create admissions.
--
-- The column is introduced nullable, backfilled from treating_physician_id
-- (which is always a valid user reference), then locked to NOT NULL. This
-- keeps the migration safe on any environment that already has admissions
-- (dev DBs carrying seed data, demo, prod retroactively reapplied).
--
-- See V114 for the RESIDENT_DOCTOR role.
-- ============================================================================

ALTER TABLE admissions
    ADD COLUMN resident_id BIGINT REFERENCES users(id);

UPDATE admissions
SET resident_id = treating_physician_id
WHERE resident_id IS NULL;

ALTER TABLE admissions
    ALTER COLUMN resident_id SET NOT NULL;

CREATE INDEX idx_admissions_resident_id ON admissions(resident_id);

-- ============================================================================
-- V130: Add discharge note to admissions
-- ============================================================================
-- Every user permitted to discharge (ADMINISTRADOR and MEDICO_RESIDENTE) must record
-- a mandatory free-text comment when releasing a patient. The note is captured at
-- discharge time (before the discharge-protection lock makes the record immutable).
--
--   * Nullable: existing discharged rows have no note.
--   * No index: the column is never queried or filtered.
--   * Mandatory is enforced at the service layer, not via a NOT NULL constraint, so
--     pre-existing rows are unaffected and a single localized error can be returned.
-- ============================================================================

ALTER TABLE admissions ADD COLUMN discharge_note TEXT;

-- Replace manually-entered `age` on patients with `date_of_birth`.
--
-- Rule (per `docs/features/new-patient-intake.md` v1.2):
--   Patient registration captures `date_of_birth` only; `age` is derived at read
--   time from `date_of_birth` (full years in America/Guatemala). The platform
--   never persists or accepts `age` as input.
--
-- Backfill strategy for existing rows:
--   We do not know each patient's true birth date, only their previously-entered
--   age. We seed `date_of_birth` to January 1 of `(current_year - age)` so that
--   the derived age matches the previously-entered age for the rest of the year
--   (and is off by at most one year afterward). Staff must correct these
--   placeholder dates as patients return; the spec calls this an explicit
--   temporary fallback.

ALTER TABLE patients ADD COLUMN date_of_birth DATE;

UPDATE patients
SET date_of_birth = make_date(EXTRACT(YEAR FROM CURRENT_DATE)::int - age, 1, 1)
WHERE date_of_birth IS NULL;

ALTER TABLE patients ALTER COLUMN date_of_birth SET NOT NULL;

ALTER TABLE patients
    ADD CONSTRAINT patients_date_of_birth_check
    CHECK (
        date_of_birth <= CURRENT_DATE
        AND date_of_birth > CURRENT_DATE - INTERVAL '151 years'
    );

ALTER TABLE patients DROP COLUMN age;

CREATE INDEX idx_patients_date_of_birth ON patients(date_of_birth);

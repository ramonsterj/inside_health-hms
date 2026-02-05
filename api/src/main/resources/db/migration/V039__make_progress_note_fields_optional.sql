-- Make progress note SOAP fields optional
-- V036 initially created these as NOT NULL following strict SOAP requirements.
-- After UX review, optional fields better support partial documentation workflows.
-- Note: Fields were made optional in a separate migration to preserve history.

ALTER TABLE progress_notes ALTER COLUMN subjective_data DROP NOT NULL;
ALTER TABLE progress_notes ALTER COLUMN objective_data DROP NOT NULL;
ALTER TABLE progress_notes ALTER COLUMN analysis DROP NOT NULL;
ALTER TABLE progress_notes ALTER COLUMN action_plans DROP NOT NULL;

-- Add optional glucometría (blood glucose) column to vital_signs.
-- Optional because glucose is only measured for specific patients (diabetics, ICU, etc.),
-- not at every vitals reading. Existing rows remain valid (NULL).
ALTER TABLE vital_signs
    ADD COLUMN glucose INTEGER;

ALTER TABLE vital_signs
    ADD CONSTRAINT chk_vital_signs_glucose
    CHECK (glucose IS NULL OR (glucose BETWEEN 20 AND 600));

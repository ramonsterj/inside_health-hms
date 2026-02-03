-- Make triage_code_id nullable for admission types that don't require triage
-- (AMBULATORY, ELECTROSHOCK_THERAPY, KETAMINE_INFUSION)
ALTER TABLE admissions ALTER COLUMN triage_code_id DROP NOT NULL;

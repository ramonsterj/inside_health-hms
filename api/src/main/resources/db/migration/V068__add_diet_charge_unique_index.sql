-- Unique partial index for daily diet charges (same pattern as room charges)
CREATE UNIQUE INDEX idx_patient_charges_daily_diet_unique
    ON patient_charges (admission_id, charge_type, charge_date)
    WHERE charge_type = 'DIET'
    AND deleted_at IS NULL;

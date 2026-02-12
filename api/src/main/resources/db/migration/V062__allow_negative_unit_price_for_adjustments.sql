ALTER TABLE patient_charges DROP CONSTRAINT IF EXISTS patient_charges_unit_price_check;
ALTER TABLE patient_charges ADD CONSTRAINT patient_charges_unit_price_check
    CHECK (unit_price >= 0 OR charge_type = 'ADJUSTMENT');

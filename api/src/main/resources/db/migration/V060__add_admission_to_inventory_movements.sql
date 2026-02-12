-- Add admission reference to inventory movements for billing context
ALTER TABLE inventory_movements
    ADD COLUMN admission_id BIGINT REFERENCES admissions(id);

CREATE INDEX idx_inventory_movements_admission_id ON inventory_movements(admission_id);

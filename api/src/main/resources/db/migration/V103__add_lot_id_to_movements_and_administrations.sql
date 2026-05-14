-- V103: Pharmacy & Inventory Evolution — Phase 1d
-- Adds nullable lot_id FKs to inventory_movements and medication_administrations.
-- Required (service-layer check) when the parent item is lot_tracking_enabled.

ALTER TABLE inventory_movements
    ADD COLUMN lot_id BIGINT NULL REFERENCES inventory_lots(id);

CREATE INDEX idx_inventory_movements_lot_id ON inventory_movements(lot_id);

ALTER TABLE medication_administrations
    ADD COLUMN lot_id BIGINT NULL REFERENCES inventory_lots(id);

CREATE INDEX idx_medication_administrations_lot_id ON medication_administrations(lot_id);

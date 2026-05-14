-- V107: Pharmacy & Inventory Evolution — persist dispense quantity on
-- medication_administrations so history endpoints can return the actual amount
-- dispensed instead of defaulting to 1.
--
-- AC-26: "Administering a medication with quantity = 3 produces exactly one
-- PatientCharge with quantity = 3 ... and writes one inventory_movements row
-- with quantity = 3." The administration row must carry the same quantity so
-- nurse/pharmacy history is accurate after the request returns.

ALTER TABLE medication_administrations
    ADD COLUMN quantity INT NOT NULL DEFAULT 1 CHECK (quantity >= 1);

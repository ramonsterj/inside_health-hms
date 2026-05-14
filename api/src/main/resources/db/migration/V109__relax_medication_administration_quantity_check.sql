-- V109: Pharmacy & Inventory Evolution — relax medication_administrations.quantity
-- CHECK constraint from `>= 1` to `>= 0`.
--
-- Non-GIVEN statuses (MISSED, REFUSED, HELD) record zero dispensed units. The
-- service layer writes `quantity = 0` in those branches, which collided with
-- the original `CHECK (quantity >= 1)` from V107 and made every non-GIVEN
-- administration fail with a constraint violation.

ALTER TABLE medication_administrations
    DROP CONSTRAINT IF EXISTS medication_administrations_quantity_check;

ALTER TABLE medication_administrations
    ADD CONSTRAINT medication_administrations_quantity_check
    CHECK (quantity >= 0);

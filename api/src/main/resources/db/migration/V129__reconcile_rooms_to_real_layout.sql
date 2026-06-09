-- ============================================================================
-- V129: Reconcile rooms to the real hospital layout
-- ============================================================================
-- The V056 seed rooms did not match the actual hospital. The customer confirmed
-- the real layout (a male/female identifier, not literal floors):
--
--   101-102 (compartida)  103-104 (compartida)  105 106 108 109 110 (privadas)
--   201 202 203 204 205 (privadas)  301 302 303 (privadas)  304-305 (compartida)
--
-- Rules:
--   * SHARED ("compartida") = one room with capacity 2, numbered as the pair.
--   * PRIVATE ("privada")    = one room with capacity 1.
--   * Gender heuristic kept: 1xx -> FEMALE, 2xx/3xx -> MALE.
--   * Pricing unchanged: SHARED = Q950/night, PRIVATE = Q1100/night.
--
-- There is no separate bed table — capacity IS the bed count — so this is a
-- pure data reconciliation. The canonical end state is 16 rooms / 19 beds.
--
-- Diff vs. V056: merge 101+102 -> 101-102, 103+104 -> 103-104; rename 304 ->
-- 304-305; convert 105/106/108/303 SHARED -> PRIVATE; drop 107; add 205.
--
-- This migration reconciles IN PLACE, preserving occupancy/charges where the
-- room survives. Two nullable FKs reference rooms(id) and are repointed BEFORE
-- any room row is deleted: admissions.room_id and patient_charges.room_id.
-- Per the V127/V128 convention, every statement is guarded by the old value so
-- re-application (or a Flyway repair) is a safe no-op, and fresh and already-
-- migrated databases converge to the same end state.
-- ============================================================================

-- ---- 1. Merge 102 into 101, then promote 101 -> 101-102 --------------------
-- Repoint FKs while the survivor still carries its old number.
UPDATE admissions SET room_id = (SELECT id FROM rooms WHERE number = '101')
  WHERE room_id = (SELECT id FROM rooms WHERE number = '102');
-- If a patient was charged for both old rooms on the same day, repointing both
-- rows would violate idx_patient_charges_daily_unique. Keep the duplicate charge
-- row, but detach it from the deleted room before the general FK repoint.
UPDATE patient_charges pc SET room_id = NULL
  WHERE pc.room_id = (SELECT id FROM rooms WHERE number = '102')
    AND pc.charge_type = 'ROOM'
    AND pc.deleted_at IS NULL
    AND EXISTS (
      SELECT 1
      FROM patient_charges existing
      WHERE existing.admission_id = pc.admission_id
        AND existing.charge_type = 'ROOM'
        AND existing.charge_date = pc.charge_date
        AND existing.room_id = (SELECT id FROM rooms WHERE number = '101')
        AND existing.deleted_at IS NULL
    );
UPDATE patient_charges SET room_id = (SELECT id FROM rooms WHERE number = '101')
  WHERE room_id = (SELECT id FROM rooms WHERE number = '102');
DELETE FROM rooms WHERE number = '102';
UPDATE rooms SET number = '101-102', updated_at = CURRENT_TIMESTAMP WHERE number = '101';

-- ---- 2. Merge 104 into 103, then promote 103 -> 103-104 --------------------
UPDATE admissions SET room_id = (SELECT id FROM rooms WHERE number = '103')
  WHERE room_id = (SELECT id FROM rooms WHERE number = '104');
UPDATE patient_charges pc SET room_id = NULL
  WHERE pc.room_id = (SELECT id FROM rooms WHERE number = '104')
    AND pc.charge_type = 'ROOM'
    AND pc.deleted_at IS NULL
    AND EXISTS (
      SELECT 1
      FROM patient_charges existing
      WHERE existing.admission_id = pc.admission_id
        AND existing.charge_type = 'ROOM'
        AND existing.charge_date = pc.charge_date
        AND existing.room_id = (SELECT id FROM rooms WHERE number = '103')
        AND existing.deleted_at IS NULL
    );
UPDATE patient_charges SET room_id = (SELECT id FROM rooms WHERE number = '103')
  WHERE room_id = (SELECT id FROM rooms WHERE number = '104');
DELETE FROM rooms WHERE number = '104';
UPDATE rooms SET number = '103-104', updated_at = CURRENT_TIMESTAMP WHERE number = '103';

-- ---- 3. Rename 304 -> 304-305 (already SHARED / capacity 2 / Q950) ---------
UPDATE rooms SET number = '304-305', updated_at = CURRENT_TIMESTAMP WHERE number = '304';

-- ---- 4. Drop 107 (null its FKs first) -------------------------------------
UPDATE admissions SET room_id = NULL
  WHERE room_id = (SELECT id FROM rooms WHERE number = '107');
UPDATE patient_charges SET room_id = NULL
  WHERE room_id = (SELECT id FROM rooms WHERE number = '107');
DELETE FROM rooms WHERE number = '107';

-- ---- 5. Convert former shared rooms to private singles ---------------------
UPDATE rooms SET type = 'PRIVATE', capacity = 1, price = 1100.00, updated_at = CURRENT_TIMESTAMP
  WHERE number IN ('105', '106', '108', '303');

-- ---- 6. Add 205 ------------------------------------------------------------
INSERT INTO rooms (number, gender, type, capacity, price, created_at, updated_at)
VALUES ('205', 'MALE', 'PRIVATE', 1, 1100.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (number) DO NOTHING;

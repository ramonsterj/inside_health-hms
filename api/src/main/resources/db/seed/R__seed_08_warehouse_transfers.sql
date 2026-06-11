-- ============================================================================
-- SEED FILE 08: Warehouse transfers (dev/acceptance convenience)
-- ============================================================================
-- Pre-transfers 10 units of every synthetic DRUG lot from ADMINISTRACION to
-- ENFERMERIA so QA can exercise the nurse-dispense flow (which debits ENFERMERIA
-- by default) without first issuing a manual transfer. Mirrors the live
-- WarehouseTransferService shape: one inventory_transfers row + two
-- inventory_movements (EXIT on source, ENTRY on destination) per lot, with the
-- per-warehouse stock rows kept consistent.
-- SEED-BUNDLE-VERSION: 2026-06-10-discharge-admin-resident-only (see R__seed_01 header for the rule)
-- ============================================================================

SET session_replication_role = replica;

-- One COMPLETED transfer per synthetic DRUG lot, issued by the admin user.
INSERT INTO inventory_transfers (
    source_warehouse_id, destination_warehouse_id, item_id, lot_id, quantity,
    status, notes, issued_at, issued_by, completed_at, completed_by,
    created_at, updated_at
)
SELECT (SELECT id FROM warehouses WHERE code = 'ADMINISTRACION'),
       (SELECT id FROM warehouses WHERE code = 'ENFERMERIA'),
       l.item_id, l.id, 10,
       'COMPLETED', 'Dev seed — initial nursing stock',
       CURRENT_TIMESTAMP,
       (SELECT id FROM users WHERE username = 'admin'),
       CURRENT_TIMESTAMP,
       (SELECT id FROM users WHERE username = 'admin'),
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM inventory_lots l
JOIN inventory_items i ON i.id = l.item_id
WHERE i.kind = 'DRUG' AND l.synthetic_legacy = TRUE AND l.deleted_at IS NULL
  -- Only transfer if ADMINISTRACION actually holds >= 10 for this lot.
  AND EXISTS (
      SELECT 1 FROM inventory_warehouse_stock s
      WHERE s.item_id = l.item_id AND s.lot_id = l.id AND s.deleted_at IS NULL
        AND s.warehouse_id = (SELECT id FROM warehouses WHERE code = 'ADMINISTRACION')
        AND s.quantity >= 10
  )
  -- Idempotent: skip lots already transferred by a prior run.
  AND NOT EXISTS (
      SELECT 1 FROM inventory_transfers t
      WHERE t.lot_id = l.id AND t.notes = 'Dev seed — initial nursing stock' AND t.deleted_at IS NULL
  );

-- Decrement ADMINISTRACION by the transferred amount.
UPDATE inventory_warehouse_stock s
SET quantity = s.quantity - t.quantity, updated_at = CURRENT_TIMESTAMP
FROM inventory_transfers t
WHERE t.notes = 'Dev seed — initial nursing stock' AND t.deleted_at IS NULL
  AND s.item_id = t.item_id AND s.lot_id = t.lot_id
  AND s.warehouse_id = t.source_warehouse_id AND s.deleted_at IS NULL
  AND s.quantity >= t.quantity;

-- Credit ENFERMERIA (insert or add).
INSERT INTO inventory_warehouse_stock (item_id, warehouse_id, lot_id, quantity, created_at, updated_at)
SELECT t.item_id, t.destination_warehouse_id, t.lot_id, t.quantity, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM inventory_transfers t
WHERE t.notes = 'Dev seed — initial nursing stock' AND t.deleted_at IS NULL
ON CONFLICT (item_id, warehouse_id, COALESCE(lot_id, -1)) WHERE deleted_at IS NULL DO UPDATE
   SET quantity = inventory_warehouse_stock.quantity + EXCLUDED.quantity,
       updated_at = CURRENT_TIMESTAMP;

-- Two historical movements per transfer (EXIT on source, ENTRY on destination).
INSERT INTO inventory_movements (
    item_id, lot_id, warehouse_id, transfer_id, movement_type, quantity,
    previous_quantity, new_quantity, notes, created_at, updated_at
)
SELECT t.item_id, t.lot_id, t.source_warehouse_id, t.id, 'EXIT', t.quantity,
       50, 40, 'Transfer out (ENFERMERIA)', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM inventory_transfers t
WHERE t.notes = 'Dev seed — initial nursing stock' AND t.deleted_at IS NULL
UNION ALL
SELECT t.item_id, t.lot_id, t.destination_warehouse_id, t.id, 'ENTRY', t.quantity,
       0, 10, 'Transfer in (ADMINISTRACION)', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM inventory_transfers t
WHERE t.notes = 'Dev seed — initial nursing stock' AND t.deleted_at IS NULL;

SET session_replication_role = DEFAULT;

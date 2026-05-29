-- ============================================================================
-- V120: Backfill inventory_warehouse_stock from the legacy global counts.
-- ============================================================================
-- Every legacy stock count lands in the ADMINISTRACION (master/receiving)
-- warehouse. Idempotent: the partial unique index uq_iws_item_wh_lot catches
-- re-runs via ON CONFLICT DO NOTHING. Production V111 catalog has quantity=0 so
-- this is a no-op there; dev seed data round-trips.
--
-- Run order matters: lot-tracked items carry their stock per lot (one row per
-- (item, ADMINISTRACION, lot)); non-lot-tracked items carry a single
-- (item, ADMINISTRACION, NULL) row.
-- ============================================================================

-- Non-lot-tracked items: copy inventory_items.quantity into a lot_id=NULL row.
INSERT INTO inventory_warehouse_stock (item_id, warehouse_id, lot_id, quantity, created_at, updated_at)
SELECT i.id,
       (SELECT id FROM warehouses WHERE code = 'ADMINISTRACION'),
       NULL,
       i.quantity,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
FROM inventory_items i
WHERE i.quantity > 0
  AND i.lot_tracking_enabled = FALSE
  AND i.deleted_at IS NULL
ON CONFLICT (item_id, warehouse_id, COALESCE(lot_id, -1)) WHERE deleted_at IS NULL DO NOTHING;

-- Lot-tracked items: copy each lot's quantity_on_hand into a per-lot row.
INSERT INTO inventory_warehouse_stock (item_id, warehouse_id, lot_id, quantity, created_at, updated_at)
SELECT l.item_id,
       (SELECT id FROM warehouses WHERE code = 'ADMINISTRACION'),
       l.id,
       l.quantity_on_hand,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
FROM inventory_lots l
WHERE l.quantity_on_hand > 0
  AND l.deleted_at IS NULL
ON CONFLICT (item_id, warehouse_id, COALESCE(lot_id, -1)) WHERE deleted_at IS NULL DO NOTHING;

-- ============================================================================
-- V120: Drop the legacy global stock columns.
-- ============================================================================
-- Final, irreversible step of the warehouse cutover. Stock now lives entirely in
-- inventory_warehouse_stock; all read/write paths route through it (see
-- InventoryWarehouseStockRepository + WarehouseScopeService). The repository
-- methods that updated/recomputed these columns (updateQuantityAtomically,
-- recomputeQuantityFromLots, findCurrentQuantity) were removed in the same PR.
--
-- inventory_items.quantity and inventory_lots.quantity_on_hand have no remaining
-- readers after the service refactor, so dropping them is safe.
-- ============================================================================

ALTER TABLE inventory_items DROP COLUMN quantity;
ALTER TABLE inventory_lots  DROP COLUMN quantity_on_hand;

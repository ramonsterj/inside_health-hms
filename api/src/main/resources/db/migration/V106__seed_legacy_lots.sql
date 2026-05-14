-- V106: Synthetic legacy lots for every lot-tracked item with quantity > 0.
-- Provides FEFO with a non-empty pool until the pharmacist registers real lots.
-- The 9999-12-31 sentinel + synthetic_legacy=TRUE flags are picked up by the
-- expiry dashboard (NO_EXPIRY) and the bulk importer's replacement rule.

INSERT INTO inventory_lots (
    item_id, lot_number, expiration_date, quantity_on_hand, received_at,
    supplier, notes, recalled, synthetic_legacy, created_at, updated_at
)
SELECT id,
       NULL,
       DATE '9999-12-31',
       quantity,
       CURRENT_DATE,
       NULL,
       'Legacy backfill — replace with real lots when restocking',
       FALSE,
       TRUE,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
FROM inventory_items
WHERE lot_tracking_enabled = TRUE
  AND quantity > 0
  AND deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM inventory_lots l WHERE l.item_id = inventory_items.id AND l.deleted_at IS NULL
  );

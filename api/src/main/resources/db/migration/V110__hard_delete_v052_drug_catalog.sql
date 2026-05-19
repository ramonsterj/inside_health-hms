-- V110: Hard-delete the V052-seeded legacy drug catalog before the workbook
-- loader (V111) installs the customer's canonical SKU-bearing catalog.
--
-- Any pre-pharmacy clinical data (orders, administrations, stock movements)
-- may still reference these rows. We sever those links in-migration so the
-- workbook becomes the single source of truth without losing clinical history:
--
--   * medical_orders.inventory_item_id → NULL (column is nullable; the order's
--     free-text description is preserved, the catalog link is dropped).
--   * patient_charges.inventory_item_id → NULL (column is nullable; immutable
--     charge description, quantity, price, and invoice link are preserved).
--   * inventory_movements rows → deleted (FK is NOT NULL; stock history for
--     superseded legacy drugs is intentionally discarded — the workbook catalog
--     is the new accounting baseline).
--   * medication_administrations are unaffected: they reference medical_orders,
--     not inventory_items directly. Their order's drug link becomes NULL and
--     any synthetic-legacy lot link is cleared before those lots are deleted.

-- 1. Drop nullable FKs from historical clinical/billing rows to the legacy
--    drug catalog. Descriptions and amounts remain on the owning rows.
UPDATE patient_charges
   SET inventory_item_id = NULL
 WHERE inventory_item_id IN (
       SELECT id FROM inventory_items
        WHERE kind = 'DRUG' AND sku IS NULL
       );

UPDATE medical_orders
   SET inventory_item_id = NULL
 WHERE inventory_item_id IN (
       SELECT id FROM inventory_items
        WHERE kind = 'DRUG' AND sku IS NULL
       );

-- 2. Delete inventory_movements tied to the legacy drug catalog (NOT NULL FK).
DELETE FROM inventory_movements
 WHERE item_id IN (
       SELECT id FROM inventory_items
        WHERE kind = 'DRUG' AND sku IS NULL
       );

-- 3. Clear medication administration lot links before deleting the legacy
--    synthetic lots. The administration and medical order rows are preserved.
UPDATE medication_administrations
   SET lot_id = NULL
 WHERE lot_id IN (
       SELECT l.id
         FROM inventory_lots l
         JOIN inventory_items i ON i.id = l.item_id
        WHERE i.kind = 'DRUG' AND i.sku IS NULL
       );

-- 4. Drop the satellites attached to the legacy drug catalog.
DELETE FROM inventory_lots
 WHERE item_id IN (
       SELECT id FROM inventory_items
        WHERE kind = 'DRUG' AND sku IS NULL
       );

DELETE FROM medication_details
 WHERE item_id IN (
       SELECT id FROM inventory_items
        WHERE kind = 'DRUG' AND sku IS NULL
       );

-- 5. Hard-delete the legacy drug catalog itself. Post-condition: every
--    remaining DRUG row carries a workbook SKU (enforced once V111 runs).
DELETE FROM inventory_items
 WHERE kind = 'DRUG' AND sku IS NULL;

-- 6. Sanity assertion — by construction nothing should reference V052 drugs
--    now. If something slipped through (a table not covered above), fail loud
--    so the gap is caught at migrate time, not at runtime.
DO $$
DECLARE
    leftover_items BIGINT;
BEGIN
    SELECT COUNT(*) INTO leftover_items
      FROM inventory_items
     WHERE kind = 'DRUG' AND sku IS NULL;

    IF leftover_items > 0 THEN
        RAISE EXCEPTION
          'V110 invariant violated: % legacy V052 drug rows still exist after cleanup',
          leftover_items;
    END IF;
END $$;

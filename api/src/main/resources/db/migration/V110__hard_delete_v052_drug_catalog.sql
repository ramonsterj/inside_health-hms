-- V110: Hard-delete the V052-seeded legacy drug catalog before the workbook
-- loader (V111) installs the customer's canonical SKU-bearing catalog.
--
-- Safe to hard-delete: the system is not yet in production. Abort loudly if
-- any clinical record references one of these legacy rows.

DO $$
DECLARE
    referenced_count BIGINT;
BEGIN
    SELECT COUNT(*) INTO referenced_count
    FROM (
        SELECT 1 FROM inventory_movements im
        JOIN inventory_items i ON i.id = im.item_id
        WHERE i.kind = 'DRUG' AND i.sku IS NULL
        UNION ALL
        SELECT 1 FROM medication_administrations ma
        JOIN medical_orders mo ON mo.id = ma.medical_order_id
        JOIN inventory_items i ON i.id = mo.inventory_item_id
        WHERE i.kind = 'DRUG' AND i.sku IS NULL
        UNION ALL
        SELECT 1 FROM medical_orders mo
        JOIN inventory_items i ON i.id = mo.inventory_item_id
        WHERE i.kind = 'DRUG' AND i.sku IS NULL
    ) AS refs;

    IF referenced_count > 0 THEN
        RAISE EXCEPTION
          'V110 aborted: % clinical references still point at legacy V052 drugs (kind=DRUG, sku IS NULL). Production data detected — manual review required.',
          referenced_count;
    END IF;
END $$;

DELETE FROM inventory_lots
WHERE item_id IN (
    SELECT id FROM inventory_items WHERE kind = 'DRUG' AND sku IS NULL
);

DELETE FROM medication_details
WHERE item_id IN (
    SELECT id FROM inventory_items WHERE kind = 'DRUG' AND sku IS NULL
);

DELETE FROM inventory_items
WHERE kind = 'DRUG' AND sku IS NULL;

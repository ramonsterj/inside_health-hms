-- V100: Pharmacy & Inventory Evolution — Phase 1a
-- Adds the composition discriminator (`kind`), the customer-printed SKU, and
-- the `lot_tracking_enabled` flag to inventory_items. Existing rows are
-- backfilled by category so no FK churn is required.

ALTER TABLE inventory_items
    ADD COLUMN kind VARCHAR(20) NOT NULL DEFAULT 'SERVICE';

ALTER TABLE inventory_items
    ADD COLUMN sku VARCHAR(20) NULL;

ALTER TABLE inventory_items
    ADD COLUMN lot_tracking_enabled BOOLEAN NOT NULL DEFAULT FALSE;

-- Backfill kind from inventory_categories.name per FR-1 mapping.
-- Medicamentos -> DRUG
UPDATE inventory_items i
SET kind = 'DRUG'
FROM inventory_categories c
WHERE i.category_id = c.id
  AND LOWER(c.name) LIKE '%medicament%';

-- Material y Equipo + FLAT pricing -> SUPPLY ; TIME_BASED -> EQUIPMENT
UPDATE inventory_items i
SET kind = CASE
        WHEN i.pricing_type = 'TIME_BASED' THEN 'EQUIPMENT'
        ELSE 'SUPPLY'
    END
FROM inventory_categories c
WHERE i.category_id = c.id
  AND (LOWER(c.name) LIKE '%material%' OR LOWER(c.name) LIKE '%equipo%');

-- Laboratorios / Servicios -> SERVICE
UPDATE inventory_items i
SET kind = 'SERVICE'
FROM inventory_categories c
WHERE i.category_id = c.id
  AND (LOWER(c.name) LIKE '%laborator%' OR LOWER(c.name) LIKE '%servicio%');

-- Personal Especial -> PERSONNEL
UPDATE inventory_items i
SET kind = 'PERSONNEL'
FROM inventory_categories c
WHERE i.category_id = c.id
  AND LOWER(c.name) LIKE '%personal%';

-- Ingredientes de Cocina / Alimentacion -> FOOD
UPDATE inventory_items i
SET kind = 'FOOD'
FROM inventory_categories c
WHERE i.category_id = c.id
  AND (LOWER(c.name) LIKE '%cocina%'
       OR LOWER(c.name) LIKE '%aliment%'
       OR LOWER(c.name) LIKE '%dieta%');

-- DRUG items are always lot-tracked.
UPDATE inventory_items
SET lot_tracking_enabled = TRUE
WHERE kind = 'DRUG';

ALTER TABLE inventory_items ALTER COLUMN kind DROP DEFAULT;

CREATE INDEX idx_inventory_items_kind ON inventory_items(kind);

CREATE UNIQUE INDEX ux_inventory_items_sku
    ON inventory_items(sku)
    WHERE sku IS NOT NULL AND deleted_at IS NULL;

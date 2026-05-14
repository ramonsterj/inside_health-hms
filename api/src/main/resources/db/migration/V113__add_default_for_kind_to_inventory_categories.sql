-- V113: Add `default_for_kind` to inventory_categories so the backend can
-- resolve the routing category for a given InventoryKind (currently only
-- DRUG) without depending on the user-facing category name. See
-- pharmacy-and-inventory-evolution.md FR-9.
--
-- Removes a UX footgun: the medication create/edit dialog used to expose
-- every active inventory category in a dropdown, letting pharmacists file
-- a drug under "Ingredientes de Cocina" or "Servicios". With this column
-- the backend assigns the category itself and the dialog drops the select.

ALTER TABLE inventory_categories
    ADD COLUMN default_for_kind VARCHAR(20) NULL;

-- At most one ACTIVE non-deleted category may be the default for any
-- given kind. NULL rows are unconstrained (the bulk of the table).
CREATE UNIQUE INDEX ux_inventory_categories_default_for_kind
    ON inventory_categories(default_for_kind)
    WHERE default_for_kind IS NOT NULL
      AND deleted_at IS NULL
      AND active = TRUE;

-- Backfill the seeded "Medicamentos" category as the DRUG default. Match
-- by substring so localized variants ("Medicamentos", "Medicamento", etc.)
-- still resolve. We only flip rows that are active and not soft-deleted
-- so the partial unique index above accepts the update.
UPDATE inventory_categories
SET default_for_kind = 'DRUG'
WHERE LOWER(name) LIKE '%medicament%'
  AND active = TRUE
  AND deleted_at IS NULL;

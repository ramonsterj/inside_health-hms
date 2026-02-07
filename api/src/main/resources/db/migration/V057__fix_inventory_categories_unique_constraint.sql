-- Replace column-level UNIQUE with partial unique index to support soft deletes.
-- The column-level UNIQUE prevents re-creating a category with the same name as a
-- soft-deleted one, causing an opaque 500 error instead of a proper validation message.
ALTER TABLE inventory_categories DROP CONSTRAINT inventory_categories_name_key;
CREATE UNIQUE INDEX idx_inventory_categories_name_unique ON inventory_categories(name) WHERE deleted_at IS NULL;

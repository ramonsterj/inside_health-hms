-- V102: Pharmacy & Inventory Evolution — Phase 1c
-- Creates the InventoryLot satellite (1:N for items where lot_tracking_enabled = TRUE).
-- See pharmacy-and-inventory-evolution.md FR-3.

CREATE TABLE inventory_lots (
    id BIGSERIAL PRIMARY KEY,
    item_id BIGINT NOT NULL REFERENCES inventory_items(id),
    lot_number VARCHAR(50) NULL,
    expiration_date DATE NOT NULL,
    quantity_on_hand INT NOT NULL CHECK (quantity_on_hand >= 0),
    received_at DATE NOT NULL,
    supplier VARCHAR(150) NULL,
    notes VARCHAR(500) NULL,
    recalled BOOLEAN NOT NULL DEFAULT FALSE,
    recalled_reason VARCHAR(500) NULL,
    synthetic_legacy BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_inventory_lots_item_id ON inventory_lots(item_id);
CREATE INDEX idx_inventory_lots_expiration_date ON inventory_lots(expiration_date);
CREATE INDEX idx_inventory_lots_deleted_at ON inventory_lots(deleted_at);

-- Partial FEFO index: covers the hot query path for lot selection.
CREATE INDEX idx_inventory_lots_item_fefo
    ON inventory_lots(item_id, expiration_date)
    WHERE deleted_at IS NULL AND recalled = FALSE AND quantity_on_hand > 0;

-- Unique identity within an item using NULLS NOT DISTINCT so duplicate
-- imports cannot create duplicate lots when lot_number is NULL.
CREATE UNIQUE INDEX ux_inventory_lots_item_lot_expiration_active
    ON inventory_lots(item_id, lot_number, expiration_date) NULLS NOT DISTINCT
    WHERE deleted_at IS NULL;

-- Partial index used by the quantity-recompute SUM.
CREATE INDEX idx_inventory_lots_item_active
    ON inventory_lots(item_id)
    WHERE deleted_at IS NULL AND recalled = FALSE;

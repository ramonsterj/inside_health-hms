CREATE TABLE inventory_movements (
    id BIGSERIAL PRIMARY KEY,
    item_id BIGINT NOT NULL REFERENCES inventory_items(id),
    movement_type VARCHAR(20) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    previous_quantity INT NOT NULL,
    new_quantity INT NOT NULL,
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_inventory_movements_deleted_at ON inventory_movements(deleted_at);
CREATE INDEX idx_inventory_movements_item_id ON inventory_movements(item_id);
CREATE INDEX idx_inventory_movements_movement_type ON inventory_movements(movement_type);
CREATE INDEX idx_inventory_movements_created_at ON inventory_movements(created_at);

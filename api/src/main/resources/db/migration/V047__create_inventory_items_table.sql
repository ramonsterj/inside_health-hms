CREATE TABLE inventory_items (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL REFERENCES inventory_categories(id),
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    price DECIMAL(12,2) NOT NULL DEFAULT 0 CHECK (price >= 0),
    cost DECIMAL(12,2) NOT NULL DEFAULT 0 CHECK (cost >= 0),
    quantity INT NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    restock_level INT NOT NULL DEFAULT 0 CHECK (restock_level >= 0),
    pricing_type VARCHAR(20) NOT NULL DEFAULT 'FLAT',
    time_unit VARCHAR(20),
    time_interval INT CHECK (time_interval > 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_inventory_items_deleted_at ON inventory_items(deleted_at);
CREATE INDEX idx_inventory_items_category_id ON inventory_items(category_id);
CREATE INDEX idx_inventory_items_active ON inventory_items(active);
CREATE INDEX idx_inventory_items_pricing_type ON inventory_items(pricing_type);
CREATE INDEX idx_inventory_items_quantity_restock ON inventory_items(quantity, restock_level);
CREATE INDEX idx_inventory_items_name ON inventory_items(LOWER(name));

ALTER TABLE medical_orders
    ADD COLUMN inventory_item_id BIGINT REFERENCES inventory_items(id);

CREATE INDEX idx_medical_orders_inventory_item_id ON medical_orders(inventory_item_id);

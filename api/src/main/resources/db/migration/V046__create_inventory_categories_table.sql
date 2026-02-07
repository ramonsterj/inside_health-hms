CREATE TABLE inventory_categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    display_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_inventory_categories_deleted_at ON inventory_categories(deleted_at);
CREATE INDEX idx_inventory_categories_display_order ON inventory_categories(display_order);
CREATE INDEX idx_inventory_categories_active ON inventory_categories(active);

-- Seed default categories
INSERT INTO inventory_categories (name, description, display_order, active, created_at, updated_at) VALUES
('Medicamentos', 'Medication and pharmaceutical supplies', 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Material y Equipo', 'Materials and equipment', 2, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Laboratorios', 'Laboratory services and supplies', 3, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Servicios', 'Hospital services', 4, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Personal Especial', 'Specialized personnel services', 5, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Ingredientes de Cocina', 'Kitchen ingredients', 6, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Alimentación', 'Food served to patients', 7, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

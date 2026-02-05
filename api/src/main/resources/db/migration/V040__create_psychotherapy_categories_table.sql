-- Create psychotherapy categories table (admin-managed lookup table)
CREATE TABLE psychotherapy_categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    display_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_psychotherapy_categories_deleted_at ON psychotherapy_categories(deleted_at);
CREATE INDEX idx_psychotherapy_categories_display_order ON psychotherapy_categories(display_order);
CREATE INDEX idx_psychotherapy_categories_active ON psychotherapy_categories(active);

-- Seed default categories
INSERT INTO psychotherapy_categories (name, description, display_order, active, created_at, updated_at) VALUES
('Taller', 'Workshop activities', 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Sesión individual', 'Private one-on-one sessions', 2, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Terapia con mascotas', 'Pet-assisted therapy', 3, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Pilates', 'Pilates exercise sessions', 4, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Meditación guiada', 'Guided meditation sessions', 5, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Terapia grupal', 'Group therapy sessions', 6, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Arte terapia', 'Art therapy activities', 7, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Musicoterapia', 'Music therapy sessions', 8, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Terapia ocupacional', 'Occupational therapy', 9, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Otra', 'Other activities', 99, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

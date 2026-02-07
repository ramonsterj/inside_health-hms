-- Seed inventory items for "Servicios" (id=4) and "Personal Especial" (id=5)
-- These are service items - no physical stock tracked

-- Servicios
INSERT INTO inventory_items (category_id, name, description, price, cost, quantity, restock_level, pricing_type, time_unit, time_interval, active, created_at, updated_at) VALUES
(4, 'ATENCION EMERGENCIA', 'ATENCION EMERGENCIA', 900.00, 0.00, 0, 0, 'FLAT', NULL, NULL, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'KETAMINA AMBULATORIA', 'PROCEDIMIENTO DE KETAMINA', 1500.00, 0.00, 0, 0, 'FLAT', NULL, NULL, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'KETAMINA INTERNA', 'PROCEDIMIENTO DE KETAMINA INTERNA', 1100.00, 0.00, 0, 0, 'FLAT', NULL, NULL, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Personal Especial
INSERT INTO inventory_items (category_id, name, description, price, cost, quantity, restock_level, pricing_type, time_unit, time_interval, active, created_at, updated_at) VALUES
(5, 'ENFERMERA SOMBRA HOSPITAL', 'ENFERMERA SOMBRA HOSPITAL', 500.00, 300.00, 0, 0, 'FLAT', NULL, NULL, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'ENFERMERA SOMBRA DOMICILIO', 'ENFERMERA SOMBRA DOMICILIO', 650.00, 350.00, 0, 0, 'FLAT', NULL, NULL, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

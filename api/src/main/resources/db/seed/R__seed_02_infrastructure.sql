-- ============================================================================
-- SEED FILE 02: Triage Codes, Rooms, Inventory
-- ============================================================================
-- Last updated: 2026-05-13 (Medicamentos + supply rows moved to R__seed_02b
-- so dev/acceptance loads the workbook catalog the same way prod does)
-- SEED-BUNDLE-VERSION: 2026-05-13a (see R__seed_01 header for the rule)

SET session_replication_role = replica;

-- ============================================================================
-- STEP 8: ENSURE TRIAGE CODES EXIST
-- ============================================================================
-- Triage codes are reference data seeded by V021. Use ON CONFLICT DO NOTHING
-- so this is idempotent whether the versioned migration already ran or not.
INSERT INTO triage_codes (code, color, description, display_order, created_at, updated_at) VALUES
('A', '#FF0000', 'Critical - Immediate attention required', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('B', '#FFA500', 'Urgent - Requires prompt attention', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('C', '#FFFF00', 'Less Urgent - Can wait for care', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('D', '#00FF00', 'Non-Urgent - Minor issues', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('E', '#0000FF', 'Referral - Scheduled admission', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- ============================================================================
-- STEP 9: ENSURE ROOMS EXIST
-- ============================================================================
-- Rooms are reference data seeded by V056. Use ON CONFLICT DO NOTHING
-- so this is idempotent whether the versioned migration already ran or not.
-- Room types: PRIVATE (1 bed), SHARED (multiple beds)
-- Room gender: MALE, FEMALE
INSERT INTO rooms (number, type, gender, capacity, price, created_at, updated_at) VALUES
-- First floor - Women's rooms
('101', 'SHARED', 'FEMALE', 2, 950.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('102', 'SHARED', 'FEMALE', 2, 950.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('103', 'SHARED', 'FEMALE', 2, 950.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('104', 'SHARED', 'FEMALE', 2, 950.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('105', 'SHARED', 'FEMALE', 2, 950.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('106', 'SHARED', 'FEMALE', 2, 950.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('107', 'SHARED', 'FEMALE', 2, 950.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('108', 'SHARED', 'FEMALE', 2, 950.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('109', 'PRIVATE', 'FEMALE', 1, 1100.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('110', 'PRIVATE', 'FEMALE', 1, 1100.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Second floor - Men's rooms
('201', 'PRIVATE', 'MALE', 1, 1100.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('202', 'PRIVATE', 'MALE', 1, 1100.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('203', 'PRIVATE', 'MALE', 1, 1100.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('204', 'PRIVATE', 'MALE', 1, 1100.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Third floor - Men's rooms
('301', 'PRIVATE', 'MALE', 1, 1100.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('302', 'PRIVATE', 'MALE', 1, 1100.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('303', 'SHARED', 'MALE', 2, 950.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('304', 'SHARED', 'MALE', 2, 950.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (number) DO NOTHING;

-- ============================================================================
-- STEP 10: RESEED INVENTORY CATEGORIES AND ITEMS
-- ============================================================================
-- Categories are wiped by TRUNCATE users CASCADE (via created_by/updated_by FKs)
-- so we must re-insert them along with all inventory items.

-- Medicamentos carries `default_for_kind = 'DRUG'` so the backend routes
-- medication creation here and the general inventory form hides it from
-- the create/edit category select (pharmacy-and-inventory-evolution.md FR-9).
-- V113 backfilled this once, but TRUNCATE above wipes the row and we must
-- re-set the flag on reseed.
INSERT INTO inventory_categories (name, description, display_order, active, default_for_kind, created_at, updated_at) VALUES
('Medicamentos', 'Medication and pharmaceutical supplies', 1, true, 'DRUG', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Material y Equipo', 'Materials and equipment', 2, true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Laboratorios', 'Laboratory services and supplies', 3, true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Servicios', 'Hospital services', 4, true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Personal Especial', 'Specialized personnel services', 5, true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Ingredientes de Cocina', 'Kitchen ingredients', 6, true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Alimentación', 'Food served to patients', 7, true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Medicamentos: workbook-sourced catalog loaded by
-- R__seed_02b_pharmacy_from_workbook.sql (mirrors V111). The legacy V052
-- drug rows that used to live here were dropped along with the bulk importer.

-- Material y Equipo: equipment rows only. Supply rows (syringes, sondas,
-- gloves, gasas, etc.) come from workbook section E via R__seed_02b.
-- Kind follows V100 backfill rule for this category: TIME_BASED -> EQUIPMENT, FLAT -> SUPPLY.
INSERT INTO inventory_items (category_id, name, description, price, cost, quantity, restock_level, pricing_type, time_unit, time_interval, active, kind, created_at, updated_at)
SELECT c.id, v.name, v.description, v.price, v.cost, v.quantity, v.restock_level, v.pricing_type, v.time_unit, v.time_interval, v.active,
       CASE WHEN v.pricing_type = 'TIME_BASED' THEN 'EQUIPMENT' ELSE 'SUPPLY' END,
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM inventory_categories c,
(VALUES
-- Time-based equipment usage
('MONITOR CARDIACO', 'USO MONITOR CARDIACO (1 HORA)', 250.00, 250.00, 0, 0, 'TIME_BASED', 'HOURS'::VARCHAR, 1::INT, true),
('BOMBA DE INFUSIÓN', 'USO DE BOMBA DE INFUSIÓN (1 HORA)', 250.00, 250.00, 0, 0, 'TIME_BASED', 'HOURS', 1, true),
('OXÍGENO', 'USO OXÍGENO (10 MINUTOS)', 117.00, 16.20, 0, 0, 'TIME_BASED', 'MINUTES', 10, true),
-- Flat-priced equipment and services
('EKG', 'EKG', 265.00, 265.00, 0, 0, 'FLAT', NULL, NULL, true),
('USO DE DESFIBRILADOR', 'USO DE DESFIBRILADOR', 3000.00, 0.00, 0, 0, 'FLAT', NULL, NULL, true),
('USO DE GLUCÓMETRO', 'USO DE GLUCÓMETRO', 49.98, 12.09, 50, 5, 'FLAT', NULL, NULL, true)
) AS v(name, description, price, cost, quantity, restock_level, pricing_type, time_unit, time_interval, active)
WHERE c.name = 'Material y Equipo';

-- Laboratorios
INSERT INTO inventory_items (category_id, name, description, price, cost, quantity, restock_level, pricing_type, time_unit, time_interval, active, kind, created_at, updated_at)
SELECT c.id, v.name, v.description, v.price, v.cost, v.quantity, v.restock_level, v.pricing_type, v.time_unit, v.time_interval, v.active, 'SERVICE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM inventory_categories c,
(VALUES
-- Chemistry panel
('ÁCIDO ÚRICO', 'SUERO', 150.00, 50.00, 0, 0, 'FLAT', NULL::VARCHAR, NULL::INT, true),
('ALBUMINA', 'SUERO', 120.00, 40.00, 0, 0, 'FLAT', NULL, NULL, true),
('AMILASA', 'SUERO', 225.00, 75.00, 0, 0, 'FLAT', NULL, NULL, true),
('BICARBONATO', 'SUERO', 900.00, 300.00, 0, 0, 'FLAT', NULL, NULL, true),
('BILIRUBINAS (TOTAL, DIRECTA E INDIRECTA)', 'SUERO', 240.00, 80.00, 0, 0, 'FLAT', NULL, NULL, true),
('CALCIO', 'SUERO', 225.00, 75.00, 0, 0, 'FLAT', NULL, NULL, true),
('CLORUROS', 'SUERO', 240.00, 80.00, 0, 0, 'FLAT', NULL, NULL, true),
('CREATININA', 'SUERO', 150.00, 50.00, 0, 0, 'FLAT', NULL, NULL, true),
('FOSFATASA ACIDA PROSTATICA', 'SUERO', 375.00, 125.00, 0, 0, 'FLAT', NULL, NULL, true),
('FOSFATASA ACIDA TOTAL', 'SUERO', 500.00, 125.00, 0, 0, 'FLAT', NULL, NULL, true),
('FOSFATASA ALCALINA', 'SUERO', 180.00, 60.00, 0, 0, 'FLAT', NULL, NULL, true),
('GAMA GLUTAMIL TRANSFERASA (GGT)', 'SUERO', 240.00, 80.00, 0, 0, 'FLAT', NULL, NULL, true),
('GLUCOSA', 'SUERO', 150.00, 50.00, 0, 0, 'FLAT', NULL, NULL, true),
('MAGNESIO', 'SUERO', 225.00, 75.00, 0, 0, 'FLAT', NULL, NULL, true),
('NITRÓGENO DE UREA', 'SUERO', 150.00, 50.00, 0, 0, 'FLAT', NULL, NULL, true),
('POTASIO', 'SUERO', 225.00, 75.00, 0, 0, 'FLAT', NULL, NULL, true),
('PROTEÍNAS TOTALES', 'SUERO', 150.00, 50.00, 0, 0, 'FLAT', NULL, NULL, true),
('SODIO', 'SUERO', 225.00, 75.00, 0, 0, 'FLAT', NULL, NULL, true),
-- Lipid panel
('COLESTEROL HDL', 'SUERO', 150.00, 50.00, 0, 0, 'FLAT', NULL, NULL, true),
('COLESTEROL LDL', 'SUERO', 150.00, 50.00, 0, 0, 'FLAT', NULL, NULL, true),
('COLESTEROL TOTAL', 'SUERO', 150.00, 50.00, 0, 0, 'FLAT', NULL, NULL, true),
('COLESTEROL VLDL', 'SUERO', 150.00, 50.00, 0, 0, 'FLAT', NULL, NULL, true),
('TRIGLICÉRIDOS', 'SUERO', 150.00, 50.00, 0, 0, 'FLAT', NULL, NULL, true),
('PERFIL DE LÍPIDOS', 'SUERO', 600.00, 200.00, 0, 0, 'FLAT', NULL, NULL, true),
-- Liver panel
('TGO', 'SUERO', 180.00, 60.00, 0, 0, 'FLAT', NULL, NULL, true),
('TGP', 'SUERO', 180.00, 60.00, 0, 0, 'FLAT', NULL, NULL, true),
('PRUEBAS HEPÁTICAS TGO-TGP-GGT', 'SUERO', 600.00, 200.00, 0, 0, 'FLAT', NULL, NULL, true),
('LIPASA', 'SUERO', 225.00, 75.00, 0, 0, 'FLAT', NULL, NULL, true),
('LIPASA TOTALES', 'SUERO', 180.00, 60.00, 0, 0, 'FLAT', NULL, NULL, true),
-- Cardiac markers
('CK-MB', 'SUERO', 300.00, 100.00, 0, 0, 'FLAT', NULL, NULL, true),
('CK-TOTAL', 'SUERO', 300.00, 100.00, 0, 0, 'FLAT', NULL, NULL, true),
('CK-MM', 'SUERO', 750.00, 250.00, 0, 0, 'FLAT', NULL, NULL, true),
-- Coagulation
('DÍMERO D', 'PLASMA CITRATADO', 1500.00, 500.00, 0, 0, 'FLAT', NULL, NULL, true),
('PROTEÍNA C', 'PLASMA CITRATADO', 1050.00, 350.00, 0, 0, 'FLAT', NULL, NULL, true),
('TIEMPO DE PROTROMBINA TP', 'PLASMA CITRATADO', 150.00, 50.00, 0, 0, 'FLAT', NULL, NULL, true),
('TIEMPO PARCIAL DE TROMBOPLASTINA TPT', 'PLASMA CITRATADO', 150.00, 50.00, 0, 0, 'FLAT', NULL, NULL, true),
-- Hormones - Thyroid
('T3 LIBRE', 'SUERO', 390.00, 130.00, 0, 0, 'FLAT', NULL, NULL, true),
('T3 TRIYODOTIRONINA', 'SUERO', 240.00, 80.00, 0, 0, 'FLAT', NULL, NULL, true),
('T3-T4, TSH', 'SUERO', 675.00, 225.00, 0, 0, 'FLAT', NULL, NULL, true),
('T4 LIBRE', 'SUERO', 390.00, 130.00, 0, 0, 'FLAT', NULL, NULL, true),
('HORMONA ESTIMULANTE DE TIROIDES (TSH)', 'SUERO', 375.00, 125.00, 0, 0, 'FLAT', NULL, NULL, true),
('HORMONA T3-T4-TSH', 'SUERO', 675.00, 125.00, 0, 0, 'FLAT', NULL, NULL, true),
-- Hormones - Other
('CORTISOL AM', 'SUERO', 450.00, 150.00, 0, 0, 'FLAT', NULL, NULL, true),
('CORTISOL PM', 'SUERO', 450.00, 150.00, 0, 0, 'FLAT', NULL, NULL, true),
('HORMONA DEL CRECIMIENTO', 'SUERO', 675.00, 225.00, 0, 0, 'FLAT', NULL, NULL, true),
('HORMONA PROGESTERONA (P4)', 'SUERO', 375.00, 125.00, 0, 0, 'FLAT', NULL, NULL, true),
('INSULINA', 'SUERO', 675.00, 225.00, 0, 0, 'FLAT', NULL, NULL, true),
('CURVA DE GLUCOSA 3 HRS', 'SUERO', 450.00, 150.00, 0, 0, 'FLAT', NULL, NULL, true),
('CURVA DE INSULINA 3 HRS', 'SUERO', 1200.00, 400.00, 0, 0, 'FLAT', NULL, NULL, true),
('TESTOSTERONA (TE)', 'SUERO', 540.00, 180.00, 0, 0, 'FLAT', NULL, NULL, true),
('TESTOSTERONA LIBRE', 'SUERO', 540.00, 180.00, 0, 0, 'FLAT', NULL, NULL, true),
-- Tumor markers
('CA-125', 'SUERO', 675.00, 225.00, 0, 0, 'FLAT', NULL, NULL, true),
('CA-15-3', 'SUERO', 675.00, 225.00, 0, 0, 'FLAT', NULL, NULL, true),
('CA-19-9', 'SUERO', 675.00, 225.00, 0, 0, 'FLAT', NULL, NULL, true),
-- Infectious disease
('HEPATITIS A IgG', 'SUERO', 525.00, 175.00, 0, 0, 'FLAT', NULL, NULL, true),
('HEPATITIS B', 'SUERO', 525.00, 175.00, 0, 0, 'FLAT', NULL, NULL, true),
('HEPATITIS C', 'SUERO', 450.00, 150.00, 0, 0, 'FLAT', NULL, NULL, true),
('CARGA VIRAL DE HCV', 'TUBO CON EDTA', 6000.00, 2000.00, 0, 0, 'FLAT', NULL, NULL, true),
('CARGA VIRAL DE HIV', 'TUBO CON EDTA', 4500.00, 1500.00, 0, 0, 'FLAT', NULL, NULL, true),
('CARGA VIRAL DE HBsAg', 'TUBO CON EDTA', 6000.00, 2000.00, 0, 0, 'FLAT', NULL, NULL, true),
('HIV 1 Y 2', 'SUERO', 375.00, 125.00, 0, 0, 'FLAT', NULL, NULL, true),
('TB COMPLEX', 'SUERO', 600.00, 200.00, 0, 0, 'FLAT', NULL, NULL, true),
('VDRL-RPR', 'SUERO', 150.00, 50.00, 0, 0, 'FLAT', NULL, NULL, true),
-- Hematology
('HEMATOLOGÍA COMPLETA', 'TUBO CON EDTA', 180.00, 60.00, 0, 0, 'FLAT', NULL, NULL, true),
('HEMOGLOBINA-HEMATOCRITO', 'SUERO', 150.00, 50.00, 0, 0, 'FLAT', NULL, NULL, true),
('HEMOGLOBINA GLICOSILADA', 'TUBO CON EDTA', 450.00, 150.00, 0, 0, 'FLAT', NULL, NULL, true),
('GRUPO SANGUÍNEO Y FACTOR RH', 'TUBO CON EDTA', 150.00, 50.00, 0, 0, 'FLAT', NULL, NULL, true),
('HIERRO CAPACIDAD DE FIJACION', 'SUERO', 450.00, 150.00, 0, 0, 'FLAT', NULL, NULL, true),
('HIERRO SÉRICO', 'SUERO', 450.00, 150.00, 0, 0, 'FLAT', NULL, NULL, true),
-- Iron and vitamins
('VITAMINA B-12', 'SUERO', 750.00, 250.00, 0, 0, 'FLAT', NULL, NULL, true),
('VITAMINA D', 'SUERO', 1050.00, 350.00, 0, 0, 'FLAT', NULL, NULL, true),
-- Inflammatory markers
('PROTEÍNA C REACTIVA', 'SUERO', 150.00, 50.00, 0, 0, 'FLAT', NULL, NULL, true),
('AMONIO', 'TUBO DE HEMATOLOGIA', 600.00, 200.00, 0, 0, 'FLAT', NULL, NULL, true),
-- Drug levels and toxicology
('CARBAMAZEPINA (TEGRETOL)', 'SUERO', 540.00, 180.00, 0, 0, 'FLAT', NULL, NULL, true),
('ALCOHOLEMIA', 'TUBO CON EDTA', 1050.00, 350.00, 0, 0, 'FLAT', NULL, NULL, true),
('BENZODIAZEPINAS', 'ORINA', 600.00, 200.00, 0, 0, 'FLAT', NULL, NULL, true),
('COCAINA', 'ORINA FRESCA', 600.00, 200.00, 0, 0, 'FLAT', NULL, NULL, true),
('MARIGUANA', 'SUERO', 525.00, 175.00, 0, 0, 'FLAT', NULL, NULL, true),
('PANEL DE DROGAS', 'ORINA FRESCA', 1650.00, 550.00, 0, 0, 'FLAT', NULL, NULL, true),
-- Cultures and urine/stool
('HEMOCULTIVO (CULTIVO EN SANGRE)', 'CALDO BHI', 450.00, 150.00, 0, 0, 'FLAT', NULL, NULL, true),
('ORINA COMPLETO', 'ORINA FRESCA', 150.00, 50.00, 0, 0, 'FLAT', NULL, NULL, true),
('OROCULTIVO', 'ISOPADO', 300.00, 100.00, 0, 0, 'FLAT', NULL, NULL, true),
('UROCULTIVO', 'ORINA FRESCA', 300.00, 100.00, 0, 0, 'FLAT', NULL, NULL, true),
('HECES COMPLETO', 'HECES FRESCAS', 150.00, 50.00, 0, 0, 'FLAT', NULL, NULL, true),
-- Pregnancy and special
('PRUEBA DE EMBARAZO', 'SUERO', 225.00, 75.00, 0, 0, 'FLAT', NULL, NULL, true),
-- Admission kit
('KIT DE INGRESO', 'KIT DE INGRESO (HEMATOLOGIA, GLUCOSA, CREATININA, NITRÓGENO DE UREA, PERFIL DE LÍPIDOS, TGO-TGP, SODIO, POTASIO, HORMONAS FT3, FT4 Y TSH, PANEL DE DROGAS EN ORINA, VITAMINA D)', 4950.00, 1650.00, 97, 10, 'FLAT', NULL, NULL, true)
) AS v(name, description, price, cost, quantity, restock_level, pricing_type, time_unit, time_interval, active)
WHERE c.name = 'Laboratorios';

-- Servicios
INSERT INTO inventory_items (category_id, name, description, price, cost, quantity, restock_level, pricing_type, time_unit, time_interval, active, kind, created_at, updated_at)
SELECT c.id, v.name, v.description, v.price, v.cost, v.quantity, v.restock_level, v.pricing_type, v.time_unit, v.time_interval, v.active, 'SERVICE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM inventory_categories c,
(VALUES
('ATENCIÓN EMERGENCIA', 'ATENCIÓN EMERGENCIA', 900.00, 0.00, 0, 0, 'FLAT', NULL::VARCHAR, NULL::INT, true),
('KETAMINA AMBULATORIA', 'PROCEDIMIENTO DE KETAMINA', 1500.00, 0.00, 0, 0, 'FLAT', NULL, NULL, true),
('KETAMINA INTERNA', 'PROCEDIMIENTO DE KETAMINA INTERNA', 1100.00, 0.00, 0, 0, 'FLAT', NULL, NULL, true)
) AS v(name, description, price, cost, quantity, restock_level, pricing_type, time_unit, time_interval, active)
WHERE c.name = 'Servicios';

-- Personal Especial
INSERT INTO inventory_items (category_id, name, description, price, cost, quantity, restock_level, pricing_type, time_unit, time_interval, active, kind, created_at, updated_at)
SELECT c.id, v.name, v.description, v.price, v.cost, v.quantity, v.restock_level, v.pricing_type, v.time_unit, v.time_interval, v.active, 'PERSONNEL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM inventory_categories c,
(VALUES
('ENFERMERA SOMBRA HOSPITAL', 'ENFERMERA SOMBRA HOSPITAL', 500.00, 300.00, 0, 0, 'FLAT', NULL::VARCHAR, NULL::INT, true),
('ENFERMERA SOMBRA DOMICILIO', 'ENFERMERA SOMBRA DOMICILIO', 650.00, 350.00, 0, 0, 'FLAT', NULL, NULL, true)
) AS v(name, description, price, cost, quantity, restock_level, pricing_type, time_unit, time_interval, active)
WHERE c.name = 'Personal Especial';

-- Ingredientes de Cocina
INSERT INTO inventory_items (category_id, name, description, price, cost, quantity, restock_level, pricing_type, time_unit, time_interval, active, kind, created_at, updated_at)
SELECT c.id, v.name, v.description, v.price, v.cost, v.quantity, v.restock_level, v.pricing_type, v.time_unit, v.time_interval, v.active, 'FOOD', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM inventory_categories c,
(VALUES
-- Granos y cereales
('ARROZ BLANCO (LIBRA)', 'ARROZ BLANCO DE PRIMERA CALIDAD', 5.50, 3.50, 100, 25, 'FLAT', NULL::VARCHAR, NULL::INT, true),
('FRIJOL NEGRO (LIBRA)', 'FRIJOL NEGRO SECO', 7.00, 4.50, 80, 20, 'FLAT', NULL, NULL, true),
('PASTA ESPAGUETI (400G)', 'PASTA TIPO ESPAGUETI', 8.50, 5.00, 40, 10, 'FLAT', NULL, NULL, true),
('PASTA CODITOS (400G)', 'PASTA TIPO CODITOS', 8.50, 5.00, 30, 10, 'FLAT', NULL, NULL, true),
('AVENA EN HOJUELAS (LIBRA)', 'AVENA EN HOJUELAS PARA COCINAR', 6.00, 3.80, 25, 10, 'FLAT', NULL, NULL, true),
('HARINA DE MAÍZ (LIBRA)', 'HARINA DE MAÍZ PARA TORTILLAS', 4.50, 2.80, 50, 15, 'FLAT', NULL, NULL, true),
('PAN DE SANDWICH (BOLSA)', 'PAN BLANCO DE MOLDE PARA SANDWICH', 18.00, 12.00, 15, 5, 'FLAT', NULL, NULL, true),
('TORTILLAS DE MAÍZ (DOCENA)', 'TORTILLAS DE MAÍZ FRESCAS', 8.00, 5.00, 20, 10, 'FLAT', NULL, NULL, true),
-- Proteínas
('POLLO ENTERO (LIBRA)', 'POLLO FRESCO ENTERO', 14.00, 9.00, 50, 15, 'FLAT', NULL, NULL, true),
('PECHUGA DE POLLO (LIBRA)', 'PECHUGA DE POLLO SIN HUESO', 22.00, 15.00, 40, 10, 'FLAT', NULL, NULL, true),
('CARNE DE RES (LIBRA)', 'CARNE DE RES PARA GUISAR', 30.00, 20.00, 30, 10, 'FLAT', NULL, NULL, true),
('HUEVOS (CARTON 30 UNIDADES)', 'HUEVOS DE GALLINA FRESCOS', 45.00, 30.00, 10, 3, 'FLAT', NULL, NULL, true),
('SALCHICHA (LIBRA)', 'SALCHICHA DE POLLO', 16.00, 10.00, 15, 5, 'FLAT', NULL, NULL, true),
-- Lácteos
('LECHE ENTERA (LITRO)', 'LECHE PASTEURIZADA ENTERA', 12.00, 8.00, 30, 10, 'FLAT', NULL, NULL, true),
('QUESO FRESCO (LIBRA)', 'QUESO FRESCO DE VACA', 25.00, 16.00, 10, 3, 'FLAT', NULL, NULL, true),
('CREMA (BOLSA 250ML)', 'CREMA FRESCA PARA COCINAR', 10.00, 6.50, 15, 5, 'FLAT', NULL, NULL, true),
('MANTEQUILLA (BARRA 250G)', 'MANTEQUILLA SIN SAL', 18.00, 12.00, 10, 3, 'FLAT', NULL, NULL, true),
-- Verduras y frutas
('TOMATE (LIBRA)', 'TOMATE FRESCO DE COCINA', 6.00, 3.50, 40, 15, 'FLAT', NULL, NULL, true),
('CEBOLLA (LIBRA)', 'CEBOLLA BLANCA', 5.00, 3.00, 35, 10, 'FLAT', NULL, NULL, true),
('PAPA (LIBRA)', 'PAPA PARA COCINAR', 5.50, 3.50, 40, 10, 'FLAT', NULL, NULL, true),
('ZANAHORIA (LIBRA)', 'ZANAHORIA FRESCA', 4.00, 2.50, 25, 10, 'FLAT', NULL, NULL, true),
('GUISQUIL (LIBRA)', 'GUISQUIL FRESCO', 4.50, 2.80, 20, 10, 'FLAT', NULL, NULL, true),
('LECHUGA (UNIDAD)', 'LECHUGA FRESCA PARA ENSALADA', 6.00, 3.50, 15, 5, 'FLAT', NULL, NULL, true),
('PLÁTANO MADURO (UNIDAD)', 'PLÁTANO MADURO PARA COCINAR', 2.50, 1.50, 40, 15, 'FLAT', NULL, NULL, true),
('BANANO (UNIDAD)', 'BANANO FRESCO', 1.50, 0.80, 60, 20, 'FLAT', NULL, NULL, true),
('NARANJA (UNIDAD)', 'NARANJA PARA JUGO', 2.00, 1.00, 50, 20, 'FLAT', NULL, NULL, true),
('LIMÓN (UNIDAD)', 'LIMÓN PERSA', 1.00, 0.50, 40, 15, 'FLAT', NULL, NULL, true),
-- Aceites, condimentos y básicos
('ACEITE VEGETAL (LITRO)', 'ACEITE VEGETAL PARA COCINAR', 22.00, 15.00, 15, 5, 'FLAT', NULL, NULL, true),
('AZÚCAR (LIBRA)', 'AZÚCAR BLANCA REFINADA', 5.00, 3.00, 40, 15, 'FLAT', NULL, NULL, true),
('SAL (LIBRA)', 'SAL DE COCINA', 2.50, 1.50, 20, 5, 'FLAT', NULL, NULL, true),
('CONSOMÉ DE POLLO (SOBRE)', 'CONSOMÉ DE POLLO EN POLVO', 3.00, 1.80, 50, 15, 'FLAT', NULL, NULL, true),
('SALSA DE TOMATE (BOTELLA 400ML)', 'SALSA DE TOMATE PARA COCINAR', 12.00, 7.50, 15, 5, 'FLAT', NULL, NULL, true),
('MAYONESA (FRASCO 400G)', 'MAYONESA PARA COCINAR', 20.00, 13.00, 8, 3, 'FLAT', NULL, NULL, true),
-- Bebidas
('CAFÉ MOLIDO (LIBRA)', 'CAFÉ MOLIDO TOSTADO', 30.00, 20.00, 10, 3, 'FLAT', NULL, NULL, true),
('TÉ EN BOLSITAS (CAJA 25 UNIDADES)', 'TÉ DE MANZANILLA O HIERBAS', 15.00, 9.00, 10, 3, 'FLAT', NULL, NULL, true),
('AGUA PURA (GARRAFÓN 5 GALONES)', 'AGUA PURIFICADA PARA CONSUMO', 25.00, 15.00, 8, 3, 'FLAT', NULL, NULL, true),
('JUGO DE NARANJA (LITRO)', 'JUGO DE NARANJA NATURAL', 15.00, 9.00, 12, 5, 'FLAT', NULL, NULL, true),
('LECHE EN POLVO (BOLSA 400G)', 'LECHE EN POLVO INSTANTÁNEA', 28.00, 18.00, 10, 3, 'FLAT', NULL, NULL, true)
) AS v(name, description, price, cost, quantity, restock_level, pricing_type, time_unit, time_interval, active)
WHERE c.name = 'Ingredientes de Cocina';

-- Alimentación
INSERT INTO inventory_items (category_id, name, description, price, cost, quantity, restock_level, pricing_type, time_unit, time_interval, active, kind, created_at, updated_at)
SELECT c.id, v.name, v.description, v.price, v.cost, v.quantity, v.restock_level, v.pricing_type, v.time_unit, v.time_interval, v.active, 'FOOD', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM inventory_categories c,
(VALUES
-- Tiempos de comida regulares
('DESAYUNO REGULAR', 'DESAYUNO COMPLETO: HUEVOS, FRIJOLES, PLÁTANO, PAN, CAFÉ O JUGO', 65.00, 35.00, 0, 0, 'FLAT', NULL::VARCHAR, NULL::INT, true),
('ALMUERZO REGULAR', 'ALMUERZO COMPLETO: SOPA, PLATO FUERTE CON PROTEINA, ARROZ, ENSALADA, REFRESCO', 85.00, 45.00, 0, 0, 'FLAT', NULL, NULL, true),
('CENA REGULAR', 'CENA COMPLETA: PLATO FUERTE LIVIANO, ACOMPAÑAMIENTO, BEBIDA', 70.00, 38.00, 0, 0, 'FLAT', NULL, NULL, true),
('MERIENDA', 'MERIENDA: FRUTA, GALLETAS O SANDWICH PEQUEÑO CON BEBIDA', 30.00, 15.00, 0, 0, 'FLAT', NULL, NULL, true),
('REFACCIÓN NOCTURNA', 'REFACCIÓN NOCTURNA LIVIANA: CEREAL, FRUTA O PAN CON BEBIDA CALIENTE', 25.00, 12.00, 0, 0, 'FLAT', NULL, NULL, true),
-- Dietas especiales
('DESAYUNO DIETA DIABÉTICA', 'DESAYUNO PARA PACIENTE DIABÉTICO: HUEVOS, FRIJOLES, PAN INTEGRAL, BEBIDA SIN AZÚCAR', 75.00, 40.00, 0, 0, 'FLAT', NULL, NULL, true),
('ALMUERZO DIETA DIABÉTICA', 'ALMUERZO PARA PACIENTE DIABÉTICO: PROTEINA, VEGETALES, PORCIÓN CONTROLADA DE CARBOHIDRATOS', 95.00, 50.00, 0, 0, 'FLAT', NULL, NULL, true),
('CENA DIETA DIABÉTICA', 'CENA PARA PACIENTE DIABÉTICO: PLATO LIVIANO, VEGETALES, BEBIDA SIN AZÚCAR', 80.00, 42.00, 0, 0, 'FLAT', NULL, NULL, true),
('DIETA BLANDA (TIEMPO DE COMIDA)', 'COMIDA DE DIETA BLANDA: ALIMENTOS SUAVES, FÁCILES DE DIGERIR', 70.00, 38.00, 0, 0, 'FLAT', NULL, NULL, true),
('DIETA LÍQUIDA (TIEMPO DE COMIDA)', 'DIETA LÍQUIDA: CALDOS, JUGOS, GELATINA, BEBIDAS NUTRITIVAS', 50.00, 25.00, 0, 0, 'FLAT', NULL, NULL, true),
('DIETA HIPOSÓDICA (TIEMPO DE COMIDA)', 'COMIDA BAJA EN SODIO PARA PACIENTES CON HIPERTENSIÓN', 80.00, 42.00, 0, 0, 'FLAT', NULL, NULL, true),
-- Extras
('SUPLEMENTO NUTRICIONAL', 'BEBIDA DE SUPLEMENTO NUTRICIONAL TIPO ENSURE O SIMILAR', 45.00, 28.00, 20, 5, 'FLAT', NULL, NULL, true),
('PORCIÓN DE FRUTA EXTRA', 'PORCIÓN ADICIONAL DE FRUTA FRESCA DE TEMPORADA', 15.00, 8.00, 0, 0, 'FLAT', NULL, NULL, true),
('BEBIDA CALIENTE EXTRA', 'CAFÉ, TÉ O CHOCOLATE CALIENTE ADICIONAL', 10.00, 5.00, 0, 0, 'FLAT', NULL, NULL, true),
('JUGO NATURAL EXTRA', 'VASO DE JUGO NATURAL DE NARANJA O FRUTA DE TEMPORADA', 15.00, 8.00, 0, 0, 'FLAT', NULL, NULL, true)
) AS v(name, description, price, cost, quantity, restock_level, pricing_type, time_unit, time_interval, active)
WHERE c.name = 'Alimentación';

SET session_replication_role = DEFAULT;
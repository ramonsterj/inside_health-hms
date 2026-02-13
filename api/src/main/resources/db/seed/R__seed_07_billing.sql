-- ============================================================================
-- SEED FILE 07: Patient Charges, Invoices
-- ============================================================================

SET session_replication_role = replica;

-- ============================================================================
-- ROOM CHARGES (daily, via generate_series)
-- ============================================================================

-- Helper: Active patients (admission_date to CURRENT_DATE - 1)
-- Juan Pérez González (room 201, Q1100)
INSERT INTO patient_charges (admission_id, charge_type, description, quantity, unit_price, total_amount, charge_date, room_id, created_at, updated_at, created_by)
SELECT a.id, 'ROOM', 'Cargo diario de habitación ' || rm.number, 1, rm.price, rm.price,
  (a.admission_date::DATE + gs.d), rm.id,
  (a.admission_date::DATE + gs.d + TIME '23:59'), (a.admission_date::DATE + gs.d + TIME '23:59'),
  (SELECT id FROM users WHERE username = 'admin')
FROM admissions a JOIN patients p ON a.patient_id = p.id JOIN rooms rm ON a.room_id = rm.id
CROSS JOIN generate_series(0, (CURRENT_DATE - 1 - a.admission_date::DATE)::INT) AS gs(d)
WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- Maria Santos López (room 101, Q950)
INSERT INTO patient_charges (admission_id, charge_type, description, quantity, unit_price, total_amount, charge_date, room_id, created_at, updated_at, created_by)
SELECT a.id, 'ROOM', 'Cargo diario de habitación ' || rm.number, 1, rm.price, rm.price,
  (a.admission_date::DATE + gs.d), rm.id,
  (a.admission_date::DATE + gs.d + TIME '23:59'), (a.admission_date::DATE + gs.d + TIME '23:59'),
  (SELECT id FROM users WHERE username = 'admin')
FROM admissions a JOIN patients p ON a.patient_id = p.id JOIN rooms rm ON a.room_id = rm.id
CROSS JOIN generate_series(0, (CURRENT_DATE - 1 - a.admission_date::DATE)::INT) AS gs(d)
WHERE p.first_name = 'Maria' AND p.last_name = 'Santos López' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- Pedro García Hernández (room 303, Q950)
INSERT INTO patient_charges (admission_id, charge_type, description, quantity, unit_price, total_amount, charge_date, room_id, created_at, updated_at, created_by)
SELECT a.id, 'ROOM', 'Cargo diario de habitación ' || rm.number, 1, rm.price, rm.price,
  (a.admission_date::DATE + gs.d), rm.id,
  (a.admission_date::DATE + gs.d + TIME '23:59'), (a.admission_date::DATE + gs.d + TIME '23:59'),
  (SELECT id FROM users WHERE username = 'admin')
FROM admissions a JOIN patients p ON a.patient_id = p.id JOIN rooms rm ON a.room_id = rm.id
CROSS JOIN generate_series(0, (CURRENT_DATE - 1 - a.admission_date::DATE)::INT) AS gs(d)
WHERE p.first_name = 'Pedro' AND p.last_name = 'García Hernández' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- Ana Martínez Ruiz (room 102, Q950)
INSERT INTO patient_charges (admission_id, charge_type, description, quantity, unit_price, total_amount, charge_date, room_id, created_at, updated_at, created_by)
SELECT a.id, 'ROOM', 'Cargo diario de habitación ' || rm.number, 1, rm.price, rm.price,
  (a.admission_date::DATE + gs.d), rm.id,
  (a.admission_date::DATE + gs.d + TIME '23:59'), (a.admission_date::DATE + gs.d + TIME '23:59'),
  (SELECT id FROM users WHERE username = 'admin')
FROM admissions a JOIN patients p ON a.patient_id = p.id JOIN rooms rm ON a.room_id = rm.id
CROSS JOIN generate_series(0, (CURRENT_DATE - 1 - a.admission_date::DATE)::INT) AS gs(d)
WHERE p.first_name = 'Ana' AND p.last_name = 'Martínez Ruiz' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- Luis Morales Castro (room 202, Q1100)
INSERT INTO patient_charges (admission_id, charge_type, description, quantity, unit_price, total_amount, charge_date, room_id, created_at, updated_at, created_by)
SELECT a.id, 'ROOM', 'Cargo diario de habitación ' || rm.number, 1, rm.price, rm.price,
  (a.admission_date::DATE + gs.d), rm.id,
  (a.admission_date::DATE + gs.d + TIME '23:59'), (a.admission_date::DATE + gs.d + TIME '23:59'),
  (SELECT id FROM users WHERE username = 'admin')
FROM admissions a JOIN patients p ON a.patient_id = p.id JOIN rooms rm ON a.room_id = rm.id
CROSS JOIN generate_series(0, (CURRENT_DATE - 1 - a.admission_date::DATE)::INT) AS gs(d)
WHERE p.first_name = 'Luis' AND p.last_name = 'Morales Castro' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- Carmen Flores Mejía (room 103, Q950)
INSERT INTO patient_charges (admission_id, charge_type, description, quantity, unit_price, total_amount, charge_date, room_id, created_at, updated_at, created_by)
SELECT a.id, 'ROOM', 'Cargo diario de habitación ' || rm.number, 1, rm.price, rm.price,
  (a.admission_date::DATE + gs.d), rm.id,
  (a.admission_date::DATE + gs.d + TIME '23:59'), (a.admission_date::DATE + gs.d + TIME '23:59'),
  (SELECT id FROM users WHERE username = 'admin')
FROM admissions a JOIN patients p ON a.patient_id = p.id JOIN rooms rm ON a.room_id = rm.id
CROSS JOIN generate_series(0, (CURRENT_DATE - 1 - a.admission_date::DATE)::INT) AS gs(d)
WHERE p.first_name = 'Carmen' AND p.last_name = 'Flores Mejía' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- Roberto Díaz Vargas (room 203, Q1100)
INSERT INTO patient_charges (admission_id, charge_type, description, quantity, unit_price, total_amount, charge_date, room_id, created_at, updated_at, created_by)
SELECT a.id, 'ROOM', 'Cargo diario de habitación ' || rm.number, 1, rm.price, rm.price,
  (a.admission_date::DATE + gs.d), rm.id,
  (a.admission_date::DATE + gs.d + TIME '23:59'), (a.admission_date::DATE + gs.d + TIME '23:59'),
  (SELECT id FROM users WHERE username = 'admin')
FROM admissions a JOIN patients p ON a.patient_id = p.id JOIN rooms rm ON a.room_id = rm.id
CROSS JOIN generate_series(0, (CURRENT_DATE - 1 - a.admission_date::DATE)::INT) AS gs(d)
WHERE p.first_name = 'Roberto' AND p.last_name = 'Díaz Vargas' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- Sofia Ramírez Paz (room 109, Q1100)
INSERT INTO patient_charges (admission_id, charge_type, description, quantity, unit_price, total_amount, charge_date, room_id, created_at, updated_at, created_by)
SELECT a.id, 'ROOM', 'Cargo diario de habitación ' || rm.number, 1, rm.price, rm.price,
  (a.admission_date::DATE + gs.d), rm.id,
  (a.admission_date::DATE + gs.d + TIME '23:59'), (a.admission_date::DATE + gs.d + TIME '23:59'),
  (SELECT id FROM users WHERE username = 'admin')
FROM admissions a JOIN patients p ON a.patient_id = p.id JOIN rooms rm ON a.room_id = rm.id
CROSS JOIN generate_series(0, (CURRENT_DATE - 1 - a.admission_date::DATE)::INT) AS gs(d)
WHERE p.first_name = 'Sofia' AND p.last_name = 'Ramírez Paz' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- Discharged: Miguel Torres Luna (room 301, Q1100, 14-day stay)
INSERT INTO patient_charges (admission_id, charge_type, description, quantity, unit_price, total_amount, charge_date, room_id, created_at, updated_at, created_by)
SELECT a.id, 'ROOM', 'Cargo diario de habitación ' || rm.number, 1, rm.price, rm.price,
  (a.admission_date::DATE + gs.d), rm.id,
  (a.admission_date::DATE + gs.d + TIME '23:59'), (a.admission_date::DATE + gs.d + TIME '23:59'),
  (SELECT id FROM users WHERE username = 'admin')
FROM admissions a JOIN patients p ON a.patient_id = p.id JOIN rooms rm ON a.room_id = rm.id
CROSS JOIN generate_series(0, (a.discharge_date::DATE - 1 - a.admission_date::DATE)::INT) AS gs(d)
WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED';

-- Discharged: Elena Sánchez Rivas (room 104, Q950, 11-day stay)
INSERT INTO patient_charges (admission_id, charge_type, description, quantity, unit_price, total_amount, charge_date, room_id, created_at, updated_at, created_by)
SELECT a.id, 'ROOM', 'Cargo diario de habitación ' || rm.number, 1, rm.price, rm.price,
  (a.admission_date::DATE + gs.d), rm.id,
  (a.admission_date::DATE + gs.d + TIME '23:59'), (a.admission_date::DATE + gs.d + TIME '23:59'),
  (SELECT id FROM users WHERE username = 'admin')
FROM admissions a JOIN patients p ON a.patient_id = p.id JOIN rooms rm ON a.room_id = rm.id
CROSS JOIN generate_series(0, (a.discharge_date::DATE - 1 - a.admission_date::DATE)::INT) AS gs(d)
WHERE p.first_name = 'Elena' AND p.last_name = 'Sánchez Rivas' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED';

-- Discharged: Francisco Mendoza Aguilar (room 302, Q1100, 11-day stay)
INSERT INTO patient_charges (admission_id, charge_type, description, quantity, unit_price, total_amount, charge_date, room_id, created_at, updated_at, created_by)
SELECT a.id, 'ROOM', 'Cargo diario de habitación ' || rm.number, 1, rm.price, rm.price,
  (a.admission_date::DATE + gs.d), rm.id,
  (a.admission_date::DATE + gs.d + TIME '23:59'), (a.admission_date::DATE + gs.d + TIME '23:59'),
  (SELECT id FROM users WHERE username = 'admin')
FROM admissions a JOIN patients p ON a.patient_id = p.id JOIN rooms rm ON a.room_id = rm.id
CROSS JOIN generate_series(0, (a.discharge_date::DATE - 1 - a.admission_date::DATE)::INT) AS gs(d)
WHERE p.first_name = 'Francisco' AND p.last_name = 'Mendoza Aguilar' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED';

-- ============================================================================
-- DIET CHARGES (daily, Q150/day for all hospitalized patients)
-- ============================================================================

-- Active patients
INSERT INTO patient_charges (admission_id, charge_type, description, quantity, unit_price, total_amount, charge_date, created_at, updated_at, created_by)
SELECT a.id, 'DIET', 'Cargo diario de alimentación', 1, 150.00, 150.00,
  (a.admission_date::DATE + gs.d),
  (a.admission_date::DATE + gs.d + TIME '23:59'), (a.admission_date::DATE + gs.d + TIME '23:59'),
  (SELECT id FROM users WHERE username = 'admin')
FROM admissions a JOIN patients p ON a.patient_id = p.id
CROSS JOIN generate_series(0, (CURRENT_DATE - 1 - a.admission_date::DATE)::INT) AS gs(d)
WHERE a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE'
  AND p.first_name IN ('Juan','Maria','Pedro','Ana','Luis','Carmen','Roberto','Sofia');

-- Discharged patients
INSERT INTO patient_charges (admission_id, charge_type, description, quantity, unit_price, total_amount, charge_date, created_at, updated_at, created_by)
SELECT a.id, 'DIET', 'Cargo diario de alimentación', 1, 150.00, 150.00,
  (a.admission_date::DATE + gs.d),
  (a.admission_date::DATE + gs.d + TIME '23:59'), (a.admission_date::DATE + gs.d + TIME '23:59'),
  (SELECT id FROM users WHERE username = 'admin')
FROM admissions a JOIN patients p ON a.patient_id = p.id
CROSS JOIN generate_series(0, (a.discharge_date::DATE - 1 - a.admission_date::DATE)::INT) AS gs(d)
WHERE a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED';

-- ============================================================================
-- MEDICATION CHARGES (one per GIVEN medication administration)
-- ============================================================================
INSERT INTO patient_charges (admission_id, charge_type, description, quantity, unit_price, total_amount, charge_date, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'MEDICATION', 'Administración de ' || mo.medication,
  1, COALESCE(ii.price, 0), COALESCE(ii.price, 0),
  ma.administered_at::DATE, ii.id,
  ma.administered_at, ma.administered_at,
  (SELECT id FROM users WHERE username = 'admin')
FROM medication_administrations ma
JOIN medical_orders mo ON ma.medical_order_id = mo.id
JOIN admissions a ON ma.admission_id = a.id
LEFT JOIN inventory_items ii ON mo.inventory_item_id = ii.id
WHERE ma.status = 'GIVEN' AND ma.deleted_at IS NULL AND mo.deleted_at IS NULL;

-- ============================================================================
-- LAB CHARGES (one per LABORATORIOS medical order)
-- ============================================================================
INSERT INTO patient_charges (admission_id, charge_type, description, quantity, unit_price, total_amount, charge_date, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'LAB', 'Laboratorio: ' || COALESCE(mo.medication, mo.observations),
  1, COALESCE(ii.price, 0), COALESCE(ii.price, 0),
  mo.start_date, ii.id,
  a.admission_date + INTERVAL '2 hours', a.admission_date + INTERVAL '2 hours',
  (SELECT id FROM users WHERE username = 'admin')
FROM medical_orders mo
JOIN admissions a ON mo.admission_id = a.id
LEFT JOIN inventory_items ii ON mo.inventory_item_id = ii.id
WHERE mo.category = 'LABORATORIOS' AND mo.deleted_at IS NULL;

-- ============================================================================
-- SERVICE CHARGES (one per psychotherapy activity)
-- ============================================================================
INSERT INTO patient_charges (admission_id, charge_type, description, quantity, unit_price, total_amount, charge_date, created_at, updated_at, created_by)
SELECT pa.admission_id, 'SERVICE', 'Actividad terapéutica: ' || pc.name,
  1, pc.price, pc.price,
  pa.created_at::DATE, pa.created_at, pa.created_at,
  (SELECT id FROM users WHERE username = 'admin')
FROM psychotherapy_activities pa
JOIN psychotherapy_categories pc ON pa.category_id = pc.id
WHERE pa.deleted_at IS NULL;

-- ============================================================================
-- PROCEDURE CHARGES (3)
-- ============================================================================
-- Electroshock therapy - Andres Ortiz
INSERT INTO patient_charges (admission_id, charge_type, description, quantity, unit_price, total_amount, charge_date, created_at, updated_at, created_by)
SELECT a.id, 'PROCEDURE', 'Terapia electroconvulsiva (TEC)', 1, 2500.00, 2500.00,
  a.admission_date::DATE, a.admission_date, a.admission_date, (SELECT id FROM users WHERE username = 'admin')
FROM admissions a JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Andres' AND p.last_name = 'Ortiz Barrios' AND a.type = 'ELECTROSHOCK_THERAPY';

-- Ketamine infusión - Gabriela Reyes
INSERT INTO patient_charges (admission_id, charge_type, description, quantity, unit_price, total_amount, charge_date, created_at, updated_at, created_by)
SELECT a.id, 'PROCEDURE', 'Infusión de ketamina', 1, 3000.00, 3000.00,
  a.admission_date::DATE, a.admission_date, a.admission_date, (SELECT id FROM users WHERE username = 'admin')
FROM admissions a JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Gabriela' AND p.last_name = 'Reyes Soto' AND a.type = 'KETAMINE_INFUSIÓN';

-- Emergency consult - Oscar Vasquez
INSERT INTO patient_charges (admission_id, charge_type, description, quantity, unit_price, total_amount, charge_date, created_at, updated_at, created_by)
SELECT a.id, 'PROCEDURE', 'Consulta de emergencia psiquiátrica', 1, 900.00, 900.00,
  a.admission_date::DATE, a.admission_date, a.admission_date, (SELECT id FROM users WHERE username = 'admin')
FROM admissions a JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Oscar' AND p.last_name = 'Vásquez Pineda' AND a.type = 'EMERGENCY';

-- ============================================================================
-- ADJUSTMENT CHARGES (2, discharged patients only)
-- ============================================================================
INSERT INTO patient_charges (admission_id, charge_type, description, quantity, unit_price, total_amount, charge_date, reason, created_at, updated_at, created_by)
SELECT a.id, 'ADJUSTMENT', 'Descuento de cortesia', 1, -500.00, -500.00,
  a.discharge_date::DATE, 'Descuento autorizado por gerencia por estadia prolongada',
  a.discharge_date, a.discharge_date, (SELECT id FROM users WHERE username = 'admin')
FROM admissions a JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna' AND a.status = 'DISCHARGED';

INSERT INTO patient_charges (admission_id, charge_type, description, quantity, unit_price, total_amount, charge_date, reason, created_at, updated_at, created_by)
SELECT a.id, 'ADJUSTMENT', 'Correccion de facturación', 1, -150.00, -150.00,
  a.discharge_date::DATE, 'Cargo duplicado de dieta corregido',
  a.discharge_date, a.discharge_date, (SELECT id FROM users WHERE username = 'admin')
FROM admissions a JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Elena' AND p.last_name = 'Sánchez Rivas' AND a.status = 'DISCHARGED';

-- ============================================================================
-- INVOICES FOR DISCHARGED PATIENTS (3)
-- ============================================================================
INSERT INTO invoices (invoice_number, admission_id, total_amount, charge_count, notes, created_at, updated_at, created_by)
SELECT 'INV-' || EXTRACT(YEAR FROM CURRENT_DATE)::TEXT || '-0001', a.id,
  (SELECT COALESCE(SUM(pc.total_amount), 0) FROM patient_charges pc WHERE pc.admission_id = a.id AND pc.deleted_at IS NULL),
  (SELECT COUNT(*) FROM patient_charges pc WHERE pc.admission_id = a.id AND pc.deleted_at IS NULL),
  'Factura generada al alta del paciente', a.discharge_date, a.discharge_date,
  (SELECT id FROM users WHERE username = 'admin')
FROM admissions a JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna' AND a.status = 'DISCHARGED';

INSERT INTO invoices (invoice_number, admission_id, total_amount, charge_count, notes, created_at, updated_at, created_by)
SELECT 'INV-' || EXTRACT(YEAR FROM CURRENT_DATE)::TEXT || '-0002', a.id,
  (SELECT COALESCE(SUM(pc.total_amount), 0) FROM patient_charges pc WHERE pc.admission_id = a.id AND pc.deleted_at IS NULL),
  (SELECT COUNT(*) FROM patient_charges pc WHERE pc.admission_id = a.id AND pc.deleted_at IS NULL),
  'Factura generada al alta del paciente', a.discharge_date, a.discharge_date,
  (SELECT id FROM users WHERE username = 'admin')
FROM admissions a JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Elena' AND p.last_name = 'Sánchez Rivas' AND a.status = 'DISCHARGED';

INSERT INTO invoices (invoice_number, admission_id, total_amount, charge_count, notes, created_at, updated_at, created_by)
SELECT 'INV-' || EXTRACT(YEAR FROM CURRENT_DATE)::TEXT || '-0003', a.id,
  (SELECT COALESCE(SUM(pc.total_amount), 0) FROM patient_charges pc WHERE pc.admission_id = a.id AND pc.deleted_at IS NULL),
  (SELECT COUNT(*) FROM patient_charges pc WHERE pc.admission_id = a.id AND pc.deleted_at IS NULL),
  'Factura generada al alta del paciente', a.discharge_date, a.discharge_date,
  (SELECT id FROM users WHERE username = 'admin')
FROM admissions a JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Francisco' AND p.last_name = 'Mendoza Aguilar' AND a.status = 'DISCHARGED';

-- Link charges to invoices
UPDATE patient_charges SET invoice_id = (
  SELECT i.id FROM invoices i JOIN admissions a ON i.admission_id = a.id JOIN patients p ON a.patient_id = p.id
  WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna' LIMIT 1
) WHERE admission_id = (
  SELECT a.id FROM admissions a JOIN patients p ON a.patient_id = p.id
  WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna' AND a.status = 'DISCHARGED'
) AND deleted_at IS NULL;

UPDATE patient_charges SET invoice_id = (
  SELECT i.id FROM invoices i JOIN admissions a ON i.admission_id = a.id JOIN patients p ON a.patient_id = p.id
  WHERE p.first_name = 'Elena' AND p.last_name = 'Sánchez Rivas' LIMIT 1
) WHERE admission_id = (
  SELECT a.id FROM admissions a JOIN patients p ON a.patient_id = p.id
  WHERE p.first_name = 'Elena' AND p.last_name = 'Sánchez Rivas' AND a.status = 'DISCHARGED'
) AND deleted_at IS NULL;

UPDATE patient_charges SET invoice_id = (
  SELECT i.id FROM invoices i JOIN admissions a ON i.admission_id = a.id JOIN patients p ON a.patient_id = p.id
  WHERE p.first_name = 'Francisco' AND p.last_name = 'Mendoza Aguilar' LIMIT 1
) WHERE admission_id = (
  SELECT a.id FROM admissions a JOIN patients p ON a.patient_id = p.id
  WHERE p.first_name = 'Francisco' AND p.last_name = 'Mendoza Aguilar' AND a.status = 'DISCHARGED'
) AND deleted_at IS NULL;

SET session_replication_role = DEFAULT;

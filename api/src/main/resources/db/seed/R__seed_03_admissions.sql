-- ============================================================================
-- SEED FILE 03: Staff, Psych Categories, Patient Notes, Admissions
-- ============================================================================

SET session_replication_role = replica;

-- ============================================================================
-- STEP 11: CREATE NEW DOCTORS AND NURSES
-- ============================================================================
INSERT INTO users (username, email, password_hash, first_name, last_name, salutation, status, email_verified, must_change_password, created_at, updated_at) VALUES
('doctor3', 'doctor3@example.com', '$2b$10$PYXULrV.BlNnIPSz8HRFJeId5axQ/qoAQNhEldlY/H7xlqIpH35YC', 'Eduardo', 'Cifuentes', 'DR', 'ACTIVE', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('doctor4', 'doctor4@example.com', '$2b$10$PYXULrV.BlNnIPSz8HRFJeId5axQ/qoAQNhEldlY/H7xlqIpH35YC', 'Claudia', 'Barrios', 'DRA', 'ACTIVE', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('doctor5', 'doctor5@example.com', '$2b$10$PYXULrV.BlNnIPSz8HRFJeId5axQ/qoAQNhEldlY/H7xlqIpH35YC', 'Fernando', 'Castellanos', 'DR', 'ACTIVE', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('doctor6', 'doctor6@example.com', '$2b$10$PYXULrV.BlNnIPSz8HRFJeId5axQ/qoAQNhEldlY/H7xlqIpH35YC', 'Ingrid', 'Solares', 'DRA', 'ACTIVE', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('doctor7', 'doctor7@example.com', '$2b$10$PYXULrV.BlNnIPSz8HRFJeId5axQ/qoAQNhEldlY/H7xlqIpH35YC', 'Marco', 'Arriaga', 'DR', 'ACTIVE', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('doctor8', 'doctor8@example.com', '$2b$10$PYXULrV.BlNnIPSz8HRFJeId5axQ/qoAQNhEldlY/H7xlqIpH35YC', 'Adriana', 'Estrada', 'DRA', 'ACTIVE', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('doctor9', 'doctor9@example.com', '$2b$10$PYXULrV.BlNnIPSz8HRFJeId5axQ/qoAQNhEldlY/H7xlqIpH35YC', 'Sergio', 'Lemus', 'DR', 'ACTIVE', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('doctor10', 'doctor10@example.com', '$2b$10$PYXULrV.BlNnIPSz8HRFJeId5axQ/qoAQNhEldlY/H7xlqIpH35YC', 'Karla', 'Monterroso', 'DRA', 'ACTIVE', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('nurse3', 'nurse3@example.com', '$2b$10$PYXULrV.BlNnIPSz8HRFJeId5axQ/qoAQNhEldlY/H7xlqIpH35YC', 'Andrea', 'Quevedo', 'SRTA', 'ACTIVE', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('nurse4', 'nurse4@example.com', '$2b$10$PYXULrV.BlNnIPSz8HRFJeId5axQ/qoAQNhEldlY/H7xlqIpH35YC', 'Pablo', 'Cifuentes', 'SR', 'ACTIVE', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ============================================================================
-- STEP 12: ASSIGN ROLES TO NEW USERS
-- ============================================================================
INSERT INTO user_roles (user_id, role_id, created_at, updated_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u, roles r
WHERE u.username IN ('doctor3','doctor4','doctor5','doctor6','doctor7','doctor8','doctor9','doctor10')
  AND r.code = 'DOCTOR';

INSERT INTO user_roles (user_id, role_id, created_at, updated_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u, roles r
WHERE u.username IN ('nurse3','nurse4') AND r.code = 'NURSE';

-- ============================================================================
-- STEP 13: RESEED PSYCHOTHERAPY CATEGORIES WITH PRICES
-- ============================================================================
INSERT INTO psychotherapy_categories (name, description, display_order, active, price, cost, created_at, updated_at) VALUES
('Taller', 'Talleres terapéuticos grupales', 1, true, 200.00, 80.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Sesión individual', 'Sesiones individuales de psicoterapia', 2, true, 450.00, 180.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Terapia con mascotas', 'Terapia asistida con animales', 3, true, 250.00, 100.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Pilates', 'Sesiones de pilates terapéutico', 4, true, 175.00, 70.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Meditación guiada', 'Sesiones de meditación y mindfulness', 5, true, 150.00, 60.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Terapia grupal', 'Sesiones de terapia grupal', 6, true, 300.00, 120.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Arte terapia', 'Terapia a través del arte y expresión creativa', 7, true, 275.00, 110.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Musicoterapia', 'Terapia a través de la música', 8, true, 275.00, 110.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Terapia ocupacional', 'Actividades de terapia ocupacional', 9, true, 225.00, 90.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Otra', 'Otras actividades terapéuticas', 10, true, 200.00, 80.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ============================================================================
-- STEP 14: UPDATE PATIENT NOTES FOR PSYCHIATRIC CONTEXT
-- ============================================================================
UPDATE patients SET notes = 'Dx: TDM severo, episodio recurrente con intento suicida reciente (sobreingesta de benzodiacepinas). Antecedentes: 2 hospitalizaciones psiquiátricas previas. Alergia: Sulfonamidas. Riesgo suicida: Alto.' WHERE first_name = 'Juan' AND last_name = 'Pérez González';
UPDATE patients SET notes = 'Dx: Trastorno Bipolar I, episodio maníaco con síntomas psicóticos. Antecedentes: Dx a los 22 años, litio previo descontinuado por nefrotoxicidad. Sin alergias conocidas.' WHERE first_name = 'Maria' AND last_name = 'Santos López';
UPDATE patients SET notes = 'Dx: Trastorno por consumo de alcohol severo, síndrome de abstinencia con riesgo de delirium tremens. DM2 en tratamiento. Hepatopatía alcohólica. Fumador 20 cigarrillos/día.' WHERE first_name = 'Pedro' AND last_name = 'García Hernández';
UPDATE patients SET notes = 'Dx: Esquizofrenia paranoide, descompensación por abandono de tratamiento. HTA controlada. Alergia: Haloperidol (distonía aguda).' WHERE first_name = 'Ana' AND last_name = 'Martínez Ruiz';
UPDATE patients SET notes = 'Dx: Trastorno por uso de polisubstancias (cannabis, cocaína, benzodiacepinas) con psicosis inducida por sustancias. Sin antecedentes médicos relevantes.' WHERE first_name = 'Luis' AND last_name = 'Morales Castro';
UPDATE patients SET notes = 'Dx: TEPT crónico secundario a violencia intrafamiliar con ideación suicida activa. Alergia: Penicilina. Sin otros antecedentes médicos.' WHERE first_name = 'Carmen' AND last_name = 'Flores Mejía';
UPDATE patients SET notes = 'Dx: Trastorno Bipolar II, episodio depresivo mayor actual. Cardiopatía isquémica estable. Hipotiroidismo en tratamiento.' WHERE first_name = 'Roberto' AND last_name = 'Díaz Vargas';
UPDATE patients SET notes = 'Dx: Trastorno límite de personalidad con autolesiónes recurrentes (cutting). Antecedentes: 3 ingresos previos por crisis. Sin alergias.' WHERE first_name = 'Sofia' AND last_name = 'Ramírez Paz';
UPDATE patients SET notes = 'Dx: Esquizofrenia paranoide crónica. Múltiples hospitalizaciones. Respondedor parcial a antipsicóticos típicos.' WHERE first_name = 'Miguel' AND last_name = 'Torres Luna';
UPDATE patients SET notes = 'Dx: Trastorno de ansiedad generalizada con trastorno de pánico. Primera hospitalización psiquiátrica. Sin antecedentes médicos relevantes.' WHERE first_name = 'Elena' AND last_name = 'Sánchez Rivas';
UPDATE patients SET notes = 'Dx: TDM recurrente en adulto mayor, episodio actual severo con síntomas melancólicos. Artritis reumatoide. Movilidad limitada.' WHERE first_name = 'Francisco' AND last_name = 'Mendoza Aguilar';
UPDATE patients SET notes = 'Dx: Trastorno depresivo persistente (distimia). Consulta ambulatoria. Primera evaluación psiquiátrica.' WHERE first_name = 'Isabella' AND last_name = 'Cruz Monzón';
UPDATE patients SET notes = 'Dx: TDM resistente a tratamiento. Referido para terapia electroconvulsiva. 3 esquemas antidepresivos fallidos.' WHERE first_name = 'Andres' AND last_name = 'Ortiz Barrios';
UPDATE patients SET notes = 'Dx: TDM resistente a tratamiento con ideación suicida pasiva. Referida para infusión de ketamina. Sin embarazo actual.' WHERE first_name = 'Gabriela' AND last_name = 'Reyes Soto';
UPDATE patients SET notes = 'Dx: Crisis de ansiedad aguda con síntomas disociativos. Evaluación de emergencia. Estrés laboral severo.' WHERE first_name = 'Oscar' AND last_name = 'Vásquez Pineda';
UPDATE patients SET notes = 'Dx: Trastorno de adaptación con ánimo depresivo. Consulta ambulatoria de seguimiento.' WHERE first_name = 'Diego' AND last_name = 'Castillo Moreno';

-- ============================================================================
-- STEP 15: CREATE ADMISSIONS (16 total)
-- ============================================================================
-- === Active Hospitalizations (8) ===

-- Juan Perez - MDD severe + suicide attempt - 14 days
INSERT INTO admissions (patient_id, triage_code_id, room_id, treating_physician_id, admission_date, status, type, created_at, updated_at, created_by)
SELECT p.id, tc.id, rm.id, u.id,
  (CURRENT_DATE - INTERVAL '14 days') + TIME '10:30',
  'ACTIVE', 'HOSPITALIZATION',
  (CURRENT_DATE - INTERVAL '14 days') + TIME '10:30', CURRENT_TIMESTAMP, u.id
FROM patients p, triage_codes tc, rooms rm, users u
WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González'
  AND tc.code = 'B' AND rm.number = '201' AND u.username = 'doctor1';

-- Maria Santos - Bipolar I manic + psychosis - 12 days
INSERT INTO admissions (patient_id, triage_code_id, room_id, treating_physician_id, admission_date, status, type, created_at, updated_at, created_by)
SELECT p.id, tc.id, rm.id, u.id,
  (CURRENT_DATE - INTERVAL '12 days') + TIME '14:15',
  'ACTIVE', 'HOSPITALIZATION',
  (CURRENT_DATE - INTERVAL '12 days') + TIME '14:15', CURRENT_TIMESTAMP, u.id
FROM patients p, triage_codes tc, rooms rm, users u
WHERE p.first_name = 'Maria' AND p.last_name = 'Santos López'
  AND tc.code = 'A' AND rm.number = '101' AND u.username = 'doctor4';

-- Pedro Garcia - Alcohol withdrawal severe - 10 days
INSERT INTO admissions (patient_id, triage_code_id, room_id, treating_physician_id, admission_date, status, type, created_at, updated_at, created_by)
SELECT p.id, tc.id, rm.id, u.id,
  (CURRENT_DATE - INTERVAL '10 days') + TIME '22:45',
  'ACTIVE', 'HOSPITALIZATION',
  (CURRENT_DATE - INTERVAL '10 days') + TIME '22:45', CURRENT_TIMESTAMP, u.id
FROM patients p, triage_codes tc, rooms rm, users u
WHERE p.first_name = 'Pedro' AND p.last_name = 'García Hernández'
  AND tc.code = 'B' AND rm.number = '303' AND u.username = 'doctor3';

-- Ana Martinez - Schizophrenia paranoid - 8 days
INSERT INTO admissions (patient_id, triage_code_id, room_id, treating_physician_id, admission_date, status, type, created_at, updated_at, created_by)
SELECT p.id, tc.id, rm.id, u.id,
  (CURRENT_DATE - INTERVAL '8 days') + TIME '09:00',
  'ACTIVE', 'HOSPITALIZATION',
  (CURRENT_DATE - INTERVAL '8 days') + TIME '09:00', CURRENT_TIMESTAMP, u.id
FROM patients p, triage_codes tc, rooms rm, users u
WHERE p.first_name = 'Ana' AND p.last_name = 'Martínez Ruiz'
  AND tc.code = 'C' AND rm.number = '102' AND u.username = 'doctor5';

-- Luis Morales - Polysubstance + psychosis - 7 days
INSERT INTO admissions (patient_id, triage_code_id, room_id, treating_physician_id, admission_date, status, type, created_at, updated_at, created_by)
SELECT p.id, tc.id, rm.id, u.id,
  (CURRENT_DATE - INTERVAL '7 days') + TIME '03:20',
  'ACTIVE', 'HOSPITALIZATION',
  (CURRENT_DATE - INTERVAL '7 days') + TIME '03:20', CURRENT_TIMESTAMP, u.id
FROM patients p, triage_codes tc, rooms rm, users u
WHERE p.first_name = 'Luis' AND p.last_name = 'Morales Castro'
  AND tc.code = 'B' AND rm.number = '202' AND u.username = 'doctor3';

-- Carmen Flores - PTSD + suicidal ideation - 5 days
INSERT INTO admissions (patient_id, triage_code_id, room_id, treating_physician_id, admission_date, status, type, created_at, updated_at, created_by)
SELECT p.id, tc.id, rm.id, u.id,
  (CURRENT_DATE - INTERVAL '5 days') + TIME '16:00',
  'ACTIVE', 'HOSPITALIZATION',
  (CURRENT_DATE - INTERVAL '5 days') + TIME '16:00', CURRENT_TIMESTAMP, u.id
FROM patients p, triage_codes tc, rooms rm, users u
WHERE p.first_name = 'Carmen' AND p.last_name = 'Flores Mejía'
  AND tc.code = 'B' AND rm.number = '103' AND u.username = 'doctor4';

-- Roberto Diaz - Bipolar II depressive - 4 days
INSERT INTO admissions (patient_id, triage_code_id, room_id, treating_physician_id, admission_date, status, type, created_at, updated_at, created_by)
SELECT p.id, tc.id, rm.id, u.id,
  (CURRENT_DATE - INTERVAL '4 days') + TIME '11:30',
  'ACTIVE', 'HOSPITALIZATION',
  (CURRENT_DATE - INTERVAL '4 days') + TIME '11:30', CURRENT_TIMESTAMP, u.id
FROM patients p, triage_codes tc, rooms rm, users u
WHERE p.first_name = 'Roberto' AND p.last_name = 'Díaz Vargas'
  AND tc.code = 'C' AND rm.number = '203' AND u.username = 'doctor5';

-- Sofia Ramirez - BPD + self-harm - 3 days
INSERT INTO admissions (patient_id, triage_code_id, room_id, treating_physician_id, admission_date, status, type, created_at, updated_at, created_by)
SELECT p.id, tc.id, rm.id, u.id,
  (CURRENT_DATE - INTERVAL '3 days') + TIME '19:45',
  'ACTIVE', 'HOSPITALIZATION',
  (CURRENT_DATE - INTERVAL '3 days') + TIME '19:45', CURRENT_TIMESTAMP, u.id
FROM patients p, triage_codes tc, rooms rm, users u
WHERE p.first_name = 'Sofia' AND p.last_name = 'Ramírez Paz'
  AND tc.code = 'B' AND rm.number = '109' AND u.username = 'doctor4';

-- === Discharged Hospitalizations (3) ===

-- Miguel Torres - Schizophrenia - admitted 30d ago, discharged 16d ago
INSERT INTO admissions (patient_id, triage_code_id, room_id, treating_physician_id, admission_date, discharge_date, status, type, created_at, updated_at, created_by)
SELECT p.id, tc.id, rm.id, u.id,
  (CURRENT_DATE - INTERVAL '30 days') + TIME '11:00',
  (CURRENT_DATE - INTERVAL '16 days') + TIME '14:00',
  'DISCHARGED', 'HOSPITALIZATION',
  (CURRENT_DATE - INTERVAL '30 days') + TIME '11:00',
  (CURRENT_DATE - INTERVAL '16 days') + TIME '14:00', u.id
FROM patients p, triage_codes tc, rooms rm, users u
WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna'
  AND tc.code = 'B' AND rm.number = '301' AND u.username = 'doctor5';

-- Elena Sanchez - GAD + Panic - admitted 25d ago, discharged 14d ago
INSERT INTO admissions (patient_id, triage_code_id, room_id, treating_physician_id, admission_date, discharge_date, status, type, created_at, updated_at, created_by)
SELECT p.id, tc.id, rm.id, u.id,
  (CURRENT_DATE - INTERVAL '25 days') + TIME '08:30',
  (CURRENT_DATE - INTERVAL '14 days') + TIME '11:00',
  'DISCHARGED', 'HOSPITALIZATION',
  (CURRENT_DATE - INTERVAL '25 days') + TIME '08:30',
  (CURRENT_DATE - INTERVAL '14 days') + TIME '11:00', u.id
FROM patients p, triage_codes tc, rooms rm, users u
WHERE p.first_name = 'Elena' AND p.last_name = 'Sánchez Rivas'
  AND tc.code = 'C' AND rm.number = '104' AND u.username = 'doctor4';

-- Francisco Mendoza - MDD recurrent geriatric - admitted 21d ago, discharged 10d ago
INSERT INTO admissions (patient_id, triage_code_id, room_id, treating_physician_id, admission_date, discharge_date, status, type, created_at, updated_at, created_by)
SELECT p.id, tc.id, rm.id, u.id,
  (CURRENT_DATE - INTERVAL '21 days') + TIME '10:00',
  (CURRENT_DATE - INTERVAL '10 days') + TIME '15:30',
  'DISCHARGED', 'HOSPITALIZATION',
  (CURRENT_DATE - INTERVAL '21 days') + TIME '10:00',
  (CURRENT_DATE - INTERVAL '10 days') + TIME '15:30', u.id
FROM patients p, triage_codes tc, rooms rm, users u
WHERE p.first_name = 'Francisco' AND p.last_name = 'Mendoza Aguilar'
  AND tc.code = 'C' AND rm.number = '302' AND u.username = 'doctor6';

-- === Non-Hospitalization Admissions (5) ===

-- Isabella Cruz - AMBULATORY
INSERT INTO admissions (patient_id, treating_physician_id, admission_date, status, type, created_at, updated_at, created_by)
SELECT p.id, u.id,
  (CURRENT_DATE - INTERVAL '5 days') + TIME '08:00',
  'ACTIVE', 'AMBULATORY',
  (CURRENT_DATE - INTERVAL '5 days') + TIME '08:00', CURRENT_TIMESTAMP, u.id
FROM patients p, users u
WHERE p.first_name = 'Isabella' AND p.last_name = 'Cruz Monzón' AND u.username = 'doctor7';

-- Andres Ortiz - ELECTROSHOCK_THERAPY
INSERT INTO admissions (patient_id, treating_physician_id, admission_date, status, type, created_at, updated_at, created_by)
SELECT p.id, u.id,
  (CURRENT_DATE - INTERVAL '3 days') + TIME '07:00',
  'ACTIVE', 'ELECTROSHOCK_THERAPY',
  (CURRENT_DATE - INTERVAL '3 days') + TIME '07:00', CURRENT_TIMESTAMP, u.id
FROM patients p, users u
WHERE p.first_name = 'Andres' AND p.last_name = 'Ortiz Barrios' AND u.username = 'doctor9';

-- Gabriela Reyes - KETAMINE_INFUSIÓN
INSERT INTO admissions (patient_id, treating_physician_id, admission_date, status, type, created_at, updated_at, created_by)
SELECT p.id, u.id,
  (CURRENT_DATE - INTERVAL '2 days') + TIME '09:00',
  'ACTIVE', 'KETAMINE_INFUSIÓN',
  (CURRENT_DATE - INTERVAL '2 days') + TIME '09:00', CURRENT_TIMESTAMP, u.id
FROM patients p, users u
WHERE p.first_name = 'Gabriela' AND p.last_name = 'Reyes Soto' AND u.username = 'doctor9';

-- Oscar Vasquez - EMERGENCY
INSERT INTO admissions (patient_id, treating_physician_id, admission_date, status, type, created_at, updated_at, created_by)
SELECT p.id, u.id,
  (CURRENT_DATE - INTERVAL '1 day') + TIME '02:30',
  'ACTIVE', 'EMERGENCY',
  (CURRENT_DATE - INTERVAL '1 day') + TIME '02:30', CURRENT_TIMESTAMP, u.id
FROM patients p, users u
WHERE p.first_name = 'Oscar' AND p.last_name = 'Vásquez Pineda' AND u.username = 'doctor7';

-- Diego Castillo - AMBULATORY
INSERT INTO admissions (patient_id, treating_physician_id, admission_date, status, type, created_at, updated_at, created_by)
SELECT p.id, u.id,
  (CURRENT_DATE - INTERVAL '7 days') + TIME '10:00',
  'ACTIVE', 'AMBULATORY',
  (CURRENT_DATE - INTERVAL '7 days') + TIME '10:00', CURRENT_TIMESTAMP, u.id
FROM patients p, users u
WHERE p.first_name = 'Diego' AND p.last_name = 'Castillo Moreno' AND u.username = 'doctor2';

-- ============================================================================
-- STEP 16: ADD CONSULTING PHYSICIANS
-- ============================================================================
-- Juan Perez
INSERT INTO admission_consulting_physicians (admission_id, physician_id, reason, requested_date, created_at, updated_at)
SELECT a.id, u.id, 'Evaluación por riesgo suicida y manejo de medicación', a.admission_date::DATE, a.admission_date, a.admission_date
FROM admissions a JOIN patients p ON a.patient_id = p.id, users u
WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González' AND a.status = 'ACTIVE' AND a.type = 'HOSPITALIZATION' AND u.username = 'doctor3';

INSERT INTO admission_consulting_physicians (admission_id, physician_id, reason, requested_date, created_at, updated_at)
SELECT a.id, u.id, 'Evaluación médica general y manejo de efectos secundarios', a.admission_date::DATE + 1, a.admission_date, a.admission_date
FROM admissions a JOIN patients p ON a.patient_id = p.id, users u
WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González' AND a.status = 'ACTIVE' AND a.type = 'HOSPITALIZATION' AND u.username = 'doctor6';

-- Maria Santos
INSERT INTO admission_consulting_physicians (admission_id, physician_id, reason, requested_date, created_at, updated_at)
SELECT a.id, u.id, 'Manejo de episodio maníaco con psicosis', a.admission_date::DATE, a.admission_date, a.admission_date
FROM admissions a JOIN patients p ON a.patient_id = p.id, users u
WHERE p.first_name = 'Maria' AND p.last_name = 'Santos López' AND a.status = 'ACTIVE' AND a.type = 'HOSPITALIZATION' AND u.username = 'doctor5';

INSERT INTO admission_consulting_physicians (admission_id, physician_id, reason, requested_date, created_at, updated_at)
SELECT a.id, u.id, 'Segunda opinión sobre estabilización farmacológica', a.admission_date::DATE + 2, a.admission_date, a.admission_date
FROM admissions a JOIN patients p ON a.patient_id = p.id, users u
WHERE p.first_name = 'Maria' AND p.last_name = 'Santos López' AND a.status = 'ACTIVE' AND a.type = 'HOSPITALIZATION' AND u.username = 'doctor8';

-- Pedro Garcia
INSERT INTO admission_consulting_physicians (admission_id, physician_id, reason, requested_date, created_at, updated_at)
SELECT a.id, u.id, 'Manejo de síndrome de abstinencia alcohólica y función hepática', a.admission_date::DATE, a.admission_date, a.admission_date
FROM admissions a JOIN patients p ON a.patient_id = p.id, users u
WHERE p.first_name = 'Pedro' AND p.last_name = 'García Hernández' AND a.status = 'ACTIVE' AND a.type = 'HOSPITALIZATION' AND u.username = 'doctor5';

INSERT INTO admission_consulting_physicians (admission_id, physician_id, reason, requested_date, created_at, updated_at)
SELECT a.id, u.id, 'Evaluación hepática y control de DM2', a.admission_date::DATE + 1, a.admission_date, a.admission_date
FROM admissions a JOIN patients p ON a.patient_id = p.id, users u
WHERE p.first_name = 'Pedro' AND p.last_name = 'García Hernández' AND a.status = 'ACTIVE' AND a.type = 'HOSPITALIZATION' AND u.username = 'doctor6';

INSERT INTO admission_consulting_physicians (admission_id, physician_id, reason, requested_date, created_at, updated_at)
SELECT a.id, u.id, 'Evaluación de uso de sustancias y plan de desintoxicación', a.admission_date::DATE, a.admission_date, a.admission_date
FROM admissions a JOIN patients p ON a.patient_id = p.id, users u
WHERE p.first_name = 'Pedro' AND p.last_name = 'García Hernández' AND a.status = 'ACTIVE' AND a.type = 'HOSPITALIZATION' AND u.username = 'doctor8';

-- Ana Martinez
INSERT INTO admission_consulting_physicians (admission_id, physician_id, reason, requested_date, created_at, updated_at)
SELECT a.id, u.id, 'Ajuste de medicación antipsicótica', a.admission_date::DATE + 1, a.admission_date, a.admission_date
FROM admissions a JOIN patients p ON a.patient_id = p.id, users u
WHERE p.first_name = 'Ana' AND p.last_name = 'Martínez Ruiz' AND a.status = 'ACTIVE' AND a.type = 'HOSPITALIZATION' AND u.username = 'doctor3';

INSERT INTO admission_consulting_physicians (admission_id, physician_id, reason, requested_date, created_at, updated_at)
SELECT a.id, u.id, 'Evaluación cardiológica por QTc prolongado', a.admission_date::DATE + 2, a.admission_date, a.admission_date
FROM admissions a JOIN patients p ON a.patient_id = p.id, users u
WHERE p.first_name = 'Ana' AND p.last_name = 'Martínez Ruiz' AND a.status = 'ACTIVE' AND a.type = 'HOSPITALIZATION' AND u.username = 'doctor4';

-- Luis Morales
INSERT INTO admission_consulting_physicians (admission_id, physician_id, reason, requested_date, created_at, updated_at)
SELECT a.id, u.id, 'Manejo de psicosis inducida por sustancias', a.admission_date::DATE, a.admission_date, a.admission_date
FROM admissions a JOIN patients p ON a.patient_id = p.id, users u
WHERE p.first_name = 'Luis' AND p.last_name = 'Morales Castro' AND a.status = 'ACTIVE' AND a.type = 'HOSPITALIZATION' AND u.username = 'doctor4';

INSERT INTO admission_consulting_physicians (admission_id, physician_id, reason, requested_date, created_at, updated_at)
SELECT a.id, u.id, 'Programa de desintoxicación', a.admission_date::DATE + 1, a.admission_date, a.admission_date
FROM admissions a JOIN patients p ON a.patient_id = p.id, users u
WHERE p.first_name = 'Luis' AND p.last_name = 'Morales Castro' AND a.status = 'ACTIVE' AND a.type = 'HOSPITALIZATION' AND u.username = 'doctor5';

-- Carmen Flores
INSERT INTO admission_consulting_physicians (admission_id, physician_id, reason, requested_date, created_at, updated_at)
SELECT a.id, u.id, 'Evaluación de trauma y riesgo suicida', a.admission_date::DATE, a.admission_date, a.admission_date
FROM admissions a JOIN patients p ON a.patient_id = p.id, users u
WHERE p.first_name = 'Carmen' AND p.last_name = 'Flores Mejía' AND a.status = 'ACTIVE' AND a.type = 'HOSPITALIZATION' AND u.username = 'doctor3';

INSERT INTO admission_consulting_physicians (admission_id, physician_id, reason, requested_date, created_at, updated_at)
SELECT a.id, u.id, 'Manejo farmacológico de TEPT', a.admission_date::DATE + 1, a.admission_date, a.admission_date
FROM admissions a JOIN patients p ON a.patient_id = p.id, users u
WHERE p.first_name = 'Carmen' AND p.last_name = 'Flores Mejía' AND a.status = 'ACTIVE' AND a.type = 'HOSPITALIZATION' AND u.username = 'doctor6';

-- Roberto Diaz
INSERT INTO admission_consulting_physicians (admission_id, physician_id, reason, requested_date, created_at, updated_at)
SELECT a.id, u.id, 'Evaluación cardiológica y ajuste de medicación', a.admission_date::DATE, a.admission_date, a.admission_date
FROM admissions a JOIN patients p ON a.patient_id = p.id, users u
WHERE p.first_name = 'Roberto' AND p.last_name = 'Díaz Vargas' AND a.status = 'ACTIVE' AND a.type = 'HOSPITALIZATION' AND u.username = 'doctor4';

INSERT INTO admission_consulting_physicians (admission_id, physician_id, reason, requested_date, created_at, updated_at)
SELECT a.id, u.id, 'Manejo de fase depresiva bipolar', a.admission_date::DATE + 1, a.admission_date, a.admission_date
FROM admissions a JOIN patients p ON a.patient_id = p.id, users u
WHERE p.first_name = 'Roberto' AND p.last_name = 'Díaz Vargas' AND a.status = 'ACTIVE' AND a.type = 'HOSPITALIZATION' AND u.username = 'doctor3';

-- Sofia Ramirez
INSERT INTO admission_consulting_physicians (admission_id, physician_id, reason, requested_date, created_at, updated_at)
SELECT a.id, u.id, 'Evaluación de autolesiónes y plan de seguridad', a.admission_date::DATE, a.admission_date, a.admission_date
FROM admissions a JOIN patients p ON a.patient_id = p.id, users u
WHERE p.first_name = 'Sofia' AND p.last_name = 'Ramírez Paz' AND a.status = 'ACTIVE' AND a.type = 'HOSPITALIZATION' AND u.username = 'doctor3';

INSERT INTO admission_consulting_physicians (admission_id, physician_id, reason, requested_date, created_at, updated_at)
SELECT a.id, u.id, 'Terapia dialéctica conductual', a.admission_date::DATE + 1, a.admission_date, a.admission_date
FROM admissions a JOIN patients p ON a.patient_id = p.id, users u
WHERE p.first_name = 'Sofia' AND p.last_name = 'Ramírez Paz' AND a.status = 'ACTIVE' AND a.type = 'HOSPITALIZATION' AND u.username = 'doctor6';

-- Miguel Torres (discharged)
INSERT INTO admission_consulting_physicians (admission_id, physician_id, reason, requested_date, created_at, updated_at)
SELECT a.id, u.id, 'Ajuste de antipsicóticos atípicos', a.admission_date::DATE + 1, a.admission_date, a.admission_date
FROM admissions a JOIN patients p ON a.patient_id = p.id, users u
WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna' AND a.status = 'DISCHARGED' AND u.username = 'doctor3';

INSERT INTO admission_consulting_physicians (admission_id, physician_id, reason, requested_date, created_at, updated_at)
SELECT a.id, u.id, 'Evaluación de síntomas negativos', a.admission_date::DATE + 3, a.admission_date, a.admission_date
FROM admissions a JOIN patients p ON a.patient_id = p.id, users u
WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna' AND a.status = 'DISCHARGED' AND u.username = 'doctor4';

-- Elena Sanchez (discharged)
INSERT INTO admission_consulting_physicians (admission_id, physician_id, reason, requested_date, created_at, updated_at)
SELECT a.id, u.id, 'Manejo de trastorno de pánico', a.admission_date::DATE + 1, a.admission_date, a.admission_date
FROM admissions a JOIN patients p ON a.patient_id = p.id, users u
WHERE p.first_name = 'Elena' AND p.last_name = 'Sánchez Rivas' AND a.status = 'DISCHARGED' AND u.username = 'doctor5';

INSERT INTO admission_consulting_physicians (admission_id, physician_id, reason, requested_date, created_at, updated_at)
SELECT a.id, u.id, 'Terapia cognitivo-conductual para ansiedad', a.admission_date::DATE + 2, a.admission_date, a.admission_date
FROM admissions a JOIN patients p ON a.patient_id = p.id, users u
WHERE p.first_name = 'Elena' AND p.last_name = 'Sánchez Rivas' AND a.status = 'DISCHARGED' AND u.username = 'doctor6';

-- Francisco Mendoza (discharged)
INSERT INTO admission_consulting_physicians (admission_id, physician_id, reason, requested_date, created_at, updated_at)
SELECT a.id, u.id, 'Evaluación geriátrica y manejo de depresión', a.admission_date::DATE + 1, a.admission_date, a.admission_date
FROM admissions a JOIN patients p ON a.patient_id = p.id, users u
WHERE p.first_name = 'Francisco' AND p.last_name = 'Mendoza Aguilar' AND a.status = 'DISCHARGED' AND u.username = 'doctor3';

INSERT INTO admission_consulting_physicians (admission_id, physician_id, reason, requested_date, created_at, updated_at)
SELECT a.id, u.id, 'Manejo de dolor articular y funciónalidad', a.admission_date::DATE + 2, a.admission_date, a.admission_date
FROM admissions a JOIN patients p ON a.patient_id = p.id, users u
WHERE p.first_name = 'Francisco' AND p.last_name = 'Mendoza Aguilar' AND a.status = 'DISCHARGED' AND u.username = 'doctor4';

INSERT INTO admission_consulting_physicians (admission_id, physician_id, reason, requested_date, created_at, updated_at)
SELECT a.id, u.id, 'Ajuste de antidepresivo en adulto mayor', a.admission_date::DATE + 3, a.admission_date, a.admission_date
FROM admissions a JOIN patients p ON a.patient_id = p.id, users u
WHERE p.first_name = 'Francisco' AND p.last_name = 'Mendoza Aguilar' AND a.status = 'DISCHARGED' AND u.username = 'doctor5';


SET session_replication_role = DEFAULT;

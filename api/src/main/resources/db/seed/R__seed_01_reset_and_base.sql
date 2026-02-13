-- ============================================================================
-- SEED FILE 01: Reset, Roles, Users, Patients, Emergency Contacts
-- ============================================================================
-- This file truncates all tables and reseeds base data.
-- WARNING: DESTRUCTIVE - DO NOT use in production!
-- Default password for all users: admin123
-- ============================================================================

SET session_replication_role = replica;

-- ============================================================================
-- STEP 1: TRUNCATE TABLES (in dependency order - children first)
-- ============================================================================
TRUNCATE TABLE medication_administrations CASCADE;
TRUNCATE TABLE patient_charges CASCADE;
TRUNCATE TABLE invoices CASCADE;
TRUNCATE TABLE vital_signs CASCADE;
TRUNCATE TABLE nursing_notes CASCADE;
TRUNCATE TABLE psychotherapy_activities CASCADE;
TRUNCATE TABLE psychotherapy_categories CASCADE;
TRUNCATE TABLE medical_orders CASCADE;
TRUNCATE TABLE progress_notes CASCADE;
TRUNCATE TABLE clinical_histories CASCADE;
TRUNCATE TABLE admission_consulting_physicians CASCADE;
TRUNCATE TABLE admission_consent_documents CASCADE;
TRUNCATE TABLE admissions CASCADE;
TRUNCATE TABLE rooms CASCADE;
TRUNCATE TABLE triage_codes CASCADE;
TRUNCATE TABLE patient_id_documents CASCADE;
TRUNCATE TABLE emergency_contacts CASCADE;
TRUNCATE TABLE patients CASCADE;
TRUNCATE TABLE user_phone_numbers CASCADE;
TRUNCATE TABLE password_reset_tokens CASCADE;
TRUNCATE TABLE refresh_tokens CASCADE;
TRUNCATE TABLE audit_logs CASCADE;
TRUNCATE TABLE user_roles CASCADE;
TRUNCATE TABLE role_permissions CASCADE;
TRUNCATE TABLE inventory_movements CASCADE;
TRUNCATE TABLE inventory_items CASCADE;
TRUNCATE TABLE inventory_categories CASCADE;
TRUNCATE TABLE users CASCADE;
-- Note: We don't truncate roles and permissions - those are managed by versioned migrations

-- ============================================================================
-- STEP 2: ENSURE ALL REQUIRED ROLES EXIST
-- ============================================================================
-- Add PSYCHOLOGIST role if it doesn't exist
INSERT INTO roles (code, name, description, is_system, created_at, updated_at)
VALUES ('PSYCHOLOGIST', 'Psychologist', 'Mental health professionals', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- ============================================================================
-- STEP 3: REBUILD ROLE PERMISSIONS (ADMIN gets all permissions)
-- ============================================================================
-- Clear existing role_permissions
DELETE FROM role_permissions;

-- ADMIN gets all permissions
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMIN' AND r.deleted_at IS NULL AND p.deleted_at IS NULL;

-- USER gets mínimal permissions
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'USER' AND p.code IN ('user:read') AND r.deleted_at IS NULL AND p.deleted_at IS NULL;

-- ADMINISTRATIVE_STAFF gets patient management permissions
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMINISTRATIVE_STAFF'
  AND p.code IN ('patient:create', 'patient:read', 'patient:update', 'patient:list', 'admission:create', 'admission:read', 'admission:update', 'admission:list')
  AND r.deleted_at IS NULL AND p.deleted_at IS NULL;

-- DOCTOR gets clínical permissions
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'DOCTOR'
  AND p.code IN ('patient:read', 'patient:update', 'patient:list', 'admission:read', 'admission:update', 'admission:list', 'admission:discharge')
  AND r.deleted_at IS NULL AND p.deleted_at IS NULL;

-- PSYCHOLOGIST gets similar permissions to DOCTOR
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'PSYCHOLOGIST'
  AND p.code IN ('patient:read', 'patient:update', 'patient:list', 'admission:read', 'admission:list')
  AND r.deleted_at IS NULL AND p.deleted_at IS NULL;

-- NURSE gets patient care permissions
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'NURSE'
  AND p.code IN ('patient:read', 'patient:list', 'admission:read', 'admission:list')
  AND r.deleted_at IS NULL AND p.deleted_at IS NULL;

-- CHIEF_NURSE gets extended nursing permissions
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'CHIEF_NURSE'
  AND p.code IN ('patient:read', 'patient:update', 'patient:list', 'admission:read', 'admission:update', 'admission:list')
  AND r.deleted_at IS NULL AND p.deleted_at IS NULL;

-- ============================================================================
-- STEP 4: CREATE USERS
-- ============================================================================
-- Password for all test users: admin123 (BCrypt hash)
-- $2b$10$PYXULrV.BlNnIPSz8HRFJeId5axQ/qoAQNhEldlY/H7xlqIpH35YC

-- Admin User (required)
INSERT INTO users (username, email, password_hash, first_name, last_name, salutation, status, email_verified, must_change_password, created_at, updated_at)
VALUES ('admin', 'admin@example.com', '$2b$10$PYXULrV.BlNnIPSz8HRFJeId5axQ/qoAQNhEldlY/H7xlqIpH35YC', 'System', 'Administrator', NULL, 'ACTIVE', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- USER role users (2)
INSERT INTO users (username, email, password_hash, first_name, last_name, salutation, status, email_verified, must_change_password, created_at, updated_at) VALUES
('user1', 'user1@example.com', '$2b$10$PYXULrV.BlNnIPSz8HRFJeId5axQ/qoAQNhEldlY/H7xlqIpH35YC', 'Carlos', 'Martinez', 'SR', 'ACTIVE', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('user2', 'user2@example.com', '$2b$10$PYXULrV.BlNnIPSz8HRFJeId5axQ/qoAQNhEldlY/H7xlqIpH35YC', 'Ana', 'Lopez', 'SRA', 'ACTIVE', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ADMINISTRATIVE_STAFF role users (2)
INSERT INTO users (username, email, password_hash, first_name, last_name, salutation, status, email_verified, must_change_password, created_at, updated_at) VALUES
('staff1', 'staff1@example.com', '$2b$10$PYXULrV.BlNnIPSz8HRFJeId5axQ/qoAQNhEldlY/H7xlqIpH35YC', 'Maria', 'Garcia', 'SRTA', 'ACTIVE', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('staff2', 'staff2@example.com', '$2b$10$PYXULrV.BlNnIPSz8HRFJeId5axQ/qoAQNhEldlY/H7xlqIpH35YC', 'Jose', 'Rodriguez', 'SR', 'ACTIVE', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- DOCTOR role users (2)
INSERT INTO users (username, email, password_hash, first_name, last_name, salutation, status, email_verified, must_change_password, created_at, updated_at) VALUES
('doctor1', 'doctor1@example.com', '$2b$10$PYXULrV.BlNnIPSz8HRFJeId5axQ/qoAQNhEldlY/H7xlqIpH35YC', 'Roberto', 'Hernandez', 'DR', 'ACTIVE', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('doctor2', 'doctor2@example.com', '$2b$10$PYXULrV.BlNnIPSz8HRFJeId5axQ/qoAQNhEldlY/H7xlqIpH35YC', 'Patricia', 'Morales', 'DRA', 'ACTIVE', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- PSYCHOLOGIST role users (2)
INSERT INTO users (username, email, password_hash, first_name, last_name, salutation, status, email_verified, must_change_password, created_at, updated_at) VALUES
('psych1', 'psych1@example.com', '$2b$10$PYXULrV.BlNnIPSz8HRFJeId5axQ/qoAQNhEldlY/H7xlqIpH35YC', 'Sofia', 'Ramirez', 'DRA', 'ACTIVE', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('psych2', 'psych2@example.com', '$2b$10$PYXULrV.BlNnIPSz8HRFJeId5axQ/qoAQNhEldlY/H7xlqIpH35YC', 'Miguel', 'Torres', 'DR', 'ACTIVE', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- NURSE role users (2)
INSERT INTO users (username, email, password_hash, first_name, last_name, salutation, status, email_verified, must_change_password, created_at, updated_at) VALUES
('nurse1', 'nurse1@example.com', '$2b$10$PYXULrV.BlNnIPSz8HRFJeId5axQ/qoAQNhEldlY/H7xlqIpH35YC', 'Laura', 'Sanchez', 'SRTA', 'ACTIVE', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('nurse2', 'nurse2@example.com', '$2b$10$PYXULrV.BlNnIPSz8HRFJeId5axQ/qoAQNhEldlY/H7xlqIpH35YC', 'Fernando', 'Diaz', 'SR', 'ACTIVE', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- CHIEF_NURSE role users (2)
INSERT INTO users (username, email, password_hash, first_name, last_name, salutation, status, email_verified, must_change_password, created_at, updated_at) VALUES
('chiefnurse1', 'chiefnurse1@example.com', '$2b$10$PYXULrV.BlNnIPSz8HRFJeId5axQ/qoAQNhEldlY/H7xlqIpH35YC', 'Carmen', 'Flores', 'SRA', 'ACTIVE', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('chiefnurse2', 'chiefnurse2@example.com', '$2b$10$PYXULrV.BlNnIPSz8HRFJeId5axQ/qoAQNhEldlY/H7xlqIpH35YC', 'Ricardo', 'Mendoza', 'SR', 'ACTIVE', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ============================================================================
-- STEP 5: ASSIGN ROLES TO USERS
-- ============================================================================
-- Admin gets ADMIN role
INSERT INTO user_roles (user_id, role_id, created_at, updated_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u, roles r
WHERE u.username = 'admin' AND r.code = 'ADMIN';

-- USER role assignments
INSERT INTO user_roles (user_id, role_id, created_at, updated_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u, roles r
WHERE u.username IN ('user1', 'user2') AND r.code = 'USER';

-- ADMINISTRATIVE_STAFF role assignments
INSERT INTO user_roles (user_id, role_id, created_at, updated_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u, roles r
WHERE u.username IN ('staff1', 'staff2') AND r.code = 'ADMINISTRATIVE_STAFF';

-- DOCTOR role assignments
INSERT INTO user_roles (user_id, role_id, created_at, updated_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u, roles r
WHERE u.username IN ('doctor1', 'doctor2') AND r.code = 'DOCTOR';

-- PSYCHOLOGIST role assignments
INSERT INTO user_roles (user_id, role_id, created_at, updated_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u, roles r
WHERE u.username IN ('psych1', 'psych2') AND r.code = 'PSYCHOLOGIST';

-- NURSE role assignments
INSERT INTO user_roles (user_id, role_id, created_at, updated_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u, roles r
WHERE u.username IN ('nurse1', 'nurse2') AND r.code = 'NURSE';

-- CHIEF_NURSE role assignments
INSERT INTO user_roles (user_id, role_id, created_at, updated_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u, roles r
WHERE u.username IN ('chiefnurse1', 'chiefnurse2') AND r.code = 'CHIEF_NURSE';

-- ============================================================================
-- STEP 6: CREATE TEST PATIENTS (20 patients)
-- ============================================================================
INSERT INTO patients (first_name, last_name, age, sex, gender, marital_status, religion, education_level, occupation, address, email, id_document_number, notes, created_at, updated_at) VALUES
-- Patient 1-5
('Juan', 'Pérez González', 35, 'MALE', 'Male', 'MARRIED', 'Catholic', 'UNIVERSITY', 'Engineer', 'Zone 10, Guatemala City', 'juan.perez@email.com', 'DPI-1234567890101', 'Regular checkup patient', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Maria', 'Santos López', 28, 'FEMALE', 'Female', 'SINGLE', 'Evangelical', 'TECHNICAL', 'Accountant', 'Zone 1, Guatemala City', 'maria.santos@email.com', 'DPI-2345678901202', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Pedro', 'García Hernández', 45, 'MALE', 'Male', 'DIVORCED', 'Catholic', 'SECONDARY', 'Mechanic', 'Zone 7, Guatemala City', 'pedro.garcia@email.com', 'DPI-3456789012303', 'Diabetic patient, requires special attention', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Ana', 'Martínez Ruiz', 62, 'FEMALE', 'Female', 'WIDOWED', 'Catholic', 'PRIMARY', 'Retired Teacher', 'Zone 12, Guatemala City', 'ana.martinez@email.com', 'DPI-4567890123404', 'Hypertensión history', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Luis', 'Morales Castro', 22, 'MALE', 'Male', 'SINGLE', 'None', 'UNIVERSITY', 'Student', 'Zone 14, Guatemala City', 'luis.morales@email.com', 'DPI-5678901234505', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Patient 6-10
('Carmen', 'Flores Mejía', 38, 'FEMALE', 'Female', 'MARRIED', 'Catholic', 'POSTGRADUATE', 'Lawyer', 'Zone 15, Guatemala City', 'carmen.flores@email.com', 'DPI-6789012345606', 'Allergic to penicillin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Roberto', 'Díaz Vargas', 55, 'MALE', 'Male', 'MARRIED', 'Evangelical', 'TECHNICAL', 'Electrician', 'Mixco, Guatemala', 'roberto.diaz@email.com', 'DPI-7890123456707', 'Heart condition, on medication', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Sofia', 'Ramírez Paz', 31, 'FEMALE', 'Female', 'SEPARATED', 'Catholic', 'UNIVERSITY', 'Nurse', 'Villa Nueva, Guatemala', 'sofia.ramirez@email.com', 'DPI-8901234567808', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Miguel', 'Torres Luna', 48, 'MALE', 'Male', 'MARRIED', 'Catholic', 'SECONDARY', 'Driver', 'Amatitlán, Guatemala', 'miguel.torres@email.com', 'DPI-9012345678909', 'Back pain issues', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Elena', 'Sánchez Rivas', 26, 'FEMALE', 'Female', 'SINGLE', 'Evangelical', 'UNIVERSITY', 'Marketing Specialist', 'Zone 4, Guatemala City', 'elena.sanchez@email.com', 'DPI-0123456789010', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Patient 11-15
('Francisco', 'Mendoza Aguilar', 70, 'MALE', 'Male', 'WIDOWED', 'Catholic', 'PRIMARY', 'Retired Farmer', 'Antigua Guatemala, Sacatepéquez', 'francisco.mendoza@email.com', 'DPI-1234509876111', 'Arthritis, limited mobility', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Isabella', 'Cruz Monzón', 19, 'FEMALE', 'Female', 'SINGLE', 'Catholic', 'SECONDARY', 'Student', 'Quetzaltenango', 'isabella.cruz@email.com', 'DPI-2345610987212', 'First visit', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Andres', 'Ortiz Barrios', 42, 'MALE', 'Male', 'MARRIED', 'Evangelical', 'TECHNICAL', 'Carpenter', 'Escuintla', 'andres.ortiz@email.com', 'DPI-3456721098313', 'Occupational injury follow-up', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Gabriela', 'Reyes Soto', 33, 'FEMALE', 'Female', 'MARRIED', 'Catholic', 'POSTGRADUATE', 'Doctor', 'Zone 9, Guatemala City', 'gabriela.reyes@email.com', 'DPI-4567832109414', 'Pregnancy monitoring', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Oscar', 'Vásquez Pineda', 58, 'MALE', 'Male', 'DIVORCED', 'None', 'UNIVERSITY', 'Businessman', 'Zone 16, Guatemala City', 'oscar.vasquez@email.com', 'DPI-5678943210515', 'Annual executive checkup', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Patient 16-20
('Patricia', 'Herrera Godínez', 44, 'FEMALE', 'Female', 'MARRIED', 'Catholic', 'TECHNICAL', 'Secretary', 'Zone 5, Guatemala City', 'patricia.herrera@email.com', 'DPI-6789054321616', 'Chronic migraines', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Diego', 'Castillo Moreno', 29, 'MALE', 'Male', 'SINGLE', 'Evangelical', 'UNIVERSITY', 'Software Developer', 'Zone 11, Guatemala City', 'diego.castillo@email.com', 'DPI-7890165432717', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Valentina', 'Estrada Juárez', 52, 'FEMALE', 'Female', 'MARRIED', 'Catholic', 'SECONDARY', 'Cook', 'San Juan Sacatepéquez', 'valentina.estrada@email.com', 'DPI-8901276543818', 'Gastritis, dietary restrictions', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Alejandro', 'Núñez Córdova', 36, 'MALE', 'Male', 'MARRIED', 'Catholic', 'UNIVERSITY', 'Architect', 'Zone 13, Guatemala City', 'alejandro.nunez@email.com', 'DPI-9012387654919', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Lucia', 'Álvarez Monroy', 24, 'FEMALE', 'Female', 'SINGLE', 'Evangelical', 'TECHNICAL', 'Dental Assistant', 'Zone 18, Guatemala City', 'lucia.alvarez@email.com', 'DPI-0123498765020', 'Regular dental patient referral', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ============================================================================
-- STEP 7: CREATE EMERGENCY CONTACTS FOR SOME PATIENTS
-- ============================================================================
-- Add emergency contacts for first 10 patients
INSERT INTO emergency_contacts (patient_id, name, relationship, phone, created_at, updated_at)
SELECT p.id, 'Rosa Perez', 'Wife', '5555-1234', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patients p WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González';

INSERT INTO emergency_contacts (patient_id, name, relationship, phone, created_at, updated_at)
SELECT p.id, 'Carlos Santos', 'Brother', '5555-2345', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patients p WHERE p.first_name = 'Maria' AND p.last_name = 'Santos López';

INSERT INTO emergency_contacts (patient_id, name, relationship, phone, created_at, updated_at)
SELECT p.id, 'Maria Garcia', 'Sister', '5555-3456', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patients p WHERE p.first_name = 'Pedro' AND p.last_name = 'García Hernández';

INSERT INTO emergency_contacts (patient_id, name, relationship, phone, created_at, updated_at)
SELECT p.id, 'Jose Martinez', 'Son', '5555-4567', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patients p WHERE p.first_name = 'Ana' AND p.last_name = 'Martínez Ruiz';

INSERT INTO emergency_contacts (patient_id, name, relationship, phone, created_at, updated_at)
SELECT p.id, 'Alberto Morales', 'Father', '5555-5678', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patients p WHERE p.first_name = 'Luis' AND p.last_name = 'Morales Castro';

INSERT INTO emergency_contacts (patient_id, name, relationship, phone, created_at, updated_at)
SELECT p.id, 'Eduardo Flores', 'Husband', '5555-6789', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patients p WHERE p.first_name = 'Carmen' AND p.last_name = 'Flores Mejía';

INSERT INTO emergency_contacts (patient_id, name, relationship, phone, created_at, updated_at)
SELECT p.id, 'Teresa Diaz', 'Wife', '5555-7890', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patients p WHERE p.first_name = 'Roberto' AND p.last_name = 'Díaz Vargas';

INSERT INTO emergency_contacts (patient_id, name, relationship, phone, created_at, updated_at)
SELECT p.id, 'Patricia Ramirez', 'Mother', '5555-8901', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patients p WHERE p.first_name = 'Sofia' AND p.last_name = 'Ramírez Paz';

INSERT INTO emergency_contacts (patient_id, name, relationship, phone, created_at, updated_at)
SELECT p.id, 'Isabel Torres', 'Wife', '5555-9012', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patients p WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna';

INSERT INTO emergency_contacts (patient_id, name, relationship, phone, created_at, updated_at)
SELECT p.id, 'Roberto Sanchez', 'Father', '5555-0123', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patients p WHERE p.first_name = 'Elena' AND p.last_name = 'Sánchez Rivas';

SET session_replication_role = DEFAULT;

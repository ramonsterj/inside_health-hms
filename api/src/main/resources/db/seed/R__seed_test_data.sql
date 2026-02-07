-- ============================================================================
-- REPEATABLE MIGRATION: Test Data Seed Script
-- ============================================================================
-- This script truncates and reseeds test data for development/testing.
-- It runs after all versioned migrations when the checksum changes.
--
-- WARNING: This script is DESTRUCTIVE - it will delete all existing data!
-- DO NOT use in production environments.
--
-- Default Admin Credentials:
--   Username: admin
--   Email: admin@example.com
--   Password: insideadmin123
-- ============================================================================

-- Disable triggers temporarily for faster inserts
SET session_replication_role = replica;

-- ============================================================================
-- STEP 1: TRUNCATE TABLES (in dependency order - children first)
-- ============================================================================
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

-- USER gets minimal permissions
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

-- DOCTOR gets clinical permissions
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
-- Password for all test users: insideadmin123 (BCrypt hash)
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
('Juan', 'Perez Gonzalez', 35, 'MALE', 'Male', 'MARRIED', 'Catholic', 'UNIVERSITY', 'Engineer', 'Zone 10, Guatemala City', 'juan.perez@email.com', 'DPI-1234567890101', 'Regular checkup patient', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Maria', 'Santos Lopez', 28, 'FEMALE', 'Female', 'SINGLE', 'Evangelical', 'TECHNICAL', 'Accountant', 'Zone 1, Guatemala City', 'maria.santos@email.com', 'DPI-2345678901202', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Pedro', 'Garcia Hernandez', 45, 'MALE', 'Male', 'DIVORCED', 'Catholic', 'SECONDARY', 'Mechanic', 'Zone 7, Guatemala City', 'pedro.garcia@email.com', 'DPI-3456789012303', 'Diabetic patient, requires special attention', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Ana', 'Martinez Ruiz', 62, 'FEMALE', 'Female', 'WIDOWED', 'Catholic', 'PRIMARY', 'Retired Teacher', 'Zone 12, Guatemala City', 'ana.martinez@email.com', 'DPI-4567890123404', 'Hypertension history', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Luis', 'Morales Castro', 22, 'MALE', 'Male', 'SINGLE', 'None', 'UNIVERSITY', 'Student', 'Zone 14, Guatemala City', 'luis.morales@email.com', 'DPI-5678901234505', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Patient 6-10
('Carmen', 'Flores Mejia', 38, 'FEMALE', 'Female', 'MARRIED', 'Catholic', 'POSTGRADUATE', 'Lawyer', 'Zone 15, Guatemala City', 'carmen.flores@email.com', 'DPI-6789012345606', 'Allergic to penicillin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Roberto', 'Diaz Vargas', 55, 'MALE', 'Male', 'MARRIED', 'Evangelical', 'TECHNICAL', 'Electrician', 'Mixco, Guatemala', 'roberto.diaz@email.com', 'DPI-7890123456707', 'Heart condition, on medication', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Sofia', 'Ramirez Paz', 31, 'FEMALE', 'Female', 'SEPARATED', 'Catholic', 'UNIVERSITY', 'Nurse', 'Villa Nueva, Guatemala', 'sofia.ramirez@email.com', 'DPI-8901234567808', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Miguel', 'Torres Luna', 48, 'MALE', 'Male', 'MARRIED', 'Catholic', 'SECONDARY', 'Driver', 'Amatitlan, Guatemala', 'miguel.torres@email.com', 'DPI-9012345678909', 'Back pain issues', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Elena', 'Sanchez Rivas', 26, 'FEMALE', 'Female', 'SINGLE', 'Evangelical', 'UNIVERSITY', 'Marketing Specialist', 'Zone 4, Guatemala City', 'elena.sanchez@email.com', 'DPI-0123456789010', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Patient 11-15
('Francisco', 'Mendoza Aguilar', 70, 'MALE', 'Male', 'WIDOWED', 'Catholic', 'PRIMARY', 'Retired Farmer', 'Antigua Guatemala, Sacatepequez', 'francisco.mendoza@email.com', 'DPI-1234509876111', 'Arthritis, limited mobility', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Isabella', 'Cruz Monzon', 19, 'FEMALE', 'Female', 'SINGLE', 'Catholic', 'SECONDARY', 'Student', 'Quetzaltenango', 'isabella.cruz@email.com', 'DPI-2345610987212', 'First visit', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Andres', 'Ortiz Barrios', 42, 'MALE', 'Male', 'MARRIED', 'Evangelical', 'TECHNICAL', 'Carpenter', 'Escuintla', 'andres.ortiz@email.com', 'DPI-3456721098313', 'Occupational injury follow-up', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Gabriela', 'Reyes Soto', 33, 'FEMALE', 'Female', 'MARRIED', 'Catholic', 'POSTGRADUATE', 'Doctor', 'Zone 9, Guatemala City', 'gabriela.reyes@email.com', 'DPI-4567832109414', 'Pregnancy monitoring', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Oscar', 'Vasquez Pineda', 58, 'MALE', 'Male', 'DIVORCED', 'None', 'UNIVERSITY', 'Businessman', 'Zone 16, Guatemala City', 'oscar.vasquez@email.com', 'DPI-5678943210515', 'Annual executive checkup', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Patient 16-20
('Patricia', 'Herrera Godinez', 44, 'FEMALE', 'Female', 'MARRIED', 'Catholic', 'TECHNICAL', 'Secretary', 'Zone 5, Guatemala City', 'patricia.herrera@email.com', 'DPI-6789054321616', 'Chronic migraines', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Diego', 'Castillo Moreno', 29, 'MALE', 'Male', 'SINGLE', 'Evangelical', 'UNIVERSITY', 'Software Developer', 'Zone 11, Guatemala City', 'diego.castillo@email.com', 'DPI-7890165432717', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Valentina', 'Estrada Juarez', 52, 'FEMALE', 'Female', 'MARRIED', 'Catholic', 'SECONDARY', 'Cook', 'San Juan Sacatepequez', 'valentina.estrada@email.com', 'DPI-8901276543818', 'Gastritis, dietary restrictions', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Alejandro', 'Nunez Cordova', 36, 'MALE', 'Male', 'MARRIED', 'Catholic', 'UNIVERSITY', 'Architect', 'Zone 13, Guatemala City', 'alejandro.nunez@email.com', 'DPI-9012387654919', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Lucia', 'Alvarez Monroy', 24, 'FEMALE', 'Female', 'SINGLE', 'Evangelical', 'TECHNICAL', 'Dental Assistant', 'Zone 18, Guatemala City', 'lucia.alvarez@email.com', 'DPI-0123498765020', 'Regular dental patient referral', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ============================================================================
-- STEP 7: CREATE EMERGENCY CONTACTS FOR SOME PATIENTS
-- ============================================================================
-- Add emergency contacts for first 10 patients
INSERT INTO emergency_contacts (patient_id, name, relationship, phone, created_at, updated_at)
SELECT p.id, 'Rosa Perez', 'Wife', '5555-1234', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patients p WHERE p.first_name = 'Juan' AND p.last_name = 'Perez Gonzalez';

INSERT INTO emergency_contacts (patient_id, name, relationship, phone, created_at, updated_at)
SELECT p.id, 'Carlos Santos', 'Brother', '5555-2345', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patients p WHERE p.first_name = 'Maria' AND p.last_name = 'Santos Lopez';

INSERT INTO emergency_contacts (patient_id, name, relationship, phone, created_at, updated_at)
SELECT p.id, 'Maria Garcia', 'Sister', '5555-3456', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patients p WHERE p.first_name = 'Pedro' AND p.last_name = 'Garcia Hernandez';

INSERT INTO emergency_contacts (patient_id, name, relationship, phone, created_at, updated_at)
SELECT p.id, 'Jose Martinez', 'Son', '5555-4567', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patients p WHERE p.first_name = 'Ana' AND p.last_name = 'Martinez Ruiz';

INSERT INTO emergency_contacts (patient_id, name, relationship, phone, created_at, updated_at)
SELECT p.id, 'Alberto Morales', 'Father', '5555-5678', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patients p WHERE p.first_name = 'Luis' AND p.last_name = 'Morales Castro';

INSERT INTO emergency_contacts (patient_id, name, relationship, phone, created_at, updated_at)
SELECT p.id, 'Eduardo Flores', 'Husband', '5555-6789', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patients p WHERE p.first_name = 'Carmen' AND p.last_name = 'Flores Mejia';

INSERT INTO emergency_contacts (patient_id, name, relationship, phone, created_at, updated_at)
SELECT p.id, 'Teresa Diaz', 'Wife', '5555-7890', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patients p WHERE p.first_name = 'Roberto' AND p.last_name = 'Diaz Vargas';

INSERT INTO emergency_contacts (patient_id, name, relationship, phone, created_at, updated_at)
SELECT p.id, 'Patricia Ramirez', 'Mother', '5555-8901', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patients p WHERE p.first_name = 'Sofia' AND p.last_name = 'Ramirez Paz';

INSERT INTO emergency_contacts (patient_id, name, relationship, phone, created_at, updated_at)
SELECT p.id, 'Isabel Torres', 'Wife', '5555-9012', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patients p WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna';

INSERT INTO emergency_contacts (patient_id, name, relationship, phone, created_at, updated_at)
SELECT p.id, 'Roberto Sanchez', 'Father', '5555-0123', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM patients p WHERE p.first_name = 'Elena' AND p.last_name = 'Sanchez Rivas';

-- ============================================================================
-- STEP 8: CREATE TRIAGE CODES
-- ============================================================================
-- Columns: code, color (#RRGGBB), description, display_order
INSERT INTO triage_codes (code, color, description, display_order, created_at, updated_at) VALUES
('A', '#FF0000', 'Critical - Immediate attention required', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('B', '#FFA500', 'Urgent - Requires prompt attention', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('C', '#FFFF00', 'Less Urgent - Can wait for care', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('D', '#00FF00', 'Non-Urgent - Minor issues', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('E', '#0000FF', 'Referral - Scheduled admission', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ============================================================================
-- STEP 9: CREATE ROOMS
-- ============================================================================
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
('304', 'SHARED', 'MALE', 2, 950.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ============================================================================
-- STEP 10: RESEED INVENTORY CATEGORIES AND ITEMS
-- ============================================================================
-- Categories are wiped by TRUNCATE users CASCADE (via created_by/updated_by FKs)
-- so we must re-insert them along with all inventory items.

INSERT INTO inventory_categories (name, description, display_order, active, created_at, updated_at) VALUES
('Medicamentos', 'Medication and pharmaceutical supplies', 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Material y Equipo', 'Materials and equipment', 2, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Laboratorios', 'Laboratory services and supplies', 3, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Servicios', 'Hospital services', 4, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Personal Especial', 'Specialized personnel services', 5, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Ingredientes de Cocina', 'Kitchen ingredients', 6, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Alimentación', 'Food served to patients', 7, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Medicamentos (category: Medicamentos)
INSERT INTO inventory_items (category_id, name, description, price, cost, quantity, restock_level, pricing_type, time_unit, time_interval, active, created_at, updated_at)
SELECT c.id, v.name, v.description, v.price, v.cost, v.quantity, v.restock_level, v.pricing_type, v.time_unit, v.time_interval, v.active, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM inventory_categories c,
(VALUES
-- Antipsychotics
('TABLETA DE QUTIAPINA (QUETIOXAL 300 MG)', 'TABLETA DE QUETIOXAL 300 MG', 47.50, 15.83, 25, 10, 'FLAT', NULL::VARCHAR, NULL::INT, true),
('AMPOLLA DE OLANZAPINA (ZYPREXA IM 10 MG)', 'AMPOLLA DE OLANZAPINA (ZYPREXA IM 10 MG)', 525.00, 175.00, 12, 3, 'FLAT', NULL, NULL, true),
('CAJA DE OLANZAPINA (ZYPREXA 5 MG X 14 TABLETAS)', 'CAJA DE ZYPREXA 5 MG X 14 TABLETAS', 1907.80, 635.96, 3, 1, 'FLAT', NULL, NULL, true),
('TABLETA INDIVIDUAL OLANZAPINA (ZYPREXA 5 MG)', 'TABLETA INDIVIDUAL OLANZAPINA (ZYPREXA 5 MG)', 136.27, 45.43, 9, 5, 'FLAT', NULL, NULL, true),
('TABLETA DE SEROQUEL (QUETIAPINA 100 MG)', 'TABLETA DE SEROQUEL (QUETIAPINA 100 MG)', 72.03, 24.01, 30, 10, 'FLAT', NULL, NULL, true),
('TABLETA INDIVIDUAL ROYALINE 5 MG', 'TABLETA INDIVIDUAL ROYALINE TABLETAS 5 MG ROYALINE 5 MG', 7.56, 2.52, 16, 5, 'FLAT', NULL, NULL, true),
('CAJA DE ARIPIPRAZOL (PRI PAX 5 MG X 30 COMPRIMIDOS)', 'CAJA DE ARIPIPRAZOL (PRI PAX 5 MG X 30 COMPRIMIDOS)', 485.76, 161.92, 1, 1, 'FLAT', NULL, NULL, true),
('1 FRASCO DE ARIPIPRAZOL DE 150 ML', '1 FRASCO DE ARIPIPRAZOL DE 150 ML', 819.00, 273.00, 0, 1, 'FLAT', NULL, NULL, true),
-- Benzodiazepines and sedatives
('AMPOLLA MIDAZOLAM (DORMICUM 15MG/3ML)', 'AMPOLLA MIDAZOLAM (DORMICUM 15MG/3ML)', 196.47, 65.49, 14, 5, 'FLAT', NULL, NULL, true),
('TABLETA INDIVIDUAL MIDAZOLAM DORMICUM 15 MG', 'TABLETA INDIVIDUAL MIDAZOLAM DORMICUM 15 MG', 22.29, 7.43, 30, 10, 'FLAT', NULL, NULL, true),
('CAJA MIDAZOLAM (DORMICUM 15 MG X 30 TABLETAS)', 'CAJA MIDAZOLAM (DORMICUM 15 MG X 30 TABLETAS)', 668.31, 222.77, 1, 1, 'FLAT', NULL, NULL, true),
('TABLETA INDIVIDUAL CLONAZEPAM (RIVOTRIL 2 MG)', 'TABLETA INDIVIDUAL CLONAZEPAM (RIVOTRIL 2 MG)', 17.32, 4.33, 28, 10, 'FLAT', NULL, NULL, true),
('TABLETA INDIVIDUAL CLONAZEPAM (RIVOTRIL 2 MG) II', 'TABLETA INDIVIDUAL CLONAZEPAM (RIVOTRIL 2 MG)', 12.99, 4.33, 27, 10, 'FLAT', NULL, NULL, true),
('AMPOLLA DE DIAZEPAM DORMICUM 10 MG', 'AMPOLLA DE DIAZEPAM DORMICUM 10 MG', 10.62, 3.54, 2, 2, 'FLAT', NULL, NULL, true),
('TABLETA INDIVIDUAL BIPERIDENO HC (BIPARK 2 MG)', 'TABLETA INDIVIDUAL BIPERIDENO HC (BIPARK 2 MG)', 13.50, 4.50, 45, 10, 'FLAT', NULL, NULL, true),
-- Pregabalin
('TABLETA PREGABALINA (MARTESIA 75 MG)', 'TABLETA PREGABALINA (MARTESIA 75 MG)', 46.48, 11.62, 18, 5, 'FLAT', NULL, NULL, true),
('TABLETA PREGABALINA (ASTIK 75 MG)', 'TABLETA PREGABALINA (ASTIK 75 MG)', 29.49, 9.83, 4, 3, 'FLAT', NULL, NULL, true),
-- Antidepressants (SSRIs)
('TABLETA INDIVIDUAL DE SEROLUX (SERTRALINA 50 MG)', 'TABLETA INDIVIDUAL DE SEROLUX (SERTRALINA 50 MG)', 37.78, 12.59, 25, 10, 'FLAT', NULL, NULL, true),
('TABLETA INDIVIDUAL ALTRULINE (SERTRALINA) 50 MG', 'TABLETA INDIVIDUAL ALTRULINE (SERTRALINA) 50 MG', 87.78, 29.26, 27, 10, 'FLAT', NULL, NULL, true),
-- IV solutions
('SOLUCION SALINA DE 100 ML', 'SOLUCION SALINA 100 ML', 30.00, 10.00, 18, 5, 'FLAT', NULL, NULL, true),
('SOLUCION SALINA DE 250 ML', 'SOLUCION SALINA DE 250 ML', 24.84, 8.28, 11, 5, 'FLAT', NULL, NULL, true),
('SOLUCION SALINA DE 500 ML', 'SOLUCION SALINA DE 500 ML', 28.98, 9.66, 0, 5, 'FLAT', NULL, NULL, true),
('SOLUCION MIXTA DE 500 ML', 'SOLUCION MIXTA DE 500 ML', 43.20, 14.40, 1, 2, 'FLAT', NULL, NULL, true),
('SOLUCION HARTMAN 1000 ML', 'SOLUCION HARTMAN 1000 ML', 48.00, 16.00, 4, 2, 'FLAT', NULL, NULL, true),
('SOLUCION DEXTROSA AL 5% 500 ML', 'SOLUCION DEXTROSA AL 5% 500 ML', 33.60, 11.20, 5, 2, 'FLAT', NULL, NULL, true),
('AGUA ESTERIL PARA INYECCION 100 ML', 'AGUA ESTERIL PARA INYECCION 100 ML', 33.60, 11.20, 2, 2, 'FLAT', NULL, NULL, true),
('AGUA ESTERIL INYECTABLE 500 ML', 'AGUA ESTERIL INYECTABLE 500 ML', 78.00, 26.00, 1, 2, 'FLAT', NULL, NULL, true),
-- Anesthetics
('LIDOCAINA CON EPINEFRINA 10 ML', 'LIDOCAINA CON EPINEFRINA 10 ML', 27.00, 9.00, 4, 2, 'FLAT', NULL, NULL, true),
('AMPOLLA PROPOFOL-LIPURO 1%', 'AMPOLLA PROPOFOL-LIPURO 1%', 81.00, 27.00, 5, 2, 'FLAT', NULL, NULL, true),
-- Corticosteroids
('TABLETA INDIVIDUAL DEXAMETASONA 0.5 MG', 'DEXAMETASONA 0.5 MG', 5.44, 1.36, 36, 10, 'FLAT', NULL, NULL, true),
('DEXAMETASONA, AMPOLLA 8 MG/2 ML', 'DEXAMETASONA, AMPOLLA 8 MG/2 ML', 126.45, 42.15, 1, 2, 'FLAT', NULL, NULL, true),
('DEXAMETASONA AMPOLLA 4MG', 'DEXAMETASONA AMPOLLA 4MG', 75.81, 25.27, 5, 2, 'FLAT', NULL, NULL, true),
-- Antihistamines and topicals
('CALADRYL CLEAR LOCION 100 ML', 'CALADRYL CLEAR LOCION 100 ML', 246.21, 82.07, 0, 1, 'FLAT', NULL, NULL, true),
('TABLETA INDIVIDUAL DE CLORHIDRATO DE DIFENHIDRAMINA 25 MG', 'TABLETA INDIVIDUAL DE CLORHIDRATO DE DIFENHIDRAMINA 25 MG', 8.31, 2.77, 5, 3, 'FLAT', NULL, NULL, true),
('HISTAPRIN AMPOLLA 10 MG', 'HISTAPRIN AMPOLLA 10 MG', 130.53, 43.51, 1, 1, 'FLAT', NULL, NULL, true),
-- Analgesics and anti-inflammatories
('TABLETA INDIVIDUAL DE DICLOFENACO 50 MG', 'TABLETA INDIVIDUAL DE DICLOFENACO 50 MG', 3.60, 1.20, 7, 5, 'FLAT', NULL, NULL, true),
('TABLETA INDIVIDUAL DE ACETAMINOFEN 50 MG', 'TABLETA INDIVIDUAL DE ACETAMINOFEN 50 MG', 2.10, 0.70, 8, 5, 'FLAT', NULL, NULL, true),
-- Electrolytes and emergency medications
('AMPOLLA CLORURO DE POTASIO 5 ML (200 MG/ML)', 'AMPOLLA DE CLORURO DE POTASIO 5 ML (200 MG/ML)', 11.40, 3.80, 1, 2, 'FLAT', NULL, NULL, true),
('AMPOLLA DE CLORURO DE POTASIO 20MEQ/10 ML', 'AMPOLLA DE CLORURO DE POTASIO 20 MEQ/10 ML', 10.50, 3.50, 25, 5, 'FLAT', NULL, NULL, true),
('AMPOLLA DE CLORURO DE SODIO 20% (200MG/10ML)', 'AMPOLLA DE CLORURO DE SODIO 20% (200MG/10ML)', 12.00, 4.00, 14, 5, 'FLAT', NULL, NULL, true),
('AMPOLLA DE BICARBONATO DE SODIO 7%', 'AMPOLLA DE BICARBONATO DE SODIO 7%', 76.50, 25.50, 5, 2, 'FLAT', NULL, NULL, true),
('AMPOLLA DE SULFATO DE MAGNESIO 50%', 'AMPOLLA DE SULFATO DE MAGNESIO 50%', 14.40, 4.80, 5, 2, 'FLAT', NULL, NULL, true),
('AMPOLLA DE AMINOFILINA 250MG/5ML', 'AMPOLLA DE AMINOFILINA 250MG/5ML', 15.60, 5.20, 5, 2, 'FLAT', NULL, NULL, true),
('AMPOLLA DE SULFATO DE ATROPINA 0.5MG/1ML', 'AMPOLLA DE SULFATO DE ATROPINA 0.5MG/1ML', 11.40, 3.80, 5, 2, 'FLAT', NULL, NULL, true),
('AMPOLLA DE EPINEFRINA 1 MG/1 ML (ADRENALINA)', 'AMPOLLA DE EPINEFRINA 1 MG/1 ML (ADRENALINA)', 11.70, 3.90, 5, 2, 'FLAT', NULL, NULL, true),
('AMPOLLA DE NALOXONA 0.4MG/1ML', 'AMPOLLA DE NALOXONA 0.4MG/1ML', 35.25, 11.75, 5, 2, 'FLAT', NULL, NULL, true),
('AMPOLLA DE FUROSEMIDA 20MG/2ML', 'AMPOLLA DE FUROSEMIDA 20MG/2ML', 7.50, 2.50, 5, 2, 'FLAT', NULL, NULL, true),
('AMPOLLA DE FLUMAZENIL 0.1MG/ML', 'AMPOLLA DE FLUMAZENIL 0.1MG/ML', 390.00, 130.00, 2, 1, 'FLAT', NULL, NULL, true),
('AMPOLLA DE INOTROPISA 5 ML (DOPAMINA)', 'AMPOLLA DE INOTROPISA 5 ML (DOPAMINA)', 255.00, 85.00, 5, 2, 'FLAT', NULL, NULL, true),
-- GI
('1 SOBRE DE CONTUMAX POLVO 17G', '1 SOBRE DE CONTUMAX POLVO 17G', 56.77, 18.92, 15, 5, 'FLAT', NULL, NULL, true)
) AS v(name, description, price, cost, quantity, restock_level, pricing_type, time_unit, time_interval, active)
WHERE c.name = 'Medicamentos';

-- Material y Equipo
INSERT INTO inventory_items (category_id, name, description, price, cost, quantity, restock_level, pricing_type, time_unit, time_interval, active, created_at, updated_at)
SELECT c.id, v.name, v.description, v.price, v.cost, v.quantity, v.restock_level, v.pricing_type, v.time_unit, v.time_interval, v.active, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM inventory_categories c,
(VALUES
-- Time-based equipment usage
('MONITOR CARDIACO', 'USO MONITOR CARDIACO (1 HORA)', 250.00, 250.00, 0, 0, 'TIME_BASED', 'HOURS'::VARCHAR, 1::INT, true),
('BOMBA DE INFUSION', 'USO DE BOMBA DE INFUSION (1 HORA)', 250.00, 250.00, 0, 0, 'TIME_BASED', 'HOURS', 1, true),
('OXIGENO', 'USO OXIGENO (10 MINUTOS)', 117.00, 16.20, 0, 0, 'TIME_BASED', 'MINUTES', 10, true),
-- Flat-priced equipment and services
('EKG', 'EKG', 265.00, 265.00, 0, 0, 'FLAT', NULL, NULL, true),
('USO DE DESFIBRILADOR', 'USO DE DESFIBRILADOR', 3000.00, 0.00, 0, 0, 'FLAT', NULL, NULL, true),
('USO DE GLUCOMETRO', 'USO DE GLUCOMETRO', 49.98, 12.09, 50, 5, 'FLAT', NULL, NULL, true),
-- Syringes
('JERINGAS DESCARTABLES 3 CC', 'JERINGAS DESCARTABLES 3 CC 21 G', 2.25, 0.56, 83, 10, 'FLAT', NULL, NULL, true),
('JERINGAS DESCARTABLES DE 5 CC', 'JERINGAS DESCARTABLES DE 5 CC 21 G', 2.35, 0.58, 62, 10, 'FLAT', NULL, NULL, true),
('JERINGAS DESCARTABLES 10 CC', 'JERINGAS DESCARTABLES 10 CC 21 G', 3.60, 0.90, 72, 10, 'FLAT', NULL, NULL, true),
('JERINGA DE INSULINA DE 1 CC', 'JERINGA DE INSULINA DE 1 CC', 3.40, 0.85, 97, 10, 'FLAT', NULL, NULL, true),
-- Aspiration probes
('SONDA DE ASPIRACION NO. 12', 'SONDA DE ASPIRACION NO. 12 OPERSON', 9.00, 2.25, 3, 2, 'FLAT', NULL, NULL, true),
('SONDA DE ASPIRACION NO. 14', 'SONDA DE ASPIRACION NO. 14 OPERSON', 9.00, 2.25, 3, 2, 'FLAT', NULL, NULL, true),
('SONDA DE ASPIRACION NO. 16', 'SONDA DE ASPIRACION NO. 16 OPERSON', 9.00, 2.25, 3, 2, 'FLAT', NULL, NULL, true),
('SONDA DE ASPIRACION NO. 18', 'SONDA DE ASPIRACION NO. 18 OPERSON', 9.00, 2.25, 3, 2, 'FLAT', NULL, NULL, true),
-- IV catheters
('CATETER INTRAVENOSO NO. 18', 'CATETER INTRAVENOSO NO. 18 (ANGIOCATH) NIPRO', 17.00, 4.25, 29, 5, 'FLAT', NULL, NULL, true),
('CATETER INTRAVENOSO NO. 20', 'CATETER INTRAVENOSO NO. 20 (ANGIOCATH) NIPRO', 17.00, 4.25, 18, 5, 'FLAT', NULL, NULL, true),
('CATETER INTRAVENOSO NO. 22', 'CATETER INTRAVENOSO NO. 22 (ANGIOCATH) NIPRO', 17.00, 4.50, 25, 5, 'FLAT', NULL, NULL, true),
('CATETER INTRAVENOSO NO. 24', 'CATETER INTRAVENOSO NO. 24 (ANGIOCATH) NIPRO', 17.00, 4.50, 22, 5, 'FLAT', NULL, NULL, true),
-- IV supplies
('SELLOS DE HEPARINA', 'SELLOS DE HEPARINA - TAPON IN LUER-LOK BARAUN', 7.20, 1.80, 17, 5, 'FLAT', NULL, NULL, true),
('VENOSETH', 'VENOSETH (EQUIPO DE SUERO) PARKINGTON', 10.40, 2.60, 4, 2, 'FLAT', NULL, NULL, true),
-- Endotracheal tubes
('TUBOS ENDOTRAQUEALES #7', 'TUBOS ENDOTRAQUEALES #7, OPERSON', 45.00, 11.25, 3, 2, 'FLAT', NULL, NULL, true),
('TUBOS ENDOTRAQUEALES #7.5', 'TUBOS ENDOTRAQUEALES #7.5, OPERSON', 45.00, 11.25, 3, 2, 'FLAT', NULL, NULL, true),
('TUBOS ENDOTRAQUEALES NO. 8', 'TUBOS ENDOTRAQUEALES NO. 8', 88.00, 22.00, 5, 2, 'FLAT', NULL, NULL, true),
-- Sutures
('SUTURAS NYLON # 2-0, 75 CM', 'SUTURAS NYLON # 2-0, 75 CM, ATRAMAT', 39.60, 9.99, 12, 5, 'FLAT', NULL, NULL, true),
('SUTURAS NYLON # 3-0, 75 CM', 'SUTURAS NYLON # 3-0, 75 CM, CE-24 ATRAMAT', 39.60, 9.90, 12, 5, 'FLAT', NULL, NULL, true),
('SUTURAS PGA # 2-0, 75 CM', 'SUTURAS # 3-0, 75 CM, G-37 AHUSADA, ATRAMAT', 63.12, 15.78, 12, 5, 'FLAT', NULL, NULL, true),
('SUTURAS CATGUT SIMPLE # 2-0, 75 CM', 'SUTURAS CATGUT SIMPLE # 2-0, 75 CM, G-37 GRUESA ATRAMAT', 59.00, 14.75, 12, 5, 'FLAT', NULL, NULL, true),
('SUTURAS SEDA # 2-0, 75 CM', 'SUTURAS SEDA # 2-0, 75 CM, SR-26, ATRAMAT', 39.00, 9.75, 12, 5, 'FLAT', NULL, NULL, true),
-- Respiratory supplies
('CANULAS DE OXIGENO BINASAL GRANDE', 'CANULAS DE OXIGENO BINASAL GRANDE, OPERSON', 19.60, 4.90, 10, 3, 'FLAT', NULL, NULL, true),
('MASCARILLA P/NEBULIZAR ADULTO L', 'MASCARILLA P/NEBULIZAR ADULTO L, OPERSON', 54.00, 13.50, 11, 3, 'FLAT', NULL, NULL, true),
('MASCARILLA CON RESERVORIO', 'MASCARILLA CON RESERVORIO', 280.00, 70.00, 5, 2, 'FLAT', NULL, NULL, true),
-- PPE and masks
('MASCARILLAS 3 PLIEGOS', 'MASCARILLAS 3 PLIEGOS ELASTICAS AMBIDEM', 1.46, 0.36, 50, 20, 'FLAT', NULL, NULL, true),
-- Gloves
('GUANTES QUIRURGICOS NO. 6', 'GUANTES QUIRURGICOS NO. 6, AMBIDERM', 11.40, 2.85, 50, 10, 'FLAT', NULL, NULL, true),
('GUANTES QUIRURGICOS NO. 7', 'GUANTES QUIRURGICOS NO. 7, AMBIDERM', 11.40, 2.85, 49, 10, 'FLAT', NULL, NULL, true),
('GUANTES DESCARTABLES TALLA S', 'GUANTES DESCARTABLES TALLA S, PROTECT', 3.40, 0.85, 64, 10, 'FLAT', NULL, NULL, true),
('GUANTES DESCARTABLES TALLA L', 'GUANTES DESCARTABLES TALLA L, PROTECT', 3.40, 0.85, 47, 10, 'FLAT', NULL, NULL, true),
-- Foley catheters
('SONDA FOLEY NO. 14', 'SONDA FOLEY NO. 14, BALON 5-15 ML 2 VIAS, OPERSON', 30.00, 7.50, 10, 3, 'FLAT', NULL, NULL, true),
('SONDA FOLEY NO. 16', 'SONDA FOLEY NO. 16, BALON 5-15 ML 2 VIAS, OPERSON', 30.00, 7.50, 10, 3, 'FLAT', NULL, NULL, true),
-- Nasogastric probe
('SONDA NASOGASTRICA NO. 5', 'SONDA NASOGASTRICA NO. 5', 7.05, 2.35, 5, 2, 'FLAT', NULL, NULL, true),
-- Urine collection bags
('BOLSA RECOLECTORA DE ORINA PIERNA', 'BOLSA RECOLECTORA DE ORINA PIERNA 750 ML, OPERSON', 22.20, 5.55, 3, 2, 'FLAT', NULL, NULL, true),
('BOLSA RECOLECTORA DE ORINA CAMA', 'BOLSA RECOLECTORA DE ORINA CAMA 2000ML, PARKINGTON', 20.00, 5.00, 3, 2, 'FLAT', NULL, NULL, true),
-- General supplies
('BAJALENGUAS DE MADERA', 'BAJALENGUAS DE MADERA', 0.54, 0.13, 196, 20, 'FLAT', NULL, NULL, true),
('SONIGEL TUBO 125 GRAMOS', 'SONIGEL TUBO 125 GRAMOS DIQUIVA', 47.00, 11.75, 3, 2, 'FLAT', NULL, NULL, true),
-- Needles
('AGUJA HIPODERMICA NO. 23 G', 'AGUJA HIPODERMICA NO. 23 G', 4.00, 1.00, 196, 20, 'FLAT', NULL, NULL, true),
('AGUJA 18X1', 'AGUJA 18X1', 1.40, 0.35, 100, 20, 'FLAT', NULL, NULL, true),
-- Wound care and dressings
('GASAS ESTERILES', 'GASAS ESTERILES (UNIDAD)', 3.68, 0.92, 42, 20, 'FLAT', NULL, NULL, true),
('CURITAS CIRCULARES', 'CURITAS CIRCULARES', 0.56, 0.14, 127, 20, 'FLAT', NULL, NULL, true),
('VENDA GASA 3X10 Y', 'ROLLO DE VENDA GASA 3 X 10 Y.', 23.28, 5.82, 3, 2, 'FLAT', NULL, NULL, true),
('TEGADERM 3M', 'TEGADERM 3M, NEXCARE', 91.00, 22.75, 5, 2, 'FLAT', NULL, NULL, true),
('CAMPO ESTERIL', 'CAMPO ESTERIL', 64.00, 16.00, 12, 5, 'FLAT', NULL, NULL, true),
-- Electrodes and tapes
('ELECTRODOS', 'ELECTRODOS EKG', 4.74, 1.58, 37, 10, 'FLAT', NULL, NULL, true),
('MICROPORE', 'MICROPORE', 15.50, 15.50, 2, 2, 'FLAT', NULL, NULL, true),
('MICROPORE DE 1 PULGADA', 'MICROPORE DE 1 PULGADA', 34.00, 8.50, 5, 2, 'FLAT', NULL, NULL, true),
('ESPARADRAPO', 'ESPARADRAPO', 19.60, 4.90, 0, 2, 'FLAT', NULL, NULL, true),
-- Cleaning and hygiene
('ALCOHOL EN GEL 60 G', 'ALCOHOL EN GEL 60 G', 12.65, 12.65, 1, 2, 'FLAT', NULL, NULL, true),
('SOLUCION DE CLORHEXIDINA 5%', 'GALON SOLUCION DE CLORHEXIDINA 5%', 300.00, 300.00, 1, 1, 'FLAT', NULL, NULL, true),
('KIT DE HIGIENE', 'KIT DE HIGIENE', 125.00, 56.00, 4, 2, 'FLAT', NULL, NULL, true),
('1 TORUNDA DE ALGODON', '1 TORUNDA DE ALGODON', 2.00, 0.20, 60, 20, 'FLAT', NULL, NULL, true),
('10 CC ALCOHOL', 'ALCOHOL', 1.00, 0.20, 198, 20, 'FLAT', NULL, NULL, true)
) AS v(name, description, price, cost, quantity, restock_level, pricing_type, time_unit, time_interval, active)
WHERE c.name = 'Material y Equipo';

-- Laboratorios
INSERT INTO inventory_items (category_id, name, description, price, cost, quantity, restock_level, pricing_type, time_unit, time_interval, active, created_at, updated_at)
SELECT c.id, v.name, v.description, v.price, v.cost, v.quantity, v.restock_level, v.pricing_type, v.time_unit, v.time_interval, v.active, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM inventory_categories c,
(VALUES
-- Chemistry panel
('ACIDO URICO', 'SUERO', 150.00, 50.00, 0, 0, 'FLAT', NULL::VARCHAR, NULL::INT, true),
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
('NITROGENO DE UREA', 'SUERO', 150.00, 50.00, 0, 0, 'FLAT', NULL, NULL, true),
('POTASIO', 'SUERO', 225.00, 75.00, 0, 0, 'FLAT', NULL, NULL, true),
('PROTEINAS TOTALES', 'SUERO', 150.00, 50.00, 0, 0, 'FLAT', NULL, NULL, true),
('SODIO', 'SUERO', 225.00, 75.00, 0, 0, 'FLAT', NULL, NULL, true),
-- Lipid panel
('COLESTEROL HDL', 'SUERO', 150.00, 50.00, 0, 0, 'FLAT', NULL, NULL, true),
('COLESTEROL LDL', 'SUERO', 150.00, 50.00, 0, 0, 'FLAT', NULL, NULL, true),
('COLESTEROL TOTAL', 'SUERO', 150.00, 50.00, 0, 0, 'FLAT', NULL, NULL, true),
('COLESTEROL VLDL', 'SUERO', 150.00, 50.00, 0, 0, 'FLAT', NULL, NULL, true),
('TRIGLICERIDOS', 'SUERO', 150.00, 50.00, 0, 0, 'FLAT', NULL, NULL, true),
('PERFIL DE LIPIDOS', 'SUERO', 600.00, 200.00, 0, 0, 'FLAT', NULL, NULL, true),
-- Liver panel
('TGO', 'SUERO', 180.00, 60.00, 0, 0, 'FLAT', NULL, NULL, true),
('TGP', 'SUERO', 180.00, 60.00, 0, 0, 'FLAT', NULL, NULL, true),
('PRUEBAS HEPATICAS TGO-TGP-GGT', 'SUERO', 600.00, 200.00, 0, 0, 'FLAT', NULL, NULL, true),
('LIPASA', 'SUERO', 225.00, 75.00, 0, 0, 'FLAT', NULL, NULL, true),
('LIPASA TOTALES', 'SUERO', 180.00, 60.00, 0, 0, 'FLAT', NULL, NULL, true),
-- Cardiac markers
('CK-MB', 'SUERO', 300.00, 100.00, 0, 0, 'FLAT', NULL, NULL, true),
('CK-TOTAL', 'SUERO', 300.00, 100.00, 0, 0, 'FLAT', NULL, NULL, true),
('CK-MM', 'SUERO', 750.00, 250.00, 0, 0, 'FLAT', NULL, NULL, true),
-- Coagulation
('DIMERO D', 'PLASMA CITRATADO', 1500.00, 500.00, 0, 0, 'FLAT', NULL, NULL, true),
('PROTEINA C', 'PLASMA CITRATADO', 1050.00, 350.00, 0, 0, 'FLAT', NULL, NULL, true),
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
('HEMATOLOGIA COMPLETA', 'TUBO CON EDTA', 180.00, 60.00, 0, 0, 'FLAT', NULL, NULL, true),
('HEMOGLOBINA-HEMATOCRITO', 'SUERO', 150.00, 50.00, 0, 0, 'FLAT', NULL, NULL, true),
('HEMOGLOBINA GLICOSILADA', 'TUBO CON EDTA', 450.00, 150.00, 0, 0, 'FLAT', NULL, NULL, true),
('GRUPO SANGUINEO Y FACTOR RH', 'TUBO CON EDTA', 150.00, 50.00, 0, 0, 'FLAT', NULL, NULL, true),
('HIERRO CAPACIDAD DE FIJACION', 'SUERO', 450.00, 150.00, 0, 0, 'FLAT', NULL, NULL, true),
('HIERRO SERICO', 'SUERO', 450.00, 150.00, 0, 0, 'FLAT', NULL, NULL, true),
-- Iron and vitamins
('VITAMINA B-12', 'SUERO', 750.00, 250.00, 0, 0, 'FLAT', NULL, NULL, true),
('VITAMINA D', 'SUERO', 1050.00, 350.00, 0, 0, 'FLAT', NULL, NULL, true),
-- Inflammatory markers
('PROTEINA C REACTIVA', 'SUERO', 150.00, 50.00, 0, 0, 'FLAT', NULL, NULL, true),
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
('KIT DE INGRESO', 'KIT DE INGRESO (HEMATOLOGIA, GLUCOSA, CREATININA, NITROGENO DE UREA, PERFIL DE LIPIDOS, TGO-TGP, SODIO, POTASIO, HORMONAS FT3, FT4 Y TSH, PANEL DE DROGAS EN ORINA, VITAMINA D)', 4950.00, 1650.00, 97, 10, 'FLAT', NULL, NULL, true)
) AS v(name, description, price, cost, quantity, restock_level, pricing_type, time_unit, time_interval, active)
WHERE c.name = 'Laboratorios';

-- Servicios
INSERT INTO inventory_items (category_id, name, description, price, cost, quantity, restock_level, pricing_type, time_unit, time_interval, active, created_at, updated_at)
SELECT c.id, v.name, v.description, v.price, v.cost, v.quantity, v.restock_level, v.pricing_type, v.time_unit, v.time_interval, v.active, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM inventory_categories c,
(VALUES
('ATENCION EMERGENCIA', 'ATENCION EMERGENCIA', 900.00, 0.00, 0, 0, 'FLAT', NULL::VARCHAR, NULL::INT, true),
('KETAMINA AMBULATORIA', 'PROCEDIMIENTO DE KETAMINA', 1500.00, 0.00, 0, 0, 'FLAT', NULL, NULL, true),
('KETAMINA INTERNA', 'PROCEDIMIENTO DE KETAMINA INTERNA', 1100.00, 0.00, 0, 0, 'FLAT', NULL, NULL, true)
) AS v(name, description, price, cost, quantity, restock_level, pricing_type, time_unit, time_interval, active)
WHERE c.name = 'Servicios';

-- Personal Especial
INSERT INTO inventory_items (category_id, name, description, price, cost, quantity, restock_level, pricing_type, time_unit, time_interval, active, created_at, updated_at)
SELECT c.id, v.name, v.description, v.price, v.cost, v.quantity, v.restock_level, v.pricing_type, v.time_unit, v.time_interval, v.active, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM inventory_categories c,
(VALUES
('ENFERMERA SOMBRA HOSPITAL', 'ENFERMERA SOMBRA HOSPITAL', 500.00, 300.00, 0, 0, 'FLAT', NULL::VARCHAR, NULL::INT, true),
('ENFERMERA SOMBRA DOMICILIO', 'ENFERMERA SOMBRA DOMICILIO', 650.00, 350.00, 0, 0, 'FLAT', NULL, NULL, true)
) AS v(name, description, price, cost, quantity, restock_level, pricing_type, time_unit, time_interval, active)
WHERE c.name = 'Personal Especial';

-- Ingredientes de Cocina
INSERT INTO inventory_items (category_id, name, description, price, cost, quantity, restock_level, pricing_type, time_unit, time_interval, active, created_at, updated_at)
SELECT c.id, v.name, v.description, v.price, v.cost, v.quantity, v.restock_level, v.pricing_type, v.time_unit, v.time_interval, v.active, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM inventory_categories c,
(VALUES
-- Granos y cereales
('ARROZ BLANCO (LIBRA)', 'ARROZ BLANCO DE PRIMERA CALIDAD', 5.50, 3.50, 100, 25, 'FLAT', NULL::VARCHAR, NULL::INT, true),
('FRIJOL NEGRO (LIBRA)', 'FRIJOL NEGRO SECO', 7.00, 4.50, 80, 20, 'FLAT', NULL, NULL, true),
('PASTA ESPAGUETI (400G)', 'PASTA TIPO ESPAGUETI', 8.50, 5.00, 40, 10, 'FLAT', NULL, NULL, true),
('PASTA CODITOS (400G)', 'PASTA TIPO CODITOS', 8.50, 5.00, 30, 10, 'FLAT', NULL, NULL, true),
('AVENA EN HOJUELAS (LIBRA)', 'AVENA EN HOJUELAS PARA COCINAR', 6.00, 3.80, 25, 10, 'FLAT', NULL, NULL, true),
('HARINA DE MAIZ (LIBRA)', 'HARINA DE MAIZ PARA TORTILLAS', 4.50, 2.80, 50, 15, 'FLAT', NULL, NULL, true),
('PAN DE SANDWICH (BOLSA)', 'PAN BLANCO DE MOLDE PARA SANDWICH', 18.00, 12.00, 15, 5, 'FLAT', NULL, NULL, true),
('TORTILLAS DE MAIZ (DOCENA)', 'TORTILLAS DE MAIZ FRESCAS', 8.00, 5.00, 20, 10, 'FLAT', NULL, NULL, true),
-- Proteinas
('POLLO ENTERO (LIBRA)', 'POLLO FRESCO ENTERO', 14.00, 9.00, 50, 15, 'FLAT', NULL, NULL, true),
('PECHUGA DE POLLO (LIBRA)', 'PECHUGA DE POLLO SIN HUESO', 22.00, 15.00, 40, 10, 'FLAT', NULL, NULL, true),
('CARNE DE RES (LIBRA)', 'CARNE DE RES PARA GUISAR', 30.00, 20.00, 30, 10, 'FLAT', NULL, NULL, true),
('HUEVOS (CARTON 30 UNIDADES)', 'HUEVOS DE GALLINA FRESCOS', 45.00, 30.00, 10, 3, 'FLAT', NULL, NULL, true),
('SALCHICHA (LIBRA)', 'SALCHICHA DE POLLO', 16.00, 10.00, 15, 5, 'FLAT', NULL, NULL, true),
-- Lacteos
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
('PLATANO MADURO (UNIDAD)', 'PLATANO MADURO PARA COCINAR', 2.50, 1.50, 40, 15, 'FLAT', NULL, NULL, true),
('BANANO (UNIDAD)', 'BANANO FRESCO', 1.50, 0.80, 60, 20, 'FLAT', NULL, NULL, true),
('NARANJA (UNIDAD)', 'NARANJA PARA JUGO', 2.00, 1.00, 50, 20, 'FLAT', NULL, NULL, true),
('LIMON (UNIDAD)', 'LIMON PERSA', 1.00, 0.50, 40, 15, 'FLAT', NULL, NULL, true),
-- Aceites, condimentos y basicos
('ACEITE VEGETAL (LITRO)', 'ACEITE VEGETAL PARA COCINAR', 22.00, 15.00, 15, 5, 'FLAT', NULL, NULL, true),
('AZUCAR (LIBRA)', 'AZUCAR BLANCA REFINADA', 5.00, 3.00, 40, 15, 'FLAT', NULL, NULL, true),
('SAL (LIBRA)', 'SAL DE COCINA', 2.50, 1.50, 20, 5, 'FLAT', NULL, NULL, true),
('CONSOMME DE POLLO (SOBRE)', 'CONSOMME DE POLLO EN POLVO', 3.00, 1.80, 50, 15, 'FLAT', NULL, NULL, true),
('SALSA DE TOMATE (BOTELLA 400ML)', 'SALSA DE TOMATE PARA COCINAR', 12.00, 7.50, 15, 5, 'FLAT', NULL, NULL, true),
('MAYONESA (FRASCO 400G)', 'MAYONESA PARA COCINAR', 20.00, 13.00, 8, 3, 'FLAT', NULL, NULL, true),
-- Bebidas
('CAFE MOLIDO (LIBRA)', 'CAFE MOLIDO TOSTADO', 30.00, 20.00, 10, 3, 'FLAT', NULL, NULL, true),
('TE EN BOLSITAS (CAJA 25 UNIDADES)', 'TE DE MANZANILLA O HIERBAS', 15.00, 9.00, 10, 3, 'FLAT', NULL, NULL, true),
('AGUA PURA (GARAFON 5 GALONES)', 'AGUA PURIFICADA PARA CONSUMO', 25.00, 15.00, 8, 3, 'FLAT', NULL, NULL, true),
('JUGO DE NARANJA (LITRO)', 'JUGO DE NARANJA NATURAL', 15.00, 9.00, 12, 5, 'FLAT', NULL, NULL, true),
('LECHE EN POLVO (BOLSA 400G)', 'LECHE EN POLVO INSTANTANEA', 28.00, 18.00, 10, 3, 'FLAT', NULL, NULL, true)
) AS v(name, description, price, cost, quantity, restock_level, pricing_type, time_unit, time_interval, active)
WHERE c.name = 'Ingredientes de Cocina';

-- Alimentación
INSERT INTO inventory_items (category_id, name, description, price, cost, quantity, restock_level, pricing_type, time_unit, time_interval, active, created_at, updated_at)
SELECT c.id, v.name, v.description, v.price, v.cost, v.quantity, v.restock_level, v.pricing_type, v.time_unit, v.time_interval, v.active, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM inventory_categories c,
(VALUES
-- Tiempos de comida regulares
('DESAYUNO REGULAR', 'DESAYUNO COMPLETO: HUEVOS, FRIJOLES, PLATANO, PAN, CAFE O JUGO', 65.00, 35.00, 0, 0, 'FLAT', NULL::VARCHAR, NULL::INT, true),
('ALMUERZO REGULAR', 'ALMUERZO COMPLETO: SOPA, PLATO FUERTE CON PROTEINA, ARROZ, ENSALADA, REFRESCO', 85.00, 45.00, 0, 0, 'FLAT', NULL, NULL, true),
('CENA REGULAR', 'CENA COMPLETA: PLATO FUERTE LIVIANO, ACOMPANAMIENTO, BEBIDA', 70.00, 38.00, 0, 0, 'FLAT', NULL, NULL, true),
('MERIENDA', 'MERIENDA: FRUTA, GALLETAS O SANDWICH PEQUENO CON BEBIDA', 30.00, 15.00, 0, 0, 'FLAT', NULL, NULL, true),
('REFACCION NOCTURNA', 'REFACCION NOCTURNA LIVIANA: CEREAL, FRUTA O PAN CON BEBIDA CALIENTE', 25.00, 12.00, 0, 0, 'FLAT', NULL, NULL, true),
-- Dietas especiales
('DESAYUNO DIETA DIABETICA', 'DESAYUNO PARA PACIENTE DIABETICO: HUEVOS, FRIJOLES, PAN INTEGRAL, BEBIDA SIN AZUCAR', 75.00, 40.00, 0, 0, 'FLAT', NULL, NULL, true),
('ALMUERZO DIETA DIABETICA', 'ALMUERZO PARA PACIENTE DIABETICO: PROTEINA, VEGETALES, PORCION CONTROLADA DE CARBOHIDRATOS', 95.00, 50.00, 0, 0, 'FLAT', NULL, NULL, true),
('CENA DIETA DIABETICA', 'CENA PARA PACIENTE DIABETICO: PLATO LIVIANO, VEGETALES, BEBIDA SIN AZUCAR', 80.00, 42.00, 0, 0, 'FLAT', NULL, NULL, true),
('DIETA BLANDA (TIEMPO DE COMIDA)', 'COMIDA DE DIETA BLANDA: ALIMENTOS SUAVES, FACILES DE DIGERIR', 70.00, 38.00, 0, 0, 'FLAT', NULL, NULL, true),
('DIETA LIQUIDA (TIEMPO DE COMIDA)', 'DIETA LIQUIDA: CALDOS, JUGOS, GELATINA, BEBIDAS NUTRITIVAS', 50.00, 25.00, 0, 0, 'FLAT', NULL, NULL, true),
('DIETA HIPOSODICA (TIEMPO DE COMIDA)', 'COMIDA BAJA EN SODIO PARA PACIENTES CON HIPERTENSION', 80.00, 42.00, 0, 0, 'FLAT', NULL, NULL, true),
-- Extras
('SUPLEMENTO NUTRICIONAL', 'BEBIDA DE SUPLEMENTO NUTRICIONAL TIPO ENSURE O SIMILAR', 45.00, 28.00, 20, 5, 'FLAT', NULL, NULL, true),
('PORCION DE FRUTA EXTRA', 'PORCION ADICIONAL DE FRUTA FRESCA DE TEMPORADA', 15.00, 8.00, 0, 0, 'FLAT', NULL, NULL, true),
('BEBIDA CALIENTE EXTRA', 'CAFE, TE O CHOCOLATE CALIENTE ADICIONAL', 10.00, 5.00, 0, 0, 'FLAT', NULL, NULL, true),
('JUGO NATURAL EXTRA', 'VASO DE JUGO NATURAL DE NARANJA O FRUTA DE TEMPORADA', 15.00, 8.00, 0, 0, 'FLAT', NULL, NULL, true)
) AS v(name, description, price, cost, quantity, restock_level, pricing_type, time_unit, time_interval, active)
WHERE c.name = 'Alimentación';

-- Re-enable triggers
SET session_replication_role = DEFAULT;

-- ============================================================================
-- VERIFICATION QUERIES (for debugging - uncomment to verify data)
-- ============================================================================
-- SELECT 'Users by role:' as info;
-- SELECT r.code as role, COUNT(ur.user_id) as user_count
-- FROM roles r
-- LEFT JOIN user_roles ur ON r.id = ur.role_id
-- GROUP BY r.code ORDER BY r.code;

-- SELECT 'Total patients:' as info, COUNT(*) as count FROM patients;
-- SELECT 'Total emergency contacts:' as info, COUNT(*) as count FROM emergency_contacts;

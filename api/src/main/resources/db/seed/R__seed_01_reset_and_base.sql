-- ============================================================================
-- SEED FILE 01: Reset, Roles, Users, Patients, Emergency Contacts
-- ============================================================================
-- This file truncates all tables and reseeds base data.
-- WARNING: DESTRUCTIVE - DO NOT use in production!
-- Default password for all users: admin123
-- Last updated: 2026-05-29 (admin no longer carries RESIDENT_DOCTOR; ADMIN admits via residentId — mirrors V122)
--
-- !!! READ BEFORE EDITING ANY R__seed_*.sql FILE !!!
-- This file TRUNCATEs patients/admissions/vitals/notes/meds/billing tables.
-- Re-inserts of those rows live in R__seed_02..08. Flyway re-runs a
-- repeatable migration only when ITS OWN checksum changes — so editing
-- ONLY this file wipes data without re-running the sibling files that
-- repopulate it (we hit this in PR #53). Whenever any R__seed_*.sql is
-- modified, bump the SEED-BUNDLE-VERSION line below in ALL nine files
-- (01, 02, 02b, 03, 04, 05, 06, 07, 08) so they re-run together.
-- SEED-BUNDLE-VERSION: 2026-06-10-discharge-admin-resident-only
-- ============================================================================

SET session_replication_role = replica;

-- ============================================================================
-- STEP 1: TRUNCATE TABLES (in dependency order - children first)
-- ============================================================================
-- NOTE: rooms and triage_codes are reference data managed by versioned migrations
-- (V021, V056). They must NOT be truncated here - if seed files fail after this
-- truncation but before R__seed_02 re-inserts them, the app becomes non-functional.
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
VALUES ('PSICOLOGO', 'Psicólogo', 'Profesionales de salud mental', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Add MEDICO_RESIDENTE role if it doesn't exist (mirrors V114)
INSERT INTO roles (code, name, description, is_system, created_at, updated_at)
VALUES ('MEDICO_RESIDENTE', 'Médico Residente', 'Médico residente a cargo de las admisiones', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Add AUXILIARY_NURSE role if it doesn't exist (mirrors V117)
INSERT INTO roles (code, name, description, is_system, created_at, updated_at)
VALUES ('AUXILIAR_ENFERMERIA', 'Auxiliar de Enfermería', 'Auxiliar de enfermería — solo notas y signos vitales', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Add MAINTENANCE role if it doesn't exist (mirrors V119)
INSERT INTO roles (code, name, description, is_system, created_at, updated_at)
VALUES ('MANTENIMIENTO', 'Mantenimiento', 'Mantenimiento — gestiona bodegas de mantenimiento, traslada insumos y carga consumibles no médicos', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Ensure the six warehouses exist (mirrors V119; warehouses survive the reset
-- but re-assert so a fresh DB seeded without migrations is still consistent).
INSERT INTO warehouses (code, name, description, active, created_at, updated_at) VALUES
('ADMINISTRACION',  'Administración',  'Bodega maestra / de recepción. Las entregas llegan aquí.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ENFERMERIA',      'Enfermería',      'Bodega de enfermería.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MANTENIMIENTO_1', 'Mantenimiento 1', 'Bodega de mantenimiento 1.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MANTENIMIENTO_2', 'Mantenimiento 2', 'Bodega de mantenimiento 2.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('COCINA',          'Cocina',          'Bodega de cocina.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PSICOLOGIA',      'Psicología',      'Bodega de psicología.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Role -> default-warehouse mapping (mirrors V119).
INSERT INTO role_default_warehouses (role_id, warehouse_id, created_at, updated_at)
SELECT r.id, w.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN warehouses w
WHERE ((r.code IN ('ENFERMERO', 'AUXILIAR_ENFERMERIA', 'JEFE_ENFERMERIA') AND w.code = 'ENFERMERIA')
    OR (r.code = 'PSICOLOGO' AND w.code = 'PSICOLOGIA'))
  AND r.deleted_at IS NULL AND w.deleted_at IS NULL
ON CONFLICT DO NOTHING;

-- ============================================================================
-- STEP 3: REBUILD ROLE PERMISSIONS
-- ============================================================================
-- This section must replicate ALL permission grants from versioned migrations
-- V020-V093, since the seed file runs after migrations and the DELETE below
-- wipes any migration-granted role_permissions.
-- ============================================================================
DELETE FROM role_permissions;

-- ADMIN gets all permissions
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMINISTRADOR' AND r.deleted_at IS NULL AND p.deleted_at IS NULL;

-- USER gets minimal permissions
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'USUARIO' AND p.code IN ('user:read') AND r.deleted_at IS NULL AND p.deleted_at IS NULL;

-- ADMINISTRATIVE_STAFF: patient management + admission + documents
-- Sources: V020, V025, V034, V091, V093
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'PERSONAL_ADMINISTRATIVO'
  AND p.code IN (
    'user:read',
    'patient:create', 'patient:read', 'patient:update',
    'patient:upload-id', 'patient:view-id',
    'triage-code:read', 'room:read', 'room:occupancy-view',
    'admission:create', 'admission:read', 'admission:update',
    'admission:upload-consent', 'admission:view-consent',
    'admission:view-documents', 'admission:upload-documents', 'admission:download-documents',
    'document-type:read',
    'billing:read', 'invoice:read',
    'medical-order:read', 'medical-order:authorize'
  )
  AND r.deleted_at IS NULL AND p.deleted_at IS NULL;

-- DOCTOR: clinical + nursing + psychotherapy read + billing read + MAR read
-- Sources: V020, V025, V038, V042, V045, V061, V066, V093
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'MEDICO'
  AND p.code IN (
    'user:read',
    'patient:read', 'patient:update',
    'triage-code:read', 'room:read',
    'admission:read',
    'admission:view-consent', 'admission:view-documents', 'admission:download-documents',
    'clinical-history:create', 'clinical-history:read',
    'progress-note:create', 'progress-note:read',
    'medical-order:create', 'medical-order:read', 'medical-order:discontinue',
    'medical-order:emergency-authorize', 'medical-order:mark-in-progress',
    'nursing-note:read', 'nursing-note:create',
    'vital-sign:read', 'vital-sign:create',
    'psychotherapy-activity:read', 'psychotherapy-category:read',
    'billing:read',
    'medication-administration:read',
    'lab-catalog:read'
  )
  AND r.deleted_at IS NULL AND p.deleted_at IS NULL;

-- RESIDENT_DOCTOR: clones DOCTOR plus admission:create (V114) and
-- admission:discharge (V131). Residents may discharge with a mandatory note,
-- but they do not inherit the broader admission:update permission.
-- lab-catalog:read is inherited via the DOCTOR clone below (V125).
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT
    (SELECT id FROM roles WHERE code = 'MEDICO_RESIDENTE'),
    rp.permission_id,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id
WHERE r.code = 'MEDICO';

INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT
    (SELECT id FROM roles WHERE code = 'MEDICO_RESIDENTE'),
    p.id,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM permissions p
WHERE p.code = 'admission:create';

INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT
    (SELECT id FROM roles WHERE code = 'MEDICO_RESIDENTE'),
    p.id,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM permissions p
WHERE p.code = 'admission:discharge';

-- RESIDENT_DOCTOR also gets room:occupancy-view (V118): the bed occupancy
-- screen is the default dashboard for residents. Not cloned from DOCTOR, which
-- is deliberately excluded from this permission.
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT
    (SELECT id FROM roles WHERE code = 'MEDICO_RESIDENTE'),
    p.id,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM permissions p
WHERE p.code = 'room:occupancy-view';

-- PSYCHOLOGIST: psychotherapy + patient/admission read + psychometric medical orders
-- Sources: V042 + base patient/admission access + V116 (medical-order:read,
--   medical-order:mark-in-progress, medical-order:upload-document — scoped to
--   PRUEBAS_PSICOMETRICAS at the service layer)
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'PSICOLOGO'
  AND p.code IN (
    'patient:read', 'patient:update',
    'triage-code:read', 'room:read',
    'admission:read',
    'admission:view-consent', 'admission:view-documents', 'admission:download-documents',
    'psychotherapy-activity:create', 'psychotherapy-activity:read',
    'psychotherapy-category:read',
    'billing:read',
    'medical-order:read', 'medical-order:mark-in-progress', 'medical-order:upload-document'
  )
  AND r.deleted_at IS NULL AND p.deleted_at IS NULL;

-- NURSE: nursing + clinical read + MAR + psychotherapy read + billing read
-- Sources: V020, V025, V038, V042, V045, V061, V066, V091
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ENFERMERO'
  AND p.code IN (
    'user:read',
    'patient:read',
    'triage-code:read', 'room:read', 'room:occupancy-view',
    'admission:read',
    'admission:view-consent', 'admission:view-documents', 'admission:download-documents',
    'clinical-history:read',
    'progress-note:create', 'progress-note:read',
    'medical-order:read', 'medical-order:mark-in-progress',
    'nursing-note:read', 'nursing-note:create',
    'vital-sign:read', 'vital-sign:create',
    'psychotherapy-activity:read', 'psychotherapy-category:read',
    'billing:read',
    'medication-administration:create', 'medication-administration:read'
  )
  AND r.deleted_at IS NULL AND p.deleted_at IS NULL;

-- AUXILIARY_NURSE: vital signs + nursing notes only, plus read-only context.
-- Explicit SUBSET of NURSE — no medication-administration:create,
-- medical-order:mark-in-progress, medical-order:upload-document,
-- progress-note:create, admission:update, or admission:discharge. Service-layer
-- guards enforce the three denied clinical actions; discharge/edit denial is by
-- omission of the grants.
-- Source: V117. Spec: docs/features/nursing-roles-split.md.
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'AUXILIAR_ENFERMERIA'
  AND p.code IN (
    'nursing-note:read', 'nursing-note:create',
    'vital-sign:read', 'vital-sign:create',
    'medication-administration:read',
    'medical-order:read',
    'progress-note:read',
    'clinical-history:read',
    'patient:read',
    'admission:read',
    'room:occupancy-view'
  )
  AND r.deleted_at IS NULL AND p.deleted_at IS NULL;

-- CHIEF_NURSE: same as NURSE + patient:update, admission:update.
-- Note: progress-note:update, nursing-note:update, and vital-sign:update are
-- intentionally NOT granted — all three record types are admin-only update per
-- medical-psychiatric-record.md v1.4 and nursing-module.md v1.4 (V096 + V097).
-- Sources: V020, V025, V038 + V096 (progress-note:create grant for chief nurse),
--   V042, V045 + V096 (nursing-note:update revocation) + V097 (vital-sign:update revocation),
--   V066, V091
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'JEFE_ENFERMERIA'
  AND p.code IN (
    'user:read',
    'patient:read', 'patient:update',
    'triage-code:read', 'room:read', 'room:occupancy-view',
    'admission:read', 'admission:update',
    'admission:view-consent', 'admission:view-documents', 'admission:download-documents',
    'clinical-history:read',
    'progress-note:create', 'progress-note:read',
    'medical-order:read', 'medical-order:mark-in-progress',
    'nursing-note:read', 'nursing-note:create',
    'vital-sign:read', 'vital-sign:create',
    'psychotherapy-activity:read', 'psychotherapy-category:read',
    'billing:read',
    'medication-administration:create', 'medication-administration:read'
  )
  AND r.deleted_at IS NULL AND p.deleted_at IS NULL;

-- Warehouse permissions (mirrors V119 grant matrix). ADMIN already has all via
-- the CROSS JOIN above; these add the per-role subsets.
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'MANTENIMIENTO'
  AND p.code IN ('warehouse:read', 'warehouse-transfer:create', 'warehouse-transfer:read',
                 'warehouse-charge:create', 'inventory-item:read', 'admission:read', 'patient:read')
  AND r.deleted_at IS NULL AND p.deleted_at IS NULL;

INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'PERSONAL_ADMINISTRATIVO'
  AND p.code IN ('warehouse:read', 'warehouse-transfer:create', 'warehouse-transfer:read',
                 'warehouse-transfer:receive', 'warehouse-charge:create')
  AND r.deleted_at IS NULL AND p.deleted_at IS NULL;

INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'JEFE_ENFERMERIA'
  AND p.code IN ('warehouse:read', 'warehouse-transfer:create', 'warehouse-transfer:read',
                 'warehouse-transfer:receive')
  AND r.deleted_at IS NULL AND p.deleted_at IS NULL;

INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ENFERMERO'
  AND p.code IN ('warehouse:read', 'warehouse-transfer:read', 'warehouse-transfer:receive')
  AND r.deleted_at IS NULL AND p.deleted_at IS NULL;

INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'AUXILIAR_ENFERMERIA'
  AND p.code IN ('warehouse:read', 'warehouse-transfer:read')
  AND r.deleted_at IS NULL AND p.deleted_at IS NULL;

INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('MEDICO', 'MEDICO_RESIDENTE')
  AND p.code = 'warehouse:read'
  AND r.deleted_at IS NULL AND p.deleted_at IS NULL;

INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'PSICOLOGO'
  AND p.code IN ('warehouse:read', 'warehouse-transfer:create', 'warehouse-transfer:read',
                 'warehouse-transfer:receive')
  AND r.deleted_at IS NULL AND p.deleted_at IS NULL;

-- ============================================================================
-- STEP 4: CREATE USERS
-- ============================================================================
-- Password for all test users: admin123 (BCrypt hash)
-- $2a$10$QpZ5b.hF/vk524E/zB/nFekW7t1E1t5fvXjdaazIHKGa8czDC1PrK

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

-- MEDICO_RESIDENTE role users (2)
INSERT INTO users (username, email, password_hash, first_name, last_name, salutation, status, email_verified, must_change_password, created_at, updated_at) VALUES
('resident1', 'resident1@example.com', '$2b$10$PYXULrV.BlNnIPSz8HRFJeId5axQ/qoAQNhEldlY/H7xlqIpH35YC', 'Andrea', 'Pineda', 'DRA', 'ACTIVE', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('resident2', 'resident2@example.com', '$2b$10$PYXULrV.BlNnIPSz8HRFJeId5axQ/qoAQNhEldlY/H7xlqIpH35YC', 'Javier', 'Cabrera', 'DR', 'ACTIVE', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

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

-- AUXILIARY_NURSE role user (1) — QA exercises the restricted nursing flow
INSERT INTO users (username, email, password_hash, first_name, last_name, salutation, status, email_verified, must_change_password, created_at, updated_at) VALUES
('aux_nurse', 'aux_nurse@example.com', '$2b$10$PYXULrV.BlNnIPSz8HRFJeId5axQ/qoAQNhEldlY/H7xlqIpH35YC', 'Lucia', 'Gomez', 'SRTA', 'ACTIVE', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- MAINTENANCE role user (1) — assigned to MANTENIMIENTO_1 below for the bodega flow
INSERT INTO users (username, email, password_hash, first_name, last_name, salutation, status, email_verified, must_change_password, created_at, updated_at) VALUES
('maint1', 'maint1@example.com', '$2b$10$PYXULrV.BlNnIPSz8HRFJeId5axQ/qoAQNhEldlY/H7xlqIpH35YC', 'Pedro', 'Castillo', 'SR', 'ACTIVE', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ============================================================================
-- STEP 5: ASSIGN ROLES TO USERS
-- ============================================================================
-- Admin gets ADMIN role
INSERT INTO user_roles (user_id, role_id, created_at, updated_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u, roles r
WHERE u.username = 'admin' AND r.code = 'ADMINISTRADOR';

-- NOTE: admin does NOT carry RESIDENT_DOCTOR. ADMIN is a first-class exception
-- in AdmissionService.resolveResident(): an admin registering an admission must
-- explicitly pick the resident doctor (residentId in the request). Admin keeps
-- admission:create via the "ADMIN gets all permissions" CROSS JOIN above.

-- USER role assignments
INSERT INTO user_roles (user_id, role_id, created_at, updated_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u, roles r
WHERE u.username IN ('user1', 'user2') AND r.code = 'USUARIO';

-- ADMINISTRATIVE_STAFF role assignments
INSERT INTO user_roles (user_id, role_id, created_at, updated_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u, roles r
WHERE u.username IN ('staff1', 'staff2') AND r.code = 'PERSONAL_ADMINISTRATIVO';

-- DOCTOR role assignments
INSERT INTO user_roles (user_id, role_id, created_at, updated_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u, roles r
WHERE u.username IN ('doctor1', 'doctor2') AND r.code = 'MEDICO';

-- MEDICO_RESIDENTE role assignments
INSERT INTO user_roles (user_id, role_id, created_at, updated_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u, roles r
WHERE u.username IN ('resident1', 'resident2') AND r.code = 'MEDICO_RESIDENTE';

-- PSYCHOLOGIST role assignments
INSERT INTO user_roles (user_id, role_id, created_at, updated_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u, roles r
WHERE u.username IN ('psych1', 'psych2') AND r.code = 'PSICOLOGO';

-- NURSE role assignments
INSERT INTO user_roles (user_id, role_id, created_at, updated_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u, roles r
WHERE u.username IN ('nurse1', 'nurse2') AND r.code = 'ENFERMERO';

-- CHIEF_NURSE role assignments
INSERT INTO user_roles (user_id, role_id, created_at, updated_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u, roles r
WHERE u.username IN ('chiefnurse1', 'chiefnurse2') AND r.code = 'JEFE_ENFERMERIA';

-- AUXILIARY_NURSE role assignment
INSERT INTO user_roles (user_id, role_id, created_at, updated_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u, roles r
WHERE u.username = 'aux_nurse' AND r.code = 'AUXILIAR_ENFERMERIA';

-- MAINTENANCE role assignment + warehouse assignment (MANTENIMIENTO_1)
INSERT INTO user_roles (user_id, role_id, created_at, updated_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u, roles r
WHERE u.username = 'maint1' AND r.code = 'MANTENIMIENTO';

INSERT INTO user_warehouses (user_id, warehouse_id, created_at, updated_at)
SELECT u.id, w.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u, warehouses w
WHERE u.username = 'maint1' AND w.code = 'MANTENIMIENTO_1'
ON CONFLICT DO NOTHING;

-- ============================================================================
-- STEP 6: CREATE TEST PATIENTS (20 patients)
-- ============================================================================
-- Note: date_of_birth replaces age (V098). Birthdates are placeholders (Jan 1 of
-- birth year) computed so the derived age matches the previous fixture value.
INSERT INTO patients (first_name, last_name, date_of_birth, sex, gender, marital_status, religion, education_level, occupation, address, email, id_document_number, notes, created_at, updated_at) VALUES
-- Patient 1-5
('Juan', 'Pérez González', make_date(EXTRACT(YEAR FROM CURRENT_DATE)::int - 35, 1, 1), 'MALE', 'Male', 'MARRIED', 'Catholic', 'UNIVERSITY', 'Engineer', 'Zone 10, Guatemala City', 'juan.perez@email.com', 'DPI-1234567890101', 'Regular checkup patient', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Maria', 'Santos López', make_date(EXTRACT(YEAR FROM CURRENT_DATE)::int - 28, 1, 1), 'FEMALE', 'Female', 'SINGLE', 'Evangelical', 'TECHNICAL', 'Accountant', 'Zone 1, Guatemala City', 'maria.santos@email.com', 'DPI-2345678901202', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Pedro', 'García Hernández', make_date(EXTRACT(YEAR FROM CURRENT_DATE)::int - 45, 1, 1), 'MALE', 'Male', 'DIVORCED', 'Catholic', 'SECONDARY', 'Mechanic', 'Zone 7, Guatemala City', 'pedro.garcia@email.com', 'DPI-3456789012303', 'Diabetic patient, requires special attention', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Ana', 'Martínez Ruiz', make_date(EXTRACT(YEAR FROM CURRENT_DATE)::int - 62, 1, 1), 'FEMALE', 'Female', 'WIDOWED', 'Catholic', 'PRIMARY', 'Retired Teacher', 'Zone 12, Guatemala City', 'ana.martinez@email.com', 'DPI-4567890123404', 'Hypertensión history', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Luis', 'Morales Castro', make_date(EXTRACT(YEAR FROM CURRENT_DATE)::int - 22, 1, 1), 'MALE', 'Male', 'SINGLE', 'None', 'UNIVERSITY', 'Student', 'Zone 14, Guatemala City', 'luis.morales@email.com', 'DPI-5678901234505', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Patient 6-10
('Carmen', 'Flores Mejía', make_date(EXTRACT(YEAR FROM CURRENT_DATE)::int - 38, 1, 1), 'FEMALE', 'Female', 'MARRIED', 'Catholic', 'POSTGRADUATE', 'Lawyer', 'Zone 15, Guatemala City', 'carmen.flores@email.com', 'DPI-6789012345606', 'Allergic to penicillin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Roberto', 'Díaz Vargas', make_date(EXTRACT(YEAR FROM CURRENT_DATE)::int - 55, 1, 1), 'MALE', 'Male', 'MARRIED', 'Evangelical', 'TECHNICAL', 'Electrician', 'Mixco, Guatemala', 'roberto.diaz@email.com', 'DPI-7890123456707', 'Heart condition, on medication', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Sofia', 'Ramírez Paz', make_date(EXTRACT(YEAR FROM CURRENT_DATE)::int - 31, 1, 1), 'FEMALE', 'Female', 'SEPARATED', 'Catholic', 'UNIVERSITY', 'Nurse', 'Villa Nueva, Guatemala', 'sofia.ramirez@email.com', 'DPI-8901234567808', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Miguel', 'Torres Luna', make_date(EXTRACT(YEAR FROM CURRENT_DATE)::int - 48, 1, 1), 'MALE', 'Male', 'MARRIED', 'Catholic', 'SECONDARY', 'Driver', 'Amatitlán, Guatemala', 'miguel.torres@email.com', 'DPI-9012345678909', 'Back pain issues', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Elena', 'Sánchez Rivas', make_date(EXTRACT(YEAR FROM CURRENT_DATE)::int - 26, 1, 1), 'FEMALE', 'Female', 'SINGLE', 'Evangelical', 'UNIVERSITY', 'Marketing Specialist', 'Zone 4, Guatemala City', 'elena.sanchez@email.com', 'DPI-0123456789010', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Patient 11-15
('Francisco', 'Mendoza Aguilar', make_date(EXTRACT(YEAR FROM CURRENT_DATE)::int - 70, 1, 1), 'MALE', 'Male', 'WIDOWED', 'Catholic', 'PRIMARY', 'Retired Farmer', 'Antigua Guatemala, Sacatepéquez', 'francisco.mendoza@email.com', 'DPI-1234509876111', 'Arthritis, limited mobility', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Isabella', 'Cruz Monzón', make_date(EXTRACT(YEAR FROM CURRENT_DATE)::int - 19, 1, 1), 'FEMALE', 'Female', 'SINGLE', 'Catholic', 'SECONDARY', 'Student', 'Quetzaltenango', 'isabella.cruz@email.com', 'DPI-2345610987212', 'First visit', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Andres', 'Ortiz Barrios', make_date(EXTRACT(YEAR FROM CURRENT_DATE)::int - 42, 1, 1), 'MALE', 'Male', 'MARRIED', 'Evangelical', 'TECHNICAL', 'Carpenter', 'Escuintla', 'andres.ortiz@email.com', 'DPI-3456721098313', 'Occupational injury follow-up', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Gabriela', 'Reyes Soto', make_date(EXTRACT(YEAR FROM CURRENT_DATE)::int - 33, 1, 1), 'FEMALE', 'Female', 'MARRIED', 'Catholic', 'POSTGRADUATE', 'Doctor', 'Zone 9, Guatemala City', 'gabriela.reyes@email.com', 'DPI-4567832109414', 'Pregnancy monitoring', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Oscar', 'Vásquez Pineda', make_date(EXTRACT(YEAR FROM CURRENT_DATE)::int - 58, 1, 1), 'MALE', 'Male', 'DIVORCED', 'None', 'UNIVERSITY', 'Businessman', 'Zone 16, Guatemala City', 'oscar.vasquez@email.com', 'DPI-5678943210515', 'Annual executive checkup', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Patient 16-20
('Patricia', 'Herrera Godínez', make_date(EXTRACT(YEAR FROM CURRENT_DATE)::int - 44, 1, 1), 'FEMALE', 'Female', 'MARRIED', 'Catholic', 'TECHNICAL', 'Secretary', 'Zone 5, Guatemala City', 'patricia.herrera@email.com', 'DPI-6789054321616', 'Chronic migraines', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Diego', 'Castillo Moreno', make_date(EXTRACT(YEAR FROM CURRENT_DATE)::int - 29, 1, 1), 'MALE', 'Male', 'SINGLE', 'Evangelical', 'UNIVERSITY', 'Software Developer', 'Zone 11, Guatemala City', 'diego.castillo@email.com', 'DPI-7890165432717', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Valentina', 'Estrada Juárez', make_date(EXTRACT(YEAR FROM CURRENT_DATE)::int - 52, 1, 1), 'FEMALE', 'Female', 'MARRIED', 'Catholic', 'SECONDARY', 'Cook', 'San Juan Sacatepéquez', 'valentina.estrada@email.com', 'DPI-8901276543818', 'Gastritis, dietary restrictions', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Alejandro', 'Núñez Córdova', make_date(EXTRACT(YEAR FROM CURRENT_DATE)::int - 36, 1, 1), 'MALE', 'Male', 'MARRIED', 'Catholic', 'UNIVERSITY', 'Architect', 'Zone 13, Guatemala City', 'alejandro.nunez@email.com', 'DPI-9012387654919', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Lucia', 'Álvarez Monroy', make_date(EXTRACT(YEAR FROM CURRENT_DATE)::int - 24, 1, 1), 'FEMALE', 'Female', 'SINGLE', 'Evangelical', 'TECHNICAL', 'Dental Assistant', 'Zone 18, Guatemala City', 'lucia.alvarez@email.com', 'DPI-0123498765020', 'Regular dental patient referral', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

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

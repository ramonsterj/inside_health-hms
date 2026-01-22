-- Add clinical roles for patient management
INSERT INTO roles (code, name, description, is_system, created_at, updated_at) VALUES
('ADMINISTRATIVE_STAFF', 'Administrative Staff', 'Front desk and registration staff', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('DOCTOR', 'Doctor', 'Medical doctors', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('NURSE', 'Nurse', 'Nursing staff', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CHIEF_NURSE', 'Chief Nurse', 'Head of nursing department', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

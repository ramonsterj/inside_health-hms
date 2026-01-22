-- Add test users for clinical roles
-- Password for all users: insidetest01 (BCrypt hash)

-- Insert test users
INSERT INTO users (
    username,
    email,
    password_hash,
    first_name,
    last_name,
    status,
    email_verified,
    created_at,
    updated_at
) VALUES
-- Administrative Staff
('staff01', 'staff01@insidehealth.test', '$2b$10$A9GpE6a9LVgwF6ZFXF4N/OKvaGq2eqzkkTeDSr9EyCLRTKJW/xN9i', 'Staff', 'Test', 'ACTIVE', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Doctor
('doctor01', 'doctor01@insidehealth.test', '$2b$10$A9GpE6a9LVgwF6ZFXF4N/OKvaGq2eqzkkTeDSr9EyCLRTKJW/xN9i', 'Doctor', 'Test', 'ACTIVE', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Nurse
('nurse01', 'nurse01@insidehealth.test', '$2b$10$A9GpE6a9LVgwF6ZFXF4N/OKvaGq2eqzkkTeDSr9EyCLRTKJW/xN9i', 'Nurse', 'Test', 'ACTIVE', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Chief Nurse
('chiefnurse01', 'chiefnurse01@insidehealth.test', '$2b$10$A9GpE6a9LVgwF6ZFXF4N/OKvaGq2eqzkkTeDSr9EyCLRTKJW/xN9i', 'Chief Nurse', 'Test', 'ACTIVE', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (email) DO NOTHING;

-- Assign roles to test users
-- staff01 -> ADMINISTRATIVE_STAFF
INSERT INTO user_roles (user_id, role_id, created_at, updated_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u
CROSS JOIN roles r
WHERE u.username = 'staff01' AND r.code = 'ADMINISTRATIVE_STAFF'
ON CONFLICT (user_id, role_id) DO NOTHING;

-- doctor01 -> DOCTOR
INSERT INTO user_roles (user_id, role_id, created_at, updated_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u
CROSS JOIN roles r
WHERE u.username = 'doctor01' AND r.code = 'DOCTOR'
ON CONFLICT (user_id, role_id) DO NOTHING;

-- nurse01 -> NURSE
INSERT INTO user_roles (user_id, role_id, created_at, updated_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u
CROSS JOIN roles r
WHERE u.username = 'nurse01' AND r.code = 'NURSE'
ON CONFLICT (user_id, role_id) DO NOTHING;

-- chiefnurse01 -> CHIEF_NURSE
INSERT INTO user_roles (user_id, role_id, created_at, updated_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u
CROSS JOIN roles r
WHERE u.username = 'chiefnurse01' AND r.code = 'CHIEF_NURSE'
ON CONFLICT (user_id, role_id) DO NOTHING;

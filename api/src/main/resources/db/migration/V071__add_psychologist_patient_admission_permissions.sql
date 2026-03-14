-- Grant patient:read and admission:read permissions to PSYCHOLOGIST role
-- Psychologists need to view admitted patients and active admissions
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'PSYCHOLOGIST' AND p.code IN (
    'patient:read',
    'admission:read'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

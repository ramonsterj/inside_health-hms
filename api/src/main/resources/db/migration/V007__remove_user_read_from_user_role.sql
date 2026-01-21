-- Remove user:read permission from USER role
-- Regular users should only be able to access their own profile, not list all users
DELETE FROM role_permissions
WHERE role_id = (SELECT id FROM roles WHERE code = 'USER')
AND permission_id = (SELECT id FROM permissions WHERE code = 'user:read');

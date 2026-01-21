-- Seed all permissions (resource:action format)
INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
-- User permissions
('user:create', 'Create User', 'Create new user accounts', 'user', 'create', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('user:read', 'Read User', 'View user details', 'user', 'read', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('user:update', 'Update User', 'Modify user information', 'user', 'update', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('user:delete', 'Delete User', 'Soft delete user accounts', 'user', 'delete', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('user:reset-password', 'Reset User Password', 'Reset password for any user', 'user', 'reset-password', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('user:list-deleted', 'List Deleted Users', 'View soft-deleted user accounts', 'user', 'list-deleted', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('user:restore', 'Restore User', 'Restore soft-deleted user accounts', 'user', 'restore', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Role permissions
('role:create', 'Create Role', 'Create new roles', 'role', 'create', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('role:read', 'Read Role', 'View role details', 'role', 'read', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('role:update', 'Update Role', 'Modify role information', 'role', 'update', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('role:delete', 'Delete Role', 'Delete non-system roles', 'role', 'delete', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('role:assign-permissions', 'Assign Permissions', 'Modify role permissions', 'role', 'assign-permissions', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Audit permissions
('audit:read', 'Read Audit Logs', 'View audit log entries', 'audit', 'read', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Seed system roles
INSERT INTO roles (code, name, description, is_system, created_at, updated_at) VALUES
('ADMIN', 'Administrator', 'Full system access with all permissions', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USER', 'Standard User', 'Basic user with limited permissions', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Assign ALL permissions to ADMIN role
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMIN';

-- Assign minimal permissions to USER role (basic read access only)
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'USER' AND p.code IN ('user:read');

-- Assign ADMIN role to existing admin user (based on old role column)
INSERT INTO user_roles (user_id, role_id, created_at, updated_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u
CROSS JOIN roles r
WHERE u.role = 'ADMIN' AND r.code = 'ADMIN'
ON CONFLICT (user_id, role_id) DO NOTHING;

-- Assign USER role to all other users
INSERT INTO user_roles (user_id, role_id, created_at, updated_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u
CROSS JOIN roles r
WHERE u.role = 'USER' AND r.code = 'USER'
ON CONFLICT (user_id, role_id) DO NOTHING;

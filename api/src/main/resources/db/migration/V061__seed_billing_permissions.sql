-- Billing permissions
INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('billing:read', 'Read Billing', 'View patient charges and balances', 'billing', 'read', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('billing:create', 'Create Billing Charge', 'Create manual patient charges', 'billing', 'create', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('billing:adjust', 'Create Billing Adjustment', 'Create billing adjustments and credits', 'billing', 'adjust', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Invoice permissions
INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('invoice:read', 'Read Invoice', 'View invoices', 'invoice', 'read', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('invoice:create', 'Create Invoice', 'Generate invoices', 'invoice', 'create', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Assign all billing and invoice permissions to ADMIN role
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMIN' AND p.resource IN ('billing', 'invoice');

-- Assign billing:read to DOCTOR and NURSE roles
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('DOCTOR', 'NURSE') AND p.code = 'billing:read';

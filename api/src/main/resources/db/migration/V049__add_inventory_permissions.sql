-- Inventory Category permissions
INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('inventory-category:create', 'Create Inventory Category', 'Create inventory categories', 'inventory-category', 'create', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('inventory-category:read', 'Read Inventory Category', 'View inventory categories', 'inventory-category', 'read', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('inventory-category:update', 'Update Inventory Category', 'Modify inventory categories', 'inventory-category', 'update', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('inventory-category:delete', 'Delete Inventory Category', 'Delete inventory categories', 'inventory-category', 'delete', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Inventory Item permissions
INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('inventory-item:create', 'Create Inventory Item', 'Create inventory items', 'inventory-item', 'create', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('inventory-item:read', 'Read Inventory Item', 'View inventory items', 'inventory-item', 'read', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('inventory-item:update', 'Update Inventory Item', 'Modify inventory items', 'inventory-item', 'update', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('inventory-item:delete', 'Delete Inventory Item', 'Delete inventory items', 'inventory-item', 'delete', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Inventory Movement permissions
INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('inventory-movement:create', 'Create Inventory Movement', 'Record stock movements', 'inventory-movement', 'create', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('inventory-movement:read', 'Read Inventory Movement', 'View stock movement history', 'inventory-movement', 'read', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Assign ALL inventory permissions to ADMIN role
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMIN' AND p.resource IN ('inventory-category', 'inventory-item', 'inventory-movement');

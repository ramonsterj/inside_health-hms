-- V104: Pharmacy & Inventory Evolution — Phase 1e
-- Seeds the eight pharmacy/lot permissions and grants them per the
-- proposed matrix in pharmacy-and-inventory-evolution.md § Authorization.

INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('medication:read',           'View Medications',          'View structured medication catalog',                 'medication',     'read',           CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('medication:create',         'Create Medication',         'Attach MedicationDetails to a DRUG item',            'medication',     'create',         CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('medication:update',         'Update Medication',         'Edit MedicationDetails fields',                      'medication',     'update',         CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('medication:bulk-import',    'Bulk Import Medications',   'Upload the customer workbook as CSV',                'medication',     'bulk-import',    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('medication:expiry-report',  'View Expiry Report',        'View the color-coded expiry dashboard',              'medication',     'expiry-report',  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('inventory-lot:read',        'View Inventory Lots',       'List and view lots for an item',                     'inventory-lot',  'read',           CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('inventory-lot:create',      'Create Inventory Lot',      'Register a new lot (entry)',                         'inventory-lot',  'create',         CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('inventory-lot:update',      'Update Inventory Lot',      'Edit lot metadata, recall a lot, override FEFO',     'inventory-lot',  'update',         CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- ADMIN gets all eight.
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMIN'
  AND p.code IN ('medication:read', 'medication:create', 'medication:update',
                 'medication:bulk-import', 'medication:expiry-report',
                 'inventory-lot:read', 'inventory-lot:create', 'inventory-lot:update')
  AND r.deleted_at IS NULL AND p.deleted_at IS NULL
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ADMINISTRATIVE_STAFF and CHIEF_NURSE: read + expiry-report.
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMINISTRATIVE_STAFF', 'CHIEF_NURSE')
  AND p.code IN ('medication:read', 'inventory-lot:read', 'medication:expiry-report')
  AND r.deleted_at IS NULL AND p.deleted_at IS NULL
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- NURSE and DOCTOR: read-only access.
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('NURSE', 'DOCTOR')
  AND p.code IN ('medication:read', 'inventory-lot:read')
  AND r.deleted_at IS NULL AND p.deleted_at IS NULL
ON CONFLICT (role_id, permission_id) DO NOTHING;

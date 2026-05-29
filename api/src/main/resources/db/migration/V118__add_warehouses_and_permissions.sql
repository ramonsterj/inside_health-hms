-- ============================================================================
-- V118: Warehouse-Scoped Inventory Management (Bodegas) — schema + seed
-- ============================================================================
-- Introduces named warehouses ("bodegas") with strict isolation: each warehouse
-- owns its own stock and can only dispense from itself. Adds inter-warehouse
-- transfers, non-medical consumable charges, the MAINTENANCE role, and the
-- data-driven role -> default-warehouse mapping.
--
-- The catalog stays single (one row per SKU). What becomes warehouse-scoped is
-- STOCK (inventory_warehouse_stock), LOTS (where they physically sit), and
-- MOVEMENTS (which bodega they entered/left from — new warehouse_id column).
--
-- Spec: docs/features/warehouse-inventory-management.md v1.0 (migrations
-- renumbered V117->V118, V118->V119, V119->V120 because V117 is AUXILIARY_NURSE).
-- This migration is additive/deploy-safe; V119 backfills, V120 drops legacy cols.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. Tables
-- ---------------------------------------------------------------------------

CREATE TABLE warehouses (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(50)  NOT NULL UNIQUE,
    name          VARCHAR(255) NOT NULL,
    description   TEXT,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    BIGINT,
    updated_by    BIGINT,
    deleted_at    TIMESTAMP
);
CREATE INDEX idx_warehouses_deleted_at ON warehouses(deleted_at);
CREATE INDEX idx_warehouses_active     ON warehouses(active) WHERE deleted_at IS NULL;

CREATE TABLE inventory_warehouse_stock (
    id            BIGSERIAL PRIMARY KEY,
    item_id       BIGINT        NOT NULL REFERENCES inventory_items(id),
    warehouse_id  BIGINT        NOT NULL REFERENCES warehouses(id),
    lot_id        BIGINT                 REFERENCES inventory_lots(id),
    quantity      NUMERIC(14,3) NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    BIGINT,
    updated_by    BIGINT,
    deleted_at    TIMESTAMP
);
CREATE INDEX idx_iws_item_wh    ON inventory_warehouse_stock(item_id, warehouse_id);
CREATE INDEX idx_iws_warehouse  ON inventory_warehouse_stock(warehouse_id);
CREATE INDEX idx_iws_lot        ON inventory_warehouse_stock(lot_id);
CREATE INDEX idx_iws_deleted_at ON inventory_warehouse_stock(deleted_at);
-- One row per (item, warehouse, lot). lot_id IS NULL for non-lot-tracked items;
-- COALESCE(lot_id, -1) keeps NULL lots colliding deterministically (same idiom
-- as inventory_lots' NULLS NOT DISTINCT index) so the upsert stays tight.
CREATE UNIQUE INDEX uq_iws_item_wh_lot
    ON inventory_warehouse_stock (item_id, warehouse_id, COALESCE(lot_id, -1))
    WHERE deleted_at IS NULL;

CREATE TABLE inventory_transfers (
    id                       BIGSERIAL PRIMARY KEY,
    source_warehouse_id      BIGINT        NOT NULL REFERENCES warehouses(id),
    destination_warehouse_id BIGINT        NOT NULL REFERENCES warehouses(id),
    item_id                  BIGINT        NOT NULL REFERENCES inventory_items(id),
    lot_id                   BIGINT                 REFERENCES inventory_lots(id),
    quantity                 NUMERIC(14,3) NOT NULL CHECK (quantity > 0),
    status                   VARCHAR(20)   NOT NULL DEFAULT 'COMPLETED',
    notes                    TEXT,
    issued_at                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    issued_by                BIGINT        NOT NULL,
    completed_at             TIMESTAMP,
    completed_by             BIGINT,
    cancelled_at             TIMESTAMP,
    cancelled_by             BIGINT,
    created_at               TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by               BIGINT,
    updated_by               BIGINT,
    deleted_at               TIMESTAMP,
    CHECK (source_warehouse_id <> destination_warehouse_id)
);
CREATE INDEX idx_transfers_source      ON inventory_transfers(source_warehouse_id);
CREATE INDEX idx_transfers_destination ON inventory_transfers(destination_warehouse_id);
CREATE INDEX idx_transfers_item        ON inventory_transfers(item_id);
CREATE INDEX idx_transfers_status      ON inventory_transfers(status);
CREATE INDEX idx_transfers_deleted_at  ON inventory_transfers(deleted_at);

CREATE TABLE warehouse_charges (
    id            BIGSERIAL PRIMARY KEY,
    warehouse_id  BIGINT        NOT NULL REFERENCES warehouses(id),
    item_id       BIGINT        NOT NULL REFERENCES inventory_items(id),
    lot_id        BIGINT                 REFERENCES inventory_lots(id),
    admission_id  BIGINT        NOT NULL REFERENCES admissions(id),
    quantity      NUMERIC(14,3) NOT NULL CHECK (quantity > 0),
    amount        NUMERIC(14,2) NOT NULL,
    reason        VARCHAR(500)  NOT NULL,
    notes         TEXT,
    charge_id     BIGINT                 REFERENCES patient_charges(id),
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    BIGINT,
    updated_by    BIGINT,
    deleted_at    TIMESTAMP
);
CREATE INDEX idx_wc_warehouse   ON warehouse_charges(warehouse_id);
CREATE INDEX idx_wc_admission   ON warehouse_charges(admission_id);
CREATE INDEX idx_wc_item        ON warehouse_charges(item_id);
CREATE INDEX idx_wc_deleted_at  ON warehouse_charges(deleted_at);

CREATE TABLE user_warehouses (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES users(id),
    warehouse_id  BIGINT       NOT NULL REFERENCES warehouses(id),
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    BIGINT,
    updated_by    BIGINT,
    deleted_at    TIMESTAMP
);
CREATE INDEX idx_uw_user        ON user_warehouses(user_id);
CREATE INDEX idx_uw_warehouse   ON user_warehouses(warehouse_id);
CREATE INDEX idx_uw_deleted_at  ON user_warehouses(deleted_at);
-- Partial unique so a soft-deleted assignment can be re-created (re-assignment).
CREATE UNIQUE INDEX uq_uw_user_warehouse
    ON user_warehouses (user_id, warehouse_id) WHERE deleted_at IS NULL;

-- Data-driven role -> default-warehouse mapping (FR-9). Adding a role's default
-- bodega is a data change, not a code change. ADMIN/ADMINISTRATIVE_STAFF (all)
-- and DOCTOR/RESIDENT_DOCTOR (none) are represented by absence + a role check.
CREATE TABLE role_default_warehouses (
    id            BIGSERIAL PRIMARY KEY,
    role_id       BIGINT       NOT NULL REFERENCES roles(id),
    warehouse_id  BIGINT       NOT NULL REFERENCES warehouses(id),
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    BIGINT,
    updated_by    BIGINT,
    deleted_at    TIMESTAMP,
    UNIQUE (role_id, warehouse_id)
);
CREATE INDEX idx_rdw_role       ON role_default_warehouses(role_id);
CREATE INDEX idx_rdw_deleted_at ON role_default_warehouses(deleted_at);

-- ---------------------------------------------------------------------------
-- 2. Link inventory_movements to a warehouse (and optionally a transfer).
--    Every EXIT/ENTRY now records which bodega it hit. Nullable because legacy
--    rows pre-date warehouses; new rows always set warehouse_id at the service
--    layer.
-- ---------------------------------------------------------------------------
ALTER TABLE inventory_movements
    ADD COLUMN warehouse_id BIGINT NULL REFERENCES warehouses(id),
    ADD COLUMN transfer_id  BIGINT NULL REFERENCES inventory_transfers(id);
CREATE INDEX idx_inventory_movements_warehouse_id ON inventory_movements(warehouse_id);
CREATE INDEX idx_inventory_movements_transfer_id  ON inventory_movements(transfer_id);

-- ---------------------------------------------------------------------------
-- 3. Seed the six day-1 warehouses (names bilingual-neutral; localized on FE).
-- ---------------------------------------------------------------------------
INSERT INTO warehouses (code, name, description, active, created_at, updated_at) VALUES
('ADMINISTRACION',  'Administración',  'Master / receiving warehouse. Deliveries land here.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ENFERMERIA',      'Enfermería',      'Nursing warehouse. Medication administration debits here by default.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MANTENIMIENTO_1', 'Mantenimiento 1', 'Maintenance warehouse 1.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MANTENIMIENTO_2', 'Mantenimiento 2', 'Maintenance warehouse 2.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('COCINA',          'Cocina',          'Kitchen warehouse. Food supplies.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PSICOLOGIA',      'Psicología',      'Psychology warehouse. Psychometric forms, therapy materials.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 4. Seed the eight warehouse permissions.
-- ---------------------------------------------------------------------------
INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('warehouse:read',             'View Warehouses',          'List warehouses and view per-warehouse stock',          'warehouse',          'read',    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('warehouse:create',           'Create Warehouse',         'Create a new warehouse',                                'warehouse',          'create',  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('warehouse:update',           'Update Warehouse',         'Rename / activate / deactivate a warehouse',            'warehouse',          'update',  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('warehouse:delete',           'Delete Warehouse',         'Soft-delete a warehouse (only if empty of stock)',      'warehouse',          'delete',  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('warehouse-transfer:create',  'Create Transfer',          'Issue a transfer from a warehouse you have access to',  'warehouse-transfer', 'create',  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('warehouse-transfer:read',    'View Transfers',           'View transfer history',                                 'warehouse-transfer', 'read',    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('warehouse-transfer:receive', 'Receive Transfer',         'Acknowledge receipt of an inbound transfer',            'warehouse-transfer', 'receive', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('warehouse-charge:create',    'Create Warehouse Charge',  'Charge a non-medical consumable to an admission',       'warehouse-charge',   'create',  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 5. MAINTENANCE role (system role; zero users in production).
-- ---------------------------------------------------------------------------
INSERT INTO roles (code, name, description, is_system, created_at, updated_at)
VALUES (
    'MAINTENANCE',
    'Maintenance',
    'Mantenimiento — manages maintenance bodegas, transfers supplies, charges non-medical consumables',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (code) DO NOTHING;

-- MAINTENANCE grants: its own permission set (NOT a clone). It needs to read
-- warehouses, transfer from assigned sources, read transfer history, charge
-- consumables, plus pick items/admissions/patients on the forms.
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'MAINTENANCE'
  AND p.code IN (
      'warehouse:read',
      'warehouse-transfer:create',
      'warehouse-transfer:read',
      'warehouse-charge:create',
      'inventory-item:read',
      'admission:read',
      'patient:read'
  )
  AND r.deleted_at IS NULL AND p.deleted_at IS NULL
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 6. Grant the warehouse permissions to existing roles per the spec matrix.
--    Source/destination scope restrictions are enforced at the service layer,
--    not as separate permissions.
-- ---------------------------------------------------------------------------

-- ADMIN: all eight.
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMIN'
  AND p.code IN (
      'warehouse:read', 'warehouse:create', 'warehouse:update', 'warehouse:delete',
      'warehouse-transfer:create', 'warehouse-transfer:read', 'warehouse-transfer:receive',
      'warehouse-charge:create'
  )
  AND r.deleted_at IS NULL AND p.deleted_at IS NULL
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ADMINISTRATIVE_STAFF: read, transfer create/read/receive, charge.
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMINISTRATIVE_STAFF'
  AND p.code IN (
      'warehouse:read',
      'warehouse-transfer:create', 'warehouse-transfer:read', 'warehouse-transfer:receive',
      'warehouse-charge:create'
  )
  AND r.deleted_at IS NULL AND p.deleted_at IS NULL
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- CHIEF_NURSE: read, transfer create (enfermería source) / read / receive.
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'CHIEF_NURSE'
  AND p.code IN (
      'warehouse:read',
      'warehouse-transfer:create', 'warehouse-transfer:read', 'warehouse-transfer:receive'
  )
  AND r.deleted_at IS NULL AND p.deleted_at IS NULL
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- NURSE: read (enfermería), transfer read + receive (no create).
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'NURSE'
  AND p.code IN ('warehouse:read', 'warehouse-transfer:read', 'warehouse-transfer:receive')
  AND r.deleted_at IS NULL AND p.deleted_at IS NULL
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- AUXILIARY_NURSE: read (enfermería), transfer read (no create/receive).
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'AUXILIARY_NURSE'
  AND p.code IN ('warehouse:read', 'warehouse-transfer:read')
  AND r.deleted_at IS NULL AND p.deleted_at IS NULL
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- DOCTOR / RESIDENT_DOCTOR: read-only on all warehouses.
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('DOCTOR', 'RESIDENT_DOCTOR')
  AND p.code IN ('warehouse:read')
  AND r.deleted_at IS NULL AND p.deleted_at IS NULL
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- PSYCHOLOGIST: read (psicología), transfer create (psicología source) / read / receive.
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'PSYCHOLOGIST'
  AND p.code IN (
      'warehouse:read',
      'warehouse-transfer:create', 'warehouse-transfer:read', 'warehouse-transfer:receive'
  )
  AND r.deleted_at IS NULL AND p.deleted_at IS NULL
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 7. Seed the role -> default-warehouse mapping.
--    NURSE / AUXILIARY_NURSE / CHIEF_NURSE -> ENFERMERIA; PSYCHOLOGIST -> PSICOLOGIA.
-- ---------------------------------------------------------------------------
INSERT INTO role_default_warehouses (role_id, warehouse_id, created_at, updated_at)
SELECT r.id, w.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN warehouses w
WHERE ((r.code IN ('NURSE', 'AUXILIARY_NURSE', 'CHIEF_NURSE') AND w.code = 'ENFERMERIA')
    OR (r.code = 'PSYCHOLOGIST' AND w.code = 'PSICOLOGIA'))
  AND r.deleted_at IS NULL AND w.deleted_at IS NULL
ON CONFLICT (role_id, warehouse_id) DO NOTHING;

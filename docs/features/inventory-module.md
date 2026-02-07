# Feature: Inventory Management (Gestión de Inventario)

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-02-06 | @author | Initial draft |
| 1.1 | 2026-02-06 | @author | Closed spec gaps: added search/sort, movement ordering, concurrency strategy, item name uniqueness clarification, room pricing i18n, bulk ops scope |

---

## Overview

Allow administrative staff to manage hospital inventory across dynamic categories (medicine, materials & equipment, laboratories, services, special personnel, kitchen ingredients, food served). Each inventory item tracks name, description, price, cost, quantity, and restock level with full stock movement history (entries/exits). Items support either flat pricing or time-based pricing (per configurable time interval) for equipment usage. Admins can also create and manage inventory categories. Additionally, price and cost fields are added to the existing Room entity.

---

## Use Case / User Story

1. As an **admin**, I want to manage inventory categories (create, edit, delete, list) so that I can organize inventory items by type (medicine, equipment, services, etc.).
2. As an **admin**, I want to manage inventory items within categories (create, edit, delete, list) so that I can track what the hospital has in stock, including name, description, price, cost, quantity, and restock level.
3. As an **admin**, I want to record stock movements (entries and exits) for inventory items so that I have a full history of how quantities change over time.
4. As an **admin**, I want to configure items with either flat pricing or time-based pricing (per configurable time interval) so that equipment usage can be billed correctly by the hour or minute.
5. As an **admin**, I want to view a low-stock report showing items at or below their restock level, filterable by category, so that I know what needs to be reordered.
6. As an **admin**, I want to add pricing information (price and cost) to existing hospital rooms so that room charges can be tracked alongside other inventory.

---

## Authorization / Role Access

### Inventory Categories

| Action | Required Role(s) | Permission | Notes |
|--------|------------------|------------|-------|
| View | ADMIN | `inventory-category:read` | Admin only |
| Create | ADMIN | `inventory-category:create` | Admin only |
| Update | ADMIN | `inventory-category:update` | Admin only |
| Delete | ADMIN | `inventory-category:delete` | Cannot delete if items exist in category |

### Inventory Items

| Action | Required Role(s) | Permission | Notes |
|--------|------------------|------------|-------|
| View | ADMIN | `inventory-item:read` | Admin only |
| Create | ADMIN | `inventory-item:create` | Admin only |
| Update | ADMIN | `inventory-item:update` | Admin only |
| Delete | ADMIN | `inventory-item:delete` | Soft delete only |

### Inventory Movements

| Action | Required Role(s) | Permission | Notes |
|--------|------------------|------------|-------|
| View | ADMIN | `inventory-movement:read` | View movement history |
| Create | ADMIN | `inventory-movement:create` | Record stock entries/exits |
| Edit | - | - | Movements are immutable |
| Delete | - | - | Movements cannot be deleted |

### Low Stock Report

| Action | Required Role(s) | Permission | Notes |
|--------|------------------|------------|-------|
| View | ADMIN | `inventory-item:read` | Uses existing item read permission |

### Room Pricing

| Action | Required Role(s) | Permission | Notes |
|--------|------------------|------------|-------|
| Update | ADMIN | `room:update` | Uses existing room permission |

---

## Functional Requirements

### Inventory Categories (Admin-managed)

- Admin-managed lookup table for inventory types
- **Fields:**
  - **Name** - Category name (required, max 100 chars, unique)
  - **Description** - Optional description (max 255 chars)
  - **Display Order** - For ordering in lists and dropdowns (default 0)
  - **Active** - Boolean to enable/disable without deleting
- **Default categories (seeded):**
  1. Medicamentos (Medicine)
  2. Material y Equipo (Material & Equipment)
  3. Laboratorios (Laboratories)
  4. Servicios (Services)
  5. Personal Especial (Special Personnel)
  6. Ingredientes de Cocina (Kitchen Ingredients)
  7. Alimentación (Food Served)
- Displayed in admin settings alongside Triage Codes, Rooms, and Activity Categories
- **Deletion restriction**: Categories that have inventory items (including soft-deleted) cannot be deleted; use the "Active" toggle to hide categories instead

### Inventory Items (Admin-managed)

- **Fields per item:**
  1. **Name** - Item name (required, max 150 chars, **not unique** — different items may share a name across or within categories, distinguished by description/pricing)
  2. **Description** - Optional description (max 500 chars)
  3. **Category** - FK to inventory category (required, dropdown)
  4. **Price** - What the hospital charges (required, DECIMAL(12,2), >= 0)
  5. **Cost** - What the hospital pays (required, DECIMAL(12,2), >= 0)
  6. **Quantity** - Current stock count (INT, >= 0, updated via movements)
  7. **Restock Level** - Minimum stock threshold (INT, >= 0, default 0)
  8. **Pricing Type** - FLAT or TIME_BASED (required, default FLAT)
  9. **Time Unit** - MINUTES or HOURS (required when pricing type is TIME_BASED)
  10. **Time Interval** - Number of time units per charge (required when pricing type is TIME_BASED, > 0)
  11. **Active** - Boolean to enable/disable (default true)
- Items are listed with filtering by category and optional **search by name** (case-insensitive substring match)
- Default sort order: by name ascending
- Full CRUD for admin users
- Quantity is not directly editable — it changes only through stock movements
- **Bulk operations**: Not supported in this version. Movements are recorded one item at a time.

### Stock Movements (Audit trail)

- **Fields per movement:**
  1. **Item** - FK to inventory item (required)
  2. **Movement Type** - ENTRY or EXIT (required)
  3. **Quantity** - Amount moved (required, > 0)
  4. **Notes** - Optional reason/description (max 500 chars)
  5. **Created At** - Auto-generated timestamp
  6. **Created By** - Auto-captured from authenticated user
- **Business Rules:**
  - When a movement is created, the item's quantity is updated atomically using a single `UPDATE ... SET quantity = quantity + :delta WHERE id = :id AND quantity + :delta >= 0` query within a `@Transactional` method. This prevents race conditions without requiring optimistic locking or `@Version`.
  - ENTRY increases quantity; EXIT decreases quantity
  - EXIT cannot reduce quantity below 0
  - Movements are immutable (cannot be edited or deleted)
- Movement history is viewable per item, sorted by **most recent first** (created_at DESC)

### Low Stock Report

- View items where `quantity <= restock_level` and `restock_level > 0`
- Filterable by category
- Shows: item name, category, current quantity, restock level
- Sorted by **severity** (largest deficit first): `(restock_level - quantity) DESC`
- Items with restock level of 0 are excluded (no threshold configured)

### Room Pricing (Enhancement to existing Room entity)

- Add `price` (DECIMAL(12,2), nullable) and `cost` (DECIMAL(12,2), nullable) fields to Room
- Displayed in rooms list and editable in room form
- Optional — existing rooms without pricing still work normally

---

## Acceptance Criteria / Scenarios

### Inventory Categories - Happy Path

- Given an admin accesses the inventory categories admin page, when they create a new category with a unique name, then the category is saved and appears in the list.
- Given an admin edits a category, when they change the name and save, then the updated name appears in the list and in the item category dropdown.
- Given an admin deactivates a category, when viewing the item form category dropdown, then the deactivated category is not shown.

### Inventory Categories - Edge Cases

- Given an admin creates a category with a duplicate name, then the API returns 400 Bad Request with message: "Inventory category with name 'X' already exists" (EN) / "La categoría de inventario con nombre 'X' ya existe" (ES).
- Given an admin creates a category without a name, then a validation error is displayed: "Name is required" (EN) / "El nombre es requerido" (ES).
- Given an admin attempts to delete a category that has inventory items, then the API returns 400 Bad Request with message: "Cannot delete category that has inventory items" (EN) / "No se puede eliminar una categoría que tiene artículos de inventario" (ES).
- Given a non-admin user attempts to access inventory categories, then they are redirected to the dashboard.

### Inventory Items - Happy Path

- Given an admin creates an item with valid data (name, category, price, cost), then the item is saved with quantity 0 and appears in the item list.
- Given an admin creates a time-based item (e.g., heart rate monitor at Q50/hour), when they select pricing type TIME_BASED, unit HOURS, interval 1, then the item is saved with time-based pricing displayed.
- Given an admin views the item list, when they filter by category, then only items in that category are shown.
- Given an admin types a search term in the search field, then the item list is filtered to show only items whose name contains the search term (case-insensitive).
- Given an admin edits an item's price and restock level, then the changes are saved successfully.

### Inventory Items - Edge Cases

- Given an admin creates an item without required fields (name, category, price, cost), then validation errors are displayed for each missing field.
- Given an admin creates an item with negative price or cost, then a validation error is displayed: "Price must be greater than or equal to 0" (EN) / "El precio debe ser mayor o igual a 0" (ES).
- Given an admin creates a TIME_BASED item without specifying time unit or interval, then validation errors are displayed for the missing fields.
- Given an admin creates a TIME_BASED item with interval 0 or negative, then a validation error is displayed: "Time interval must be greater than 0" (EN) / "El intervalo de tiempo debe ser mayor a 0" (ES).
- Given a FLAT pricing item, when time_unit and time_interval are provided, then they are ignored/null.

### Stock Movements - Happy Path

- Given an admin records an ENTRY movement of quantity 50 for an item with quantity 10, then the item's quantity becomes 60 and the movement appears in the history.
- Given an admin records an EXIT movement of quantity 5 for an item with quantity 60, then the item's quantity becomes 55 and the movement appears in the history.
- Given an admin views the movement history for an item, then all movements are listed in reverse chronological order (most recent first) with type, quantity, notes, timestamp, and who created it.

### Stock Movements - Edge Cases

- Given an admin records an EXIT movement of quantity 100 for an item with quantity 50, then the API returns 400 Bad Request with message: "Insufficient stock. Current quantity: 50, requested: 100" (EN) / "Stock insuficiente. Cantidad actual: 50, solicitada: 100" (ES).
- Given an admin records a movement with quantity 0 or negative, then a validation error is displayed: "Quantity must be greater than 0" (EN) / "La cantidad debe ser mayor a 0" (ES).
- Given a movement is created, then it cannot be edited or deleted via the API.

### Low Stock Report - Happy Path

- Given items exist where quantity <= restock_level and restock_level > 0, when an admin views the low stock report, then those items are displayed with name, category, current quantity, and restock level.
- Given an admin filters the report by category, then only low-stock items in that category are shown.

### Low Stock Report - Edge Cases

- Given no items are below restock level, then the report shows a friendly message: "No low stock items" (EN) / "No hay artículos con stock bajo" (ES).
- Given an item has restock_level = 0, then it never appears in the low stock report regardless of quantity.

### Room Pricing - Happy Path

- Given an admin edits a room and sets price and cost, then the values are saved and displayed in the rooms list.
- Given an existing room has no price/cost set, then it displays "-" in those columns.

### Room Pricing - Edge Cases

- Given an admin sets a negative price or cost on a room, then a validation error is displayed: "Price must be greater than or equal to 0" (EN) / "El precio debe ser mayor o igual a 0" (ES) (or cost equivalent).

---

## Non-Functional Requirements

- **Security**: Only admin users can access all inventory features; stock movements are immutable
- **Audit**: Full audit trail on all entities (created_by, updated_by, created_at, updated_at)
- **i18n**: All user-facing text localized in English and Spanish
- **Data Integrity**: Stock quantity updates must be atomic to prevent race conditions

---

## API Contract

### Inventory Category Endpoints (Admin)

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| GET | `/api/v1/admin/inventory-categories` | - | `List<InventoryCategoryResponse>` | Yes | List all categories |
| GET | `/api/v1/inventory-categories` | - | `List<InventoryCategoryResponse>` | Yes | List active categories (for dropdown). Requires `inventory-category:read`. |
| GET | `/api/v1/admin/inventory-categories/{id}` | - | `InventoryCategoryResponse` | Yes | Get single category |
| POST | `/api/v1/admin/inventory-categories` | `CreateInventoryCategoryRequest` | `InventoryCategoryResponse` | Yes | Create category |
| PUT | `/api/v1/admin/inventory-categories/{id}` | `UpdateInventoryCategoryRequest` | `InventoryCategoryResponse` | Yes | Update category |
| DELETE | `/api/v1/admin/inventory-categories/{id}` | - | - | Yes | Soft delete category |

### Inventory Item Endpoints (Admin)

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| GET | `/api/v1/admin/inventory-items?categoryId={id}&search={term}` | - | `List<InventoryItemResponse>` | Yes | List items (optional category filter and name search). Default sort: name ASC. |
| GET | `/api/v1/admin/inventory-items/{id}` | - | `InventoryItemResponse` | Yes | Get single item |
| POST | `/api/v1/admin/inventory-items` | `CreateInventoryItemRequest` | `InventoryItemResponse` | Yes | Create item |
| PUT | `/api/v1/admin/inventory-items/{id}` | `UpdateInventoryItemRequest` | `InventoryItemResponse` | Yes | Update item |
| DELETE | `/api/v1/admin/inventory-items/{id}` | - | - | Yes | Soft delete item |
| GET | `/api/v1/admin/inventory-items/low-stock?categoryId={id}` | - | `List<InventoryItemResponse>` | Yes | Low stock report (sorted by deficit DESC) |

### Inventory Movement Endpoints (Admin)

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| GET | `/api/v1/admin/inventory-items/{itemId}/movements` | - | `List<InventoryMovementResponse>` | Yes | List movements for item (sorted by created_at DESC) |
| POST | `/api/v1/admin/inventory-items/{itemId}/movements` | `CreateInventoryMovementRequest` | `InventoryMovementResponse` | Yes | Record stock movement |

### Room Pricing (Updated existing endpoints)

Existing `PUT /api/v1/rooms/{id}` endpoint is updated to accept `price` and `cost` fields.

### Request/Response Examples

```json
// POST /api/v1/admin/inventory-categories - Request
{
  "name": "Medicamentos",
  "description": "Medication and pharmaceutical supplies",
  "displayOrder": 1,
  "active": true
}

// Response - InventoryCategoryResponse
{
  "id": 1,
  "name": "Medicamentos",
  "description": "Medication and pharmaceutical supplies",
  "displayOrder": 1,
  "active": true,
  "createdAt": "2026-02-06T10:00:00",
  "updatedAt": "2026-02-06T10:00:00",
  "createdBy": null,
  "updatedBy": null
}

// POST /api/v1/admin/inventory-items - Request (Flat pricing)
{
  "name": "Aspirina 500mg",
  "description": "Aspirin tablets 500mg",
  "categoryId": 1,
  "price": 5.00,
  "cost": 2.50,
  "restockLevel": 100,
  "pricingType": "FLAT",
  "active": true
}

// POST /api/v1/admin/inventory-items - Request (Time-based pricing)
{
  "name": "Monitor de Frecuencia Cardíaca",
  "description": "Heart rate monitor for patient monitoring",
  "categoryId": 2,
  "price": 50.00,
  "cost": 10.00,
  "restockLevel": 5,
  "pricingType": "TIME_BASED",
  "timeUnit": "HOURS",
  "timeInterval": 1,
  "active": true
}

// Response - InventoryItemResponse
{
  "id": 1,
  "name": "Monitor de Frecuencia Cardíaca",
  "description": "Heart rate monitor for patient monitoring",
  "category": {
    "id": 2,
    "name": "Material y Equipo"
  },
  "price": 50.00,
  "cost": 10.00,
  "quantity": 0,
  "restockLevel": 5,
  "pricingType": "TIME_BASED",
  "timeUnit": "HOURS",
  "timeInterval": 1,
  "active": true,
  "createdAt": "2026-02-06T10:00:00",
  "updatedAt": "2026-02-06T10:00:00",
  "createdBy": { "id": 1, "salutation": null, "firstName": "Admin", "lastName": "User" },
  "updatedBy": null
}

// POST /api/v1/admin/inventory-items/{itemId}/movements - Request
{
  "type": "ENTRY",
  "quantity": 50,
  "notes": "Monthly restock from supplier"
}

// Response - InventoryMovementResponse
{
  "id": 1,
  "itemId": 1,
  "type": "ENTRY",
  "quantity": 50,
  "previousQuantity": 0,
  "newQuantity": 50,
  "notes": "Monthly restock from supplier",
  "createdAt": "2026-02-06T10:30:00",
  "createdBy": { "id": 1, "salutation": null, "firstName": "Admin", "lastName": "User" }
}

// GET /api/v1/admin/inventory-items/low-stock - Response
[
  {
    "id": 5,
    "name": "Aspirina 500mg",
    "category": { "id": 1, "name": "Medicamentos" },
    "price": 5.00,
    "cost": 2.50,
    "quantity": 8,
    "restockLevel": 100,
    "pricingType": "FLAT",
    "timeUnit": null,
    "timeInterval": null,
    "active": true,
    "createdAt": "2026-02-06T10:00:00",
    "updatedAt": "2026-02-06T11:00:00",
    "createdBy": null,
    "updatedBy": null
  }
]

// PUT /api/v1/rooms/{id} - Updated Request (with pricing)
{
  "number": "101",
  "type": "PRIVATE",
  "capacity": 1,
  "price": 1500.00,
  "cost": 800.00
}
```

---

## Database Changes

### New Entities

| Entity | Table | Extends | Description |
|--------|-------|---------|-------------|
| `InventoryCategory` | `inventory_categories` | `BaseEntity` | Inventory type lookup table |
| `InventoryItem` | `inventory_items` | `BaseEntity` | Individual inventory items |
| `InventoryMovement` | `inventory_movements` | `BaseEntity` | Stock movement audit trail |

### Modified Entities

| Entity | Table | Changes |
|--------|-------|---------|
| `Room` | `rooms` | Add `price DECIMAL(12,2)` and `cost DECIMAL(12,2)` columns |

### New Migrations

| Migration | Description |
|-----------|-------------|
| `V043__create_inventory_categories_table.sql` | Creates inventory_categories table with seed data |
| `V044__create_inventory_items_table.sql` | Creates inventory_items table |
| `V045__create_inventory_movements_table.sql` | Creates inventory_movements table |
| `V046__add_inventory_permissions.sql` | Adds inventory permissions and assigns to ADMIN role |
| `V047__add_room_pricing.sql` | Adds price and cost columns to rooms table |

### Schema

```sql
-- V043__create_inventory_categories_table.sql
CREATE TABLE inventory_categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    display_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_inventory_categories_deleted_at ON inventory_categories(deleted_at);
CREATE INDEX idx_inventory_categories_display_order ON inventory_categories(display_order);
CREATE INDEX idx_inventory_categories_active ON inventory_categories(active);

-- Seed default categories
INSERT INTO inventory_categories (name, description, display_order, active, created_at, updated_at) VALUES
('Medicamentos', 'Medication and pharmaceutical supplies', 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Material y Equipo', 'Materials and equipment', 2, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Laboratorios', 'Laboratory services and supplies', 3, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Servicios', 'Hospital services', 4, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Personal Especial', 'Specialized personnel services', 5, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Ingredientes de Cocina', 'Kitchen ingredients', 6, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Alimentación', 'Food served to patients', 7, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- V044__create_inventory_items_table.sql
CREATE TABLE inventory_items (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL REFERENCES inventory_categories(id),
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    price DECIMAL(12,2) NOT NULL DEFAULT 0 CHECK (price >= 0),
    cost DECIMAL(12,2) NOT NULL DEFAULT 0 CHECK (cost >= 0),
    quantity INT NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    restock_level INT NOT NULL DEFAULT 0 CHECK (restock_level >= 0),
    pricing_type VARCHAR(20) NOT NULL DEFAULT 'FLAT',
    time_unit VARCHAR(20),
    time_interval INT CHECK (time_interval > 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_inventory_items_deleted_at ON inventory_items(deleted_at);
CREATE INDEX idx_inventory_items_category_id ON inventory_items(category_id);
CREATE INDEX idx_inventory_items_active ON inventory_items(active);
CREATE INDEX idx_inventory_items_pricing_type ON inventory_items(pricing_type);
CREATE INDEX idx_inventory_items_quantity_restock ON inventory_items(quantity, restock_level);
CREATE INDEX idx_inventory_items_name ON inventory_items(LOWER(name));

-- V045__create_inventory_movements_table.sql
CREATE TABLE inventory_movements (
    id BIGSERIAL PRIMARY KEY,
    item_id BIGINT NOT NULL REFERENCES inventory_items(id),
    movement_type VARCHAR(20) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    previous_quantity INT NOT NULL,
    new_quantity INT NOT NULL,
    notes VARCHAR(500),
    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_inventory_movements_deleted_at ON inventory_movements(deleted_at);
CREATE INDEX idx_inventory_movements_item_id ON inventory_movements(item_id);
CREATE INDEX idx_inventory_movements_movement_type ON inventory_movements(movement_type);
CREATE INDEX idx_inventory_movements_created_at ON inventory_movements(created_at);

-- V046__add_inventory_permissions.sql
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

-- V047__add_room_pricing.sql
ALTER TABLE rooms ADD COLUMN price DECIMAL(12,2) CHECK (price >= 0);
ALTER TABLE rooms ADD COLUMN cost DECIMAL(12,2) CHECK (cost >= 0);
```

### Index Requirements

- [x] `deleted_at` - Required for soft delete queries on all tables
- [x] `category_id` on inventory_items - FK lookup and filtering
- [x] `item_id` on inventory_movements - FK lookup for movement history
- [x] `active` on categories and items - Filtering active records
- [x] `display_order` on categories - Ordering in dropdowns
- [x] `pricing_type` on items - Filtering by pricing type
- [x] `(quantity, restock_level)` on items - Low stock report queries
- [x] `LOWER(name)` on items - Case-insensitive name search
- [x] `movement_type` on movements - Filtering by type
- [x] `created_at` on movements - Chronological ordering

---

## Frontend Changes

### Components

| Component | Location | Description |
|-----------|----------|-------------|
| `InventoryCategoriesView.vue` | `src/views/admin/` | Admin CRUD list for inventory categories |
| `InventoryCategoryFormView.vue` | `src/views/admin/` | Admin create/edit category form |
| `InventoryItemsView.vue` | `src/views/inventory/` | Item list with category filter |
| `InventoryItemFormView.vue` | `src/views/inventory/` | Create/edit item form (with pricing type toggle) |
| `InventoryItemDetailView.vue` | `src/views/inventory/` | Item detail with movement history |
| `InventoryMovementForm.vue` | `src/components/inventory/` | Record stock movement dialog/form |
| `LowStockReportView.vue` | `src/views/inventory/` | Low stock report with category filter |

### Pinia Stores

| Store | Location | Description |
|-------|----------|-------------|
| `useInventoryCategoryStore` | `src/stores/inventoryCategory.ts` | Category CRUD operations |
| `useInventoryItemStore` | `src/stores/inventoryItem.ts` | Item CRUD, movements, low stock |

### Types

| Type File | Location | Description |
|-----------|----------|-------------|
| `inventoryCategory.ts` | `src/types/` | InventoryCategory, Create/Update requests |
| `inventoryItem.ts` | `src/types/` | InventoryItem, InventoryMovement, PricingType, TimeUnit, MovementType |

### Routes

| Path | Name | Component | Auth | Permission |
|------|------|-----------|------|------------|
| `/admin/inventory-categories` | `inventory-categories` | `InventoryCategoriesView` | Yes | `inventory-category:read` |
| `/admin/inventory-categories/new` | `inventory-category-create` | `InventoryCategoryFormView` | Yes | `inventory-category:create` |
| `/admin/inventory-categories/:id/edit` | `inventory-category-edit` | `InventoryCategoryFormView` | Yes | `inventory-category:update` |
| `/inventory` | `inventory-items` | `InventoryItemsView` | Yes | `inventory-item:read` |
| `/inventory/new` | `inventory-item-create` | `InventoryItemFormView` | Yes | `inventory-item:create` |
| `/inventory/:id` | `inventory-item-detail` | `InventoryItemDetailView` | Yes | `inventory-item:read` |
| `/inventory/:id/edit` | `inventory-item-edit` | `InventoryItemFormView` | Yes | `inventory-item:update` |
| `/inventory/low-stock` | `inventory-low-stock` | `LowStockReportView` | Yes | `inventory-item:read` |

### Validation (Zod Schemas)

```typescript
// src/validation/inventory.ts
import { z } from 'zod'

export const inventoryCategorySchema = z.object({
  name: z
    .string()
    .min(1, 'validation.inventory.category.name.required')
    .max(100, 'validation.inventory.category.name.max'),
  description: z
    .string()
    .max(255, 'validation.inventory.category.description.max')
    .optional()
    .or(z.literal('')),
  displayOrder: z
    .number({ invalid_type_error: 'validation.inventory.category.displayOrder.invalid' })
    .int()
    .min(0)
    .default(0),
  active: z.boolean().default(true),
})

export const inventoryItemSchema = z.object({
  name: z
    .string()
    .min(1, 'validation.inventory.item.name.required')
    .max(150, 'validation.inventory.item.name.max'),
  description: z
    .string()
    .max(500, 'validation.inventory.item.description.max')
    .optional()
    .or(z.literal('')),
  categoryId: z
    .number({ required_error: 'validation.inventory.item.category.required' })
    .positive(),
  price: z
    .number({ required_error: 'validation.inventory.item.price.required' })
    .min(0, 'validation.inventory.item.price.min'),
  cost: z
    .number({ required_error: 'validation.inventory.item.cost.required' })
    .min(0, 'validation.inventory.item.cost.min'),
  restockLevel: z.number().int().min(0).default(0),
  pricingType: z.enum(['FLAT', 'TIME_BASED']).default('FLAT'),
  timeUnit: z.enum(['MINUTES', 'HOURS']).nullable().optional(),
  timeInterval: z.number().int().positive().nullable().optional(),
  active: z.boolean().default(true),
}).refine(
  (data) => data.pricingType !== 'TIME_BASED' || (data.timeUnit != null && data.timeInterval != null),
  { message: 'validation.inventory.item.timeBased.required', path: ['timeUnit'] }
)

export const inventoryMovementSchema = z.object({
  type: z.enum(['ENTRY', 'EXIT'], {
    required_error: 'validation.inventory.movement.type.required',
  }),
  quantity: z
    .number({ required_error: 'validation.inventory.movement.quantity.required' })
    .int()
    .positive('validation.inventory.movement.quantity.positive'),
  notes: z
    .string()
    .max(500, 'validation.inventory.movement.notes.max')
    .optional()
    .or(z.literal('')),
})
```

### Sidebar Navigation

Add new "Inventory" section to `AppMenu.vue`:

```typescript
// New "Inventory" section in model computed property (before the admin section)
// Uses hasPermission() for consistency with other non-admin sections (patient, admissions)
if (authStore.hasPermission('inventory-item:read')) {
  items.push({
    label: 'nav.inventory',
    items: [
      {
        label: 'nav.inventoryItems',
        icon: 'pi pi-fw pi-box',
        to: '/inventory'
      },
      {
        label: 'nav.lowStockReport',
        icon: 'pi pi-fw pi-exclamation-triangle',
        to: '/inventory/low-stock'
      }
    ]
  })
}

// Add to existing admin section (inside the `if (authStore.isAdmin)` block)
{
  label: 'nav.inventoryCategories',
  icon: 'pi pi-fw pi-list',
  to: '/admin/inventory-categories'
}
```

### i18n Keys

```json
// Additions to en.json
{
  "nav": {
    "inventory": "Inventory",
    "inventoryItems": "Inventory Items",
    "inventoryCategories": "Inventory Categories",
    "lowStockReport": "Low Stock Report"
  },
  "inventory": {
    "category": {
      "title": "Inventory Categories",
      "new": "New Category",
      "edit": "Edit Category",
      "name": "Name",
      "description": "Description",
      "displayOrder": "Display Order",
      "active": "Active",
      "empty": "No inventory categories found.",
      "created": "Category created successfully.",
      "updated": "Category updated successfully.",
      "deleted": "Category deleted successfully.",
      "confirmDelete": "Are you sure you want to delete this category?"
    },
    "item": {
      "title": "Inventory",
      "new": "New Item",
      "edit": "Edit Item",
      "detail": "Item Detail",
      "name": "Name",
      "description": "Description",
      "category": "Category",
      "price": "Price",
      "cost": "Cost",
      "quantity": "Quantity",
      "restockLevel": "Restock Level",
      "pricingType": "Pricing Type",
      "pricingTypes": {
        "FLAT": "Flat",
        "TIME_BASED": "Time-based"
      },
      "timeUnit": "Time Unit",
      "timeUnits": {
        "MINUTES": "Minutes",
        "HOURS": "Hours"
      },
      "timeInterval": "Time Interval",
      "perTimeLabel": "per {interval} {unit}",
      "active": "Active",
      "allCategories": "All Categories",
      "searchPlaceholder": "Search by name...",
      "empty": "No inventory items found.",
      "created": "Item created successfully.",
      "updated": "Item updated successfully.",
      "deleted": "Item deleted successfully.",
      "confirmDelete": "Are you sure you want to delete this item?"
    },
    "movement": {
      "title": "Stock Movements",
      "new": "Record Movement",
      "type": "Type",
      "types": {
        "ENTRY": "Entry",
        "EXIT": "Exit"
      },
      "quantity": "Quantity",
      "notes": "Notes",
      "previousQuantity": "Previous Qty",
      "newQuantity": "New Qty",
      "registeredBy": "Registered by",
      "registeredAt": "Date",
      "empty": "No movements recorded.",
      "created": "Movement recorded successfully."
    },
    "lowStock": {
      "title": "Low Stock Report",
      "empty": "No low stock items.",
      "belowThreshold": "Below restock level"
    },
    "room": {
      "price": "Price",
      "cost": "Cost"
    }
  },
  "validation": {
    "inventory": {
      "room": {
        "price": {
          "min": "Price must be greater than or equal to 0"
        },
        "cost": {
          "min": "Cost must be greater than or equal to 0"
        }
      }
    }
  }
}

// Additions to es.json
{
  "nav": {
    "inventory": "Inventario",
    "inventoryItems": "Artículos de Inventario",
    "inventoryCategories": "Categorías de Inventario",
    "lowStockReport": "Reporte de Stock Bajo"
  },
  "inventory": {
    "category": {
      "title": "Categorías de Inventario",
      "new": "Nueva Categoría",
      "edit": "Editar Categoría",
      "name": "Nombre",
      "description": "Descripción",
      "displayOrder": "Orden de Visualización",
      "active": "Activo",
      "empty": "No se encontraron categorías de inventario.",
      "created": "Categoría creada exitosamente.",
      "updated": "Categoría actualizada exitosamente.",
      "deleted": "Categoría eliminada exitosamente.",
      "confirmDelete": "¿Está seguro que desea eliminar esta categoría?"
    },
    "item": {
      "title": "Inventario",
      "new": "Nuevo Artículo",
      "edit": "Editar Artículo",
      "detail": "Detalle del Artículo",
      "name": "Nombre",
      "description": "Descripción",
      "category": "Categoría",
      "price": "Precio",
      "cost": "Costo",
      "quantity": "Cantidad",
      "restockLevel": "Nivel de Reabastecimiento",
      "pricingType": "Tipo de Precio",
      "pricingTypes": {
        "FLAT": "Fijo",
        "TIME_BASED": "Por Tiempo"
      },
      "timeUnit": "Unidad de Tiempo",
      "timeUnits": {
        "MINUTES": "Minutos",
        "HOURS": "Horas"
      },
      "timeInterval": "Intervalo de Tiempo",
      "perTimeLabel": "por {interval} {unit}",
      "active": "Activo",
      "allCategories": "Todas las Categorías",
      "searchPlaceholder": "Buscar por nombre...",
      "empty": "No se encontraron artículos de inventario.",
      "created": "Artículo creado exitosamente.",
      "updated": "Artículo actualizado exitosamente.",
      "deleted": "Artículo eliminado exitosamente.",
      "confirmDelete": "¿Está seguro que desea eliminar este artículo?"
    },
    "movement": {
      "title": "Movimientos de Stock",
      "new": "Registrar Movimiento",
      "type": "Tipo",
      "types": {
        "ENTRY": "Entrada",
        "EXIT": "Salida"
      },
      "quantity": "Cantidad",
      "notes": "Notas",
      "previousQuantity": "Cant. Anterior",
      "newQuantity": "Cant. Nueva",
      "registeredBy": "Registrado por",
      "registeredAt": "Fecha",
      "empty": "No hay movimientos registrados.",
      "created": "Movimiento registrado exitosamente."
    },
    "lowStock": {
      "title": "Reporte de Stock Bajo",
      "empty": "No hay artículos con stock bajo.",
      "belowThreshold": "Bajo nivel de reabastecimiento"
    },
    "room": {
      "price": "Precio",
      "cost": "Costo"
    }
  },
  "validation": {
    "inventory": {
      "room": {
        "price": {
          "min": "El precio debe ser mayor o igual a 0"
        },
        "cost": {
          "min": "El costo debe ser mayor o igual a 0"
        }
      }
    }
  }
}
```

---

## Implementation Notes

- **Pattern Reference**: Follow `PsychotherapyCategory` / `PsychotherapyCategoryService` pattern for inventory categories (admin CRUD with duplicate name checks, active/inactive toggle, delete restriction)
- **Item Quantity**: The `quantity` field on `InventoryItem` is not directly editable through the update endpoint. It only changes through stock movements. The `UpdateInventoryItemRequest` should NOT include `quantity`.
- **Atomic Stock Updates**: Use a native `@Modifying @Query` on the repository: `UPDATE inventory_items SET quantity = quantity + :delta, updated_at = CURRENT_TIMESTAMP WHERE id = :id AND quantity + :delta >= 0`. Check the returned update count — if 0, the item either doesn't exist or has insufficient stock. Wrap the movement creation and quantity update in a single `@Transactional` method. Do **not** use `@Version` / optimistic locking — the single atomic UPDATE is sufficient and simpler.
- **Time-Based Pricing Validation**: Backend must validate that when `pricingType = TIME_BASED`, both `timeUnit` and `timeInterval` are provided and valid. When `pricingType = FLAT`, `timeUnit` and `timeInterval` should be set to null.
- **Room Pricing**: Add `price` and `cost` fields to existing `Room` entity, `RoomResponse`, `CreateRoomRequest`, `UpdateRoomRequest`, and the room form/list views
- **Enums**: Create `PricingType` (FLAT, TIME_BASED), `TimeUnit` (MINUTES, HOURS), `MovementType` (ENTRY, EXIT) enums in the entity package
- **Low Stock Query**: Use `@Query` for readability: `SELECT i FROM InventoryItem i WHERE i.quantity <= i.restockLevel AND i.restockLevel > 0 AND i.active = true ORDER BY (i.restockLevel - i.quantity) DESC`. Optional `categoryId` filter via `AND (:categoryId IS NULL OR i.category.id = :categoryId)`.
- **Item Search**: Repository method for search: `@Query("SELECT i FROM InventoryItem i WHERE (:categoryId IS NULL OR i.category.id = :categoryId) AND (:search IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%'))) AND i.active = true ORDER BY i.name ASC")`. The `search` param is optional.
- **Admin Menu**: Add "Inventory Categories" to the admin navigation section alongside existing admin items
- **Sidebar**: Add a new "Inventory" navigation section (separate from admin) for items and low stock report
- **Movement Immutability**: No PUT or DELETE endpoints for movements. The `InventoryMovement` entity stores `previousQuantity` and `newQuantity` for audit purposes

---

## QA Checklist

### Backend
- [ ] All functional requirements implemented
- [ ] `InventoryCategory` entity extends `BaseEntity` with `@SQLRestriction`
- [ ] `InventoryItem` entity extends `BaseEntity` with `@SQLRestriction`
- [ ] `InventoryMovement` entity extends `BaseEntity` with `@SQLRestriction`
- [ ] DTOs used in controllers (no entity exposure)
- [ ] Input validation in place (Jakarta Bean Validation)
- [ ] Permission checks: only ADMIN can access all inventory endpoints
- [ ] Business rule: categories in use cannot be deleted
- [ ] Business rule: EXIT movements cannot reduce quantity below 0
- [ ] Business rule: movements are immutable (no edit/delete endpoints)
- [ ] Business rule: TIME_BASED items require timeUnit and timeInterval
- [ ] Stock quantity updates are atomic (single native UPDATE query, not read-modify-write)
- [ ] Room entity updated with price/cost fields
- [ ] Low stock report query works correctly
- [ ] Unit tests written and passing
- [ ] Integration tests written and passing (Testcontainers)
- [ ] Detekt passes (no new violations)
- [ ] OWASP dependency-check passes

### Frontend
- [ ] InventoryCategoriesView admin page working (CRUD)
- [ ] InventoryItemsView with category filtering and name search working
- [ ] InventoryItemFormView with pricing type toggle working
- [ ] InventoryItemDetailView with movement history working
- [ ] InventoryMovementForm dialog working
- [ ] LowStockReportView with category filter working
- [ ] Room form updated with price/cost fields
- [ ] Rooms list shows price/cost columns
- [ ] Pinia stores implemented (inventoryCategory, inventoryItem)
- [ ] Routes configured with proper permission guards
- [ ] Form validation with VeeValidate + Zod
- [ ] Error handling with localized messages
- [ ] Sidebar navigation updated (inventory section + admin categories)
- [ ] ESLint/oxlint passes
- [ ] i18n keys added for all user-facing text (EN/ES)
- [ ] Unit tests written and passing (Vitest)

### E2E Tests (Playwright)
- [ ] Admin creates/edits/deletes inventory category
- [ ] Admin cannot delete category with items (error displayed)
- [ ] Admin creates flat-priced inventory item
- [ ] Admin creates time-based priced inventory item
- [ ] Admin filters items by category
- [ ] Admin searches items by name
- [ ] Admin records ENTRY movement (quantity increases)
- [ ] Admin records EXIT movement (quantity decreases)
- [ ] EXIT movement blocked when insufficient stock (error displayed)
- [ ] Movement history displayed on item detail
- [ ] Low stock report shows items below threshold
- [ ] Low stock report filterable by category
- [ ] Room form includes price/cost fields
- [ ] Non-admin cannot access inventory pages (redirected)
- [ ] Form validation errors displayed correctly (EN/ES)

### General
- [ ] API contract documented
- [ ] Database migrations tested
- [ ] Feature documentation updated
- [ ] Reviewed by project owner

---

## Documentation Updates Required

### Must Update

- [ ] **[CLAUDE.md](../../CLAUDE.md)**
  - Add Inventory Management to "Implemented Features" section
  - Update migration count (V043-V047)
- [ ] **[ARCHITECTURE.md](../architecture/ARCHITECTURE.md)**
  - Add new entities to entity diagram

### Review for Consistency

- [ ] **[README.md](../../web/README.md)**
  - Check if setup instructions need updates

### Code Documentation

- [ ] **`InventoryCategory.kt`** - Document admin-managed lookup table
- [ ] **`InventoryItem.kt`** - Document pricing types and quantity management
- [ ] **`InventoryMovement.kt`** - Document immutability and atomic quantity updates
- [ ] **`InventoryItemService.kt`** - Document stock movement logic

---

## Related Docs/Commits/Issues

- Related feature: [Patient Admission](./patient-admission.md) (Room entity being enhanced)
- Related feature: [Psychotherapeutic Activities](./psychotherapeutic-activities.md) (similar category/item pattern)
- Similar pattern: `PsychotherapyCategory` (admin-managed lookup table)
- Similar pattern: `Room` (admin CRUD with pricing being added)

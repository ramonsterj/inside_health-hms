# Module: Inventory Management

**Module ID**: IC (Categories), II (Items), IM (Movements)
**Required Permissions**: inventory-category:*, inventory-item:*, inventory-movement:*
**API Base**: `/api/v1/inventory-categories`, `/api/v1/admin/inventory-categories`, `/api/v1/admin/inventory-items`

---

## Overview

Inventory management for hospital supplies, medications, and services:
- **Categories**: Flat hierarchy for organizing items
- **Items**: Flat or time-based pricing, stock quantity tracking, restock levels
- **Movements**: ENTRY (receive), EXIT (dispense), ADJUSTMENT (corrections)
- **Low Stock Report**: Items at or below restock level

**Billing integration**: EXIT movements linked to an admission auto-generate billing charges.

---

## Category Test Cases

### IC-01: List active categories
**Role**: ADMIN
**Precondition**: Logged in as admin, categories exist in seed data
**Steps**:
1. Navigate to Inventory Categories (admin)
2. View list
**Expected Result**: Categories listed with name, description, display order, active status.
**Type**: Happy Path

---

### IC-02: Create category
**Role**: ADMIN
**Precondition**: Logged in as admin
**Steps**:
1. Click "Create Category"
2. Fill: name "Medical Supplies", description "Bandages, syringes, etc.", displayOrder 5, active true
3. Save
**Expected Result**: Category created. Available as filter in items list.
**Type**: Happy Path

---

### IC-03: Update category
**Role**: ADMIN
**Precondition**: Logged in as admin, category exists
**Steps**:
1. Edit a category
2. Change description and display order
3. Save
**Expected Result**: Changes saved.
**Type**: Happy Path

---

### IC-04: Delete category - no items
**Role**: ADMIN
**Precondition**: Logged in as admin, empty category
**Steps**:
1. Delete the empty category
**Expected Result**: Category soft-deleted.
**Type**: Happy Path

---

### IC-05: Delete category - has items
**Role**: ADMIN
**Precondition**: Logged in as admin, category has items
**Steps**:
1. Try to delete category with items
**Expected Result**: Error: cannot delete category that has items assigned.
**Type**: Negative

---

### IC-06: Category validation
**Role**: ADMIN
**Precondition**: Logged in as admin
**Steps**:
1. Try to create category without name
2. Try duplicate name
**Expected Result**: Validation errors.
**Type**: Negative

---

### IC-07: Non-admin category access
**Role**: DOCTOR, NURSE
**Precondition**: Logged in as non-admin
**Steps**:
1. Try to access inventory category management
2. Try CRUD API calls
**Expected Result**: 403 Forbidden.
**Type**: Permission

---

## Item Test Cases

### II-01: List items with pagination
**Role**: ADMIN
**Precondition**: Logged in as admin, items exist
**Steps**:
1. Navigate to Inventory Items
2. View list with pagination
3. Verify columns: name, category, price, quantity, restock level, active
**Expected Result**: Items listed with correct data. Pagination works.
**Type**: Happy Path

---

### II-02: Search items by name
**Role**: ADMIN
**Precondition**: Logged in as admin
**Steps**:
1. Type in the search field
2. Verify items filtered by name
3. Clear search
**Expected Result**: Search filters correctly by item name.
**Type**: Happy Path

---

### II-03: Filter items by category
**Role**: ADMIN
**Precondition**: Logged in as admin
**Steps**:
1. Select a category filter
2. Verify only items in that category shown
3. Clear filter
**Expected Result**: Filter works correctly.
**Type**: Happy Path

---

### II-04: Create item - FLAT pricing
**Role**: ADMIN
**Precondition**: Logged in as admin, a category exists
**Steps**:
1. Click "Create Item"
2. Fill: name "Paracetamol 500mg", category (Medications), pricingType FLAT, price 25.00, quantity 100, restockLevel 20, active true
3. Save
**Expected Result**: Item created with FLAT pricing. Stock level 100.
**Type**: Happy Path

---

### II-05: Create item - TIME_BASED pricing (daily)
**Role**: ADMIN
**Precondition**: Logged in as admin
**Steps**:
1. Create item with pricingType TIME_BASED, timePricingUnit DAILY, price 50.00
2. Save
**Expected Result**: Item created with time-based daily pricing.
**Type**: Happy Path

---

### II-06: Create item - TIME_BASED pricing (hourly)
**Role**: ADMIN
**Precondition**: Logged in as admin
**Steps**:
1. Create item with pricingType TIME_BASED, timePricingUnit HOURLY, price 10.00
2. Save
**Expected Result**: Item created with hourly pricing.
**Type**: Happy Path

---

### II-07: Update item
**Role**: ADMIN
**Precondition**: Logged in as admin, item exists
**Steps**:
1. Edit an item
2. Change price, restock level, description
3. Save
**Expected Result**: Changes saved. New price applies to future charges.
**Type**: Happy Path

---

### II-08: Delete item (soft delete)
**Role**: ADMIN
**Precondition**: Logged in as admin
**Steps**:
1. Delete an item
2. Confirm
**Expected Result**: Item soft-deleted. Removed from list.
**Type**: Happy Path

---

### II-09: Low stock report
**Role**: ADMIN
**Precondition**: Logged in as admin, items with quantity <= restockLevel
**Steps**:
1. Navigate to Low Stock Report
2. View items
**Expected Result**: Only items where quantity <= restockLevel shown.
**Type**: Happy Path

---

### II-10: Low stock report - filter by category
**Role**: ADMIN
**Precondition**: Logged in as admin
**Steps**:
1. Open Low Stock Report
2. Filter by a specific category
**Expected Result**: Only low-stock items in selected category shown.
**Type**: Happy Path

---

### II-11: Item validation
**Role**: ADMIN
**Precondition**: Logged in as admin
**Steps**:
1. Try to create item without name or category
2. Try negative price
3. Try negative quantity
**Expected Result**: Validation errors for each case.
**Type**: Negative

---

### II-12: Non-admin item access
**Role**: All non-admin roles
**Precondition**: Logged in as non-admin
**Steps**:
1. Try to access inventory item management
2. Try CRUD API calls
**Expected Result**: 403 Forbidden for all write operations.
**Type**: Permission

---

## Movement Test Cases

### IM-01: ENTRY movement (receive stock)
**Role**: ADMIN
**Precondition**: Logged in as admin, item exists with quantity 100
**Steps**:
1. Open item detail
2. Click "Record Movement"
3. Select type: ENTRY, quantity: 50, notes: "Supplier delivery"
4. Submit
5. Check item stock level
**Expected Result**: Movement recorded. Item quantity now 150 (100 + 50).
**Type**: Happy Path

---

### IM-02: EXIT movement (dispense)
**Role**: ADMIN
**Precondition**: Logged in as admin, item has stock (e.g., 150)
**Steps**:
1. Record movement: type EXIT, quantity 10, notes: "Manual dispense"
2. Submit
3. Check stock
**Expected Result**: Movement recorded. Item quantity now 140 (150 - 10).
**Type**: Happy Path

---

### IM-03: EXIT movement with admission link
**Role**: ADMIN
**Precondition**: Logged in as admin, active admission exists, item has stock
**Steps**:
1. Record EXIT movement
2. Link to an admission (admissionId field)
3. Submit
4. Check billing for that admission
**Expected Result**: Movement recorded. Stock decreased. Billing charge auto-created for the admission (InventoryDispensedEvent).
**Type**: Happy Path

---

### IM-04: ADJUSTMENT movement
**Role**: ADMIN
**Precondition**: Logged in as admin, item exists
**Steps**:
1. Record movement: type ADJUSTMENT, quantity 5, notes: "Physical count correction"
2. Submit
**Expected Result**: Movement recorded. Quantity adjusted.
**Type**: Happy Path

---

### IM-05: EXIT more than available (denied)
**Role**: ADMIN
**Precondition**: Logged in as admin, item has quantity 10
**Steps**:
1. Try EXIT movement with quantity 20
**Expected Result**: Error: insufficient stock. Movement not recorded.
**Type**: Negative

---

### IM-06: View movement history
**Role**: ADMIN
**Precondition**: Logged in as admin, item has multiple movements
**Steps**:
1. Open item detail
2. View Movements tab/section
**Expected Result**: All movements listed with type, quantity, notes, date, user. Chronological order.
**Type**: Happy Path

---

### IM-07: Non-admin movement access
**Role**: All non-admin roles
**Precondition**: Logged in as non-admin
**Steps**:
1. Try to record a movement via API
**Expected Result**: 403 Forbidden.
**Type**: Permission

---

## Permission Matrix

| Action | ADMIN | STAFF | DOCTOR | PSYCH | NURSE | CHIEF_NURSE | USER |
|--------|-------|-------|--------|-------|-------|-------------|------|
| List categories | G | D | D | D | D | D | D |
| CRUD categories | G | D | D | D | D | D | D |
| List items | G | D | D | D | D | D | D |
| CRUD items | G | D | D | D | D | D | D |
| Record movements | G | D | D | D | D | D | D |
| View movements | G | D | D | D | D | D | D |
| Low stock report | G | D | D | D | D | D | D |

G = Granted, D = Denied

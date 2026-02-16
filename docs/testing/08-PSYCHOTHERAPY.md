# Module: Psychotherapy Activities

**Module ID**: PC (Categories), PA (Activities)
**Required Permissions**: psychotherapy-category:*, psychotherapy-activity:*
**API Base**: `/api/v1/psychotherapy-categories`, `/api/v1/admin/psychotherapy-categories`, `/api/v1/admissions/{admissionId}/psychotherapy-activities`

---

## Overview

Psychotherapy module for managing therapy categories and registering activities for hospitalized patients:
- **Categories** (Admin): Configurable with optional pricing for auto-billing
- **Activities** (Psychologist): Registration of therapy sessions per admission

**Billing integration**: When an activity is created with a priced category, a SERVICE billing charge is auto-generated. Unpriced categories create activities without charges.

---

## Category Test Cases

### PC-01: List active categories
**Role**: PSYCHOLOGIST
**Precondition**: Logged in as psych1, categories exist
**Steps**:
1. Navigate to psychotherapy section
2. View the category dropdown or list
**Expected Result**: Only active categories shown. Sorted by display order.
**Type**: Happy Path

---

### PC-02: List all categories (admin)
**Role**: ADMIN
**Precondition**: Logged in as admin
**Steps**:
1. Navigate to Psychotherapy Category management
2. View all categories including inactive
**Expected Result**: All categories listed with name, description, display order, active status, price, cost.
**Type**: Happy Path

---

### PC-03: Create category with price
**Role**: ADMIN
**Precondition**: Logged in as admin
**Steps**:
1. Click "Create Category"
2. Fill: name "Individual Therapy", description "1-on-1 session", displayOrder 1, active true, price 500.00, cost 200.00
3. Save
**Expected Result**: Category created with price. Available in activity registration.
**Type**: Happy Path

---

### PC-04: Create category without price
**Role**: ADMIN
**Precondition**: Logged in as admin
**Steps**:
1. Create category with name "Group Support", no price (null/0)
2. Save
**Expected Result**: Category created without price. Activities with this category won't generate charges.
**Type**: Happy Path

---

### PC-05: Update category - change price
**Role**: ADMIN
**Precondition**: Logged in as admin, category exists
**Steps**:
1. Edit existing category
2. Change price from Q500 to Q600
3. Save
4. Verify new price
**Expected Result**: Price updated. Future activities use new price.
**Type**: Happy Path

---

### PC-06: Toggle category active/inactive
**Role**: ADMIN
**Precondition**: Logged in as admin
**Steps**:
1. Edit an active category
2. Set active to false
3. Save
4. Verify it disappears from psychologist's dropdown
5. Re-enable it
**Expected Result**: Inactive categories don't appear in activity registration dropdowns.
**Type**: Happy Path

---

### PC-07: Delete category
**Role**: ADMIN
**Precondition**: Logged in as admin, category has no activities
**Steps**:
1. Delete an unused category
2. Confirm
**Expected Result**: Category soft-deleted.
**Type**: Happy Path

---

### PC-08: Category validation
**Role**: ADMIN
**Precondition**: Logged in as admin
**Steps**:
1. Try to create category without name (required)
2. Try negative price
3. Try duplicate name
**Expected Result**: Validation errors for each case.
**Type**: Negative

---

### PC-09: Non-admin category management
**Role**: PSYCHOLOGIST
**Precondition**: Logged in as psych1
**Steps**:
1. Try to access category management page
2. Try API: `POST /api/v1/admin/psychotherapy-categories`
**Expected Result**: 403 Forbidden. Psychologists can only READ categories, not manage them.
**Type**: Permission

---

## Activity Test Cases

### PA-01: Create activity - priced category
**Role**: PSYCHOLOGIST
**Precondition**: Logged in as psych1, active admission exists, priced category exists (e.g., Q500)
**Steps**:
1. Open active admission
2. Navigate to Psychotherapy Activities tab
3. Click "Register Activity"
4. Select the priced category (Q500)
5. Enter notes: "Individual CBT session, 45 minutes"
6. Set activity date
7. Submit
**Expected Result**: Activity created. SERVICE billing charge of Q500 auto-generated for this admission. Success notification.
**Type**: Happy Path

---

### PA-02: Create activity - unpriced category
**Role**: PSYCHOLOGIST
**Precondition**: Logged in as psych1, active admission, unpriced category exists
**Steps**:
1. Register activity with the unpriced category
2. Submit
3. Check billing for the admission
**Expected Result**: Activity created successfully. NO billing charge generated.
**Type**: Happy Path

---

### PA-03: List activities for admission
**Role**: PSYCHOLOGIST
**Precondition**: Logged in as psych1, admission has multiple activities
**Steps**:
1. Open admission with psychotherapy activities
2. View activity list
**Expected Result**: Activities listed with category, date, notes, created by. Sorted by date.
**Type**: Happy Path

---

### PA-04: Multiple activities per admission
**Role**: PSYCHOLOGIST
**Precondition**: Logged in as psych1
**Steps**:
1. Create 3 activities for the same admission (different categories and dates)
2. View list
**Expected Result**: All 3 activities shown. Each has correct category and date.
**Type**: Happy Path

---

### PA-05: Activity without notes
**Role**: PSYCHOLOGIST
**Precondition**: Logged in as psych1
**Steps**:
1. Create activity without entering notes (if notes are optional)
2. Submit
**Expected Result**: Activity created. Notes field empty but no error.
**Type**: Happy Path

---

### PA-06: Activity - doctor cannot create
**Role**: DOCTOR
**Precondition**: Logged in as doctor1
**Steps**:
1. Open an admission
2. Try to access Psychotherapy Activities
3. Try API: `POST /api/v1/admissions/{id}/psychotherapy-activities`
**Expected Result**: 403 Forbidden.
**Type**: Permission

---

### PA-07: Activity - nurse cannot create
**Role**: NURSE
**Precondition**: Logged in as nurse1
**Steps**:
1. Try to create a psychotherapy activity
**Expected Result**: 403 Forbidden.
**Type**: Permission

---

### PA-08: Activity - staff cannot create
**Role**: ADMINISTRATIVE_STAFF
**Precondition**: Logged in as staff1
**Steps**:
1. Try to create a psychotherapy activity
**Expected Result**: 403 Forbidden.
**Type**: Permission

---

### PA-09: Activity for discharged admission
**Role**: PSYCHOLOGIST
**Precondition**: Logged in as psych1, discharged admission
**Steps**:
1. Open discharged admission
2. Try to register activity
**Expected Result**: Cannot create activities for discharged admissions.
**Type**: Negative

---

### PA-10: Verify billing charge from priced activity
**Role**: ADMIN
**Precondition**: A psychotherapy activity was created with a priced category (PA-01)
**Steps**:
1. Log in as admin
2. Navigate to Billing for the admission where the activity was created
3. Look for a SERVICE charge
**Expected Result**: SERVICE charge exists with amount matching the category price. Description references psychotherapy.
**Type**: Happy Path

---

### PA-11: Verify no billing charge from unpriced activity
**Role**: ADMIN
**Precondition**: A psychotherapy activity was created with an unpriced category (PA-02)
**Steps**:
1. Log in as admin
2. Navigate to Billing for the admission
3. Check that no new SERVICE charge was added for the unpriced activity
**Expected Result**: No charge generated for unpriced category activity.
**Type**: Happy Path

---

### PA-12: Delete activity - admin only
**Role**: ADMIN
**Precondition**: Logged in as admin, a psychotherapy activity exists (created in PA-01 or seed data), and a billing charge was generated for the priced activity
**Steps**:
1. Open admission detail
2. Navigate to Psychotherapy Activities
3. Click delete on an existing activity
4. Confirm deletion
5. Check billing charges for the admission
**Expected Result**: Activity soft-deleted. No longer appears in the activity list. The associated billing charge (if any) persists and is NOT removed - charges are immutable financial records.
**Type**: Happy Path

---

### PA-13: Delete activity - psychologist denied
**Role**: PSYCHOLOGIST
**Precondition**: Logged in as psych1, a psychotherapy activity exists
**Steps**:
1. Open admission with psychotherapy activities
2. Look for delete button on an activity (should not be visible)
3. Try API: `DELETE /api/v1/admissions/{id}/psychotherapy-activities/{actId}`
**Expected Result**: 403 Forbidden. PSYCHOLOGIST has `psychotherapy-activity:create` and `psychotherapy-activity:read` but NOT `psychotherapy-activity:delete`.
**Type**: Permission

---

## Permission Matrix

| Action | ADMIN | STAFF | DOCTOR | PSYCH | NURSE | CHIEF_NURSE | USER |
|--------|-------|-------|--------|-------|-------|-------------|------|
| List categories | G | D | G | G | G | G | D |
| Create category | G | D | D | D | D | D | D |
| Update category | G | D | D | D | D | D | D |
| Delete category | G | D | D | D | D | D | D |
| Create activity | G | D | D | G | D | D | D |
| Read activities | G | D | G | G | G | G | D |
| Delete activity | G | D | D | D | D | D | D |

G = Granted, D = Denied

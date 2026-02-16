# Module: Medical Records

**Module ID**: CH (Clinical History), PN (Progress Notes), MO (Medical Orders)
**Required Permissions**: clinical-history:*, progress-note:*, medical-order:*
**API Base**: `/api/v1/admissions/{admissionId}/clinical-history`, `/api/v1/admissions/{admissionId}/progress-notes`, `/api/v1/admissions/{admissionId}/medical-orders`

---

## Overview

Three sub-modules for medical documentation within admissions:
1. **Clinical History** - One per admission, 16+ rich-text fields, comprehensive psychiatric assessment
2. **Progress Notes** - Multiple per admission, SOAP format (Subjective/Objective/Analysis/Plans)
3. **Medical Orders** - Multiple per admission, 11 categories, can be discontinued

**Append-only rule**: Non-admin clinical staff can CREATE but NOT UPDATE. Only ADMIN can edit existing records.

---

## Clinical History Test Cases

### CH-01: Create clinical history
**Role**: DOCTOR
**Precondition**: Logged in as doctor1, active admission exists WITHOUT a clinical history
**Steps**:
1. Open the active admission
2. Navigate to Clinical History tab
3. Verify empty state (no clinical history yet)
4. Click "Create Clinical History"
5. Fill key fields: reason for consultation, current condition, psychiatric history, diagnosis, management plan
6. Save
**Expected Result**: Clinical history created. All fields saved correctly. Rich text formatting preserved. createdBy shows doctor1.
**Type**: Happy Path

---

### CH-02: Create clinical history - all 16 fields
**Role**: DOCTOR
**Precondition**: Logged in as doctor1, admission without clinical history
**Steps**:
1. Create clinical history filling ALL fields:
   - Reason for consultation
   - Current condition
   - Psychiatric history
   - Family medical history
   - Family psychiatric history
   - Personal medical history
   - Personal surgical history
   - Allergies
   - Personal psychiatric history
   - Gynecological history (if applicable)
   - Substance use
   - Psychomotor development
   - Mental status exam
   - Temperament
   - Previous lab results
   - Previous treatments
   - Diagnosis
   - Management plan
   - Prognosis
2. Use rich text formatting (bold, italic, lists) in some fields
3. Save
**Expected Result**: All fields saved with rich text formatting preserved. All content viewable.
**Type**: Happy Path

---

### CH-03: Duplicate clinical history prevented
**Role**: DOCTOR
**Precondition**: Logged in as doctor1, admission already HAS a clinical history
**Steps**:
1. Open admission that already has clinical history
2. Try to create another one (button should not be shown)
3. Try API: `POST /api/v1/admissions/{id}/clinical-history`
**Expected Result**: UI does not show create button. API returns error (already exists).
**Type**: Negative

---

### CH-04: View clinical history
**Role**: DOCTOR
**Precondition**: Logged in as doctor1, admission has clinical history
**Steps**:
1. Open admission with existing clinical history
2. Navigate to Clinical History tab
3. Review all fields
**Expected Result**: All fields displayed with correct data. Rich text renders properly. Shows created by/date.
**Type**: Happy Path

---

### CH-05: Edit clinical history - admin only
**Role**: ADMIN
**Precondition**: Logged in as admin, admission has clinical history
**Steps**:
1. Open admission clinical history
2. Click Edit
3. Modify a field (e.g., add to diagnosis)
4. Save
**Expected Result**: Changes saved. updatedBy shows admin.
**Type**: Happy Path

---

### CH-06: Edit clinical history - doctor denied
**Role**: DOCTOR
**Precondition**: Logged in as doctor1, admission has clinical history created by doctor1
**Steps**:
1. Open the clinical history
2. Look for Edit button
3. Try API: `PUT /api/v1/admissions/{id}/clinical-history`
**Expected Result**: Edit button not shown for doctor. API returns 403 (append-only, non-admin cannot update).
**Type**: Permission

---

### CH-07: Clinical history - nurse can read
**Role**: NURSE
**Precondition**: Logged in as nurse1, admission has clinical history
**Steps**:
1. Open an admission with clinical history
2. Navigate to Clinical History tab
3. Verify clinical history is visible
4. Try API: `GET /api/v1/admissions/{id}/clinical-history`
**Expected Result**: Clinical history displayed (read-only). API returns 200.
**Type**: Permission

---

### CH-08: Clinical history - nurse cannot create or edit
**Role**: NURSE
**Precondition**: Logged in as nurse1
**Steps**:
1. Open an admission without clinical history
2. Look for "Create Clinical History" button
3. Try API: `POST /api/v1/admissions/{id}/clinical-history`
4. On admission with existing clinical history, try API: `PUT /api/v1/admissions/{id}/clinical-history`
**Expected Result**: Create button not shown. API returns 403 for both POST and PUT.
**Type**: Permission

---

### CH-09: Clinical history - discharged admission
**Role**: DOCTOR
**Precondition**: Logged in as doctor1, discharged admission without clinical history
**Steps**:
1. Open discharged admission
2. Try to create clinical history
**Expected Result**: Cannot create clinical history for discharged admission.
**Type**: Negative

---

## Progress Notes Test Cases

### PN-01: Create progress note - doctor
**Role**: DOCTOR
**Precondition**: Logged in as doctor1, active admission exists
**Steps**:
1. Open active admission
2. Navigate to Progress Notes tab
3. Click "New Progress Note"
4. Fill SOAP fields: Subjective, Objective, Analysis, Plans
5. Save
**Expected Result**: Progress note created. Appears in list. createdBy shows doctor1.
**Type**: Happy Path

---

### PN-02: Create progress note - nurse
**Role**: NURSE
**Precondition**: Logged in as nurse1, active admission exists
**Steps**:
1. Open active admission
2. Navigate to Progress Notes tab
3. Click "New Progress Note"
4. Fill SOAP fields
5. Save
**Expected Result**: Progress note created. createdBy shows nurse1.
**Type**: Happy Path

---

### PN-03: List progress notes with pagination
**Role**: DOCTOR
**Precondition**: Logged in as doctor1, admission has multiple progress notes
**Steps**:
1. Open admission with multiple progress notes
2. Verify list sorted by date DESC (newest first)
3. Verify pagination if many notes
**Expected Result**: Notes listed in reverse chronological order. Pagination works.
**Type**: Happy Path

---

### PN-04: View progress note detail
**Role**: DOCTOR
**Precondition**: Logged in as doctor1
**Steps**:
1. Click on a progress note in the list
2. Review all SOAP fields
**Expected Result**: All fields displayed. Rich text renders. Shows author and date.
**Type**: Happy Path

---

### PN-05: Edit progress note - admin only
**Role**: ADMIN
**Precondition**: Logged in as admin
**Steps**:
1. Open a progress note
2. Click Edit
3. Modify content
4. Save
**Expected Result**: Changes saved. updatedBy shows admin.
**Type**: Happy Path

---

### PN-06: Edit progress note - doctor denied
**Role**: DOCTOR
**Precondition**: Logged in as doctor1
**Steps**:
1. Open a progress note (even one created by doctor1)
2. Try to edit
**Expected Result**: Edit not allowed. Append-only for non-admin.
**Type**: Permission

---

### PN-07: Progress note - psychologist cannot create
**Role**: PSYCHOLOGIST
**Precondition**: Logged in as psych1
**Steps**:
1. Open an admission
2. Try to create a progress note
3. Try API: `POST /api/v1/admissions/{id}/progress-notes`
**Expected Result**: 403 Forbidden. Psychologists don't have progress-note:create.
**Type**: Permission

---

### PN-08: Progress note - empty state
**Role**: DOCTOR
**Precondition**: Active admission with no progress notes
**Steps**:
1. Open admission
2. Navigate to Progress Notes
**Expected Result**: Empty state message displayed. "Create" button available.
**Type**: Happy Path

---

### PN-09: Progress note - discharged admission
**Role**: DOCTOR
**Precondition**: Logged in as doctor1, discharged admission
**Steps**:
1. Open discharged admission
2. Try to create a progress note
**Expected Result**: Cannot create notes for discharged admission.
**Type**: Negative

---

## Medical Orders Test Cases

### MO-01: Create medical order - general
**Role**: DOCTOR
**Precondition**: Logged in as doctor1, active admission
**Steps**:
1. Open active admission
2. Navigate to Medical Orders tab
3. Click "New Order"
4. Select category: ORDENES_GENERALES
5. Enter description
6. Save
**Expected Result**: Order created. Appears in the ORDENES_GENERALES group.
**Type**: Happy Path

---

### MO-02: Create medical order - medication with details
**Role**: DOCTOR
**Precondition**: Logged in as doctor1, active admission, inventory items exist
**Steps**:
1. Create new medical order
2. Select category: MEDICAMENTOS
3. Fill medication-specific fields: name, dosage, route, frequency, schedule
4. Link an inventory item (for MAR and billing)
5. Save
**Expected Result**: Order created with medication details. Inventory item linked. Appears in MEDICAMENTOS group.
**Type**: Happy Path

---

### MO-03: Create medical order - each category
**Role**: DOCTOR
**Precondition**: Logged in as doctor1, active admission
**Steps**:
1. Create orders for each of the 11 categories:
   - ORDENES_GENERALES
   - MEDICAMENTOS
   - LABORATORIOS
   - REFERENCIAS_MEDICAS
   - PRUEBAS_PSICOMETRICAS
   - ACTIVIDAD_FISICA
   - CUIDADOS_ESPECIALES
   - DIETA
   - RESTRICCIONES_MOVILIDAD
   - PERMISOS_VISITA
   - OTROS
**Expected Result**: All categories work. Orders grouped correctly by category in the UI.
**Type**: Happy Path

---

### MO-04: Medical orders grouped by category
**Role**: DOCTOR
**Precondition**: Logged in as doctor1, admission has orders in multiple categories
**Steps**:
1. Open admission with multiple order categories
2. Review the grouping
**Expected Result**: Orders organized into category sections/tabs. Each category shows its orders.
**Type**: Happy Path

---

### MO-05: Discontinue medical order
**Role**: DOCTOR
**Precondition**: Logged in as doctor1, an active medical order exists
**Steps**:
1. Open the medical order
2. Click "Discontinue"
3. Confirm
**Expected Result**: Order status changes to discontinued. Cannot be used for medication administration.
**Type**: Happy Path

---

### MO-06: Edit medical order - admin only
**Role**: ADMIN
**Precondition**: Logged in as admin
**Steps**:
1. Open a medical order
2. Edit description or details
3. Save
**Expected Result**: Changes saved.
**Type**: Happy Path

---

### MO-07: Edit medical order - doctor denied
**Role**: DOCTOR
**Precondition**: Logged in as doctor1
**Steps**:
1. Open a medical order
2. Try to edit
**Expected Result**: Edit not allowed (append-only for doctors).
**Type**: Permission

---

### MO-08: Medical order - nurse cannot create
**Role**: NURSE
**Precondition**: Logged in as nurse1
**Steps**:
1. Open an admission
2. Try to create a medical order
3. Try API: `POST /api/v1/admissions/{id}/medical-orders`
**Expected Result**: 403 Forbidden.
**Type**: Permission

---

### MO-09: Medical order - psychologist cannot create
**Role**: PSYCHOLOGIST
**Precondition**: Logged in as psych1
**Steps**:
1. Try to create a medical order
**Expected Result**: 403 Forbidden.
**Type**: Permission

---

### MO-10: Medical order with inventory item link
**Role**: DOCTOR
**Precondition**: Logged in as doctor1, inventory items exist with stock
**Steps**:
1. Create MEDICAMENTOS order
2. In the inventory item field, search and select an item
3. Save
4. Verify the linked item is shown on the order
**Expected Result**: Inventory item linked. This enables medication administration and auto-billing.
**Type**: Happy Path

---

### MO-11: Medical order - billlable categories trigger charges
**Role**: DOCTOR
**Precondition**: Logged in as doctor1, inventory items linked
**Steps**:
1. Create medical order for LABORATORIOS with inventory item
2. Check billing for the admission
3. Create medical order for CUIDADOS_ESPECIALES with inventory item
4. Check billing again
**Expected Result**: LAB charge created for lab order. PROCEDURE charge created for special care order.
**Type**: Happy Path

---

### MO-12: Discharged admission - cannot create orders
**Role**: DOCTOR
**Precondition**: Logged in as doctor1, discharged admission
**Steps**:
1. Open discharged admission
2. Try to create a medical order
**Expected Result**: Cannot create orders for discharged admission.
**Type**: Negative

---

## Permission Matrix

| Action | ADMIN | STAFF | DOCTOR | PSYCH | NURSE | CHIEF_NURSE | USER |
|--------|-------|-------|--------|-------|-------|-------------|------|
| Create clinical history | G | D | G | D | D | D | D |
| Read clinical history | G | D | G | D | G | G | D |
| Update clinical history | G | D | D | D | D | D | D |
| Create progress note | G | D | G | D | G | G | D |
| Read progress notes | G | D | G | D | G | G | D |
| Update progress note | G | D | D | D | D | D | D |
| Create medical order | G | D | G | D | D | D | D |
| Read medical orders | G | D | G | D | G | G | D |
| Update medical order | G | D | D | D | D | D | D |
| Discontinue order | G | D | G | D | D | D | D |

G = Granted, D = Denied

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

### MO-13: Newly created order starts in SOLICITADO
**Role**: DOCTOR
**Precondition**: Logged in as doctor1, active admission
**Steps**:
1. Create a medical order for any category
2. Open the order card
**Expected Result**: Order shows the `Solicitado` state badge. No billing charge has been created yet, even for billable categories with a linked inventory item.
**Type**: Happy Path

---

### MO-14: Administrative staff authorizes a SOLICITADO order
**Role**: ADMINISTRATIVE_STAFF
**Precondition**: Logged in as admin-staff user, an order in `SOLICITADO` exists (LABORATORIOS with linked inventory item)
**Steps**:
1. Open `/medical-orders` (orders by state)
2. Filter by status `SOLICITADO`
3. Click `Authorize` on the order
**Expected Result**: Order transitions to `AUTORIZADO`. `authorizedAt` and `authorizedBy` are stamped. The corresponding LAB billing charge is created on the admission. The row leaves the `SOLICITADO` bucket.
**Type**: Happy Path

---

### MO-15: Administrative staff rejects an order with reason
**Role**: ADMINISTRATIVE_STAFF
**Precondition**: A `SOLICITADO` order exists
**Steps**:
1. From `/medical-orders`, click `Reject` on the order
2. Enter a rejection reason ("Pendiente cobertura del seguro")
3. Confirm
**Expected Result**: Order transitions to `NO_AUTORIZADO` (terminal). The rejection reason is stored and visible on the order card. No billing charge is created.
**Type**: Happy Path

---

### MO-16: Nurse marks an authorized lab order as in progress
**Role**: NURSE (or DOCTOR)
**Precondition**: An `AUTORIZADO` order with category `LABORATORIOS` exists
**Steps**:
1. From `/medical-orders` or the admission detail, click `Sample taken` (label varies by category — `Patient referred` for referrals, `Test administered` for psychometric)
**Expected Result**: Order transitions to `EN_PROCESO`. `inProgressAt` and `inProgressBy` are stamped from the nurse. After this transition the order can no longer be discontinued.
**Type**: Happy Path

---

### MO-17: Mark-in-progress denied for non-results category
**Role**: NURSE
**Precondition**: An `AUTORIZADO` order with category `MEDICAMENTOS` exists
**Steps**:
1. Try to invoke `Mark in progress` (UI button should be hidden; API call must be blocked)
**Expected Result**: API returns 400 with a clear validation message. UI does not render the button for this category.
**Type**: Negative

---

### MO-18: Uploading a result document auto-marks the order RESULTADOS_RECIBIDOS
**Role**: DOCTOR (or NURSE)
**Precondition**: An order in `EN_PROCESO` (or `AUTORIZADO`) exists for a results-bearing category
**Steps**:
1. Open the order
2. Upload a result document (PDF or image)
**Expected Result**: After the upload completes, the order's state badge updates to `Resultados recibidos`. `resultsReceivedAt` and `resultsReceivedBy` are stamped from the uploader. The document appears in the attachments grid and is viewable by all roles with `medical-order:read`. Uploading from `AUTORIZADO` skips `EN_PROCESO`.
**Type**: Happy Path

---

### MO-19: Cannot manually set RESULTADOS_RECIBIDOS without a document
**Role**: ADMIN
**Precondition**: An order in `EN_PROCESO` exists with no documents uploaded
**Steps**:
1. Attempt to PUT the order with `status="RESULTADOS_RECIBIDOS"` directly via API
**Expected Result**: API returns 400. The only path to `RESULTADOS_RECIBIDOS` is through document upload.
**Type**: Negative

---

### MO-20: Authorize denied when order is not SOLICITADO
**Role**: ADMINISTRATIVE_STAFF
**Precondition**: An order in `AUTORIZADO` (or any non-`SOLICITADO` state) exists
**Steps**:
1. Attempt to authorize via API
**Expected Result**: API returns 400. UI does not show the `Authorize` button for this order.
**Type**: Negative

---

### MO-21: Discontinue blocked from EN_PROCESO and terminal states
**Role**: DOCTOR
**Precondition**: An order in `EN_PROCESO`, `NO_AUTORIZADO`, `RESULTADOS_RECIBIDOS`, or `DESCONTINUADO` exists
**Steps**:
1. Attempt to discontinue via API
**Expected Result**: API returns 400. UI hides the `Discontinue` button for these states. The error message for `EN_PROCESO` clarifies that the sample is at the lab and the order can no longer be cancelled.
**Type**: Negative

---

### MO-21b: Authorize denied for directive categories
**Role**: ADMINISTRATIVE_STAFF
**Precondition**: A `DIETA` order (in `ACTIVA`) exists
**Steps**:
1. Attempt to authorize via API
**Expected Result**: API returns 400 — directive categories don't have an authorization step. UI does not render the `Authorize` button for directive orders.
**Type**: Negative

---

### MO-21c: Doctor emergency-authorizes a SOLICITADO order
**Role**: DOCTOR
**Precondition**: A `SOLICITADO` order (medication, lab, referral, or psychometric) exists
**Steps**:
1. From `/medical-orders` or the order card, click `Emergency authorize`
2. In the dialog, choose a structured reason (`Patient in crisis` / `After-hours, no admin staff available` / `Family unreachable` / `Other`)
3. If reason is `Other`, fill in the required note
4. Submit
**Expected Result**: Order transitions to `AUTORIZADO`. `emergencyAuthorized=true`, `emergencyReason`, `emergencyAt`, `emergencyBy` (and optional `emergencyReasonNote`) are stamped. Billing fires the same as a normal authorize. The card shows an "Emergency-authorized" banner with the reason.
**Type**: Happy Path

---

### MO-21d: Emergency-authorize requires reasonNote when reason is OTHER
**Role**: DOCTOR
**Precondition**: A `SOLICITADO` order exists
**Steps**:
1. Submit an emergency-authorize call with `reason="OTHER"` and no `reasonNote`
**Expected Result**: API returns 400. UI submit button is disabled until the note is filled in.
**Type**: Negative

---

### MO-22: Orders-by-state dashboard - filtering and navigation
**Role**: ADMINISTRATIVE_STAFF
**Precondition**: Several orders in mixed states across multiple admissions
**Steps**:
1. Navigate to `/medical-orders` from the sidebar
2. Default filter selects the action-needed buckets (`SOLICITADO`, `AUTORIZADO`, `EN_PROCESO`) — verify only those orders are shown
3. Switch the status filter to multi-select `EN_PROCESO, RESULTADOS_RECIBIDOS`
4. Apply category filter `LABORATORIOS`
5. Click on a row to open the underlying admission detail
**Expected Result**: Default filter shows the action-needed buckets. Multi-select status and category filters narrow the list correctly across admissions. Each row links to its admission detail. State badge color matches the spec (green / gray / red / green / amber / blue / secondary).
**Type**: Happy Path

---

### MO-23: Sidebar entry hidden for roles without medical-order:read
**Role**: PSYCHOLOGIST (no `medical-order:read`)
**Precondition**: Logged in as a psychologist
**Steps**:
1. Inspect the sidebar
2. Try to navigate directly to `/medical-orders`
**Expected Result**: Sidebar does not show the `Medical Orders` entry. Direct navigation returns 403 / redirects to forbidden page.
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
| Read medical orders | G | G | G | D | G | G | D |
| Update medical order | G | D | D | D | D | D | D |
| Discontinue order | G | D | G | D | D | D | D |
| Authorize / reject order | G | G | D | D | D | D | D |
| Mark "claim results" | G | D | G | D | D | D | D |
| Upload result document (auto-claims) | G | D | G | D | G | D | D |
| Open `/medical-orders` dashboard | G | G | G | D | G | G | D |

G = Granted, D = Denied

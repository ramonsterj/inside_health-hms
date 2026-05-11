# Module: Nursing Module

**Module ID**: NN (Nursing Notes), VS (Vital Signs), MAR (Medication Administration)
**Required Permissions**: nursing-note:*, vital-sign:*, medication-administration:*
**API Base**: `/api/v1/admissions/{admissionId}/nursing-notes`, `/api/v1/admissions/{admissionId}/vital-signs`, `/api/v1/admissions/{admissionId}/medical-orders/{orderId}/administrations`

---

## Overview

Three nursing sub-modules:
1. **Nursing Notes** - Free-text observations per admission
2. **Vital Signs** - BP, heart rate, temperature, O2 sat — **admin-only update** (append-only for non-admins)
3. **Medication Administration Record (MAR)** - Records medication given/missed/refused/held, triggers inventory and billing

**Key rules**:
- Nursing notes and vital signs can only be updated by ADMIN (V096 + V097). Doctors, nurses, and chief nurses are append-only and must request edits from an administrator.
- Discharge protection blocks all writes (including for ADMIN) on both record types.
- MAR records are immutable (no edits after creation)
- GIVEN MAR status triggers inventory EXIT + billing MEDICATION charge
- NURSE, CHIEF_NURSE, DOCTOR, and ADMIN can create nursing notes and vital signs (V045)
- Only NURSE, CHIEF_NURSE, and ADMIN can administer medications (MAR)

---

## Nursing Notes Test Cases

### NN-01: Create nursing note
**Role**: NURSE
**Precondition**: Logged in as nurse1, active admission exists
**Steps**:
1. Open active admission
2. Navigate to Nursing Notes tab
3. Click "New Note"
4. Enter content describing patient observations
5. Save
**Expected Result**: Note created. Appears in list. createdBy shows nurse1.
**Type**: Happy Path

---

### NN-02: Create nursing note - chief nurse
**Role**: CHIEF_NURSE
**Precondition**: Logged in as chiefnurse1
**Steps**:
1. Open active admission
2. Create nursing note
3. Save
**Expected Result**: Note created. createdBy shows chiefnurse1.
**Type**: Happy Path

---

### NN-03: List nursing notes
**Role**: NURSE
**Precondition**: Logged in as nurse1, admission has multiple notes
**Steps**:
1. Open admission with nursing notes
2. Verify list sorted by date DESC
3. Test pagination if many notes
**Expected Result**: Notes listed in reverse chronological order.
**Type**: Happy Path

---

### NN-04: Edit nursing note
**Role**: NURSE
**Precondition**: Logged in as nurse1, note exists
**Steps**:
1. Open a nursing note
2. Click Edit
3. Modify content
4. Save
**Expected Result**: Note updated successfully.
**Type**: Happy Path

---

### NN-05: Nursing note - doctor can create
**Role**: DOCTOR
**Precondition**: Logged in as doctor1, active admission exists
**Steps**:
1. Open an active admission
2. Navigate to Nursing Notes tab (should be visible)
3. Click "New Note"
4. Enter content: "Patient improving, reduced agitation"
5. Save
**Expected Result**: Note created successfully. createdBy shows doctor1. V045 grants DOCTOR all nursing-note permissions.
**Type**: Happy Path

---

### NN-06: Nursing note - psychologist cannot create
**Role**: PSYCHOLOGIST
**Precondition**: Logged in as psych1
**Steps**:
1. Try to create a nursing note
**Expected Result**: 403 Forbidden.
**Type**: Permission

---

### NN-07: Nursing note - discharged admission
**Role**: NURSE
**Precondition**: Logged in as nurse1, discharged admission
**Steps**:
1. Open discharged admission
2. Try to create a nursing note
**Expected Result**: Cannot create notes for discharged admission.
**Type**: Negative

---

### NN-08: Nursing note - psychologist cannot create
**Role**: PSYCHOLOGIST
**Precondition**: Logged in as psych1
**Steps**:
1. Open an active admission
2. Look for Nursing Notes tab (should be hidden or inaccessible)
3. Try API: `POST /api/v1/admissions/{id}/nursing-notes`
**Expected Result**: 403 Forbidden. PSYCHOLOGIST has no nursing-note permissions.
**Type**: Permission

---

## Vital Signs Test Cases

### VS-01: Create vital signs - all fields
**Role**: NURSE
**Precondition**: Logged in as nurse1, active admission
**Steps**:
1. Open active admission
2. Navigate to Vital Signs tab
3. Click "Record Vital Signs"
4. Fill all fields:
   - Systolic BP: 120
   - Diastolic BP: 80
   - Heart Rate: 72
   - Respiratory Rate: 16
   - Temperature: 36.5
   - Oxygen Saturation: 98
   - Notes: "Patient resting comfortably"
5. Save
**Expected Result**: Vital signs recorded. Appears in list. All values correct.
**Type**: Happy Path

---

### VS-02: Multiple vital sign readings per day
**Role**: NURSE
**Precondition**: Logged in as nurse1, admission already has vital signs for today
**Steps**:
1. Create another vital signs entry for the same admission
2. Use different values
3. Save
**Expected Result**: Second reading saved. Both entries visible in list.
**Type**: Happy Path

---

### VS-03: Edit vital signs - admin can update on active admission
**Role**: ADMIN
**Precondition**: Logged in as admin, vital signs exist on an active admission
**Steps**:
1. Open a vital signs entry
2. Click Edit
3. Change a value (e.g., heart rate from 72 to 75)
4. Save
**Expected Result**: Edit succeeds. Updated value saved with `updatedBy = admin`.
**Type**: Happy Path

---

### VS-04: Edit vital signs - non-admin denied
**Role**: NURSE / DOCTOR / CHIEF_NURSE
**Precondition**: Logged in as nurse1 (or doctor1, or chiefnurse1), vital signs exist
**Steps**:
1. Open any vital signs entry (own or others')
2. Try to edit (button should be hidden — `canEdit = false`)
3. Try API: `PUT /api/v1/admissions/{id}/vital-signs/{vsId}`
**Expected Result**: Edit button hidden. API returns 403 Forbidden — only ADMIN holds `vital-sign:update` after V097.
**Type**: Permission

---

### VS-05: Edit vital signs - admin denied for discharged admission
**Role**: ADMIN
**Precondition**: Admission discharged, vital signs exist
**Steps**:
1. Open discharged admission
2. Try to edit a vital signs entry
**Expected Result**: Cannot edit vital signs once admission is discharged, even as ADMIN. API returns 400.
**Type**: Negative

---

### VS-06: Vital signs validation - boundary values
**Role**: NURSE
**Precondition**: Logged in as nurse1
**Steps**:
1. Try to enter systolic BP of 0 or 500
2. Try temperature of 50.0
3. Try heart rate of -10
4. Try oxygen saturation of 150
**Expected Result**: Validation errors for out-of-range values.
**Type**: Negative

---

### VS-07: Vital signs - doctor can create
**Role**: DOCTOR
**Precondition**: Logged in as doctor1, active admission exists
**Steps**:
1. Open an active admission
2. Navigate to Vital Signs tab (should be visible)
3. Click "Record Vital Signs"
4. Fill: systolic 118, diastolic 76, HR 70, temp 36.4, O2 97%
5. Save
**Expected Result**: Vital signs recorded successfully. createdBy shows doctor1. V045 grants DOCTOR all vital-sign permissions.
**Type**: Happy Path

---

### VS-08: Vital signs - chief nurse can create but cannot edit
**Role**: CHIEF_NURSE
**Precondition**: Logged in as chiefnurse1
**Steps**:
1. Create vital signs for an admission
2. Try to edit them
**Expected Result**: Create succeeds. Edit button hidden — chief nurse no longer holds `vital-sign:update` after V097. API returns 403 if called directly.
**Type**: Permission

---

### VS-09: Vital signs - psychologist cannot create
**Role**: PSYCHOLOGIST
**Precondition**: Logged in as psych1
**Steps**:
1. Open an active admission
2. Look for Vital Signs tab (should be hidden or inaccessible)
3. Try API: `POST /api/v1/admissions/{id}/vital-signs`
**Expected Result**: 403 Forbidden. PSYCHOLOGIST has no vital-sign permissions.
**Type**: Permission

---

## Medication Administration Record (MAR) Test Cases

### MAR-01: Administer medication - GIVEN status
**Role**: NURSE
**Precondition**: Logged in as nurse1, active admission with MEDICAMENTOS medical order that has an inventory item linked, inventory item has stock
**Steps**:
1. Open active admission
2. Navigate to the medication medical order
3. Click "Administer Medication"
4. Select status: GIVEN
5. Add notes: "Administered orally with water"
6. Submit
**Expected Result**: MAR record created. Status GIVEN. `billable: true` in response. Inventory item stock decreased by 1. MEDICATION billing charge created for this admission.
**Type**: Happy Path

---

### MAR-02: Record medication - MISSED status
**Role**: NURSE
**Precondition**: Same as MAR-01
**Steps**:
1. Open medication order
2. Click "Administer Medication"
3. Select status: MISSED
4. Add notes: "Patient was sleeping at scheduled time"
5. Submit
**Expected Result**: MAR record created. Status MISSED. NO inventory deduction. NO billing charge. `billable: false` in response.
**Type**: Happy Path

---

### MAR-03: Record medication - REFUSED status
**Role**: NURSE
**Precondition**: Same as MAR-01
**Steps**:
1. Select status: REFUSED
2. Add notes: "Patient refused medication, states nausea"
3. Submit
**Expected Result**: MAR record created. Status REFUSED. NO inventory or billing impact.
**Type**: Happy Path

---

### MAR-04: Record medication - HELD status
**Role**: NURSE
**Precondition**: Same as MAR-01
**Steps**:
1. Select status: HELD
2. Add notes: "Held pending lab results"
3. Submit
**Expected Result**: MAR record created. Status HELD. NO inventory or billing impact.
**Type**: Happy Path

---

### MAR-05: MAR history with pagination
**Role**: NURSE
**Precondition**: Logged in as nurse1, medical order has multiple MAR entries
**Steps**:
1. Open medication order
2. View administration history
3. Verify sorted by administeredAt DESC
4. Test pagination
**Expected Result**: History shows all administrations with status, notes, date, administered by. Sorted correctly.
**Type**: Happy Path

---

### MAR-06: MAR immutability - cannot edit
**Role**: NURSE, ADMIN
**Precondition**: MAR record exists
**Steps**:
1. Open a MAR record
2. Look for Edit button (should not exist)
3. Try API: `PUT /api/v1/admissions/{id}/medical-orders/{orderId}/administrations/{marId}`
**Expected Result**: No edit capability. MAR records are immutable.
**Type**: Negative

---

### MAR-07: MAR requires inventory item on order
**Role**: NURSE
**Precondition**: Logged in as nurse1, medication order WITHOUT inventory item linked
**Steps**:
1. Open medication order that has no inventory item
2. Try to administer
**Expected Result**: Error: medical order must have inventory item linked before administration.
**Type**: Negative

---

### MAR-08: MAR only for MEDICAMENTOS category
**Role**: NURSE
**Precondition**: Logged in as nurse1
**Steps**:
1. Open a non-medication medical order (e.g., DIETA or LABORATORIOS)
2. Look for "Administer" button
**Expected Result**: Administer button only appears for MEDICAMENTOS category orders.
**Type**: Negative

---

### MAR-09: MAR for discontinued order
**Role**: NURSE
**Precondition**: A MEDICAMENTOS order has been discontinued
**Steps**:
1. Open the discontinued medication order
2. Try to administer
**Expected Result**: Cannot administer discontinued orders. Button disabled or error message.
**Type**: Negative

---

### MAR-10: MAR for discharged admission
**Role**: NURSE
**Precondition**: Discharged admission
**Steps**:
1. Open discharged admission
2. Try to administer medication
**Expected Result**: Cannot administer for discharged admissions.
**Type**: Negative

---

### MAR-11: MAR - doctor cannot administer
**Role**: DOCTOR
**Precondition**: Logged in as doctor1
**Steps**:
1. Open an admission with medication orders
2. Try to administer medication
3. Try API: `POST .../administrations`
**Expected Result**: 403 Forbidden. Doctors cannot administer medications.
**Type**: Permission

---

### MAR-12: MAR - verify inventory deduction
**Role**: ADMIN + NURSE
**Precondition**: Inventory item has known stock level
**Steps**:
1. As admin, note current stock level of an inventory item
2. Switch to nurse1
3. Administer medication (GIVEN) linked to that item
4. Switch to admin, check inventory item stock
**Expected Result**: Stock level decreased by exactly 1.
**Type**: Happy Path

---

### MAR-13: MAR - verify billing charge amount
**Role**: ADMIN + NURSE
**Precondition**: Inventory item has a known price (e.g., Q50.00)
**Steps**:
1. As nurse1, administer medication GIVEN
2. As admin, check billing charges for the admission
**Expected Result**: MEDICATION charge created with amount matching the inventory item's price.
**Type**: Happy Path

---

### MAR-14: MAR status badges
**Role**: NURSE
**Precondition**: MAR history has entries with all 4 statuses
**Steps**:
1. Open MAR history
2. Verify visual status badges for GIVEN, MISSED, REFUSED, HELD
**Expected Result**: Each status has a distinct visual badge/color for easy identification.
**Type**: Happy Path

---

## Permission Matrix

| Action | ADMIN | STAFF | DOCTOR | PSYCH | NURSE | CHIEF_NURSE | USER |
|--------|-------|-------|--------|-------|-------|-------------|------|
| Create nursing note | G | D | G | D | G | G | D |
| Read nursing notes | G | D | G | D | G | G | D |
| Update nursing note (admin-only after V096) | G | D | D | D | D | D | D |
| Create vital signs | G | D | G | D | G | G | D |
| Read vital signs | G | D | G | D | G | G | D |
| Update vital signs (admin-only after V097) | G | D | D | D | D | D | D |
| Create MAR | G | D | D | D | G | G | D |
| Read MAR | G | D | G | D | G | G | D |

G = Granted, D = Denied

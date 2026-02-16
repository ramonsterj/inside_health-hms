# Module: Admission Management

**Module ID**: ADM
**Required Permissions**: admission:create, admission:read, admission:update, admission:list, admission:discharge
**API Base**: `/api/v1/admissions`, `/api/v1/admin/triage-codes`, `/api/v1/admin/rooms`

---

## Overview

Hospital admission management with multi-step wizard, room capacity tracking, triage codes, treating/consulting physicians, consent documents, and discharge flow. Admission types include HOSPITALIZATION, AMBULATORY, EMERGENCY, ELECTROSHOCK_THERAPY, and KETAMINE_INFUSION.

---

## Admission Wizard Test Cases

### ADM-01: Complete admission - HOSPITALIZATION
**Role**: ADMINISTRATIVE_STAFF
**Precondition**: Logged in as staff1, patients/rooms/triage codes exist in seed data
**Steps**:
1. Navigate to Admissions page
2. Click "New Admission"
3. **Step 1 - Patient**: Select/confirm a patient (e.g., "Juan Perez Gonzalez")
4. **Step 2 - Details**: Select type HOSPITALIZATION, triage code, room (with available beds), treating physician (doctor1 or doctor2), admission date
5. **Step 3 - Additional**: Optionally add patient belongings, upload consent document
6. **Step 4 - Review**: Verify all entered data, submit
**Expected Result**: Admission created with status ACTIVE. Room available beds decreased by 1. Success notification. Admission appears in list.
**Type**: Happy Path

---

### ADM-02: Complete admission - AMBULATORY
**Role**: ADMINISTRATIVE_STAFF
**Precondition**: Logged in as staff1
**Steps**:
1. Follow admission wizard with type AMBULATORY
2. Note that room may not be required for ambulatory visits
3. Submit
**Expected Result**: Admission created. Status ACTIVE.
**Type**: Happy Path

---

### ADM-03: Complete admission - EMERGENCY
**Role**: ADMINISTRATIVE_STAFF
**Precondition**: Logged in as staff1
**Steps**:
1. Follow admission wizard with type EMERGENCY
2. Submit
**Expected Result**: Admission created with EMERGENCY type.
**Type**: Happy Path

---

### ADM-04: Complete admission - ELECTROSHOCK_THERAPY
**Role**: ADMINISTRATIVE_STAFF
**Precondition**: Logged in as staff1
**Steps**:
1. Follow admission wizard with type ELECTROSHOCK_THERAPY
2. Submit
3. Check billing for this admission
**Expected Result**: Admission created. PROCEDURE charge auto-generated in billing.
**Type**: Happy Path

---

### ADM-05: Complete admission - KETAMINE_INFUSION
**Role**: ADMINISTRATIVE_STAFF
**Precondition**: Logged in as staff1
**Steps**:
1. Follow admission wizard with type KETAMINE_INFUSION
2. Submit
3. Check billing for this admission
**Expected Result**: Admission created. PROCEDURE charge auto-generated in billing.
**Type**: Happy Path

---

### ADM-06: Wizard step validation
**Role**: ADMINISTRATIVE_STAFF
**Precondition**: Logged in as staff1
**Steps**:
1. Start admission wizard
2. Try to proceed from Step 1 without selecting a patient
3. Try to proceed from Step 2 without selecting type, room, triage code
4. Observe validation messages
**Expected Result**: Cannot proceed to next step without completing required fields. Clear validation messages shown.
**Type**: Negative

---

### ADM-07: Room capacity - full room not shown
**Role**: ADMINISTRATIVE_STAFF
**Precondition**: Logged in as staff1, a room is at full capacity (all beds taken)
**Steps**:
1. Start admission wizard
2. In Step 2, open the room dropdown
3. Look for the full-capacity room
**Expected Result**: Rooms with zero available beds are NOT shown in the dropdown (or are disabled).
**Type**: Negative

---

### ADM-08: Treating physician dropdown shows doctors only
**Role**: ADMINISTRATIVE_STAFF
**Precondition**: Logged in as staff1
**Steps**:
1. Start admission wizard
2. In Step 2, open the treating physician dropdown
3. Verify the listed users
**Expected Result**: Only users with DOCTOR role appear (doctor1, doctor2). No nurses, psychologists, or staff.
**Type**: Happy Path

---

### ADM-09: Upload consent document
**Role**: ADMINISTRATIVE_STAFF
**Precondition**: Logged in as staff1, in admission wizard Step 3
**Steps**:
1. In Step 3 of admission wizard, click "Upload Consent Document"
2. Select a valid file (PDF, under 25MB)
3. Upload
4. Complete the admission
**Expected Result**: Consent document uploaded and linked to admission. Viewable from admission detail.
**Type**: Happy Path

---

### ADM-10: List admissions with filters
**Role**: DOCTOR
**Precondition**: Logged in as doctor1, multiple admissions exist
**Steps**:
1. Navigate to Admissions page
2. View the list (verify columns: patient name, admission type, room, status, date)
3. Filter by status ACTIVE
4. Filter by status DISCHARGED
5. Clear filters
**Expected Result**: Filters work correctly. Only matching admissions shown.
**Type**: Happy Path

---

### ADM-11: View admission detail
**Role**: DOCTOR
**Precondition**: Logged in as doctor1
**Steps**:
1. Click on an active admission
2. Review all details: patient info, admission type, room, triage code, treating physician, dates, status
**Expected Result**: All admission details displayed correctly.
**Type**: Happy Path

---

### ADM-12: Update admission
**Role**: ADMINISTRATIVE_STAFF
**Precondition**: Logged in as staff1, active admission exists
**Steps**:
1. Open admission detail
2. Edit admission (e.g., change triage code or add notes)
3. Save
**Expected Result**: Changes saved. Success notification.
**Type**: Happy Path

---

### ADM-13: Discharge patient - doctor
**Role**: DOCTOR
**Precondition**: Logged in as doctor1, active HOSPITALIZATION admission exists
**Steps**:
1. Open the active admission
2. Click "Discharge" button
3. Confirm discharge
4. Verify admission status changes to DISCHARGED
5. Check room available beds (should increase by 1)
**Expected Result**: Status changed to DISCHARGED. Room bed freed. Billing charges and invoice triggered (verify in billing module).
**Type**: Happy Path

---

### ADM-14: Discharge - already discharged
**Role**: DOCTOR
**Precondition**: Logged in as doctor1, a discharged admission exists
**Steps**:
1. Open a discharged admission
2. Look for Discharge button
**Expected Result**: Discharge button not shown or disabled for already discharged admissions.
**Type**: Negative

---

### ADM-15: Discharge - only admin/doctor can discharge
**Role**: NURSE, ADMINISTRATIVE_STAFF
**Precondition**: Logged in as nurse1 or staff1
**Steps**:
1. Open an active admission
2. Look for Discharge button
3. Try API: `POST /api/v1/admissions/{id}/discharge`
**Expected Result**: Discharge button hidden for non-authorized roles. API returns 403.
**Type**: Permission

---

### ADM-16: Add consulting physician (interconsulta)
**Role**: DOCTOR
**Precondition**: Logged in as doctor1, active admission exists
**Steps**:
1. Open admission detail
2. Navigate to Consulting Physicians section
3. Click "Add Consulting Physician"
4. Select a doctor (doctor2) and enter reason
5. Save
**Expected Result**: Consulting physician added with reason. Appears in the list.
**Type**: Happy Path

---

### ADM-17: List consulting physicians
**Role**: DOCTOR
**Precondition**: Logged in as doctor1, admission has consulting physicians
**Steps**:
1. Open admission detail
2. Navigate to Consulting Physicians section
**Expected Result**: All consulting physicians listed with name, specialty, and reason.
**Type**: Happy Path

---

## Triage Code Management (Admin)

### ADM-18: List triage codes
**Role**: ADMIN
**Precondition**: Logged in as admin
**Steps**:
1. Navigate to Triage Code management (admin)
2. View list
**Expected Result**: Triage codes listed with code, description, color, display order, active status.
**Type**: Happy Path

---

### ADM-19: Create triage code
**Role**: ADMIN
**Precondition**: Logged in as admin
**Steps**:
1. Click "Create Triage Code"
2. Fill: code "D", description "Non-urgent", color "#00FF00", display order 4, active true
3. Save
**Expected Result**: Triage code created. Appears in list and in admission wizard dropdown.
**Type**: Happy Path

---

### ADM-20: Update triage code
**Role**: ADMIN
**Precondition**: Logged in as admin
**Steps**:
1. Edit an existing triage code
2. Change color and description
3. Save
**Expected Result**: Changes saved.
**Type**: Happy Path

---

### ADM-21: Delete triage code - no active admissions
**Role**: ADMIN
**Precondition**: Logged in as admin, triage code not used by active admissions
**Steps**:
1. Delete the unused triage code
2. Confirm
**Expected Result**: Triage code soft-deleted. Removed from list.
**Type**: Happy Path

---

### ADM-22: Delete triage code - has active admissions
**Role**: ADMIN
**Precondition**: Logged in as admin, triage code used by active admission
**Steps**:
1. Try to delete the triage code in use
**Expected Result**: Error message: cannot delete while referenced by active admissions.
**Type**: Negative

---

## Room Management (Admin)

### ADM-23: List rooms
**Role**: ADMIN
**Precondition**: Logged in as admin
**Steps**:
1. Navigate to Room management
2. View list
**Expected Result**: Rooms listed with name, type (PRIVATE/SHARED), capacity, floor, price, active status, available beds.
**Type**: Happy Path

---

### ADM-24: Create room
**Role**: ADMIN
**Precondition**: Logged in as admin
**Steps**:
1. Click "Create Room"
2. Fill: name "Room 201", type SHARED, capacity 4, floor "2nd", price 200.00, active true
3. Save
**Expected Result**: Room created. Appears in list. Available in admission wizard dropdown.
**Type**: Happy Path

---

### ADM-25: Update room
**Role**: ADMIN
**Precondition**: Logged in as admin
**Steps**:
1. Edit a room
2. Change price and capacity
3. Save
**Expected Result**: Changes saved. New price used for future daily charges.
**Type**: Happy Path

---

### ADM-26: Delete room - no active admissions
**Role**: ADMIN
**Precondition**: Logged in as admin, room has no active admissions
**Steps**:
1. Delete the room
2. Confirm
**Expected Result**: Room soft-deleted.
**Type**: Happy Path

---

### ADM-27: Delete room - has active admissions
**Role**: ADMIN
**Precondition**: Logged in as admin, room has active admissions
**Steps**:
1. Try to delete room with active admissions
**Expected Result**: Error: cannot delete room with active admissions.
**Type**: Negative

---

### ADM-28: Room capacity validation
**Role**: ADMIN
**Precondition**: Logged in as admin
**Steps**:
1. Try to create room with capacity 0 or negative
**Expected Result**: Validation error. Capacity must be > 0.
**Type**: Negative

---

### ADM-29: Non-admin triage/room management
**Role**: DOCTOR, NURSE, STAFF
**Precondition**: Logged in as non-admin
**Steps**:
1. Try to access room/triage code admin pages
2. Try API calls to CRUD endpoints
**Expected Result**: 403 Forbidden for all.
**Type**: Permission

---

### ADM-30: Permission - staff can create, nurse cannot
**Role**: NURSE
**Precondition**: Logged in as nurse1
**Steps**:
1. Navigate to Admissions page (should see list - admission:list)
2. Look for "New Admission" button (should be hidden - no admission:create)
3. Try API: `POST /api/v1/admissions`
**Expected Result**: Can view admissions. Cannot create. API returns 403.
**Type**: Permission

---

### ADM-31: Room gender matching
**Role**: ADMINISTRATIVE_STAFF
**Precondition**: Logged in as staff1. Rooms exist with gender restrictions (e.g., "Female Ward" accepts only FEMALE, "Male Ward" accepts only MALE). A mixed/unisex room also exists.
**Steps**:
1. Start admission wizard for a FEMALE patient
2. In Step 2, open the room dropdown
3. Observe available rooms
4. Cancel and start admission wizard for a MALE patient
5. In Step 2, open the room dropdown
**Expected Result**: Female patient sees only female-designated and unisex rooms. Male patient sees only male-designated and unisex rooms. Gender-mismatched rooms are not shown or are disabled.
**Type**: Happy Path

---

### ADM-32: Remove consulting physician
**Role**: DOCTOR
**Precondition**: Logged in as doctor1, active admission with at least one consulting physician added (ADM-16)
**Steps**:
1. Open admission detail
2. Navigate to Consulting Physicians section
3. Click remove/delete on an existing consulting physician
4. Confirm removal
**Expected Result**: Consulting physician removed from the admission. No longer appears in the list. The physician's user account is unaffected.
**Type**: Happy Path

---

## Document Type Management (Admin)

### DT-01: List document types
**Role**: ADMIN
**Precondition**: Logged in as admin, document types exist in seed data
**Steps**:
1. Navigate to Document Type management (admin settings)
2. View the list
**Expected Result**: Document types listed with code, name, description, and active status.
**Type**: Happy Path

---

### DT-02: Create document type
**Role**: ADMIN
**Precondition**: Logged in as admin
**Steps**:
1. Click "Create Document Type"
2. Fill: code "LAB_RESULT", name "Lab Result", description "Laboratory test results"
3. Save
**Expected Result**: Document type created. Appears in list. Available in admission document upload dropdown.
**Type**: Happy Path

---

### DT-03: Create document type - code pattern validation
**Role**: ADMIN
**Precondition**: Logged in as admin
**Steps**:
1. Try to create document type with invalid code (e.g., lowercase "lab result", special characters "LAB@RESULT")
2. Try duplicate code that already exists
**Expected Result**: Validation errors for invalid code pattern. Duplicate code rejected with server-side error.
**Type**: Negative

---

### DT-04: Update document type
**Role**: ADMIN
**Precondition**: Logged in as admin, document type exists
**Steps**:
1. Edit an existing document type
2. Change name and description
3. Save
**Expected Result**: Changes saved. Success notification.
**Type**: Happy Path

---

### DT-05: Delete document type - no documents
**Role**: ADMIN
**Precondition**: Logged in as admin, document type exists with no associated documents
**Steps**:
1. Delete the unused document type
2. Confirm
**Expected Result**: Document type soft-deleted. Removed from list and dropdowns.
**Type**: Happy Path

---

### DT-06: Delete document type - has documents (denied)
**Role**: ADMIN
**Precondition**: Logged in as admin, document type has associated admission documents
**Steps**:
1. Try to delete the document type that has documents
**Expected Result**: Error message: cannot delete document type with existing documents.
**Type**: Negative

---

### DT-07: Non-admin document type management
**Role**: ADMINISTRATIVE_STAFF, DOCTOR
**Precondition**: Logged in as staff1 or doctor1
**Steps**:
1. Try to access document type management page
2. Try API: `POST /api/v1/admin/document-types`
**Expected Result**: 403 Forbidden. Non-admin roles can only read document types (document-type:read), not manage them.
**Type**: Permission

---

## Admission Documents

### DOC-01: Upload document with type selection
**Role**: ADMINISTRATIVE_STAFF
**Precondition**: Logged in as staff1, active admission exists, document types configured
**Steps**:
1. Open admission detail
2. Navigate to Documents section
3. Click "Upload Document"
4. Select document type from dropdown (e.g., "Lab Result")
5. Select a valid file (PDF, JPG, PNG, under 25MB)
6. Upload
**Expected Result**: Document uploaded. Appears in document list with type, filename, upload date, and uploaded-by user.
**Type**: Happy Path

---

### DOC-02: List admission documents
**Role**: DOCTOR
**Precondition**: Logged in as doctor1, admission has uploaded documents
**Steps**:
1. Open admission detail
2. Navigate to Documents section
3. View document list
**Expected Result**: All documents listed with type, filename, date, and uploader. Documents accessible to roles with `admission:view-documents` permission.
**Type**: Happy Path

---

### DOC-03: Download document
**Role**: DOCTOR
**Precondition**: Logged in as doctor1, admission has documents
**Steps**:
1. Open admission document list
2. Click download on a document
**Expected Result**: File downloads correctly. Content matches the uploaded file.
**Type**: Happy Path

---

### DOC-04: View document thumbnail/preview
**Role**: NURSE
**Precondition**: Logged in as nurse1, admission has an image document (JPG/PNG)
**Steps**:
1. Open admission document list
2. Observe if thumbnails/previews are shown for image files
**Expected Result**: Image documents show a thumbnail or preview. PDF documents show a generic icon.
**Type**: Happy Path

---

### DOC-05: Delete document - admin only
**Role**: ADMIN
**Precondition**: Logged in as admin, admission has documents
**Steps**:
1. Open admission document list
2. Click delete on a document
3. Confirm deletion
**Expected Result**: Document soft-deleted. Removed from list. Only ADMIN has `admission:delete-documents` permission.
**Type**: Happy Path

---

### DOC-06: Delete document - non-admin denied
**Role**: ADMINISTRATIVE_STAFF
**Precondition**: Logged in as staff1, admission has documents
**Steps**:
1. Open admission document list
2. Look for delete button (should not be visible)
3. Try API: `DELETE /api/v1/admissions/{id}/documents/{docId}`
**Expected Result**: Delete button hidden. API returns 403. STAFF can upload but not delete documents.
**Type**: Permission

---

### DOC-07: Upload invalid file type
**Role**: ADMINISTRATIVE_STAFF
**Precondition**: Logged in as staff1
**Steps**:
1. Try to upload a file with unsupported extension (e.g., `.exe`, `.sh`)
2. Try to upload a file exceeding the size limit
**Expected Result**: Validation error for unsupported file type. Size limit error for oversized files.
**Type**: Negative

---

## Permission Matrix

| Action | ADMIN | STAFF | DOCTOR | PSYCH | NURSE | CHIEF_NURSE | USER |
|--------|-------|-------|--------|-------|-------|-------------|------|
| List admissions | G | G | G | G | G | G | D |
| View admission | G | G | G | G | G | G | D |
| Create admission | G | G | D | D | D | D | D |
| Update admission | G | G | G | D | D | G | D |
| Discharge | G | D | G | D | D | D | D |
| View documents | G | G | G | G | G | G | D |
| Upload documents | G | G | D | D | D | D | D |
| Download documents | G | G | G | G | G | G | D |
| Delete documents | G | D | D | D | D | D | D |
| Manage document types | G | D | D | D | D | D | D |
| Manage rooms | G | D | D | D | D | D | D |
| Manage triage codes | G | D | D | D | D | D | D |

G = Granted, D = Denied (403)

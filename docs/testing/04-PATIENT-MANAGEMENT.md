# Module: Patient Management

**Module ID**: PAT
**Required Permissions**: patient:create, patient:read, patient:update, patient:delete, patient:list
**API Base**: `/api/v1/patients`

---

## Overview

Patient registration and management module. Includes demographics, emergency contacts, and ID document uploads. Uses file system storage for documents. All deletions are soft deletes.

---

## Test Cases

### PAT-01: List patients with pagination
**Role**: ADMIN
**Precondition**: Logged in as admin, 20 seed patients exist
**Steps**:
1. Navigate to Patients page
2. Observe patient list table
3. Verify pagination (if >10/page, navigate pages)
4. Verify columns: name, age, sex, ID number, email
**Expected Result**: All 20 seed patients listed. Pagination works. Data matches seed data.
**Type**: Happy Path

---

### PAT-02: Search patients by name
**Role**: ADMIN
**Precondition**: Logged in as admin, seed patients exist
**Steps**:
1. Navigate to Patients page
2. Search for "Juan"
3. Observe filtered results
4. Search for "Garcia"
5. Clear search
**Expected Result**: Search filters by first/last name. Clearing restores full list.
**Type**: Happy Path

---

### PAT-03: Create patient - all fields
**Role**: ADMINISTRATIVE_STAFF
**Precondition**: Logged in as staff1
**Steps**:
1. Click "New Patient"
2. Fill all fields:
   - First name: "Test"
   - Last name: "Patient"
   - Age: 30
   - Sex: MALE
   - Gender: "Male"
   - Marital status: SINGLE
   - Religion: "Catholic"
   - Education level: UNIVERSITY
   - Occupation: "Engineer"
   - Address: "Zone 10, Guatemala City"
   - Email: "test.patient@email.com"
   - ID document number: "DPI-9999999999999"
   - Notes: "Test patient for manual testing"
3. Submit
**Expected Result**: Patient created. Success notification. Patient appears in list.
**Type**: Happy Path

---

### PAT-04: Create patient - required fields only
**Role**: ADMINISTRATIVE_STAFF
**Precondition**: Logged in as staff1
**Steps**:
1. Click "New Patient"
2. Fill only required fields (first name, last name, age, sex)
3. Submit
**Expected Result**: Patient created successfully with minimal data. Optional fields are null/empty.
**Type**: Happy Path

---

### PAT-05: Create patient - validation errors
**Role**: ADMINISTRATIVE_STAFF
**Precondition**: Logged in as staff1
**Steps**:
1. Click "New Patient"
2. Submit with empty form (test required field validation)
3. Enter invalid email format
4. Enter negative age
5. Enter duplicate ID document number (use one from seed data: "DPI-1234567890101")
**Expected Result**: Validation errors for each case. Duplicate ID number shows server error.
**Type**: Negative

---

### PAT-06: View patient details
**Role**: DOCTOR
**Precondition**: Logged in as doctor1, patients exist
**Steps**:
1. Navigate to Patients page
2. Click on patient "Juan Perez Gonzalez"
3. Review all fields
**Expected Result**: All patient details displayed correctly: demographics, emergency contacts, documents. All data matches seed data.
**Type**: Happy Path

---

### PAT-07: Update patient
**Role**: ADMINISTRATIVE_STAFF
**Precondition**: Logged in as staff1
**Steps**:
1. Open patient detail for "Juan Perez Gonzalez"
2. Click Edit
3. Change address to "Zone 14, Guatemala City"
4. Change occupation to "Senior Engineer"
5. Save
6. Refresh and verify
**Expected Result**: Changes saved. Success notification. Updated values shown on refresh.
**Type**: Happy Path

---

### PAT-08: Soft-delete patient (admin only)
**Role**: ADMIN
**Precondition**: Logged in as admin, a test patient exists with no active admissions
**Steps**:
1. Navigate to patient detail
2. Click Delete
3. Confirm
4. Check patient list
**Expected Result**: Patient removed from list (soft deleted). No errors on related records.
**Type**: Happy Path

---

### PAT-09: Delete patient - non-admin denied
**Role**: ADMINISTRATIVE_STAFF
**Precondition**: Logged in as staff1
**Steps**:
1. Navigate to patient detail
2. Look for delete button
3. If visible, try to click it
4. Try API call: `DELETE /api/v1/patients/{id}`
**Expected Result**: Delete button hidden or disabled. API returns 403 Forbidden.
**Type**: Permission

---

### PAT-10: Add emergency contact
**Role**: ADMINISTRATIVE_STAFF
**Precondition**: Logged in as staff1, a patient exists
**Steps**:
1. Open patient detail
2. Navigate to Emergency Contacts section
3. Click "Add Contact"
4. Fill: name "Maria Test", relationship "Sister", phone "5555-9999"
5. Save
**Expected Result**: Emergency contact added. Appears in the contacts list.
**Type**: Happy Path

---

### PAT-11: Edit emergency contact
**Role**: ADMINISTRATIVE_STAFF
**Precondition**: Logged in as staff1, patient has emergency contacts
**Steps**:
1. Open patient with existing emergency contact
2. Click edit on the contact
3. Change phone number
4. Save
**Expected Result**: Contact updated. New phone number shown.
**Type**: Happy Path

---

### PAT-12: Delete emergency contact
**Role**: ADMINISTRATIVE_STAFF
**Precondition**: Logged in as staff1, patient has emergency contacts
**Steps**:
1. Open patient with emergency contacts
2. Click delete on a contact
3. Confirm
**Expected Result**: Contact removed from list.
**Type**: Happy Path

---

### PAT-13: Upload ID document
**Role**: ADMINISTRATIVE_STAFF
**Precondition**: Logged in as staff1, a patient exists
**Steps**:
1. Open patient detail
2. Navigate to Documents section
3. Click "Upload ID Document"
4. Select a valid file (PDF, JPG, PNG - under 25MB)
5. Upload
**Expected Result**: File uploaded successfully. Appears in document list with filename and upload date. File stored at `{base-path}/patients/{id}/id-documents/{uuid}_{filename}`.
**Type**: Happy Path

---

### PAT-14: Upload ID document - invalid file
**Role**: ADMINISTRATIVE_STAFF
**Precondition**: Logged in as staff1
**Steps**:
1. Try to upload a file exceeding 25MB
2. Try to upload an unsupported file type (if restricted)
**Expected Result**: Appropriate error message for each case. File not stored.
**Type**: Negative

---

### PAT-15: View/download ID document
**Role**: DOCTOR
**Precondition**: Logged in as doctor1, patient has uploaded documents
**Steps**:
1. Open patient with documents
2. Click on a document to view/download
**Expected Result**: Document opens or downloads correctly.
**Type**: Happy Path

---

### PAT-16: Delete ID document
**Role**: ADMIN
**Precondition**: Logged in as admin, patient has documents
**Steps**:
1. Open patient with documents
2. Click delete on a document
3. Confirm
**Expected Result**: Document removed from list. File removed from storage.
**Type**: Happy Path

---

### PAT-17: Permission - NURSE can read but not create/update
**Role**: NURSE
**Precondition**: Logged in as nurse1
**Steps**:
1. Navigate to Patients page - should see list (patient:list, patient:read)
2. Try to find "New Patient" button - should be hidden
3. Open a patient detail - should see data
4. Try to find Edit button - should be hidden
5. Try API: `POST /api/v1/patients` - should get 403
**Expected Result**: Can view patients. Cannot create or edit. API returns 403 for write operations.
**Type**: Permission

---

### PAT-18: Permission - PSYCHOLOGIST can read and update
**Role**: PSYCHOLOGIST
**Precondition**: Logged in as psych1
**Steps**:
1. Navigate to Patients page - should see list
2. Open patient detail - should see data
3. Edit patient - should be allowed (patient:update)
4. Try to create new patient - should be denied
**Expected Result**: Can view and update patients. Cannot create new patients.
**Type**: Permission

---

### PAT-19: Permission - USER role has no access
**Role**: USER
**Precondition**: Logged in as user1
**Steps**:
1. Try to navigate to Patients page
2. Try API: `GET /api/v1/patients`
**Expected Result**: Page not accessible. API returns 403. Menu item hidden.
**Type**: Permission

---

### PAT-20: Empty state for new system
**Role**: ADMIN
**Precondition**: No patients in system (or filtered to show none)
**Steps**:
1. Navigate to Patients page with a search that returns no results
**Expected Result**: Empty state message displayed (e.g., "No patients found").
**Type**: Happy Path

---

## Permission Matrix

| Action | ADMIN | STAFF | DOCTOR | PSYCH | NURSE | CHIEF_NURSE | USER |
|--------|-------|-------|--------|-------|-------|-------------|------|
| List patients | G | G | G | G | G | G | D |
| View patient | G | G | G | G | G | G | D |
| Create patient | G | G | D | D | D | D | D |
| Update patient | G | G | G | G | D | G | D |
| Delete patient | G | D | D | D | D | D | D |

G = Granted, D = Denied (403)

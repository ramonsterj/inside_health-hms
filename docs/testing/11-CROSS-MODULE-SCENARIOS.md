# Cross-Module End-to-End Scenarios

**Module ID**: E2E
**Purpose**: Verify complete workflows that span multiple modules

---

## Scenario Summary

| Scenario | Modules Covered | Roles Involved |
|----------|----------------|----------------|
| E2E-01 | All | STAFF, DOCTOR, NURSE, PSYCH, ADMIN |
| E2E-02 | Medical Orders, MAR, Inventory, Billing | ADMIN, DOCTOR, NURSE |
| E2E-03 | Psychotherapy, Billing | ADMIN, PSYCH |
| E2E-04 | Admissions, Billing | STAFF, ADMIN |
| E2E-05 | All | All 7 roles |
| E2E-06 | Admissions, Progress Notes, Nursing, MAR | STAFF, DOCTOR, NURSE |
| E2E-07 | Admissions, Billing (Scheduler) | STAFF, ADMIN |
| E2E-08 | All clinical, Billing, Invoicing | STAFF, DOCTOR, NURSE, PSYCH, ADMIN |
| E2E-09 | All (i18n) | ADMIN |
| E2E-10 | Auth, Clinical History | DOCTOR |
| E2E-11 | Audit, Patient, Admission, Medical Orders | STAFF, DOCTOR, ADMIN |

---

### E2E-01: Complete Patient Journey (Admission to Discharge)
**Roles Involved**: ADMINISTRATIVE_STAFF, DOCTOR, NURSE, PSYCHOLOGIST, ADMIN
**Modules Tested**: Patient, Admission, Clinical History, Progress Notes, Medical Orders, Nursing Notes, Vital Signs, MAR, Psychotherapy, Billing, Invoice
**Preconditions**: System running with seed data. All test users available.

**Steps**:
1. **[staff1]** Create a new patient: "Manual Test Patient", age 35, MALE, MARRIED
2. **[staff1]** Admit the patient: type HOSPITALIZATION, select a room with beds, triage code A, treating physician doctor1, upload consent document
3. **Verify**: Admission created (ACTIVE). Room available beds decreased by 1. Patient has admission in list.
4. **[doctor1]** Open the admission. Create Clinical History filling at least 5 key fields (reason, current condition, psychiatric history, diagnosis, management plan)
5. **[doctor1]** Create 2 Progress Notes with SOAP format
6. **[doctor1]** Create Medical Orders:
   - MEDICAMENTOS order with inventory item linked (for a medication with stock)
   - DIETA order
   - LABORATORIOS order
7. **[nurse1]** Record Vital Signs: systolic 120, diastolic 80, HR 72, temp 36.5, O2 98%
8. **[nurse1]** Create a Nursing Note with patient observations
9. **[nurse1]** Administer medication (GIVEN status) from the medication order created in step 6
10. **Verify**: Inventory stock decreased. MEDICATION billing charge created.
11. **[nurse1]** Record medication as MISSED for the same order
12. **Verify**: NO inventory or billing impact from MISSED.
13. **[psych1]** Create psychotherapy activity with a priced category
14. **Verify**: SERVICE billing charge created.
15. **[admin]** View billing charges, verify all auto-charges present
16. **[admin]** Create a manual adjustment (-Q50, reason: "Courtesy discount")
17. **[admin]** View daily balance, verify running total
18. **[doctor1]** Discharge the patient
19. **Verify**: Admission status = DISCHARGED. Room bed freed. Final-day room and diet charges added. Invoice auto-generated.
20. **[admin]** Open invoice. Verify charge summary by type. Verify total matches balance.

**Expected Results**:
- Complete lifecycle works without errors
- Each role can perform their specific actions
- All billing automation fires correctly
- Invoice reflects all charges and adjustments
- Room capacity restored after discharge

**Type**: End-to-End

---

### E2E-02: Medication Full Flow (Order to Billing)
**Roles Involved**: ADMIN, DOCTOR, NURSE
**Modules Tested**: Inventory, Medical Orders, MAR, Billing
**Preconditions**: Active admission exists. Inventory item "Test Medication" with stock 50 and price Q25.

**Steps**:
1. **[admin]** Note current stock of "Test Medication": 50 units
2. **[doctor1]** Create MEDICAMENTOS medical order linked to "Test Medication"
3. **[nurse1]** Administer medication - status GIVEN
4. **[admin]** Check inventory: stock should be 49
5. **[admin]** Check billing: MEDICATION charge of Q25 should exist
6. **[nurse1]** Administer same medication - status REFUSED
7. **[admin]** Check inventory: stock should still be 49 (no change)
8. **[admin]** Check billing: no new charge from REFUSED
9. **[nurse1]** Administer same medication - status GIVEN again
10. **[admin]** Check inventory: stock should be 48
11. **[admin]** Check billing: second MEDICATION charge of Q25

**Expected Results**:
- GIVEN: stock -1, charge +Q25 each time
- REFUSED/MISSED/HELD: no inventory or billing impact
- Multiple administrations tracked independently

**Type**: Integration

---

### E2E-03: Psychotherapy Billing Integration
**Roles Involved**: ADMIN, PSYCHOLOGIST
**Modules Tested**: Psychotherapy Categories, Activities, Billing
**Preconditions**: Active admission exists.

**Steps**:
1. **[admin]** Create psychotherapy category "CBT Individual" with price Q500
2. **[admin]** Create psychotherapy category "Group Support" with NO price
3. **[psych1]** Create activity for the admission using "CBT Individual" (Q500)
4. **[admin]** Check billing: SERVICE charge of Q500 should exist
5. **[psych1]** Create activity using "Group Support" (no price)
6. **[admin]** Check billing: NO new charge from unpriced activity
7. **[admin]** Update "CBT Individual" price to Q600
8. **[psych1]** Create another activity with "CBT Individual"
9. **[admin]** Check billing: new SERVICE charge should be Q600 (updated price)

**Expected Results**:
- Priced activities generate SERVICE charges at current category price
- Unpriced activities generate no charges
- Price changes affect future activities only

**Type**: Integration

---

### E2E-04: Procedure Admission Auto-Billing
**Roles Involved**: ADMINISTRATIVE_STAFF, ADMIN
**Modules Tested**: Admissions, Billing
**Preconditions**: Patients available for admission.

**Steps**:
1. **[staff1]** Create admission type ELECTROSHOCK_THERAPY for patient A
2. **[admin]** Check billing for this admission: PROCEDURE charge should exist
3. **[staff1]** Create admission type KETAMINE_INFUSION for patient B
4. **[admin]** Check billing: PROCEDURE charge should exist
5. **[staff1]** Create admission type HOSPITALIZATION for patient C
6. **[admin]** Check billing: NO PROCEDURE charge (only HOSPITALIZATION, no auto procedure)

**Expected Results**:
- ELECTROSHOCK_THERAPY and KETAMINE_INFUSION auto-generate PROCEDURE charges
- HOSPITALIZATION does not auto-generate PROCEDURE charges
- Charge amounts match configured base prices

**Type**: Integration

---

### E2E-05: Role-Based Access Control Full Matrix
**Roles Involved**: All 7 roles
**Modules Tested**: All modules
**Preconditions**: All test users available. Active admission with clinical data.

**Steps**:
For each role, log in and verify access:

1. **[admin]** Full navigation visible. Can access ALL modules. Can CRUD everything.

2. **[staff1]** (ADMINISTRATIVE_STAFF)
   - CAN: create/edit patients, create/edit admissions, upload/view/download documents, view document types
   - CANNOT: clinical history, progress notes, medical orders, nursing, psychotherapy, billing, user management
   - Verify: menu items hidden for unauthorized modules

3. **[doctor1]** (DOCTOR)
   - CAN: view patients, clinical history CRUD, progress notes create/read, medical orders CRUD + discontinue, discharge, nursing notes CRUD, vital signs CRUD, view documents, psychotherapy read, billing read, MAR read
   - CANNOT: create patients, create admissions, upload documents, MAR create, psychotherapy create, billing create/adjust, user management

4. **[psych1]** (PSYCHOLOGIST)
   - CAN: view patients, psychotherapy activities create/read, psychotherapy categories read, view/download documents
   - CANNOT: create patients, create admissions, clinical history, progress notes, medical orders, nursing, billing, MAR

5. **[nurse1]** (NURSE)
   - CAN: view patients/admissions, nursing notes CRUD, vital signs CRUD, MAR create/read, clinical history read, progress notes create/read, medical orders read, psychotherapy read, billing read, view documents
   - CANNOT: create patients, create admissions, clinical history create, medical orders create, psychotherapy create, billing create/adjust

6. **[chiefnurse1]** (CHIEF_NURSE)
   - CAN: same as NURSE plus patient update, admission update, progress notes update, billing read
   - CANNOT: create patients, create admissions, clinical history create, medical orders create, psychotherapy create, billing create/adjust

7. **[user1]** (USER)
   - CAN: view own profile (user:read)
   - CANNOT: access any clinical, patient, admission, billing, or admin module

**Expected Results**:
- Each role sees exactly the correct menu items
- Unauthorized API calls return 403
- Unauthorized page visits redirect or show error

**Type**: End-to-End

---

### E2E-06: Multi-User Concurrent Workflow
**Roles Involved**: ADMINISTRATIVE_STAFF, DOCTOR, NURSE
**Modules Tested**: Admissions, Progress Notes, Nursing Notes, Medical Orders, MAR
**Preconditions**: Active admission with medication orders. Two browser sessions available.

**Steps**:
1. **[doctor1]** in Browser A: open admission, start writing progress note
2. **[nurse1]** in Browser B: open same admission, start writing nursing note
3. **[doctor1]** in Browser A: save progress note
4. **[nurse1]** in Browser B: save nursing note
5. **Verify**: Both notes saved with correct attribution
6. **[doctor1]** in Browser A: discontinue a medication order
7. **[nurse1]** in Browser B (stale view): try to administer that medication
8. **Verify**: Server rejects administration of discontinued order

**Expected Results**:
- Concurrent writes to different record types succeed
- Server-side validation catches stale state (discontinued order)
- No data corruption from concurrent access

**Type**: End-to-End

---

### E2E-07: Daily Charge Scheduler Verification
**Roles Involved**: ADMINISTRATIVE_STAFF, ADMIN
**Modules Tested**: Admissions, Billing (Scheduler)
**Preconditions**: Room with price configured (e.g., Q200/day). Daily meal rate configured.

**Steps**:
1. **[staff1]** Create HOSPITALIZATION admission in the priced room
2. Wait for scheduler to run (1:00 AM) or trigger manually
3. **[admin]** Check billing: ROOM charge for Q200 should exist
4. **[admin]** Check billing: DIET charge should exist
5. Trigger scheduler again for the same day
6. **[admin]** Verify NO duplicate charges (idempotency)

**Expected Results**:
- Room and diet charges created daily for active hospitalizations
- Idempotent: no duplicates on re-run
- Correct amounts from room price and meal rate config

**Type**: Integration

---

### E2E-08: Discharge with Full Billing Reconciliation
**Roles Involved**: All clinical roles + ADMIN
**Modules Tested**: All clinical modules, Billing, Invoice
**Preconditions**: Active admission with accumulated charges from multiple sources:
- 2+ medication charges (from MAR GIVEN)
- 1+ room charges (from scheduler)
- 1+ diet charges (from scheduler)
- 1+ psychotherapy service charges
- 1 manual charge from admin
- 1 adjustment

**Steps**:
1. **[admin]** Review charge list: verify all expected charge types present
2. **[admin]** View daily balance: verify grouping and running totals
3. **[admin]** Calculate expected total manually
4. **[doctor1]** Discharge the patient
5. **[admin]** Verify: final-day room charge added
6. **[admin]** Verify: final-day diet charge added
7. **[admin]** Verify: invoice auto-generated
8. **[admin]** Open invoice: verify charge summary by type
9. **[admin]** Verify: invoice total matches balance
10. **[admin]** Verify: invoice number format INV-YYYY-NNNN
11. **[admin]** Try to generate another invoice: should fail (one per admission)

**Expected Results**:
- Complete billing reconciliation at discharge
- Final-day charges added automatically
- Invoice totals match charge sum minus adjustments
- One invoice per admission enforced

**Type**: End-to-End

---

### E2E-09: i18n Full Application Test
**Roles Involved**: ADMIN
**Modules Tested**: All (UI labels and messages)
**Preconditions**: Admin can access all modules.

**Steps**:
1. **[admin]** Log in. Default language English.
2. Switch to Spanish (language switcher in header)
3. Navigate through each module:
   - Dashboard, User Management, Patient Management, Admissions
   - Clinical History, Progress Notes, Medical Orders
   - Nursing Notes, Vital Signs, Medication Administration
   - Psychotherapy, Inventory, Billing, Audit Logs
4. Spot-check the following specific translations (verify each appears correctly in Spanish):

   | # | Location | English | Expected Spanish |
   |---|----------|---------|-----------------|
   | 1 | Login page title | Welcome Back | Bienvenido de Nuevo |
   | 2 | Login username field | Email or Username | Correo o Usuario |
   | 3 | Login password field | Password | Contraseña |
   | 4 | Login button | Sign In | Iniciar Sesión |
   | 5 | Sidebar nav | Patients | Pacientes |
   | 6 | Sidebar nav | Admissions | Hospitalizaciones |
   | 7 | Sidebar nav | Users | Usuarios |
   | 8 | Sidebar nav | Dashboard | Panel |
   | 9 | Patient form label | First Name | Nombres |
   | 10 | Patient form label | Last Name | Apellidos |
   | 11 | Patient form label | Marital Status | Estado Civil |
   | 12 | Patient success toast | Patient created successfully. | Paciente creado exitosamente. |
   | 13 | Admission page title | Admissions | Hospitalizaciones |
   | 14 | Admission button | New Admission | Nueva Hospitalización |
   | 15 | Admission form label | Triage Code | Código de Triage |
   | 16 | Admission form label | Room | Habitación |
   | 17 | User mgmt page title | User Management | Gestión de Usuarios |
   | 18 | User mgmt button | Add User | Agregar Usuario |
   | 19 | Validation error | {field} is required | {field} es requerido |
   | 20 | Validation error | Invalid email format | Formato de correo electrónico invalido |

5. Trigger a validation error: verify error in Spanish
6. Trigger a success notification: verify in Spanish
7. Switch back to English
8. Verify all text reverts to English
9. Log out and log back in: verify language preference persisted

**Expected Results**:
- Complete Spanish translation coverage
- No untranslated keys visible
- Language preference persists across sessions

**Type**: End-to-End

---

### E2E-10: Session Expiration During Work
**Roles Involved**: DOCTOR
**Modules Tested**: Auth, Clinical History
**Preconditions**: doctor1 can log in. Active admission without clinical history. Token TTL known or configurable.

**Steps**:
1. **[doctor1]** Log in
2. Open admission, start filling clinical history form (enter substantial text in multiple fields)
3. Wait for token to expire (or manipulate TTL for testing)
4. Observe: session expiration modal should appear
5. Verify: current page NOT navigated away
6. Click re-login in modal
7. Enter credentials, submit
8. Verify: redirected back to clinical history form page
9. Check if previously entered form data is preserved
10. Complete and save the clinical history
11. Verify: saved successfully with new token

**Expected Results**:
- Proactive session expiration notification
- Re-login redirects back to original page
- Session fully restored after re-authentication
- Document data preservation behavior (positive if preserved, noted if lost)

**Type**: End-to-End

---

### E2E-11: Audit Trail Verification
**Roles Involved**: ADMINISTRATIVE_STAFF, DOCTOR, ADMIN
**Modules Tested**: Audit Logs, Patient, Admission, Medical Orders
**Preconditions**: Audit logging enabled.

**Steps**:
1. **[staff1]** Create a new patient. Note time.
2. **[staff1]** Edit the patient (change address). Note time.
3. **[staff1]** Create admission for the patient. Note time.
4. **[doctor1]** Create a medical order for the admission. Note time.
5. **[doctor1]** Edit the medical order. Note time.
6. **[admin]** Open Audit Logs page
7. Filter by entity "Patient": verify CREATE and UPDATE entries by staff1
8. Filter by user "doctor1": verify medical order entries
9. Filter by date range (today): verify all today's entries
10. Open the Patient UPDATE entry: verify before/after values show the address change
11. Clear filters: verify full log restored

**Expected Results**:
- Every CRUD operation generates audit entry
- Entries contain: entity type, ID, action, user, timestamp, before/after values
- Filters work correctly (entity, user, date)
- Before/after values accurately reflect data changes

**Type**: End-to-End

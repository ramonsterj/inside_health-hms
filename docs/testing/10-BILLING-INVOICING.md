# Module: Billing & Invoicing

**Module ID**: BIL (Charges), BAL (Balance), ADJ (Adjustments), INV (Invoices), AUTO (Automation)
**Required Permissions**: billing:read, billing:create, billing:adjust, billing:configure, invoice:read, invoice:create
**API Base**: `/api/v1/admissions/{admissionId}/charges`, `/api/v1/admissions/{admissionId}/balance`, `/api/v1/admissions/{admissionId}/adjustments`, `/api/v1/admissions/{admissionId}/invoice`

---

## Overview

Hospital billing system with real-time charge capture and automated billing from clinical events:
- **Charge Types**: MEDICATION, ROOM, PROCEDURE, LAB, SERVICE, DIET, ADJUSTMENT
- **Daily Balance**: Grouped by date with running totals
- **Adjustments**: Negative amounts with required reason
- **Invoices**: Auto-generated at discharge, one per admission
- **Automation**: 6 phases of event-driven charge generation
- **Immutability**: Charges cannot be edited once created

**Write operations are ADMIN-only**. DOCTOR, NURSE, and CHIEF_NURSE have read-only billing access (`billing:read`).

---

## Charge Test Cases

### BIL-01: List charges for admission
**Role**: ADMIN
**Precondition**: Logged in as admin, admission has charges (from seed data)
**Steps**:
1. Open an admission with billing data
2. Navigate to Billing/Charges tab
3. Review charge list
**Expected Result**: All charges listed with: description, charge type, amount, date, auto/manual flag. Sorted by date.
**Type**: Happy Path

---

### BIL-02: Create manual charge - MEDICATION
**Role**: ADMIN
**Precondition**: Logged in as admin, active admission
**Steps**:
1. Click "Create Charge"
2. Select type: MEDICATION, enter description and amount (Q100)
3. Submit
**Expected Result**: Charge created. Appears in charge list.
**Type**: Happy Path

---

### BIL-03: Create manual charge - each type
**Role**: ADMIN
**Precondition**: Logged in as admin
**Steps**:
1. Create charges of each type: MEDICATION (Q50), ROOM (Q200), PROCEDURE (Q500), LAB (Q150), SERVICE (Q300), DIET (Q75)
2. Verify each appears in list
**Expected Result**: All charge types can be created manually.
**Type**: Happy Path

---

### BIL-04: Charge immutability
**Role**: ADMIN
**Precondition**: Logged in as admin, charges exist
**Steps**:
1. Open a charge in the list
2. Look for Edit/Delete buttons
3. Try API: `PUT /api/v1/admissions/{id}/charges/{chargeId}`
**Expected Result**: No edit or delete capability. Charges are immutable. To correct, use adjustments.
**Type**: Negative

---

### BIL-05: Non-admin billing read access
**Role**: DOCTOR, NURSE, CHIEF_NURSE
**Precondition**: Logged in as non-admin with billing:read
**Steps**:
1. Log in as doctor1
2. Navigate to billing for an admission
3. Try API: `GET /api/v1/admissions/{id}/charges`
4. Try API: `GET /api/v1/admissions/{id}/balance`
5. Try API: `POST /api/v1/admissions/{id}/charges`
6. Repeat for nurse1, chief_nurse1
**Expected Result**: GET requests return 200 (read access granted). POST returns 403 (write operations denied).
**Type**: Permission

---

### BIL-06: Staff and other roles billing access denied
**Role**: STAFF, PSYCHOLOGIST, USER
**Precondition**: Logged in as role without billing:read
**Steps**:
1. Log in as staff1
2. Try API: `GET /api/v1/admissions/{id}/charges`
3. Try API: `POST /api/v1/admissions/{id}/charges`
4. Repeat for psych1, user1
**Expected Result**: 403 Forbidden for all billing operations (read and write).
**Type**: Permission

---

## Balance Test Cases

### BAL-01: View daily balance
**Role**: ADMIN
**Precondition**: Logged in as admin, admission has charges over multiple days
**Steps**:
1. Open admission billing
2. Navigate to Balance view
3. Review daily breakdown
**Expected Result**: Charges grouped by date. Each day shows subtotal. Running cumulative total displayed.
**Type**: Happy Path

---

### BAL-02: Balance with adjustments
**Role**: ADMIN
**Precondition**: Admission has charges and at least one adjustment
**Steps**:
1. View daily balance
2. Verify adjustment (negative amount) is included
3. Verify total is reduced by adjustment amount
**Expected Result**: Adjustments shown in daily breakdown. Total = sum of charges - adjustments.
**Type**: Happy Path

---

### BAL-03: Balance - empty state
**Role**: ADMIN
**Precondition**: Admission with no charges
**Steps**:
1. Open billing for a newly created admission (no charges yet)
2. View balance
**Expected Result**: Empty state or Q0.00 balance displayed. No errors.
**Type**: Happy Path

---

### BAL-04: Balance calculation accuracy
**Role**: ADMIN
**Precondition**: Admission with known charges
**Steps**:
1. Manually calculate expected total from charge list
2. Compare with displayed balance
**Expected Result**: Displayed balance matches manual calculation exactly.
**Type**: Happy Path

---

## Adjustment Test Cases

### ADJ-01: Create adjustment with reason
**Role**: ADMIN
**Precondition**: Logged in as admin, active admission
**Steps**:
1. Click "Create Adjustment"
2. Enter amount: -100.00 (or positive with adjustment type)
3. Enter reason: "Insurance discount applied"
4. Submit
**Expected Result**: Adjustment created as ADJUSTMENT charge type with negative amount. Reason documented.
**Type**: Happy Path

---

### ADJ-02: Adjustment without reason (denied)
**Role**: ADMIN
**Precondition**: Logged in as admin
**Steps**:
1. Try to create adjustment without entering a reason
2. Submit
**Expected Result**: Validation error: reason is required.
**Type**: Negative

---

### ADJ-03: Adjustment appears in balance
**Role**: ADMIN
**Precondition**: Adjustment created (ADJ-01)
**Steps**:
1. View daily balance
2. Find the adjustment entry
3. Verify total is reduced
**Expected Result**: Adjustment visible in charge list and balance. Running total correctly reduced.
**Type**: Happy Path

---

## Invoice Test Cases

### INV-01: Generate invoice at discharge
**Role**: ADMIN
**Precondition**: Patient has been discharged, charges exist
**Steps**:
1. Open the discharged admission
2. Navigate to Invoice section
3. Verify invoice was auto-generated at discharge
**Expected Result**: Invoice exists with number format INV-YEAR-SEQUENCE. Contains charge summary by type. Grand total correct.
**Type**: Happy Path

---

### INV-02: View invoice details
**Role**: ADMIN
**Precondition**: Invoice exists for an admission
**Steps**:
1. Open the invoice
2. Review: invoice number, date, patient info, charge summary by type, total
**Expected Result**: All details correct. Charge breakdown by type (MEDICATION total, ROOM total, etc.).
**Type**: Happy Path

---

### INV-03: Invoice number format
**Role**: ADMIN
**Precondition**: Invoice exists
**Steps**:
1. Check invoice number
**Expected Result**: Format is INV-YYYY-NNNN (e.g., INV-2026-0001).
**Type**: Happy Path

---

### INV-04: Duplicate invoice prevented
**Role**: ADMIN
**Precondition**: Admission already has an invoice
**Steps**:
1. Try API: `POST /api/v1/admissions/{id}/invoice`
**Expected Result**: Error: invoice already exists for this admission. Only one per admission allowed.
**Type**: Negative

---

### INV-05: Invoice for non-discharged admission
**Role**: ADMIN
**Precondition**: Active admission (not discharged)
**Steps**:
1. Try to generate invoice for an active admission
2. Try API: `POST /api/v1/admissions/{id}/invoice`
**Expected Result**: Error: can only generate invoice for discharged admissions.
**Type**: Negative

---

### INV-06: Invoice includes final-day charges
**Role**: ADMIN
**Precondition**: Just discharged a patient
**Steps**:
1. Check charges before discharge (note last room/diet charge date)
2. Discharge the patient
3. Check charges after discharge
4. View invoice
**Expected Result**: Final-day room charge and diet charge added at discharge. Invoice includes these final charges.
**Type**: Happy Path

---

## Automation Verification Test Cases

### AUTO-01: MAR GIVEN creates MEDICATION charge
**Role**: NURSE + ADMIN
**Precondition**: Medication order with inventory item linked
**Steps**:
1. As nurse1, administer medication with GIVEN status
2. As admin, check billing charges for the admission
**Expected Result**: MEDICATION charge auto-created with amount matching inventory item price.
**Type**: Automation

---

### AUTO-02: MAR non-GIVEN does NOT create charge
**Role**: NURSE + ADMIN
**Precondition**: Same medication order
**Steps**:
1. As nurse1, record medication with MISSED status
2. As admin, check billing charges
**Expected Result**: NO new MEDICATION charge for the MISSED administration.
**Type**: Automation

---

### AUTO-03: Psychotherapy activity creates SERVICE charge
**Role**: PSYCHOLOGIST + ADMIN
**Precondition**: Priced psychotherapy category exists (Q500)
**Steps**:
1. As psych1, create activity with priced category
2. As admin, check billing charges
**Expected Result**: SERVICE charge of Q500 auto-created.
**Type**: Automation

---

### AUTO-04: Unpriced psychotherapy - no charge
**Role**: PSYCHOLOGIST + ADMIN
**Precondition**: Unpriced category exists
**Steps**:
1. As psych1, create activity with unpriced category
2. As admin, check billing charges
**Expected Result**: NO new SERVICE charge.
**Type**: Automation

---

### AUTO-05: Medical order with inventory creates charge
**Role**: DOCTOR + ADMIN
**Precondition**: Inventory item exists
**Steps**:
1. As doctor1, create LABORATORIOS order with inventory item
2. As admin, check billing
**Expected Result**: LAB charge auto-created.
**Type**: Automation

---

### AUTO-06: Procedure admission creates PROCEDURE charge
**Role**: STAFF + ADMIN
**Precondition**: None
**Steps**:
1. As staff1, create ELECTROSHOCK_THERAPY admission
2. As admin, check billing for that admission
**Expected Result**: PROCEDURE charge auto-created with configured base price.
**Type**: Automation

---

### AUTO-07: Daily room charge (scheduler)
**Role**: ADMIN
**Precondition**: Active HOSPITALIZATION admission in a priced room, scheduler has run
**Steps**:
1. Check billing for an admission that has been active for at least 1 day
2. Look for ROOM charges
**Expected Result**: Daily ROOM charge exists for each day of stay, matching room price.
**Type**: Automation

---

### AUTO-08: Daily diet charge (scheduler)
**Role**: ADMIN
**Precondition**: Active HOSPITALIZATION admission, scheduler has run
**Steps**:
1. Check billing for the admission
2. Look for DIET charges
**Expected Result**: Daily DIET charge exists matching configured daily meal rate.
**Type**: Automation

---

### AUTO-09: Scheduler idempotency
**Role**: ADMIN
**Precondition**: Scheduler has already run today
**Steps**:
1. Note current charges count and amounts
2. Trigger scheduler again (or wait for next run)
3. Check charges again
**Expected Result**: NO duplicate charges created. Same count and amounts.
**Type**: Automation

---

### AUTO-10: Discharge triggers final charges and invoice
**Role**: DOCTOR + ADMIN
**Precondition**: Active admission with existing charges
**Steps**:
1. As doctor1, discharge the patient
2. As admin, check billing
3. Verify final-day room charge added
4. Verify final-day diet charge added
5. Verify invoice auto-generated
**Expected Result**: Final charges added. Invoice generated with all charges.
**Type**: Automation

---

## Permission Matrix

| Action | ADMIN | STAFF | DOCTOR | PSYCH | NURSE | CHIEF_NURSE | USER |
|--------|-------|-------|--------|-------|-------|-------------|------|
| Read charges | G | D | G | D | G | G | D |
| Create charge | G | D | D | D | D | D | D |
| Create adjustment | G | D | D | D | D | D | D |
| Read balance | G | D | G | D | G | G | D |
| Read invoice | G | D | D | D | D | D | D |
| Generate invoice | G | D | D | D | D | D | D |
| Configure billing | G | D | D | D | D | D | D |

G = Granted, D = Denied

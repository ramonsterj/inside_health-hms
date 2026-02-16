# HMS Manual Testing Plan - Overview

**Version**: 1.0
**Date**: February 2026
**System**: Inside Health HMS (Hospital Management System)

---

## Purpose

This manual testing plan provides a structured guide for a human tester to verify all functionality of the HMS application. The plan covers every module, every user role, and both happy paths and negative/error paths.

---

## Document Structure

| Document | Module | Description |
|----------|--------|-------------|
| `00-TESTING-OVERVIEW.md` | General | This file. Prerequisites, test accounts, execution order |
| `01-CHECKLIST-TEMPLATE.md` | General | Reusable checklist to fill per module per test round |
| `02-AUTH-SESSION.md` | Authentication | Login, logout, token refresh, session expiration, password reset |
| `03-USER-ROLE-MANAGEMENT.md` | Admin | User CRUD, role/permission management, audit logs |
| `04-PATIENT-MANAGEMENT.md` | Patient | Patient registration, search, edit, documents |
| `05-ADMISSION-MANAGEMENT.md` | Admission | Admission wizard, room management, triage, discharge |
| `06-MEDICAL-RECORDS.md` | Clinical | Clinical history, progress notes, medical orders |
| `07-NURSING-MODULE.md` | Nursing | Nursing notes, vital signs, medication administration |
| `08-PSYCHOTHERAPY.md` | Psychotherapy | Categories, activity registration, pricing |
| `09-INVENTORY-MANAGEMENT.md` | Inventory | Categories, items, stock movements, low stock |
| `10-BILLING-INVOICING.md` | Billing | Charges, balance, adjustments, invoices, automation |
| `11-CROSS-MODULE-SCENARIOS.md` | Integration | End-to-end workflows spanning multiple modules |

---

## Prerequisites

### Environment Setup

1. **Backend running** on `http://localhost:8080` (Spring Boot)
2. **Frontend running** on `http://localhost:5173` (Vite dev server)
3. **PostgreSQL 17+** database running with seed data loaded
4. **Seed data applied**: All 7 seed files (`R__seed_01` through `R__seed_07`) must be loaded
5. **File storage directory** exists and is writable (default: `./data/files` in dev)

### Browser Requirements

- Test primarily in **Chrome** (latest)
- Secondary verification in **Firefox** (latest)
- Check responsive behavior at 1920px, 1366px, and 768px widths

---

## Test User Accounts

All test users share the password: **`admin123`**

| Username | Role | Full Name | Purpose |
|----------|------|-----------|---------|
| `admin` | ADMIN | System Administrator | Full access, admin operations |
| `staff1` | ADMINISTRATIVE_STAFF | Maria Garcia | Patient/admission management |
| `staff2` | ADMINISTRATIVE_STAFF | Jose Rodriguez | Secondary admin staff |
| `doctor1` | DOCTOR | Dr. Roberto Hernandez | Clinical operations |
| `doctor2` | DOCTOR | Dra. Patricia Morales | Secondary doctor |
| `psych1` | PSYCHOLOGIST | Dra. Sofia Ramirez | Psychotherapy activities |
| `psych2` | PSYCHOLOGIST | Dr. Miguel Torres | Secondary psychologist |
| `nurse1` | NURSE | Laura Sanchez | Nursing operations |
| `nurse2` | NURSE | Fernando Diaz | Secondary nurse |
| `chiefnurse1` | CHIEF_NURSE | Carmen Flores | Extended nursing permissions |
| `chiefnurse2` | CHIEF_NURSE | Ricardo Mendoza | Secondary chief nurse |
| `user1` | USER | Carlos Martinez | Minimal permissions |
| `user2` | USER | Ana Lopez | Minimal permissions |

---

## Role-Permission Summary

### ADMIN
- **ALL permissions** across all modules
- Can configure system settings, manage users/roles, adjust billing

### ADMINISTRATIVE_STAFF
- User: read
- Patient: create, read, update, list, upload-id, view-id
- Triage Code: read | Room: read
- Admission: create, read, update, list, upload-consent, view-consent
- Documents: view, upload, download | Document Type: read

### DOCTOR
- User: read
- Patient: read, update, list
- Triage Code: read | Room: read
- Admission: read, update, list, **discharge**, view-consent
- Documents: view, download
- Clinical History: create, read
- Progress Notes: create, read
- Medical Orders: create, read, discontinue
- Nursing Notes: create, read, update
- Vital Signs: create, read, update
- Psychotherapy Activities: read | Categories: read
- Billing: read
- Medication Administration: read

### PSYCHOLOGIST
- Patient: read, update, list
- Triage Code: read | Room: read
- Admission: read, list, view-consent
- Documents: view, download
- Psychotherapy Activities: create, read
- Psychotherapy Categories: read

### NURSE
- User: read
- Patient: read, list
- Triage Code: read | Room: read
- Admission: read, list, view-consent
- Documents: view, download
- Clinical History: read
- Progress Notes: create, read
- Medical Orders: read
- Nursing Notes: create, read, update
- Vital Signs: create, read, update
- Psychotherapy Activities: read | Categories: read
- Billing: read
- Medication Administration: create, read

### CHIEF_NURSE
- User: read
- Patient: read, update, list
- Triage Code: read | Room: read
- Admission: read, update, list, view-consent
- Documents: view, download
- Clinical History: read
- Progress Notes: create, read, update
- Medical Orders: read
- Nursing Notes: create, read, update
- Vital Signs: create, read, update
- Psychotherapy Activities: read | Categories: read
- Billing: read
- Medication Administration: create, read

### USER
- User: read (own profile only)
- No clinical or administrative access

---

## Recommended Testing Order

Test modules in this order due to data dependencies:

```
1. Authentication & Session (02)     ← No dependencies
2. User & Role Management (03)       ← Requires login
3. Patient Management (04)           ← Requires login + patient permissions
4. Admission Management (05)         ← Requires patients to exist
5. Medical Records (06)              ← Requires active admissions
6. Inventory Management (09)         ← No admission dependency, but needed for billing
7. Psychotherapy (08)                ← Requires active admissions + categories
8. Nursing Module (07)               ← Requires medical orders with inventory items
9. Billing & Invoicing (10)          ← Requires charges from all modules
10. Cross-Module Scenarios (11)      ← Full end-to-end flows
```

---

## Issue Reporting Format

When a test case fails, report the issue using this template:

```
### Issue: [Brief title]

- **Module**: [Module name from testing doc]
- **Test Case ID**: [e.g., AUTH-01, PAT-03]
- **Path**: [Happy path / Negative path / Permission test]
- **Role Tested**: [e.g., DOCTOR, NURSE]
- **Severity**: [Critical / High / Medium / Low]
- **Steps to Reproduce**:
  1. [Step 1]
  2. [Step 2]
  3. ...
- **Expected Result**: [What should happen]
- **Actual Result**: [What actually happened]
- **Screenshots**: [Attach if applicable]
- **Browser/Resolution**: [e.g., Chrome 120 / 1920x1080]
- **Notes**: [Any additional context]
```

### Severity Definitions

| Severity | Definition |
|----------|-----------|
| **Critical** | System crash, data loss, security vulnerability, blocks all testing |
| **High** | Major feature broken, no workaround, affects core workflow |
| **Medium** | Feature partially broken, workaround exists, cosmetic issues affecting usability |
| **Low** | Minor cosmetic issue, typo, slight UI inconsistency |

---

## Test Data Available (Seed Data)

### Patients (20 pre-loaded)
- Juan Perez Gonzalez, Maria Santos Lopez, Pedro Garcia Hernandez, etc.
- Mix of ages (19-70), genders, marital statuses, occupations
- 10 patients have emergency contacts pre-configured

### Admissions (pre-loaded)
- Multiple active admissions across different rooms and admission types
- Some discharged admissions for historical testing
- Various triage codes and treating physicians assigned

### Rooms (pre-loaded)
- Private and shared room types with different capacities
- Rooms with pricing configured for billing tests

### Inventory (pre-loaded)
- Multiple categories (medications, supplies, meals, services)
- Items with flat and time-based pricing
- Stock levels for movement testing

### Clinical Records (pre-loaded)
- Clinical histories for some admissions
- Progress notes with SOAP format entries
- Medical orders across multiple categories
- Medication administration records

### Billing Data (pre-loaded)
- Various charge types (medication, room, procedure, lab, service)
- Some invoices already generated
- Adjustments and credits for balance testing

---

## General Testing Guidelines

1. **Always verify notifications**: After any create/update/delete action, verify the toast notification appears with the correct message
2. **Check i18n**: Toggle between English and Spanish to verify translations on key screens
3. **Test pagination**: For list views with many records, verify pagination controls work
4. **Test search/filter**: Where search or filter controls exist, verify they work correctly
5. **Check loading states**: Verify loading spinners appear during API calls
6. **Test form validation**: Submit forms with empty required fields, invalid data, and boundary values
7. **Verify audit trail**: For sensitive operations, check that audit logs are created (admin can view)
8. **Check soft deletes**: Deleted records should disappear from lists but not cause errors in related records
9. **Mobile responsiveness**: Check that the sidebar collapses and layouts adapt on narrow screens
10. **Session persistence**: Verify that refreshing the page maintains the logged-in state

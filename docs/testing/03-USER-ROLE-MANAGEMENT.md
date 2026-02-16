# Module: User & Role Management

**Module ID**: USR / ROL / AUD
**Required Permissions**: user:*, role:*, audit:read (ADMIN only)
**API Base**: `/api/v1/admin/users`, `/api/v1/admin/roles`, `/api/v1/admin/permissions`, `/api/v1/admin/audit-logs`

---

## Overview

Admin-only module for managing system users, roles, permissions, and viewing audit logs. All operations are soft-delete. Users have statuses (ACTIVE, INACTIVE, SUSPENDED, DELETED) and can be assigned multiple roles.

---

## User Management Test Cases

### USR-01: List users with pagination
**Role**: ADMIN
**Precondition**: Logged in as admin, seed data loaded (15 users)
**Steps**:
1. Navigate to User Management page
2. Observe the user list table
3. Verify pagination controls (if >10 users per page)
4. Navigate to page 2
**Expected Result**: Users listed with username, email, name, roles, status. Pagination works correctly. Total count matches expected users.
**Type**: Happy Path

---

### USR-02: Search users
**Role**: ADMIN
**Precondition**: Logged in as admin
**Steps**:
1. Navigate to User Management
2. Use search field to search for "doctor"
3. Observe filtered results
4. Clear search
**Expected Result**: Only users matching "doctor" appear (doctor1, doctor2). Clearing search shows all users again.
**Type**: Happy Path

---

### USR-03: Create new user - all fields
**Role**: ADMIN
**Precondition**: Logged in as admin
**Steps**:
1. Click "Create User" button
2. Fill in all fields: username (`testuser`), email (`test@example.com`), password, first name, last name, salutation (DR), status (ACTIVE)
3. Assign a role (DOCTOR)
4. Add a phone number
5. Submit
**Expected Result**: User created successfully. Success notification shown. User appears in list with correct data.
**Type**: Happy Path

---

### USR-04: Create user - validation errors
**Role**: ADMIN
**Precondition**: Logged in as admin
**Steps**:
1. Click "Create User"
2. Submit with empty required fields (username, email, password)
3. Enter an invalid email format
4. Enter a username that already exists (`admin`)
5. Enter an email that already exists (`admin@example.com`)
**Expected Result**: Validation errors shown for each case. Form not submitted until all validation passes. Duplicate username/email shows server-side error.
**Type**: Negative

---

### USR-05: Update user
**Role**: ADMIN
**Precondition**: Logged in as admin, a test user exists
**Steps**:
1. Click on a user in the list (e.g., `user1`)
2. Change first name, last name, and salutation
3. Save
4. Reopen the user
**Expected Result**: Changes saved. Success notification. Updated values shown when reopening.
**Type**: Happy Path

---

### USR-06: Change user status
**Role**: ADMIN
**Precondition**: Logged in as admin
**Steps**:
1. Edit user `user1`
2. Change status from ACTIVE to SUSPENDED
3. Save
4. Verify in user list
**Expected Result**: Status updated. User shows as SUSPENDED in list. User cannot log in (verify separately in AUTH-05).
**Type**: Happy Path

---

### USR-07: Soft-delete user
**Role**: ADMIN
**Precondition**: Logged in as admin, a test user exists
**Steps**:
1. Navigate to User Management
2. Click delete on a non-essential user (e.g., `user2`)
3. Confirm deletion
**Expected Result**: User disappears from list (soft deleted). User cannot log in. No cascade errors on related data.
**Type**: Happy Path

---

### USR-08: Assign/remove roles from user
**Role**: ADMIN
**Precondition**: Logged in as admin
**Steps**:
1. Edit user `user1`
2. Assign the DOCTOR role
3. Save
4. Verify user now has DOCTOR role
5. Remove the DOCTOR role
6. Save
7. Verify role removed
**Expected Result**: Role assignment and removal work. User's permissions change accordingly.
**Type**: Happy Path

---

### USR-09: User phone numbers CRUD
**Role**: ADMIN
**Precondition**: Logged in as admin
**Steps**:
1. Edit any user
2. Add a phone number (e.g., "5555-1234")
3. Save
4. Reopen user, verify phone exists
5. Add a second phone number
6. Delete the first phone number
7. Save
**Expected Result**: Phone numbers can be added, listed, and removed. Multiple phone numbers supported.
**Type**: Happy Path

---

### USR-10: Non-admin access to user management
**Role**: DOCTOR, NURSE, STAFF, USER
**Precondition**: Logged in as non-admin user
**Steps**:
1. Log in as `doctor1`
2. Try to navigate to `/admin/users` URL directly
3. Try to call `GET /api/v1/admin/users` via dev tools
4. Repeat for `staff1`, `nurse1`, `user1`
**Expected Result**: UI hides admin menu items. Direct URL navigation shows 403 or redirects. API returns 403 Forbidden.
**Type**: Permission

---

### USR-11: Admin resets another user's password
**Role**: ADMIN
**Precondition**: Logged in as admin, a non-admin user exists (e.g., `user1`)
**Steps**:
1. Navigate to User Management
2. Click on `user1`
3. Click "Reset Password" button
4. Confirm the action
**Expected Result**: A new random password is generated (displayed to admin or sent via email). `mustChangePassword` is set to `true` on the user. On next login, the user is forced to change their password (see AUTH-21).
**Type**: Happy Path

---

### USR-12: List deleted users
**Role**: ADMIN
**Precondition**: Logged in as admin, at least one user has been soft-deleted (USR-07)
**Steps**:
1. Navigate to User Management
2. Look for a "Deleted Users" tab or toggle
3. View the deleted users list
**Expected Result**: Soft-deleted users are listed separately with their original data (username, email, name). These users do not appear in the main active user list.
**Type**: Happy Path

---

### USR-13: Restore deleted user
**Role**: ADMIN
**Precondition**: Logged in as admin, a soft-deleted user exists (from USR-07 or USR-12)
**Steps**:
1. Navigate to the deleted users list (USR-12)
2. Click "Restore" on a deleted user
3. Confirm restoration
4. Check the active users list
**Expected Result**: User status restored to ACTIVE. User appears in active user list again. User can log in. `deleted_at` cleared in database.
**Type**: Happy Path

---

## Role Management Test Cases

### ROL-01: List roles
**Role**: ADMIN
**Precondition**: Logged in as admin
**Steps**:
1. Navigate to Role Management page
2. Observe the role list
**Expected Result**: All roles listed: ADMIN, USER, ADMINISTRATIVE_STAFF, DOCTOR, PSYCHOLOGIST, NURSE, CHIEF_NURSE. Shows name, code, description, and permission count.
**Type**: Happy Path

---

### ROL-02: Create new role
**Role**: ADMIN
**Precondition**: Logged in as admin
**Steps**:
1. Click "Create Role"
2. Enter name: "Pharmacist", code: "PHARMACIST", description: "Pharmacy staff"
3. Assign some permissions (e.g., inventory-item:read, inventory-movement:create)
4. Submit
**Expected Result**: Role created. Appears in list. Assigned permissions are correct.
**Type**: Happy Path

---

### ROL-03: Update role
**Role**: ADMIN
**Precondition**: Logged in as admin, test role exists
**Steps**:
1. Click on the test role
2. Change description
3. Add a new permission
4. Remove an existing permission
5. Save
**Expected Result**: All changes saved. Permissions correctly updated.
**Type**: Happy Path

---

### ROL-04: Delete role - no users assigned
**Role**: ADMIN
**Precondition**: Logged in as admin, a role exists with no users assigned
**Steps**:
1. Click delete on the unused role
2. Confirm
**Expected Result**: Role soft-deleted. Disappears from list.
**Type**: Happy Path

---

### ROL-05: Delete role - users assigned
**Role**: ADMIN
**Precondition**: Logged in as admin
**Steps**:
1. Try to delete DOCTOR role (which has users assigned)
2. Confirm deletion
**Expected Result**: Error message indicating role cannot be deleted because users are assigned to it.
**Type**: Negative

---

### ROL-06: View all permissions
**Role**: ADMIN
**Precondition**: Logged in as admin
**Steps**:
1. Navigate to permissions list (part of role management)
2. Review all available permissions
**Expected Result**: Complete list of permissions displayed: user:*, role:*, audit:read, patient:*, admission:*, clinical-history:*, progress-note:*, medical-order:*, nursing-note:*, vital-sign:*, medication-administration:*, psychotherapy-activity:*, psychotherapy-category:*, inventory-category:*, inventory-item:*, inventory-movement:*, billing:*, invoice:*.
**Type**: Happy Path

---

### ROL-07: Non-admin access to role management
**Role**: All non-admin roles
**Precondition**: Logged in as non-admin
**Steps**:
1. Log in as `staff1`
2. Try to access role management page
3. Try to call `GET /api/v1/admin/roles` directly
**Expected Result**: 403 Forbidden. Menu item hidden.
**Type**: Permission

---

## Audit Log Test Cases

### AUD-01: View audit logs
**Role**: ADMIN
**Precondition**: Logged in as admin, some operations have been performed
**Steps**:
1. Navigate to Audit Logs page
2. Observe the log entries
**Expected Result**: Paginated list of audit entries showing: entity type, entity ID, action (CREATE/UPDATE/DELETE), user, timestamp, before/after values.
**Type**: Happy Path

---

### AUD-02: Filter audit logs by entity type
**Role**: ADMIN
**Precondition**: Logged in as admin, audit entries exist for multiple entity types
**Steps**:
1. Navigate to Audit Logs
2. Filter by entity type "User"
3. Observe filtered results
4. Change filter to "Patient"
5. Clear filter
**Expected Result**: Only entries for the selected entity type shown. Clearing filter shows all entries.
**Type**: Happy Path

---

### AUD-03: Filter audit logs by action type
**Role**: ADMIN
**Precondition**: Logged in as admin
**Steps**:
1. Filter by action "CREATE"
2. Observe results
3. Filter by action "UPDATE"
4. Filter by action "DELETE"
**Expected Result**: Each filter shows only the matching action type entries.
**Type**: Happy Path

---

### AUD-04: Filter audit logs by user
**Role**: ADMIN
**Precondition**: Logged in as admin, multiple users have performed actions
**Steps**:
1. Filter by user "staff1"
2. Observe results
3. Filter by user "doctor1"
**Expected Result**: Only entries by the selected user shown.
**Type**: Happy Path

---

### AUD-05: Filter audit logs by date range
**Role**: ADMIN
**Precondition**: Logged in as admin
**Steps**:
1. Set date range to today
2. Observe results
3. Set date range to yesterday
**Expected Result**: Only entries within the date range shown.
**Type**: Happy Path

---

### AUD-06: Verify audit entry content
**Role**: ADMIN
**Precondition**: Logged in as admin, a user was recently updated
**Steps**:
1. Navigate to audit logs
2. Find the UPDATE entry for a user change
3. Inspect the before/after values
**Expected Result**: "Before" shows original values, "After" shows new values. Only changed fields differ.
**Type**: Happy Path

---

### AUD-07: Non-admin access to audit logs
**Role**: All non-admin roles
**Precondition**: Logged in as non-admin
**Steps**:
1. Log in as `doctor1`
2. Try to access audit logs page
3. Try to call `GET /api/v1/admin/audit-logs` directly
**Expected Result**: 403 Forbidden. Menu item hidden.
**Type**: Permission

---

## Permission Matrix

| Action | ADMIN | STAFF | DOCTOR | PSYCH | NURSE | CHIEF_NURSE | USER |
|--------|-------|-------|--------|-------|-------|-------------|------|
| List users | G | D | D | D | D | D | D |
| Create user | G | D | D | D | D | D | D |
| Update user | G | D | D | D | D | D | D |
| Delete user | G | D | D | D | D | D | D |
| List roles | G | D | D | D | D | D | D |
| Create role | G | D | D | D | D | D | D |
| Update role | G | D | D | D | D | D | D |
| Delete role | G | D | D | D | D | D | D |
| View audit logs | G | D | D | D | D | D | D |

G = Granted, D = Denied (403)

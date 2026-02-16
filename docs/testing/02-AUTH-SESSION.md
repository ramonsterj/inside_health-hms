# Module: Authentication & Session Management

**Module ID**: AUTH
**Related Roles**: All roles
**API Base**: `/api/auth/*`, `/api/v1/users/me`

---

## Overview

The HMS uses a dual-token JWT authentication system:
- **Access Token**: Short-lived (15-30 min), sent in Authorization header
- **Refresh Token**: Long-lived (7-30 days), stored in database, revocable
- Proactive token refresh 5 minutes before expiration
- Session expiration modal with redirect preservation
- Password reset via email token flow

---

## Test Cases

### AUTH-01: Login with valid credentials (each role)
**Role**: All roles
**Precondition**: Application running, seed data loaded
**Steps**:
1. Navigate to login page
2. Enter username and password for each user: `admin/admin123`, `staff1/admin123`, `doctor1/admin123`, `psych1/admin123`, `nurse1/admin123`, `chiefnurse1/admin123`, `user1/admin123`
3. Click "Login"
**Expected Result**: Login succeeds, user is redirected to dashboard/home page. User's name and role are displayed in the header/topbar.
**Type**: Happy Path

---

### AUTH-02: Login with invalid password
**Role**: N/A
**Precondition**: Application running
**Steps**:
1. Navigate to login page
2. Enter username `admin` and password `wrongpassword`
3. Click "Login"
**Expected Result**: Login fails. Error message displayed (e.g., "Invalid credentials"). No redirect occurs. No tokens stored.
**Type**: Negative

---

### AUTH-03: Login with nonexistent username
**Role**: N/A
**Precondition**: Application running
**Steps**:
1. Navigate to login page
2. Enter username `nonexistentuser` and password `admin123`
3. Click "Login"
**Expected Result**: Login fails with generic error message (should NOT reveal whether username exists). No tokens stored.
**Type**: Negative / Security

---

### AUTH-04: Login with empty fields
**Role**: N/A
**Precondition**: Application running
**Steps**:
1. Navigate to login page
2. Leave both username and password empty, click "Login"
3. Enter username only, leave password empty, click "Login"
4. Enter password only, leave username empty, click "Login"
**Expected Result**: Form validation prevents submission. Validation messages appear for required fields.
**Type**: Negative

---

### AUTH-05: Login with inactive/suspended user
**Role**: ADMIN (to suspend user first)
**Precondition**: A user exists with status INACTIVE or SUSPENDED
**Steps**:
1. As admin, set a user's status to SUSPENDED via user management
2. Log out
3. Try to log in with the suspended user's credentials
**Expected Result**: Login fails with appropriate message (e.g., "Account is suspended").
**Type**: Negative

---

### AUTH-06: Successful logout
**Role**: Any authenticated user
**Precondition**: Logged in as any user
**Steps**:
1. Click the logout button/link in the header
2. Observe redirect
3. Try to navigate to a protected route (e.g., `/patients`)
**Expected Result**: User is redirected to login page. Tokens are cleared from browser storage. Protected routes redirect to login.
**Type**: Happy Path

---

### AUTH-07: Access token automatic refresh
**Role**: Any authenticated user
**Precondition**: Logged in, access token about to expire
**Steps**:
1. Log in as any user
2. Wait until ~5 minutes before access token expiry (or modify token TTL to a short duration for testing)
3. Observe network requests
**Expected Result**: A refresh token request is automatically sent before expiry. New access token is stored. User session continues without interruption.
**Type**: Happy Path

---

### AUTH-08: Session expiration modal
**Role**: Any authenticated user
**Precondition**: Logged in, token expired or about to expire
**Steps**:
1. Log in as `doctor1`
2. Navigate to an admission detail page
3. Wait for both access and refresh tokens to expire (or clear tokens from storage to simulate)
4. Observe the UI
**Expected Result**: A session expiration modal appears. The modal shows a clear message and a re-login button. The current page is NOT navigated away from.
**Type**: Happy Path

---

### AUTH-09: Redirect preservation after re-login
**Role**: Any authenticated user
**Precondition**: Session expired while on a specific page
**Steps**:
1. Log in as `doctor1`
2. Navigate to a specific admission detail page (note the URL)
3. Wait for session to expire
4. When modal appears, click re-login
5. Enter credentials and log in
**Expected Result**: After re-login, user is redirected back to the exact page they were on (the admission detail URL), NOT the home/dashboard.
**Type**: Happy Path

---

### AUTH-10: Unauthenticated access to protected API
**Role**: N/A (no authentication)
**Precondition**: Not logged in
**Steps**:
1. Open browser dev tools
2. Make direct API calls to protected endpoints without Authorization header:
   - `GET /api/v1/patients`
   - `GET /api/v1/admissions`
   - `GET /api/v1/admin/users`
**Expected Result**: All return HTTP 401 Unauthorized.
**Type**: Security

---

### AUTH-11: Unauthenticated access to protected routes (frontend)
**Role**: N/A (not logged in)
**Precondition**: Not logged in, no tokens in storage
**Steps**:
1. Clear all browser storage (localStorage, sessionStorage)
2. Navigate directly to protected URLs:
   - `/patients`
   - `/admissions`
   - `/admin/users`
**Expected Result**: All routes redirect to the login page.
**Type**: Security

---

### AUTH-12: Password reset - request reset
**Role**: N/A
**Precondition**: Application running, a user exists with email
**Steps**:
1. Navigate to login page
2. Click "Forgot Password" link
3. Enter email `admin@example.com`
4. Submit
**Expected Result**: Success message displayed (e.g., "If the email exists, a reset link has been sent"). In dev, check console for email output (ConsoleEmailService). A PasswordResetToken is created in the database.
**Type**: Happy Path

---

### AUTH-13: Password reset - use token
**Role**: N/A
**Precondition**: A password reset token has been generated
**Steps**:
1. Obtain the reset token from console output (dev) or email
2. Navigate to the reset password page with the token
3. Enter a new password and confirm
4. Submit
**Expected Result**: Password is changed. Success message displayed. User can log in with the new password. Old password no longer works.
**Type**: Happy Path

---

### AUTH-14: Password reset - expired token
**Role**: N/A
**Precondition**: A password reset token that has expired
**Steps**:
1. Use an expired or previously used reset token
2. Try to reset the password
**Expected Result**: Error message (e.g., "Token is expired or invalid"). Password is NOT changed.
**Type**: Negative

---

### AUTH-15: Password reset - nonexistent email
**Role**: N/A
**Precondition**: Application running
**Steps**:
1. Navigate to forgot password page
2. Enter a non-existent email: `nobody@example.com`
3. Submit
**Expected Result**: Same success message as valid email (to prevent email enumeration). No error revealed.
**Type**: Security

---

### AUTH-16: Concurrent sessions (multiple tabs)
**Role**: Any user
**Precondition**: Logged in
**Steps**:
1. Log in as `admin` in Tab 1
2. Open a new tab (Tab 2) and navigate to the application
3. Verify Tab 2 is also logged in (shared storage)
4. Log out from Tab 1
5. Switch to Tab 2 and perform an action
**Expected Result**: Tab 2 should detect the logout and redirect to login (or show session expired modal).
**Type**: Happy Path

---

### AUTH-17: Concurrent sessions (multiple browsers)
**Role**: Any user
**Precondition**: Two different browsers or incognito windows
**Steps**:
1. Log in as `admin` in Browser A
2. Log in as `admin` in Browser B
3. Perform actions in both browsers
**Expected Result**: Both sessions work independently. Actions in one browser don't affect the other (both have separate refresh tokens).
**Type**: Happy Path

---

### AUTH-18: Login page i18n
**Role**: N/A
**Precondition**: Not logged in
**Steps**:
1. Navigate to login page
2. Find the language switcher
3. Switch to Spanish
4. Observe all labels (Username, Password, Login button, Forgot Password)
5. Switch back to English
**Expected Result**: All login page text translates correctly to Spanish and back to English.
**Type**: Happy Path

---

### AUTH-19: Current user profile endpoint
**Role**: Any authenticated user
**Precondition**: Logged in
**Steps**:
1. Log in as `doctor1`
2. Call `GET /api/v1/users/me` (or navigate to profile page)
**Expected Result**: Returns current user's profile (username, email, firstName, lastName, roles, permissions). Does NOT include password hash.
**Type**: Happy Path

---

### AUTH-20: Loading state on login
**Role**: N/A
**Precondition**: Application running
**Steps**:
1. Navigate to login page
2. Enter valid credentials
3. Click Login and observe the button
**Expected Result**: Login button shows a loading spinner/disabled state while the API call is in progress. Prevents double-submission.
**Type**: Happy Path

---

## Force Password Change Test Cases

### AUTH-21: Force password change - redirect on login
**Role**: ADMIN (setup), then test user
**Precondition**: Admin logged in
**Steps**:
1. As admin, create a new user with `mustChangePassword: true` (or use the admin password reset feature which sets it automatically)
2. Log out
3. Log in as the newly created user
**Expected Result**: After successful login, user is redirected to a force-change-password page instead of the dashboard. User cannot navigate away to other pages until password is changed.
**Type**: Happy Path

---

### AUTH-22: Force password change - successful change
**Role**: User with mustChangePassword=true
**Precondition**: Logged in as user who must change password, on force-change page
**Steps**:
1. Enter current password (`admin123`)
2. Enter new password (e.g., `NewSecure456!`)
3. Enter confirm password matching the new password
4. Submit
**Expected Result**: Password changed successfully. `mustChangePassword` flag cleared. User redirected to dashboard. User can now navigate freely. Logging out and back in with new password works normally (no force-change redirect).
**Type**: Happy Path

---

### AUTH-23: Force password change - validation errors
**Role**: User with mustChangePassword=true
**Precondition**: On force-change-password page
**Steps**:
1. Submit with all fields empty
2. Enter current password only, leave new/confirm empty
3. Enter new password that differs from confirm password
4. Enter new password that is the same as the current password
**Expected Result**: Validation errors for each case: required fields, password mismatch, and same-as-current rejection. Form not submitted until all validation passes.
**Type**: Negative

---

## Permission Tests Summary

| Action | Unauthenticated | Any Authenticated |
|--------|----------------|-------------------|
| Access login page | Allowed | Redirect to dashboard |
| Access protected routes | Redirect to login | Allowed (per role) |
| Access protected APIs | 401 Unauthorized | Per permission |
| Logout | N/A | Clears session |
| Password reset request | Allowed | Allowed |

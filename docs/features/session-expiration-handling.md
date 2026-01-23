# Feature: Session Expiration Handling

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-23 | @author | Initial draft |
| 1.1 | 2026-01-23 | @author | Updated to reflect implementation; added proactive monitoring documentation |

---

## Overview

Gracefully handle session expiration by notifying users with a modal dialog and redirecting them to login, then returning them to their previous location after re-authentication. Currently users experience confusing API errors without understanding their session has expired.

---

## Use Case / User Story

1. **As an authenticated user**, I want to be clearly notified when my session expires so that I understand why my actions are no longer working.

2. **As an authenticated user**, I want to be redirected to the login page after my session expires so that I can re-authenticate and continue my work.

3. **As an authenticated user**, I want to return to where I was working after re-authenticating so that I don't lose my place in the application.

4. **As an authenticated user**, I want the session expiration modal to prevent me from interacting with the page so that I don't accidentally lose data or make actions I think will be saved.

---

## Authorization / Role Access

| Action | Required Role(s) | Permission | Notes |
|--------|------------------|------------|-------|
| Experience session expiration | Any authenticated user | None | System-level behavior, applies universally |

This feature has no role-based differences - all authenticated users experience the same behavior when their session expires.

---

## Functional Requirements

1. **Detect session expiration**: When an API request fails with 401 and the refresh token is invalid/expired, trigger the session expiration flow. Additionally, proactively detect expiration by decoding the JWT and monitoring for when the token expires.

2. **Display notification modal**: Show a clear, blocking modal dialog explaining that the session has expired and the user must log in again.

3. **Block page interaction**: The modal must prevent all interaction with the page behind it (pointer events, scrolling, keyboard navigation).

4. **Redirect to login**: When the user acknowledges the modal, redirect them to the `/login` page.

5. **Preserve intended destination**: Store the user's current route (including query parameters) in a `redirect` query parameter on the login URL.

6. **Restore location after login**: After successful re-authentication, automatically redirect the user to their preserved location.

7. **Handle concurrent failures**: When multiple API requests fail simultaneously with 401, display only one modal (prevent stacking/duplicates).

---

## Acceptance Criteria / Scenarios

### Happy Path

1. **When** an API request fails with 401 and the refresh token is invalid/expired, **then** the session expiration modal is displayed.

2. **When** the modal is displayed, **then** it clearly communicates that the session has expired and the user needs to log in again.

3. **When** the modal is displayed, **then** it prevents all interaction with the page behind it (pointer events disabled, no scrolling, no keyboard interaction with underlying page).

4. **When** the user clicks the "Log In" button in the modal, **then** they are redirected to `/login` with the current route preserved in a `redirect` query parameter.

5. **When** the user successfully logs in with a `redirect` query parameter present, **then** they are automatically redirected to that location.

### Edge Cases

6. **When** multiple API requests fail simultaneously with 401, **then** only one modal is displayed (no stacking or duplicates).

7. **When** the preserved route contains query parameters (e.g., `/patients?page=2&filter=active`), **then** the full path including query params is preserved and restored after login.

8. **When** the user manually navigates to a different page after login (ignoring the redirect), **then** the redirect parameter is cleared and not persisted.

9. **When** a 401 error occurs but the refresh token is still valid, **then** the token refresh happens silently without showing the modal (existing behavior preserved).

10. **When** a network error occurs (not a 401), **then** the session expiration modal is NOT shown (differentiate auth failures from connectivity issues).

11. **When** the user is on a public/guest route (like `/login` or `/forgot-password`) and a background request fails, **then** no session modal is shown.

12. **When** the user attempts to dismiss the modal by pressing Escape or clicking outside, **then** the modal remains open (modal is not dismissable without clicking the Log In button).

---

## Non-Functional Requirements

- **User Experience**: The modal must be truly modal (blocking) to prevent users from interacting with the page and potentially losing data or making actions they believe will be persisted.
- **Reliability**: The single-modal guarantee must work even under rapid concurrent API failures.
- **Security**: Tokens must be cleared from storage before redirecting to login.

---

## API Contract

N/A - This is a frontend-only feature. No new API endpoints are required.

The feature relies on existing authentication infrastructure:
- `POST /api/auth/refresh` - Existing token refresh endpoint
- `POST /api/auth/login` - Existing login endpoint

---

## Database Changes

N/A - This is a frontend-only feature. No database changes are required.

---

## Frontend Changes

### Components

| Component | Location | Description |
|-----------|----------|-------------|
| `SessionExpiredModal.vue` | `src/components/auth/` | Modal dialog shown when session expires |

### Composables

| Composable | Location | Description |
|------------|----------|-------------|
| `useSessionExpiration` | `src/composables/useSessionExpiration.ts` | Manages session expiration state, event handling, and proactive token monitoring |

### Modified Files

| File | Location | Changes |
|------|----------|---------|
| `api.ts` | `src/services/` | Replace `window.location.href` redirect with event emission |
| `App.vue` | `src/` | Add SessionExpiredModal component |
| `LoginView.vue` | `src/views/auth/` | Handle redirect query parameter after successful login |
| `index.ts` | `src/router/` | Use `to.fullPath` for complete route preservation |

### Routes

No new routes required. Existing `/login` route will handle the redirect query parameter.

### State Management

The session expiration state will be managed via a composable with reactive state, not a Pinia store, since:
- The state is transient (not persisted)
- It's only used by one component
- It needs to be accessible from the API service layer

### Validation (Zod Schemas)

N/A - No form validation required for this feature.

---

## Implementation Notes

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        App.vue                               │
│  ┌─────────────────────────────────────────────────────┐    │
│  │           SessionExpiredModal.vue                    │    │
│  │  - Uses useSessionExpiration() composable            │    │
│  │  - Renders PrimeVue Dialog with modal=true           │    │
│  │  - closable=false, closeOnEscape=false               │    │
│  │  - On confirm: router.push('/login?redirect=...')    │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                              ▲
                              │ reads sessionExpired state
                              │
┌─────────────────────────────────────────────────────────────┐
│              useSessionExpiration.ts                         │
│  - sessionExpired: Ref<boolean>                              │
│  - intendedRoute: Ref<string | null>                         │
│  - triggerSessionExpired(route: string)                      │
│  - resetSessionExpired()                                     │
│  - scheduleExpirationCheck()                                 │
│  - Singleton pattern (shared state across imports)           │
│  - Proactive monitoring (JWT decode, visibility, intervals)  │
└─────────────────────────────────────────────────────────────┘
                              ▲
                              │ calls triggerSessionExpired()
                              │
┌─────────────────────────────────────────────────────────────┐
│                      api.ts interceptor                      │
│  - On refresh failure: call triggerSessionExpired()          │
│  - Guard: only trigger if not already expired                │
│  - Clear tokens before triggering                            │
└─────────────────────────────────────────────────────────────┘
```

### Key Implementation Details

1. **Singleton Composable**: The `useSessionExpiration` composable must use module-level state (outside the function) to ensure all consumers share the same reactive state.

2. **Guard Against Duplicates**: The `triggerSessionExpired` function should check if already expired before setting state, preventing multiple triggers.

3. **PrimeVue Dialog Props**: Use `modal`, `closable="false"`, `closeOnEscape="false"`, and `dismissableMask="false"` to create a truly blocking modal.

4. **Token Cleanup**: Tokens must be cleared in `api.ts` BEFORE triggering the modal, not after.

5. **Route Preservation**: Use `router.currentRoute.value.fullPath` to capture the complete route including query parameters.

6. **Login Redirect Handling**: In `LoginView.vue`, after successful login, check for `route.query.redirect` and use `router.push()` to navigate there.

### Proactive Token Expiration Detection

In addition to reactive detection (when API calls fail with 401), the implementation includes proactive monitoring that shows the modal immediately when the token expires:

1. **JWT Decoding**: The composable decodes the access token to read the `exp` claim and determine the exact expiration time.

2. **Scheduled Expiration Check**: A timeout is scheduled to trigger exactly when the token expires (with a small buffer), so the modal appears proactively.

3. **Visibility Change Detection**: When the user returns to the browser tab after being away, the token is checked immediately in case it expired while the tab was inactive.

4. **Periodic Polling**: A 60-second interval runs as a backup to catch any edge cases.

5. **Re-initialization After Login**: The `scheduleExpirationCheck()` function is exposed and called after successful login to set up monitoring for the new token.

This proactive approach improves UX by notifying users immediately when their session expires, rather than waiting for them to attempt an action that fails.

### Patterns to Follow

- Follow existing composable patterns in `src/composables/`
- Use PrimeVue Dialog component (already used in the project)
- Follow i18n patterns for all user-facing text
- Follow existing error handling patterns

---

## QA Checklist

### Backend
- [ ] N/A - Frontend-only feature

### Frontend
- [x] Components created and functional
- [x] ~~Pinia store implemented~~ Using composable instead (more appropriate for this use case)
- [x] Routes configured with proper guards (existing routes, enhanced handling)
- [ ] ~~Form validation with VeeValidate + Zod~~ N/A - No forms
- [x] Error handling implemented
- [x] ESLint/oxlint passes (0 errors, warnings are pre-existing)
- [x] i18n keys added for all user-facing text (en.json, es.json)
- [x] Unit tests written and passing (Vitest) - `useSessionExpiration.spec.ts`

### E2E Tests (Playwright)
- [x] Session expiration modal appears when refresh token is invalid
- [x] Modal blocks interaction with underlying page (Escape key test)
- [x] User is redirected to login with redirect parameter
- [x] Redirect parameter preserved in URL through navigation
- [x] Multiple concurrent 401s show only one modal
- [x] Full round-trip: user returns to original location after re-authentication
- [x] Network errors do not trigger session expiration modal

### General
- [x] Feature documentation updated (this document)
- [ ] Reviewed by project owner

---

## Documentation Updates Required

### Must Update

- [x] **[CLAUDE.md](../../CLAUDE.md)**
  - Add "Session expiration handling with redirect" to Frontend implemented features

### Code Documentation

- [x] **`src/composables/useSessionExpiration.ts`**
  - Document the singleton pattern and usage (inline JSDoc comments)
- [x] **`src/services/api.ts`**
  - Document the session expiration trigger in interceptor comments

---

## Related Docs/Commits/Issues

- Related feature: Existing authentication system in `src/stores/auth.ts`
- Related file: Current API interceptor in `src/services/api.ts`
- Related file: Current router guards in `src/router/index.ts`

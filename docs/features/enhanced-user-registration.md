# Feature: Enhanced User Registration

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-23 | @author | Initial draft |

---

## Overview

Enhance the user registration system to improve security and data completeness. Changes include: password confirmation during creation, salutation field, multiple phone numbers per user, search/filter capabilities on the user table, dynamic role loading from database, removal of public registration (Admin-only user creation), and mandatory password change on first login.

**i18n**: Spanish - "Registro de usuario mejorado"

---

## Use Case / User Story

1. **As an Admin**, I want to create users with a confirmed password so that I can ensure passwords are entered correctly without typos.

2. **As an Admin**, I want to select a salutation (Sr., Sra., Srta., Dr., Dra.) when creating a user so that I can address them formally and appropriately.

3. **As an Admin**, I want to add multiple phone numbers with labels (mobile, practice, home) to a user so that I can store all their contact information.

4. **As an Admin**, I want to search users by name or email so that I can quickly find specific users.

5. **As an Admin**, I want to filter the users table by role so that I can view users grouped by their function.

6. **As an Admin**, I want roles in the user form to be loaded from the database so that newly created roles are immediately available for assignment.

7. **As a newly created User**, I want to be prompted to change my password on first login so that I can set a secure password only I know.

---

## Authorization / Role Access

| Action | Required Role(s) | Permission | Notes |
|--------|------------------|------------|-------|
| View users | ADMIN | `user:read` | List and view user details |
| Create user | ADMIN | `user:create` | Only Admin can create users |
| Update user | ADMIN | `user:update` | Includes phone numbers, salutation |
| Delete user | ADMIN | `user:delete` | Soft delete only |
| Search/filter users | ADMIN | `user:read` | |

**Important**: Public registration endpoint (`POST /api/auth/register`) will be **removed**.

---

## Functional Requirements

### New User Fields

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| `salutation` | Enum | SR, SRA, SRTA, DR, DRA, MR, MRS, MISS | Optional |
| `mustChangePassword` | Boolean | Default: true | Set to false after first password change |

### Phone Numbers (New Entity)

**Note**: Each user must have at least one phone number. The phone numbers array is required.

| Field | Type | Constraints |
|-------|------|-------------|
| `phoneNumber` | String | max 20 chars, required |
| `phoneType` | Enum | MOBILE, PRACTICE, HOME, WORK, OTHER |
| `isPrimary` | Boolean | Default: false |

### Requirements

- FR1: Password confirmation field in user creation form (frontend validation)
- FR2: Salutation dropdown with Spanish values (Sr., Sra., Srta., Dr., Dra.) and English (Mr., Mrs., Miss, Dr.)
- FR3: Support multiple phone numbers per user with type labels (at least one required)
- FR4: Search users by name or email in the user table
- FR5: Filter users table by role (dropdown filter)
- FR6: Load roles dynamically from database (remove hardcoded role options)
- FR7: Remove public registration endpoint - users can only be created by Admin
- FR8: New `mustChangePassword` flag on User, default `true` for new users
- FR9: First login detection redirects to password change screen
- FR10: Block navigation until password is changed on first login

---

## Acceptance Criteria / Scenarios

### User Creation - Happy Path

- When an Admin creates a user with matching password and confirm password, valid salutation, and at least one phone number, then the user is created successfully and 201 Created is returned.
- When a user is created, then the `mustChangePassword` flag is set to `true` by default.

### Password Confirmation

- When password and confirm password do not match, then the form shows validation error "Passwords do not match" and submit is disabled.
- When password is empty or doesn't meet complexity requirements, then return 400 Bad Request with appropriate validation errors.

### Salutation

- When an Admin selects a salutation from the dropdown, then only valid values (SR, SRA, SRTA, DR, DRA, MR, MRS, MISS) are accepted.
- When an invalid salutation value is submitted via API, then return 400 Bad Request.
- When salutation is not provided, then user is created without salutation (optional field).

### Phone Numbers

- When an Admin adds multiple phone numbers with types (MOBILE, PRACTICE, HOME, WORK, OTHER), then all are saved and associated with the user.
- When a phone number is submitted without a type, then return 400 Bad Request.
- When a user has no phone numbers, then return 400 Bad Request with "At least one phone number is required".
- When a phone number is marked as primary, then any other primary phone number for that user is unmarked.
- When updating a user and phone numbers array is empty, then return 400 Bad Request with "At least one phone number is required".

### Search & Filter

- When an Admin searches for "john", then users with "john" in their first name, last name, email, or username are returned.
- When an Admin filters by role "DOCTOR", then only users with that role are displayed.
- When an Admin searches with no results, then an empty list is returned with appropriate messaging.
- When an Admin combines search and role filter, then both criteria are applied.

### Dynamic Roles

- When roles exist in the database, then the role dropdown displays all active roles.
- When a new role is added to the database, then it appears in the dropdown without code changes.

### No Public Registration

- When an unauthenticated user attempts to access the registration endpoint, then return 401 Unauthorized (or 404 to hide existence).
- When a non-Admin user attempts to create a user, then return 403 Forbidden.

### First Login Password Change

- When a user with `mustChangePassword=true` logs in, then they are redirected to the password change screen.
- When a user on the password change screen tries to navigate away, then they are blocked until password is changed.
- When a user successfully changes their password on first login, then `mustChangePassword` is set to `false` and they proceed to the dashboard.
- When a user's new password matches their temporary password, then return 400 Bad Request with "New password must be different".

---

## Non-Functional Requirements

- Comprehensive E2E tests with Playwright covering all user flows
- At least one phone number is required per user; each phone number must have a valid type
- Salutation is optional
- All existing audit logging continues to work

---

## API Contract

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| POST | `/api/users` | `CreateUserRequest` | `UserResponse` | `user:create` | Create new user (enhanced) |
| GET | `/api/users` | Query params | `PagedResponse<UserResponse>` | `user:read` | List users with search/filter |
| GET | `/api/users/{id}` | - | `UserResponse` | `user:read` | Get user by ID |
| PUT | `/api/users/{id}` | `AdminUpdateUserRequest` | `UserResponse` | `user:update` | Update user (enhanced) |
| DELETE | `/api/users/{id}` | - | - | `user:delete` | Soft delete user |
| POST | `/api/users/{id}/phone-numbers` | `CreatePhoneNumberRequest` | `UserResponse` | `user:update` | Add phone number |
| PUT | `/api/users/{id}/phone-numbers/{phoneId}` | `UpdatePhoneNumberRequest` | `UserResponse` | `user:update` | Update phone number |
| DELETE | `/api/users/{id}/phone-numbers/{phoneId}` | - | `UserResponse` | `user:update` | Remove phone number |
| POST | `/api/auth/change-password` | `ChangePasswordRequest` | `AuthResponse` | Authenticated | Change own password |
| ~~POST~~ | ~~`/api/auth/register`~~ | - | - | - | **REMOVED** |

### Query Parameters for GET /api/users

| Parameter | Type | Description |
|-----------|------|-------------|
| `page` | Integer | Page number (default: 0) |
| `size` | Integer | Page size (default: 20) |
| `search` | String | Search by username, email, firstName, lastName |
| `roleCode` | String | Filter by role code (e.g., "ADMIN", "DOCTOR") |
| `status` | String | Filter by status (ACTIVE, INACTIVE, SUSPENDED) |
| `includeDeleted` | Boolean | Include soft-deleted users (default: false) |

### Request/Response Examples

```json
// POST /api/users - Request (Enhanced)
{
  "username": "dr.garcia",
  "email": "garcia@hospital.com",
  "password": "TempPass123!",
  "firstName": "Carlos",
  "lastName": "García",
  "salutation": "DR",
  "roleCodes": ["DOCTOR"],
  "status": "ACTIVE",
  "phoneNumbers": [
    {
      "phoneNumber": "+502 5555-1234",
      "phoneType": "MOBILE",
      "isPrimary": true
    },
    {
      "phoneNumber": "+502 2222-5678",
      "phoneType": "PRACTICE",
      "isPrimary": false
    }
  ]
}

// Response - 201 Created
{
  "success": true,
  "data": {
    "id": 5,
    "username": "dr.garcia",
    "email": "garcia@hospital.com",
    "firstName": "Carlos",
    "lastName": "García",
    "salutation": "DR",
    "salutationDisplay": "Dr.",
    "status": "ACTIVE",
    "mustChangePassword": true,
    "emailVerified": false,
    "roles": [
      {
        "code": "DOCTOR",
        "name": "Doctor"
      }
    ],
    "phoneNumbers": [
      {
        "id": 1,
        "phoneNumber": "+502 5555-1234",
        "phoneType": "MOBILE",
        "isPrimary": true
      },
      {
        "id": 2,
        "phoneNumber": "+502 2222-5678",
        "phoneType": "PRACTICE",
        "isPrimary": false
      }
    ],
    "createdAt": "2026-01-23T10:00:00Z",
    "createdBy": 1
  }
}

// POST /api/auth/change-password - Request
{
  "currentPassword": "TempPass123!",
  "newPassword": "MySecurePass456!",
  "confirmNewPassword": "MySecurePass456!"
}

// Response - 200 OK (mustChangePassword flow)
{
  "success": true,
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "...",
    "user": {
      "id": 5,
      "username": "dr.garcia",
      "mustChangePassword": false,
      ...
    }
  }
}
```

### Salutation Enum Values

| Code | Spanish Display | English Display |
|------|-----------------|-----------------|
| SR | Sr. | Mr. |
| SRA | Sra. | Mrs. |
| SRTA | Srta. | Miss |
| DR | Dr. | Dr. |
| DRA | Dra. | Dr. |
| MR | Mr. | Mr. |
| MRS | Mrs. | Mrs. |
| MISS | Miss | Miss |

---

## Database Changes

### Modified Entities

| Entity | Table | Changes |
|--------|-------|---------|
| `User` | `users` | Add `salutation`, `must_change_password` columns |

### New Entities

| Entity | Table | Extends | Description |
|--------|-------|---------|-------------|
| `UserPhoneNumber` | `user_phone_numbers` | `BaseEntity` | User phone numbers with type |

### New Migrations

| Migration | Description |
|-----------|-------------|
| `V016__add_user_salutation_and_must_change_password.sql` | Add columns to users table |
| `V017__create_user_phone_numbers_table.sql` | Create phone numbers table |
| `V018__set_existing_users_must_change_password_false.sql` | Set existing users to not require password change |

### Schema

```sql
-- V016__add_user_salutation_and_must_change_password.sql
ALTER TABLE users ADD COLUMN salutation VARCHAR(10);
ALTER TABLE users ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT TRUE;

-- V017__create_user_phone_numbers_table.sql
CREATE TABLE user_phone_numbers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    phone_number VARCHAR(20) NOT NULL,
    phone_type VARCHAR(20) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_user_phone_numbers_user_id ON user_phone_numbers(user_id);
CREATE INDEX idx_user_phone_numbers_deleted_at ON user_phone_numbers(deleted_at);

-- V018__set_existing_users_must_change_password_false.sql
-- Existing users should not be forced to change password
UPDATE users SET must_change_password = FALSE WHERE deleted_at IS NULL;
```

### Index Requirements

- [x] `deleted_at` - Required for soft delete queries
- [x] `user_id` - Foreign key on user_phone_numbers
- [x] Existing indexes on users table remain unchanged

---

## Frontend Changes

### Components

| Component | Location | Description |
|-----------|----------|-------------|
| `UsersView.vue` | `src/views/` | Enhanced with role filter, dynamic roles |
| `UserForm.vue` | `src/components/users/` | New: confirm password, salutation, phone numbers |
| `PhoneNumberInput.vue` | `src/components/users/` | New: reusable phone number sub-form |
| `ForcePasswordChange.vue` | `src/views/auth/` | New: mandatory password change screen |

### Pinia Stores

| Store | Location | Description |
|-------|----------|-------------|
| `useUserStore` | `src/stores/user.ts` | Enhanced: search, filter by role |
| `useRoleStore` | `src/stores/role.ts` | Already exists, use for dynamic role loading |
| `useAuthStore` | `src/stores/auth.ts` | Enhanced: handle mustChangePassword redirect |

### Routes

| Path | Component | Auth Required | Notes |
|------|-----------|---------------|-------|
| `/users` | `UsersView` | Yes (ADMIN) | Existing, enhanced |
| `/auth/change-password` | `ForcePasswordChange` | Yes | New route for first-login flow |

### Router Guard Enhancement

```typescript
// src/router/index.ts
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  // If user must change password, redirect to change-password page
  if (authStore.user?.mustChangePassword && to.path !== '/auth/change-password') {
    next('/auth/change-password')
    return
  }

  // ... existing guards
})
```

### Validation (Zod Schemas)

```typescript
// src/schemas/user.ts
import { z } from 'zod'

export const salutationEnum = z.enum(['SR', 'SRA', 'SRTA', 'DR', 'DRA', 'MR', 'MRS', 'MISS'])

export const phoneTypeEnum = z.enum(['MOBILE', 'PRACTICE', 'HOME', 'WORK', 'OTHER'])

export const phoneNumberSchema = z.object({
  id: z.number().optional(),
  phoneNumber: z.string().min(1, 'user.phone.required').max(20),
  phoneType: phoneTypeEnum,
  isPrimary: z.boolean().default(false)
})

export const createUserSchema = z.object({
  username: z.string().min(3).max(50),
  email: z.string().email().max(255),
  password: z.string().min(8, 'user.password.minLength'),
  confirmPassword: z.string().min(8),
  firstName: z.string().max(100).optional(),
  lastName: z.string().max(100).optional(),
  salutation: salutationEnum.optional(),
  roleCodes: z.array(z.string()).min(1, 'user.roles.required'),
  status: z.enum(['ACTIVE', 'INACTIVE', 'SUSPENDED']).default('ACTIVE'),
  phoneNumbers: z.array(phoneNumberSchema).min(1, 'validation.phone.required')
}).refine((data) => data.password === data.confirmPassword, {
  message: 'user.password.mismatch',
  path: ['confirmPassword']
})

export const changePasswordSchema = z.object({
  currentPassword: z.string().min(1, 'user.password.currentRequired'),
  newPassword: z.string().min(8, 'user.password.minLength'),
  confirmNewPassword: z.string().min(8)
}).refine((data) => data.newPassword === data.confirmNewPassword, {
  message: 'user.password.mismatch',
  path: ['confirmNewPassword']
}).refine((data) => data.currentPassword !== data.newPassword, {
  message: 'user.password.mustBeDifferent',
  path: ['newPassword']
})
```

### i18n Keys Required

```json
// English (en.json) additions
{
  "user": {
    "salutation": "Salutation",
    "salutations": {
      "SR": "Mr.",
      "SRA": "Mrs.",
      "SRTA": "Miss",
      "DR": "Dr.",
      "DRA": "Dr.",
      "MR": "Mr.",
      "MRS": "Mrs.",
      "MISS": "Miss"
    },
    "phoneNumbers": "Phone Numbers",
    "phoneNumber": "Phone Number",
    "phoneType": "Type",
    "phoneTypes": {
      "MOBILE": "Mobile",
      "PRACTICE": "Practice",
      "HOME": "Home",
      "WORK": "Work",
      "OTHER": "Other"
    },
    "addPhoneNumber": "Add Phone Number",
    "removePhoneNumber": "Remove",
    "primaryPhone": "Primary",
    "confirmPassword": "Confirm Password",
    "filterByRole": "Filter by Role",
    "allRoles": "All Roles",
    "mustChangePassword": "Must change password on next login",
    "password": {
      "mismatch": "Passwords do not match",
      "minLength": "Password must be at least 8 characters",
      "currentRequired": "Current password is required",
      "mustBeDifferent": "New password must be different from current password"
    }
  },
  "auth": {
    "changePassword": "Change Password",
    "currentPassword": "Current Password",
    "newPassword": "New Password",
    "confirmNewPassword": "Confirm New Password",
    "passwordChangeRequired": "You must change your password before continuing",
    "passwordChanged": "Password changed successfully"
  }
}

// Spanish (es.json) additions
{
  "user": {
    "salutation": "Tratamiento",
    "salutations": {
      "SR": "Sr.",
      "SRA": "Sra.",
      "SRTA": "Srta.",
      "DR": "Dr.",
      "DRA": "Dra.",
      "MR": "Mr.",
      "MRS": "Mrs.",
      "MISS": "Miss"
    },
    "phoneNumbers": "Teléfonos",
    "phoneNumber": "Número de Teléfono",
    "phoneType": "Tipo",
    "phoneTypes": {
      "MOBILE": "Móvil",
      "PRACTICE": "Consultorio",
      "HOME": "Casa",
      "WORK": "Trabajo",
      "OTHER": "Otro"
    },
    "addPhoneNumber": "Agregar Teléfono",
    "removePhoneNumber": "Eliminar",
    "primaryPhone": "Principal",
    "confirmPassword": "Confirmar Contraseña",
    "filterByRole": "Filtrar por Rol",
    "allRoles": "Todos los Roles",
    "mustChangePassword": "Debe cambiar contraseña en próximo inicio",
    "password": {
      "mismatch": "Las contraseñas no coinciden",
      "minLength": "La contraseña debe tener al menos 8 caracteres",
      "currentRequired": "La contraseña actual es requerida",
      "mustBeDifferent": "La nueva contraseña debe ser diferente a la actual"
    }
  },
  "auth": {
    "changePassword": "Cambiar Contraseña",
    "currentPassword": "Contraseña Actual",
    "newPassword": "Nueva Contraseña",
    "confirmNewPassword": "Confirmar Nueva Contraseña",
    "passwordChangeRequired": "Debe cambiar su contraseña antes de continuar",
    "passwordChanged": "Contraseña cambiada exitosamente"
  }
}
```

---

## Implementation Notes

- **Password Confirmation**: Frontend-only validation. Backend does not receive `confirmPassword` - the validation happens before form submission.
- **Salutation Enum**: Create `Salutation` enum in backend, store as STRING in database.
- **Phone Numbers**: Use `@OneToMany` with cascade. When only one primary is allowed, implement logic in service layer to unset other primaries.
- **Dynamic Roles**: The role store already fetches from `/api/roles`. Remove hardcoded role options in UsersView and use the store.
- **Remove Public Registration**: Delete or comment out the `/api/auth/register` endpoint in `AuthController.kt`. Consider keeping the endpoint but requiring authentication with `user:create` permission.
- **First Login Flow**: Check `mustChangePassword` in the JWT claims or user response. Frontend router guard handles redirect.
- **Existing User Pattern**: Follow the existing `User` entity pattern. Use `@SQLRestriction` on `UserPhoneNumber`.

---

## QA Checklist

### Backend
- [ ] All functional requirements implemented
- [ ] `User` entity updated with `salutation` and `mustChangePassword` fields
- [ ] `UserPhoneNumber` entity extends `BaseEntity`
- [ ] `UserPhoneNumber` entity has `@SQLRestriction("deleted_at IS NULL")`
- [ ] `Salutation` enum created with all values
- [ ] `PhoneType` enum created with all values
- [ ] DTOs updated: `CreateUserRequest`, `AdminUpdateUserRequest`, `UserResponse`
- [ ] Phone number CRUD endpoints implemented
- [ ] Password change endpoint updates `mustChangePassword` to false
- [ ] Public registration endpoint removed/secured
- [ ] Search by name/email implemented
- [ ] Filter by role implemented
- [ ] Input validation in place
- [ ] Unit tests written and passing
- [ ] Integration tests written and passing (Testcontainers)
- [ ] Detekt passes (no new violations)
- [ ] OWASP dependency-check passes

### Frontend
- [ ] `UsersView.vue` enhanced with role filter dropdown
- [ ] Role options loaded dynamically from role store (hardcoded options removed)
- [ ] User form has confirm password field with validation
- [ ] User form has salutation dropdown
- [ ] `PhoneNumberInput.vue` component created
- [ ] Phone numbers can be added/removed in user form
- [ ] Primary phone toggle works correctly
- [ ] `ForcePasswordChange.vue` view created
- [ ] Router guard redirects users with `mustChangePassword=true`
- [ ] Navigation blocked on password change screen
- [ ] Pinia stores updated
- [ ] Form validation with VeeValidate + Zod
- [ ] Error handling implemented
- [ ] ESLint/oxlint passes
- [ ] i18n keys added (English and Spanish)
- [ ] Unit tests written and passing (Vitest)

### E2E Tests (Playwright)
- [ ] **Create User Flow**: Admin can create user with confirm password, salutation, and phone numbers
- [ ] **Password Mismatch**: Form shows error when passwords don't match
- [ ] **Phone Number Required**: Form shows error when no phone numbers are provided
- [ ] **Phone Number Management**: Admin can add multiple phone numbers with different types
- [ ] **Primary Phone Toggle**: Setting primary unsets other primaries
- [ ] **Search Users**: Admin can search users by name or email
- [ ] **Filter by Role**: Admin can filter users by role
- [ ] **Combined Search/Filter**: Search and role filter work together
- [ ] **Dynamic Roles**: Newly created roles appear in dropdown
- [ ] **No Public Registration**: Unauthenticated POST to /api/auth/register returns 401/404
- [ ] **First Login Redirect**: New user is redirected to password change on first login
- [ ] **Navigation Block**: User cannot navigate away from password change screen
- [ ] **Password Change Success**: After changing password, user proceeds to dashboard
- [ ] **Same Password Rejected**: Using current password as new password shows error
- [ ] **Permission Denied**: Non-admin attempting user creation sees 403 error

### General
- [ ] API contract documented
- [ ] Database migrations tested
- [ ] Feature documentation updated
- [ ] Reviewed by project owner

---

## Documentation Updates Required

### Must Update

- [ ] **[CLAUDE.md](../../CLAUDE.md)**
  - Add `UserPhoneNumber` entity to "Implemented Features"
  - Note removal of public registration endpoint
  - Add `mustChangePassword` to user management features
- [ ] **[ARCHITECTURE.md](../architecture/ARCHITECTURE.md)** (if exists)
  - Update user management section

### Review for Consistency

- [ ] **[README.md](../../web/README.md)**
  - Update if setup instructions mention public registration

### Code Documentation

- [ ] **`api/src/main/kotlin/.../entity/User.kt`**
  - Document new fields
- [ ] **`api/src/main/kotlin/.../entity/UserPhoneNumber.kt`**
  - Add KDoc comments
- [ ] **`api/src/main/kotlin/.../service/UserService.kt`**
  - Document phone number management methods

---

## Related Docs/Commits/Issues

- Related feature: User management (existing)
- Related feature: [new-patient-intake.md](./new-patient-intake.md) - Similar phone number pattern
- Design discussion: This feature spec document

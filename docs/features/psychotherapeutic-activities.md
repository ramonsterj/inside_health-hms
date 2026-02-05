# Feature: Psychotherapeutic Activities (Actividades Psicoterapéuticas)

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-02-05 | @author | Initial draft |
| 1.1 | 2026-02-05 | @author | Added: API validation for outpatient admissions, category deletion restriction (cannot delete if in use), delete confirmation modal, sort query parameter documentation |

---

## Overview

Enables psychologists to register therapeutic activities for hospitalized patients during their stay. Activities are displayed within the admission details but are part of the ongoing hospitalization care. Each activity includes a description, category (workshop, private session, pet therapy, etc.), and is automatically timestamped with the registering user. Activities are immutable once created; only admins can delete entries.

---

## Use Case / User Story

**For Psychologists:**
1. As a **psychologist**, I want to register therapeutic activities for hospitalized patients so that I can document their participation in therapeutic interventions during their stay.
2. As a **psychologist**, I want to select an activity category from a predefined list so that activities are consistently categorized.

**For Clinical Staff (Doctors, Nurses, Psychologists, Admins):**
3. As a **clinical staff member**, I want to view all therapeutic activities for a patient within their admission details so that I can understand their therapeutic progress.
4. As a **clinical staff member**, I want to sort activities by newest or oldest first so that I can review them in my preferred order.

**For Admins:**
5. As an **admin**, I want to manage activity categories (create, edit, delete) so that psychologists have appropriate options when registering activities.
6. As an **admin**, I want to delete activities when necessary so that erroneous entries can be removed.

---

## Authorization / Role Access

### Psychotherapeutic Activities

| Action | Required Role(s) | Permission | Notes |
|--------|------------------|------------|-------|
| View | PSYCHOLOGIST, DOCTOR, NURSE, ADMIN | `psychotherapy-activity:read` | All clinical staff can view |
| Create | PSYCHOLOGIST | `psychotherapy-activity:create` | Only psychologists can register |
| Edit | - | - | Activities are immutable (no editing allowed) |
| Delete | ADMIN | `psychotherapy-activity:delete` | Soft delete only |

### Psychotherapeutic Activity Categories

| Action | Required Role(s) | Permission | Notes |
|--------|------------------|------------|-------|
| View | PSYCHOLOGIST, DOCTOR, NURSE, ADMIN | `psychotherapy-category:read` | All clinical staff can view |
| Create | ADMIN | `psychotherapy-category:create` | Admin only |
| Edit | ADMIN | `psychotherapy-category:update` | Admin only |
| Delete | ADMIN | `psychotherapy-category:delete` | Cannot delete if in use by activities |

---

## Functional Requirements

### Psychotherapeutic Activity Categories (Admin-managed)

- Admin-managed lookup table for activity types
- **Fields:**
  - **Name** - Category name (required, max 100 chars)
  - **Description** - Optional description (max 255 chars)
  - **Display Order** - For ordering in dropdown (default 0)
  - **Active** - Boolean to enable/disable without deleting
- **Default categories (seeded):**
  1. Taller (Workshop)
  2. Sesión individual (Private session)
  3. Terapia con mascotas (Pet therapy)
  4. Pilates
  5. Meditación guiada (Guided meditation)
  6. Terapia grupal (Group therapy)
  7. Arte terapia (Art therapy)
  8. Musicoterapia (Music therapy)
  9. Terapia ocupacional (Occupational therapy)
  10. Otra (Other)
- Displayed in admin settings alongside Triage Codes and Rooms
- **Deletion restriction**: Categories that have been used by any activities (including soft-deleted activities) cannot be deleted; use the "Active" toggle to hide categories from the dropdown instead

### Psychotherapeutic Activities

- **Multiple entries allowed** per admission (unlimited)
- Only for **hospitalized patients** (admission type = HOSPITALIZACION)
- **Fields per activity:**
  1. **Category** - Selected from predefined categories (required, dropdown)
  2. **Description** - Free text description of the activity (required, max 2000 chars)
  3. **Created At** - Auto-generated timestamp
  4. **Created By** - Auto-captured from authenticated user
- Display as chronological list within admission details
- Sortable by newest-first or oldest-first (toggle, default newest-first)
- Each entry shows: category name, description, timestamp, who registered it
- **Immutable**: Activities cannot be edited after creation
- Only admins can delete activities (soft delete)

### General Requirements

- Displayed as a tab/section within admission detail view (alongside Clinical History, Progress Notes, Medical Orders)
- Only visible for hospitalized admissions (not outpatient/ambulatorio)
- Full audit trail: created_by, created_at visible on all entries
- All user-facing text localized in English and Spanish

---

## Acceptance Criteria / Scenarios

### Activity Registration - Happy Path

- Given a psychologist viewing admission details for a hospitalized patient, when they submit a valid activity with category and description, then the activity is saved with current timestamp and registering user, and appears in the activity list.
- Given an activity is registered, when viewing the activity list, then it shows category name, description, date/time, and who registered it.

### Activity Viewing & Sorting

- Given a patient has multiple activities, when a clinical staff member views the admission details, then all activities are displayed.
- Given multiple activities exist, when the user toggles sort order, then activities are sorted by newest-first or oldest-first accordingly.

### Activity Categories Management - Happy Path

- Given an admin accesses the activity categories admin page, when they create a new category, then it becomes available in the dropdown for psychologists.
- Given an admin edits a category name, when psychologists register new activities, then the updated name appears in the dropdown.
- Given an admin deactivates a category, when psychologists view the category dropdown, then the deactivated category is not shown.

### Authorization - Edge Cases

- Given a non-psychologist user (doctor, nurse, admin), when they attempt to create an activity, then the API returns 403 Forbidden and the UI displays a localized message: "You do not have permission to register activities. Only psychologists can perform this action." (EN) / "No tiene permiso para registrar actividades. Solo los psicólogos pueden realizar esta acción." (ES)
- Given a psychologist attempts to delete an activity, then the API returns 403 Forbidden and the UI displays a localized message: "You do not have permission to delete activities. Please contact an administrator." (EN) / "No tiene permiso para eliminar actividades. Por favor contacte a un administrador." (ES)
- Given an unauthenticated user, when they attempt any activity operation, then they are redirected to login.

### Validation - Edge Cases

- Given a psychologist submits an activity without a description, then a validation error is displayed: "Description is required." (EN) / "La descripción es requerida." (ES)
- Given a psychologist submits an activity without selecting a category, then a validation error is displayed: "Category is required." (EN) / "La categoría es requerida." (ES)
- Given a psychologist submits a description exceeding 2000 characters, then a validation error is displayed: "Description must not exceed 2000 characters." (EN) / "La descripción no debe exceder 2000 caracteres." (ES)

### Business Rule - Edge Cases

- Given a patient admission type is AMBULATORIO (outpatient), when viewing the admission details, then the Psychotherapeutic Activities tab/section is not displayed.
- Given a patient admission type is AMBULATORIO (outpatient), when a psychologist attempts to create an activity via API, then the API returns 400 Bad Request with message: "Psychotherapeutic activities can only be registered for hospitalized patients." (EN) / "Las actividades psicoterapéuticas solo pueden registrarse para pacientes hospitalizados." (ES)
- Given an admin attempts to delete an activity category that has existing activities (including soft-deleted activities), then the API returns 400 Bad Request with message: "Cannot delete category that is in use by existing activities." (EN) / "No se puede eliminar una categoría que está en uso por actividades existentes." (ES)

### Delete - Edge Cases

- Given an admin clicks the delete button on an activity, then a confirmation modal is displayed asking: "Are you sure you want to delete this activity?" (EN) / "¿Está seguro de que desea eliminar esta actividad?" (ES)
- Given an admin confirms deletion in the modal, then the activity is soft-deleted and no longer appears in the activity list.
- Given a non-admin attempts to delete an activity via API, then the API returns 403 Forbidden and the UI displays a localized permission error message.

---

## Non-Functional Requirements

- **Security**: Activities are immutable for all users; only admins can delete
- **Audit**: Full audit trail on all entries (created_by, created_at)
- **i18n**: All user-facing text must be localized in English and Spanish
- **Performance**: Activity list should handle many entries per admission efficiently

---

## API Contract

### Activity Category Endpoints (Admin)

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| GET | `/api/v1/admin/psychotherapy-categories` | - | `List<PsychotherapyCategoryResponse>` | Yes | List all categories (admin) |
| GET | `/api/v1/psychotherapy-categories` | - | `List<PsychotherapyCategoryResponse>` | Yes | List active categories (for dropdown) |
| GET | `/api/v1/admin/psychotherapy-categories/{id}` | - | `PsychotherapyCategoryResponse` | Yes | Get single category |
| POST | `/api/v1/admin/psychotherapy-categories` | `CreatePsychotherapyCategoryRequest` | `PsychotherapyCategoryResponse` | Yes | Create category (admin) |
| PUT | `/api/v1/admin/psychotherapy-categories/{id}` | `UpdatePsychotherapyCategoryRequest` | `PsychotherapyCategoryResponse` | Yes | Update category (admin) |
| DELETE | `/api/v1/admin/psychotherapy-categories/{id}` | - | - | Yes | Soft delete category (admin) |

### Psychotherapeutic Activity Endpoints

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| GET | `/api/v1/admissions/{admissionId}/psychotherapy-activities?sort={asc\|desc}` | - | `List<PsychotherapyActivityResponse>` | Yes | List activities for admission (default: desc) |
| GET | `/api/v1/admissions/{admissionId}/psychotherapy-activities/{id}` | - | `PsychotherapyActivityResponse` | Yes | Get single activity |
| POST | `/api/v1/admissions/{admissionId}/psychotherapy-activities` | `CreatePsychotherapyActivityRequest` | `PsychotherapyActivityResponse` | Yes | Create activity (psychologist only) |
| DELETE | `/api/v1/admissions/{admissionId}/psychotherapy-activities/{id}` | - | - | Yes | Soft delete activity (admin only) |

### Request/Response Examples

```json
// GET /api/v1/psychotherapy-categories - Response
[
  {
    "id": 1,
    "name": "Taller",
    "description": "Workshop activities",
    "displayOrder": 1,
    "active": true
  },
  {
    "id": 2,
    "name": "Sesión individual",
    "description": "Private one-on-one sessions",
    "displayOrder": 2,
    "active": true
  }
]

// POST /api/v1/admin/psychotherapy-categories - Request
{
  "name": "Yoga terapéutico",
  "description": "Therapeutic yoga sessions",
  "displayOrder": 11,
  "active": true
}

// POST /api/v1/admissions/{admissionId}/psychotherapy-activities - Request
{
  "categoryId": 1,
  "description": "Patient participated in morning art workshop. Engaged well with group activities and expressed positive emotions through painting."
}

// Response - PsychotherapyActivityResponse
{
  "id": 1,
  "admissionId": 123,
  "category": {
    "id": 1,
    "name": "Taller"
  },
  "description": "Patient participated in morning art workshop. Engaged well with group activities and expressed positive emotions through painting.",
  "createdAt": "2026-02-05T10:30:00",
  "createdBy": {
    "id": 10,
    "salutation": "Lic.",
    "firstName": "Sofia",
    "lastName": "Martinez"
  }
}

// GET /api/v1/admissions/{admissionId}/psychotherapy-activities?sort=desc - Response
[
  {
    "id": 3,
    "admissionId": 123,
    "category": { "id": 5, "name": "Meditación guiada" },
    "description": "Evening guided meditation session...",
    "createdAt": "2026-02-05T18:00:00",
    "createdBy": { "id": 10, "salutation": "Lic.", "firstName": "Sofia", "lastName": "Martinez" }
  },
  {
    "id": 2,
    "admissionId": 123,
    "category": { "id": 2, "name": "Sesión individual" },
    "description": "Private session focusing on anxiety management...",
    "createdAt": "2026-02-05T14:00:00",
    "createdBy": { "id": 10, "salutation": "Lic.", "firstName": "Sofia", "lastName": "Martinez" }
  },
  {
    "id": 1,
    "admissionId": 123,
    "category": { "id": 1, "name": "Taller" },
    "description": "Patient participated in morning art workshop...",
    "createdAt": "2026-02-05T10:30:00",
    "createdBy": { "id": 10, "salutation": "Lic.", "firstName": "Sofia", "lastName": "Martinez" }
  }
]
```

---

## Database Changes

### New Entities

| Entity | Table | Extends | Description |
|--------|-------|---------|-------------|
| `PsychotherapyCategory` | `psychotherapy_categories` | `BaseEntity` | Activity category lookup table |
| `PsychotherapyActivity` | `psychotherapy_activities` | `BaseEntity` | Individual activity records |

### New Migrations

| Migration | Description |
|-----------|-------------|
| `V040__create_psychotherapy_categories_table.sql` | Creates psychotherapy_categories table with seed data |
| `V041__create_psychotherapy_activities_table.sql` | Creates psychotherapy_activities table |
| `V042__add_psychotherapy_permissions.sql` | Adds permissions for psychotherapy-activity and psychotherapy-category resources |

### Schema

```sql
-- V040__create_psychotherapy_categories_table.sql
CREATE TABLE psychotherapy_categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    display_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_psychotherapy_categories_deleted_at ON psychotherapy_categories(deleted_at);
CREATE INDEX idx_psychotherapy_categories_display_order ON psychotherapy_categories(display_order);
CREATE INDEX idx_psychotherapy_categories_active ON psychotherapy_categories(active);

-- Seed default categories
INSERT INTO psychotherapy_categories (name, description, display_order, active, created_at, updated_at) VALUES
('Taller', 'Workshop activities', 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Sesión individual', 'Private one-on-one sessions', 2, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Terapia con mascotas', 'Pet-assisted therapy', 3, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Pilates', 'Pilates exercise sessions', 4, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Meditación guiada', 'Guided meditation sessions', 5, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Terapia grupal', 'Group therapy sessions', 6, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Arte terapia', 'Art therapy activities', 7, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Musicoterapia', 'Music therapy sessions', 8, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Terapia ocupacional', 'Occupational therapy', 9, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Otra', 'Other activities', 99, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- V041__create_psychotherapy_activities_table.sql
CREATE TABLE psychotherapy_activities (
    id BIGSERIAL PRIMARY KEY,
    admission_id BIGINT NOT NULL REFERENCES admissions(id),
    category_id BIGINT NOT NULL REFERENCES psychotherapy_categories(id),
    description VARCHAR(2000) NOT NULL,
    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_psychotherapy_activities_deleted_at ON psychotherapy_activities(deleted_at);
CREATE INDEX idx_psychotherapy_activities_admission_id ON psychotherapy_activities(admission_id);
CREATE INDEX idx_psychotherapy_activities_category_id ON psychotherapy_activities(category_id);
CREATE INDEX idx_psychotherapy_activities_created_at ON psychotherapy_activities(created_at);

-- V042__add_psychotherapy_permissions.sql
-- Psychotherapy Activity permissions
INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('psychotherapy-activity:create', 'Create Psychotherapy Activity', 'Register psychotherapy activities', 'psychotherapy-activity', 'create', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('psychotherapy-activity:read', 'Read Psychotherapy Activity', 'View psychotherapy activities', 'psychotherapy-activity', 'read', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('psychotherapy-activity:delete', 'Delete Psychotherapy Activity', 'Delete psychotherapy activities', 'psychotherapy-activity', 'delete', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Psychotherapy Category permissions
INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('psychotherapy-category:create', 'Create Psychotherapy Category', 'Create activity categories', 'psychotherapy-category', 'create', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('psychotherapy-category:read', 'Read Psychotherapy Category', 'View activity categories', 'psychotherapy-category', 'read', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('psychotherapy-category:update', 'Update Psychotherapy Category', 'Modify activity categories', 'psychotherapy-category', 'update', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('psychotherapy-category:delete', 'Delete Psychotherapy Category', 'Delete activity categories', 'psychotherapy-category', 'delete', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Assign ADMIN full access to both resources
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMIN' AND p.resource IN ('psychotherapy-activity', 'psychotherapy-category');

-- Assign PSYCHOLOGIST: create/read activities, read categories
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'PSYCHOLOGIST' AND p.code IN (
    'psychotherapy-activity:create', 'psychotherapy-activity:read',
    'psychotherapy-category:read'
);

-- Assign DOCTOR: read activities, read categories
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'DOCTOR' AND p.code IN (
    'psychotherapy-activity:read',
    'psychotherapy-category:read'
);

-- Assign NURSE: read activities, read categories
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'NURSE' AND p.code IN (
    'psychotherapy-activity:read',
    'psychotherapy-category:read'
);
```

### Index Requirements

- [x] `deleted_at` - Required for soft delete queries on all tables
- [x] `admission_id` - FK lookup for activity queries
- [x] `category_id` - FK lookup and filtering
- [x] `created_at` on activities - Sorting by date
- [x] `display_order` on categories - Ordering in dropdown
- [x] `active` on categories - Filtering active categories

---

## Frontend Changes

### Components

| Component | Location | Description |
|-----------|----------|-------------|
| `PsychotherapyActivityList.vue` | `src/components/psychotherapy/` | List of activities with sorting |
| `PsychotherapyActivityForm.vue` | `src/components/psychotherapy/` | Create activity form (category dropdown + description) |
| `PsychotherapyActivityCard.vue` | `src/components/psychotherapy/` | Single activity display card |
| `PsychotherapyCategoryList.vue` | `src/views/admin/` | Admin CRUD list for categories |
| `PsychotherapyCategoryForm.vue` | `src/components/admin/` | Admin create/edit category form |

### Pinia Stores

| Store | Location | Description |
|-------|----------|-------------|
| `usePsychotherapyActivityStore` | `src/stores/psychotherapyActivity.ts` | Activity CRUD operations |
| `usePsychotherapyCategoryStore` | `src/stores/psychotherapyCategory.ts` | Category CRUD operations |

### Routes

| Path | Component | Auth Required | Roles |
|------|-----------|---------------|-------|
| `/admin/psychotherapy-categories` | `PsychotherapyCategoryList` | Yes | ADMIN |

Note: Activities are displayed within the existing admission detail view (`/admissions/:id`) as a tab, similar to Progress Notes.

### Validation (Zod Schemas)

```typescript
// src/schemas/psychotherapyActivity.ts
import { z } from 'zod'

export const createPsychotherapyActivitySchema = z.object({
  categoryId: z.number({
    required_error: 'validation.categoryRequired',
  }).positive(),
  description: z
    .string({
      required_error: 'validation.descriptionRequired',
    })
    .min(1, 'validation.descriptionRequired')
    .max(2000, 'validation.descriptionMaxLength'),
})

// src/schemas/psychotherapyCategory.ts
export const psychotherapyCategorySchema = z.object({
  name: z
    .string()
    .min(1, 'validation.nameRequired')
    .max(100, 'validation.nameMaxLength'),
  description: z.string().max(255).optional(),
  displayOrder: z.number().int().default(0),
  active: z.boolean().default(true),
})
```

### i18n Keys (English/Spanish)

```json
// en.json
{
  "psychotherapy": {
    "activities": "Psychotherapeutic Activities",
    "activity": "Activity",
    "category": "Category",
    "description": "Description",
    "registeredBy": "Registered by",
    "registeredAt": "Registered at",
    "addActivity": "Add Activity",
    "noActivities": "No activities registered yet",
    "sortNewest": "Newest first",
    "sortOldest": "Oldest first",
    "categories": "Activity Categories",
    "addCategory": "Add Category",
    "editCategory": "Edit Category",
    "categoryName": "Category Name",
    "displayOrder": "Display Order",
    "active": "Active"
  },
  "validation": {
    "categoryRequired": "Category is required",
    "descriptionRequired": "Description is required",
    "descriptionMaxLength": "Description must not exceed 2000 characters"
  },
  "errors": {
    "noPermissionCreateActivity": "You do not have permission to register activities. Only psychologists can perform this action.",
    "noPermissionDeleteActivity": "You do not have permission to delete activities. Please contact an administrator.",
    "activityNotAllowedForOutpatient": "Psychotherapeutic activities can only be registered for hospitalized patients.",
    "categoryInUse": "Cannot delete category that is in use by existing activities."
  },
  "confirm": {
    "deleteActivity": "Are you sure you want to delete this activity?"
  }
}

// es.json
{
  "psychotherapy": {
    "activities": "Actividades Psicoterapéuticas",
    "activity": "Actividad",
    "category": "Categoría",
    "description": "Descripción",
    "registeredBy": "Registrado por",
    "registeredAt": "Registrado el",
    "addActivity": "Agregar Actividad",
    "noActivities": "No hay actividades registradas aún",
    "sortNewest": "Más recientes primero",
    "sortOldest": "Más antiguas primero",
    "categories": "Categorías de Actividades",
    "addCategory": "Agregar Categoría",
    "editCategory": "Editar Categoría",
    "categoryName": "Nombre de Categoría",
    "displayOrder": "Orden de Visualización",
    "active": "Activo"
  },
  "validation": {
    "categoryRequired": "La categoría es requerida",
    "descriptionRequired": "La descripción es requerida",
    "descriptionMaxLength": "La descripción no debe exceder 2000 caracteres"
  },
  "errors": {
    "noPermissionCreateActivity": "No tiene permiso para registrar actividades. Solo los psicólogos pueden realizar esta acción.",
    "noPermissionDeleteActivity": "No tiene permiso para eliminar actividades. Por favor contacte a un administrador.",
    "activityNotAllowedForOutpatient": "Las actividades psicoterapéuticas solo pueden registrarse para pacientes hospitalizados.",
    "categoryInUse": "No se puede eliminar una categoría que está en uso por actividades existentes."
  },
  "confirm": {
    "deleteActivity": "¿Está seguro de que desea eliminar esta actividad?"
  }
}
```

---

## Implementation Notes

- **Pattern Reference**: Follow the same pattern as `TriageCode` and `Room` entities for the category admin management
- **Activity List Pattern**: Follow the same pattern as `ProgressNoteList` for displaying activities with sorting
- **Immutability**: No PUT/PATCH endpoint for activities - they cannot be edited after creation
- **Tab Integration**: Add "Actividades Psicoterapéuticas" tab to the admission detail view, conditionally shown only for HOSPITALIZACION admissions (UI prevents adding activities for outpatient)
- **Permission Checks**: Service layer must verify user has PSYCHOLOGIST role before allowing activity creation
- **Admission Type Validation**: Service layer must verify admission type is HOSPITALIZACION before allowing activity creation (returns 400 Bad Request if not)
- **Category Dropdown**: Use PrimeVue `Dropdown` component, populate with active categories ordered by `displayOrder`
- **Category Deletion Check**: Before deleting a category, check if any activities (including soft-deleted) reference it; if so, return 400 Bad Request (follow `TriageCodeService` pattern)
- **Delete Confirmation**: Use PrimeVue `useConfirm` for activity deletion confirmation modal (follow `TriageCodesView` pattern)
- **Admin Menu**: Add "Activity Categories" link to admin settings menu alongside Triage Codes and Rooms

---

## QA Checklist

### Backend
- [ ] All functional requirements implemented
- [ ] `PsychotherapyCategory` entity extends `BaseEntity`
- [ ] `PsychotherapyActivity` entity extends `BaseEntity`
- [ ] Both entities have `@SQLRestriction("deleted_at IS NULL")`
- [ ] DTOs used in controllers (no entity exposure)
- [ ] Input validation in place (description required, max 2000 chars)
- [ ] Permission checks: only PSYCHOLOGIST can create activities
- [ ] Permission checks: only ADMIN can delete activities
- [ ] Permission checks: only ADMIN can manage categories
- [ ] Business rule: activities can only be created for HOSPITALIZACION admissions
- [ ] Business rule: categories in use by activities cannot be deleted
- [ ] Activities cannot be edited (no PUT endpoint)
- [ ] Audit trail (createdBy) properly tracked
- [ ] Unit tests written and passing
- [ ] Integration tests written and passing (Testcontainers)
- [ ] Detekt passes (no new violations)
- [ ] OWASP dependency-check passes

### Frontend
- [ ] PsychotherapyActivityList component displays activities
- [ ] PsychotherapyActivityForm with category dropdown and description
- [ ] Sort toggle (newest/oldest) working
- [ ] Activity tab only shown for HOSPITALIZACION admissions
- [ ] Add Activity button only shown for PSYCHOLOGIST role
- [ ] Delete button only shown for ADMIN role
- [ ] Delete confirmation modal displayed before deleting activity
- [ ] PsychotherapyCategoryList admin page working
- [ ] PsychotherapyCategoryForm create/edit working
- [ ] Pinia stores implemented
- [ ] Form validation with VeeValidate + Zod
- [ ] Error handling with localized messages
- [ ] ESLint/oxlint passes
- [ ] i18n keys added for all user-facing text (EN/ES)
- [ ] Unit tests written and passing (Vitest)

### E2E Tests (Playwright)
- [ ] Psychologist creates activity successfully
- [ ] Doctor cannot create activity (permission denied)
- [ ] Nurse cannot create activity (permission denied)
- [ ] Admin cannot create activity (permission denied)
- [ ] All clinical staff can view activities
- [ ] Activity sorting (newest/oldest) works
- [ ] Admin deletes activity successfully (with confirmation modal)
- [ ] Psychologist cannot delete activity (permission denied)
- [ ] Admin creates/edits category
- [ ] Admin cannot delete category in use (error displayed)
- [ ] Activity tab hidden for AMBULATORIO admissions
- [ ] Form validation errors displayed correctly (EN/ES)
- [ ] Permission denied messages displayed correctly (EN/ES)

### General
- [ ] API contract documented
- [ ] Database migrations tested
- [ ] Feature documentation updated
- [ ] Reviewed by project owner

---

## Documentation Updates Required

### Must Update

- [ ] **[CLAUDE.md](../../CLAUDE.md)**
  - Add Psychotherapeutic Activities to "Implemented Features" section
  - Update migration count (V040-V042)

### Review for Consistency

- [ ] **[ARCHITECTURE.md](../architecture/ARCHITECTURE.md)**
  - Add new entities to entity diagram if exists

### Code Documentation

- [ ] **`PsychotherapyCategory.kt`** - Document admin-managed lookup table
- [ ] **`PsychotherapyActivity.kt`** - Document immutability and psychologist-only creation
- [ ] **`PsychotherapyActivityService.kt`** - Document permission checks

---

## Related Docs/Commits/Issues

- Related feature: [Patient Admission](./patient-admission.md)
- Related feature: [Medical/Psychiatric Record](./medical-psychiatric-record.md) (similar tab pattern)
- Related entity: `Admission` (parent entity)
- Similar pattern: `TriageCode`, `Room` (admin-managed lookup tables)
- Similar pattern: `ProgressNote` (list with sorting in admission details)

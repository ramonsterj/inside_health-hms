# Feature: [Feature Name]

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | YYYY-MM-DD | @username | Initial draft |

---

## Overview

Describe, in 1–3 sentences, what this feature is about and why it exists.

---

## Use Case / User Story

> *As a [role/user], I want to [do something] so that [desired outcome].*

Summarize one or more user stories or concrete use cases for this feature.

---

## Authorization / Role Access

| Action | Required Role(s) | Permission | Notes |
|--------|------------------|------------|-------|
| View resource | USER, ADMIN | `resource:read` | All authenticated users |
| Create resource | ADMIN | `resource:write` | |
| Update resource | ADMIN | `resource:write` | |
| Delete resource | ADMIN | `resource:delete` | Soft delete only |

---

## Functional Requirements

- Bullet-list the requirements this feature must fulfill.
- Be specific about what is included and left out.
- E.g. "The API must accept pagination parameters (page, size) with defaults of 0 and 20."

---

## Acceptance Criteria / Scenarios

- List concrete scenarios that must be tested for this feature to be considered complete.
- E.g. "When a user with ADMIN role creates a new resource with valid data, the system returns 201 Created with the resource ID."
- E.g. "When a user without required permissions attempts to delete, the system returns 403 Forbidden."

---

## Non-Functional Requirements (Optional)

- Performance: e.g., "API response time < 200ms for list endpoints"
- Security: e.g., "All inputs must be validated; no raw SQL queries"
- Reliability: e.g., "Failed operations must be logged to audit_logs"

---

## API Contract

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| GET | `/api/v1/resources` | - | `PagedResponse<ResourceResponse>` | Yes | List all resources |
| GET | `/api/v1/resources/{id}` | - | `ResourceResponse` | Yes | Get resource by ID |
| POST | `/api/v1/resources` | `CreateResourceRequest` | `ResourceResponse` | Yes | Create new resource |
| PUT | `/api/v1/resources/{id}` | `UpdateResourceRequest` | `ResourceResponse` | Yes | Update resource |
| DELETE | `/api/v1/resources/{id}` | - | - | Yes | Soft delete resource |

### Request/Response Examples (Optional)

```json
// POST /api/v1/resources - Request
{
  "name": "Example",
  "description": "Description here"
}

// Response
{
  "id": 1,
  "name": "Example",
  "description": "Description here",
  "createdAt": "2026-01-20T10:00:00Z"
}
```

---

## Database Changes

### New Entities

| Entity | Table | Extends | Description |
|--------|-------|---------|-------------|
| `Resource` | `resources` | `BaseEntity` | Main resource entity |

### New Migrations

| Migration | Description |
|-----------|-------------|
| `V0XX__create_resources_table.sql` | Creates resources table with all BaseEntity fields |

### Schema Example

```sql
CREATE TABLE resources (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_resources_deleted_at ON resources(deleted_at);
-- Add other indexes as needed
```

### Index Requirements

- [ ] `deleted_at` - Required for soft delete queries
- [ ] Foreign keys - If referencing other tables
- [ ] Frequently queried columns - Based on expected access patterns

---

## Frontend Changes

### Components

| Component | Location | Description |
|-----------|----------|-------------|
| `ResourceList.vue` | `src/views/resources/` | List view with pagination |
| `ResourceForm.vue` | `src/components/resources/` | Create/Edit form |

### Pinia Stores

| Store | Location | Description |
|-------|----------|-------------|
| `useResourceStore` | `src/stores/resource.ts` | State management for resources |

### Routes

| Path | Component | Auth Required | Roles |
|------|-----------|---------------|-------|
| `/resources` | `ResourceList` | Yes | USER, ADMIN |
| `/resources/new` | `ResourceForm` | Yes | ADMIN |
| `/resources/:id/edit` | `ResourceForm` | Yes | ADMIN |

### Validation (Zod Schemas)

```typescript
// src/schemas/resource.ts
export const createResourceSchema = z.object({
  name: z.string().min(1).max(255),
  description: z.string().optional(),
});
```

---

## Implementation Notes

- Optional tips, background, architectural context, or links to relevant discussions.
- E.g. "This feature follows the existing pattern in UserController/UserService."
- E.g. "Consider using @Cacheable for frequently accessed resources."

---

## QA Checklist

### Backend
- [ ] All functional requirements implemented
- [ ] Entity extends `BaseEntity`
- [ ] Entity has `@SQLRestriction("deleted_at IS NULL")`
- [ ] DTOs used in controller (no entity exposure)
- [ ] Input validation in place
- [ ] Unit tests written and passing
- [ ] Integration tests written and passing (Testcontainers)
- [ ] Detekt passes (no new violations)
- [ ] OWASP dependency-check passes

### Frontend
- [ ] Components created and functional
- [ ] Pinia store implemented
- [ ] Routes configured with proper guards
- [ ] Form validation with VeeValidate + Zod
- [ ] Error handling implemented
- [ ] ESLint/oxlint passes
- [ ] i18n keys added for all user-facing text

### General
- [ ] API contract documented
- [ ] Database migration tested
- [ ] Feature documentation updated
- [ ] Reviewed by project owner

---

## Documentation Updates Required

### Must Update

- [ ] **[CLAUDE.md](../../CLAUDE.md)**
  - Add new feature to "Implemented Features" if applicable
- [ ] **[ARCHITECTURE.md](../../ARCHITECTURE.md)** (if exists)
  - Update if architectural patterns changed

### Review for Consistency

- [ ] **[README.md](../../README.md)**
  - Check if setup instructions need updates

### Code Documentation

- [ ] **`src/main/kotlin/.../entity/Resource.kt`**
  - Add KDoc comments for complex logic
- [ ] **`src/main/kotlin/.../service/ResourceService.kt`**
  - Document public methods

---

## Related Docs/Commits/Issues

- Related feature: [Link to related feature doc]
- GitHub Issue: [#XXX](link)
- Design discussion: [Link to Claude session or other discussion]

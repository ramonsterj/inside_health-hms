# Base Application Template - Architecture Documentation

## Overview

This document describes the architecture for a reusable application template designed to accelerate development of business applications. The template provides a solid foundation with authentication, authorization, audit logging, and common features needed across multiple client projects.

## Root Project Structure

```
kotlin-spring-template/
├── api/                    # Spring Boot 4 Kotlin REST API (backend)
├── web/                    # Vue 3 TypeScript SPA (frontend)
├── docker-compose.yml      # Docker services (PostgreSQL, Redis)
├── ARCHITECTURE.md         # Complete architecture documentation (this file)
├── CLAUDE.md               # Implementation rules and essential guidelines
├── VERSION_UPDATES.md      # Version decisions and rationale
└── README.md               # Quick start guide
```

**Directory Purposes:**
- **`api/`** - Backend application (Spring Boot, Kotlin, PostgreSQL)
- **`web/`** - Frontend application (Vue 3, TypeScript, Vite)
- **`docker-compose.yml`** - Local development environment setup
- **Documentation files** - Architecture, implementation rules, and version decisions

## Project Goals

- **Reusability**: Single template for multiple client projects
- **Security**: Built-in authentication, authorization, and audit logging
- **Maintainability**: Clean architecture with clear separation of concerns
- **Developer Experience**: Type-safe, easy to understand and extend
- **Deployment**: Simple, containerized deployments per client

## Current Implementation Summary

This section documents what is currently implemented in the template.

### Backend Entities

All entities inherit from `BaseEntity` (providing `id`, `createdAt`, `updatedAt`, `createdBy`, `updatedBy`, `deletedAt`) and use `@SQLRestriction("deleted_at IS NULL")` for automatic soft-delete filtering.

| Entity | Table | Key Fields | Relationships |
|--------|-------|------------|---------------|
| **User** | `users` | `username`, `email`, `passwordHash`, `firstName`, `lastName`, `status`, `emailVerified`, `localePreference` | ManyToMany → Role |
| **Role** | `roles` | `code`, `name`, `description`, `isSystem` | ManyToMany → Permission, User |
| **Permission** | `permissions` | `code`, `name`, `description`, `resource`, `action` | ManyToMany → Role |
| **RefreshToken** | `refresh_tokens` | `token`, `expiresAt` | ManyToOne → User |
| **PasswordResetToken** | `password_reset_tokens` | `token`, `expiresAt` | ManyToOne → User |
| **AuditLog** | `audit_logs` | `userId`, `username`, `action`, `entityType`, `entityId`, `oldValues` (JSONB), `newValues` (JSONB), `ipAddress`, `timestamp` | None (standalone) |

### Data Flow: Backend to Frontend

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  BACKEND (api/)                                                             │
│                                                                             │
│  JPA Entity  ──▶  Service Layer  ──▶  Response DTO  ──▶  REST Controller   │
│  (User.kt)       (UserService)       (UserResponse)     (JSON output)       │
│                                                                             │
│  • Entity: Full database model with relationships                           │
│  • Service: Business logic, validation, entity manipulation                 │
│  • DTO: Safe output shape (hides passwordHash, internal fields)             │
│  • Controller: Wraps in ApiResponse<T>, sets HTTP status                    │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼ HTTP Response (JSON + JWT in header)
┌─────────────────────────────────────────────────────────────────────────────┐
│  FRONTEND (web/)                                                            │
│                                                                             │
│  Axios Service  ──▶  Pinia Store  ──▶  TypeScript Type  ──▶  Vue Component │
│  (api.ts)           (auth.ts)         (User interface)      (reactive UI)   │
│                                                                             │
│  • Axios: Adds Bearer token, handles 401 refresh, unwraps ApiResponse       │
│  • Pinia: State management, caches data, exposes actions                    │
│  • Types: Mirrors backend DTOs for compile-time safety                      │
│  • Vue: Reactive rendering from store state                                 │
└─────────────────────────────────────────────────────────────────────────────┘
```

### DTO ↔ TypeScript Type Mapping

Backend DTOs (Kotlin) map directly to frontend types (TypeScript):

| Backend DTO | Frontend Type | Location |
|-------------|---------------|----------|
| `LoginRequest` | `LoginRequest` | `web/src/types/auth.ts` |
| `RegisterRequest` | `RegisterRequest` | `web/src/types/auth.ts` |
| `AuthResponse` | `AuthResponse` | `web/src/types/auth.ts` |
| `UserResponse` | `User` | `web/src/types/user.ts` |
| `UpdateUserRequest` | `UpdateUserRequest` | `web/src/types/user.ts` |
| `AdminUpdateUserRequest` | `AdminUpdateUserRequest` | `web/src/types/user.ts` |
| `CreateUserRequest` | `CreateUserRequest` | `web/src/types/user.ts` |
| `ChangePasswordRequest` | `ChangePasswordRequest` | `web/src/types/user.ts` |
| `RoleResponse` | `Role` | `web/src/types/role.ts` |
| `PermissionResponse` | `Permission` | `web/src/types/role.ts` |
| `AuditLogResponse` | `AuditLog` | `web/src/types/audit.ts` |
| `ApiResponse<T>` | `ApiResponse<T>` | `web/src/types/api.ts` |
| `ErrorResponse` | `ErrorResponse` | `web/src/types/api.ts` |
| `PageResponse<T>` | `PageResponse<T>` | `web/src/types/api.ts` |

### Pinia Stores

| Store | State | Key Methods |
|-------|-------|-------------|
| **auth** (`stores/auth.ts`) | `user`, `loading` | `login()`, `register()`, `logout()`, `refreshToken()`, `fetchCurrentUser()` |
| **user** (`stores/user.ts`) | `users`, `deletedUsers`, `loading` | `fetchUsers()`, `createUser()`, `updateUser()`, `deleteUser()`, `restoreUser()`, `resetUserPassword()` |
| **audit** (`stores/audit.ts`) | `logs`, `filters`, `loading` | `fetchLogs()`, `fetchLogsForEntity()`, `setFilters()` |

### API Endpoints Summary

**Authentication** (`/api/auth`):
| Method | Endpoint | Request DTO | Response |
|--------|----------|-------------|----------|
| POST | `/register` | `RegisterRequest` | `ApiResponse<AuthResponse>` |
| POST | `/login` | `LoginRequest` | `ApiResponse<AuthResponse>` |
| POST | `/refresh` | `RefreshTokenRequest` | `ApiResponse<AuthResponse>` |
| POST | `/logout` | - | `ApiResponse<Unit>` |
| POST | `/forgot-password` | `ForgotPasswordRequest` | `ApiResponse<Unit>` |
| POST | `/reset-password` | `ResetPasswordRequest` | `ApiResponse<Unit>` |
| GET | `/check-username` | `?username=` | `ApiResponse<UsernameAvailabilityResponse>` |

**Current User** (`/api/users/me`):
| Method | Endpoint | Request DTO | Response |
|--------|----------|-------------|----------|
| GET | `/me` | - | `ApiResponse<UserResponse>` |
| PUT | `/me` | `UpdateUserRequest` | `ApiResponse<UserResponse>` |
| PUT | `/me/password` | `ChangePasswordRequest` | `ApiResponse<Unit>` |
| PUT | `/me/locale` | `?locale=` | `ApiResponse<Unit>` |

**User Management** (`/api/users`) - requires permissions:
| Method | Endpoint | Permission | Request DTO | Response |
|--------|----------|------------|-------------|----------|
| POST | `/` | `user:create` | `CreateUserRequest` | `ApiResponse<UserResponse>` |
| GET | `/` | `user:read` | `?status=&page=&size=` | `ApiResponse<Page<UserResponse>>` |
| GET | `/{id}` | `user:read` | - | `ApiResponse<UserResponse>` |
| PUT | `/{id}` | `user:update` | `AdminUpdateUserRequest` | `ApiResponse<UserResponse>` |
| DELETE | `/{id}` | `user:delete` | - | `ApiResponse<Unit>` |
| POST | `/{id}/reset-password` | `user:reset-password` | - | `ApiResponse<{temporaryPassword}>` |
| GET | `/deleted` | `user:list-deleted` | `?page=&size=` | `ApiResponse<Page<UserResponse>>` |
| POST | `/{id}/restore` | `user:restore` | - | `ApiResponse<UserResponse>` |
| PUT | `/{id}/roles` | `user:update` | `AssignRolesRequest` | `ApiResponse<UserResponse>` |

**Role Management** (`/api/roles`) - requires permissions:
| Method | Endpoint | Permission | Request DTO | Response |
|--------|----------|------------|-------------|----------|
| GET | `/` | `role:read` | - | `ApiResponse<List<RoleResponse>>` |
| GET | `/{id}` | `role:read` | - | `ApiResponse<RoleResponse>` |
| POST | `/` | `role:create` | `CreateRoleRequest` | `ApiResponse<RoleResponse>` |
| PUT | `/{id}` | `role:update` | `UpdateRoleRequest` | `ApiResponse<RoleResponse>` |
| DELETE | `/{id}` | `role:delete` | - | `ApiResponse<Unit>` |
| PUT | `/{id}/permissions` | `role:assign-permissions` | `AssignPermissionsRequest` | `ApiResponse<RoleResponse>` |
| GET | `/permissions` | `role:read` | - | `ApiResponse<List<PermissionResponse>>` |

**Audit Logs** (`/api/audit-logs`) - requires `audit:read`:
| Method | Endpoint | Request | Response |
|--------|----------|---------|----------|
| GET | `/` | `?userId=&entityType=&action=&page=&size=` | `ApiResponse<Page<AuditLogResponse>>` |
| GET | `/entity` | `?entityType=&entityId=&page=&size=` | `ApiResponse<Page<AuditLogResponse>>` |

## Technology Stack

### Backend
- **Language**: Kotlin 2.3+
- **Framework**: Spring Boot 4.x (built on Spring Framework 7)
- **Database**: PostgreSQL 17+
- **ORM**: JPA/Hibernate (Spring Data JPA)
- **Build Tool**: Gradle (Kotlin DSL)
- **Authentication**: JWT access tokens + Refresh tokens
- **Migration**: Flyway 11.20+

### Frontend
- **Framework**: Vue 3.5+ (Composition API)
- **Language**: TypeScript 5.9+
- **Build Tool**: Vite 7.3+
- **State Management**: Pinia 3.0+
- **Routing**: Vue Router 4.5+
- **HTTP Client**: Axios 1.7+
- **UI Components**: PrimeVue 4.5+

### Infrastructure
- **Database**: PostgreSQL 17+
- **Caching**: Consider adding Redis if your application requires caching or session storage
- **Containerization**: Docker + Docker Compose
- **Deployment**: Uncloud - Lightweight container orchestration
- **Reverse Proxy**: Caddy (via Uncloud)
- **CI/CD**: GitHub Actions (or equivalent)

## Architecture Principles

### Backend Architecture

**Layered Architecture**:
```
┌─────────────────────────────────┐
│     Presentation Layer          │  Controllers, DTOs, Request/Response
├─────────────────────────────────┤
│     Service Layer               │  Business Logic, Validation
├─────────────────────────────────┤
│     Repository Layer            │  Spring Data JPA Repositories (Interfaces)
├─────────────────────────────────┤
│     Domain Layer                │  Entities, Value Objects
└─────────────────────────────────┘
```

**Package Structure**:
```
com.insidehealthgt.hms
├── config/                 # Spring configuration
├── controller/             # REST controllers
├── dto/                    # Data Transfer Objects
│   ├── request/
│   └── response/
├── entity/                 # Database entities
├── repository/             # Spring Data JPA repository interfaces
├── service/                # Business logic
├── security/               # Auth, JWT, filters
├── exception/              # Custom exceptions, handlers
├── util/                   # Utilities, helpers
└── audit/                  # Audit logging
```

### Frontend Architecture

**Component-Based Architecture**:
```
src/
├── assets/                 # Images, fonts, static files
├── components/             # Reusable components
│   ├── common/            # Buttons, inputs, modals
│   ├── layout/            # Navbar, sidebar, footer
│   └── forms/             # Form components
├── views/                  # Page components
├── router/                 # Route definitions
├── stores/                 # Pinia stores
│   ├── auth.ts
│   ├── user.ts
│   └── notification.ts
├── services/               # API services
│   └── api.ts
├── composables/            # Reusable composition functions
├── types/                  # TypeScript types/interfaces
└── utils/                  # Utilities, helpers
```

**Frontend Dependencies (`package.json`)**:
```json
{
  "name": "kotlin-spring-template-frontend",
  "version": "1.0.0",
  "type": "module",
  "dependencies": {
    "vue": "^3.5.24",
    "vue-router": "^4.6.4",
    "pinia": "^3.0.4",
    "primevue": "^4.5.4",
    "axios": "^1.13.2",
    "primeicons": "^7.0.0",
    "vue-i18n": "^10.0.8",
    "vee-validate": "^4.15.1",
    "@vee-validate/zod": "^4.15.1",
    "zod": "^3.25.76"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^6.0.1",
    "vite": "^7.2.4",
    "typescript": "~5.9.3",
    "vue-tsc": "^3.1.4",
    "@types/node": "^24.10.9",
    "vitest": "^4.0.17",
    "happy-dom": "^20.3.4",
    "@vue/test-utils": "^2.4.6",
    "@pinia/testing": "^1.0.3",
    "@playwright/test": "^1.57.0"
  }
}
```

**Key Library Choices**:
- **Vue 3.5+**: Latest stable with improved reactivity and performance
- **Vite 7.3+**: Lightning-fast dev server and optimized builds
- **TypeScript 5.9+**: Latest type safety features
- **Pinia 3.0+**: Official Vue state management (replaced Vuex)
- **PrimeVue 4.5+**: 80+ enterprise-grade UI components
- **Axios 1.7+**: HTTP client with interceptors for auth tokens

## Core Features

### 1. Authentication & Authorization

**Authentication Flow** (Dual-Token System):
1. User submits credentials (email OR username + password)
2. Backend validates and returns:
   - **Access token** (JWT) - Short-lived (15-30 min), used for API requests
   - **Refresh token** - Long-lived (7-30 days), stored in database
3. Frontend stores both tokens (localStorage/sessionStorage or httpOnly cookies)
4. Access token included in all API requests (Authorization header)
5. When access token expires, use refresh token to get new access token
6. Refresh tokens can be revoked (logout, security breach)

**Authorization** (Role-Based Access Control with Fine-Grained Permissions):
- **Database-driven roles and permissions** - manageable without code changes
- **Multiple roles per user** - users can have many roles (e.g., ADMIN + EDITOR)
- **Resource-action permissions** - fine-grained access control (e.g., `user:create`, `role:delete`)
- **Default roles**: ADMIN (all permissions), USER (minimal permissions)
- **System roles**: ADMIN and USER cannot be deleted
- **Spring Security integration**: `@PreAuthorize("hasAuthority('permission:code')")`
- Frontend route guards based on roles/permissions
- See "Role & Permission System" section for full details

**Endpoints**:
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - Login
- `POST /api/auth/logout` - Logout
- `POST /api/auth/refresh` - Refresh token
- `POST /api/auth/forgot-password` - Password reset request
- `POST /api/auth/reset-password` - Password reset

### 2. User Management

**Features**:
- Full CRUD operations for users (admin)
- Profile management (self)
- Password change (self) and reset (admin)
- Email verification (optional)
- Role assignment (admin only)
- Soft delete and restore functionality
- List deleted users (admin)

**Current User Endpoints** (authenticated users):
- `GET /api/users/me` - Get current user profile
- `PUT /api/users/me` - Update current user profile
- `PUT /api/users/me/password` - Change own password

**Admin User Management Endpoints** (requires permissions):
- `POST /api/users` - Create user (`user:create`)
- `GET /api/users` - List users (`user:read`)
- `GET /api/users/{id}` - Get user (`user:read`)
- `PUT /api/users/{id}` - Update user (`user:update`)
- `DELETE /api/users/{id}` - Soft delete user (`user:delete`)
- `POST /api/users/{id}/reset-password` - Reset user password (`user:reset-password`)
- `GET /api/users/deleted` - List soft-deleted users (`user:list-deleted`)
- `POST /api/users/{id}/restore` - Restore deleted user (`user:restore`)
- `PUT /api/users/{id}/roles` - Assign roles to user (`user:update`)

**Soft Delete Strategy**:
- All entity deletions are soft deletes by default (via `deleted_at` in BaseEntity)
- Soft-deleted records are excluded from queries automatically (use `@Where(clause = "deleted_at IS NULL")`)
- Hard deletes should only be done for compliance/GDPR requirements

### 3. Role & Permission System

The application uses a **database-driven role and permission system** with fine-grained access control.

**Architecture**:
- **Roles** are stored in the database and can be managed via API
- **Permissions** follow a `resource:action` naming convention (e.g., `user:create`, `role:delete`)
- **Users can have multiple roles** (many-to-many relationship)
- **Roles have multiple permissions** (many-to-many relationship)
- **System roles** (ADMIN, USER) cannot be deleted but can have permissions modified

**Default Permissions** (seeded via migration):
| Permission | Description |
|------------|-------------|
| `user:create` | Create new user accounts |
| `user:read` | View user details |
| `user:update` | Modify user information |
| `user:delete` | Soft delete user accounts |
| `user:reset-password` | Reset password for any user |
| `user:list-deleted` | View soft-deleted users |
| `user:restore` | Restore soft-deleted users |
| `role:create` | Create new roles |
| `role:read` | View role details |
| `role:update` | Modify role information |
| `role:delete` | Delete non-system roles |
| `role:assign-permissions` | Modify role permissions |
| `audit:read` | View audit log entries |

**Default Roles**:
- **ADMIN**: System role with all permissions
- **USER**: System role with minimal permissions (`user:read` only)

**Role Management Endpoints** (requires permissions):
- `GET /api/roles` - List all roles (`role:read`)
- `GET /api/roles/{id}` - Get role details (`role:read`)
- `POST /api/roles` - Create new role (`role:create`)
- `PUT /api/roles/{id}` - Update role (`role:update`)
- `DELETE /api/roles/{id}` - Delete role (`role:delete`)
- `PUT /api/roles/{id}/permissions` - Assign permissions to role (`role:assign-permissions`)
- `GET /api/roles/permissions` - List all available permissions (`role:read`)

**Spring Security Integration**:
```kotlin
// Permission-based access control
@PreAuthorize("hasAuthority('user:create')")
fun createUser(request: CreateUserRequest): UserResponse

// Role-based access control (still supported)
@PreAuthorize("hasRole('ADMIN')")
fun adminOnlyEndpoint(): Response

// Combined checks
@PreAuthorize("hasAuthority('user:update') or hasRole('ADMIN')")
fun updateUser(id: Long, request: UpdateUserRequest): UserResponse
```

**CustomUserDetails** returns both roles and permissions as authorities:
- Role authorities: `ROLE_ADMIN`, `ROLE_USER`
- Permission authorities: `user:create`, `role:delete`, etc.

### 4. Audit Logging

**What to Track**:
- Who: User ID and username
- What: Action performed (CREATE, UPDATE, DELETE, etc.)
- When: Timestamp
- Where: Entity type and ID
- Details: Before/after values for updates

**Implementation**:
- Database table: `audit_log`
- Automatic tracking via **JPA Entity Listeners** (`@EntityListeners`)
- Use `@PrePersist`, `@PreUpdate`, `@PreRemove` lifecycle callbacks
- Capture user context from Spring Security
- Queryable for compliance and debugging

**Schema**:
```sql
CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(255),
    action VARCHAR(50),
    entity_type VARCHAR(255),
    entity_id BIGINT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    old_value JSONB,
    new_value JSONB,
    ip_address VARCHAR(45)
);
```

### 4. Base Entities

All entities inherit common fields from BaseEntity:

```kotlin
@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null
    
    @CreatedBy
    @Column(name = "created_by")
    var createdBy: Long? = null
    
    @LastModifiedBy
    @Column(name = "updated_by")
    var updatedBy: Long? = null
    
    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null  // Soft delete
}
```

**Configuration Required**:
Enable JPA auditing in your Spring Boot application:
```kotlin
@Configuration
@EnableJpaAuditing
class JpaConfig {
    @Bean
    fun auditorAware(): AuditorAware<Long> {
        return AuditorAware {
            // Get current user ID from Spring Security context
            val authentication = SecurityContextHolder.getContext().authentication
            if (authentication != null && authentication.isAuthenticated) {
                // Extract user ID from authentication principal
                Optional.of((authentication.principal as UserDetails).id)
            } else {
                Optional.empty()
            }
        }
    }
}
```

**How It Works**:
- `@CreatedDate` and `@LastModifiedDate` are automatically set by Spring Data JPA
- `@CreatedBy` and `@LastModifiedBy` are set from the `AuditorAware` bean (uses Spring Security)
- Timestamps are set at persistence time, not object creation time
- All entities extending BaseEntity get these fields automatically
```

### 5. API Design Patterns

**Consistent Response Format**:
```json
{
  "success": true,
  "data": { ... },
  "message": "Operation successful",
  "timestamp": "2025-01-14T10:30:00Z"
}
```

**Error Response Format**:
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid input",
    "details": {
      "email": ["Email is required"],
      "password": ["Password must be at least 8 characters"]
    }
  },
  "timestamp": "2025-01-14T10:30:00Z"
}
```

**Pagination**:
- Query parameters: `page`, `size`, `sort`
- Response includes: `content`, `totalElements`, `totalPages`, `currentPage`

**Filtering**:
- Query parameters for common filters
- Example: `/api/users?role=ADMIN&status=ACTIVE`

### 6. Exception Handling

**Global Exception Handler**:
- Catches all exceptions
- Returns consistent error responses
- Logs errors appropriately
- HTTP status codes mapped to error types

**Common Exceptions**:
- `ResourceNotFoundException` → 404
- `ValidationException` → 400
- `UnauthorizedException` → 401
- `ForbiddenException` → 403
- `ConflictException` → 409

### 7. Validation

**Backend Validation**:
- Jakarta Bean Validation annotations
- Custom validators for business rules
- DTO-level validation

**Frontend Validation** (VeeValidate + Zod):
- **VeeValidate 4.x**: Form state management with Vue 3 Composition API
- **Zod**: Type-safe schema validation that mirrors backend rules
- Real-time feedback on field blur and form submit

**Validation Schema Structure**:
```
web/src/
├── validation/
│   ├── index.ts           # Re-exports all schemas
│   └── schemas.ts         # Zod schemas mirroring backend DTOs
└── composables/
    └── useFormValidation.ts  # Reusable form validation composable
```

**Usage Example**:
```typescript
// schemas.ts - Define validation rules matching backend
import { z } from 'zod'

export const loginSchema = z.object({
  identifier: z.string().min(1, 'Email or username is required'),
  password: z.string().min(1, 'Password is required')
})

export type LoginFormData = z.infer<typeof loginSchema>

// Component usage
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import { loginSchema } from '@/validation'

const { defineField, handleSubmit, errors } = useForm({
  validationSchema: toTypedSchema(loginSchema),
  initialValues: { identifier: '', password: '' }
})

const [identifier] = defineField('identifier')
const [password] = defineField('password')

const onSubmit = handleSubmit(async (values) => {
  // values is fully typed
})
```

**Key Principles**:
- Keep frontend schemas in sync with backend Jakarta validation
- Use Zod's `.refine()` for cross-field validation (e.g., password confirmation)
- Leverage TypeScript inference (`z.infer<typeof schema>`) for type safety

### 8. Configuration Management

**Environment-Based Configuration**:
- `application.yml` - Default configuration
- `application-dev.yml` - Development
- `application-prod.yml` - Production
- Environment variables for sensitive data

**External Configuration**:
```yaml
# Database
spring.datasource.url=${DATABASE_URL}
spring.datasource.username=${DATABASE_USERNAME}
spring.datasource.password=${DATABASE_PASSWORD}

# JPA/Hibernate Configuration
spring.jpa.hibernate.ddl-auto=validate  # CRITICAL: Use 'validate' with Flyway, never 'update' or 'create'
spring.jpa.show-sql=false  # Set to true in development for debugging
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.open-in-view=false  # Recommended for performance

# Flyway Migration
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.locations=classpath:db/migration

# JWT
jwt.secret=${JWT_SECRET}
jwt.expiration=${JWT_EXPIRATION:3600000}

# Email (if needed)
spring.mail.host=${MAIL_HOST}
spring.mail.port=${MAIL_PORT}
```

**Important Notes**:
- **`ddl-auto=validate`**: Hibernate validates schema against entities but doesn't modify it. Flyway handles all schema changes.
- **Never use `ddl-auto=update` or `create`** with Flyway - this causes conflicts and unpredictable schema changes.
- In development, you can use `ddl-auto=create-drop` ONLY if you disable Flyway temporarily.

### 9. Kotlin-Specific Configuration

**Gradle Plugins for JPA/Hibernate**:

Kotlin requires specific compiler plugins to work seamlessly with JPA/Hibernate. Add these to your `build.gradle.kts`:

```kotlin
plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.spring") version "2.3.0"
    kotlin("plugin.jpa") version "2.3.0"  // Handles no-arg and all-open automatically
    id("org.springframework.boot") version "4.0.1"  // Latest stable (Dec 2025)
    id("io.spring.dependency-management") version "1.1.7"
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    
    runtimeOnly("org.postgresql:postgresql")
    
    // Flyway for migrations
    implementation("org.flywaydb:flyway-core:11.20.2")
    implementation("org.flywaydb:flyway-database-postgresql:11.20.2")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
}

// The kotlin-jpa plugin automatically:
// 1. Generates no-arg constructors for @Entity classes
// 2. Makes @Entity, @MappedSuperclass, and @Embeddable classes open (non-final)
// No additional configuration needed!
```

**Why Spring Boot 4.0.1 (not 3.5.9)?**

**For NEW projects, Spring Boot 4.0 is the right choice:**

**Timeline Argument (Strongest):**
- Released November 20, 2025 (currently 2 months old)
- By the time you reach production (6+ months), it will have **8+ months of real-world usage**
- Spring officially declares it GA (General Availability) and production-ready
- Breaking changes don't affect new projects - only migrations

**Technical Benefits:**
- **Performance**: Up to 2x better write throughput, 85% faster startup (8s → 1.2s)
- **Memory**: Up to 70% reduction in memory usage
- **GraalVM**: First-class native image support (stable)
- **Virtual Threads**: Deep integration with Java 21+ for higher concurrency
- **Modern Stack**: Built on Spring Framework 7 and Jakarta EE 11

**Avoiding Future Migration Pain:**
- If you start with 3.5.9, you'll need to migrate to 4.x eventually (2-4 weeks effort)
- Starting with 4.0 avoids this technical debt entirely
- 3.5.9 support ends June 2026 (only 5 months away)

**Breaking Changes Don't Apply to New Projects:**
- ✅ Jakarta EE 11 (`jakarta.*` packages) - use from day 1
- ✅ Jackson 3 - no legacy serializers to migrate
- ✅ JUnit 5 - modern testing from the start
- ✅ Undertow removed - use Tomcat/Jetty (standard)
- ✅ Strict MVC paths - follow modern patterns

**Why NOT 3.5.9?**
- Would require migration in 6-12 months anyway
- Older performance characteristics
- Limited support timeline (ends June 2026)
- Missing modern features (API versioning, HTTP service clients, etc.)

**Why Kotlin 2.3.0?**
- Released December 17, 2025 - stable and battle-tested
- **Fully compatible with Spring Boot 4.0** (Kotlin 2.2+ baseline)
- Improved type inference and compilation performance
- Better null safety with enhanced checks via JSpecify integration
- Stable features from previous beta releases
- Spring Boot 4.0 officially recommends Kotlin 2.2+ with new null-safety support

**Why PostgreSQL 17+?**
- Released September 2024 - production-ready and stable
- Significant performance improvements (up to 2x write throughput)
- Enhanced I/O layer and query optimization
- Improved logical replication for HA deployments
- Supported until 2029 (5-year support cycle)
- PostgreSQL 18+ is available but very new (Nov 2025)

**Why These Plugins Are Needed**:
- **kotlin-jpa plugin**: Automatically generates no-arg constructors AND makes JPA classes open
- Without this plugin, you'd get runtime errors due to Kotlin's final classes by default

**Entity Class Best Practices**:
```kotlin
@Entity
@Table(name = "users")
@SQLRestriction("deleted_at IS NULL")  // Hibernate 6+ (Spring Boot 4) - excludes soft-deleted users
class User(
    @Column(nullable = false, unique = true, length = 50)
    var username: String,  // Unique username for login (alternative to email)

    @Column(nullable = false, unique = true, length = 255)
    var email: String,

    @Column(name = "password_hash", nullable = false, length = 255)
    var passwordHash: String,

    @Column(name = "first_name", length = 100)
    var firstName: String? = null,

    @Column(name = "last_name", length = 100)
    var lastName: String? = null,

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    var role: UserRole = UserRole.USER,

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    var status: UserStatus = UserStatus.ACTIVE,

    @Column(name = "email_verified", nullable = false)
    var emailVerified: Boolean = false

    // Note: id, createdAt, updatedAt, etc. are inherited from BaseEntity
) : BaseEntity()

enum class UserRole {
    ADMIN, USER
}

enum class UserStatus {
    ACTIVE, INACTIVE, SUSPENDED
}

// Do NOT use data classes for JPA entities
// Regular classes work better with JPA/Hibernate
```

**Key Points**:
- **ID is inherited from BaseEntity** - don't redeclare it
- Always specify column lengths with `@Column(length = ...)`
- Use enums for fixed value sets (role, status)
- Nullable fields should be `String?` with default values
- Use `@Where` annotation to automatically exclude soft-deleted records
- Use `var` for mutable properties (entities need to be mutable)
- kotlin-jpa plugin automatically makes this class open (non-final)

## Database Schema

### Core Tables

**users**:
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,  -- Login with username or email
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    email_verified BOOLEAN DEFAULT FALSE,
    -- BaseEntity fields (inherited by all entities)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_deleted_at ON users(deleted_at);  -- For soft delete queries
```

**Default Admin User**:
A default admin user is created via migration:
- **Username**: `admin`
- **Email**: `admin@example.com`
- **Password**: `admin123`
- **Role**: `ADMIN`

> **Important**: Change the admin password immediately after first deployment!

**refresh_tokens**:
```sql
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(500) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);  -- For cleanup jobs
```

**audit_log**: (See Audit Logging section above)

**permissions**:
```sql
CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) UNIQUE NOT NULL,  -- e.g., 'user:create', 'role:delete'
    name VARCHAR(255) NOT NULL,
    description TEXT,
    resource VARCHAR(100) NOT NULL,     -- e.g., 'user', 'role', 'audit'
    action VARCHAR(100) NOT NULL,       -- e.g., 'create', 'read', 'update', 'delete'
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_permissions_code ON permissions(code);
CREATE INDEX idx_permissions_resource ON permissions(resource);
CREATE INDEX idx_permissions_deleted_at ON permissions(deleted_at);
```

**roles**:
```sql
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,   -- e.g., 'ADMIN', 'USER', 'EDITOR'
    name VARCHAR(255) NOT NULL,
    description TEXT,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,  -- System roles cannot be deleted
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_roles_code ON roles(code);
CREATE INDEX idx_roles_is_system ON roles(is_system);
CREATE INDEX idx_roles_deleted_at ON roles(deleted_at);
```

**role_permissions** (junction table):
```sql
CREATE TABLE role_permissions (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP,
    CONSTRAINT uk_role_permission UNIQUE (role_id, permission_id)
);

CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions(permission_id);
```

**user_roles** (junction table):
```sql
CREATE TABLE user_roles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP,
    CONSTRAINT uk_user_role UNIQUE (user_id, role_id)
);

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);
```

### Migration Strategy

- **Flyway** for version-controlled migrations
- Naming convention: `V{version}__{description}.sql`
- Example: `V001__create_users_table.sql`
- Never modify existing migrations
- Always create new migration for changes

## Security Considerations

### Backend Security

1. **Password Storage**: BCrypt hashing
2. **JWT Security**: 
   - Short-lived access tokens (15-30 min)
   - Longer-lived refresh tokens (7-30 days)
   - Token stored securely on frontend
3. **CORS**: Configured for allowed origins
4. **SQL Injection**: Prevented by using ORM/prepared statements
5. **XSS**: Input sanitization and output encoding
6. **CSRF**: Token-based protection for state-changing operations

### Frontend Security

1. **Token Storage**: localStorage or httpOnly cookies
2. **Route Guards**: Prevent unauthorized access
3. **API Error Handling**: Don't expose sensitive info in errors
4. **Input Sanitization**: Before sending to backend

## Testing Strategy

### Backend Testing

**Unit Tests**:
- Service layer logic
- Utility functions
- Custom validators

**Integration Tests**:
- Controller endpoints
- Repository queries
- Use Testcontainers for PostgreSQL
- **Requires Docker running locally**

**Test Structure**:
```kotlin
@SpringBootTest
@Testcontainers
class UserServiceIntegrationTest {
    @Container
    val postgres = PostgreSQLContainer<Nothing>("postgres:15")
    
    // Tests...
}
```

**Prerequisites for Integration Tests**:
- Docker must be installed and running
- Testcontainers will automatically start PostgreSQL container for tests
- Tests run against real PostgreSQL (not H2 or mocks)
```

### Frontend Testing

**Unit Tests (Vitest)**:
- Configuration: `web/vitest.config.ts`
- Test location: `web/src/**/*.{test,spec}.ts`
- Environment: happy-dom (lightweight DOM implementation)
- Coverage: v8 provider with text, JSON, and HTML reporters

```bash
npm test              # Watch mode
npm run test:run      # Single run
npm run test:coverage # With coverage report
```

**E2E Tests (Playwright)**:
- Configuration: `web/playwright.config.ts`
- Test location: `web/e2e/*.spec.ts`
- Browser: Chromium (Firefox/WebKit available for CI)
- Auto-starts dev server when running tests

```bash
npx playwright install chromium  # First time setup
npm run test:e2e                 # Run tests
npm run test:e2e:ui              # Interactive UI mode
npm run test:e2e:headed          # See browser
```

**Test Structure**:
```
web/
├── src/
│   ├── test/
│   │   └── setup.ts            # Vitest setup (localStorage mock)
│   ├── stores/
│   │   └── notification.spec.ts # Store unit tests
│   └── utils/
│       └── tokenStorage.spec.ts # Utility unit tests
└── e2e/
    ├── auth.spec.ts             # Authentication flow tests
    └── navigation.spec.ts       # Navigation tests
```

**Coverage Target**: Aim for 70%+ on business logic

## Deployment Architecture

### Deployment Platform: Uncloud

**Uncloud** is a lightweight tool for deploying and managing containerized applications across a network of Docker hosts. It bridges the gap between simple Docker Compose and complex Kubernetes, providing production-grade features without operational overhead.

**Why Uncloud?**
- Uses familiar Docker Compose syntax
- Automatic HTTPS via Caddy reverse proxy
- Zero-downtime deployments (blue-green strategy)
- WireGuard mesh networking for secure inter-service communication
- No central control plane to maintain
- Deploy on any Linux machine (VPS, bare metal, cloud)
- Free `*.uncld.dev` subdomains or use custom domains

**Key Features**:
- Decentralized architecture - all machines are equal
- Cross-machine container communication
- Built-in service discovery and DNS
- Automatic TLS certificate management (Let's Encrypt)
- Simple CLI (feels like Docker/Compose)

### Per-Client Deployment Model

Each client deployment consists of:
- **Separate Uncloud cluster** (or separate compose stack within a cluster)
- **Separate database** (PostgreSQL instance)
- **Isolated application containers** (backend, frontend, database)
- **Automatic subdomain** (e.g., `client1.your-cluster.uncld.dev` or custom domain)

**Benefits**:
- Complete data isolation between clients
- Independent scaling per client
- Custom configurations per client
- No multi-tenancy complexity
- Easy to add redundancy (multiple machines per client)

### Docker Compose Configuration

Uncloud extends the standard Docker Compose format with custom attributes (`x-ports`, `x-caddy`, etc.) for production deployment.

**compose.yml** (Development & Production):
```yaml
version: '3.8'

services:
  # PostgreSQL Database
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: ${DATABASE_NAME:-template_db}
      POSTGRES_USER: ${DATABASE_USER:-admin}
      POSTGRES_PASSWORD: ${DATABASE_PASSWORD:-password}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    # Internal service - no external ports exposed
    # Accessible via 'postgres:5432' from other containers
    deploy:
      replicas: 1
      # Optional: pin to specific machine for data locality
      # x-machines:
      #   - db-server

  # Redis Cache (Optional - can be removed if not needed)
  redis:
    image: redis:7-alpine
    volumes:
      - redis_data:/data
    deploy:
      replicas: 1
    # To remove Redis:
    # 1. Delete this entire redis service block
    # 2. Remove redis from backend depends_on
    # 3. Remove REDIS_URL from backend environment
    # 4. Remove redis_data from volumes section

  # Backend API
  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/${DATABASE_NAME:-template_db}
      DATABASE_USERNAME: ${DATABASE_USER:-admin}
      DATABASE_PASSWORD: ${DATABASE_PASSWORD:-password}
      JWT_SECRET: ${JWT_SECRET}
      REDIS_URL: redis://redis:6379
      SPRING_PROFILES_ACTIVE: ${ENVIRONMENT:-prod}
    depends_on:
      - postgres
      - redis
    # Uncloud-specific: expose service publicly via Caddy
    x-ports:
      - api.${DOMAIN:-uncld.dev}:8080/https
    deploy:
      replicas: 2  # Run 2 instances for high availability
      # Optional: custom health check
      # healthcheck:
      #   test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      #   interval: 30s
      #   timeout: 10s
      #   retries: 3

  # Frontend (Vue 3 SPA)
  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
      args:
        VITE_API_URL: https://api.${DOMAIN:-uncld.dev}
    depends_on:
      - backend
    x-ports:
      - ${DOMAIN:-uncld.dev}:80/https
      - www.${DOMAIN:-uncld.dev}:80/https
    # Optional: custom Caddy configuration for this service
    # x-caddy:
    #   spa: true  # SPA mode - serve index.html for all routes
    deploy:
      replicas: 2

volumes:
  postgres_data:
  redis_data:
```

**Key Uncloud Extensions**:

1. **x-ports**: Expose services publicly
   - Format: `hostname:container_port/protocol`
   - Protocol: `http` or `https`
   - Caddy automatically handles HTTPS/TLS certificates
   - Example: `api.myapp.uncld.dev:8080/https`

2. **x-caddy**: Custom Caddy reverse proxy configuration
   - Add custom headers, authentication, rate limiting
   - Useful for advanced routing requirements

3. **x-machines**: Pin service to specific machines
   - For stateful services like databases
   - Example: ensure DB always runs on same server

4. **deploy.replicas**: Number of container instances
   - Load balanced automatically across machines
   - For high availability and scaling

### Environment Configuration

**Development (.env.dev)**:
```bash
DATABASE_NAME=template_dev
DATABASE_USER=admin
DATABASE_PASSWORD=devpassword
JWT_SECRET=dev-secret-key-change-in-production
ENVIRONMENT=dev
DOMAIN=localhost
```

**Production (.env.prod)**:
```bash
DATABASE_NAME=production_db
DATABASE_USER=prod_admin
DATABASE_PASSWORD=<secure-password>
JWT_SECRET=<secure-random-string>
ENVIRONMENT=prod
DOMAIN=myapp.uncld.dev
# Or use custom domain:
# DOMAIN=myapp.example.com
```

### Uncloud Deployment Workflow

**1. Initial Setup**

Install Uncloud CLI locally:
```bash
# macOS/Linux via Homebrew
brew install psviderski/tap/uncloud

# Or via install script
curl -fsS https://get.uncloud.run/install.sh | sh
```

**2. Initialize Cluster**

Initialize your first machine (VPS/cloud server):
```bash
# SSH into your server and initialize Uncloud
uc machine init [email protected]

# This will:
# - Install Docker on the server
# - Install Uncloud daemon
# - Setup WireGuard mesh network
# - Deploy Caddy reverse proxy
# - Reserve free *.uncld.dev subdomain
```

**3. Deploy Application**

From your local machine (same directory as compose.yml):
```bash
# Build and deploy in one command
uc deploy

# Or separate steps:
uc build --push    # Build images and push to cluster
uc deploy --no-build  # Deploy with existing images

# The deploy command will:
# - Pull/build images on cluster machines
# - Start containers with zero-downtime rolling updates
# - Configure Caddy for HTTPS
# - Update DNS records (if using uncld.dev)
```

**4. Manage Deployment**

```bash
# List all services and their endpoints
uc ls

# View service logs
uc logs backend
uc logs backend -f  # Follow logs

# Scale a service
uc scale backend=4

# View service details
uc ps backend

# Restart a service
uc restart backend

# Execute command in container
uc exec backend /bin/bash
```

**5. Add More Machines (Optional)**

For high availability or scaling:
```bash
# Add a second machine to the cluster
uc machine init [email protected]

# Services with replicas > 1 will automatically spread across machines
# Database can be pinned to specific machine via x-machines
```

**6. Custom Domain Setup**

To use your own domain instead of `*.uncld.dev`:
```bash
# 1. Point your domain DNS to cluster machine IPs
#    A record: myapp.example.com -> your-server-ip
#    A record: api.myapp.example.com -> your-server-ip

# 2. Update compose.yml with your domain
#    DOMAIN=myapp.example.com

# 3. Redeploy
uc deploy

# Caddy will automatically obtain Let's Encrypt certificates
```

### CI/CD Pipeline with Uncloud

**GitHub Actions Workflow** (.github/workflows/deploy.yml):
```yaml
name: Build and Deploy

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Run backend tests
        run: |
          cd backend
          ./gradlew test
      
      - name: Set up Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
      
      - name: Run frontend tests
        run: |
          cd frontend
          npm ci
          npm test
      
      - name: Install Uncloud CLI
        run: |
          curl -fsS https://get.uncloud.run/install.sh | sh
      
      - name: Configure SSH
        run: |
          mkdir -p ~/.ssh
          echo "${{ secrets.SSH_PRIVATE_KEY }}" > ~/.ssh/id_rsa
          chmod 600 ~/.ssh/id_rsa
          ssh-keyscan -H ${{ secrets.SERVER_IP }} >> ~/.ssh/known_hosts
      
      - name: Initialize Uncloud context
        run: |
          uc machine init ${{ secrets.SSH_USER }}@${{ secrets.SERVER_IP }}
      
      - name: Deploy to production
        env:
          DATABASE_PASSWORD: ${{ secrets.DATABASE_PASSWORD }}
          JWT_SECRET: ${{ secrets.JWT_SECRET }}
        run: |
          # Create production env file
          cat > .env.prod << EOF
          DATABASE_NAME=production_db
          DATABASE_USER=prod_admin
          DATABASE_PASSWORD=${DATABASE_PASSWORD}
          JWT_SECRET=${JWT_SECRET}
          ENVIRONMENT=prod
          DOMAIN=${{ secrets.PRODUCTION_DOMAIN }}
          EOF
          
          # Deploy with production env
          uc deploy --env-file .env.prod
      
      - name: Verify deployment
        run: |
          uc ps
          # Optional: run smoke tests against deployed app
```

**Required GitHub Secrets**:
- `SSH_PRIVATE_KEY`: Private key for SSH access to server
- `SSH_USER`: SSH username (e.g., `root` or `ubuntu`)
- `SERVER_IP`: Server IP address
- `DATABASE_PASSWORD`: Production database password
- `JWT_SECRET`: Production JWT secret key
- `PRODUCTION_DOMAIN`: Your production domain

### Production Deployment Checklist

Before deploying to production:

**Infrastructure**:
- [ ] VPS/server provisioned (minimum: 2GB RAM, 2 vCPU)
- [ ] Ubuntu 22.04+ or Debian 11+ installed
- [ ] SSH access configured with key-based authentication
- [ ] Firewall configured (allow ports 80, 443, 22)
- [ ] Domain DNS configured (if using custom domain)

**Application**:
- [ ] All tests passing (backend + frontend)
- [ ] Environment variables configured in `.env.prod`
- [ ] Database migrations tested
- [ ] Secure passwords and secrets generated
- [ ] CORS configured for production domain
- [ ] Error tracking setup (Sentry, etc.)

**Uncloud**:
- [ ] Uncloud CLI installed locally
- [ ] Machine initialized with `uc machine init`
- [ ] Compose file validated
- [ ] Deployment tested in staging environment

**Post-Deployment**:
- [ ] Verify all services running: `uc ps`
- [ ] Check HTTPS certificates active
- [ ] Test API endpoints
- [ ] Test frontend accessibility
- [ ] Monitor logs: `uc logs -f`
- [ ] Setup monitoring alerts

### Backup and Recovery

**Database Backups**:
```bash
# Create backup script on cluster machine
uc exec postgres pg_dump -U admin template_db > backup.sql

# Or use automated backups
# Setup cron job on cluster machine:
# 0 2 * * * docker exec $(docker ps -qf name=postgres) pg_dump -U admin template_db | gzip > /backups/db-$(date +\%Y\%m\%d).sql.gz
```

**Recovery**:
```bash
# Restore from backup
uc exec postgres psql -U admin template_db < backup.sql
```

### Scaling Strategies

**Vertical Scaling** (single machine):
- Increase VPS resources (CPU, RAM)
- Adjust container resource limits in compose file

**Horizontal Scaling** (multiple machines):
```bash
# Add more machines to cluster
uc machine init [email protected]
uc machine init [email protected]

# Increase replicas in compose.yml
services:
  backend:
    deploy:
      replicas: 4  # Will distribute across available machines

# Redeploy
uc deploy
```

**Database Scaling**:
- Use managed PostgreSQL (e.g., DigitalOcean, AWS RDS) for better scaling
- Update `DATABASE_URL` in environment variables
- Remove postgres service from compose.yml

### Monitoring and Health Checks

**Built-in Health Checks**:
```yaml
services:
  backend:
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
```

**Monitoring Commands**:
```bash
# View cluster status
uc ls

# Monitor service logs
uc logs backend -f
uc logs postgres -f

# Check resource usage
uc exec backend top
```

**External Monitoring** (Optional):
- Setup Uptime monitoring (UptimeRobot, Pingdom)
- Application monitoring (New Relic, Datadog)
- Log aggregation (Papertrail, Loggly)

## Extending the Template

This base template is designed to be extended for specific business applications. Each new project should:

1. **Clone the template repository**
2. **Add domain-specific modules** (e.g., products, orders, appointments, etc.)
3. **Extend base entities** with application-specific fields
4. **Add custom business logic** in new service classes
5. **Create domain-specific DTOs and endpoints**
6. **Customize frontend components** for the specific use case

**Example Extensions**:
- E-commerce: Products, cart, orders, payments
- Healthcare: Patients, appointments, medical records
- Project Management: Projects, tasks, time tracking
- CRM: Contacts, leads, opportunities, pipeline

## Future Enhancements (Optional)

**Features to Consider**:
- Email notifications (Spring Mail + templates)
- File upload/storage (S3 or local)
- Export functionality (PDF, Excel)
- Advanced search (Elasticsearch)
- Real-time features (WebSockets)
- API rate limiting
- API versioning
- GraphQL endpoint (alternative to REST)
- Admin dashboard for monitoring
- Two-factor authentication (2FA)
- OAuth/SSO integration
- Background job processing (Spring @Async or message queue)
- Scheduled tasks (Spring @Scheduled)

## Performance Considerations

1. **Database Indexing**: On frequently queried columns
2. **Query Optimization**: Use explain analyze, avoid N+1
3. **Caching**: Redis for session data, frequently accessed data
4. **Connection Pooling**: HikariCP (default in Spring Boot)
5. **Lazy Loading**: For entity relationships
6. **Pagination**: For large datasets
7. **CDN**: For frontend static assets (production)

## Monitoring & Logging

**Backend Logging**:
- Structured JSON logs
- Log levels: ERROR, WARN, INFO, DEBUG
- Include correlation IDs for request tracing

**Monitoring**:
- Spring Boot Actuator endpoints
- Health checks: `/actuator/health`
- Metrics: `/actuator/metrics`
- Consider: Prometheus + Grafana for production

## Documentation

**API Documentation**:
- Springdoc OpenAPI (Swagger UI)
- Accessible at `/swagger-ui.html`
- Auto-generated from annotations

**Code Documentation**:
- KDoc for Kotlin code
- TSDoc for TypeScript code
- README files in each major module

## Development Workflow

1. **Setup**: Clone repo, run `docker-compose up`
2. **Backend**: Open in IntelliJ IDEA, run Spring Boot app
3. **Frontend**: `npm install`, `npm run dev`
4. **Database**: Migrations run automatically on startup
5. **Testing**: `./gradlew test` (backend), `npm test` (frontend)

## Conclusion

This architecture provides a solid, secure, and scalable foundation for building business applications. The template can be cloned and customized for each new client project, significantly reducing initial development time while maintaining high code quality and security standards.

---

**Version**: 1.7
**Last Updated**: January 19, 2026
**Maintained By**: [Your Name/Team]

**Changelog**:
- v1.7: **Role & Permission System**
  - Implemented database-driven role and permission system
  - Users can now have multiple roles (many-to-many)
  - Roles can have multiple permissions (many-to-many)
  - Resource-action permission naming: `user:create`, `role:delete`, etc.
  - Default permissions seeded: user management, role management, audit
  - Default roles: ADMIN (all permissions), USER (minimal permissions)
  - System roles (ADMIN, USER) protected from deletion
  - Spring Security integration with `@PreAuthorize("hasAuthority('permission:code')")`
  - New endpoints: Role management (`/api/roles/*`)
  - New user management endpoints: create, update, reset-password, list-deleted, restore
  - Removed `role` enum column from User entity (replaced by user_roles junction)
  - Added migrations V004-V006 for roles/permissions schema
- v1.6: **Username login and default admin user**
  - Added username field to User entity (unique, 3-50 characters)
  - Login now accepts email OR username (flexible authentication)
  - Added default admin user created via migration:
    - Username: `admin`, Email: `admin@example.com`, Password: `admin123`
  - Updated LoginRequest DTO: `email` field renamed to `identifier`
  - Updated RegisterRequest DTO: added required `username` field
  - Updated frontend login form to accept "Email or Username"
  - Updated frontend registration form with username field
  - Added V003 migration for username column and admin user seed
- v1.5: **MAJOR UPDATE - Switched to Spring Boot 4.0.1**
  - Spring Boot 3.5.9 → 4.0.1 (latest GA, production-ready)
  - **Added root project structure documentation** (api/, web/, docker-compose.yml)
  - **Rationale**: For NEW projects, 4.0 is the right choice
    - Breaking changes don't affect new projects (only migrations)
    - By production time (6+ months), will have 8+ months of usage
    - Avoids future migration pain (3.5.9 support ends June 2026)
    - Better performance: 2x write throughput, 85% faster startup, 70% less memory
    - Built on Spring Framework 7 + Jakarta EE 11
  - Added Kotlin compiler options for Spring Boot 4.0 compatibility:
    - `-Xjsr305=strict` for null safety
    - `-Xannotation-default-target=param-property` for annotation handling
  - Updated version rationale with detailed NEW project vs migration argument
  - Kotlin 2.3.0 confirmed fully compatible with Spring Boot 4.0 (Kotlin 2.2+ baseline)
- v1.4: Updated all dependencies to latest stable versions
  - Kotlin 2.1.0 → 2.3.0 (latest stable, Dec 2025)
  - PostgreSQL 15+ → 17+ (latest stable major version)
  - Added specific versions for all frontend libraries:
    - Vue 3.5+, TypeScript 5.9+, Vite 7.3+
    - Pinia 3.0+, Vue Router 4.5+, PrimeVue 4.5+
  - Added Flyway version specifications (11.20.2)
  - Added complete package.json example with all dependencies
  - Updated Spring Boot comment (Jan 2026 instead of Jan 2025)
- v1.3: Updated to latest stable versions
  - Spring Boot 3.5.9 (latest stable, not 4.0 due to newness)
  - Kotlin 2.1.0 (latest)
  - Added rationale for version choices
  - Added Flyway PostgreSQL driver dependency
- v1.2: Major corrections and improvements
  - Fixed BaseEntity to use JPA lifecycle callbacks (@CreatedDate, @LastModifiedDate, etc.)
  - Removed ID conflict in User entity example (inherits from BaseEntity)
  - Updated User entity to match database schema with all fields
  - Added column length specifications to entity examples
  - Clarified dual-token authentication (JWT access + refresh tokens)
  - Specified JPA auditing implementation approach (EntityListeners)
  - Added comprehensive JPA configuration (ddl-auto=validate for Flyway compatibility)
  - Removed redundant noArg configuration (kotlin-jpa handles it)
  - Clarified soft delete strategy
  - Improved Redis optional configuration with removal instructions
  - Added Docker requirement note for Testcontainers
  - Clarified repository layer as Spring Data JPA interfaces
  - Removed Quasar from UI component options (standardized on PrimeVue)
- v1.1: Standardized on JPA/Hibernate for ORM, added Kotlin-specific JPA configuration
- v1.0: Initial architecture with Uncloud deployment

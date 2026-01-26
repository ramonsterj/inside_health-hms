# Implementation Guide - kotlin-spring-template

**Purpose**: Essential rules for implementing the base application. See ARCHITECTURE.md for full details.

---

## 🚨 NON-NEGOTIABLE VERSIONS

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.spring") version "2.3.0"
    kotlin("plugin.jpa") version "2.3.0"
    id("org.springframework.boot") version "4.0.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("dev.detekt") version "main-SNAPSHOT"              // Code quality
    id("org.owasp.dependencycheck") version "12.2.0"      // Vulnerability scanning
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}
```

**Database**: PostgreSQL 17+ | **Java**: 21+

---

## ✅ CRITICAL RULES

### 1. Entities MUST Inherit BaseEntity

```kotlin
// ✅ CORRECT
@Entity
@Table(name = "users")
@SQLRestriction("deleted_at IS NULL")  // Hibernate 6+ (Spring Boot 4)
class User(...) : BaseEntity()

// ❌ WRONG - Don't add your own id field
class User(
    @Id var id: Long,  // WRONG! BaseEntity has this
    ...
)
```

### 2. JPA Configuration (NEVER Change)

```yaml
# application.yml
spring.jpa.hibernate.ddl-auto: validate  # CRITICAL: Use 'validate' with Flyway
spring.jpa.open-in-view: false
```

**Never use**: `create`, `create-drop`, or `update` with Flyway - causes schema conflicts.

### 3. Soft Deletes (Default Behavior)

- **ALL** deletions are soft deletes (set `deleted_at`)
- Use `@SQLRestriction("deleted_at IS NULL")` on ALL entities (Hibernate 6+)
- Hard deletes ONLY for GDPR/compliance

```kotlin
// Service layer - soft delete
fun deleteUser(id: Long) {
    user.deletedAt = LocalDateTime.now()
    userRepository.save(user)
}
```

### 4. Entity Column Specifications

```kotlin
// ✅ ALWAYS specify lengths
@Column(nullable = false, unique = true, length = 255)
var email: String

// ✅ Use enums with STRING
@Column(nullable = false, length = 50)
@Enumerated(EnumType.STRING)
var role: UserRole

// ❌ WRONG - No length specified
@Column(nullable = false)
var email: String  // Will default to 255, be explicit!
```

### 5. JPA Auditing Setup

```kotlin
@Configuration
@EnableJpaAuditing
class JpaConfig {
    @Bean
    fun auditorAware(): AuditorAware<Long> = AuditorAware {
        val auth = SecurityContextHolder.getContext().authentication
        val principal = auth?.principal
        if (auth != null && auth.isAuthenticated && principal is CustomUserDetails) {
            Optional.of(principal.id)
        } else Optional.empty()
    }
}
```

### 6. Package Structure

```
com.insidehealthgt.hms/
├── audit/           # Audit logging (AuditEntityListener, AuditContext)
├── config/          # Spring configs (@Configuration, I18nConfig)
├── controller/      # REST controllers (@RestController)
├── dto/request/     # Request DTOs
├── dto/response/    # Response DTOs
├── entity/          # JPA entities (@Entity)
├── repository/      # Spring Data JPA interfaces
├── service/         # Business logic (@Service, EmailService)
├── security/        # Auth, JWT, CustomUserDetails
└── exception/       # Custom exceptions, @ControllerAdvice
```

### 7. Migration Files (Flyway)

**Location**: `src/main/resources/db/migration/`
**Naming**: `V{version}__{description}.sql`

**Current migrations**: V001-V010 (users, audit_logs, roles/permissions, password_reset_tokens, locale)

```sql
-- Example: Always include BaseEntity fields in new tables
CREATE TABLE your_table (
    id BIGSERIAL PRIMARY KEY,
    -- your fields...
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP
);
CREATE INDEX idx_your_table_deleted_at ON your_table(deleted_at);
```

**ALWAYS** create indexes on: foreign keys, `deleted_at`, frequently queried columns

### 8. NO Data Classes for Entities

```kotlin
// ❌ WRONG
@Entity
data class User(...)  // Don't use data class!

// ✅ CORRECT
@Entity
class User(...)
```

**Why**: JPA requires mutable entities with proper equals/hashCode based on ID.

---

## 🔒 SECURITY RULES

### Authentication: Dual-Token System

- **Access Token (JWT)**: 15-30 min, in Authorization header
- **Refresh Token**: 7-30 days, stored in database, revocable

```kotlin
// ✅ Store refresh tokens in DB
@Entity
@Table(name = "refresh_tokens")
class RefreshToken(
    @Column(nullable = false, unique = true, length = 500)
    var token: String,
    
    @Column(name = "expires_at", nullable = false)
    var expiresAt: LocalDateTime,
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    var user: User
) : BaseEntity()
```

### Password Handling

```kotlin
// ✅ ALWAYS hash passwords
@Service
class UserService(private val passwordEncoder: PasswordEncoder) {
    fun createUser(request: CreateUserRequest): User {
        val hashedPassword = passwordEncoder.encode(request.password)
        return User(username = request.username, email = request.email, passwordHash = hashedPassword)
    }
}

// ❌ NEVER store plain text passwords
```

### Default Admin User (V003 migration)

- **Username**: `admin` | **Email**: `admin@example.com` | **Password**: `admin123`
- ⚠️ **Change this password immediately after first deployment!**

### Password Reset Flow

- `PasswordResetToken` entity stores time-limited tokens
- `EmailService` interface with `ConsoleEmailService` (dev) and `SmtpEmailService` (prod)
- Endpoints: `POST /api/auth/forgot-password`, `POST /api/auth/reset-password`

---

## 🚫 COMMON MISTAKES TO AVOID

### ❌ DON'T Do This:

```kotlin
// 1. Don't use LocalDateTime.now() in entities
@Column(name = "created_at")
var createdAt: LocalDateTime = LocalDateTime.now()  // WRONG!
// Use @CreatedDate instead (JPA lifecycle)

// 2. Don't create entities without @SQLRestriction
@Entity
class User(...)  // WRONG! Missing @SQLRestriction for soft deletes

// 3. Don't use nullable ID in BaseEntity
@Id
var id: Long = 0  // WRONG! Use Long? = null

// 4. Don't use ddl-auto=update with Flyway
spring.jpa.hibernate.ddl-auto: update  // WRONG!

// 5. Don't expose entities directly in controllers
@GetMapping("/users/{id}")
fun getUser(@PathVariable id: Long): User  // WRONG!
// Use DTOs instead
```

### ✅ DO This Instead:

```kotlin
// 1. Use DTOs in controllers
@GetMapping("/users/{id}")
fun getUser(@PathVariable id: Long): UserResponse {
    val user = userService.findById(id)
    return UserResponse.from(user)
}

// 2. Use proper error handling
@ControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(EntityNotFoundException::class)
    fun handleNotFound(ex: EntityNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(message = ex.message))
    }
}
```

---

## 📋 IMPLEMENTED FEATURES

### Backend (Complete)
- ✅ Spring Boot 4.0.1 + Kotlin 2.3.0 + Java 21
- ✅ BaseEntity with JPA auditing, soft deletes
- ✅ User, Role, Permission entities (many-to-many)
- ✅ JWT auth with refresh tokens
- ✅ Password reset with email service
- ✅ Audit logging (AuditEntityListener)
- ✅ i18n support (I18nConfig, MessageService)
- ✅ Testcontainers for PostgreSQL
- ✅ Patient Admission (admissions, triage codes, rooms, discharge flow)

### Frontend (Complete)
- ✅ Vue 3.5 + TypeScript 5.9 + Vite 7.x
- ✅ Pinia stores: auth, user, role, audit, locale, notification
- ✅ PrimeVue 4.5 UI components
- ✅ vue-i18n for internationalization
- ✅ VeeValidate + Zod for form validation
- ✅ Axios with auth interceptors
- ✅ Session expiration handling with modal notification, redirect preservation, and proactive token monitoring
- ✅ Vitest for unit testing (happy-dom, @vue/test-utils, @pinia/testing)
- ✅ Playwright for E2E testing
- ✅ Patient Admission UI (multi-step wizard, admin CRUD for triage codes and rooms)

### Security Tooling
- ✅ Detekt (Kotlin static analysis)
- ✅ OWASP dependency-check
- ✅ ESLint + oxlint + eslint-plugin-security

---

## 📚 Key Frontend Dependencies

```json
{
  "vue": "^3.5.24",
  "pinia": "^3.0.4",
  "primevue": "^4.5.4",
  "axios": "^1.13.2",
  "vue-i18n": "^10.0.8",
  "vue-router": "^4.6.4",
  "vee-validate": "^4.15.1",
  "zod": "^3.25.76"
}
```

**Test Dependencies** (devDependencies):
```json
{
  "vitest": "^4.0.17",
  "happy-dom": "^20.3.4",
  "@vue/test-utils": "^2.4.6",
  "@pinia/testing": "^1.0.3",
  "@playwright/test": "^1.57.0"
}
```

**Test Commands**:
- `npm test` - Unit tests (watch mode)
- `npm run test:run` - Unit tests (single run)
- `npm run test:coverage` - Unit tests with coverage
- `npm run test:e2e` - E2E tests (Playwright)
- `npm run test:all` - All tests

See **ARCHITECTURE.md** for full documentation, **VERSION_UPDATES.md** for version rationale.

---

## 🆘 Quick Fixes

**JPA Error: "No identifier specified for entity"**  
→ Entity is missing `BaseEntity` inheritance

**Flyway Error: "Schema-validation: missing table"**  
→ Check `ddl-auto=validate` and run migrations

**Soft Delete Not Working**
→ Missing `@SQLRestriction("deleted_at IS NULL")` on entity

**createdBy/updatedBy Always Null**  
→ Missing `@EnableJpaAuditing` or AuditorAware not configured

**Build Error: "Kotlin classes are final"**  
→ Missing `kotlin("plugin.jpa")` in build.gradle.kts

---

**Last Updated**: January 23, 2026
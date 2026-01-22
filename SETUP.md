# Project Setup Guide

This guide explains how to use the kotlin-spring-template as a starting point for your new project.

---

## Prerequisites

Before running the setup script or starting development, ensure you have:

- **Java 21+** (for backend)
- **Node.js 20+** (for frontend)
- **Docker** (for PostgreSQL database)
- **Bash** with `awk` (standard on Linux/macOS)
- **Playwright browsers** (optional, for E2E tests): `npx playwright install chromium`

---

## Quick Start (Automated)

Run the setup script to automatically configure your project:

```bash
./setup.sh
```

The script will prompt you for:
- **Company name**: Your organization identifier (e.g., `acme` or `dropsmethod`)
- **Project name** (optional): Your application name (e.g., `inventory`)
  - For `com.acme.inventory` → enter `inventory`
  - For `com.dropsmethod` (company IS the app) → leave empty
- **Display name**: Human-readable name for the UI (e.g., `Inventory Manager`)
- **Database name**: PostgreSQL database name (defaults to `{project}_db` or `{company}_db`)

The script handles all package renaming, configuration updates, and optionally reinitializes git.

---

## Manual Setup

If you prefer manual control or need to customize beyond what the script provides:

### Step 1: Clone and Detach from Template

```bash
# Option A: Clone and remove git history
git clone <template-repo-url> my-project
cd my-project
rm -rf .git
git init

# Option B: Download as ZIP (already has no git history)
# Extract and rename the folder
```

### Step 2: Rename Kotlin Packages

**Current package**: `com.yourcompany.template`

**Change to one of:**
- `com.{yourcompany}.{yourproject}` (e.g., `com.acme.inventory`)
- `com.{yourcompany}` if company IS the app (e.g., `com.dropsmethod`)

#### 2.1 Move Source Files

```bash
# Option A: With project name (com.acme.inventory)
cd api/src/main/kotlin
mkdir -p com/{yourcompany}/{yourproject}
mv com/yourcompany/template/* com/{yourcompany}/{yourproject}/
rm -rf com/yourcompany

# Option B: Company only (com.dropsmethod)
cd api/src/main/kotlin
mkdir -p com/{yourcompany}
mv com/yourcompany/template/* com/{yourcompany}/
rm -rf com/yourcompany/template

# Test source (same pattern)
cd ../../test/kotlin
# ... repeat for your chosen structure
```

#### 2.2 Update Package Declarations

In **all** `.kt` files, update the package statement:

```kotlin
// Before
package com.yourcompany.template

// After (with project name)
package com.acme.inventory

// After (company only)
package com.dropsmethod
```

#### 2.3 Rename Application Class

Rename `TemplateApplication.kt` to `{YourProject}Application.kt` and update the class name:

```kotlin
// Before
class TemplateApplication

// After
class YourProjectApplication
```

Also rename and update the test file:
- Rename `TemplateApplicationTest.kt` → `{YourProject}ApplicationTest.kt`
- Update the class name and imports inside the test file

### Step 3: Update Gradle Configuration

#### api/settings.gradle.kts
```kotlin
// Before
rootProject.name = "template-api"

// After
rootProject.name = "{yourproject}-api"
```

#### api/build.gradle.kts
```kotlin
// Before
group = "com.yourcompany"

// After
group = "com.{yourcompany}"
```

### Step 4: Update Application Configuration

#### api/src/main/resources/application.yml

```yaml
spring:
  application:
    name: {yourproject}-api  # Line 3

  datasource:
    url: jdbc:postgresql://.../${DATABASE_NAME:{yourproject}_db}  # Line 6

logging:
  level:
    com.{yourcompany}.{yourproject}: DEBUG  # Line 56
```

#### api/src/main/resources/application-dev.yml
```yaml
logging:
  level:
    com.{yourcompany}.{yourproject}: TRACE
```

#### api/src/main/resources/application-prod.yml
```yaml
logging:
  level:
    com.{yourcompany}.{yourproject}: INFO
```

### Step 5: Update Docker Configuration

#### docker-compose.yml

```yaml
services:
  postgres:
    container_name: {yourproject}-postgres  # Line 5
    environment:
      POSTGRES_DB: ${DATABASE_NAME:-{yourproject}_db}  # Line 7
```

### Step 6: Update Frontend Configuration

#### web/package.json
```json
{
  "name": "{yourproject}-web"
}
```

#### web/src/i18n/locales/en.json
```json
{
  "common": {
    "appName": "Your Application Name"
  }
}
```

#### web/src/i18n/locales/es.json
```json
{
  "common": {
    "appName": "Nombre de Tu Aplicacion"
  }
}
```

### Step 7: Update Documentation

#### CLAUDE.md
Update package references from `com.yourcompany.template` to your new package.

#### docs/architecture/ARCHITECTURE.md
Update package references from `com.yourcompany.template` to your new package.

#### lefthook.yml
Update project name in comments.

---

## Files Changed Summary

| File | Changes Required |
|------|------------------|
| `api/settings.gradle.kts` | `rootProject.name` → `{identifier}-api` |
| `api/build.gradle.kts` | `group` → `com.{company}` |
| `api/src/main/resources/application.yml` | App name, DB name, logging package |
| `api/src/main/resources/application-dev.yml` | Logging package |
| `api/src/main/resources/application-prod.yml` | Logging package |
| `api/src/main/kotlin/com/.../TemplateApplication.kt` | Package, class name, file name |
| `api/src/main/kotlin/com/.../` (all files) | Package declarations |
| `api/src/test/kotlin/com/.../TemplateApplicationTest.kt` | Package, class name, file name |
| `api/src/test/kotlin/com/.../` (all files) | Package declarations, imports |
| `docker-compose.yml` | Container name, DB name |
| `web/package.json` | `name` → `{identifier}-web` |
| `web/src/i18n/locales/*.json` | `appName` |
| `CLAUDE.md` | Package references |
| `docs/architecture/ARCHITECTURE.md` | Package references |
| `lefthook.yml` | Project name in comments |

*`{identifier}` = project name if provided, otherwise company name*

---

## Post-Setup Security Checklist

After completing the setup, ensure you address these security items before deploying:

### Critical (Before First Production Deploy)

- [ ] **Change default admin password**: Default is `admin123` (set in V003 migration)
- [ ] **Generate secure JWT secret**: Minimum 256 bits for production
  ```bash
  # Generate a secure secret
  openssl rand -base64 32
  ```
- [ ] **Update database credentials**: Defaults are `admin`/`password`

### Production Environment Variables

Set these in your deployment environment:

```bash
# Database
DATABASE_HOST=your-db-host
DATABASE_PORT=5432
DATABASE_NAME=your_db
DATABASE_USERNAME=secure_user
DATABASE_PASSWORD=secure_password

# JWT (CRITICAL - must be unique per environment)
JWT_SECRET=your-256-bit-minimum-secret-here
JWT_ACCESS_EXPIRATION=900000      # 15 minutes (recommended)
JWT_REFRESH_EXPIRATION=604800000  # 7 days

# Optional: OWASP dependency check
NVD_API_KEY=your-nvd-api-key
```

---

## Running the Project

### Start Development Environment

```bash
# 1. Start database
docker-compose up -d

# 2. Run backend (in one terminal)
cd api
./gradlew bootRun

# 3. Run frontend (in another terminal)
cd web
npm install
npm run dev
```

### Access Points

- **Backend API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Frontend**: http://localhost:5173 (Vite default)

### Default Login

- **Username**: `admin`
- **Password**: `admin123`

### Running Tests

**Unit Tests (Vitest)**:
```bash
cd web

# Watch mode (re-runs on file changes)
npm test

# Single run
npm run test:run

# With coverage report
npm run test:coverage
```

**E2E Tests (Playwright)**:
```bash
cd web

# First time only: install browsers
npx playwright install chromium

# Run E2E tests (auto-starts dev server)
npm run test:e2e

# Run with interactive UI (great for debugging)
npm run test:e2e:ui

# Run in headed mode (see browser)
npm run test:e2e:headed
```

**Run All Tests**:
```bash
cd web && npm run test:all
```

---

## Troubleshooting

### "No identifier specified for entity"
Your entity is missing `BaseEntity` inheritance. All entities must extend `BaseEntity`.

### Flyway migration errors
Ensure `spring.jpa.hibernate.ddl-auto=validate` is set. Never use `update` or `create` with Flyway.

### Package not found errors after rename
1. Clean build: `./gradlew clean`
2. Invalidate IDE caches
3. Check all package declarations match directory structure

### Tests fail to find application class
Update test files to import the renamed application class:
```kotlin
// Update from
import com.yourcompany.template.TemplateApplication
// To
import com.{yourcompany}.{yourproject}.{YourProject}Application
```

---

## Additional Customization

### Adding New Locales

1. Create a new file in `web/src/i18n/locales/` (e.g., `fr.json`)
2. Copy structure from `en.json`
3. Translate values
4. Register in `web/src/i18n/index.ts`

### Changing the UI Theme

PrimeVue themes are configured in `web/src/main.ts`. See [PrimeVue Theming](https://primevue.org/theming/styled/) for options.

### Adding New Entities

1. Create entity class extending `BaseEntity`
2. Add `@SQLRestriction("deleted_at IS NULL")` for soft delete support
3. Create Flyway migration in `api/src/main/resources/db/migration/`
4. Create repository, service, and controller

See [CLAUDE.md](CLAUDE.md) for detailed entity requirements and patterns.

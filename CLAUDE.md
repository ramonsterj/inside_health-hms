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

## 🚨 ZERO TOLERANCE: BUGS & BROKEN TESTS

- **All tests must pass.** No exceptions.
- **Bugs must be fixed when found**, regardless of who introduced them or whether they are pre-existing.
- **Broken tests must be fixed immediately** when identified — do not skip, disable, or ignore them.
- Before considering any task complete, run the relevant test suite and verify all tests pass.

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
├── event/           # Domain events (billing automation events, listeners)
├── scheduler/       # Scheduled tasks (DailyChargeScheduler)
├── security/        # Auth, JWT, CustomUserDetails
└── exception/       # Custom exceptions, @ControllerAdvice
```

### 7. Migration Files (Flyway)

**See [`docs/architecture/MIGRATIONS.md`](docs/architecture/MIGRATIONS.md)** for the full guide:
the `db/migration` vs `db/seed` split, dev/prod profile differences, the seed-bundle versioning
rule, when to use SQL vs `BaseJavaMigration`, recovery from failed migrations, and the
before-you-add checklist. The rules below are the entity-side contract.

**Location**: `api/src/main/resources/db/migration/`
**Naming**: `V{version}__{description}.sql`

**Current migrations**: V001-V131 (users, audit_logs, roles/permissions, password_reset_tokens, locale, patients, admissions, file storage, admission types, document types, clinical histories, progress notes, medical orders, psychotherapy categories, psychotherapy activities, nursing notes, vital signs, inventory categories, inventory items, inventory movements, inventory permissions, room pricing, patient charges, invoices, billing permissions, billing adjustments, medication administrations, psychotherapy category pricing, medical order inventory link, medication administration permissions, billing configure permission, diet charge unique index, unaccent extension, psychologist permissions, medical order documents, medical order document permissions, bank accounts, treasury employees, salary history, expenses, expense payments, income records, payroll entries, treasury permissions, doctor fees, bank account column mappings, bank statements, seed treasury test data, treasury integrity fixes, vital_signs glucose column, room occupancy permission, medical order category-driven workflow states + emergency authorization columns, medical order workflow permissions — authorize, emergency-authorize, mark-in-progress, medical order rejection audit columns — `rejected_at`/`rejected_by`, lock progress-note + nursing-note update to ADMINISTRADOR-only — adds `progress-note:create` to JEFE_ENFERMERIA, revokes any non-admin `progress-note:update` and `nursing-note:update` grants, lock vital-sign update to ADMINISTRADOR-only — revokes any non-admin `vital-sign:update` grants, replace patient `age` column with `date_of_birth` — age is now derived server-side from `dateOfBirth`; existing rows are backfilled to Jan 1 of `(current_year - age)` as a temporary placeholder per new-patient-intake.md v1.2, admission PDF export — `admission:export-pdf` permission granted to ADMINISTRADOR and PERSONAL_ADMINISTRATIVO; audit logs gain `ADMISSION_EXPORT` action and nullable `status` / `details` columns; no export table — generation is synchronous with request-scoped temp files (V099), pharmacy & inventory evolution V100-V108 — `inventory_items` gains `kind`/`sku`/`lot_tracking_enabled` (V100), new `medication_details` 1:1 satellite (V101) and `inventory_lots` 1:N satellite with FEFO + NULLS-NOT-DISTINCT partial unique index (V102), `lot_id` FK added to `inventory_movements` + `medication_administrations` (V103), seven new permissions `medication:read/create/update/expiry-report` + `inventory-lot:read/create/update` (V104) — `medication:bulk-import` was originally seeded by V104 and dropped by V112 along with the bulk importer, JavaMigration backfill of `medication_details` from legacy free-text via `MedicationNameParser` (V105), synthetic `9999-12-31` legacy lots for every lot-tracked item with `quantity>0` (V106), `medication_administrations.quantity` column added with CHECK (>= 1) so dispense history responses carry the actual amount (V107), `pharmacy_backfill_review` view exposes only `medication_details.review_status = 'NEEDS_REVIEW'` rows — auto-empties once a row is transitioned to CONFIRMED per AC-19 (V108), V109 relaxes the medication administration quantity check for historical zero/null rows while keeping new service-level validation strict, V110 hard-deletes the V052 legacy drug catalog (`kind='DRUG' AND sku IS NULL`) along with its `medication_details` and `synthetic_legacy` lots; self-heals pre-existing clinical/billing references in the same migration by nulling `medical_orders.inventory_item_id`, `patient_charges.inventory_item_id`, and legacy `medication_administrations.lot_id`, deleting `inventory_movements` rows that point at the legacy catalog, then asserts no legacy DRUG row remains — originally shipped as an abort-loudly safety check, but the demo accumulated real test data (50 V052 drugs + 196 refs) before V110 first ran, so the migration was rewritten to sever links in place rather than block deployment; any environment that already recorded old V110 as successful must run `flyway repair` once after deploying the rewritten script, V111 (SQL migration) loads ~615 workbook SKUs into `inventory_items` + `medication_details` so the customer workbook is the single source of truth for the medication and supply catalog (no initial lots — pharmacist registers real stock on first restock). Originally shipped as a Kotlin `BaseJavaMigration` that read `db/migration/data/pharmacy-initial-load.csv` from the classpath, but the CSV was never committed and the workbook data has only ever lived in SQL form (`R__seed_02b_pharmacy_from_workbook.sql`); the Java loader was replaced in-place with a pure SQL migration that mirrors that seed file (prod shape: `quantity=0`, no initial lots), V112 drops the `medication:bulk-import` permission seeded by V104 since the one-shot loader replaces the deleted bulk importer, V113 adds `inventory_categories.default_for_kind VARCHAR(20)` with a partial unique index over active non-deleted rows and backfills the seeded `Medicamentos` category to `'DRUG'` — `PharmacyService.createMedication` now resolves the drug category via this column instead of accepting `categoryId` from the request, and `InventoryCategoryService` blocks delete/deactivate on rows whose `default_for_kind` is non-null per pharmacy-and-inventory-evolution.md FR-9, V114 introduces the `MEDICO_RESIDENTE` system role, clones every MEDICO permission grant onto it via a set-based insert, and additionally grants `admission:create` (which MEDICO itself does not hold) so residents can register admissions, V115 adds `admissions.resident_id BIGINT NOT NULL REFERENCES users(id)` plus an index — resident is the user who admits the patient and is auto-bound from the authenticated principal at create time; the service rejects creates from any user lacking `MEDICO_RESIDENTE` with `error.admission.resident.role.required` — **except `ADMINISTRADOR`, see V122 below**. (V115 originally added `residentId` only as an auto-bound principal slot and required every admitting account, including the seeded `admin`, to carry `MEDICO_RESIDENTE`; V122 changed that.) `MEDICO_RESIDENTE` users see every admission/patient listing — only the standalone `MEDICO` role is scoped to own patients; admission cards and the detail view display both treating physician and resident, V116 grants the `PSICOLOGO` role `medical-order:read` / `medical-order:mark-in-progress` / `medical-order:upload-document` (scoped to `PRUEBAS_PSICOMETRICAS` at the service layer), V117 introduces the `AUXILIAR_ENFERMERIA` system role with an explicit SUBSET of ENFERMERO's grants — `nursing-note:read/create`, `vital-sign:read/create`, `medication-administration:read`, `medical-order:read`, `progress-note:read`, `clinical-history:read`, `patient:read`, `admission:read`, `room:occupancy-view` — deliberately excluding `medication-administration:create`, `medical-order:mark-in-progress`, `medical-order:upload-document`, `progress-note:create`, and `admission:update` (the latter gates discharge / admission edit / consulting-physician changes — out of the notes/vitals-only scope, and ENFERMERO itself only holds `admission:read`); service-layer guards in `MedicationAdministrationService` / `MedicalOrderService` / `MedicalOrderDocumentService` reject those three clinical actions with 403 `error.nursing.auxiliary.denied` when the caller's only nursing-or-better role is `AUXILIAR_ENFERMERIA` (stacked ENFERMERO/JEFE_ENFERMERIA/MEDICO/ADMINISTRADOR holders pass), while the discharge/edit denial is enforced purely by the absent `admission:update` grant. `ENFERMERO` is retained unchanged and now means the graduate nurse — see docs/features/nursing-roles-split.md, V118 grants `room:occupancy-view` to `MEDICO_RESIDENTE` (which clones MEDICO via V114 and so did not inherit it) so the Bed Occupancy screen can serve as the default landing page for resident doctors; plain MEDICO remains excluded — see docs/features/bed-occupancy-view.md, V119 introduces warehouse-scoped inventory (bodegas): creates `warehouses`, `inventory_warehouse_stock` (per-(item, warehouse, optional lot) NUMERIC quantity), `inventory_transfers`, `warehouse_charges`, `user_warehouses`, and the data-driven `role_default_warehouses` mapping table; adds `warehouse_id` + `transfer_id` columns to `inventory_movements`; seeds six warehouses (`ADMINISTRACION`, `ENFERMERIA`, `MANTENIMIENTO_1`, `MANTENIMIENTO_2`, `COCINA`, `PSICOLOGIA`), eight permissions (`warehouse:read/create/update/delete`, `warehouse-transfer:create/read/receive`, `warehouse-charge:create`), the `MANTENIMIENTO` system role + its grants, and the role → default-warehouse mapping rows — stock now lives per-warehouse with strict isolation (each bodega only dispenses from itself; FEFO is warehouse-scoped) and non-medical maintenance charges bill an admission as `ChargeType.SERVICE` via a `WarehouseChargeCreatedEvent` (AFTER_COMMIT, REQUIRES_NEW); audit actions `WAREHOUSE_TRANSFER`/`WAREHOUSE_CHARGE` are written in REQUIRES_NEW, V120 backfills `inventory_warehouse_stock` from the legacy `inventory_items.quantity` and `inventory_lots.quantity_on_hand` columns into the `ADMINISTRACION` warehouse, V121 drops the now-redundant legacy `inventory_items.quantity` and `inventory_lots.quantity_on_hand` columns so per-warehouse stock is the single source of truth — see docs/features/warehouse-inventory-management.md. NOTE: the warehouse feature was specced against V117/V118/V119 but V117 was claimed by AUXILIAR_ENFERMERIA and V118 by the MEDICO_RESIDENTE occupancy-view grant, so it shipped as V119–V121, V122 revokes `MEDICO_RESIDENTE` from the seeded `admin` (idempotent `DELETE` of the `user_roles` row scoped to `username = 'admin'`). Patient admissions may now be registered ONLY by a `MEDICO_RESIDENTE` (resident slot auto-bound to the authenticated user); `ADMINISTRADOR` is the sole exception and admits by explicitly picking the resident — `CreateAdmissionRequest.residentId` is required for admins and must reference a `MEDICO_RESIDENTE`. `AdmissionService.resolveResident()` enforces this: admin without `residentId` → 400 `error.admission.resident.required`, admin with a non-resident `residentId` → 400 `error.admission.resident.invalid.role`, non-admin/non-resident holders of `admission:create` (e.g. `PERSONAL_ADMINISTRATIVO`) → 400 `error.admission.resident.role.required`. Admin keeps `admission:create` via the ADMINISTRADOR role's own grant; the admin admission form shows a resident picker fed by `GET /api/v1/admissions/residents` — see docs/features/patient-admission.md. (V122 was authored as V119 on its branch but renumbered on merge since the warehouse feature claimed V119–V121.), V123–V126 introduce **provider-aware laboratory orders** (docs/features/laboratory-orders-with-providers.md): V123 creates the lab catalog tables `lab_providers`, `lab_tests`, `lab_provider_tests` (per-provider `display_name`/`cost`/`sales_price` with `CHECK (cost>=0)` / `CHECK (sales_price>0)` and a partial unique index on `(provider_id, lab_test_id)`), `lab_panels`, and `lab_panel_items` — all with `deleted_at`/FK indexes and partial unique indexes on `LOWER(name)`/membership; V124 adds nullable `medical_orders.lab_provider_id` (FK + index) and the snapshot line table `medical_order_lab_tests` (`lab_provider_test_id`, `lab_test_id`, snapshotted `display_name`/`cost`/`sales_price`); V125 seeds two permissions `lab-catalog:read` (granted to ADMINISTRADOR/MEDICO/MEDICO_RESIDENTE only — deliberately NOT to the medical-order:read holders PSICOLOGO/ENFERMERO/JEFE_ENFERMERIA/AUXILIAR_ENFERMERIA/PERSONAL_ADMINISTRATIVO) and `lab-catalog:manage` (ADMINISTRADOR only); V126 seeds the two providers (CLONY, HOSPITAL HERRERA LLERANDI), the canonical tests, each provider's offered `lab_provider_tests` with **invented demo prices** (PDF has no pricing — finance must correct via the admin UI; CLONY offers every test incl. *Panel de drogas en sangre*, Herrera omits it), and the three panels (`Laboratorios de ingreso`, `… mujeres en edad fértil`, `Laboratorios control`). A `LABORATORIOS` order now carries one `LabProvider` + ≥1 `MedicalOrderLabTest` line; on authorization `MedicalOrderService.publishBillingEventIfNeeded` publishes a `LabOrderAuthorizedEvent` (instead of `MedicalOrderAuthorizedEvent`) whose `BillingEventListener`/`BillingService.createChargeFromLabOrder` creates **one** `ChargeType.LAB` `PatientCharge` **per test line** (each itemized at its snapshotted sales price; the charges sum to Σ line sales-prices) so every billed test appears on the account; lines are immutable once the order leaves `SOLICITADO` (AC14) and catalog edits never change recorded orders (full snapshot, AC12). No legacy lab path — every `LABORATORIOS` order requires a provider + ≥1 line (AC15), and editing a lab order to another category clears its provider/lines; the old single-`inventoryItem` `MedicalOrderAuthorizedEvent` billing path survives only for `REFERENCIAS_MEDICAS` / `PRUEBAS_PSICOMETRICAS`. Dev seed: `lab-catalog:read` added to the MEDICO grant list in `R__seed_01` STEP 3 (MEDICO_RESIDENTE clones it) and the SEED-BUNDLE-VERSION bumped across all nine `R__seed_*` files. V127 renames the ten system role codes (and display names) from English to Spanish **in place** — `ADMIN→ADMINISTRADOR`, `USER→USUARIO`, `ADMINISTRATIVE_STAFF→PERSONAL_ADMINISTRATIVO`, `DOCTOR→MEDICO`, `NURSE→ENFERMERO`, `CHIEF_NURSE→JEFE_ENFERMERIA`, `PSYCHOLOGIST→PSICOLOGO`, `RESIDENT_DOCTOR→MEDICO_RESIDENTE`, `AUXILIARY_NURSE→AUXILIAR_ENFERMERIA`, `MAINTENANCE→MANTENIMIENTO`. The runtime identifies system roles by these Spanish codes; historical migrations V001-V126 keep their original English codes (and Flyway checksums) untouched, and this forward migration converges fresh and already-migrated databases to the same end state (each `UPDATE` is guarded by the old code, so re-application is a no-op). Roles are referenced everywhere by `roles.id` (FK), so updating `roles.code` covers permissions/user_roles/role_default_warehouses (the legacy `users.role` string column was already dropped by V006, so there is nothing else to update). V128 Spanishizes lingering English/mixed reference-data text (roles/permissions/warehouse names + descriptions, etc.) in place — a forward, idempotent defense-in-depth migration so the i18n fallback, PDF export, and direct API consumers also read Spanish; each `UPDATE` is guarded by the stable key (code, or name for admin-renamable categories) so re-application is a no-op and historical migrations keep their English checksums. V129 reconciles the `rooms` table to the real hospital layout (16 rooms / 19 beds): merges `101`+`102`→`101-102` and `103`+`104`→`103-104` (SHARED, capacity 2), renames `304`→`304-305`, converts `105`/`106`/`108`/`303` from SHARED→PRIVATE (capacity 1, Q1100), drops `107`, and adds `205` (PRIVATE). Gender keeps the number-prefix heuristic (`1xx`→FEMALE, else MALE); pricing unchanged (SHARED Q950, PRIVATE Q1100). There is no separate bed table — `capacity` IS the bed count — so this is a pure data reconciliation needing no Kotlin/Vue changes. The two nullable FKs into `rooms(id)` (`admissions.room_id`, `patient_charges.room_id`) are repointed to the surviving room before each `DELETE` so occupancy/charges are preserved where the room survives; every statement is guarded by the old number, so re-application/repair converges fresh and already-migrated databases to the same 16-row end state. `R__seed_02` STEP 9 and the `R__seed_03`/`R__seed_07` room references are updated to the same canonical set (dev/acceptance source of truth). V130 adds nullable `admissions.discharge_note TEXT` — a mandatory free-text discharge comment captured at discharge time (before the discharge-protection lock); mandatory-ness is enforced at the service layer rather than via a NOT NULL constraint so pre-existing discharged rows are unaffected, no index. V131 splits discharge out of the broad `admission:update` authority into a dedicated `admission:discharge` permission granted to ADMINISTRADOR (via the all-permissions grant) and explicitly to MEDICO_RESIDENTE only — no other role may discharge even if it holds `admission:update` (PERSONAL_ADMINISTRATIVO, JEFE_ENFERMERIA); idempotent `INSERT … ON CONFLICT DO NOTHING`. See docs/features/discharge-protection.md. (V130/V131 shipped with PR #88; the RBAC-hardening work — `SystemRole`/`constants/roles.ts` constants, the `SystemRoleCoverageTest` rename/typo safety net, the system-role permission lock in `RoleService.assignPermissions`, and the services/frontend moving from `hasRole("ADMINISTRADOR")` to permission checks — added no migration, since `SystemRoleCoverageTest` confirmed ADMINISTRADOR already holds every permission.)))

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

### 9. Date / Time Formatting (Guatemala Hospital)

The platform targets a hospital in Guatemala. All dates and times must follow a single, locale-independent standard. Full rationale and audit in [`.context/datetime-audit.md`](.context/datetime-audit.md).

**Display formats (frontend, what the user sees):**

| Kind | Format | Example |
| ---- | ------ | ------- |
| Date | `dd/MM/yyyy` | `09/05/2026` |
| Time | `HH:mm` (24-hour) | `14:30` |
| Date + time | `dd/MM/yyyy - HH:mm` | `09/05/2026 - 14:30` |
| Relative | i18n keys in `common.time.*` (`justNow`, `minutesAgo`, …) | `2h ago` |

**Wire formats (API JSON):**

- Date-only: `yyyy-MM-dd` (ISO).
- Datetime: ISO 8601 (`yyyy-MM-ddTHH:mm:ss`). Jackson is configured with `JavaTimeModule` and `WRITE_DATES_AS_TIMESTAMPS` disabled — leave it alone.

**Backend storage rules:**

- "Effective on a day" → `LocalDate` + `DATE` column (e.g. `chargeDate`, `expenseDate`, `effectiveFrom`).
- Event timestamp → `LocalDateTime` + `TIMESTAMP` column (e.g. `admissionDate`, `administeredAt`, all `BaseEntity` audit fields).
- Never store dates as `String`. Never use `java.util.Date` in new code (legacy JJWT internals are the only exception).
- Never use `TIMESTAMPTZ` or `TIME`-only columns.

**Frontend display rules:**

- Always import the helpers from `@/utils/format`: `formatDate`, `formatTime`, `formatDateTime`.
- Never call `toLocaleString` / `toLocaleDateString` / `toLocaleTimeString` directly.
- Never call vue-i18n's `d(date, …)` formatter. There is no `datetimeFormats` config — `d()` will fall back to browser locale and break the standard.
- Use `getRelativeTime` from `@/composables/useRelativeTime` for "ago"-style strings.

**Frontend form / picker rules:**

- Every `<DatePicker>` inherits `dd/mm/yy` from the global PrimeVue locale set in `web/src/main.ts`. Do not pass `dateFormat="yy-mm-dd"` per-component.
- Every `<DatePicker showTime>` must also have `hourFormat="24"`.
- Convert `Date` → API string with `toApiDate(value)` from `@/utils/format`. Never inline `.toISOString().substring(0, 10)` or `.split('T')[0]` — these have a UTC-shift bug in Guatemala (UTC-6). For round-tripping form fields, use the `useFormDateField` composable.

```ts
// ✅ CORRECT
import { formatDate, formatDateTime, formatTime, toApiDate } from '@/utils/format'

formatDate(admission.admissionDate)        // "09/05/2026"
formatDateTime(admission.admissionDate)    // "09/05/2026 - 14:30"
formatTime(vitalSigns.recordedAt)          // "14:30"
toApiDate(datePickerValue)                 // "2026-05-09" (or null)

// ❌ WRONG
new Date(value).toLocaleString()           // browser-locale-dependent
d(new Date(value), 'long')                 // no datetimeFormats config — browser fallback
date.toISOString().substring(0, 10)        // UTC-shift bug for local dates
<DatePicker dateFormat="yy-mm-dd" />       // overrides the global standard
<DatePicker showTime />                    // missing hourFormat="24"
```

ESLint blocks the wrong patterns — if lint fails, use the helpers above. Treat any drift as a bug.

### 10. i18n / Reference-Data Labels (Spanish-default app)

Any label derived from a **stable backend code** (role code, role description,
permission code, permission *resource*, document-type code, triage code,
warehouse code) MUST be rendered through a vue-i18n key derived from that code.
The DB `name`/`description` is a **fallback only** (admin-created rows not yet in
the bundle). Full guide + key-naming map: [`docs/architecture/I18N.md`](docs/architecture/I18N.md).

- **Use `useCodeLabels`** (`web/src/composables/useCodeLabels.ts`) — `roleName`,
  `roleDescription`, `permissionName`, `permissionDescription`,
  `permissionGroupLabel`, `documentTypeName`, `triageCodeLabel`, `warehouseName`,
  `warehouseDescription`. **Never** render `role.name`, `permission.name`,
  `permission.description`, a warehouse `name`/`description`, etc. raw.
- **Admin-authored free-text without a stable code** (inventory *item* names, lab
  test names, admin-renamable inventory/psychotherapy *categories*) is rendered
  **directly from the DB** and stored in Spanish — explicitly *out* of this rule.
- **Two guards enforce it** (ESLint `no-raw-text` does NOT catch `{{ permission.name }}`
  DB bindings): the backend Testcontainers coverage test
  (`I18nReferenceDataCoverageTest` — DB is the source of truth for codes) and the
  frontend catalog vitest (`web/src/i18n/catalogs.ts` + `catalogs.spec.ts`). Adding
  a role/permission/warehouse means adding its code to `catalogs.ts`, its key(s) to
  both `es.json`/`en.json`, and Spanishizing the DB text in the migration + seed.

```ts
// ✅ CORRECT
const { roleName, permissionName } = useCodeLabels()
roleName(role.code, role.name)            // roleNames.<CODE>, DB name as fallback
permissionName(p.code, p.name)            // permissions.<code>.name

// ❌ WRONG — leaks seeded English to Spanish users, invisible to no-raw-text
{{ role.description }}    {{ permission.name }}    optionLabel="name"
```

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

### File Storage Configuration

Files are stored on the local file system instead of database BYTEA columns.

```yaml
# application.yml
app:
  file-storage:
    base-path: ${FILE_STORAGE_PATH:/var/data/hms/files}
```

**Directory Structure**:
```
{base-path}/
└── patients/
    └── {patientId}/
        ├── id-documents/
        │   └── {uuid}_{filename}
        └── consent-documents/
            └── {uuid}_{filename}
```

**Environment variable**: `FILE_STORAGE_PATH` (defaults to `/var/data/hms/files` in production, `./data/files` in dev)

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
- ✅ File System Storage for patient documents (ID docs, consent forms)
- ✅ Medical Record System (clinical histories, progress notes — admin-only update with discharge protection; JEFE_ENFERMERIA can author, only ADMINISTRADOR can edit, medical orders)
- ✅ Medical Order Workflow (three category-driven shapes — directive `ACTIVA`, authorize-only `MEDICAMENTOS`, authorize+execute+results for labs / referencias / pruebas psicométricas. States: `ACTIVA`, `SOLICITADO`, `NO_AUTORIZADO`, `AUTORIZADO`, `EN_PROCESO`, `RESULTADOS_RECIBIDOS`, `DESCONTINUADO`. Endpoints: authorize, reject, emergency-authorize, mark-in-progress, discontinue. Billing fires on authorization, results auto-saved on document upload, discontinue blocked once `EN_PROCESO`.)
- ✅ Psychotherapeutic Activities (activity registration for hospitalized patients, category management)
- ✅ Nursing Module (nursing notes — admin-only update with discharge protection; vital signs — BP, HR, RR, temperature, oxygen saturation, glucometría — admin-only update with discharge protection, append-only for non-admins per V097). Two operational nursing roles: `ENFERMERO` (graduate nurse — full clinical scope) and `AUXILIAR_ENFERMERIA` (V117 — vital signs + nursing notes only; cannot administer meds, mark orders in progress, or upload result documents). `JEFE_ENFERMERIA` is the supervisory superset of ENFERMERO. See docs/features/nursing-roles-split.md.
- ✅ Inventory Management (categories, items with flat/time-based pricing, stock movements, low stock report, room pricing — stock is now per-warehouse via `inventory_warehouse_stock`; see Warehouse-Scoped Inventory below)
- ✅ Hospital Billing System (real-time charge capture, scheduled daily room charges, daily balance, adjustments, invoice generation at discharge, event-driven integration)
- ✅ Clinical Event Billing Automation (medication administration record, psychotherapy activity billing, medical order billing, procedure admission billing, daily diet charges, discharge auto-invoice)
- ✅ Medical Order Document Attachments (file upload/download for lab results, thumbnails, permission-based access)
- ✅ Nursing Kardex Dashboard (aggregated clinical view for nurses: active medications, care instructions, latest vitals, nursing notes, quick actions)
- ✅ Treasury Module Phase 1-2 (bank accounts, expenses, expense payments, income, employees, salary history, payroll, contractor payments)
- ✅ Doctor Fee Billing (Phase 3: fee creation, invoice submission, document upload, settlement with auto-expense creation, payment history integration)
- ✅ Bank Statement Reconciliation (Phase 4: XLSX/CSV upload with configurable column mappings, auto-match against expense payments and income, confirm/reject suggestions, manual match, acknowledge rows, create expense/income from unmatched rows, statement completion)
- ✅ Bed Occupancy View (aggregated `GET /api/v1/rooms/occupancy` endpoint, gated by new `room:occupancy-view` permission granted to ADMINISTRADOR/ENFERMERO/JEFE_ENFERMERIA/PERSONAL_ADMINISTRATIVO — explicitly NOT to MEDICO; counts only ACTIVE non-deleted HOSPITALIZATION admissions)
- ✅ Patient Admissions History (`GET /api/v1/admissions/patients/{patientId}/admissions`, paged most-recent-first by `admissionDate DESC, id DESC`; reuses `AdmissionListResponse` — no new DTO/permission/migration. Authorization is a **gate, not a filter**: `patient:read` + the SAME patient-level visibility as the patient detail page via the extracted `PatientService.assertPatientAccessible` (single source of truth shared with `getPatient`) — a standalone MEDICO must be assigned to the patient via an active admission, a PSICOLOGO needs an active admission, else 403; unknown patient → 404. Once access is granted it returns **all** of that patient's admissions (every status + type, incl. DISCHARGED) with NO per-row filtering — the deliberate opposite of the global list. See docs/features/patient-admissions-history.md)
- ✅ Admission PDF Export (`GET /api/v1/admissions/{id}/export.pdf`, synchronous render via openhtmltopdf + PDFBox into a request-scoped OS temp directory, SHA-256 echoed in `X-Admission-Export-Sha256`, 500 MB pre-flight cap, jsoup rich-text sanitizer at render time, `ADMISSION_EXPORT` audit row written in `REQUIRES_NEW` tx — no server-side retention; gated by `admission:export-pdf` granted to ADMINISTRADOR/PERSONAL_ADMINISTRATIVO only)
- ✅ Discharge Protection (a `DISCHARGED` admission's record is **immutable / read-only**; every mutating endpoint returns `400 error.admission.discharged.records` — enforced at the service layer via the `validateAdmissionActive` pattern, so it holds even for ADMINISTRADOR and direct API calls. Extended (2026-06-01) from the original progress-note/nursing-note/vital-sign coverage to the **entire** record: clinical history create/update, medical order create/update + every state transition (authorize/emergency-authorize/reject/mark-in-progress/discontinue) + result-document upload/delete, psychotherapy activity create/delete, consent-document upload, admission-document upload/delete, and consulting-physician add/remove. Reads/lists/downloads/PDF export are never blocked. Per customer decision, **no exceptions** — lab results never arrive post-discharge and post-discharge orders are out of scope. Admission soft-delete (ADMINISTRADOR) is deliberately exempt — it removes an erroneous record, not record content. Frontend: `MedicalRecordHub` threads `admissionStatus` into every authoring section, hides all write affordances, and shows a `medicalRecord.dischargedReadOnly` banner. The cross-admission `/medical-orders` by-state dashboard also hides per-row actions for discharged admissions — `MedicalOrderListItemResponse` carries `admissionStatus` and the row-action predicates gate on it. See docs/features/discharge-protection.md)
- ✅ Warehouse-Scoped Inventory (Bodegas) (V119-V121; six seeded warehouses ADMINISTRACION/ENFERMERIA/MANTENIMIENTO_1/MANTENIMIENTO_2/COCINA/PSICOLOGIA; per-warehouse stock in `inventory_warehouse_stock` (NUMERIC) with the single catalog unchanged; **strict isolation** — medication administration dispenses from the caller's default warehouse (nurses → ENFERMERIA) and returns 422 `error.warehouse.out-of-stock` if empty there even when stock exists elsewhere; **warehouse-scoped FEFO**; atomic inter-warehouse transfers (one `inventory_transfers` row + two `inventory_movements`, `SELECT … FOR UPDATE` on source); maintenance/non-medical warehouse charges create a `PatientCharge` of type `SERVICE` via a `WarehouseChargeCreatedEvent` (AFTER_COMMIT, REQUIRES_NEW); per-warehouse low-stock and warehouse-scoped expiry report; new `MANTENIMIENTO` system role with assigned warehouses via `user_warehouses`; data-driven role → default-warehouse mapping (`role_default_warehouses`); audit actions `WAREHOUSE_TRANSFER`/`WAREHOUSE_CHARGE` in REQUIRES_NEW. Eight permissions `warehouse:read/create/update/delete`, `warehouse-transfer:create/read/receive`, `warehouse-charge:create`. See docs/features/warehouse-inventory-management.md)

- ✅ Laboratory Orders with Providers (V123-V126; a `LABORATORIOS` medical order is now a provider-aware multi-test requisition backed by an **admin-managed catalog** — `LabProvider`, canonical `LabTest`, per-provider `LabProviderTest` (own `displayName`/`cost`/`salesPrice`), and `LabPanel`/`LabPanelItem` presets. `LabCatalogService` + `/api/v1/lab/*` provide CRUD (soft delete) + `resolvePanel(panelId, providerId)` → matched provider-tests + `unmatchedTests`. Ordering reuses `MedicalOrderController`: `CreateMedicalOrderRequest` carries `labProviderId` + `labProviderTestIds`; `MedicalOrderService.buildLabLines` validates provider present (400), ≥1 line (400, AC8), all active (400, AC11), all belong to the order's provider (400, AC7), then **snapshots** `displayName`/`cost`/`salesPrice` into `MedicalOrderLabTest` lines so catalog edits never change a recorded order or its bill (AC12). Lines mutable only while `SOLICITADO` (AC14). On authorization `publishBillingEventIfNeeded` emits `LabOrderAuthorizedEvent` → `BillingService.createChargeFromLabOrder` creates **one** `ChargeType.LAB` charge **per test line** (itemized; summing to Σ line sales prices, zero-total guard). No legacy lab path: every `LABORATORIOS` order requires a provider + ≥1 line (AC15); editing a lab order to another category clears its provider/lines. The single-`inventoryItem` `MedicalOrderAuthorizedEvent` path remains only for `REFERENCIAS_MEDICAS` / `PRUEBAS_PSICOMETRICAS`. Discharge protection unchanged. Two permissions `lab-catalog:read` (ADMINISTRADOR/MEDICO/MEDICO_RESIDENTE) / `lab-catalog:manage` (ADMINISTRADOR). Dashboard/detail responses batch-load lab lines + a projected aggregate (no N+1). See docs/features/laboratory-orders-with-providers.md)

### Frontend (Complete)
- ✅ Vue 3.5 + TypeScript 5.9 + Vite 7.x
- ✅ Pinia stores: auth, user, role, audit, locale, notification
- ✅ PrimeVue 4.5 UI components
- ✅ vue-i18n for internationalization
- ✅ VeeValidate + Zod for form validation
- ✅ Axios with auth interceptors
- ✅ Dashboard shows admitted patients with role-based filtering (doctors see their patients, all other roles see all)
- ✅ Session expiration handling with modal notification, redirect preservation, and proactive token monitoring
- ✅ Vitest for unit testing (happy-dom, @vue/test-utils, @pinia/testing)
- ✅ Playwright for E2E testing
- ✅ Patient Admission UI (multi-step wizard, admin CRUD for triage codes and rooms)
- ✅ Psychotherapeutic Activities UI (activity list with sorting, admin category management)
- ✅ Inventory Management UI (item list with filtering/search, category admin CRUD, movement dialog, low stock report, room pricing)
- ✅ Billing UI (charge list with filtering, balance view with daily breakdown, invoice view with charge summary, create charge/adjustment dialogs)
- ✅ Medication Administration UI (administer dialog, history with pagination, status badges, inventory item linking on medical orders)
- ✅ Psychotherapy Category Pricing UI (price/cost fields on category form and list)
- ✅ Medical Order Document Attachments UI (upload dialog, thumbnail grid, document viewer, count badges on order cards)
- ✅ Nursing Kardex Dashboard UI (expandable patient cards, medication/vitals/care sections, quick-administer/record-vitals/add-note, auto-refresh, role-based routing from `/dashboard`)
- ✅ Medical Record Card Hub UI (the admission detail medical record is a card hub, not a tab bar: `AdmissionHeroHeader` gradient identity header + facts strip replaces the two info cards; `MedicalRecordHub` shows a responsive grid of `MedicalRecordSectionCard`s with live metrics + last-updated — no default-open section — each drilling into the section's full content with a back control; `ClinicalHistoryView` renders field groups as a card grid. Metrics are prefetched per visible section via `useMedicalRecordSummary` (`Promise.allSettled`, size=1, silent degradation). All prior ACs preserved — per-section permissions, server `canEdit`, discharge read-only, sanitized rich text, date helpers.)
- ✅ Treasury UI (bank accounts, expenses, income, employees, payroll, contractor payments)
- ✅ Doctor Fee Billing UI (fee list with summary, create form with net amount preview, invoice dialog, settle dialog, document upload, status-based actions)
- ✅ Bank Statement Reconciliation UI (statement list with progress, upload dialog, column mapping config, reconciliation view with color-coded rows, confirm/reject/acknowledge/create expense/income actions, complete statement)
- ✅ Admissions list cards view (cards-only — the table view was removed) with two-level additive grouping (primary "Group by" + "Then by" across None/Sex/Type/Triage; the second level excludes the primary dimension and is hidden when primary = None), persisted per user via localStorage (`hms.admissionsListView.v2`, `{primaryGroupBy, secondaryGroupBy}`; Dashboard and Admissions screen share preferences)
- ✅ Bed Occupancy View UI (`/bed-occupancy`: per-room cards with one bed slot per capacity, summary header, filters by sex/type/status, search by room number or patient name, 30s auto-refresh paused on hidden tab, "Admit here" deep-link that pre-selects the room in the admission wizard via `?roomId=`)
- ✅ Patient Admissions History UI (`PatientAdmissionsHistory.vue` section on `PatientDetailView`: paged table of the patient's admissions most-recent-first via the dedicated `patientAdmissions` store ref — kept separate from the global `admissions` ref to avoid cross-view bleed; shows admission/discharge `formatDateTime` or an "Active" `Tag`, reused `AdmissionTypeBadge`, room, treating physician; loading/empty/error states + `Paginator`; rows navigate to `/admissions/:id` only when the user holds `admission:read`, otherwise render non-navigable)
- ✅ Medical Orders by State dashboard (`/medical-orders`): cross-admission listing with status / category / date filters, contextual action buttons per state (authorize, reject, emergency-authorize, mark-in-progress, upload result document, discontinue), state badge with color mapping per state shape
- ✅ Admission PDF Export button (`AdmissionExportButton` on `AdmissionDetailView`, hidden when the user lacks `admission:export-pdf`, downloads via blob with filename from `Content-Disposition`, 401/403/404/413 mapped through `useErrorHandler`)
- ✅ Pharmacy bounded context (`/pharmacy` list with section tabs and review-status badge; `/pharmacy/medications/:id` detail view with `LotListPanel`; `/pharmacy/expiry-report` color-coded dashboard; `MedicationFormDialog` creates item + details atomically; `LotFormDialog` with recall toggle gated by `inventory-lot:update`; `ExpiryStatusChip` reused across dashboard/kardex/lot panel; `MedicationAdministrationDialog` shows FEFO preview chip + quantity > 1 + admin lot override; `InventoryItemsView` gains kind filter and routes DRUG rows to the pharmacy detail page; side-nav Pharmacy section gated by `medication:read`). Initial workbook catalog (~615 SKUs) is loaded once by SQL migration V111 — there is no bulk-import UI; the catalog is maintained manually after the initial load. FEFO previews and stock figures are now warehouse-scoped.
- ✅ Warehouse-Scoped Inventory UI (Bodegas) (side-nav "Bodegas" section gated by `warehouse:read`; `WarehouseList` admin CRUD; `WarehouseStockView` per-warehouse catalog with search + low-stock filter; `TransferFormDialog` / `TransferListView` for inter-warehouse transfers with source-scope-aware dropdowns; `WarehouseChargeDialog` + `MaintenanceDashboardView` for the maintenance "charge a broken towel to an admission" flow; `UserWarehousesAssignmentField` multi-select on user-edit when the user has the MANTENIMIENTO role; warehouse-scope dispense error surfaced inline on the medication administration dialog)

- ✅ Laboratory Orders with Providers UI (`MedicalOrderFormDialog` LABORATORIOS branch: replaces the single inventory `<Select>` with provider `<Select>` → that provider's tests `<MultiSelect display="chip">` (label = test `displayName` only — **prices are not shown to the ordering clinician**) → an **Apply panel** control (panel `<Select>` + button → `resolvePanel` → merges matched ids, non-blocking `<Message severity="warn">` listing the provider + unmatched test names); the lab branch **hides** the start-date / end-date / *Horario* fields (a requisition has no treatment window — `startDate` is auto-set to the request date) and shows **no** per-test price or running total; provider change prunes selections not offered by the new provider (one order = one provider); edit-after-authorize disables all lab inputs (AC14); `.superRefine` requires provider + ≥1 test when `category === LABORATORIOS`. Read side: `MedicalOrderCard` renders provider + test chips (no total) with a legacy `inventoryItemName` fallback; the by-state dashboard summary cell shows provider + test count (no total) from projected list fields. Prices stay a finance concern — snapshotted per line and used for charge creation, edited via the admin catalog. Admin catalog at `/lab/catalog` (gated by `lab-catalog:read`, NOT isAdmin — MEDICO/MEDICO_RESIDENTE need it): `LabCatalogView` Tabs → `LabProviderList` / `LabTestList` / `LabProviderTestPanel` (per-provider CRUD with `InputNumber` currency) / `LabPanelList` + `LabPanelFormDialog`; mutate affordances gated by `lab-catalog:manage`. `useLabCatalogStore` unwraps `ApiResponse`, caches provider-tests per provider. Side-nav "Lab Catalog" (`pi-flask`). See docs/features/laboratory-orders-with-providers.md)

### Pharmacy / Lot module (Backend additions)
- ✅ `InventoryItem` extended with `kind` (DRUG/SUPPLY/EQUIPMENT/SERVICE/PERSONNEL/FOOD), `sku` (unique when set), `lotTrackingEnabled` flag
- ✅ `MedicationDetails` 1:1 satellite — generic name, commercial name, strength, dosage form, route, controlled flag, section, review status (`CONFIRMED`/`NEEDS_REVIEW`)
- ✅ `InventoryLot` 1:N satellite — lot number, expiration date, quantity on hand, supplier, recalled flag, `syntheticLegacy` flag for legacy backfill
- ✅ `InventoryMovementService` extracted from `InventoryItemService` with scalar branch (legacy atomic update) and lot branch (FEFO `SELECT FOR UPDATE` on EXIT, find-or-create upsert on ENTRY, then `recomputeQuantityFromLots`) — now warehouse-aware: every EXIT/ENTRY resolves a warehouse and FEFO runs only against lots in that warehouse (see Warehouse-Scoped Inventory)
- ✅ `PharmacyService` — composes item + details, atomic create, list with section/controlled/search filters
- ✅ `MedicationDetailsService` — auto-transitions `NEEDS_REVIEW` → `CONFIRMED` on edit (AC-18)
- ✅ `InventoryLotService` — create/update/recall/soft-delete; 409 if movements reference the lot
- ✅ `ExpiryReportService` — single read; EXPIRED/RED/YELLOW/GREEN/NO_EXPIRY classification; totals
- ✅ `MedicationNameParser` (V105 backfill) + `MedicationExpirationParser` (MM/YY, MM/YYYY, dd/MM/yyyy)
- ✅ `RecalledLotStockCheckJob` — daily `@Scheduled` 03:30 cron, flags warehouse stock still sitting on recalled lots to `audit_logs` with `status='FAILED'` (no auto-heal; replaces the old scalar quantity-drift check now that stock is per-warehouse)

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

**Last Updated**: June 9, 2026

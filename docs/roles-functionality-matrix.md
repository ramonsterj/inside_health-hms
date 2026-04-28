# Roles and Functionality Matrix

## Scope

This matrix describes the shipped system roles in the codebase today.

It is based on:

- backend authorization checks in the API controllers and services
- default role grants in `api/src/main/resources/db/seed/R__seed_01_reset_and_base.sql`
- versioned role and permission migrations in `api/src/main/resources/db/migration`
- frontend route and menu gating in `web/src/router/index.ts` and `web/src/layout/AppMenu.vue`

Custom roles can be created in the platform, so this document only covers the built-in roles and the default grants that ship with the project.

## Legend

| Value | Meaning |
| --- | --- |
| Full | Create, update, delete, and manage the feature area |
| Work | Day-to-day operational use, but not full system administration |
| Read | View or use existing records only |
| Scoped | Access is limited by extra business rules beyond the raw permission |
| Self | Own profile/account only |
| None | No shipped access |
| * | Has `user:read` at the API level, but the frontend hides the screen behind `requiresAdmin` |
| ** | Granted by `V084__seed_treasury_permissions.sql` but lost after reseed — see Mismatches section |

## Matrix

| Functionality | ADMIN | ADMINISTRATIVE_STAFF | DOCTOR | PSYCHOLOGIST | NURSE | CHIEF_NURSE | USER | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Own profile, password, locale | Self | Self | Self | Self | Self | Self | Self | `/api/users/me` endpoints are available to any authenticated user. |
| User administration | Full | None* | None* | None | None* | None* | None* | Frontend user-management screen is admin-only. |
| Role administration | Full | None | None | None | None | None | None | Frontend role-management screen is admin-only. |
| Audit logs | Read | None | None | None | None | None | None | Frontend audit-log screen is admin-only. |
| Patients | Full | Work | Scoped | Scoped | Read | Work | None | Staff can create/update. Doctors are scoped to assigned patients (treating or consulting physician) on patient list/detail. Psychologists are limited to patients with active admissions on patient list/detail; psychologists also have `patient:update`. |
| Patient ID documents | Full | Work | None | None | None | None | None | Staff can upload, view, and delete because delete reuses `patient:upload-id`. |
| Admissions | Full | Work | Scoped | Scoped | Read | Work | None | Staff and chief nurse can update and discharge. Doctor admission lists are scoped to treating or consulting physician. Psychologist lists/search/detail are active-only. |
| Admission consent document | Full | Work | Read | Read | Read | Read | None | Download endpoint is permission-based only. |
| Admission documents | Full | Work | Read | Read | Read | Read | None | Staff can upload; clinical roles can view/download. |
| Clinical history | Full | None | Work | None | Read | Read | None | Only admin has default update permission. Doctors can create/read. Nurses and chief nurses are read-only. |
| Progress notes | Full | None | Work | None | Work | Work | None | Chief nurse adds update permission. |
| Medical orders | Full | Read | Work | None | Read | Read | None | Doctors can create and discontinue. Only admin has default `medical-order:update`. Initial state is category-driven: directive categories (diet / mobility / etc.) start in `ACTIVA`; auth-required categories (medications / labs / referrals / psychometric) start in `SOLICITADO`. Administrative staff gains read access in order to authorize. |
| Medical-order authorization | Full | Work | None | None | None | None | None | `medical-order:authorize` lets the role transition `SOLICITADO → AUTORIZADO` or `NO_AUTORIZADO`. Only valid for auth-required categories. Granted to admin and administrative staff by default. |
| Medical-order emergency authorize | Full | None | Work | None | None | None | None | `medical-order:emergency-authorize` lets a doctor self-authorize an order without admin staff (after-hours, patient in crisis, family unreachable). Requires a structured reason enum + optional note (required when reason is OTHER). Stamps `emergency_*` audit fields and triggers billing the same as normal authorize. Granted to admin and doctor by default. |
| Medical-order mark in progress | Full | None | Work | None | Work | Work | None | `medical-order:mark-in-progress` transitions `AUTORIZADO → EN_PROCESO` for labs / referrals / psychometric tests (sample taken / patient referred / test administered). Owns the nursing worklist. After this transition, discontinue is no longer allowed — the action has been initiated externally. The `RESULTADOS_RECIBIDOS` terminal state is reached implicitly when a result document is uploaded. Granted to admin, nurse, doctor, and chief nurse by default. |
| Medical-order documents | Full | None | Work | None | Work | Read | None | Doctor and nurse can upload. Uploading on `AUTORIZADO` or `EN_PROCESO` auto-transitions to `RESULTADOS_RECIBIDOS`. Chief nurse can read/download through `medical-order:read` but cannot upload by default. There is no separate download permission; download is gated by `medical-order:read`. |
| Medical orders by state (cross-admission dashboard) | Full | Read | Read | None | Read | Read | None | Top-level screen at `/medical-orders`. Visible to any role with `medical-order:read`. Default filter selects the action-needed buckets (`SOLICITADO`, `AUTORIZADO`, `EN_PROCESO`). Action buttons inside the screen are still gated by the underlying permission (authorize / emergency-authorize / mark-in-progress / discontinue / upload-document). |
| Nursing notes | Full | None | Scoped | None | Scoped | Scoped | None | Non-admin edits are limited to the author, within 24 hours, while the admission is active. |
| Vital signs | Full | None | Scoped | None | Scoped | Scoped | None | Same edit restrictions as nursing notes. |
| Medication administration | Full | None | Read | None | Work | Work | None | Create is limited to medication orders on active admissions and requires an inventory-linked order for `GIVEN`. |
| Psychotherapy activities | Full | None | Read | Work | Read | Read | None | Create also requires the actual `PSYCHOLOGIST` role and a hospitalization admission. Delete is effectively admin-only in shipped roles. |
| Psychotherapy categories | Full | None | Read | Read | Read | Read | None | Category management is admin-only by default. |
| Billing charges and balances | Full | Read | Read | Read | Read | Read | None | Manual charges and adjustments are admin-only in shipped roles. Staff and psychologist read access comes from the seed file, not the versioned billing migration. |
| Invoices | Full | Read | None | None | None | None | None | Invoice generation is admin-only in shipped roles. Staff invoice-read access comes from the seed file, not the versioned billing migration. |
| Triage codes | Full | Read | Read | Read | Read | Read | None | Management is admin-only by default. |
| Rooms | Full | Read | Read | Read | Read | Read | None | Management is admin-only by default. |
| Document types | Full | Read | None | None | None | None | None | Routes and views exist, but there is no sidebar entry for document types. |
| Inventory categories, items, and stock movements | Full | None | None | None | None | None | None | Inventory movement records can still be created indirectly by medication administration flows. |
| Treasury operations | Full | Full** | None | None | None | None | None | Bank accounts, expenses, income, employees, payroll, and doctor fees. |
| Treasury reports | Full | Full** | None | None | None | None | None | Dashboard plus monthly, upcoming, bank summary, compensation, indemnizacion, and reconciliation summary reports. |
| Treasury reconciliation | Full | Full** | None | None | None | None | None | Bank statements and reconciliation workflow. |

## Important Enforcement Notes

- The frontend treats `ADMIN` as a superuser. `web/src/stores/auth.ts` returns `true` for any permission check when the user has the `ADMIN` role.
- `/users`, `/roles`, and `/audit-logs` are gated in the frontend by `requiresAdmin`, not just by raw permissions.
- Nurses and chief nurses are redirected from `/dashboard` to `/nursing-kardex`.
- The nursing kardex itself is keyed off `admission:read`, so staff, doctors, psychologists, and admin can also reach it if they navigate there.
- Doctor scoping is partial, not universal. Patient list/detail and admission list are scoped to the treating physician **or** consulting physicians, but `AdmissionService.getAdmission()` does not enforce physician scoping on direct admission detail fetches.
- Psychologist scoping is also partial. The code enforces active-only behavior for patient list/detail, admission list/detail, patient search, patient summary, and consulting physician list, but the admission document and consent download endpoints are only permission-gated.
- Nursing note and vital-sign updates are stricter than the raw permission name suggests. Non-admin users can only edit their own records, within 24 hours, and only while the admission remains active.
- Progress note updates are broader than nursing/vitals updates. If a role has `progress-note:update`, the service does not add author or time-window restrictions.
- Medical-order updates are not part of the default doctor, nurse, or chief-nurse roles. Doctors can create and discontinue orders, but only admin has the default update permission.
- Medical orders carry a category-driven workflow state. Directive categories (e.g. `DIETA`) start in `ACTIVA` and skip authorization. Auth-required categories (`MEDICAMENTOS`, `LABORATORIOS`, `REFERENCIAS_MEDICAS`, `PRUEBAS_PSICOMETRICAS`) start in `SOLICITADO`. Authorization (`SOLICITADO → AUTORIZADO`/`NO_AUTORIZADO`) is admin/administrative-staff only. Doctors can use emergency-authorize (`medical-order:emergency-authorize`) to self-authorize crisis or after-hours orders, with a structured reason captured in the audit trail. For results-bearing categories, nursing/admin/doctor can mark an `AUTORIZADO` order `EN_PROCESO` (sample taken / patient referred / test administered); after that, discontinue is no longer allowed. The `RESULTADOS_RECIBIDOS` terminal state cannot be set manually — it is reached only by uploading a result document, which auto-transitions the order in the same transaction (skipping `EN_PROCESO` if necessary). Discontinue (`DESCONTINUADO`) is allowed only from `ACTIVA`, `SOLICITADO`, or `AUTORIZADO`.
- Chief nurse is not just a label. In the shipped roles it extends nurse access with `patient:update`, `admission:update`, and `progress-note:update`.

## Mismatches and Gaps To Track

- `USER` role mismatch: `V007__remove_user_read_from_user_role.sql` removes `user:read` from `USER`, but `R__seed_01_reset_and_base.sql` adds it back during reseed. In seeded environments the `USER` role can call the `/api/users` API, but the frontend still hides user administration behind `requiresAdmin` so it has no practical UI effect.
- `ADMINISTRATIVE_STAFF` billing mismatch: the seed file grants `billing:read` and `invoice:read`, but the versioned billing migration only grants billing read to doctor and nurse plus full billing/invoice access to admin.
- `ADMINISTRATIVE_STAFF` treasury mismatch: `V084__seed_treasury_permissions.sql` grants full treasury access to staff, but the reseed file rebuilds role permissions without replaying those treasury grants. In seeded dev or acceptance environments, treasury access can disappear after reseed.
- Document types are reachable by route and permission, but `web/src/layout/AppMenu.vue` does not expose a sidebar link for them.
- `CHIEF_NURSE` cannot upload medical-order documents despite being a nurse-plus role. `V073__add_medical_order_document_permissions.sql` grants `medical-order:upload-document` to `NURSE` but not `CHIEF_NURSE`.
- Psychotherapy activity create enforces the `PSYCHOLOGIST` role name at the service level (`PsychotherapyActivityService`), not just the `psychotherapy-activity:create` permission. A custom role with that permission but without the `PSYCHOLOGIST` role name would be rejected.

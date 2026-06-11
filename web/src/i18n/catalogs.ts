/**
 * Catalog of stable backend codes that the "render coded reference data through
 * i18n keys" standard covers (see CLAUDE.md → i18n / Reference-Data Labels and
 * docs/architecture/I18N.md).
 *
 * This is the frontend half of the two automated guards:
 *   1. catalogs.spec.ts asserts every entry below has its required key in BOTH
 *      locale bundles (fast, no DB; doubles as documentation).
 *   2. The backend Testcontainers coverage test (I18nReferenceDataCoverageTest)
 *      asserts the live DB code set EQUALS the catalog below, closing the
 *      DB ↔ catalog ↔ i18n loop so a new role/permission/warehouse cannot ship
 *      without translations.
 *
 * Keep these lists in sync with the seeded reference data. When you add a system
 * role, permission, or warehouse in a migration, add its code here and its key(s)
 * to es.json + en.json — the guards will fail until you do.
 */

// roles.code — the 10 system roles (Spanish codes after V127).
export const ROLE_CODES = [
  'ADMINISTRADOR',
  'USUARIO',
  'PERSONAL_ADMINISTRATIVO',
  'MEDICO',
  'ENFERMERO',
  'JEFE_ENFERMERIA',
  'PSICOLOGO',
  'MEDICO_RESIDENTE',
  'AUXILIAR_ENFERMERIA',
  'MANTENIMIENTO'
] as const

// permissions.code — every seeded permission (medication:bulk-import removed by V112).
export const PERMISSION_CODES = [
  // user
  'user:create',
  'user:read',
  'user:update',
  'user:delete',
  'user:reset-password',
  'user:list-deleted',
  'user:restore',
  // role
  'role:create',
  'role:read',
  'role:update',
  'role:delete',
  'role:assign-permissions',
  // audit
  'audit:read',
  // patient
  'patient:create',
  'patient:read',
  'patient:update',
  'patient:upload-id',
  'patient:view-id',
  'patient:delete',
  // triage-code
  'triage-code:create',
  'triage-code:read',
  'triage-code:update',
  'triage-code:delete',
  // room
  'room:create',
  'room:read',
  'room:update',
  'room:delete',
  'room:occupancy-view',
  // admission
  'admission:create',
  'admission:read',
  'admission:update',
  'admission:discharge',
  'admission:delete',
  'admission:upload-consent',
  'admission:view-consent',
  'admission:view-documents',
  'admission:upload-documents',
  'admission:download-documents',
  'admission:delete-documents',
  'admission:export-pdf',
  // document-type
  'document-type:create',
  'document-type:read',
  'document-type:update',
  'document-type:delete',
  // clinical-history
  'clinical-history:create',
  'clinical-history:read',
  'clinical-history:update',
  // progress-note
  'progress-note:create',
  'progress-note:read',
  'progress-note:update',
  // medical-order
  'medical-order:create',
  'medical-order:read',
  'medical-order:update',
  'medical-order:discontinue',
  'medical-order:upload-document',
  'medical-order:delete-document',
  'medical-order:authorize',
  'medical-order:emergency-authorize',
  'medical-order:mark-in-progress',
  // nursing-note
  'nursing-note:read',
  'nursing-note:create',
  'nursing-note:update',
  // vital-sign
  'vital-sign:read',
  'vital-sign:create',
  'vital-sign:update',
  // psychotherapy-activity
  'psychotherapy-activity:create',
  'psychotherapy-activity:read',
  'psychotherapy-activity:delete',
  // psychotherapy-category
  'psychotherapy-category:create',
  'psychotherapy-category:read',
  'psychotherapy-category:update',
  'psychotherapy-category:delete',
  // inventory-category
  'inventory-category:create',
  'inventory-category:read',
  'inventory-category:update',
  'inventory-category:delete',
  // inventory-item
  'inventory-item:create',
  'inventory-item:read',
  'inventory-item:update',
  'inventory-item:delete',
  // inventory-movement
  'inventory-movement:create',
  'inventory-movement:read',
  // inventory-lot
  'inventory-lot:read',
  'inventory-lot:create',
  'inventory-lot:update',
  // billing
  'billing:read',
  'billing:create',
  'billing:adjust',
  'billing:configure',
  // invoice
  'invoice:read',
  'invoice:create',
  // medication-administration
  'medication-administration:create',
  'medication-administration:read',
  // medication
  'medication:read',
  'medication:create',
  'medication:update',
  'medication:expiry-report',
  // treasury
  'treasury:read',
  'treasury:write',
  'treasury:delete',
  'treasury:configure',
  'treasury:reconcile',
  'treasury:report',
  // warehouse
  'warehouse:read',
  'warehouse:create',
  'warehouse:update',
  'warehouse:delete',
  // warehouse-transfer
  'warehouse-transfer:create',
  'warehouse-transfer:read',
  'warehouse-transfer:receive',
  // warehouse-charge
  'warehouse-charge:create',
  // lab-catalog
  'lab-catalog:read',
  'lab-catalog:manage'
] as const

// distinct permissions.resource — used for the permission-group headers.
export const PERMISSION_RESOURCES = [
  'user',
  'role',
  'audit',
  'patient',
  'triage-code',
  'room',
  'admission',
  'document-type',
  'clinical-history',
  'progress-note',
  'medical-order',
  'nursing-note',
  'vital-sign',
  'psychotherapy-activity',
  'psychotherapy-category',
  'inventory-category',
  'inventory-item',
  'inventory-movement',
  'inventory-lot',
  'billing',
  'invoice',
  'medication-administration',
  'medication',
  'treasury',
  'warehouse',
  'warehouse-transfer',
  'warehouse-charge',
  'lab-catalog'
] as const

// warehouses.code — the 6 seeded warehouses (V119).
export const WAREHOUSE_CODES = [
  'ADMINISTRACION',
  'ENFERMERIA',
  'MANTENIMIENTO_1',
  'MANTENIMIENTO_2',
  'COCINA',
  'PSICOLOGIA'
] as const

// document_types.code — the 7 seeded document types (V031).
export const DOCUMENT_TYPE_CODES = [
  'CONSENT_ADMISSION',
  'CONSENT_ISOLATION',
  'CONSENT_RESTRAINT',
  'CONSENT_SEDATION',
  'INVENTORY_LIST',
  'INVENTORY_PHOTO',
  'OTHER'
] as const

// triage_codes.code — the 5 seeded triage codes (V021).
export const TRIAGE_CODES = ['A', 'B', 'C', 'D', 'E'] as const

import { useI18n } from 'vue-i18n'

/**
 * Single entry point for the "render coded reference data through i18n keys,
 * never the raw DB value" standard (see CLAUDE.md → i18n / Reference-Data Labels
 * and docs/architecture/I18N.md).
 *
 * Any label derived from a stable backend code (role code, permission code,
 * permission resource, document-type code, triage code, warehouse code, …) MUST
 * be rendered through one of these helpers. The DB `name`/`description` is a
 * fallback ONLY — used for admin-created rows that are not yet in the bundle.
 *
 * Keys may contain `:` (e.g. `permissions.user:create.name`). vue-i18n only
 * splits on `.`, so the `:` stays inside a single path segment and resolves
 * against the nested `"permissions": { "user:create": { "name", "description" } }`
 * structure. The two automated guards (backend coverage test + frontend catalog
 * vitest) keep the bundles complete.
 */
export function useCodeLabels() {
  const { t, te } = useI18n()

  // Generic helper: `${namespace}.${code}`, te()-checked, DB value as fallback.
  function codeLabel(namespace: string, code: string, fallback?: string): string {
    const key = `${namespace}.${code}`
    return te(key) ? t(key) : (fallback ?? code)
  }

  // Two-part helper for codes that carry a name AND a description under the same
  // node (permissions). `${namespace}.${code}.${field}`.
  function codeField(namespace: string, code: string, field: string, fallback?: string): string {
    const key = `${namespace}.${code}.${field}`
    return te(key) ? t(key) : (fallback ?? code)
  }

  return {
    codeLabel,

    // Roles — roleNames.<CODE> / roleDescriptions.<CODE>
    roleName: (code: string, fallback?: string) => codeLabel('roleNames', code, fallback),
    roleDescription: (code: string, fallback?: string) =>
      codeLabel('roleDescriptions', code, fallback),

    // Permissions — permissions.<code>.{name,description}
    permissionName: (code: string, fallback?: string) =>
      codeField('permissions', code, 'name', fallback),
    permissionDescription: (code: string, fallback?: string) =>
      codeField('permissions', code, 'description', fallback),

    // Permission-group header — roles.permissionGroups.<resource>; falls back to
    // a capitalized resource name (matching the prior inline behaviour).
    permissionGroupLabel: (resource: string): string => {
      const key = `roles.permissionGroups.${resource}`
      return te(key) ? t(key) : resource.charAt(0).toUpperCase() + resource.slice(1)
    },

    // Document types — document.types.<code> (name) / document.typeDescriptions.<code>
    documentTypeName: (code: string, fallback?: string) =>
      codeLabel('document.types', code, fallback),
    documentTypeDescription: (code: string, fallback?: string) =>
      codeLabel('document.typeDescriptions', code, fallback),

    // Triage codes — triageCode.codes.<code>
    triageCodeLabel: (code: string, fallback?: string) =>
      codeLabel('triageCode.codes', code, fallback),

    // Warehouses — warehouse.names.<CODE> / warehouse.descriptions.<CODE>
    warehouseName: (code: string, fallback?: string) =>
      codeLabel('warehouse.names', code, fallback),
    warehouseDescription: (code: string, fallback?: string) =>
      codeLabel('warehouse.descriptions', code, fallback)
  }
}

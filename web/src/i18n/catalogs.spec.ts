import { describe, it, expect } from 'vitest'
import enJson from './locales/en.json'
import esJson from './locales/es.json'
import {
  ROLE_CODES,
  PERMISSION_CODES,
  PERMISSION_RESOURCES,
  WAREHOUSE_CODES,
  DOCUMENT_TYPE_CODES,
  TRIAGE_CODES
} from './catalogs'

/**
 * Frontend half of the reference-data i18n guard (see CLAUDE.md → i18n /
 * Reference-Data Labels). For every stable backend code in catalogs.ts, asserts
 * the required i18n key exists and is non-empty in BOTH locale bundles. Fast,
 * no DB — the backend coverage test additionally proves the catalog matches the
 * live DB code set.
 */

const locales: Record<string, unknown> = { en: enJson, es: esJson }

// Resolve a literal-segment path (segments may contain ':' — they are NOT split).
function lookup(root: unknown, segments: string[]): unknown {
  let node: unknown = root
  for (const seg of segments) {
    if (node && typeof node === 'object' && seg in (node as Record<string, unknown>)) {
      // eslint-disable-next-line security/detect-object-injection -- seg is a fixed catalog code, not user input
      node = (node as Record<string, unknown>)[seg]
    } else {
      return undefined
    }
  }
  return node
}

function expectNonEmptyString(root: unknown, segments: string[], label: string) {
  const value = lookup(root, segments)
  expect(typeof value, `${label} → expected a string at ${segments.join(' › ')}`).toBe('string')
  expect((value as string).trim().length, `${label} is empty`).toBeGreaterThan(0)
}

for (const [lang, root] of Object.entries(locales)) {
  describe(`reference-data i18n coverage (${lang}.json)`, () => {
    it('has roleNames + roleDescriptions for every system role', () => {
      for (const code of ROLE_CODES) {
        expectNonEmptyString(root, ['roleNames', code], `${lang} roleNames.${code}`)
        expectNonEmptyString(root, ['roleDescriptions', code], `${lang} roleDescriptions.${code}`)
      }
    })

    it('has permissions.name + permissions.description for every permission', () => {
      for (const code of PERMISSION_CODES) {
        expectNonEmptyString(
          root,
          ['permissions', code, 'name'],
          `${lang} permissions.${code}.name`
        )
        expectNonEmptyString(
          root,
          ['permissions', code, 'description'],
          `${lang} permissions.${code}.description`
        )
      }
    })

    it('has a permission-group label for every resource', () => {
      for (const resource of PERMISSION_RESOURCES) {
        expectNonEmptyString(
          root,
          ['roles', 'permissionGroups', resource],
          `${lang} roles.permissionGroups.${resource}`
        )
      }
    })

    it('has warehouse names + descriptions for every warehouse', () => {
      for (const code of WAREHOUSE_CODES) {
        expectNonEmptyString(root, ['warehouse', 'names', code], `${lang} warehouse.names.${code}`)
        expectNonEmptyString(
          root,
          ['warehouse', 'descriptions', code],
          `${lang} warehouse.descriptions.${code}`
        )
      }
    })

    it('has document-type names + descriptions for every document type', () => {
      for (const code of DOCUMENT_TYPE_CODES) {
        expectNonEmptyString(root, ['document', 'types', code], `${lang} document.types.${code}`)
        expectNonEmptyString(
          root,
          ['document', 'typeDescriptions', code],
          `${lang} document.typeDescriptions.${code}`
        )
      }
    })

    it('has a label for every triage code', () => {
      for (const code of TRIAGE_CODES) {
        expectNonEmptyString(
          root,
          ['triageCode', 'codes', code],
          `${lang} triageCode.codes.${code}`
        )
      }
    })

    // Reverse direction: no bundle key may exist outside the catalog, so the
    // catalog stays a faithful mirror of the bundle (and, via the backend
    // coverage test which proves bundle == DB, of the live DB code set).
    it('has no reference-data keys beyond the catalog', () => {
      const keysAt = (segments: string[]) => {
        const node = lookup(root, segments)
        return node && typeof node === 'object' ? Object.keys(node as object) : []
      }
      expect(keysAt(['roleNames']).sort()).toEqual([...ROLE_CODES].sort())
      expect(keysAt(['roleDescriptions']).sort()).toEqual([...ROLE_CODES].sort())
      expect(keysAt(['permissions']).sort()).toEqual([...PERMISSION_CODES].sort())
      expect(keysAt(['roles', 'permissionGroups']).sort()).toEqual([...PERMISSION_RESOURCES].sort())
      expect(keysAt(['warehouse', 'names']).sort()).toEqual([...WAREHOUSE_CODES].sort())
      expect(keysAt(['warehouse', 'descriptions']).sort()).toEqual([...WAREHOUSE_CODES].sort())
      expect(keysAt(['document', 'types']).sort()).toEqual([...DOCUMENT_TYPE_CODES].sort())
      expect(keysAt(['document', 'typeDescriptions']).sort()).toEqual(
        [...DOCUMENT_TYPE_CODES].sort()
      )
      expect(keysAt(['triageCode', 'codes']).sort()).toEqual([...TRIAGE_CODES].sort())
    })
  })
}

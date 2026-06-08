import { describe, it, expect } from 'vitest'
import enJson from './locales/en.json'
import esJson from './locales/es.json'
// Backend message bundles imported as raw strings (vite ?raw). Keeping the check
// here means a single `npm run test:run` guards both frontend and backend parity.
import enProps from '../../../api/src/main/resources/messages/messages.properties?raw'
import esProps from '../../../api/src/main/resources/messages/messages_es.properties?raw'
import enErrors from '../../../api/src/main/resources/messages/errors.properties?raw'
import esErrors from '../../../api/src/main/resources/messages/errors_es.properties?raw'
import enValidation from '../../../api/src/main/resources/messages/validation.properties?raw'
import esValidation from '../../../api/src/main/resources/messages/validation_es.properties?raw'

/**
 * Guards that every user-facing label exists in BOTH languages and stays that way.
 * A missing translation (frontend or backend) becomes a red build.
 *
 * Checks key presence + non-emptiness only — NOT value inequality, so legitimate
 * cross-locale cognates / proper nouns ("Oral", "mmHg", "Total", "Bono 14", …) are fine.
 */

// Recursively flatten a nested locale object to its leaf key paths -> values.
function flatten(
  obj: unknown,
  prefix = '',
  out: Map<string, string> = new Map()
): Map<string, string> {
  if (obj && typeof obj === 'object' && !Array.isArray(obj)) {
    for (const [key, value] of Object.entries(obj as Record<string, unknown>)) {
      flatten(value, prefix ? `${prefix}.${key}` : key, out)
    }
  } else {
    out.set(prefix, String(obj))
  }
  return out
}

function symmetricDiff(a: Set<string>, b: Set<string>): { onlyInA: string[]; onlyInB: string[] } {
  return {
    onlyInA: [...a].filter(k => !b.has(k)).sort(),
    onlyInB: [...b].filter(k => !a.has(k)).sort()
  }
}

// Parse a Java .properties bundle into key -> value (skip blank lines and # comments).
function parseProperties(content: string): Map<string, string> {
  const out = new Map<string, string>()
  for (const rawLine of content.split(/\r?\n/)) {
    const line = rawLine.trim()
    if (!line || line.startsWith('#') || line.startsWith('!')) continue
    const eq = line.indexOf('=')
    if (eq === -1) continue
    out.set(line.slice(0, eq).trim(), line.slice(eq + 1).trim())
  }
  return out
}

function emptyValueKeys(map: Map<string, string>): string[] {
  return [...map.entries()].filter(([, v]) => v.trim() === '').map(([k]) => k)
}

describe('frontend locale parity (en.json ⇄ es.json)', () => {
  const en = flatten(enJson)
  const es = flatten(esJson)

  it('has exactly the same leaf keys in both locales', () => {
    const { onlyInA: onlyInEn, onlyInB: onlyInEs } = symmetricDiff(
      new Set(en.keys()),
      new Set(es.keys())
    )
    expect(
      { onlyInEn, onlyInEs },
      `Locale key drift between en.json and es.json:\n` +
        `  Keys only in en.json: ${onlyInEn.join(', ') || '(none)'}\n` +
        `  Keys only in es.json: ${onlyInEs.join(', ') || '(none)'}`
    ).toEqual({ onlyInEn: [], onlyInEs: [] })
  })

  it('has no empty leaf values in en.json', () => {
    const empty = emptyValueKeys(en)
    expect(empty, `Empty values in en.json: ${empty.join(', ')}`).toEqual([])
  })

  it('has no empty leaf values in es.json', () => {
    const empty = emptyValueKeys(es)
    expect(empty, `Empty values in es.json: ${empty.join(', ')}`).toEqual([])
  })
})

function describePropertiesParity(
  bundleName: string,
  enContent: string,
  esContent: string,
  enFile: string,
  esFile: string
) {
  describe(`backend message bundle parity (${enFile} ⇄ ${esFile})`, () => {
    const en = parseProperties(enContent)
    const es = parseProperties(esContent)

    it('has exactly the same keys in both bundles', () => {
      const { onlyInA: onlyInEn, onlyInB: onlyInEs } = symmetricDiff(
        new Set(en.keys()),
        new Set(es.keys())
      )
      expect(
        { onlyInEn, onlyInEs },
        `${bundleName} key drift:\n` +
          `  Keys only in ${enFile}: ${onlyInEn.join(', ') || '(none)'}\n` +
          `  Keys only in ${esFile}: ${onlyInEs.join(', ') || '(none)'}`
      ).toEqual({ onlyInEn: [], onlyInEs: [] })
    })

    it('has no empty values in either bundle', () => {
      expect({ emptyEn: emptyValueKeys(en), emptyEs: emptyValueKeys(es) }).toEqual({
        emptyEn: [],
        emptyEs: []
      })
    })
  })
}

describePropertiesParity(
  'Message bundle',
  enProps,
  esProps,
  'messages.properties',
  'messages_es.properties'
)
describePropertiesParity(
  'Error bundle',
  enErrors,
  esErrors,
  'errors.properties',
  'errors_es.properties'
)
describePropertiesParity(
  'Validation bundle',
  enValidation,
  esValidation,
  'validation.properties',
  'validation_es.properties'
)

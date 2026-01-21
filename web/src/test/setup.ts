import { vi } from 'vitest'

// Mock localStorage
const localStorageMock = (() => {
  const store = new Map<string, string>()
  return {
    getItem: vi.fn((key: string) => store.get(key) ?? null),
    setItem: vi.fn((key: string, value: string) => {
      store.set(key, value)
    }),
    removeItem: vi.fn((key: string) => {
      store.delete(key)
    }),
    clear: vi.fn(() => {
      store.clear()
    }),
    get length() {
      return store.size
    },
    // eslint-disable-next-line security/detect-object-injection -- Safe: index is from function parameter, not user input
    key: vi.fn((index: number) => Array.from(store.keys())[index] ?? null)
  }
})()

Object.defineProperty(globalThis, 'localStorage', {
  value: localStorageMock,
  writable: true
})

// Reset mocks between tests
beforeEach(() => {
  localStorageMock.clear()
  vi.clearAllMocks()
})

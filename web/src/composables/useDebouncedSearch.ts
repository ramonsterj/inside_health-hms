import { ref, watch, type Ref } from 'vue'

export interface DebouncedSearchOptions {
  /** Delay (ms) before a committed term is emitted. Default 300. */
  debounceMs?: number
  /** Minimum characters before a non-empty term is committed. Default 3. */
  minLength?: number
  /**
   * Existing ref to drive the search from (e.g. a component's `defineModel`).
   * When omitted, the composable creates and owns its own `term` ref.
   */
  source?: Ref<string>
}

/**
 * Shared as-you-type search behavior used by every search field in the platform.
 *
 * The returned `term` ref is bound to the input. The `onSearch` callback fires
 * (debounced) only when the trimmed term is either empty (a reset — show all)
 * or at least `minLength` characters. Terms of length 1..minLength-1 are ignored
 * so the list keeps its last committed state instead of searching on a partial word.
 *
 * Duplicate committed terms are suppressed, so re-typing the same value does not
 * re-fetch.
 */
export function useDebouncedSearch(
  onSearch: (term: string) => void,
  options: DebouncedSearchOptions = {}
) {
  const { debounceMs = 300, minLength = 3, source } = options
  const term = source ?? ref('')
  let timeoutId: ReturnType<typeof setTimeout> | null = null
  let lastCommitted = ''

  function clear() {
    if (timeoutId) {
      clearTimeout(timeoutId)
      timeoutId = null
    }
  }

  watch(term, value => {
    const trimmed = value.trim()
    clear()
    // Ignore partial input (1..minLength-1 chars); only empty or >= minLength commits.
    if (trimmed.length !== 0 && trimmed.length < minLength) return
    if (trimmed === lastCommitted) return
    timeoutId = setTimeout(() => {
      timeoutId = null
      lastCommitted = trimmed
      onSearch(trimmed)
    }, debounceMs)
  })

  /** Reset the input and committed state without emitting. */
  function reset() {
    clear()
    term.value = ''
    lastCommitted = ''
  }

  /** Cancel any pending debounced emit (e.g. on component unmount). */
  function cancel() {
    clear()
  }

  return { term, reset, cancel }
}

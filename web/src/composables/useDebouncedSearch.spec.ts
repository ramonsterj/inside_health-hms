import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { nextTick } from 'vue'
import { useDebouncedSearch } from './useDebouncedSearch'

describe('useDebouncedSearch', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  async function type(term: { value: string }, value: string) {
    term.value = value
    await nextTick() // let the watcher run
  }

  it('does not fire for 1..minLength-1 characters', async () => {
    const onSearch = vi.fn()
    const { term } = useDebouncedSearch(onSearch, { minLength: 3, debounceMs: 300 })

    await type(term, 'a')
    await type(term, 'ab')
    vi.advanceTimersByTime(1000)

    expect(onSearch).not.toHaveBeenCalled()
  })

  it('fires once, debounced, when the term reaches minLength', async () => {
    const onSearch = vi.fn()
    const { term } = useDebouncedSearch(onSearch, { minLength: 3, debounceMs: 300 })

    await type(term, 'abc')
    expect(onSearch).not.toHaveBeenCalled() // still within debounce window

    vi.advanceTimersByTime(300)
    expect(onSearch).toHaveBeenCalledTimes(1)
    expect(onSearch).toHaveBeenCalledWith('abc')
  })

  it('collapses rapid keystrokes into a single trailing call', async () => {
    const onSearch = vi.fn()
    const { term } = useDebouncedSearch(onSearch, { minLength: 3, debounceMs: 300 })

    await type(term, 'abc')
    vi.advanceTimersByTime(100)
    await type(term, 'abcd')
    vi.advanceTimersByTime(100)
    await type(term, 'abcde')
    vi.advanceTimersByTime(300)

    expect(onSearch).toHaveBeenCalledTimes(1)
    expect(onSearch).toHaveBeenCalledWith('abcde')
  })

  it('fires an empty term when cleared (reset to full list)', async () => {
    const onSearch = vi.fn()
    const { term } = useDebouncedSearch(onSearch, { minLength: 3, debounceMs: 300 })

    await type(term, 'abc')
    vi.advanceTimersByTime(300)
    onSearch.mockClear()

    await type(term, '')
    vi.advanceTimersByTime(300)

    expect(onSearch).toHaveBeenCalledTimes(1)
    expect(onSearch).toHaveBeenCalledWith('')
  })

  it('trims whitespace before evaluating and emitting', async () => {
    const onSearch = vi.fn()
    const { term } = useDebouncedSearch(onSearch, { minLength: 3, debounceMs: 300 })

    await type(term, '   ab   ') // trims to 2 chars -> below threshold
    vi.advanceTimersByTime(300)
    expect(onSearch).not.toHaveBeenCalled()

    await type(term, '  abc  ')
    vi.advanceTimersByTime(300)
    expect(onSearch).toHaveBeenCalledWith('abc')
  })

  it('does not re-fire when the committed term is unchanged', async () => {
    const onSearch = vi.fn()
    const { term } = useDebouncedSearch(onSearch, { minLength: 3, debounceMs: 300 })

    await type(term, 'abc')
    vi.advanceTimersByTime(300)
    expect(onSearch).toHaveBeenCalledTimes(1)

    // Same trimmed value again -> no new emit.
    await type(term, 'abc ')
    vi.advanceTimersByTime(300)
    expect(onSearch).toHaveBeenCalledTimes(1)
  })

  it('reset() clears the term without emitting', async () => {
    const onSearch = vi.fn()
    const { term, reset } = useDebouncedSearch(onSearch, { minLength: 3, debounceMs: 300 })

    await type(term, 'abc')
    vi.advanceTimersByTime(300)
    onSearch.mockClear()

    reset()
    await nextTick()
    vi.advanceTimersByTime(300)

    expect(term.value).toBe('')
    expect(onSearch).not.toHaveBeenCalled()
  })
})

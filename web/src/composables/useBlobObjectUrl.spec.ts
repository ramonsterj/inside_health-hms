import { describe, it, expect, vi, beforeEach } from 'vitest'
import { effectScope } from 'vue'
import { useBlobObjectUrl } from './useBlobObjectUrl'

describe('useBlobObjectUrl', () => {
  let urlCounter = 0

  beforeEach(() => {
    urlCounter = 0
    URL.createObjectURL = vi.fn(() => `blob:mock-${++urlCounter}`)
    URL.revokeObjectURL = vi.fn()
  })

  function inScope() {
    const scope = effectScope()
    const composable = scope.run(() => useBlobObjectUrl())
    if (!composable) throw new Error('scope.run returned nothing')
    return { scope, ...composable }
  }

  it('creates an object URL for a blob and clears it on null', () => {
    const { url, setBlob } = inScope()

    setBlob(new Blob(['a']))
    expect(url.value).toBe('blob:mock-1')

    setBlob(null)
    expect(url.value).toBeNull()
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:mock-1')
  })

  it('revokes the previous URL when a new blob is set', () => {
    const { url, setBlob } = inScope()

    setBlob(new Blob(['a']))
    setBlob(new Blob(['b']))

    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:mock-1')
    expect(url.value).toBe('blob:mock-2')
  })

  it('revokes the URL when the owning scope is disposed', () => {
    const { scope, setBlob } = inScope()

    setBlob(new Blob(['a']))
    scope.stop()

    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:mock-1')
  })
})

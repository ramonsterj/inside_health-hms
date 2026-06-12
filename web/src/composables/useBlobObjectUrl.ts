import { ref, onScopeDispose } from 'vue'

/**
 * Owns the lifecycle of an object URL created from a Blob: setting a new blob
 * (or null) revokes the previous URL, and disposal of the owning scope revokes
 * the last one. Object URLs pin the blob bytes in memory until revoked, so
 * every createObjectURL must be paired with a revoke.
 */
export function useBlobObjectUrl() {
  const url = ref<string | null>(null)

  function revoke() {
    if (url.value) {
      URL.revokeObjectURL(url.value)
      url.value = null
    }
  }

  function setBlob(blob: Blob | null) {
    revoke()
    if (blob) {
      url.value = URL.createObjectURL(blob)
    }
  }

  onScopeDispose(revoke)

  return { url, setBlob, revoke }
}

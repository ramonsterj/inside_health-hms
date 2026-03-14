import { isAxiosError } from 'axios'

/**
 * Extracts error message from an API error response.
 * Pure utility with no Vue/i18n dependencies.
 *
 * Handles:
 * - ErrorResponse structure: { error: { message } }
 * - ApiResponse structure: { message }
 * - Standard Error objects
 */
export function extractApiErrorMessage(error: unknown, fallback: string): string {
  if (isAxiosError(error) && error.response?.data) {
    const data = error.response.data

    // Primary path: ErrorResponse structure { error: { message } }
    if (typeof data === 'object' && data.error?.message) {
      return data.error.message
    }

    // Legacy fallback: ApiResponse structure { message }
    if (typeof data === 'object' && 'message' in data && typeof data.message === 'string') {
      return data.message
    }
  }

  // Handle standard Error objects
  if (error instanceof Error) {
    return error.message
  }

  return fallback
}

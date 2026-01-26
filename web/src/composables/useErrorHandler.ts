import { useI18n } from 'vue-i18n'
import { useToast } from 'primevue/usetoast'
import { isAxiosError } from 'axios'

/**
 * Map of backend error codes to i18n keys.
 * These codes come from GlobalExceptionHandler.kt
 */
const ERROR_CODE_MAP: Record<string, string> = {
  NOT_FOUND: 'errors.notFound',
  BAD_REQUEST: 'errors.badRequest',
  UNAUTHORIZED: 'errors.unauthorized',
  INVALID_TOKEN: 'errors.invalidToken',
  FORBIDDEN: 'errors.forbidden',
  CONFLICT: 'errors.conflict',
  ACCOUNT_DISABLED: 'errors.accountDisabled',
  VALIDATION_ERROR: 'errors.validation',
  INVALID_REQUEST_BODY: 'errors.badRequest',
  AUTHENTICATION_FAILED: 'errors.authenticationFailed',
  INTERNAL_ERROR: 'errors.serverError'
}

/**
 * Fallback map for specific backend messages that need special handling.
 * Use sparingly - prefer code-based mapping above.
 */
const ERROR_MESSAGE_MAP: Record<string, string> = {
  'Patient already has an active admission': 'errors.patient.alreadyAdmitted'
}

/**
 * Composable for standardized error handling with toast notifications.
 * Handles API errors with proper extraction from ErrorResponse structure.
 */
export function useErrorHandler() {
  const { t } = useI18n()
  const toast = useToast()

  /**
   * Extracts error code from API ErrorResponse.
   * Backend returns: { error: { code: "NOT_FOUND", message: "..." } }
   */
  function extractErrorCode(error: unknown): string | null {
    if (isAxiosError(error) && error.response?.data) {
      const data = error.response.data
      if (typeof data === 'object' && data.error?.code) {
        return data.error.code
      }
    }
    return null
  }

  /**
   * Extracts error message from various error types.
   * Handles API ErrorResponse: { error: { code, message, details } }
   */
  function extractErrorMessage(error: unknown): string {
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

    return t('errors.generic')
  }

  /**
   * Extracts field-level validation errors from API ErrorResponse.
   * Backend returns: { error: { details: { fieldName: ["error1", "error2"] } } }
   */
  function extractFieldErrors(error: unknown): Record<string, string[]> | null {
    if (isAxiosError(error) && error.response?.data) {
      const data = error.response.data
      if (typeof data === 'object' && data.error?.details) {
        return data.error.details
      }
    }
    return null
  }

  /**
   * Translates error to user-friendly message using i18n.
   * Priority: 1) Error code mapping, 2) Message mapping, 3) Raw message
   */
  function translateError(error: unknown): string {
    const code = extractErrorCode(error)
    const rawMessage = extractErrorMessage(error)

    // First try code-based translation
    if (code && ERROR_CODE_MAP[code]) {
      return t(ERROR_CODE_MAP[code])
    }

    // Then try message-based translation for specific known messages
    if (ERROR_MESSAGE_MAP[rawMessage]) {
      return t(ERROR_MESSAGE_MAP[rawMessage])
    }

    // Fall back to raw message (backend messages are already localized)
    return rawMessage
  }

  /**
   * Sets VeeValidate field errors from API validation response.
   * @param setErrors - VeeValidate's setErrors function from useForm
   * @param error - The caught error from API call
   * @returns true if field errors were set, false otherwise
   */
  function setFieldErrorsFromResponse(
    setErrors: (errors: Record<string, string>) => void,
    error: unknown
  ): boolean {
    const fieldErrors = extractFieldErrors(error)
    if (fieldErrors && Object.keys(fieldErrors).length > 0) {
      // Convert string[] to single string (VeeValidate expects string per field)
      const veeErrors: Record<string, string> = {}
      for (const [field, messages] of Object.entries(fieldErrors)) {
        const firstMessage = messages[0]
        if (firstMessage) {
          veeErrors[field] = firstMessage
        }
      }
      if (Object.keys(veeErrors).length > 0) {
        setErrors(veeErrors)
        return true
      }
    }
    return false
  }

  /**
   * Shows an error toast notification with the appropriate message.
   * @param error - The error to display (Error object or unknown)
   * @param life - Toast display duration in ms (default: 5000)
   */
  function showError(error: unknown, life = 5000): void {
    const message = translateError(error)
    toast.add({
      severity: 'error',
      summary: t('common.error'),
      detail: message,
      life
    })
  }

  /**
   * Shows a success toast notification.
   * @param messageKey - i18n key for the success message
   * @param life - Toast display duration in ms (default: 3000)
   */
  function showSuccess(messageKey: string, life = 3000): void {
    toast.add({
      severity: 'success',
      summary: t('common.success'),
      detail: t(messageKey),
      life
    })
  }

  return {
    showError,
    showSuccess,
    extractErrorMessage,
    extractErrorCode,
    extractFieldErrors,
    setFieldErrorsFromResponse
  }
}

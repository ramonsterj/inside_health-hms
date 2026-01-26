import { useI18n } from 'vue-i18n'
import { useToast } from 'primevue/usetoast'
import { isAxiosError } from 'axios'

// Map of known API error messages to i18n keys
const ERROR_MESSAGE_MAP: Record<string, string> = {
  'Patient already has an active admission': 'admission.patientAlreadyAdmitted'
}

/**
 * Composable for standardized error handling with toast notifications.
 * Extracts error message from Error objects or uses a generic fallback.
 */
export function useErrorHandler() {
  const { t } = useI18n()
  const toast = useToast()

  /**
   * Translates known API error messages to i18n keys.
   * Returns original message if no translation exists.
   */
  function translateError(rawMessage: string): string {
    const i18nKey = ERROR_MESSAGE_MAP[rawMessage]
    return i18nKey ? t(i18nKey) : rawMessage
  }

  /**
   * Extracts error message from various error types.
   * Handles Axios errors by extracting message from response body.
   */
  function extractErrorMessage(error: unknown): string {
    // Handle Axios errors - extract message from response body
    if (isAxiosError(error) && error.response?.data) {
      const data = error.response.data
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
   * Shows an error toast notification with the appropriate message.
   * @param error - The error to display (Error object or unknown)
   * @param life - Toast display duration in ms (default: 5000)
   */
  function showError(error: unknown, life = 5000): void {
    const rawMessage = extractErrorMessage(error)
    const message = translateError(rawMessage)
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
    showSuccess
  }
}

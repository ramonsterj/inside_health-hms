import { useI18n } from 'vue-i18n'
import { useToast } from 'primevue/usetoast'

/**
 * Composable for standardized error handling with toast notifications.
 * Extracts error message from Error objects or uses a generic fallback.
 */
export function useErrorHandler() {
  const { t } = useI18n()
  const toast = useToast()

  /**
   * Shows an error toast notification with the appropriate message.
   * @param error - The error to display (Error object or unknown)
   * @param life - Toast display duration in ms (default: 5000)
   */
  function showError(error: unknown, life = 5000): void {
    const message = error instanceof Error ? error.message : t('errors.generic')
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

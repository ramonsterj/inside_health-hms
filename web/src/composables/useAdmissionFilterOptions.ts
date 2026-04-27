import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { AdmissionStatus, AdmissionType } from '@/types/admission'

export function useAdmissionFilterOptions() {
  const { t } = useI18n()

  const statusOptions = computed(() => [
    { label: t('common.all'), value: null },
    { label: t('admission.statuses.ACTIVE'), value: AdmissionStatus.ACTIVE },
    { label: t('admission.statuses.DISCHARGED'), value: AdmissionStatus.DISCHARGED }
  ])

  const typeOptions = computed(() => [
    { label: t('common.all'), value: null },
    { label: t('admission.types.HOSPITALIZATION'), value: AdmissionType.HOSPITALIZATION },
    { label: t('admission.types.AMBULATORY'), value: AdmissionType.AMBULATORY },
    { label: t('admission.types.ELECTROSHOCK_THERAPY'), value: AdmissionType.ELECTROSHOCK_THERAPY },
    { label: t('admission.types.KETAMINE_INFUSION'), value: AdmissionType.KETAMINE_INFUSION },
    { label: t('admission.types.EMERGENCY'), value: AdmissionType.EMERGENCY }
  ])

  return {
    statusOptions,
    typeOptions
  }
}

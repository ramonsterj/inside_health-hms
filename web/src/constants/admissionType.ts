import { AdmissionType } from '@/types/admission'

export interface AdmissionTypeMeta {
  /** Tailwind background utility class for the colored dot/swatch (e.g. `bg-indigo-500`). */
  dotClass: string
  /** i18n key path resolving to the human-readable label. */
  labelKey: string
}

export const ADMISSION_TYPE_META: Record<AdmissionType, AdmissionTypeMeta> = {
  [AdmissionType.HOSPITALIZATION]: {
    dotClass: 'bg-indigo-500',
    labelKey: 'admission.types.HOSPITALIZATION'
  },
  [AdmissionType.AMBULATORY]: {
    dotClass: 'bg-emerald-500',
    labelKey: 'admission.types.AMBULATORY'
  },
  [AdmissionType.ELECTROSHOCK_THERAPY]: {
    dotClass: 'bg-amber-500',
    labelKey: 'admission.types.ELECTROSHOCK_THERAPY'
  },
  [AdmissionType.KETAMINE_INFUSION]: {
    dotClass: 'bg-purple-500',
    labelKey: 'admission.types.KETAMINE_INFUSION'
  },
  [AdmissionType.EMERGENCY]: {
    dotClass: 'bg-red-500',
    labelKey: 'admission.types.EMERGENCY'
  }
}

export const ADMISSION_TYPE_ORDER: AdmissionType[] = [
  AdmissionType.HOSPITALIZATION,
  AdmissionType.AMBULATORY,
  AdmissionType.ELECTROSHOCK_THERAPY,
  AdmissionType.KETAMINE_INFUSION,
  AdmissionType.EMERGENCY
]

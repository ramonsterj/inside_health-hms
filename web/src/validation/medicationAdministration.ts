import { z } from 'zod'
import { AdministrationStatus } from '@/types/medicationAdministration'

/**
 * Medication Administration validation schemas that mirror backend Jakarta Bean Validation rules.
 * Error messages use i18n keys that are translated via zodI18n.ts
 */

const administrationStatusValues = [
  AdministrationStatus.GIVEN,
  AdministrationStatus.MISSED,
  AdministrationStatus.REFUSED,
  AdministrationStatus.HELD
] as const

export const createMedicationAdministrationSchema = z.object({
  status: z.enum(administrationStatusValues, {
    required_error: 'validation.medicationAdministration.status.required',
    invalid_type_error: 'validation.medicationAdministration.status.required'
  }),
  notes: z
    .string()
    .max(1000, 'validation.medicationAdministration.notes.max')
    .optional()
    .or(z.literal(''))
})

export type MedicationAdministrationFormData = z.infer<typeof createMedicationAdministrationSchema>

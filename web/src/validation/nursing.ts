import { z } from 'zod'

/**
 * Nursing Module validation schemas that mirror backend Jakarta Bean Validation rules.
 * Error messages use i18n keys that are translated via the validation system.
 */

// Nursing Note schema - description is required with max 5000 chars
export const nursingNoteSchema = z.object({
  description: z
    .string()
    .min(1, 'validation.nursing.note.description.required')
    .max(5000, 'validation.nursing.note.description.max')
})

export type NursingNoteFormData = z.infer<typeof nursingNoteSchema>

// Vital Sign schema with range validations and cross-field BP validation
export const vitalSignSchema = z
  .object({
    recordedAt: z.string().optional().or(z.literal('')).or(z.literal(null)),
    systolicBp: z
      .number({
        required_error: 'validation.nursing.vitalSign.systolicBp.required',
        invalid_type_error: 'validation.nursing.vitalSign.systolicBp.required'
      })
      .int('validation.nursing.vitalSign.systolicBp.integer')
      .min(60, 'validation.nursing.vitalSign.systolicBp.min')
      .max(250, 'validation.nursing.vitalSign.systolicBp.max'),
    diastolicBp: z
      .number({
        required_error: 'validation.nursing.vitalSign.diastolicBp.required',
        invalid_type_error: 'validation.nursing.vitalSign.diastolicBp.required'
      })
      .int('validation.nursing.vitalSign.diastolicBp.integer')
      .min(30, 'validation.nursing.vitalSign.diastolicBp.min')
      .max(150, 'validation.nursing.vitalSign.diastolicBp.max'),
    heartRate: z
      .number({
        required_error: 'validation.nursing.vitalSign.heartRate.required',
        invalid_type_error: 'validation.nursing.vitalSign.heartRate.required'
      })
      .int('validation.nursing.vitalSign.heartRate.integer')
      .min(20, 'validation.nursing.vitalSign.heartRate.min')
      .max(250, 'validation.nursing.vitalSign.heartRate.max'),
    respiratoryRate: z
      .number({
        required_error: 'validation.nursing.vitalSign.respiratoryRate.required',
        invalid_type_error: 'validation.nursing.vitalSign.respiratoryRate.required'
      })
      .int('validation.nursing.vitalSign.respiratoryRate.integer')
      .min(5, 'validation.nursing.vitalSign.respiratoryRate.min')
      .max(60, 'validation.nursing.vitalSign.respiratoryRate.max'),
    temperature: z
      .number({
        required_error: 'validation.nursing.vitalSign.temperature.required',
        invalid_type_error: 'validation.nursing.vitalSign.temperature.required'
      })
      .min(30.0, 'validation.nursing.vitalSign.temperature.min')
      .max(45.0, 'validation.nursing.vitalSign.temperature.max'),
    oxygenSaturation: z
      .number({
        required_error: 'validation.nursing.vitalSign.oxygenSaturation.required',
        invalid_type_error: 'validation.nursing.vitalSign.oxygenSaturation.required'
      })
      .int('validation.nursing.vitalSign.oxygenSaturation.integer')
      .min(50, 'validation.nursing.vitalSign.oxygenSaturation.min')
      .max(100, 'validation.nursing.vitalSign.oxygenSaturation.max'),
    other: z
      .string()
      .max(1000, 'validation.nursing.vitalSign.other.max')
      .optional()
      .or(z.literal(''))
  })
  .refine(data => data.systolicBp > data.diastolicBp, {
    message: 'validation.nursing.vitalSign.bpComparison',
    path: ['systolicBp']
  })

export type VitalSignFormData = z.infer<typeof vitalSignSchema>

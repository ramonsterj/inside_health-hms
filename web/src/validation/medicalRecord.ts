import { z } from 'zod'
import { MedicalOrderCategory, AdministrationRoute } from '@/types/medicalRecord'

/**
 * Medical Record validation schemas that mirror backend Jakarta Bean Validation rules.
 * Error messages use i18n keys that are translated via zodI18n.ts
 */

// Clinical History schema - all fields are optional rich text
export const clinicalHistorySchema = z.object({
  reasonForAdmission: z.string().optional().or(z.literal('')),
  historyOfPresentIllness: z.string().optional().or(z.literal('')),
  psychiatricHistory: z.string().optional().or(z.literal('')),
  medicalHistory: z.string().optional().or(z.literal('')),
  familyHistory: z.string().optional().or(z.literal('')),
  personalHistory: z.string().optional().or(z.literal('')),
  substanceUseHistory: z.string().optional().or(z.literal('')),
  legalHistory: z.string().optional().or(z.literal('')),
  socialHistory: z.string().optional().or(z.literal('')),
  developmentalHistory: z.string().optional().or(z.literal('')),
  educationalOccupationalHistory: z.string().optional().or(z.literal('')),
  sexualHistory: z.string().optional().or(z.literal('')),
  religiousSpiritualHistory: z.string().optional().or(z.literal('')),
  mentalStatusExam: z.string().optional().or(z.literal('')),
  physicalExam: z.string().optional().or(z.literal('')),
  diagnosticImpression: z.string().optional().or(z.literal('')),
  treatmentPlan: z.string().optional().or(z.literal('')),
  riskAssessment: z.string().optional().or(z.literal('')),
  prognosis: z.string().optional().or(z.literal('')),
  informedConsentNotes: z.string().optional().or(z.literal('')),
  additionalNotes: z.string().optional().or(z.literal(''))
})

export type ClinicalHistoryFormData = z.infer<typeof clinicalHistorySchema>

// Progress Note schema - all 4 SOAP fields are optional
export const progressNoteSchema = z.object({
  subjectiveData: z.string().optional().or(z.literal('')),
  objectiveData: z.string().optional().or(z.literal('')),
  analysis: z.string().optional().or(z.literal('')),
  actionPlans: z.string().optional().or(z.literal(''))
})

export type ProgressNoteFormData = z.infer<typeof progressNoteSchema>

// Medical Order category enum values
const medicalOrderCategoryValues = [
  MedicalOrderCategory.ORDENES_MEDICAS,
  MedicalOrderCategory.MEDICAMENTOS,
  MedicalOrderCategory.LABORATORIOS,
  MedicalOrderCategory.REFERENCIAS_MEDICAS,
  MedicalOrderCategory.PRUEBAS_PSICOMETRICAS,
  MedicalOrderCategory.ACTIVIDAD_FISICA,
  MedicalOrderCategory.CUIDADOS_ESPECIALES,
  MedicalOrderCategory.DIETA,
  MedicalOrderCategory.RESTRICCIONES_MOVILIDAD,
  MedicalOrderCategory.PERMISOS_VISITA,
  MedicalOrderCategory.OTRAS
] as const

// Administration route enum values
const administrationRouteValues = [
  AdministrationRoute.ORAL,
  AdministrationRoute.IV,
  AdministrationRoute.IM,
  AdministrationRoute.SC,
  AdministrationRoute.TOPICAL,
  AdministrationRoute.INHALATION,
  AdministrationRoute.RECTAL,
  AdministrationRoute.SUBLINGUAL,
  AdministrationRoute.OTHER
] as const

// Medical Order schema
export const medicalOrderSchema = z.object({
  category: z.enum(medicalOrderCategoryValues, {
    required_error: 'validation.medicalRecord.medicalOrder.category.required',
    invalid_type_error: 'validation.medicalRecord.medicalOrder.category.required'
  }),
  startDate: z.string().min(1, 'validation.medicalRecord.medicalOrder.startDate.required'),
  endDate: z.string().optional().or(z.literal('')),
  medication: z
    .string()
    .max(255, 'validation.medicalRecord.medicalOrder.medication.max')
    .optional()
    .or(z.literal('')),
  dosage: z
    .string()
    .max(100, 'validation.medicalRecord.medicalOrder.dosage.max')
    .optional()
    .or(z.literal('')),
  route: z
    .enum(administrationRouteValues, {
      invalid_type_error: 'validation.medicalRecord.medicalOrder.route.invalid'
    })
    .nullable()
    .optional(),
  frequency: z
    .string()
    .max(100, 'validation.medicalRecord.medicalOrder.frequency.max')
    .optional()
    .or(z.literal('')),
  schedule: z
    .string()
    .max(100, 'validation.medicalRecord.medicalOrder.schedule.max')
    .optional()
    .or(z.literal('')),
  observations: z.string().optional().or(z.literal(''))
})

export type MedicalOrderFormData = z.infer<typeof medicalOrderSchema>

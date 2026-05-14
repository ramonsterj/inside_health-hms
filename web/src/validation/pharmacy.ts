import { z } from 'zod'
import { DosageForm, AdministrationRoute, MedicationSection } from '@/types/pharmacy'

export const createMedicationSchema = z.object({
  name: z.string().min(1, 'validation.medication.name.required').max(150),
  description: z.string().max(500).optional().or(z.literal('')),
  price: z.number({ required_error: 'validation.medication.price.required' }).min(0),
  cost: z.number({ required_error: 'validation.medication.cost.required' }).min(0),
  sku: z.string().max(20).optional().or(z.literal('')),
  restockLevel: z.number().int().min(0).default(0),
  genericName: z.string().min(1, 'validation.medication.genericName.required').max(150),
  commercialName: z.string().max(150).optional().or(z.literal('')),
  strength: z.string().max(50).optional().or(z.literal('')),
  dosageForm: z.nativeEnum(DosageForm),
  route: z.nativeEnum(AdministrationRoute).nullable().optional(),
  controlled: z.boolean().default(false),
  atcCode: z.string().max(10).optional().or(z.literal('')),
  section: z.nativeEnum(MedicationSection),
  active: z.boolean().default(true)
})

export const updateMedicationSchema = createMedicationSchema

export type MedicationFormData = z.infer<typeof createMedicationSchema>

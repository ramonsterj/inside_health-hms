import { z } from 'zod'
import { toTypedSchema as veeToTypedSchema } from '@vee-validate/zod'
import { i18n } from '@/i18n'

/**
 * Translates an i18n key using the current locale.
 * Works with vue-i18n in composition API mode (legacy: false).
 */
export function translateMessage(message: string): string {
  // Only translate if it looks like an i18n key
  if (message.startsWith('validation.') || message.startsWith('errors.')) {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const translated = (i18n.global as any).t(message) as string
    // Return translation if found, otherwise return original
    return translated !== message ? translated : message
  }
  return message
}

/**
 * Custom toTypedSchema that translates i18n keys in error messages.
 *
 * This wraps VeeValidate's toTypedSchema and post-processes error messages
 * to translate any i18n keys (starting with 'validation.' or 'errors.').
 *
 * @example
 * // In your schema:
 * const schema = z.object({
 *   name: z.string().min(1, 'validation.patient.firstName.required')
 * })
 *
 * // In your component:
 * const { errors } = useForm({
 *   validationSchema: toTypedSchema(schema)
 * })
 * // errors.name will show the translated message
 */
export function toTypedSchema<TSchema extends z.ZodType>(schema: TSchema) {
  const baseSchema = veeToTypedSchema(schema)

  return {
    ...baseSchema,
    async parse(value: z.input<TSchema>) {
      const result = await baseSchema.parse(value)

      // Translate error messages if there are errors
      if (result.errors && result.errors.length > 0) {
        result.errors = result.errors.map(error => ({
          ...error,
          errors: error.errors.map(msg => translateMessage(msg))
        }))
      }

      return result
    }
  }
}

/**
 * Custom Zod error map for built-in Zod errors (when no custom message is provided).
 * This handles cases like invalid_type, too_small, etc.
 */
export const zodI18nErrorMap: z.ZodErrorMap = (issue, ctx) => {
  // For built-in Zod errors without custom messages, provide i18n defaults
  switch (issue.code) {
    case z.ZodIssueCode.invalid_type:
      if (issue.received === 'undefined' || issue.received === 'null') {
        return { message: translateMessage('validation.required') }
      }
      return { message: ctx.defaultError }

    case z.ZodIssueCode.too_small:
      if (issue.type === 'string' && issue.minimum === 1) {
        return { message: translateMessage('validation.required') }
      }
      return { message: ctx.defaultError }

    case z.ZodIssueCode.invalid_string:
      if (issue.validation === 'email') {
        return { message: translateMessage('validation.email') }
      }
      return { message: ctx.defaultError }

    default:
      return { message: ctx.defaultError }
  }
}

/**
 * Initializes Zod with the i18n error map.
 * Call this once during app initialization.
 */
export function initZodI18n(): void {
  z.setErrorMap(zodI18nErrorMap)
}

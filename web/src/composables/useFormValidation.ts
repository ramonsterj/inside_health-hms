import { useForm, useField } from 'vee-validate'
import type { GenericObject } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import type { ZodSchema, ZodTypeDef } from 'zod'
import { computed } from 'vue'

/**
 * Composable for form validation using VeeValidate + Zod.
 *
 * @example
 * ```vue
 * <script setup lang="ts">
 * import { useFormValidation } from '@/composables/useFormValidation'
 * import { loginSchema } from '@/validation/schemas'
 *
 * const { form, errors, isValid, handleSubmit, defineField } = useFormValidation(loginSchema, {
 *   identifier: '',
 *   password: ''
 * })
 *
 * const [identifier, identifierAttrs] = defineField('identifier')
 * const [password, passwordAttrs] = defineField('password')
 *
 * const onSubmit = handleSubmit(async (values) => {
 *   await authStore.login(values)
 * })
 * </script>
 * ```
 */
export function useFormValidation<TInput, TOutput extends GenericObject>(
  schema: ZodSchema<TOutput, ZodTypeDef, TInput>,
  initialValues: TInput
) {
  const typedSchema = toTypedSchema(schema)

  const { handleSubmit, errors, values, resetForm, validate, meta } = useForm<TOutput>({
    validationSchema: typedSchema,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    initialValues: initialValues as any
  })

  const isValid = computed(() => meta.value.valid)
  const isDirty = computed(() => meta.value.dirty)

  return {
    form: values,
    errors,
    isValid,
    isDirty,
    handleSubmit,
    resetForm,
    validate,
    defineField: <K extends keyof TOutput>(name: K) => useField<TOutput[K]>(name as string)
  }
}

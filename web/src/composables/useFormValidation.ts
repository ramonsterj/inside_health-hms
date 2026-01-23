import { useForm, useField } from 'vee-validate'
import type { GenericObject } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import type { ZodSchema, ZodTypeDef } from 'zod'
import { computed, reactive, watch } from 'vue'

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

  const { handleSubmit, errors, values, resetForm, validate, meta, setFieldValue } =
    useForm<TOutput>({
      validationSchema: typedSchema,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      initialValues: initialValues as any
    })

  const isValid = computed(() => meta.value.valid)
  const isDirty = computed(() => meta.value.dirty)

  // Create a reactive proxy that syncs with VeeValidate's values
  // This allows v-model binding while respecting VeeValidate's validation
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const form = reactive({ ...(initialValues as any) }) as TOutput

  // Sync form changes to VeeValidate
  watch(
    () => ({ ...form }),
    newValues => {
      Object.keys(newValues).forEach(key => {
        if (values[key as keyof TOutput] !== newValues[key as keyof TOutput]) {
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          setFieldValue(key as any, newValues[key as keyof TOutput])
        }
      })
    },
    { deep: true }
  )

  // Sync VeeValidate values back to form (e.g., after resetForm)
  watch(
    values,
    newValues => {
      // Safe: keys come from Object.keys() which only returns own enumerable properties,
      // and both form and newValues are controlled objects created within this composable
      /* eslint-disable security/detect-object-injection */
      Object.keys(newValues).forEach(key => {
        const typedKey = key as keyof TOutput
        if (form[typedKey] !== newValues[typedKey]) {
          Reflect.set(form, key, newValues[typedKey])
        }
      })
      /* eslint-enable security/detect-object-injection */
    },
    { deep: true }
  )

  return {
    form,
    errors,
    isValid,
    isDirty,
    handleSubmit,
    resetForm,
    validate,
    defineField: <K extends keyof TOutput>(name: K) => useField<TOutput[K]>(name as string)
  }
}

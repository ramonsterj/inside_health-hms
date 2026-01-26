<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useToast } from 'primevue/usetoast'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@/validation/zodI18n'
import Card from 'primevue/card'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import Button from 'primevue/button'
import { useAuthStore } from '@/stores/auth'
import { useSessionExpiration } from '@/composables/useSessionExpiration'
import { loginSchema, type LoginFormData } from '@/validation'

const router = useRouter()
const { t } = useI18n()
const { scheduleExpirationCheck } = useSessionExpiration()
const route = useRoute()
const toast = useToast()
const authStore = useAuthStore()

const { defineField, handleSubmit, errors } = useForm<LoginFormData>({
  validationSchema: toTypedSchema(loginSchema),
  initialValues: {
    identifier: '',
    password: ''
  }
})

const [identifier] = defineField('identifier')
const [password] = defineField('password')

const onSubmit = handleSubmit(async values => {
  try {
    await authStore.login(values)

    // Reschedule token expiration monitoring with the new token
    scheduleExpirationCheck()

    toast.add({
      severity: 'success',
      summary: t('auth.login.success'),
      detail: t('auth.login.successDetail'),
      life: 3000
    })
    const redirect = route.query.redirect as string
    router.push(redirect || { name: 'dashboard' })
  } catch (error) {
    const message = error instanceof Error ? error.message : t('auth.login.failedDetail')
    toast.add({
      severity: 'error',
      summary: t('auth.login.failed'),
      detail: message,
      life: 5000
    })
  }
})
</script>

<template>
  <div class="login-container">
    <Card class="login-card">
      <template #title>
        <div class="text-center">
          <img src="@/assets/logo.svg" alt="Inside Health" class="login-logo" />
        </div>
      </template>
      <template #content>
        <form @submit="onSubmit" class="login-form">
          <div class="field">
            <label for="identifier">{{ t('auth.login.identifier') }}</label>
            <InputText
              id="identifier"
              v-model="identifier"
              :placeholder="t('auth.login.identifierPlaceholder')"
              class="w-full"
              :class="{ 'p-invalid': errors.identifier }"
            />
            <small v-if="errors.identifier" class="p-error">{{ errors.identifier }}</small>
          </div>

          <div class="field">
            <label for="password">{{ t('auth.login.password') }}</label>
            <Password
              id="password"
              v-model="password"
              :placeholder="t('auth.login.passwordPlaceholder')"
              :feedback="false"
              toggleMask
              class="w-full"
              :invalid="!!errors.password"
              :inputStyle="{ width: '100%' }"
            />
            <small v-if="errors.password" class="p-error">{{ errors.password }}</small>
          </div>

          <Button
            type="submit"
            :label="t('auth.login.submit')"
            :loading="authStore.loading"
            class="w-full mt-4"
          />
        </form>
      </template>
    </Card>
  </div>
</template>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 2rem;
  background: var(--surface-ground);
}

.login-card {
  width: 100%;
  max-width: 400px;
}

.login-logo {
  height: 5rem;
  width: auto;
  display: block;
  margin: 2rem auto;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.field label {
  font-weight: 500;
}

.p-error {
  color: var(--red-500);
}
</style>

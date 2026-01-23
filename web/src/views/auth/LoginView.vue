<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import Card from 'primevue/card'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import Button from 'primevue/button'
import { useAuthStore } from '@/stores/auth'
import { loginSchema, type LoginFormData } from '@/validation'

const router = useRouter()
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
    toast.add({
      severity: 'success',
      summary: 'Welcome back!',
      detail: 'You have successfully logged in.',
      life: 3000
    })
    const redirect = route.query.redirect as string
    router.push(redirect || { name: 'dashboard' })
  } catch (error) {
    const message =
      error instanceof Error ? error.message : 'Login failed. Please check your credentials.'
    toast.add({
      severity: 'error',
      summary: 'Login Failed',
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
          <h1>Welcome Back</h1>
          <p class="subtitle">Sign in to your account</p>
        </div>
      </template>
      <template #content>
        <form @submit="onSubmit" class="login-form">
          <div class="field">
            <label for="identifier">Email or Username</label>
            <InputText
              id="identifier"
              v-model="identifier"
              placeholder="Enter your email or username"
              class="w-full"
              :class="{ 'p-invalid': errors.identifier }"
            />
            <small v-if="errors.identifier" class="p-error">{{ errors.identifier }}</small>
          </div>

          <div class="field">
            <label for="password">Password</label>
            <Password
              id="password"
              v-model="password"
              placeholder="Enter your password"
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
            label="Sign In"
            icon="pi pi-sign-in"
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

.subtitle {
  color: var(--text-color-secondary);
  margin-top: 0.5rem;
  font-size: 0.9rem;
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

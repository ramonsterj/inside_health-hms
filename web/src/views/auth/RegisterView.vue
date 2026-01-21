<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import Card from 'primevue/card'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import Button from 'primevue/button'
import ProgressSpinner from 'primevue/progressspinner'
import { useAuthStore } from '@/stores/auth'
import { registerSchema, type RegisterFormData } from '@/validation'
import { useUsernameAvailability } from '@/composables/useUsernameAvailability'

const router = useRouter()
const toast = useToast()
const authStore = useAuthStore()

const { defineField, handleSubmit, errors } = useForm<RegisterFormData>({
  validationSchema: toTypedSchema(registerSchema),
  initialValues: {
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
    firstName: '',
    lastName: ''
  }
})

const [username] = defineField('username')
const { state: usernameState } = useUsernameAvailability(username)
const [email] = defineField('email')
const [password] = defineField('password')
const [confirmPassword] = defineField('confirmPassword')
const [firstName] = defineField('firstName')
const [lastName] = defineField('lastName')

const onSubmit = handleSubmit(async values => {
  try {
    await authStore.register({
      username: values.username,
      email: values.email,
      password: values.password,
      firstName: values.firstName || null,
      lastName: values.lastName || null
    })
    toast.add({
      severity: 'success',
      summary: 'Account Created',
      detail: 'Welcome! Your account has been created successfully.',
      life: 3000
    })
    router.push({ name: 'dashboard' })
  } catch (error) {
    const message =
      error instanceof Error ? error.message : 'Registration failed. Please try again.'
    toast.add({
      severity: 'error',
      summary: 'Registration Failed',
      detail: message,
      life: 5000
    })
  }
})
</script>

<template>
  <div class="register-container">
    <Card class="register-card">
      <template #title>
        <div class="text-center">
          <h1>Create Account</h1>
          <p class="subtitle">Get started with your free account</p>
        </div>
      </template>
      <template #content>
        <form @submit="onSubmit" class="register-form">
          <div class="field">
            <label for="username">Username *</label>
            <div class="username-input-wrapper">
              <InputText
                id="username"
                v-model="username"
                placeholder="Choose a username"
                class="w-full"
                :class="{
                  'p-invalid': errors.username || usernameState.isAvailable === false,
                  'p-valid': usernameState.isAvailable === true && !errors.username
                }"
              />
              <span class="username-status">
                <ProgressSpinner
                  v-if="usernameState.isChecking"
                  style="width: 20px; height: 20px"
                  strokeWidth="4"
                />
                <i
                  v-else-if="usernameState.isAvailable === true"
                  class="pi pi-check-circle"
                  style="color: var(--green-500)"
                />
                <i
                  v-else-if="usernameState.isAvailable === false"
                  class="pi pi-times-circle"
                  style="color: var(--red-500)"
                />
              </span>
            </div>
            <small v-if="errors.username" class="p-error">{{ errors.username }}</small>
            <small
              v-else-if="usernameState.message"
              :class="usernameState.isAvailable ? 'p-success' : 'p-error'"
            >
              {{ usernameState.message }}
            </small>
          </div>

          <div class="name-fields">
            <div class="field">
              <label for="firstName">First Name</label>
              <InputText
                id="firstName"
                v-model="firstName"
                placeholder="First name"
                class="w-full"
                :class="{ 'p-invalid': errors.firstName }"
              />
              <small v-if="errors.firstName" class="p-error">{{ errors.firstName }}</small>
            </div>

            <div class="field">
              <label for="lastName">Last Name</label>
              <InputText
                id="lastName"
                v-model="lastName"
                placeholder="Last name"
                class="w-full"
                :class="{ 'p-invalid': errors.lastName }"
              />
              <small v-if="errors.lastName" class="p-error">{{ errors.lastName }}</small>
            </div>
          </div>

          <div class="field">
            <label for="email">Email *</label>
            <InputText
              id="email"
              v-model="email"
              type="email"
              placeholder="Enter your email"
              class="w-full"
              :class="{ 'p-invalid': errors.email }"
            />
            <small v-if="errors.email" class="p-error">{{ errors.email }}</small>
          </div>

          <div class="field">
            <label for="password">Password *</label>
            <Password
              id="password"
              v-model="password"
              placeholder="Create a password"
              toggleMask
              class="w-full"
              :invalid="!!errors.password"
              :inputStyle="{ width: '100%' }"
            />
            <small v-if="errors.password" class="p-error">{{ errors.password }}</small>
          </div>

          <div class="field">
            <label for="confirmPassword">Confirm Password *</label>
            <Password
              id="confirmPassword"
              v-model="confirmPassword"
              placeholder="Confirm your password"
              :feedback="false"
              toggleMask
              class="w-full"
              :invalid="!!errors.confirmPassword"
              :inputStyle="{ width: '100%' }"
            />
            <small v-if="errors.confirmPassword" class="p-error">{{
              errors.confirmPassword
            }}</small>
          </div>

          <Button
            type="submit"
            label="Create Account"
            icon="pi pi-user-plus"
            :loading="authStore.loading"
            class="w-full mt-4"
          />
        </form>
      </template>
      <template #footer>
        <div class="text-center">
          <span>Already have an account? </span>
          <router-link :to="{ name: 'login' }">Sign in</router-link>
        </div>
      </template>
    </Card>
  </div>
</template>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 2rem;
  background: var(--surface-ground);
}

.register-card {
  width: 100%;
  max-width: 450px;
}

.subtitle {
  color: var(--text-color-secondary);
  margin-top: 0.5rem;
  font-size: 0.9rem;
}

.register-form {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.name-fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
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

.p-success {
  color: var(--green-500);
}

.username-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.username-status {
  position: absolute;
  right: 12px;
  display: flex;
  align-items: center;
}

.username-input-wrapper :deep(.p-inputtext) {
  padding-right: 40px;
}

.p-valid {
  border-color: var(--green-500);
}

@media (max-width: 500px) {
  .name-fields {
    grid-template-columns: 1fr;
  }
}
</style>

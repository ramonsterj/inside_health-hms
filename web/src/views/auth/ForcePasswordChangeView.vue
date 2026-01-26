<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@/validation/zodI18n'
import { useAuthStore } from '@/stores/auth'
import { forceChangePasswordSchema } from '@/validation/user'
import Password from 'primevue/password'
import Button from 'primevue/button'
import Message from 'primevue/message'

const { t } = useI18n()
const router = useRouter()
const authStore = useAuthStore()

const submitting = ref(false)
const errorMessage = ref('')

const { defineField, handleSubmit, errors } = useForm({
  validationSchema: toTypedSchema(forceChangePasswordSchema)
})

const [currentPassword] = defineField('currentPassword')
const [newPassword] = defineField('newPassword')
const [confirmNewPassword] = defineField('confirmNewPassword')

const onSubmit = handleSubmit(async values => {
  submitting.value = true
  errorMessage.value = ''
  try {
    await authStore.changePasswordForced(values)
    router.push({ name: 'dashboard' })
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t('errors.generic')
  } finally {
    submitting.value = false
  }
})
</script>

<template>
  <div class="flex items-center justify-center min-h-screen bg-surface-ground">
    <div class="w-full max-w-md p-6">
      <div class="bg-surface-card rounded-lg shadow-lg p-8">
        <div class="text-center mb-6">
          <h1 class="text-2xl font-bold text-surface-900">
            {{ t('auth.changePassword') }}
          </h1>
          <p class="text-surface-600 mt-2">
            {{ t('auth.passwordChangeRequired') }}
          </p>
        </div>

        <Message v-if="errorMessage" severity="error" :closable="false" class="mb-4">
          {{ errorMessage }}
        </Message>

        <form @submit.prevent="onSubmit" class="flex flex-col gap-4">
          <div class="flex flex-col gap-2">
            <label for="currentPassword" class="font-medium">
              {{ t('auth.currentPassword') }}
            </label>
            <Password
              id="currentPassword"
              v-model="currentPassword"
              :feedback="false"
              toggleMask
              :class="{ 'p-invalid': errors.currentPassword }"
              class="w-full"
              inputClass="w-full"
            />
            <small v-if="errors.currentPassword" class="p-error">
              {{ errors.currentPassword }}
            </small>
          </div>

          <div class="flex flex-col gap-2">
            <label for="newPassword" class="font-medium">
              {{ t('auth.newPassword') }}
            </label>
            <Password
              id="newPassword"
              v-model="newPassword"
              toggleMask
              :class="{ 'p-invalid': errors.newPassword }"
              class="w-full"
              inputClass="w-full"
            />
            <small v-if="errors.newPassword" class="p-error">
              {{ errors.newPassword }}
            </small>
          </div>

          <div class="flex flex-col gap-2">
            <label for="confirmNewPassword" class="font-medium">
              {{ t('auth.confirmNewPassword') }}
            </label>
            <Password
              id="confirmNewPassword"
              v-model="confirmNewPassword"
              :feedback="false"
              toggleMask
              :class="{ 'p-invalid': errors.confirmNewPassword }"
              class="w-full"
              inputClass="w-full"
            />
            <small v-if="errors.confirmNewPassword" class="p-error">
              {{ errors.confirmNewPassword }}
            </small>
          </div>

          <Button
            type="submit"
            :label="t('auth.changePassword')"
            :loading="submitting"
            class="w-full mt-4"
          />
        </form>
      </div>
    </div>
  </div>
</template>

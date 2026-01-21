<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useToast } from 'primevue/usetoast'
import Card from 'primevue/card'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import Button from 'primevue/button'
import Divider from 'primevue/divider'
import LocaleSwitcher from '@/components/common/LocaleSwitcher.vue'
import { useAuthStore } from '@/stores/auth'
import { useUserStore } from '@/stores/user'
import { useFormValidation } from '@/composables/useFormValidation'
import { profileUpdateSchema, changePasswordSchema } from '@/validation'

const { t } = useI18n()
const toast = useToast()
const authStore = useAuthStore()
const userStore = useUserStore()

// Profile form using validation composable
const {
  form: profileForm,
  errors: profileErrors,
  isDirty: profileChanged,
  handleSubmit: handleProfileSubmit
} = useFormValidation(profileUpdateSchema, {
  firstName: authStore.user?.firstName || '',
  lastName: authStore.user?.lastName || ''
})

// Password form using validation composable
const {
  form: passwordForm,
  errors: passwordErrors,
  handleSubmit: handlePasswordSubmit,
  resetForm: resetPasswordForm
} = useFormValidation(changePasswordSchema, {
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const handleProfileUpdate = handleProfileSubmit(async values => {
  try {
    await userStore.updateProfile({
      firstName: values.firstName || null,
      lastName: values.lastName || null
    })
    toast.add({
      severity: 'success',
      summary: t('profile.updateSuccess'),
      detail: t('profile.updateSuccessDetail'),
      life: 3000
    })
  } catch (error) {
    const message = error instanceof Error ? error.message : t('errors.generic')
    toast.add({
      severity: 'error',
      summary: t('profile.updateFailed'),
      detail: message,
      life: 5000
    })
  }
})

const handlePasswordChange = handlePasswordSubmit(async values => {
  try {
    await userStore.changePassword({
      currentPassword: values.currentPassword,
      newPassword: values.newPassword
    })
    resetPasswordForm()
    toast.add({
      severity: 'success',
      summary: t('profile.passwordSuccess'),
      detail: t('profile.passwordSuccessDetail'),
      life: 3000
    })
  } catch (error) {
    const message = error instanceof Error ? error.message : t('errors.generic')
    toast.add({
      severity: 'error',
      summary: t('profile.passwordFailed'),
      detail: message,
      life: 5000
    })
  }
})
</script>

<template>
  <div class="profile-page">
    <h1 class="page-title">{{ t('profile.title') }}</h1>

    <div class="profile-grid">
      <Card class="profile-card">
        <template #title>
          <i class="pi pi-user" style="margin-right: 0.5rem"></i>
          {{ t('profile.personalInfo') }}
        </template>
        <template #content>
          <form @submit.prevent="handleProfileUpdate" class="profile-form">
            <div class="field">
              <label for="email">{{ t('profile.email') }}</label>
              <InputText id="email" :modelValue="authStore.user?.email" disabled class="w-full" />
              <small class="field-hint">{{ t('profile.emailHint') }}</small>
            </div>

            <div class="name-fields">
              <div class="field">
                <label for="firstName">{{ t('profile.firstName') }}</label>
                <InputText
                  id="firstName"
                  v-model="profileForm.firstName"
                  :placeholder="t('profile.firstNamePlaceholder')"
                  class="w-full"
                  :class="{ 'p-invalid': profileErrors.firstName }"
                />
                <small v-if="profileErrors.firstName" class="p-error">
                  {{ profileErrors.firstName }}
                </small>
              </div>

              <div class="field">
                <label for="lastName">{{ t('profile.lastName') }}</label>
                <InputText
                  id="lastName"
                  v-model="profileForm.lastName"
                  :placeholder="t('profile.lastNamePlaceholder')"
                  class="w-full"
                  :class="{ 'p-invalid': profileErrors.lastName }"
                />
                <small v-if="profileErrors.lastName" class="p-error">
                  {{ profileErrors.lastName }}
                </small>
              </div>
            </div>

            <Button
              type="submit"
              :label="t('profile.saveChanges')"
              icon="pi pi-check"
              :loading="userStore.loading"
              :disabled="!profileChanged"
              class="mt-3"
            />
          </form>
        </template>
      </Card>

      <Card class="profile-card">
        <template #title>
          <i class="pi pi-cog" style="margin-right: 0.5rem"></i>
          {{ t('profile.preferences') }}
        </template>
        <template #content>
          <div class="preferences-form">
            <div class="field">
              <label for="language">{{ t('profile.language') }}</label>
              <LocaleSwitcher inputId="language" class="w-full" />
              <small class="field-hint">{{ t('profile.languageHint') }}</small>
            </div>
          </div>
        </template>
      </Card>

      <Card class="profile-card">
        <template #title>
          <i class="pi pi-lock" style="margin-right: 0.5rem"></i>
          {{ t('profile.changePassword') }}
        </template>
        <template #content>
          <form @submit.prevent="handlePasswordChange" class="password-form">
            <div class="field">
              <label for="currentPassword">{{ t('profile.currentPassword') }}</label>
              <Password
                id="currentPassword"
                v-model="passwordForm.currentPassword"
                :placeholder="t('profile.currentPasswordPlaceholder')"
                :feedback="false"
                toggleMask
                class="w-full"
                :invalid="!!passwordErrors.currentPassword"
                :inputStyle="{ width: '100%' }"
              />
              <small v-if="passwordErrors.currentPassword" class="p-error">
                {{ passwordErrors.currentPassword }}
              </small>
            </div>

            <Divider />

            <div class="field">
              <label for="newPassword">{{ t('profile.newPassword') }}</label>
              <Password
                id="newPassword"
                v-model="passwordForm.newPassword"
                :placeholder="t('profile.newPasswordPlaceholder')"
                toggleMask
                class="w-full"
                :invalid="!!passwordErrors.newPassword"
                :inputStyle="{ width: '100%' }"
              />
              <small v-if="passwordErrors.newPassword" class="p-error">
                {{ passwordErrors.newPassword }}
              </small>
            </div>

            <div class="field">
              <label for="confirmPassword">{{ t('profile.confirmPassword') }}</label>
              <Password
                id="confirmPassword"
                v-model="passwordForm.confirmPassword"
                :placeholder="t('profile.confirmPasswordPlaceholder')"
                :feedback="false"
                toggleMask
                class="w-full"
                :invalid="!!passwordErrors.confirmPassword"
                :inputStyle="{ width: '100%' }"
              />
              <small v-if="passwordErrors.confirmPassword" class="p-error">
                {{ passwordErrors.confirmPassword }}
              </small>
            </div>

            <Button
              type="submit"
              :label="t('profile.changePasswordBtn')"
              icon="pi pi-key"
              :loading="userStore.loading"
              class="mt-3"
            />
          </form>
        </template>
      </Card>
    </div>
  </div>
</template>

<style scoped>
.profile-page {
  max-width: 900px;
  margin: 0 auto;
}

.page-title {
  margin-bottom: 2rem;
}

.profile-grid {
  display: grid;
  gap: 1.5rem;
}

.profile-card {
  width: 100%;
}

.profile-form,
.password-form,
.preferences-form {
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

.field-hint {
  color: var(--text-color-secondary);
  font-size: 0.85rem;
}

.p-error {
  color: var(--red-500);
}

@media (max-width: 600px) {
  .name-fields {
    grid-template-columns: 1fr;
  }
}
</style>

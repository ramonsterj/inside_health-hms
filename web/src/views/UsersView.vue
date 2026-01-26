<script setup lang="ts">
import { ref, reactive, computed, onMounted, toRef } from 'vue'
import { useI18n } from 'vue-i18n'
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import MultiSelect from 'primevue/multiselect'
import ToggleSwitch from 'primevue/toggleswitch'
import ProgressSpinner from 'primevue/progressspinner'
import { useUserStore } from '@/stores/user'
import { useAuthStore } from '@/stores/auth'
import { useRoleStore } from '@/stores/role'
import { useUsernameAvailability } from '@/composables/useUsernameAvailability'
import { usePhoneNumberList } from '@/composables/usePhoneNumberList'
import PhoneNumberInput from '@/components/users/PhoneNumberInput.vue'
import type { User, CreateUserRequest, AdminUpdateUserRequest, PhoneNumberRequest } from '@/types'
import { UserStatus, Salutation, PhoneType } from '@/types'

// Constants
const SEARCH_DEBOUNCE_MS = 300

const { t } = useI18n()
const toast = useToast()
const confirm = useConfirm()
const userStore = useUserStore()
const authStore = useAuthStore()
const roleStore = useRoleStore()

const first = ref(0)
const rows = ref(10)

// Filter state
const statusFilter = ref<UserStatus | null>(null)
const showDeleted = ref(false)
const searchQuery = ref('')
const roleFilter = ref<string | null>(null)
let searchTimeout: ReturnType<typeof setTimeout> | null = null

const filterStatusOptions = computed(() => [
  { label: t('users.filters.allStatuses'), value: null },
  { label: t('users.status.active'), value: UserStatus.ACTIVE },
  { label: t('users.status.inactive'), value: UserStatus.INACTIVE },
  { label: t('users.status.suspended'), value: UserStatus.SUSPENDED }
])

// Computed properties for current data source
const displayedUsers = computed(() =>
  showDeleted.value ? userStore.deletedUsers : userStore.users
)

const displayedTotal = computed(() =>
  showDeleted.value ? userStore.totalDeletedUsers : userStore.totalUsers
)

// Add User Dialog
const showAddUserDialog = ref(false)
const confirmPassword = ref('')
const newUserPhoneNumbers = ref<PhoneNumberRequest[]>([])
const newUser = reactive<Omit<CreateUserRequest, 'phoneNumbers'>>({
  username: '',
  email: '',
  password: '',
  firstName: '',
  lastName: '',
  salutation: null,
  roleCodes: ['USER'],
  status: UserStatus.ACTIVE
})
const newUserUsername = toRef(newUser, 'username')
const { state: usernameState, reset: resetUsernameState } = useUsernameAvailability(newUserUsername)
const {
  addPhone: addNewUserPhone,
  removePhone: removeNewUserPhone,
  updatePhone: updateNewUserPhone
} = usePhoneNumberList(newUserPhoneNumbers)

// Edit User Dialog
const showEditUserDialog = ref(false)
const editingUserId = ref<number | null>(null)
const editUserPhoneNumbers = ref<PhoneNumberRequest[]>([])
const editUserLoading = ref(false)
const editUser = reactive<Omit<AdminUpdateUserRequest, 'phoneNumbers'>>({
  firstName: '',
  lastName: '',
  salutation: null,
  roleCodes: [],
  status: UserStatus.ACTIVE,
  emailVerified: false
})
const {
  addPhone: addEditUserPhone,
  removePhone: removeEditUserPhone,
  updatePhone: updateEditUserPhone
} = usePhoneNumberList(editUserPhoneNumbers)

// Dynamic role options from roleStore
const roleOptions = computed(() =>
  roleStore.roles.map(role => ({
    label: role.name,
    value: role.code
  }))
)

const salutationOptions = computed(() => {
  // Only show salutations that have translations in the current locale
  return Object.values(Salutation)
    .filter(key => t(`user.salutations.${key}`) !== `user.salutations.${key}`)
    .map(key => ({
      label: t(`user.salutations.${key}`),
      value: key
    }))
})

const roleFilterOptions = computed(() => [
  { label: t('user.allRoles'), value: null },
  ...roleStore.roles.map(role => ({
    label: role.name,
    value: role.code
  }))
])

const statusOptions = computed(() => [
  { label: t('users.status.active'), value: UserStatus.ACTIVE },
  { label: t('users.status.inactive'), value: UserStatus.INACTIVE },
  { label: t('users.status.suspended'), value: UserStatus.SUSPENDED }
])

// Reset Password Dialog
const showPasswordDialog = ref(false)
const temporaryPassword = ref('')

onMounted(() => {
  loadUsers()
  roleStore.fetchRoles()
})

async function loadUsers() {
  try {
    const page = Math.floor(first.value / rows.value)
    if (showDeleted.value) {
      await userStore.fetchDeletedUsers(page, rows.value)
    } else {
      await userStore.fetchUsers(
        page,
        rows.value,
        statusFilter.value,
        searchQuery.value,
        roleFilter.value
      )
    }
  } catch (error) {
    const message = error instanceof Error ? error.message : t('errors.generic')
    toast.add({
      severity: 'error',
      summary: t('common.error'),
      detail: message,
      life: 5000
    })
  }
}

function onSearchInput() {
  if (searchTimeout) {
    clearTimeout(searchTimeout)
  }
  searchTimeout = setTimeout(() => {
    first.value = 0
    loadUsers()
  }, SEARCH_DEBOUNCE_MS)
}

function onRoleFilterChange() {
  first.value = 0
  loadUsers()
}

function onStatusFilterChange() {
  first.value = 0
  loadUsers()
}

function onDeletedToggleChange() {
  first.value = 0
  loadUsers()
}

function onPageChange() {
  // first and rows are already updated by v-model bindings
  loadUsers()
}

function confirmDelete(user: User) {
  if (user.id === authStore.user?.id) {
    toast.add({
      severity: 'warn',
      summary: t('users.notAllowed'),
      detail: t('users.cannotDeleteSelf'),
      life: 3000
    })
    return
  }

  confirm.require({
    message: t('users.deleteConfirm', { email: user.email }),
    header: t('users.deleteConfirmHeader'),
    icon: 'pi pi-exclamation-triangle',
    rejectClass: 'p-button-secondary p-button-outlined',
    acceptClass: 'p-button-danger',
    accept: () => deleteUser(user),
    reject: () => {}
  })
}

async function deleteUser(user: User) {
  try {
    await userStore.deleteUser(user.id)
    toast.add({
      severity: 'success',
      summary: t('users.deleteSuccess'),
      detail: t('users.deleteSuccessDetail', { email: user.email }),
      life: 3000
    })
  } catch (error) {
    const message = error instanceof Error ? error.message : t('errors.generic')
    toast.add({
      severity: 'error',
      summary: t('users.deleteFailed'),
      detail: message,
      life: 5000
    })
  }
}

async function restoreUser(user: User) {
  try {
    await userStore.restoreUser(user.id)
    toast.add({
      severity: 'success',
      summary: t('users.restoreSuccess'),
      detail: t('users.restoreSuccessDetail', { email: user.email }),
      life: 3000
    })
  } catch (error) {
    const message = error instanceof Error ? error.message : t('errors.generic')
    toast.add({
      severity: 'error',
      summary: t('users.restoreFailed'),
      detail: message,
      life: 5000
    })
  }
}

function getRoleSeverity(role: string): 'danger' | 'info' {
  return role === 'ADMIN' ? 'danger' : 'info'
}

function getStatusSeverity(status: UserStatus): 'success' | 'warn' | 'danger' {
  switch (status) {
    case UserStatus.ACTIVE:
      return 'success'
    case UserStatus.INACTIVE:
      return 'warn'
    case UserStatus.SUSPENDED:
      return 'danger'
    default:
      return 'warn'
  }
}

function formatDate(dateString: string | null): string {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleDateString()
}

function openAddUserDialog() {
  newUser.username = ''
  newUser.email = ''
  newUser.password = ''
  confirmPassword.value = ''
  newUser.firstName = ''
  newUser.lastName = ''
  newUser.salutation = null
  newUser.roleCodes = ['USER']
  newUser.status = UserStatus.ACTIVE
  // Initialize with one empty phone entry to make it clear at least one is required
  newUserPhoneNumbers.value = [
    {
      phoneNumber: '',
      phoneType: PhoneType.MOBILE,
      isPrimary: true
    }
  ]
  resetUsernameState()
  showAddUserDialog.value = true
}

const passwordsMatch = computed(() => {
  return newUser.password === confirmPassword.value
})

const hasValidPhoneNumbers = computed(() => {
  return newUserPhoneNumbers.value.some(p => p.phoneNumber.trim() !== '')
})

const hasValidEditPhoneNumbers = computed(() => {
  return editUserPhoneNumbers.value.some(p => p.phoneNumber.trim() !== '')
})

// Validation error messages for Create User form
const usernameError = computed(() => {
  if (!newUser.username) return null
  if (newUser.username.length < 3) return t('validation.username.min')
  if (newUser.username.length > 50) return t('validation.username.max')
  return null
})

const emailError = computed(() => {
  if (!newUser.email) return null
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(newUser.email)) return t('validation.emailField.invalid')
  return null
})

const passwordError = computed(() => {
  if (!newUser.password) return null
  if (newUser.password.length < 8) return t('validation.password.min')
  return null
})

const phoneNumbersError = computed(() => {
  if (newUserPhoneNumbers.value.length === 0) return t('validation.phone.required')
  if (!hasValidPhoneNumbers.value) return t('validation.phone.required')
  return null
})

// Check if form is valid for submission
const isCreateFormValid = computed(() => {
  return (
    newUser.username.length >= 3 &&
    newUser.username.length <= 50 &&
    /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(newUser.email) &&
    newUser.password.length >= 8 &&
    confirmPassword.value &&
    passwordsMatch.value &&
    hasValidPhoneNumbers.value &&
    usernameState.value.isAvailable !== false
  )
})

async function createUser() {
  if (!passwordsMatch.value) {
    toast.add({
      severity: 'error',
      summary: t('common.error'),
      detail: t('user.password.mismatch'),
      life: 3000
    })
    return
  }

  try {
    const userData: CreateUserRequest = {
      ...newUser,
      phoneNumbers: newUserPhoneNumbers.value.filter(p => p.phoneNumber.trim() !== '')
    }
    await userStore.createUser(userData)
    showAddUserDialog.value = false
    toast.add({
      severity: 'success',
      summary: t('users.createSuccess'),
      detail: t('users.createSuccessDetail', { email: newUser.email }),
      life: 3000
    })
  } catch (error) {
    const message = error instanceof Error ? error.message : t('errors.generic')
    toast.add({
      severity: 'error',
      summary: t('users.createFailed'),
      detail: message,
      life: 5000
    })
  }
}

function confirmResetPassword(user: User) {
  confirm.require({
    message: t('users.resetPasswordConfirm', { email: user.email }),
    header: t('users.resetPassword'),
    icon: 'pi pi-key',
    rejectClass: 'p-button-secondary p-button-outlined',
    acceptClass: 'p-button-warning',
    accept: () => resetPassword(user)
  })
}

async function resetPassword(user: User) {
  try {
    const password = await userStore.resetUserPassword(user.id)
    temporaryPassword.value = password
    showPasswordDialog.value = true
  } catch (error) {
    const message = error instanceof Error ? error.message : t('errors.generic')
    toast.add({
      severity: 'error',
      summary: t('users.resetPasswordFailed'),
      detail: message,
      life: 5000
    })
  }
}

function copyPassword() {
  navigator.clipboard.writeText(temporaryPassword.value)
  toast.add({
    severity: 'success',
    summary: t('users.copied'),
    detail: t('users.copiedToClipboard'),
    life: 2000
  })
}

async function openEditUserDialog(user: User) {
  editingUserId.value = user.id
  showEditUserDialog.value = true
  editUserLoading.value = true

  try {
    // Fetch fresh user data to ensure phone numbers are loaded
    const freshUser = await userStore.getUserById(user.id)
    editUser.firstName = freshUser.firstName || ''
    editUser.lastName = freshUser.lastName || ''
    editUser.salutation = freshUser.salutation
    editUser.roleCodes = [...freshUser.roles]
    editUser.status = freshUser.status
    editUser.emailVerified = freshUser.emailVerified
    const existingPhones =
      freshUser.phoneNumbers?.map(p => ({
        id: p.id,
        phoneNumber: p.phoneNumber,
        phoneType: p.phoneType,
        isPrimary: p.isPrimary
      })) || []
    // Ensure at least one phone entry exists for editing
    editUserPhoneNumbers.value =
      existingPhones.length > 0
        ? existingPhones
        : [{ phoneNumber: '', phoneType: PhoneType.MOBILE, isPrimary: true }]
  } catch (error) {
    showEditUserDialog.value = false
    const message = error instanceof Error ? error.message : t('errors.generic')
    toast.add({
      severity: 'error',
      summary: t('common.error'),
      detail: message,
      life: 5000
    })
  } finally {
    editUserLoading.value = false
  }
}

async function saveEditedUser() {
  if (!editingUserId.value) return

  try {
    const userData: AdminUpdateUserRequest = {
      ...editUser,
      phoneNumbers: editUserPhoneNumbers.value.filter(p => p.phoneNumber.trim() !== '')
    }
    await userStore.updateUser(editingUserId.value, userData)
    showEditUserDialog.value = false
    toast.add({
      severity: 'success',
      summary: t('users.updateSuccess'),
      detail: t('users.updateSuccessDetail'),
      life: 3000
    })
  } catch (error) {
    const message = error instanceof Error ? error.message : t('errors.generic')
    toast.add({
      severity: 'error',
      summary: t('users.updateFailed'),
      detail: message,
      life: 5000
    })
  }
}
</script>

<template>
  <div class="users-page">
    <div class="page-header">
      <h1 class="page-title">{{ t('users.title') }}</h1>
      <div class="header-actions">
        <Button icon="pi pi-plus" :label="t('users.addUser')" @click="openAddUserDialog" />
        <Button
          icon="pi pi-refresh"
          :label="t('users.refresh')"
          severity="secondary"
          outlined
          @click="loadUsers"
          :loading="userStore.loading"
        />
      </div>
    </div>

    <Card class="filter-card">
      <template #content>
        <div class="filter-bar">
          <div class="filter-item search-item">
            <label for="searchQuery">{{ t('users.filters.search') }}</label>
            <InputText
              id="searchQuery"
              v-model="searchQuery"
              :placeholder="t('users.filters.searchPlaceholder')"
              :disabled="showDeleted"
              @input="onSearchInput"
              class="search-input"
            />
          </div>
          <div class="filter-item">
            <label for="roleFilter">{{ t('user.filterByRole') }}</label>
            <Select
              id="roleFilter"
              v-model="roleFilter"
              :options="roleFilterOptions"
              optionLabel="label"
              optionValue="value"
              :placeholder="t('user.allRoles')"
              :disabled="showDeleted"
              @change="onRoleFilterChange"
              class="filter-dropdown"
            />
          </div>
          <div class="filter-item">
            <label for="statusFilter">{{ t('users.filters.status') }}</label>
            <Select
              id="statusFilter"
              v-model="statusFilter"
              :options="filterStatusOptions"
              optionLabel="label"
              optionValue="value"
              :placeholder="t('users.filters.allStatuses')"
              :disabled="showDeleted"
              @change="onStatusFilterChange"
              class="filter-dropdown"
            />
          </div>
          <div class="filter-item toggle-item">
            <label for="showDeleted">{{ t('users.filters.showDeleted') }}</label>
            <ToggleSwitch
              inputId="showDeleted"
              v-model="showDeleted"
              @change="onDeletedToggleChange"
            />
          </div>
        </div>
      </template>
    </Card>

    <Card>
      <template #content>
        <DataTable
          :value="displayedUsers"
          :loading="userStore.loading"
          :paginator="true"
          v-model:rows="rows"
          v-model:first="first"
          :totalRecords="displayedTotal"
          :lazy="true"
          @page="onPageChange"
          :rowsPerPageOptions="[5, 10, 25, 50]"
          dataKey="id"
          stripedRows
          scrollable
        >
          <template #empty>
            <div class="text-center p-4">
              {{ showDeleted ? t('users.empty.deleted') : t('users.empty.active') }}
            </div>
          </template>

          <Column field="email" :header="t('users.columns.email')" sortable>
            <template #body="{ data }">
              <div class="user-email">
                <span>{{ data.email }}</span>
                <Tag
                  v-if="data.id === authStore.user?.id"
                  :value="t('users.you')"
                  severity="secondary"
                  class="ml-2"
                />
              </div>
            </template>
          </Column>

          <Column :header="t('users.columns.name')">
            <template #body="{ data }">
              {{
                data.firstName || data.lastName
                  ? `${data.firstName || ''} ${data.lastName || ''}`.trim()
                  : '-'
              }}
            </template>
          </Column>

          <Column field="roles" :header="t('users.columns.roles')">
            <template #body="{ data }">
              <div class="role-tags">
                <Tag
                  v-for="role in data.roles"
                  :key="role"
                  :value="role"
                  :severity="getRoleSeverity(role)"
                  class="role-tag"
                />
              </div>
            </template>
          </Column>

          <Column field="status" :header="t('users.columns.status')">
            <template #body="{ data }">
              <Tag
                :value="
                  showDeleted
                    ? t('users.status.deleted')
                    : t(`users.status.${data.status.toLowerCase()}`)
                "
                :severity="showDeleted ? 'danger' : getStatusSeverity(data.status)"
              />
            </template>
          </Column>

          <Column field="createdAt" :header="t('users.columns.createdAt')">
            <template #body="{ data }">
              {{ formatDate(data.createdAt) }}
            </template>
          </Column>

          <Column :header="t('users.columns.actions')" style="width: 180px">
            <template #body="{ data }">
              <div class="action-buttons" v-if="!showDeleted">
                <Button
                  icon="pi pi-pencil"
                  severity="info"
                  text
                  rounded
                  @click="openEditUserDialog(data)"
                  v-tooltip.top="t('users.tooltips.edit')"
                />
                <Button
                  icon="pi pi-key"
                  severity="warning"
                  text
                  rounded
                  @click="confirmResetPassword(data)"
                  v-tooltip.top="t('users.tooltips.resetPassword')"
                />
                <Button
                  icon="pi pi-trash"
                  severity="danger"
                  text
                  rounded
                  @click="confirmDelete(data)"
                  :disabled="data.id === authStore.user?.id"
                  v-tooltip.top="t('users.tooltips.delete')"
                />
              </div>
              <div class="action-buttons" v-else>
                <Button
                  icon="pi pi-refresh"
                  severity="success"
                  text
                  rounded
                  @click="restoreUser(data)"
                  v-tooltip.top="t('users.tooltips.restore')"
                />
              </div>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>

    <!-- Add User Dialog -->
    <Dialog
      v-model:visible="showAddUserDialog"
      :header="t('users.addUserDialog.title')"
      :modal="true"
      :closable="!userStore.loading"
      :style="{ width: '600px' }"
      :breakpoints="{ '768px': '90vw' }"
    >
      <div class="form-grid">
        <div class="form-field">
          <label for="username">{{ t('users.addUserDialog.username') }} *</label>
          <div class="username-input-wrapper">
            <InputText
              id="username"
              v-model="newUser.username"
              :placeholder="t('users.addUserDialog.usernamePlaceholder')"
              class="w-full"
              :class="{
                'p-invalid': usernameState.isAvailable === false || usernameError,
                'p-valid': usernameState.isAvailable === true && !usernameError
              }"
            />
            <span class="username-status">
              <ProgressSpinner
                v-if="usernameState.isChecking"
                style="width: 20px; height: 20px"
                strokeWidth="4"
              />
              <i
                v-else-if="usernameState.isAvailable === true && !usernameError"
                class="pi pi-check-circle"
                style="color: var(--green-500)"
              />
              <i
                v-else-if="usernameState.isAvailable === false || usernameError"
                class="pi pi-times-circle"
                style="color: var(--red-500)"
              />
            </span>
          </div>
          <small v-if="usernameError" class="p-error">
            {{ usernameError }}
          </small>
          <small
            v-else-if="usernameState.message"
            :class="usernameState.isAvailable ? 'p-success' : 'p-error'"
          >
            {{ usernameState.message }}
          </small>
        </div>
        <div class="form-field">
          <label for="email">{{ t('users.addUserDialog.email') }} *</label>
          <InputText
            id="email"
            v-model="newUser.email"
            type="email"
            :placeholder="t('users.addUserDialog.emailPlaceholder')"
            class="w-full"
            :class="{ 'p-invalid': emailError }"
          />
          <small v-if="emailError" class="p-error">
            {{ emailError }}
          </small>
        </div>
        <div class="form-row">
          <div class="form-field">
            <label for="password">{{ t('users.addUserDialog.password') }} *</label>
            <InputText
              id="password"
              v-model="newUser.password"
              type="password"
              :placeholder="t('users.addUserDialog.passwordPlaceholder')"
              class="w-full"
              :class="{ 'p-invalid': passwordError }"
            />
            <small v-if="passwordError" class="p-error">
              {{ passwordError }}
            </small>
          </div>
          <div class="form-field">
            <label for="confirmPassword">{{ t('user.confirmPassword') }} *</label>
            <InputText
              id="confirmPassword"
              v-model="confirmPassword"
              type="password"
              :placeholder="t('user.confirmPassword')"
              class="w-full"
              :class="{ 'p-invalid': confirmPassword && !passwordsMatch }"
            />
            <small v-if="confirmPassword && !passwordsMatch" class="p-error">
              {{ t('user.password.mismatch') }}
            </small>
          </div>
        </div>
        <div class="form-row-name">
          <div class="form-field salutation-field">
            <label for="salutation">{{ t('user.salutation') }}</label>
            <Select
              id="salutation"
              v-model="newUser.salutation"
              :options="salutationOptions"
              optionLabel="label"
              optionValue="value"
              :placeholder="t('user.salutation')"
              showClear
              class="w-full"
            />
          </div>
          <div class="form-field">
            <label for="firstName">{{ t('users.addUserDialog.firstName') }}</label>
            <InputText
              id="firstName"
              v-model="newUser.firstName"
              :placeholder="t('users.addUserDialog.firstNamePlaceholder')"
              class="w-full"
            />
          </div>
          <div class="form-field">
            <label for="lastName">{{ t('users.addUserDialog.lastName') }}</label>
            <InputText
              id="lastName"
              v-model="newUser.lastName"
              :placeholder="t('users.addUserDialog.lastNamePlaceholder')"
              class="w-full"
            />
          </div>
        </div>
        <div class="form-field">
          <label for="roles">{{ t('users.addUserDialog.roles') }}</label>
          <MultiSelect
            inputId="roles"
            v-model="newUser.roleCodes"
            :options="roleOptions"
            optionLabel="label"
            optionValue="value"
            :placeholder="t('users.addUserDialog.rolesPlaceholder')"
            class="w-full"
          />
        </div>
        <div class="form-field">
          <label for="status">{{ t('users.addUserDialog.status') }}</label>
          <Select
            id="status"
            v-model="newUser.status"
            :options="statusOptions"
            optionLabel="label"
            optionValue="value"
            :placeholder="t('users.addUserDialog.statusPlaceholder')"
            class="w-full"
          />
        </div>

        <!-- Phone Numbers -->
        <div class="form-field">
          <div class="flex justify-between items-center mb-2">
            <label>{{ t('user.phoneNumbers') }} *</label>
            <Button
              icon="pi pi-plus"
              :label="t('user.addPhoneNumber')"
              size="small"
              text
              @click="addNewUserPhone"
            />
          </div>
          <div class="phone-numbers-list">
            <PhoneNumberInput
              v-for="(phone, index) in newUserPhoneNumbers"
              :key="`new-${index}`"
              :model-value="phone"
              @update:model-value="updateNewUserPhone(index, $event)"
              :show-remove="newUserPhoneNumbers.length > 1"
              :index="index"
              @remove="removeNewUserPhone(index)"
            />
          </div>
          <small v-if="phoneNumbersError" class="p-error">
            {{ phoneNumbersError }}
          </small>
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          severity="secondary"
          outlined
          @click="showAddUserDialog = false"
        />
        <Button
          :label="t('users.createUser')"
          icon="pi pi-check"
          @click="createUser"
          :loading="userStore.loading"
          :disabled="!isCreateFormValid"
        />
      </template>
    </Dialog>

    <!-- Temporary Password Dialog -->
    <Dialog
      v-model:visible="showPasswordDialog"
      :header="t('users.resetPasswordSuccess')"
      :modal="true"
      :style="{ width: '400px' }"
      :breakpoints="{ '640px': '90vw' }"
    >
      <div class="password-display">
        <p>{{ t('users.temporaryPasswordLabel') }}</p>
        <div class="password-box">
          <code>{{ temporaryPassword }}</code>
          <Button
            icon="pi pi-copy"
            severity="secondary"
            text
            rounded
            @click="copyPassword"
            v-tooltip.top="t('users.copiedToClipboard')"
          />
        </div>
        <p class="password-note">
          {{ t('users.temporaryPasswordNote') }}
        </p>
      </div>
      <template #footer>
        <Button :label="t('users.done')" @click="showPasswordDialog = false" />
      </template>
    </Dialog>

    <!-- Edit User Dialog -->
    <Dialog
      v-model:visible="showEditUserDialog"
      :header="t('users.editUserDialog.title')"
      :modal="true"
      :closable="!editUserLoading && !userStore.loading"
      :style="{ width: '600px' }"
      :breakpoints="{ '768px': '90vw' }"
    >
      <div v-if="editUserLoading" class="flex justify-center p-4">
        <ProgressSpinner style="width: 50px; height: 50px" />
      </div>
      <div v-else class="form-grid">
        <div class="form-row-name">
          <div class="form-field salutation-field">
            <label for="editSalutation">{{ t('user.salutation') }}</label>
            <Select
              id="editSalutation"
              v-model="editUser.salutation"
              :options="salutationOptions"
              optionLabel="label"
              optionValue="value"
              :placeholder="t('user.salutation')"
              showClear
              class="w-full"
            />
          </div>
          <div class="form-field">
            <label for="editFirstName">{{ t('users.editUserDialog.firstName') }}</label>
            <InputText
              id="editFirstName"
              v-model="editUser.firstName"
              :placeholder="t('users.editUserDialog.firstNamePlaceholder')"
              class="w-full"
            />
          </div>
          <div class="form-field">
            <label for="editLastName">{{ t('users.editUserDialog.lastName') }}</label>
            <InputText
              id="editLastName"
              v-model="editUser.lastName"
              :placeholder="t('users.editUserDialog.lastNamePlaceholder')"
              class="w-full"
            />
          </div>
        </div>
        <div class="form-field">
          <label for="editRoles">{{ t('users.editUserDialog.roles') }}</label>
          <MultiSelect
            inputId="editRoles"
            v-model="editUser.roleCodes"
            :options="roleOptions"
            optionLabel="label"
            optionValue="value"
            :placeholder="t('users.editUserDialog.rolesPlaceholder')"
            class="w-full"
          />
        </div>
        <div class="form-field">
          <label for="editStatus">{{ t('users.editUserDialog.status') }}</label>
          <Select
            id="editStatus"
            v-model="editUser.status"
            :options="statusOptions"
            optionLabel="label"
            optionValue="value"
            :placeholder="t('users.editUserDialog.statusPlaceholder')"
            class="w-full"
          />
        </div>

        <!-- Phone Numbers -->
        <div class="form-field">
          <div class="flex justify-between items-center mb-2">
            <label>{{ t('user.phoneNumbers') }} *</label>
            <Button
              icon="pi pi-plus"
              :label="t('user.addPhoneNumber')"
              size="small"
              text
              @click="addEditUserPhone"
            />
          </div>
          <div class="phone-numbers-list">
            <PhoneNumberInput
              v-for="(phone, index) in editUserPhoneNumbers"
              :key="phone.id ?? `edit-${index}`"
              :model-value="phone"
              @update:model-value="updateEditUserPhone(index, $event)"
              :show-remove="editUserPhoneNumbers.length > 1"
              :index="index"
              @remove="removeEditUserPhone(index)"
            />
          </div>
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          severity="secondary"
          outlined
          @click="showEditUserDialog = false"
          :disabled="editUserLoading"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          @click="saveEditedUser"
          :loading="userStore.loading"
          :disabled="editUserLoading || !hasValidEditPhoneNumbers"
        />
      </template>
    </Dialog>
  </div>
</template>

<style scoped>
.users-page {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.page-title {
  margin: 0;
}

.user-email {
  display: flex;
  align-items: center;
}

.ml-2 {
  margin-left: 0.5rem;
}

.role-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.25rem;
}

.role-tag {
  font-size: 0.75rem;
}

.header-actions {
  display: flex;
  gap: 0.5rem;
}

.action-buttons {
  display: flex;
  gap: 0.25rem;
}

.form-grid {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-field label {
  font-weight: 500;
  font-size: 0.875rem;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.form-row-name {
  display: grid;
  grid-template-columns: 1fr 1.5fr 1.5fr;
  gap: 1rem;
}

.salutation-field {
  min-width: 100px;
}

.w-full {
  width: 100%;
}

.password-display {
  text-align: center;
}

.password-box {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  background: var(--surface-ground);
  padding: 1rem;
  border-radius: 6px;
  margin: 1rem 0;
}

.password-box code {
  font-size: 1.25rem;
  font-weight: 600;
  letter-spacing: 0.05em;
}

.password-note {
  font-size: 0.875rem;
  color: var(--text-color-secondary);
  margin-top: 1rem;
}

.filter-card {
  margin-bottom: 1rem;
}

.filter-bar {
  display: flex;
  align-items: flex-end;
  gap: 1.5rem;
  flex-wrap: wrap;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.filter-item label {
  font-weight: 500;
  font-size: 0.875rem;
  color: var(--text-color-secondary);
}

.filter-dropdown {
  min-width: 180px;
}

.toggle-item {
  flex-direction: row;
  align-items: center;
  gap: 0.75rem;
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

.p-success {
  color: var(--green-500);
}

.p-error {
  color: var(--red-500);
}

.search-item {
  flex: 1;
  min-width: 200px;
}

.search-input {
  width: 100%;
}

.phone-numbers-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.text-muted {
  color: var(--text-color-secondary);
}

.text-sm {
  font-size: 0.875rem;
}

.flex {
  display: flex;
}

.justify-between {
  justify-content: space-between;
}

.items-center {
  align-items: center;
}

.mb-2 {
  margin-bottom: 0.5rem;
}
</style>

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
import { useUsernameAvailability } from '@/composables/useUsernameAvailability'
import type { User, CreateUserRequest, AdminUpdateUserRequest } from '@/types'
import { UserStatus } from '@/types'

const { t } = useI18n()
const toast = useToast()
const confirm = useConfirm()
const userStore = useUserStore()
const authStore = useAuthStore()

const first = ref(0)
const rows = ref(10)

// Filter state
const statusFilter = ref<UserStatus | null>(null)
const showDeleted = ref(false)

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
const newUser = reactive<CreateUserRequest>({
  username: '',
  email: '',
  password: '',
  firstName: '',
  lastName: '',
  roleCodes: ['USER'],
  status: UserStatus.ACTIVE
})
const newUserUsername = toRef(newUser, 'username')
const { state: usernameState, reset: resetUsernameState } = useUsernameAvailability(newUserUsername)

// Edit User Dialog
const showEditUserDialog = ref(false)
const editingUserId = ref<number | null>(null)
const editUser = reactive<AdminUpdateUserRequest>({
  firstName: '',
  lastName: '',
  roleCodes: [],
  status: UserStatus.ACTIVE,
  emailVerified: false
})

const roleOptions = computed(() => [
  { label: t('users.roles.admin'), value: 'ADMIN' },
  { label: t('users.roles.user'), value: 'USER' }
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
})

async function loadUsers() {
  try {
    const page = Math.floor(first.value / rows.value)
    if (showDeleted.value) {
      await userStore.fetchDeletedUsers(page, rows.value)
    } else {
      await userStore.fetchUsers(page, rows.value, statusFilter.value)
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
  newUser.firstName = ''
  newUser.lastName = ''
  newUser.roleCodes = ['USER']
  newUser.status = UserStatus.ACTIVE
  resetUsernameState()
  showAddUserDialog.value = true
}

async function createUser() {
  try {
    await userStore.createUser(newUser)
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

function openEditUserDialog(user: User) {
  editingUserId.value = user.id
  editUser.firstName = user.firstName || ''
  editUser.lastName = user.lastName || ''
  editUser.roleCodes = [...user.roles]
  editUser.status = user.status
  editUser.emailVerified = user.emailVerified
  showEditUserDialog.value = true
}

async function saveEditedUser() {
  if (!editingUserId.value) return

  try {
    await userStore.updateUser(editingUserId.value, editUser)
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
      :style="{ width: '500px' }"
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
                'p-invalid': usernameState.isAvailable === false,
                'p-valid': usernameState.isAvailable === true
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
          <small
            v-if="usernameState.message"
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
          />
        </div>
        <div class="form-field">
          <label for="password">{{ t('users.addUserDialog.password') }} *</label>
          <InputText
            id="password"
            v-model="newUser.password"
            type="password"
            :placeholder="t('users.addUserDialog.passwordPlaceholder')"
            class="w-full"
          />
        </div>
        <div class="form-row">
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
          :disabled="
            !newUser.username ||
            !newUser.email ||
            !newUser.password ||
            usernameState.isAvailable === false
          "
        />
      </template>
    </Dialog>

    <!-- Temporary Password Dialog -->
    <Dialog
      v-model:visible="showPasswordDialog"
      :header="t('users.resetPasswordSuccess')"
      :modal="true"
      :style="{ width: '400px' }"
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
      :style="{ width: '500px' }"
    >
      <div class="form-grid">
        <div class="form-row">
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
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          severity="secondary"
          outlined
          @click="showEditUserDialog = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          @click="saveEditedUser"
          :loading="userStore.loading"
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
</style>

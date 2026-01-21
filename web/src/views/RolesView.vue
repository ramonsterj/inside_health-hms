<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
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
import Textarea from 'primevue/textarea'
import Checkbox from 'primevue/checkbox'
import AppLayout from '@/components/layout/AppLayout.vue'
import { useRoleStore } from '@/stores/role'
import type { Role, CreateRoleRequest, UpdateRoleRequest } from '@/types'

const { t } = useI18n()
const toast = useToast()
const confirm = useConfirm()
const roleStore = useRoleStore()

// Add Role Dialog
const showAddRoleDialog = ref(false)
const newRole = reactive<CreateRoleRequest>({
  code: '',
  name: '',
  description: '',
  permissionCodes: []
})

// Edit Role Dialog
const showEditRoleDialog = ref(false)
const editingRoleId = ref<number | null>(null)
const editRole = reactive<UpdateRoleRequest>({
  name: '',
  description: ''
})

// Permissions Dialog
const showPermissionsDialog = ref(false)
const permissionsRoleId = ref<number | null>(null)
const permissionsRoleName = ref('')
const selectedPermissions = ref<string[]>([])

// Code validation
const codePattern = /^[A-Z][A-Z0-9_]*$/
const isCodeValid = computed(() => {
  if (!newRole.code) return null
  if (newRole.code.length < 2) return false
  if (newRole.code.length > 50) return false
  return codePattern.test(newRole.code)
})

const codeValidationMessage = computed(() => {
  if (!newRole.code) return ''
  if (newRole.code.length < 2) return t('roles.validation.codeMin')
  if (newRole.code.length > 50) return t('roles.validation.codeMax')
  if (!codePattern.test(newRole.code)) return t('roles.validation.codeFormat')
  return ''
})

onMounted(async () => {
  await Promise.all([roleStore.fetchRoles(), roleStore.fetchPermissions()])
})

async function loadRoles() {
  try {
    await roleStore.fetchRoles()
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

function openAddRoleDialog() {
  newRole.code = ''
  newRole.name = ''
  newRole.description = ''
  newRole.permissionCodes = []
  showAddRoleDialog.value = true
}

async function createRole() {
  try {
    const role = await roleStore.createRole(newRole)
    showAddRoleDialog.value = false
    toast.add({
      severity: 'success',
      summary: t('roles.messages.createSuccess'),
      detail: t('roles.messages.createSuccessDetail', { name: role.name }),
      life: 3000
    })
  } catch (error) {
    const message = error instanceof Error ? error.message : t('errors.generic')
    toast.add({
      severity: 'error',
      summary: t('roles.messages.createFailed'),
      detail: message,
      life: 5000
    })
  }
}

function openEditRoleDialog(role: Role) {
  if (role.isSystem) return
  editingRoleId.value = role.id
  editRole.name = role.name
  editRole.description = role.description || ''
  showEditRoleDialog.value = true
}

async function saveEditedRole() {
  if (!editingRoleId.value) return

  try {
    await roleStore.updateRole(editingRoleId.value, editRole)
    showEditRoleDialog.value = false
    toast.add({
      severity: 'success',
      summary: t('roles.messages.updateSuccess'),
      detail: t('roles.messages.updateSuccessDetail'),
      life: 3000
    })
  } catch (error) {
    const message = error instanceof Error ? error.message : t('errors.generic')
    toast.add({
      severity: 'error',
      summary: t('roles.messages.updateFailed'),
      detail: message,
      life: 5000
    })
  }
}

function openPermissionsDialog(role: Role) {
  permissionsRoleId.value = role.id
  permissionsRoleName.value = role.name
  selectedPermissions.value = role.permissions.map(p => p.code)
  showPermissionsDialog.value = true
}

async function savePermissions() {
  if (!permissionsRoleId.value) return

  try {
    await roleStore.assignPermissions(permissionsRoleId.value, selectedPermissions.value)
    showPermissionsDialog.value = false
    toast.add({
      severity: 'success',
      summary: t('roles.messages.permissionsSuccess'),
      detail: t('roles.messages.permissionsSuccessDetail', { role: permissionsRoleName.value }),
      life: 3000
    })
  } catch (error) {
    const message = error instanceof Error ? error.message : t('errors.generic')
    toast.add({
      severity: 'error',
      summary: t('roles.messages.permissionsFailed'),
      detail: message,
      life: 5000
    })
  }
}

function selectAllPermissions() {
  selectedPermissions.value = roleStore.permissions.map(p => p.code)
}

function deselectAllPermissions() {
  selectedPermissions.value = []
}

function confirmDelete(role: Role) {
  if (role.isSystem) return

  confirm.require({
    message: t('roles.deleteConfirm', { name: role.name }),
    header: t('roles.deleteConfirmHeader'),
    icon: 'pi pi-exclamation-triangle',
    rejectClass: 'p-button-secondary p-button-outlined',
    acceptClass: 'p-button-danger',
    accept: () => deleteRole(role),
    reject: () => {}
  })
}

async function deleteRole(role: Role) {
  try {
    await roleStore.deleteRole(role.id)
    toast.add({
      severity: 'success',
      summary: t('roles.messages.deleteSuccess'),
      detail: t('roles.messages.deleteSuccessDetail', { name: role.name }),
      life: 3000
    })
  } catch (error) {
    const message = error instanceof Error ? error.message : t('errors.generic')
    toast.add({
      severity: 'error',
      summary: t('roles.messages.deleteFailed'),
      detail: message,
      life: 5000
    })
  }
}

function getPermissionGroupLabel(resource: string): string {
  const key = `roles.permissionGroups.${resource}`
  const translated = t(key)
  // If no translation found, capitalize the resource name
  return translated === key ? resource.charAt(0).toUpperCase() + resource.slice(1) : translated
}
</script>

<template>
  <AppLayout>
    <div class="roles-page">
      <div class="page-header">
        <h1 class="page-title">{{ t('roles.title') }}</h1>
        <div class="header-actions">
          <Button icon="pi pi-plus" :label="t('roles.addRole')" @click="openAddRoleDialog" />
          <Button
            icon="pi pi-refresh"
            :label="t('roles.refresh')"
            severity="secondary"
            outlined
            @click="loadRoles"
            :loading="roleStore.loading"
          />
        </div>
      </div>

      <Card>
        <template #content>
          <DataTable
            :value="roleStore.roles"
            :loading="roleStore.loading"
            dataKey="id"
            stripedRows
            scrollable
          >
            <template #empty>
              <div class="text-center p-4">
                {{ t('roles.empty') }}
              </div>
            </template>

            <Column field="code" :header="t('roles.columns.code')" sortable>
              <template #body="{ data }">
                <code class="role-code">{{ data.code }}</code>
              </template>
            </Column>

            <Column field="name" :header="t('roles.columns.name')" sortable />

            <Column field="description" :header="t('roles.columns.description')">
              <template #body="{ data }">
                {{ data.description || '-' }}
              </template>
            </Column>

            <Column field="permissions" :header="t('roles.columns.permissions')">
              <template #body="{ data }">
                <Tag
                  :value="data.permissions.length.toString()"
                  :severity="data.permissions.length > 0 ? 'info' : 'secondary'"
                />
              </template>
            </Column>

            <Column field="isSystem" :header="t('roles.columns.system')" style="width: 100px">
              <template #body="{ data }">
                <Tag
                  v-if="data.isSystem"
                  :value="t('roles.badges.system')"
                  severity="warn"
                  icon="pi pi-lock"
                />
                <Tag v-else :value="t('roles.badges.custom')" severity="secondary" />
              </template>
            </Column>

            <Column :header="t('roles.columns.actions')" style="width: 150px">
              <template #body="{ data }">
                <div class="action-buttons">
                  <Button
                    icon="pi pi-pencil"
                    severity="info"
                    text
                    rounded
                    @click="openEditRoleDialog(data)"
                    :disabled="data.isSystem"
                    v-tooltip.top="
                      data.isSystem ? t('roles.tooltips.systemRole') : t('roles.tooltips.edit')
                    "
                  />
                  <Button
                    icon="pi pi-key"
                    severity="warning"
                    text
                    rounded
                    @click="openPermissionsDialog(data)"
                    v-tooltip.top="t('roles.tooltips.permissions')"
                  />
                  <Button
                    icon="pi pi-trash"
                    severity="danger"
                    text
                    rounded
                    @click="confirmDelete(data)"
                    :disabled="data.isSystem"
                    v-tooltip.top="
                      data.isSystem ? t('roles.tooltips.systemRole') : t('roles.tooltips.delete')
                    "
                  />
                </div>
              </template>
            </Column>
          </DataTable>
        </template>
      </Card>
    </div>

    <!-- Add Role Dialog -->
    <Dialog
      v-model:visible="showAddRoleDialog"
      :header="t('roles.addDialog.title')"
      :modal="true"
      :style="{ width: '500px' }"
    >
      <div class="form-grid">
        <div class="form-field">
          <label for="code">{{ t('roles.addDialog.code') }} *</label>
          <InputText
            id="code"
            v-model="newRole.code"
            :placeholder="t('roles.addDialog.codePlaceholder')"
            class="w-full"
            :class="{ 'p-invalid': isCodeValid === false }"
            @input="newRole.code = ($event.target as HTMLInputElement).value.toUpperCase()"
          />
          <small class="hint" :class="{ 'p-error': isCodeValid === false }">
            {{ codeValidationMessage || t('roles.addDialog.codeHint') }}
          </small>
        </div>
        <div class="form-field">
          <label for="name">{{ t('roles.addDialog.name') }} *</label>
          <InputText
            id="name"
            v-model="newRole.name"
            :placeholder="t('roles.addDialog.namePlaceholder')"
            class="w-full"
          />
        </div>
        <div class="form-field">
          <label for="description">{{ t('roles.addDialog.description') }}</label>
          <Textarea
            id="description"
            v-model="newRole.description"
            :placeholder="t('roles.addDialog.descriptionPlaceholder')"
            class="w-full"
            rows="3"
            autoResize
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          severity="secondary"
          outlined
          @click="showAddRoleDialog = false"
        />
        <Button
          :label="t('roles.addRole')"
          icon="pi pi-check"
          @click="createRole"
          :loading="roleStore.loading"
          :disabled="!newRole.code || !newRole.name || isCodeValid === false"
        />
      </template>
    </Dialog>

    <!-- Edit Role Dialog -->
    <Dialog
      v-model:visible="showEditRoleDialog"
      :header="t('roles.editDialog.title')"
      :modal="true"
      :style="{ width: '500px' }"
    >
      <div class="form-grid">
        <div class="form-field">
          <label for="editName">{{ t('roles.editDialog.name') }} *</label>
          <InputText
            id="editName"
            v-model="editRole.name"
            :placeholder="t('roles.editDialog.namePlaceholder')"
            class="w-full"
          />
        </div>
        <div class="form-field">
          <label for="editDescription">{{ t('roles.editDialog.description') }}</label>
          <Textarea
            id="editDescription"
            v-model="editRole.description"
            :placeholder="t('roles.editDialog.descriptionPlaceholder')"
            class="w-full"
            rows="3"
            autoResize
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          severity="secondary"
          outlined
          @click="showEditRoleDialog = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          @click="saveEditedRole"
          :loading="roleStore.loading"
          :disabled="!editRole.name"
        />
      </template>
    </Dialog>

    <!-- Permissions Dialog -->
    <Dialog
      v-model:visible="showPermissionsDialog"
      :header="t('roles.permissionsDialog.title', { role: permissionsRoleName })"
      :modal="true"
      :style="{ width: '600px' }"
    >
      <div class="permissions-dialog-content">
        <div class="permissions-actions">
          <Button
            :label="t('roles.permissionsDialog.selectAll')"
            severity="secondary"
            text
            size="small"
            @click="selectAllPermissions"
          />
          <Button
            :label="t('roles.permissionsDialog.deselectAll')"
            severity="secondary"
            text
            size="small"
            @click="deselectAllPermissions"
          />
        </div>

        <div
          v-if="Object.keys(roleStore.permissionsByResource).length === 0"
          class="no-permissions"
        >
          {{ t('roles.permissionsDialog.noPermissions') }}
        </div>

        <div v-else class="permissions-groups">
          <div
            v-for="(perms, resource) in roleStore.permissionsByResource"
            :key="resource"
            class="permission-group"
          >
            <h4 class="group-title">{{ getPermissionGroupLabel(resource) }}</h4>
            <div class="permission-list">
              <div v-for="permission in perms" :key="permission.code" class="permission-item">
                <Checkbox
                  v-model="selectedPermissions"
                  :inputId="permission.code"
                  :value="permission.code"
                />
                <label :for="permission.code" class="permission-label">
                  <span class="permission-name">{{ permission.name }}</span>
                  <span class="permission-code">{{ permission.code }}</span>
                  <span v-if="permission.description" class="permission-description">
                    {{ permission.description }}
                  </span>
                </label>
              </div>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          severity="secondary"
          outlined
          @click="showPermissionsDialog = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          @click="savePermissions"
          :loading="roleStore.loading"
        />
      </template>
    </Dialog>
  </AppLayout>
</template>

<style scoped>
.roles-page {
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

.header-actions {
  display: flex;
  gap: 0.5rem;
}

.action-buttons {
  display: flex;
  gap: 0.25rem;
}

.role-code {
  background: var(--surface-ground);
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-size: 0.875rem;
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

.hint {
  font-size: 0.75rem;
  color: var(--text-color-secondary);
}

.w-full {
  width: 100%;
}

.permissions-dialog-content {
  max-height: 400px;
  overflow-y: auto;
}

.permissions-actions {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid var(--surface-border);
}

.no-permissions {
  text-align: center;
  padding: 2rem;
  color: var(--text-color-secondary);
}

.permissions-groups {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.permission-group {
  border: 1px solid var(--surface-border);
  border-radius: 8px;
  padding: 1rem;
}

.group-title {
  margin: 0 0 0.75rem 0;
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--primary-color);
}

.permission-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.permission-item {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
}

.permission-label {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
  cursor: pointer;
}

.permission-name {
  font-weight: 500;
  font-size: 0.875rem;
}

.permission-code {
  font-family: monospace;
  font-size: 0.75rem;
  color: var(--text-color-secondary);
}

.permission-description {
  font-size: 0.75rem;
  color: var(--text-color-secondary);
}

.text-center {
  text-align: center;
}

.p-4 {
  padding: 1rem;
}
</style>

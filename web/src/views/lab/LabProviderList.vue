<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useForm } from 'vee-validate'
import { useConfirm } from 'primevue/useconfirm'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Checkbox from 'primevue/checkbox'
import Message from 'primevue/message'
import { toTypedSchema } from '@/validation/zodI18n'
import { labProviderSchema, type LabProviderFormData } from '@/validation/lab'
import { useLabCatalogStore } from '@/stores/labCatalog'
import { useAuthStore } from '@/stores/auth'
import { useErrorHandler } from '@/composables/useErrorHandler'
import type { LabProvider } from '@/types/lab'

const { t } = useI18n()
const confirm = useConfirm()
const { showError, showSuccess } = useErrorHandler()
const store = useLabCatalogStore()
const authStore = useAuthStore()

const canManage = computed(() => authStore.hasPermission('lab-catalog:manage'))

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)

const { defineField, handleSubmit, errors, resetForm } = useForm<LabProviderFormData>({
  validationSchema: toTypedSchema(labProviderSchema),
  initialValues: { name: '', code: '', active: true }
})
const [name] = defineField('name')
const [code] = defineField('code')
const [active] = defineField('active')

onMounted(load)

async function load(): Promise<void> {
  try {
    await store.fetchProviders(false)
  } catch (error) {
    showError(error)
  }
}

function openCreate(): void {
  editingId.value = null
  resetForm({ values: { name: '', code: '', active: true } })
  dialogVisible.value = true
}

function openEdit(provider: LabProvider): void {
  editingId.value = provider.id
  resetForm({ values: { name: provider.name, code: provider.code ?? '', active: provider.active } })
  dialogVisible.value = true
}

const onSubmit = handleSubmit(async values => {
  saving.value = true
  try {
    const payload = { name: values.name, code: values.code || null, active: values.active }
    if (editingId.value != null) {
      await store.updateProvider(editingId.value, payload)
      showSuccess('lab.provider.updated')
    } else {
      await store.createProvider(payload)
      showSuccess('lab.provider.created')
    }
    dialogVisible.value = false
    await load()
  } catch (error) {
    showError(error)
  } finally {
    saving.value = false
  }
})

function confirmDelete(provider: LabProvider): void {
  confirm.require({
    message: t('lab.provider.confirmDelete'),
    header: t('common.confirm'),
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    accept: () => remove(provider.id)
  })
}

async function remove(id: number): Promise<void> {
  try {
    await store.deleteProvider(id)
    showSuccess('lab.provider.deleted')
    await load()
  } catch (error) {
    showError(error)
  }
}
</script>

<template>
  <Card>
    <template #content>
      <div class="list-header">
        <Button
          v-if="canManage"
          icon="pi pi-plus"
          :label="t('lab.provider.new')"
          @click="openCreate"
        />
        <Button
          icon="pi pi-refresh"
          :label="t('common.refresh')"
          severity="secondary"
          outlined
          :loading="store.loading"
          @click="load"
        />
      </div>

      <DataTable :value="store.providers" :loading="store.loading" dataKey="id" stripedRows>
        <template #empty>
          <div class="text-center p-4">{{ t('lab.provider.empty') }}</div>
        </template>
        <Column field="name" :header="t('lab.provider.name')" />
        <Column field="code" :header="t('lab.provider.code')">
          <template #body="{ data }">{{ data.code || '-' }}</template>
        </Column>
        <Column :header="t('lab.provider.active')" style="width: 100px">
          <template #body="{ data }">
            <Tag
              :value="data.active ? t('common.yes') : t('common.no')"
              :severity="data.active ? 'success' : 'secondary'"
            />
          </template>
        </Column>
        <Column v-if="canManage" :header="t('common.actions')" style="width: 120px">
          <template #body="{ data }">
            <Button
              icon="pi pi-pencil"
              severity="secondary"
              text
              rounded
              :aria-label="t('common.edit')"
              v-tooltip.top="t('common.edit')"
              @click="openEdit(data)"
            />
            <Button
              icon="pi pi-trash"
              severity="danger"
              text
              rounded
              :aria-label="t('common.delete')"
              v-tooltip.top="t('common.delete')"
              @click="confirmDelete(data)"
            />
          </template>
        </Column>
      </DataTable>
    </template>
  </Card>

  <Dialog
    v-model:visible="dialogVisible"
    :header="editingId != null ? t('lab.provider.edit') : t('lab.provider.new')"
    modal
    :style="{ width: '480px' }"
    :breakpoints="{ '768px': '90vw' }"
  >
    <form class="lab-form" @submit.prevent="onSubmit">
      <div class="form-field">
        <label for="provider-name">{{ t('lab.provider.name') }} *</label>
        <InputText id="provider-name" v-model="name" :class="{ 'p-invalid': errors.name }" />
        <Message v-if="errors.name" severity="error" :closable="false">{{ errors.name }}</Message>
      </div>
      <div class="form-field">
        <label for="provider-code">{{ t('lab.provider.code') }}</label>
        <InputText id="provider-code" v-model="code" :class="{ 'p-invalid': errors.code }" />
        <Message v-if="errors.code" severity="error" :closable="false">{{ errors.code }}</Message>
      </div>
      <div class="form-field-inline">
        <Checkbox v-model="active" inputId="provider-active" binary />
        <label for="provider-active">{{ t('lab.provider.active') }}</label>
      </div>
    </form>
    <template #footer>
      <Button
        :label="t('common.cancel')"
        severity="secondary"
        :disabled="saving"
        @click="dialogVisible = false"
      />
      <Button :label="t('common.save')" :loading="saving" @click="onSubmit" />
    </template>
  </Dialog>
</template>

<style scoped>
.list-header {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-bottom: 1rem;
}
.lab-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.form-field-inline {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
</style>

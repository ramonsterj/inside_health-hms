<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useForm } from 'vee-validate'
import { useConfirm } from 'primevue/useconfirm'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Select from 'primevue/select'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Checkbox from 'primevue/checkbox'
import Message from 'primevue/message'
import { toTypedSchema } from '@/validation/zodI18n'
import { createLabProviderTestSchema, type LabProviderTestFormData } from '@/validation/lab'
import { useLabCatalogStore } from '@/stores/labCatalog'
import { useAuthStore } from '@/stores/auth'
import { useErrorHandler } from '@/composables/useErrorHandler'
import { formatPrice } from '@/utils/format'
import type { LabProviderTest } from '@/types/lab'

const { t } = useI18n()
const confirm = useConfirm()
const { showError, showSuccess } = useErrorHandler()
const store = useLabCatalogStore()
const authStore = useAuthStore()

const canManage = computed(() => authStore.hasPermission('lab-catalog:manage'))

const selectedProviderId = ref<number | null>(null)
const rows = ref<LabProviderTest[]>([])
const loadingRows = ref(false)

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)

const providerOptions = computed(() => store.providers.map(p => ({ label: p.name, value: p.id })))

// Canonical tests not yet offered by the selected provider (create dialog only).
const availableTestOptions = computed(() => {
  const offered = new Set(rows.value.map(r => r.labTestId))
  return store.tests
    .filter(test => test.active && !offered.has(test.id))
    .map(test => ({ label: test.name, value: test.id }))
})

const { defineField, handleSubmit, errors, resetForm } = useForm<LabProviderTestFormData>({
  validationSchema: toTypedSchema(createLabProviderTestSchema),
  initialValues: { labTestId: 0, displayName: '', cost: 0, salesPrice: 0, active: true }
})
const [labTestId] = defineField('labTestId')
const [displayName] = defineField('displayName')
const [cost] = defineField('cost')
const [salesPrice] = defineField('salesPrice')
const [active] = defineField('active')

onMounted(async () => {
  try {
    await Promise.all([store.fetchProviders(false), store.fetchTests(false)])
    const first = store.providers[0]
    if (first) {
      selectedProviderId.value = first.id
    }
  } catch (error) {
    showError(error)
  }
})

watch(selectedProviderId, loadRows)

async function loadRows(): Promise<void> {
  if (selectedProviderId.value == null) {
    rows.value = []
    return
  }
  loadingRows.value = true
  try {
    rows.value = await store.fetchProviderTests(selectedProviderId.value, false)
  } catch (error) {
    showError(error)
  } finally {
    loadingRows.value = false
  }
}

function openCreate(): void {
  editingId.value = null
  resetForm({ values: { labTestId: 0, displayName: '', cost: 0, salesPrice: 0, active: true } })
  dialogVisible.value = true
}

function openEdit(row: LabProviderTest): void {
  editingId.value = row.id
  resetForm({
    values: {
      labTestId: row.labTestId,
      displayName: row.displayName,
      cost: row.cost,
      salesPrice: row.salesPrice,
      active: row.active
    }
  })
  dialogVisible.value = true
}

const onSubmit = handleSubmit(async values => {
  if (selectedProviderId.value == null) return
  saving.value = true
  try {
    if (editingId.value != null) {
      await store.updateProviderTest(editingId.value, {
        displayName: values.displayName || null,
        cost: values.cost,
        salesPrice: values.salesPrice,
        active: values.active
      })
      showSuccess('lab.providerTest.updated')
    } else {
      await store.createProviderTest(selectedProviderId.value, {
        labTestId: values.labTestId,
        displayName: values.displayName || null,
        cost: values.cost,
        salesPrice: values.salesPrice,
        active: values.active
      })
      showSuccess('lab.providerTest.created')
    }
    dialogVisible.value = false
    await loadRows()
  } catch (error) {
    showError(error)
  } finally {
    saving.value = false
  }
})

function confirmDelete(row: LabProviderTest): void {
  confirm.require({
    message: t('lab.providerTest.confirmDelete'),
    header: t('common.confirm'),
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    accept: () => remove(row.id)
  })
}

async function remove(id: number): Promise<void> {
  try {
    await store.deleteProviderTest(id)
    showSuccess('lab.providerTest.deleted')
    await loadRows()
  } catch (error) {
    showError(error)
  }
}
</script>

<template>
  <Card>
    <template #content>
      <div class="panel-toolbar">
        <Select
          v-model="selectedProviderId"
          :options="providerOptions"
          optionLabel="label"
          optionValue="value"
          :placeholder="t('lab.providerTest.selectProvider')"
          class="provider-select"
        />
        <div class="toolbar-actions">
          <Button
            v-if="canManage"
            icon="pi pi-plus"
            :label="t('lab.providerTest.new')"
            :disabled="selectedProviderId == null"
            @click="openCreate"
          />
          <Button
            icon="pi pi-refresh"
            :label="t('common.refresh')"
            severity="secondary"
            outlined
            :loading="loadingRows"
            @click="loadRows"
          />
        </div>
      </div>

      <DataTable :value="rows" :loading="loadingRows" dataKey="id" stripedRows>
        <template #empty>
          <div class="text-center p-4">{{ t('lab.providerTest.empty') }}</div>
        </template>
        <Column field="labTestName" :header="t('lab.providerTest.canonicalTest')" />
        <Column field="displayName" :header="t('lab.providerTest.displayName')" />
        <Column :header="t('lab.providerTest.cost')" style="width: 120px">
          <template #body="{ data }">{{ formatPrice(data.cost) }}</template>
        </Column>
        <Column :header="t('lab.providerTest.salesPrice')" style="width: 120px">
          <template #body="{ data }">{{ formatPrice(data.salesPrice) }}</template>
        </Column>
        <Column :header="t('lab.providerTest.active')" style="width: 100px">
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
    :header="editingId != null ? t('lab.providerTest.edit') : t('lab.providerTest.new')"
    modal
    :style="{ width: '520px' }"
    :breakpoints="{ '768px': '90vw' }"
  >
    <form class="lab-form" @submit.prevent="onSubmit">
      <div v-if="editingId == null" class="form-field">
        <label for="pt-test">{{ t('lab.providerTest.canonicalTest') }} *</label>
        <Select
          id="pt-test"
          v-model="labTestId"
          :options="availableTestOptions"
          optionLabel="label"
          optionValue="value"
          filter
          :placeholder="t('lab.providerTest.selectTest')"
          :class="{ 'p-invalid': errors.labTestId }"
        />
        <Message v-if="errors.labTestId" severity="error" :closable="false">
          {{ errors.labTestId }}
        </Message>
      </div>
      <div class="form-field">
        <label for="pt-display">{{ t('lab.providerTest.displayName') }}</label>
        <InputText
          id="pt-display"
          v-model="displayName"
          :placeholder="t('lab.providerTest.displayNamePlaceholder')"
          :class="{ 'p-invalid': errors.displayName }"
        />
        <Message v-if="errors.displayName" severity="error" :closable="false">
          {{ errors.displayName }}
        </Message>
      </div>
      <div class="form-row">
        <div class="form-field">
          <label for="pt-cost">{{ t('lab.providerTest.cost') }} *</label>
          <InputNumber
            id="pt-cost"
            v-model="cost"
            mode="currency"
            currency="GTQ"
            locale="es-GT"
            :min="0"
            :class="{ 'p-invalid': errors.cost }"
          />
          <Message v-if="errors.cost" severity="error" :closable="false">{{ errors.cost }}</Message>
        </div>
        <div class="form-field">
          <label for="pt-price">{{ t('lab.providerTest.salesPrice') }} *</label>
          <InputNumber
            id="pt-price"
            v-model="salesPrice"
            mode="currency"
            currency="GTQ"
            locale="es-GT"
            :min="0"
            :class="{ 'p-invalid': errors.salesPrice }"
          />
          <Message v-if="errors.salesPrice" severity="error" :closable="false">
            {{ errors.salesPrice }}
          </Message>
        </div>
      </div>
      <div class="form-field-inline">
        <Checkbox v-model="active" inputId="pt-active" binary />
        <label for="pt-active">{{ t('lab.providerTest.active') }}</label>
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
.panel-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 1rem;
}
.provider-select {
  min-width: 260px;
}
.toolbar-actions {
  display: flex;
  gap: 0.5rem;
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
.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}
.form-field-inline {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
</style>

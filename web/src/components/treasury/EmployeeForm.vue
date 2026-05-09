<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@/validation/zodI18n'
import { useErrorHandler } from '@/composables/useErrorHandler'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Textarea from 'primevue/textarea'
import Select from 'primevue/select'
import DatePicker from 'primevue/datepicker'
import Message from 'primevue/message'
import { useTreasuryEmployeeStore } from '@/stores/treasuryEmployee'
import { EmployeeType, DoctorFeeArrangement } from '@/types/treasury'
import { toApiDate } from '@/utils/format'
import type { TreasuryEmployee } from '@/types/treasury'
import {
  createTreasuryEmployeeSchema,
  updateTreasuryEmployeeSchema,
  type CreateTreasuryEmployeeFormData
} from '@/validation/treasury'

const props = defineProps<{
  visible: boolean
  employee?: TreasuryEmployee | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  saved: []
}>()

const { t } = useI18n()
const { showError, showSuccess } = useErrorHandler()
const employeeStore = useTreasuryEmployeeStore()

const loading = ref(false)
const localHireDate = ref<Date | null>(null)
const isEditing = computed(() => !!props.employee)

const employeeTypeOptions = computed(() =>
  Object.values(EmployeeType).map(v => ({
    label: t(`treasury.employee.types.${v}`),
    value: v
  }))
)

const doctorFeeArrangementOptions = computed(() =>
  Object.values(DoctorFeeArrangement).map(v => ({
    label: t(`treasury.employee.doctorFeeArrangements.${v}`),
    value: v
  }))
)

const schema = computed(() =>
  isEditing.value ? updateTreasuryEmployeeSchema : createTreasuryEmployeeSchema
)

const { defineField, handleSubmit, errors, resetForm, values } =
  useForm<CreateTreasuryEmployeeFormData>({
    validationSchema: computed(() => toTypedSchema(schema.value)),
    initialValues: {
      fullName: '',
      employeeType: EmployeeType.PAYROLL,
      taxId: '',
      position: '',
      baseSalary: null,
      contractedRate: null,
      doctorFeeArrangement: null,
      hospitalCommissionPct: 0,
      hireDate: '',
      userId: null,
      notes: ''
    }
  })

const [fullName] = defineField('fullName')
const [employeeType] = defineField('employeeType')
const [taxId] = defineField('taxId')
const [position] = defineField('position')
const [baseSalary] = defineField('baseSalary')
const [contractedRate] = defineField('contractedRate')
const [doctorFeeArrangement] = defineField('doctorFeeArrangement')
const [hospitalCommissionPct] = defineField('hospitalCommissionPct')
const [hireDate] = defineField('hireDate')
const [notes] = defineField('notes')

const selectedType = computed(() =>
  isEditing.value ? props.employee?.employeeType : values.employeeType
)

watch(localHireDate, val => {
  hireDate.value = val ? toApiDate(val) : ''
})

watch(
  () => props.visible,
  visible => {
    if (visible) {
      if (props.employee) {
        resetForm({
          values: {
            fullName: props.employee.fullName,
            taxId: props.employee.taxId ?? '',
            position: props.employee.position ?? '',
            contractedRate: props.employee.contractedRate ?? null,
            doctorFeeArrangement: props.employee.doctorFeeArrangement ?? null,
            hospitalCommissionPct: props.employee.hospitalCommissionPct,
            hireDate: props.employee.hireDate ?? '',
            userId: props.employee.userId ?? null,
            notes: props.employee.notes ?? ''
          }
        })
        localHireDate.value = props.employee.hireDate
          ? new Date(props.employee.hireDate + 'T00:00:00')
          : null
      } else {
        resetForm()
        localHireDate.value = null
      }
    }
  }
)

const onSubmit = handleSubmit(async formValues => {
  loading.value = true
  try {
    const data = {
      ...formValues,
      taxId: (formValues.taxId as string) || null,
      position: (formValues.position as string) || null,
      hireDate: (formValues.hireDate as string) || null,
      notes: (formValues.notes as string) || null
    }
    if (isEditing.value && props.employee) {
      await employeeStore.updateEmployee(props.employee.id, data)
      showSuccess('treasury.employee.updated')
    } else {
      await employeeStore.createEmployee(data)
      showSuccess('treasury.employee.created')
    }
    emit('saved')
    onHide()
  } catch (error) {
    showError(error)
  } finally {
    loading.value = false
  }
})

function onHide() {
  resetForm()
  localHireDate.value = null
  emit('update:visible', false)
}
</script>

<template>
  <Dialog
    :visible="visible"
    @update:visible="onHide"
    :header="isEditing ? t('treasury.employee.edit') : t('treasury.employee.new')"
    modal
    :style="{ width: '600px' }"
    :breakpoints="{ '768px': '95vw' }"
  >
    <form @submit.prevent="onSubmit" class="employee-form">
      <div class="form-row">
        <div class="form-field">
          <label for="emp-name">{{ t('treasury.employee.fullName') }} *</label>
          <InputText id="emp-name" v-model="fullName" :class="{ 'p-invalid': errors.fullName }" />
          <Message v-if="errors.fullName" severity="error" :closable="false">
            {{ errors.fullName }}
          </Message>
        </div>

        <div class="form-field">
          <label for="emp-type">{{ t('treasury.employee.employeeType') }} *</label>
          <Select
            id="emp-type"
            v-model="employeeType"
            :options="employeeTypeOptions"
            option-label="label"
            option-value="value"
            :disabled="isEditing"
            :class="{ 'p-invalid': errors.employeeType }"
          />
          <Message v-if="errors.employeeType" severity="error" :closable="false">
            {{ errors.employeeType }}
          </Message>
        </div>
      </div>

      <div class="form-row">
        <div class="form-field">
          <label for="emp-tax">{{ t('treasury.employee.taxId') }}</label>
          <InputText id="emp-tax" v-model="taxId" />
        </div>

        <div class="form-field">
          <label for="emp-position">{{ t('treasury.employee.position') }}</label>
          <InputText id="emp-position" v-model="position" />
        </div>
      </div>

      <!-- PAYROLL specific -->
      <div v-if="selectedType === EmployeeType.PAYROLL && !isEditing" class="form-field">
        <label for="emp-salary">{{ t('treasury.employee.baseSalary') }} *</label>
        <InputNumber
          id="emp-salary"
          v-model="baseSalary"
          :min="0.01"
          :max-fraction-digits="2"
          :class="{ 'p-invalid': errors.baseSalary }"
        />
        <Message v-if="errors.baseSalary" severity="error" :closable="false">
          {{ errors.baseSalary }}
        </Message>
      </div>

      <!-- CONTRACTOR specific -->
      <div v-if="selectedType === EmployeeType.CONTRACTOR" class="form-field">
        <label for="emp-rate">{{ t('treasury.employee.contractedRate') }} *</label>
        <InputNumber
          id="emp-rate"
          v-model="contractedRate"
          :min="0.01"
          :max-fraction-digits="2"
          :class="{ 'p-invalid': errors.contractedRate }"
        />
        <Message v-if="errors.contractedRate" severity="error" :closable="false">
          {{ errors.contractedRate }}
        </Message>
      </div>

      <!-- DOCTOR specific -->
      <div v-if="selectedType === EmployeeType.DOCTOR" class="form-row">
        <div class="form-field">
          <label for="emp-fee-arrangement">{{ t('treasury.employee.doctorFeeArrangement') }}</label>
          <Select
            id="emp-fee-arrangement"
            v-model="doctorFeeArrangement"
            :options="doctorFeeArrangementOptions"
            option-label="label"
            option-value="value"
          />
        </div>

        <div class="form-field">
          <label for="emp-commission">{{ t('treasury.employee.hospitalCommissionPct') }}</label>
          <InputNumber
            id="emp-commission"
            v-model="hospitalCommissionPct"
            :min="0"
            :max="100"
            :max-fraction-digits="2"
            suffix="%"
          />
        </div>
      </div>

      <div class="form-row">
        <div class="form-field">
          <label for="emp-hire">{{ t('treasury.employee.hireDate') }}</label>
          <DatePicker id="emp-hire" v-model="localHireDate" />
        </div>
      </div>

      <div class="form-field">
        <label for="emp-notes">{{ t('treasury.employee.notes') }}</label>
        <Textarea id="emp-notes" v-model="notes" rows="2" />
      </div>

      <div class="form-actions">
        <Button
          type="button"
          :label="t('common.cancel')"
          severity="secondary"
          outlined
          @click="onHide"
        />
        <Button type="submit" :label="t('common.save')" :loading="loading" />
      </div>
    </form>
  </Dialog>
</template>

<style scoped>
.employee-form {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.form-field label {
  font-weight: 500;
  font-size: 0.9rem;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 0.5rem;
}

@media (max-width: 600px) {
  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>

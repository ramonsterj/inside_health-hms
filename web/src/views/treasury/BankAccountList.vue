<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useConfirm } from 'primevue/useconfirm'
import { useErrorHandler } from '@/composables/useErrorHandler'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import BankAccountForm from '@/components/treasury/BankAccountForm.vue'
import { useBankAccountStore } from '@/stores/bankAccount'
import { useAuthStore } from '@/stores/auth'
import type { BankAccount } from '@/types/treasury'
import { formatCurrency } from '@/utils/format'

const { t } = useI18n()
const confirm = useConfirm()
const { showError, showSuccess } = useErrorHandler()
const bankAccountStore = useBankAccountStore()
const authStore = useAuthStore()

const showForm = ref(false)
const selectedBankAccount = ref<BankAccount | null>(null)

const canConfigure = computed(() => authStore.hasPermission('treasury:configure'))

onMounted(() => loadAccounts())

async function loadAccounts() {
  try {
    await bankAccountStore.fetchBankAccounts()
  } catch (error) {
    showError(error)
  }
}

function openCreate() {
  selectedBankAccount.value = null
  showForm.value = true
}

function openEdit(account: BankAccount) {
  selectedBankAccount.value = account
  showForm.value = true
}

function onSaved() {
  loadAccounts()
}

function confirmDelete(account: BankAccount) {
  confirm.require({
    message: t('treasury.bankAccount.confirmDelete'),
    header: t('common.confirm'),
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    accept: () => deleteAccount(account.id)
  })
}

async function deleteAccount(id: number) {
  try {
    await bankAccountStore.deleteBankAccount(id)
    showSuccess('treasury.bankAccount.deleted')
    loadAccounts()
  } catch (error) {
    showError(error)
  }
}
</script>

<template>
  <div class="bank-account-list">
    <div class="page-header">
      <h1 class="page-title">{{ t('treasury.bankAccount.title') }}</h1>
      <div class="header-actions">
        <Button
          v-if="canConfigure"
          icon="pi pi-plus"
          :label="t('treasury.bankAccount.new')"
          @click="openCreate"
        />
        <Button
          icon="pi pi-refresh"
          :label="t('common.refresh')"
          severity="secondary"
          outlined
          @click="loadAccounts"
          :loading="bankAccountStore.loading"
        />
      </div>
    </div>

    <Card>
      <template #content>
        <DataTable
          :value="bankAccountStore.bankAccounts"
          :loading="bankAccountStore.loading"
          data-key="id"
          striped-rows
        >
          <template #empty>
            <div class="text-center p-4">{{ t('treasury.bankAccount.empty') }}</div>
          </template>

          <Column :header="t('treasury.bankAccount.name')">
            <template #body="{ data }">
              <span>{{ data.name }}</span>
              <Tag
                v-if="data.isPettyCash"
                :value="t('treasury.bankAccount.pettyCashBadge')"
                severity="info"
                class="ml-2"
              />
            </template>
          </Column>

          <Column field="bankName" :header="t('treasury.bankAccount.bankName')">
            <template #body="{ data }">{{ data.bankName || '—' }}</template>
          </Column>

          <Column :header="t('treasury.bankAccount.maskedAccountNumber')">
            <template #body="{ data }">{{ data.maskedAccountNumber || '—' }}</template>
          </Column>

          <Column :header="t('treasury.bankAccount.accountType')" style="width: 120px">
            <template #body="{ data }">
              {{ t(`treasury.bankAccount.accountTypes.${data.accountType}`) }}
            </template>
          </Column>

          <Column :header="t('treasury.bankAccount.openingBalance')" style="width: 140px">
            <template #body="{ data }">
              {{ formatCurrency(data.openingBalance) }}
            </template>
          </Column>

          <Column :header="t('treasury.bankAccount.bookBalance')" style="width: 140px">
            <template #body="{ data }">
              <span :class="data.bookBalance < 0 ? 'text-red-500' : ''">
                {{ formatCurrency(data.bookBalance) }}
              </span>
            </template>
          </Column>

          <Column :header="t('treasury.bankAccount.active')" style="width: 90px">
            <template #body="{ data }">
              <Tag
                :value="data.active ? t('common.yes') : t('common.no')"
                :severity="data.active ? 'success' : 'secondary'"
              />
            </template>
          </Column>

          <Column :header="t('common.actions')" style="width: 100px">
            <template #body="{ data }">
              <div class="action-buttons">
                <Button
                  v-if="canConfigure"
                  icon="pi pi-pencil"
                  severity="secondary"
                  text
                  rounded
                  v-tooltip.top="t('common.edit')"
                  :aria-label="t('common.edit')"
                  @click="openEdit(data)"
                />
                <Button
                  v-if="canConfigure && !data.isPettyCash"
                  icon="pi pi-trash"
                  severity="danger"
                  text
                  rounded
                  v-tooltip.top="t('common.delete')"
                  :aria-label="t('common.delete')"
                  @click="confirmDelete(data)"
                />
              </div>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>

    <BankAccountForm
      v-model:visible="showForm"
      :bank-account="selectedBankAccount"
      @saved="onSaved"
    />
  </div>
</template>

<style scoped>
.bank-account-list {
  max-width: 1100px;
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
  align-items: center;
  gap: 0.25rem;
}

.ml-2 {
  margin-left: 0.5rem;
}

.text-red-500 {
  color: var(--p-red-500);
}
</style>

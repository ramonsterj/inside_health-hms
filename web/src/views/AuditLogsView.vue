<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useToast } from 'primevue/usetoast'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Select from 'primevue/select'
import InputNumber from 'primevue/inputnumber'
import AppLayout from '@/components/layout/AppLayout.vue'
import { useAuditStore } from '@/stores/audit'
import { AuditAction, type AuditLog } from '@/types/audit'

const { t } = useI18n()
const toast = useToast()
const auditStore = useAuditStore()

const first = ref(0)
const rows = ref(10)
const expandedRows = ref<AuditLog[]>([])

// Filter state
const selectedAction = ref<AuditAction | null>(null)
const selectedEntityType = ref<string | null>(null)
const selectedUserId = ref<number | null>(null)

// Action options for dropdown
const actionOptions = computed(() => [
  { label: t('auditLogs.filters.allActions'), value: null },
  { label: t('auditLogs.actions.CREATE'), value: AuditAction.CREATE },
  { label: t('auditLogs.actions.UPDATE'), value: AuditAction.UPDATE },
  { label: t('auditLogs.actions.DELETE'), value: AuditAction.DELETE }
])

// Entity type options (dynamically populated from logs)
const entityTypeOptions = computed(() => {
  const types = new Set(auditStore.logs.map(log => log.entityType))
  return [
    { label: t('auditLogs.filters.allEntities'), value: null },
    ...Array.from(types).map(type => ({ label: type, value: type }))
  ]
})

onMounted(() => {
  loadLogs()
})

async function loadLogs() {
  try {
    // Update filters in store
    auditStore.setFilters({
      action: selectedAction.value ?? undefined,
      entityType: selectedEntityType.value ?? undefined,
      userId: selectedUserId.value ?? undefined
    })
    const page = Math.floor(first.value / rows.value)
    await auditStore.fetchLogs(page, rows.value)
  } catch (error) {
    const message = error instanceof Error ? error.message : t('auditLogs.loadFailed')
    toast.add({
      severity: 'error',
      summary: t('common.error'),
      detail: message,
      life: 5000
    })
  }
}

function onPageChange(event: { first: number; rows: number }) {
  first.value = event.first
  rows.value = event.rows
  loadLogs()
}

function applyFilters() {
  first.value = 0
  loadLogs()
}

function clearFilters() {
  selectedAction.value = null
  selectedEntityType.value = null
  selectedUserId.value = null
  first.value = 0
  auditStore.clearFilters()
  loadLogs()
}

function getActionSeverity(action: AuditAction): 'success' | 'info' | 'danger' {
  switch (action) {
    case AuditAction.CREATE:
      return 'success'
    case AuditAction.UPDATE:
      return 'info'
    case AuditAction.DELETE:
      return 'danger'
    default:
      return 'info'
  }
}

function formatTimestamp(timestamp: string): string {
  return new Date(timestamp).toLocaleString()
}

function formatJson(jsonString: string | null): string {
  if (!jsonString) return '-'
  try {
    const obj = JSON.parse(jsonString)
    return JSON.stringify(obj, null, 2)
  } catch {
    return jsonString
  }
}

function getChangedFields(oldValues: string | null, newValues: string | null): string[] {
  if (!oldValues || !newValues) return []
  try {
    const oldObj = new Map<string, unknown>(Object.entries(JSON.parse(oldValues)))
    const newObj = JSON.parse(newValues) as Record<string, unknown>
    const changed: string[] = []

    for (const [key, newValue] of Object.entries(newObj)) {
      const oldValue = oldObj.get(key)
      if (JSON.stringify(oldValue) !== JSON.stringify(newValue)) {
        changed.push(key)
      }
    }
    return changed
  } catch {
    return []
  }
}
</script>

<template>
  <AppLayout>
    <div class="audit-logs-page">
      <div class="page-header">
        <h1 class="page-title">{{ t('auditLogs.title') }}</h1>
        <Button
          icon="pi pi-refresh"
          :label="t('auditLogs.refresh')"
          severity="secondary"
          outlined
          @click="loadLogs"
          :loading="auditStore.loading"
        />
      </div>

      <!-- Filters -->
      <Card class="filters-card">
        <template #content>
          <div class="filters-row">
            <div class="filter-item">
              <label id="label-filter-action">{{ t('auditLogs.filters.action') }}</label>
              <Select
                v-model="selectedAction"
                :options="actionOptions"
                optionLabel="label"
                optionValue="value"
                :placeholder="t('auditLogs.filters.allActions')"
                class="filter-select"
                aria-labelledby="label-filter-action"
              />
            </div>
            <div class="filter-item">
              <label id="label-filter-entity-type">{{ t('auditLogs.filters.entityType') }}</label>
              <Select
                v-model="selectedEntityType"
                :options="entityTypeOptions"
                optionLabel="label"
                optionValue="value"
                :placeholder="t('auditLogs.filters.allEntities')"
                class="filter-select"
                aria-labelledby="label-filter-entity-type"
              />
            </div>
            <div class="filter-item">
              <label id="label-filter-user-id">{{ t('auditLogs.filters.userId') }}</label>
              <InputNumber
                v-model="selectedUserId"
                :placeholder="t('auditLogs.filters.anyUser')"
                class="filter-input"
                :useGrouping="false"
                aria-labelledby="label-filter-user-id"
              />
            </div>
            <div class="filter-actions">
              <Button
                :label="t('auditLogs.filters.apply')"
                icon="pi pi-filter"
                @click="applyFilters"
                :loading="auditStore.loading"
              />
              <Button
                :label="t('auditLogs.filters.clear')"
                icon="pi pi-filter-slash"
                severity="secondary"
                outlined
                @click="clearFilters"
              />
            </div>
          </div>
        </template>
      </Card>

      <!-- Data Table -->
      <Card>
        <template #content>
          <DataTable
            v-model:expandedRows="expandedRows"
            :value="auditStore.logs"
            :loading="auditStore.loading"
            :paginator="true"
            v-model:rows="rows"
            v-model:first="first"
            :totalRecords="auditStore.totalLogs"
            :lazy="true"
            @page="onPageChange"
            :rowsPerPageOptions="[10, 25, 50, 100]"
            dataKey="id"
            stripedRows
            scrollable
          >
            <template #empty>
              <div class="text-center p-4">{{ t('auditLogs.empty') }}</div>
            </template>

            <Column expander style="width: 3rem" />

            <Column field="id" :header="t('auditLogs.columns.id')" style="width: 80px" />

            <Column field="timestamp" :header="t('auditLogs.columns.timestamp')" sortable>
              <template #body="{ data }">
                {{ formatTimestamp(data.timestamp) }}
              </template>
            </Column>

            <Column field="action" :header="t('auditLogs.columns.action')" style="width: 100px">
              <template #body="{ data }">
                <Tag
                  :value="t(`auditLogs.actions.${data.action}`)"
                  :severity="getActionSeverity(data.action)"
                />
              </template>
            </Column>

            <Column field="entityType" :header="t('auditLogs.columns.entity')" style="width: 120px">
              <template #body="{ data }">
                <span class="entity-badge">{{ data.entityType }}</span>
              </template>
            </Column>

            <Column
              field="entityId"
              :header="t('auditLogs.columns.entityId')"
              style="width: 100px"
            />

            <Column field="username" :header="t('auditLogs.columns.user')">
              <template #body="{ data }">
                <span v-if="data.username">{{ data.username }}</span>
                <span v-else-if="data.userId" class="text-muted">ID: {{ data.userId }}</span>
                <span v-else class="text-muted">{{ t('auditLogs.system') }}</span>
              </template>
            </Column>

            <Column
              field="ipAddress"
              :header="t('auditLogs.columns.ipAddress')"
              style="width: 140px"
            >
              <template #body="{ data }">
                {{ data.ipAddress || '-' }}
              </template>
            </Column>

            <Column :header="t('auditLogs.columns.changes')" style="width: 150px">
              <template #body="{ data }">
                <div v-if="data.action === 'UPDATE'" class="changed-fields">
                  <Tag
                    v-for="field in getChangedFields(data.oldValues, data.newValues).slice(0, 3)"
                    :key="field"
                    :value="field"
                    severity="secondary"
                    class="field-tag"
                  />
                  <span
                    v-if="getChangedFields(data.oldValues, data.newValues).length > 3"
                    class="more-fields"
                  >
                    {{
                      t('auditLogs.moreFields', {
                        count: getChangedFields(data.oldValues, data.newValues).length - 3
                      })
                    }}
                  </span>
                </div>
                <span v-else class="text-muted">-</span>
              </template>
            </Column>

            <!-- Expanded Row Content -->
            <template #expansion="{ data }">
              <div class="expansion-content">
                <div class="json-panels">
                  <div v-if="data.oldValues" class="json-panel">
                    <h4>{{ t('auditLogs.expansion.oldValues') }}</h4>
                    <pre class="json-display">{{ formatJson(data.oldValues) }}</pre>
                  </div>
                  <div v-if="data.newValues" class="json-panel">
                    <h4>{{ t('auditLogs.expansion.newValues') }}</h4>
                    <pre class="json-display">{{ formatJson(data.newValues) }}</pre>
                  </div>
                  <div v-if="!data.oldValues && !data.newValues" class="no-data">
                    {{ t('auditLogs.expansion.noData') }}
                  </div>
                </div>
              </div>
            </template>
          </DataTable>
        </template>
      </Card>
    </div>
  </AppLayout>
</template>

<style scoped>
.audit-logs-page {
  max-width: 1400px;
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

.filters-card {
  margin-bottom: 1rem;
}

.filters-row {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  align-items: flex-end;
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

.filter-select,
.filter-input {
  min-width: 150px;
}

.filter-actions {
  display: flex;
  gap: 0.5rem;
  margin-left: auto;
}

.entity-badge {
  background: var(--surface-200);
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-family: monospace;
  font-size: 0.875rem;
}

.text-muted {
  color: var(--text-color-secondary);
}

.changed-fields {
  display: flex;
  flex-wrap: wrap;
  gap: 0.25rem;
  align-items: center;
}

.field-tag {
  font-size: 0.75rem;
}

.more-fields {
  font-size: 0.75rem;
  color: var(--text-color-secondary);
}

.expansion-content {
  padding: 1rem;
  background: var(--surface-50);
  border-radius: 8px;
  margin: 0.5rem 0;
}

.json-panels {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1rem;
}

.json-panel h4 {
  margin: 0 0 0.5rem 0;
  font-size: 0.875rem;
  color: var(--text-color-secondary);
}

.json-display {
  background: var(--surface-0);
  border: 1px solid var(--surface-200);
  border-radius: 4px;
  padding: 1rem;
  margin: 0;
  overflow-x: auto;
  font-size: 0.8125rem;
  line-height: 1.5;
  max-height: 300px;
  overflow-y: auto;
}

.no-data {
  color: var(--text-color-secondary);
  font-style: italic;
}
</style>

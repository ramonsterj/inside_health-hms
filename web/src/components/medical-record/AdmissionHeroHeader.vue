<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import Card from 'primevue/card'
import { formatDateTime, getContrastColor, getFullName } from '@/utils/format'
import { AdmissionStatus, type AdmissionDetail } from '@/types/admission'
import AdmissionTypeBadge from '@/components/admissions/AdmissionTypeBadge.vue'

const props = defineProps<{
  admission: AdmissionDetail
  admissionStatus: AdmissionStatus
}>()

const { t } = useI18n()

const isDischarged = computed(() => props.admissionStatus === AdmissionStatus.DISCHARGED)

const patientName = computed(() =>
  getFullName(props.admission.patient.firstName, props.admission.patient.lastName)
)

const initials = computed(() => {
  const first = props.admission.patient.firstName?.trim().charAt(0) ?? ''
  const last = props.admission.patient.lastName?.trim().charAt(0) ?? ''
  return `${first}${last}`.toUpperCase() || '?'
})

function formatDoctorName(
  doctor:
    | {
        salutation: string | null
        firstName: string | null
        lastName: string | null
      }
    | null
    | undefined
): string {
  if (!doctor) return '-'
  const salutationLabel = doctor.salutation ? t(`user.salutations.${doctor.salutation}`) : ''
  return `${salutationLabel} ${getFullName(doctor.firstName, doctor.lastName)}`.trim() || '-'
}
</script>

<template>
  <Card
    class="patient-hero"
    :class="{ 'is-discharged': isDischarged }"
    data-testid="admission-hero"
  >
    <template #content>
      <div class="hero-banner">
        <div class="hero-identity">
          <div class="hero-avatar">{{ initials }}</div>
          <div class="hero-text">
            <h1 class="hero-name">{{ patientName }}</h1>
            <span class="hero-doc">
              <i class="pi pi-id-card" /> {{ admission.patient.idDocumentNumber || '-' }}
            </span>
            <div class="hero-chips">
              <span class="chip" :class="isDischarged ? 'chip-muted' : 'chip-active'">
                <i class="pi" :class="isDischarged ? 'pi-lock' : 'pi-circle-fill'" />
                {{ t(`admission.statuses.${admissionStatus}`) }}
              </span>
              <AdmissionTypeBadge :type="admission.type" />
              <span
                v-if="admission.triageCode"
                class="chip chip-triage"
                :style="{
                  backgroundColor: admission.triageCode.color,
                  color: getContrastColor(admission.triageCode.color)
                }"
              >
                {{ admission.triageCode.code }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <div class="hero-facts">
        <div class="fact">
          <i class="pi pi-home fact-icon" />
          <div class="fact-text">
            <span class="fact-label">{{ t('admission.room') }}</span>
            <span class="fact-value">{{ admission.room?.number || '-' }}</span>
          </div>
        </div>
        <div class="fact">
          <i class="pi pi-user fact-icon" />
          <div class="fact-text">
            <span class="fact-label">{{ t('admission.treatingPhysician') }}</span>
            <span class="fact-value">{{ formatDoctorName(admission.treatingPhysician) }}</span>
          </div>
        </div>
        <div class="fact">
          <i class="pi pi-user-plus fact-icon" />
          <div class="fact-text">
            <span class="fact-label">{{ t('admission.resident') }}</span>
            <span class="fact-value">{{ formatDoctorName(admission.resident) }}</span>
          </div>
        </div>
        <div class="fact">
          <i class="pi pi-calendar-plus fact-icon" />
          <div class="fact-text">
            <span class="fact-label">{{ t('admission.admissionDate') }}</span>
            <span class="fact-value">{{ formatDateTime(admission.admissionDate) }}</span>
          </div>
        </div>
        <div v-if="isDischarged && admission.dischargeDate" class="fact">
          <i class="pi pi-calendar-minus fact-icon" />
          <div class="fact-text">
            <span class="fact-label">{{ t('admission.dischargeDate') }}</span>
            <span class="fact-value">{{ formatDateTime(admission.dischargeDate) }}</span>
          </div>
        </div>
      </div>
    </template>
  </Card>
</template>

<style scoped>
.patient-hero {
  overflow: hidden;
}
.patient-hero :deep(.p-card-body),
.patient-hero :deep(.p-card-content) {
  padding: 0;
}

.hero-banner {
  background: linear-gradient(
    135deg,
    var(--p-primary-color),
    color-mix(in srgb, var(--p-primary-color) 55%, #1e1b4b)
  );
  padding: 1.5rem;
  color: #fff;
}
.patient-hero.is-discharged .hero-banner {
  background: linear-gradient(135deg, var(--p-surface-500), var(--p-surface-700));
}
.hero-identity {
  display: flex;
  align-items: center;
  gap: 1.25rem;
}
.hero-avatar {
  width: 64px;
  height: 64px;
  flex-shrink: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.18);
  border: 2px solid rgba(255, 255, 255, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 1.5rem;
  letter-spacing: 0.05em;
}
.hero-text {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  min-width: 0;
}
.hero-name {
  margin: 0;
  font-size: 1.6rem;
  font-weight: 700;
  line-height: 1.1;
}
.hero-doc {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.85rem;
  opacity: 0.85;
}
.hero-chips {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  align-items: center;
  margin-top: 0.35rem;
}
.chip {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.2rem 0.6rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(2px);
}
.chip .pi {
  font-size: 0.6rem;
}
.chip-active {
  color: #fff;
}
.chip-active .pi-circle-fill {
  color: #4ade80;
}
.chip-triage {
  font-size: 0.75rem;
}

.hero-facts {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 1rem 1.5rem;
  padding: 1.25rem 1.5rem;
}
.fact {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}
.fact-icon {
  font-size: 1.1rem;
  color: var(--p-primary-color);
  width: 2.25rem;
  height: 2.25rem;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--p-border-radius);
  background: var(--p-primary-50);
}
.fact-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.fact-label {
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--p-text-muted-color);
}
.fact-value {
  font-weight: 600;
  font-size: 0.9rem;
}
</style>

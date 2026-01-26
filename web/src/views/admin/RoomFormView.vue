<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter, useRoute } from 'vue-router'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import { useErrorHandler } from '@/composables/useErrorHandler'
import Card from 'primevue/card'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Select from 'primevue/select'
import Message from 'primevue/message'
import { useRoomStore } from '@/stores/room'
import { roomSchema, type RoomFormData } from '@/validation/room'
import { RoomType } from '@/types/room'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const { showError, showSuccess } = useErrorHandler()
const roomStore = useRoomStore()

const loading = ref(false)
const isEditMode = computed(() => !!route.params.id)
const roomId = computed(() => Number(route.params.id) || null)

const roomTypeOptions = computed(() => [
  { label: t('room.types.PRIVATE'), value: RoomType.PRIVATE },
  { label: t('room.types.SHARED'), value: RoomType.SHARED }
])

const { defineField, handleSubmit, errors, setValues } = useForm<RoomFormData>({
  validationSchema: toTypedSchema(roomSchema),
  initialValues: {
    number: '',
    type: RoomType.PRIVATE,
    capacity: 1
  }
})

const [number] = defineField('number')
const [type] = defineField('type')
const [capacity] = defineField('capacity')

onMounted(async () => {
  if (isEditMode.value && roomId.value) {
    await loadRoom()
  }
})

async function loadRoom() {
  loading.value = true
  try {
    const room = await roomStore.fetchRoom(roomId.value!)
    setValues({
      number: room.number,
      type: room.type,
      capacity: room.capacity
    })
  } catch (error) {
    showError(error)
    router.push({ name: 'rooms' })
  } finally {
    loading.value = false
  }
}

const onSubmit = handleSubmit(async (values) => {
  loading.value = true
  try {
    if (isEditMode.value && roomId.value) {
      await roomStore.updateRoom(roomId.value, values)
      showSuccess('room.updated')
    } else {
      await roomStore.createRoom(values)
      showSuccess('room.created')
    }
    router.push({ name: 'rooms' })
  } catch (error) {
    showError(error)
  } finally {
    loading.value = false
  }
})

function cancel() {
  router.push({ name: 'rooms' })
}
</script>

<template>
  <div class="room-form-page">
    <div class="page-header">
      <h1 class="page-title">
        {{ isEditMode ? t('room.edit') : t('room.new') }}
      </h1>
    </div>

    <Card>
      <template #content>
        <form @submit="onSubmit" class="form-grid">
          <div class="form-field">
            <label for="number">{{ t('room.number') }} *</label>
            <InputText id="number" v-model="number" :class="{ 'p-invalid': errors.number }" />
            <Message v-if="errors.number" severity="error" :closable="false">
              {{ errors.number }}
            </Message>
          </div>

          <div class="form-field">
            <label for="type">{{ t('room.type') }} *</label>
            <Select
              id="type"
              v-model="type"
              :options="roomTypeOptions"
              optionLabel="label"
              optionValue="value"
              :class="{ 'p-invalid': errors.type }"
            />
            <Message v-if="errors.type" severity="error" :closable="false">
              {{ errors.type }}
            </Message>
          </div>

          <div class="form-field">
            <label for="capacity">{{ t('room.capacity') }} *</label>
            <InputNumber
              id="capacity"
              v-model="capacity"
              :min="1"
              :class="{ 'p-invalid': errors.capacity }"
            />
            <Message v-if="errors.capacity" severity="error" :closable="false">
              {{ errors.capacity }}
            </Message>
          </div>

          <div class="form-actions full-width">
            <Button
              type="button"
              :label="t('common.cancel')"
              severity="secondary"
              outlined
              @click="cancel"
            />
            <Button type="submit" :label="t('common.save')" :loading="loading" />
          </div>
        </form>
      </template>
    </Card>
  </div>
</template>

<style scoped>
.room-form-page {
  max-width: 600px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 1.5rem;
}

.page-title {
  margin: 0;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.5rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-field.full-width {
  grid-column: span 2;
}

.form-field label {
  font-weight: 500;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 1rem;
}
</style>

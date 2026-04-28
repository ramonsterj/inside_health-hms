import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createI18n } from 'vue-i18n'
import RoomOccupancyCard from './RoomOccupancyCard.vue'
import en from '@/i18n/locales/en.json'
import es from '@/i18n/locales/es.json'
import { useAuthStore } from '@/stores/auth'
import { RoomGender, RoomType, type RoomOccupancyItem } from '@/types/room'
import { UserStatus } from '@/types/user'

function buildRoom(overrides: Partial<RoomOccupancyItem> = {}): RoomOccupancyItem {
  return {
    id: 1,
    number: '201',
    type: RoomType.SHARED,
    gender: RoomGender.MALE,
    capacity: 3,
    occupiedBeds: 1,
    availableBeds: 2,
    occupants: [
      {
        admissionId: 100,
        patientId: 50,
        patientName: 'Carlos Pérez',
        admissionDate: '2026-04-22'
      }
    ],
    ...overrides
  }
}

function mountCard(props: { room: RoomOccupancyItem; searchHighlight?: string }) {
  const i18n = createI18n({
    legacy: false,
    locale: 'en',
    fallbackLocale: 'en',
    messages: { en, es }
  })
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/', component: { template: '<div />' } }] })
  return mount(RoomOccupancyCard, {
    props,
    global: {
      plugins: [i18n, router],
      stubs: {
        Card: { template: '<div class="card"><slot name="header" /><slot name="content" /></div>' },
        Tag: {
          props: ['value', 'severity'],
          template: '<span class="p-tag" :data-severity="severity">{{ value }}</span>'
        },
        Button: {
          props: ['label', 'icon'],
          template: '<button class="p-button">{{ label }}</button>'
        }
      }
    }
  })
}

describe('RoomOccupancyCard', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    const auth = useAuthStore()
    // Grant admission:create so the "Admit here" button is visible in tests
    auth.user = {
      id: 1,
      username: 'tester',
      email: 't@example.com',
      firstName: 'T',
      lastName: 'X',
      salutation: null,
      salutationDisplay: null,
      roles: [],
      permissions: ['admission:create'],
      status: UserStatus.ACTIVE,
      emailVerified: true,
      mustChangePassword: false,
      createdAt: null,
      localePreference: null,
      phoneNumbers: []
    }
  })

  it('renders the room number and gender/type tags', () => {
    const wrapper = mountCard({ room: buildRoom() })
    expect(wrapper.text()).toContain('Room 201')
    expect(wrapper.text()).toContain('Men')
    expect(wrapper.text()).toContain('Double')
  })

  it('renders one bed slot per unit of capacity', () => {
    const wrapper = mountCard({ room: buildRoom({ capacity: 4, occupants: [] }) })
    expect(wrapper.findAll('.bed-slot')).toHaveLength(4)
  })

  it('renders an Occupied slot for each occupant and Free slots for the rest', () => {
    const wrapper = mountCard({ room: buildRoom() })
    const occupied = wrapper.findAll('.bed-slot--occupied')
    const free = wrapper.findAll('.bed-slot--free')
    expect(occupied).toHaveLength(1)
    expect(free).toHaveLength(2)
    expect(occupied[0]!.text()).toContain('Carlos Pérez')
  })

  it('shows the available/capacity count in the header', () => {
    const wrapper = mountCard({ room: buildRoom() })
    expect(wrapper.text()).toContain('2 / 3')
  })

  it('renders zero occupants when room is empty', () => {
    const wrapper = mountCard({
      room: buildRoom({ occupiedBeds: 0, availableBeds: 3, occupants: [] })
    })
    expect(wrapper.findAll('.bed-slot--occupied')).toHaveLength(0)
    expect(wrapper.findAll('.bed-slot--free')).toHaveLength(3)
  })

  it('renders all occupied when room is full', () => {
    const wrapper = mountCard({
      room: buildRoom({
        capacity: 2,
        occupiedBeds: 2,
        availableBeds: 0,
        occupants: [
          { admissionId: 1, patientId: 10, patientName: 'A', admissionDate: '2026-04-01' },
          { admissionId: 2, patientId: 20, patientName: 'B', admissionDate: '2026-04-02' }
        ]
      })
    })
    expect(wrapper.findAll('.bed-slot--occupied')).toHaveLength(2)
    expect(wrapper.findAll('.bed-slot--free')).toHaveLength(0)
  })

  it('highlights the slot whose occupant matches the search query', () => {
    const wrapper = mountCard({
      room: buildRoom({
        capacity: 2,
        occupiedBeds: 2,
        availableBeds: 0,
        occupants: [
          { admissionId: 1, patientId: 10, patientName: 'María González', admissionDate: '2026-04-01' },
          { admissionId: 2, patientId: 20, patientName: 'Pedro López', admissionDate: '2026-04-02' }
        ]
      }),
      searchHighlight: 'maría'
    })
    const highlighted = wrapper.findAll('.bed-slot--highlighted')
    expect(highlighted).toHaveLength(1)
    expect(highlighted[0]!.text()).toContain('María González')
  })
})

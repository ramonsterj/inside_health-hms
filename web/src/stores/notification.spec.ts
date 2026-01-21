import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useNotificationStore } from './notification'

describe('useNotificationStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('should start with empty notifications', () => {
    const store = useNotificationStore()
    expect(store.notifications).toEqual([])
  })

  it('should add a notification', () => {
    const store = useNotificationStore()
    const id = store.add('success', 'Test Summary', 'Test Detail')

    expect(store.notifications).toHaveLength(1)
    expect(store.notifications[0]).toMatchObject({
      id,
      type: 'success',
      summary: 'Test Summary',
      detail: 'Test Detail'
    })
  })

  it('should remove a notification by id', () => {
    const store = useNotificationStore()
    const id = store.add('info', 'Test', undefined, 0) // life=0 to prevent auto-remove

    expect(store.notifications).toHaveLength(1)
    store.remove(id)
    expect(store.notifications).toHaveLength(0)
  })

  it('should auto-remove notification after life duration', () => {
    const store = useNotificationStore()
    store.add('success', 'Auto Remove', undefined, 3000)

    expect(store.notifications).toHaveLength(1)

    vi.advanceTimersByTime(3000)

    expect(store.notifications).toHaveLength(0)
  })

  it('should not auto-remove if life is 0', () => {
    const store = useNotificationStore()
    store.add('success', 'Persistent', undefined, 0)

    expect(store.notifications).toHaveLength(1)

    vi.advanceTimersByTime(10000)

    expect(store.notifications).toHaveLength(1)
  })

  it('should add success notification with default life', () => {
    const store = useNotificationStore()
    store.success('Success Message')

    expect(store.notifications[0]!.type).toBe('success')
    expect(store.notifications[0]!.summary).toBe('Success Message')
  })

  it('should add error notification with longer life', () => {
    const store = useNotificationStore()
    store.error('Error Message')

    expect(store.notifications[0]!.type).toBe('error')
    expect(store.notifications[0]!.life).toBe(5000)
  })

  it('should add info notification', () => {
    const store = useNotificationStore()
    store.info('Info Message', 'Details')

    expect(store.notifications[0]!.type).toBe('info')
    expect(store.notifications[0]!.detail).toBe('Details')
  })

  it('should add warn notification', () => {
    const store = useNotificationStore()
    store.warn('Warning')

    expect(store.notifications[0]!.type).toBe('warn')
  })

  it('should clear all notifications', () => {
    const store = useNotificationStore()
    store.success('One')
    store.info('Two')
    store.error('Three')

    expect(store.notifications).toHaveLength(3)

    store.clear()

    expect(store.notifications).toHaveLength(0)
  })

  it('should assign unique ids to each notification', () => {
    const store = useNotificationStore()
    const id1 = store.add('success', 'First')
    const id2 = store.add('info', 'Second')
    const id3 = store.add('warn', 'Third')

    expect(id1).not.toBe(id2)
    expect(id2).not.toBe(id3)
  })
})

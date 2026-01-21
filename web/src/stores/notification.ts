import { defineStore } from 'pinia'
import { ref } from 'vue'

export type NotificationType = 'success' | 'info' | 'warn' | 'error'

export interface Notification {
  id: number
  type: NotificationType
  summary: string
  detail?: string
  life?: number
}

let notificationId = 0

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref<Notification[]>([])

  function add(type: NotificationType, summary: string, detail?: string, life = 3000) {
    const notification: Notification = {
      id: ++notificationId,
      type,
      summary,
      detail,
      life
    }
    notifications.value.push(notification)

    if (life > 0) {
      setTimeout(() => {
        remove(notification.id)
      }, life)
    }

    return notification.id
  }

  function remove(id: number) {
    notifications.value = notifications.value.filter(n => n.id !== id)
  }

  function success(summary: string, detail?: string) {
    return add('success', summary, detail)
  }

  function info(summary: string, detail?: string) {
    return add('info', summary, detail)
  }

  function warn(summary: string, detail?: string) {
    return add('warn', summary, detail)
  }

  function error(summary: string, detail?: string) {
    return add('error', summary, detail, 5000)
  }

  function clear() {
    notifications.value = []
  }

  return {
    notifications,
    add,
    remove,
    success,
    info,
    warn,
    error,
    clear
  }
})

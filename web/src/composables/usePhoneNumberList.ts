import type { Ref } from 'vue'
import { PhoneType } from '@/types'
import type { PhoneNumberRequest } from '@/types'

/**
 * Composable for managing a list of phone numbers in a form.
 * Handles add, remove, and update operations with primary flag management.
 */
export function usePhoneNumberList(phoneNumbers: Ref<PhoneNumberRequest[]>) {
  function addPhone() {
    phoneNumbers.value.push({
      phoneNumber: '',
      phoneType: PhoneType.MOBILE,
      isPrimary: phoneNumbers.value.length === 0
    })
  }

  function removePhone(index: number) {
    phoneNumbers.value.splice(index, 1)
  }

  function updatePhone(index: number, phone: PhoneNumberRequest) {
    const currentPhone = phoneNumbers.value[index]
    // If this phone is being marked as primary, uncheck all others
    if (phone.isPrimary && currentPhone && !currentPhone.isPrimary) {
      phoneNumbers.value.forEach((p, i) => {
        if (i !== index) {
          p.isPrimary = false
        }
      })
    }
    phoneNumbers.value[index] = phone
  }

  return {
    addPhone,
    removePhone,
    updatePhone
  }
}

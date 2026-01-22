package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.EmergencyContact

data class EmergencyContactResponse(val id: Long, val name: String, val relationship: String, val phone: String) {
    companion object {
        fun from(contact: EmergencyContact): EmergencyContactResponse = EmergencyContactResponse(
            id = contact.id!!,
            name = contact.name,
            relationship = contact.relationship,
            phone = contact.phone,
        )
    }
}

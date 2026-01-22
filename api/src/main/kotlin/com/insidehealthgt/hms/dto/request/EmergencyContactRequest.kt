package com.insidehealthgt.hms.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class EmergencyContactRequest(
    val id: Long? = null,

    @field:NotBlank(message = "{validation.contact.name.required}")
    @field:Size(max = 200, message = "{validation.contact.name.max}")
    val name: String,

    @field:NotBlank(message = "{validation.contact.relationship.required}")
    @field:Size(max = 100, message = "{validation.contact.relationship.max}")
    val relationship: String,

    @field:NotBlank(message = "{validation.contact.phone.required}")
    @field:Size(max = 20, message = "{validation.contact.phone.max}")
    val phone: String,
)

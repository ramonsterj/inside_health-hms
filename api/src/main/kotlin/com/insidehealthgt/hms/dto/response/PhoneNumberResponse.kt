package com.insidehealthgt.hms.dto.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.insidehealthgt.hms.entity.PhoneType
import com.insidehealthgt.hms.entity.UserPhoneNumber

data class PhoneNumberResponse(
    val id: Long,
    val phoneNumber: String,
    val phoneType: PhoneType,
    @get:JsonProperty("isPrimary")
    val isPrimary: Boolean,
) {
    companion object {
        fun from(phone: UserPhoneNumber): PhoneNumberResponse = PhoneNumberResponse(
            id = phone.id!!,
            phoneNumber = phone.phoneNumber,
            phoneType = phone.phoneType,
            isPrimary = phone.isPrimary,
        )
    }
}

package com.insidehealthgt.hms.dto.request

import com.fasterxml.jackson.annotation.JsonProperty
import com.insidehealthgt.hms.entity.PhoneType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class PhoneNumberRequest(
    val id: Long? = null,

    @field:NotBlank(message = "{validation.phone.number.required}")
    @field:Size(max = 20, message = "{validation.phone.number.max}")
    val phoneNumber: String,

    @field:NotNull(message = "{validation.phone.type.required}")
    val phoneType: PhoneType,

    @JsonProperty("isPrimary")
    val isPrimary: Boolean = false,
)

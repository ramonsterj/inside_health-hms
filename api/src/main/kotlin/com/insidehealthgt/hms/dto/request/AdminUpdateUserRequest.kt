package com.insidehealthgt.hms.dto.request

import com.insidehealthgt.hms.entity.Salutation
import com.insidehealthgt.hms.entity.UserStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.Size

data class AdminUpdateUserRequest(
    @field:Size(max = 100, message = "{validation.firstName.max}")
    val firstName: String? = null,

    @field:Size(max = 100, message = "{validation.lastName.max}")
    val lastName: String? = null,

    val salutation: Salutation? = null,

    val roleCodes: List<String>? = null,

    val status: UserStatus? = null,

    val emailVerified: Boolean? = null,

    @field:Valid
    val phoneNumbers: List<PhoneNumberRequest>? = null,
)

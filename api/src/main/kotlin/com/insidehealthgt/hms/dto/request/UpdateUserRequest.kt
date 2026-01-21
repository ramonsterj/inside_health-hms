package com.insidehealthgt.hms.dto.request

import jakarta.validation.constraints.Size

data class UpdateUserRequest(
    @field:Size(max = 100, message = "{validation.firstName.max}")
    val firstName: String? = null,

    @field:Size(max = 100, message = "{validation.lastName.max}")
    val lastName: String? = null,
)

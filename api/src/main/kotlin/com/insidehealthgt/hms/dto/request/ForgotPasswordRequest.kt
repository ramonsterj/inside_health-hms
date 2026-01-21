package com.insidehealthgt.hms.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class ForgotPasswordRequest(
    @field:NotBlank(message = "{validation.email.required}")
    @field:Email(message = "{validation.email.invalid}")
    val email: String,
)

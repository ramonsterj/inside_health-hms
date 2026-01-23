package com.insidehealthgt.hms.dto.request

import com.insidehealthgt.hms.entity.Salutation
import com.insidehealthgt.hms.entity.UserStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

data class CreateUserRequest(
    @field:NotBlank(message = "{validation.username.required}")
    @field:Size(min = 3, max = 50, message = "{validation.username.size}")
    val username: String,

    @field:NotBlank(message = "{validation.email.required}")
    @field:Email(message = "{validation.email.invalid}")
    val email: String,

    @field:NotBlank(message = "{validation.password.required}")
    @field:Size(min = 8, message = "{validation.password.min}")
    val password: String,

    @field:Size(max = 100, message = "{validation.firstName.max}")
    val firstName: String? = null,

    @field:Size(max = 100, message = "{validation.lastName.max}")
    val lastName: String? = null,

    val salutation: Salutation? = null,

    val roleCodes: List<String> = emptyList(),

    val status: UserStatus? = null,

    val emailVerified: Boolean? = null,

    @field:Valid
    @field:NotEmpty(message = "{validation.phone.required}")
    val phoneNumbers: List<PhoneNumberRequest>,
)

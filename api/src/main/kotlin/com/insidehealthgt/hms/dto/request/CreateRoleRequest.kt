package com.insidehealthgt.hms.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class CreateRoleRequest(
    @field:NotBlank(message = "{validation.role.code.required}")
    @field:Size(min = 2, max = 50, message = "{validation.role.code.size}")
    @field:Pattern(
        regexp = "^[A-Z][A-Z0-9_]*$",
        message = "Role code must be uppercase letters, numbers, and underscores, starting with a letter",
    )
    val code: String,

    @field:NotBlank(message = "{validation.role.name.required}")
    @field:Size(max = 255, message = "{validation.role.name.size}")
    val name: String,

    @field:Size(max = 500, message = "{validation.role.description.size}")
    val description: String? = null,

    val permissionCodes: List<String> = emptyList(),
)
